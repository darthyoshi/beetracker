/*
* BeeTracker
* Copyright (C) 2015 Kay Choi
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package beetracker;

import com.jogamp.newt.event.WindowEvent;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.data.FloatList;
import processing.data.JSONObject;
import processing.video.Movie;

/**
 * @class BeeTracker
 * @author Kay Choi, 909926828
 * @date 25 Jul 16
 * @description A tool for tracking bees in a video.
 */
@SuppressWarnings("serial")
public class BeeTracker extends PApplet {
  static final int[] viewBounds = {50, 70, 749, 569};
  private static final int[] defaultThresholds = {40, 60, 255, 60, 255};
  private static final String months[] = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  };
  private static final String[] thresholdKeys = {
    "hue",
    "satMin", "satMax",
    "valMin", "valMax"
  };
  private static final String[] imgTypes = {"gif", "jpg", "png", "tga"};

  private processing.data.IntList colors;
  private int[] movieDims = null, movieOffset, frameDims, frameOffset;
  private float[] exitCenter;

  private final java.util.concurrent.Semaphore sem =
    new java.util.concurrent.Semaphore(1, true);

  private boolean isPlaying = false;
  private boolean record = false, replay = false;
  private boolean pip = false, selectExit = false;
  private int listVal = -1;

  private boolean imgSequenceMode = false;
  private boolean waggleMode = false;

  private File currentFile = null;

  private float[] insetBox, exitRadial;
  private boolean isDrag = false;

  private Movie movie = null;
  private String[] imgNames = null;
  private PImage[] imgSequence = null;
  private PImage stillFrame = null;
  private int imgIndex = -1;
  private float duration = -1f;
  private float time = -1f;
  private int fps = 0;

  private UIControl uic;
  private BlobDetectionUtils bdu;
  private TrackingUtils tu;

  private PrintStream log = null;

  private PImage titleImg;

  static final boolean debug = false;

  private HashMap<Float, HashMap<Integer, List<float[]>>> allFramePoints = null;
  private HashMap<Integer, List<float[]>> centroids;
  private FloatList allFrameTimes = null;
  private int timeStampIndex = -1;
  private boolean replayCheckForTimeOut;

  private String videoName = null;

  private Calendar videoDate = null;

  private FloatList settingsTimeStamps = null;
  private HashMap<Float, float[]> insets, radials;
  private HashMap<Float, int[]> thresholds;
  private int settingIndex = 0;
  private int[] threshold;

  private PGraphics viewFrame;

  private JDialog eventDialog = null;

  private HashMap<Float, String> msgs;

  processing.core.PFont font;

  /**
   * Overrides from PApplet.
   */
  @Override
  public void settings() {
    size(800, 620, P2D);

    processing.opengl.PJOGL.setIcon("img/icon.png");
  }

  /**
   * Overrides from PApplet.
   */
  @Override
  public void setup() {
    titleImg = loadImage("img/title.png");

    //create log file
    if(!debug) {
      try {
        log = new PrintStream(new File("Console.log"), "UTF-8");

        System.setErr(log);
        System.setOut(log);
      } catch (java.io.FileNotFoundException |
        java.io.UnsupportedEncodingException ex)
      {
        java.util.logging.Logger.getLogger(BeeTracker.class.getName())
          .log(java.util.logging.Level.SEVERE, null, ex);
        crash(ex.toString());
      }
    }

    font = loadFont("LiberationSansNarrow-15.vlw");
    textFont(font);

    uic = new UIControl(this);

    frameRate(60);
    strokeWeight(1);

    File outputDir = new File(System.getProperty("user.dir") +
      File.separatorChar + "output");
    outputDir.mkdir();

    surface.setTitle("BeeTracker");

    ((com.jogamp.newt.opengl.GLWindow)surface.getNative()).addWindowListener(
      new com.jogamp.newt.event.WindowAdapter() {
        @Override
        public void windowDestroyNotify(WindowEvent arg0) {
          exit();
        }

        @Override
        public void windowDestroyed(WindowEvent arg0) {
          exit();
        }
      }
    );

    bdu = new BlobDetectionUtils(this, width, height);

    tu = new TrackingUtils(this);

    movieOffset = new int[2];
    frameDims = new int[2];
    frameOffset = new int[2];

    exitCenter = new float[2];

    viewFrame = createGraphics(
      viewBounds[2] - viewBounds[0] + 1,
      viewBounds[3] - viewBounds[1] + 1
    );
    viewFrame.beginDraw();
    viewFrame.textSize(10);
    viewFrame.textAlign(LEFT, TOP);
    viewFrame.endDraw();

    msgs = new HashMap<>();
  }

  /**
   * Loads a settings file.
   * @param filePath the path to the file
   */
  private void loadSettings(String filePath) {
    colors = new processing.data.IntList();
    settingsTimeStamps = new FloatList();

    File settings = new File(filePath);

    boolean[] settingsErrors = {!settings.exists(), false, false};

    System.out.append("initializing settings... ").flush();

    if(!settingsErrors[0]) {
      try {
        JSONObject jsonSettings = loadJSONObject(settings.getAbsolutePath());

        int tmp;
        String jsonKey, timeKey;
        JSONObject jsonSetting, timeSetting, setting;
        Iterator<?> jsonIter, settingIter;

        System.out.append("reading from file - ").flush();

        //initialize color list
        try {
          jsonSetting = jsonSettings.getJSONObject("colors");
          jsonIter = jsonSetting.keyIterator();
          while(jsonIter.hasNext()) {
            tmp = (int)Long.parseLong(
              jsonSetting.getString((String) jsonIter.next()), 16);

            uic.addListItem(String.format("%06x", tmp));

            colors.append(tmp);
          }
        } catch(NumberFormatException e1) {
          settingsErrors[1] = true;

          e1.printStackTrace(System.err);
        } finally {
          uic.updateColorLabel(UIControl.listLbl);
        }

        thresholds = new HashMap<>();
        radials = new HashMap<>();
        insets = new HashMap<>();

        try {
          waggleMode = jsonSettings.getString("eventType", "exit").equals("waggle");
          uic.activateEventRadio(waggleMode);
          modeRadios(waggleMode ? 1: 0);

          jsonSetting = jsonSettings.getJSONObject("time");
          settingIter = jsonSetting.keyIterator();
          float timeStamp;
          while(settingIter.hasNext()) {
            timeKey = (String)settingIter.next();
            timeStamp = Float.parseFloat(timeKey);
            settingsTimeStamps.append(timeStamp);

            timeSetting = jsonSetting.getJSONObject(timeKey);

            //initialize selection box
            try {
              insetBox = new float[4];

              setting = timeSetting.getJSONObject("insetBox");
              jsonIter = setting.keyIterator();

              while(jsonIter.hasNext()) {
                jsonKey = (String) jsonIter.next();
                tmp = Integer.parseInt(jsonKey);

                insetBox[tmp] = setting.getFloat(jsonKey);
              }
            } catch(Exception e2) {
              insetBox[0] = insetBox[1] = 0f;
              insetBox[2] = insetBox[3] = 1f;

              e2.printStackTrace(System.err);
            } finally {
              insets.put(timeStamp, insetBox);
            }

            //initialize exit circle
            try {
              exitRadial = new float[4];

              setting = timeSetting.getJSONObject("exitRadial");
              jsonIter = setting.keyIterator();

              while(jsonIter.hasNext()) {
                jsonKey = (String) jsonIter.next();
                tmp = Integer.parseInt(jsonKey);

                exitRadial[tmp] = setting.getFloat(jsonKey);
              }
            } catch(Exception e3) {
              exitRadial[0] = exitRadial[1] = 0.5f;
              exitRadial[2] = exitRadial[3] = 0.5f;

              e3.printStackTrace(System.err);
            } finally {
              radials.put(timeStamp, exitRadial);
            }

            //set thresholds
            threshold = new int[5];
            try {
              setting = timeSetting.getJSONObject("thresholds");

              for(tmp = 0; tmp < thresholdKeys.length; tmp++) {
                threshold[tmp] = setting.getInt(thresholdKeys[tmp]);
              }
            } catch(Exception e4) {
              threshold[0] = defaultThresholds[0];
              threshold[1] = defaultThresholds[1];
              threshold[2] = defaultThresholds[2];
              threshold[3] = defaultThresholds[3];
              threshold[4] = defaultThresholds[4];

              e4.printStackTrace(System.err);
            } finally {
              thresholds.put(timeStamp, threshold);

              uic.setThresholdValue(threshold[uic.getThresholdType()]);
            }
          }
        } catch(RuntimeException e5) {
          settingsErrors[2] = true;

          e5.printStackTrace(System.err);
        }
      } catch(RuntimeException ex) {
        ex.printStackTrace(System.err);
      }
    } else {
      System.out.append("file not found - ").flush();
    }

    if(settingsErrors[0] || settingsErrors[1]) {
      colors.clear();
    }

    if(settingsErrors[0] || settingsErrors[2]) {
      settingsTimeStamps.clear();
      settingsTimeStamps.append(0f);

      thresholds = new HashMap<>();
      threshold = new int[5];
      threshold[0] = defaultThresholds[0];
      threshold[1] = defaultThresholds[1];
      threshold[2] = defaultThresholds[2];
      threshold[3] = defaultThresholds[3];
      threshold[4] = defaultThresholds[4];
      thresholds.put(0f, threshold);

      radials = new HashMap<>();
      exitRadial = new float[4];
      exitRadial[0] = exitRadial[1] = 0.5f;
      exitRadial[2] = exitRadial[3] = 0.5f;
      radials.put(0f, exitRadial);

      insets = new HashMap<>();
      insetBox = new float[4];
      insetBox[0] = insetBox[1] = 0f;
      insetBox[2] = insetBox[3] = 1f;
      insets.put(0f, insetBox);
    }

    System.out.append("done\n").flush();
  }

  /**
   * Overrides from PApplet.
   */
  @SuppressWarnings("unused")
  @Override
  public void draw() {
    background(0x222222);

    textAlign(CENTER, CENTER);
    textSize(24);

    //menu bar
    strokeWeight(0);
    fill(0xffeeeeee);
    rectMode(CORNERS);
    rect(-1, -1, width+1, 21);

    //main window
    strokeWeight(1);
    stroke(0xffffffff);
    fill(0xff444444);
    rect(viewBounds[0]-1, viewBounds[1]-1, viewBounds[2]+1, viewBounds[3]+1);

    //begin critical section
    try {
      sem.acquire();
    } catch (InterruptedException ex) {
      ex.printStackTrace(System.err);

      Thread.currentThread().interrupt();
    }

    PImage curFrame = updateFrame();

    if(curFrame != null) {
      time = imgSequenceMode ? ((float)imgIndex)/fps : movie.time();

      if(movieDims != null) {
        viewFrame.beginDraw();
        viewFrame.clear();
        viewFrame.copy(
          curFrame,
          0,
          0,
          curFrame.width,
          curFrame.height,
          movieOffset[0]-viewBounds[0],
          movieOffset[1]-viewBounds[1],
          movieDims[0],
          movieDims[1]
        );

        PImage insetFrame = copyInsetFrame(curFrame);

        float[] exitAxes = new float[2];
        if(pip) {
          exitAxes[0] = exitRadial[2]*frameDims[0]/(insetBox[2]-insetBox[0]);
          exitAxes[1] = exitRadial[3]*frameDims[1]/(insetBox[3]-insetBox[1]);
        } else {
          exitAxes[0] = exitRadial[2]*movieDims[0];
          exitAxes[1] = exitRadial[3]*movieDims[1];
        }

        if(insetFrame != null) {
          float timeStamp;

          if(settingIndex >= 0 &&
            settingIndex < settingsTimeStamps.size() - 1)
          {
            timeStamp = settingsTimeStamps.get(settingIndex + 1);

            if(time - timeStamp > 0.000001f) {
              threshold = thresholds.get(timeStamp);
              uic.setThresholdValue(threshold[uic.getThresholdType()]);

              exitRadial = radials.get(timeStamp);
              insetBox = insets.get(timeStamp);
              settingIndex++;

              updateFrameParams();
              updateExitCenter();
            }
          }

          if(replay) {
            if(timeStampIndex >= 0 &&
              timeStampIndex < allFrameTimes.size())
            {
              timeStamp = allFrameTimes.get(timeStampIndex);

              if(debug) {
                println("frame time: " + timeStamp +
                  "s, actual time " + time + 's');
              }

              if(time > timeStamp) {
                replayCheckForTimeOut = true;

                centroids = allFramePoints.get(timeStamp);

                timeStampIndex++;
              }
            }

            //clear point data after 1s
            if(
              replayCheckForTimeOut &&
              timeStampIndex > 0 &&
              time - allFrameTimes.get(timeStampIndex-1) > 1f
            ) {
              replayCheckForTimeOut = false;

              centroids = new HashMap<>();

              for(int color : colors) {
                centroids.put(color, new ArrayList<float[]>(1));
              }
            }
          } else {
            bdu.filterImg(insetFrame, colors, threshold);

            //BlobDetection expects certain image size
            insetFrame.resize(bdu.getImageWidth(), bdu.getImageHeight());

            centroids = bdu.getCentroids(insetFrame, colors);
          }

          if(isPlaying) {
            if(record) {
              boolean frameIsEmpty = true;

              for(int color : colors) {
                if(!centroids.get(color).isEmpty()) {
                  frameIsEmpty = false;
                  break;
                }
              }

              if(!frameIsEmpty && !allFrameTimes.hasValue(time)) {
                allFrameTimes.append(time);
                allFramePoints.put(time, centroids);
              }
            }

            if(record || replay) {
              if(debug) {
                println(String.format("---------BEGIN FRAME (%.2fs)---------\npip: %b", time, pip));
              }

              tu.trackCentroids(
                centroids,
                frameDims, frameOffset,
                exitCenter, exitAxes,
                movieDims, movieOffset,
                time,
                duration
              );
            }
          }

          int[] insetOffset = new int[2];

          //zoomed
          if(pip) {
            PImage zoomedInset = copyInsetFrame(curFrame);

            insetOffset[0] = (viewFrame.width-frameDims[0])/2;
            insetOffset[1] = (viewFrame.height-frameDims[1])/2;

            if(zoomedInset != null) {
              viewFrame.copy(
                zoomedInset,
                0,
                0,
                zoomedInset.width,
                zoomedInset.height,
                insetOffset[0],
                insetOffset[1],
                frameDims[0],
                frameDims[1]
              );
            }
          } else {
            insetOffset[0] = (int)(insetBox[0]*movieDims[0]) +
              movieOffset[0] - viewBounds[0];
            insetOffset[1] = (int)(insetBox[1]*movieDims[1]) +
              movieOffset[1] - viewBounds[1];
          }

          if(!replay) {
            //highlight filtered pixels
            viewFrame.blend(
              insetFrame,
              0,
              0,
              insetFrame.width,
              insetFrame.height,
              insetOffset[0],
              insetOffset[1],
              frameDims[0],
              frameDims[1],
              ADD
            );

            //draw detected blobs
            bdu.drawBlobs(viewFrame, viewBounds,
              frameDims, frameOffset);
          } else {  //mark bees
            viewFrame.stroke(0xffdddd00);
            viewFrame.ellipseMode(CENTER);
            viewFrame.colorMode(RGB, 255);

            List<float[]> centroidList;
            for(int color : colors) {
              viewFrame.fill(0xff000000 + color);

              centroidList = centroids.get(color);
              if(centroidList != null) {
                for(float[] centroid : centroidList) {
                  viewFrame.ellipse(
                    centroid[0]*frameDims[0] +
                      frameOffset[0] - viewBounds[0],
                    centroid[1]*frameDims[1] +
                      frameOffset[1] - viewBounds[1],
                    10,
                    10
                  );
                }
              }
            }
          }

          //draw recorded paths
          tu.drawPaths(viewFrame, frameDims, frameOffset);
        }

        //list recent events
        if(!msgs.isEmpty()) {
          FloatList msgTimes = new FloatList(msgs.size());
          for(Float t : msgs.keySet()) {
            msgTimes.append(t);
          }
          msgTimes.sort();
          StringBuilder msg = new StringBuilder();
          for(float t : msgTimes) {
            if(time < t) {
              msg.append(msgs.get(t)).append('\n');
            } else {
              msgs.remove(t);
              msgTimes.remove(msgTimes.index(t));
            }
          }
          viewFrame.fill(0xff000000);
          viewFrame.text(msg.toString(), 6, 6);
          viewFrame.text(msg.toString(), 4, 4);
          viewFrame.text(msg.toString(), 6, 4);
          viewFrame.text(msg.toString(), 4, 6);
          viewFrame.fill(0xffffffff);
          viewFrame.text(msg.toString(), 5, 5);
        }

        viewFrame.endDraw();

        imageMode(CORNER);
        image(viewFrame, viewBounds[0], viewBounds[1]);

        //bee count
        if(isPlaying && record && debug) {
          println("---------END FRAME---------");
        }

        stroke(0xffffa600);
        ellipseMode(RADIUS);
        noFill();
        rectMode(CORNER);

        //unzoomed
        if(!pip) {
          //inset box
          rect(
            (int)(insetBox[0]*movieDims[0]) + movieOffset[0],
            (int)(insetBox[1]*movieDims[1]) + movieOffset[1],
            (int)((insetBox[2]-insetBox[0])*movieDims[0]) - 1,
            (int)((insetBox[3]-insetBox[1])*movieDims[1]) - 1
          );

          if(!waggleMode) {
            //exit circle
            ellipse(
              exitRadial[0]*movieDims[0] + movieOffset[0],
              exitRadial[1]*movieDims[1] + movieOffset[1],
              exitAxes[0],
              exitAxes[1]
            );
          }
        } else {  //zoomed
          //inset box
          rect(
            frameOffset[0],
            frameOffset[1],
            frameDims[0] - 1,
            frameDims[1] - 1
          );

          //exit circle
          if(!waggleMode) {
            ellipse(
              exitCenter[0],
              exitCenter[1],
              exitAxes[0],
              exitAxes[1]
            );
          }
        }

        if(isDrag && !selectExit) {
          //draw selection box diagonals
          line(
            insetBox[0]*movieDims[0] + movieOffset[0],
            insetBox[1]*movieDims[1] + movieOffset[1],
            insetBox[2]*movieDims[0] + movieOffset[0],
            insetBox[3]*movieDims[1] + movieOffset[1]
          );
          line(
            insetBox[0]*movieDims[0] + movieOffset[0],
            insetBox[3]*movieDims[1] + movieOffset[1],
            insetBox[2]*movieDims[0] + movieOffset[0],
            insetBox[1]*movieDims[1] + movieOffset[1]
          );
        } else if(!waggleMode){
          int[] tmpDims;

          if(pip) {
            tmpDims = new int[2];
            tmpDims[0] = (int)(frameDims[0]/(insetBox[2]-insetBox[0]));
            tmpDims[1] = (int)(frameDims[1]/(insetBox[3]-insetBox[1]));
          } else {
            tmpDims = movieDims;
          }

          //draw selection circle interior lines
          line(
            exitCenter[0],
            exitCenter[1] - exitRadial[3]*tmpDims[1],
            exitCenter[0],
            exitCenter[1] + exitRadial[3]*tmpDims[1]
          );
          line(
            exitCenter[0] - exitRadial[2]*tmpDims[0],
            exitCenter[1],
            exitCenter[0] + exitRadial[2]*tmpDims[0],
            exitCenter[1]
          );
        }

        //end of movie reached
        if(
          isPlaying &&
          ((time >= duration) || (!imgSequenceMode &&
          duration - time <= 1f/movie.frameRate))
        ) {
          if(record && !waggleMode) {
            tu.eventCheckAll(
              frameDims, frameOffset,
              exitCenter, exitAxes,
              time
            );
          }

          isPlaying = false;

          StringBuilder msg = new StringBuilder("End of video reached.\n");
          msg.append("video date: ")
            .append(months[videoDate.get(Calendar.MONTH)])
            .append(' ')
            .append(Integer.toString(videoDate.get(Calendar.DATE)))
            .append(' ')
            .append(Integer.toString(videoDate.get(Calendar.YEAR)))
            .append(',')
            .append(String.format(
              "%02d:%02d:%02d\n\n",
              videoDate.get(Calendar.HOUR_OF_DAY),
              videoDate.get(Calendar.MINUTE),
              videoDate.get(Calendar.SECOND)
            ));

          HashMap<Float, String> formattedTime = new HashMap<>();
          HashMap<Float, String> summary = tu.getSummary();
          FloatList timeStamps = new FloatList(summary.size());

          if(allFramePoints.isEmpty()) {
            msg.append("No points saved!")
              .append(" Enable recording to generate events.\n");
          } else {
            msg.append("Summary of events: \n");

            for(Float timeStamp : summary.keySet()) {
              timeStamps.append(timeStamp);
            }
            timeStamps.sort();

            StringBuilder timeStringBuilder;
            int tmp;
            for(float timeStamp : timeStamps) {
              tmp = (int)(timeStamp*100f);

              timeStringBuilder = new StringBuilder();
              timeStringBuilder.append(String.format(
                "%02d:%02d:%02d.%02d",
                tmp/360000,
                (tmp/6000)%60,
                (tmp/100)%60,
                tmp%100
              ));

              formattedTime.put(timeStamp, timeStringBuilder.toString());

              msg.append(timeStringBuilder.toString())
                .append(" - bee #")
                .append(summary.get(timeStamp).replaceAll(",", ", "))
                .append('\n');
            }
          }

          if(debug) {
            println('\n' + msg.toString());
          }

          String path = saveSummaryResults(timeStamps, formattedTime, summary);

          PGraphics events;
          if((record || replay) && path != null) {
            events = tu.getEventTimeline(duration, duration);

            char[] tmp = path.toCharArray();
            tmp[tmp.length - 3] = 'p';
            tmp[tmp.length - 2] = 'n';
            tmp[tmp.length - 1] = 'g';

            events.save(String.valueOf(tmp));
          } else {
            events = null;
          }

          MessageDialogue.endVideoMessage(this, msg.toString(),
            (events == null ? null : events.get()), path);
        }

        uic.draw(this, settingsTimeStamps);
      } else {
        text(
          "Loading...",
          (viewBounds[0]+viewBounds[2])*.5f,
          (viewBounds[1]+viewBounds[3])*.5f
        );

        uic.draw(this, settingsTimeStamps);

        if(curFrame.height > 0) {
          if(!imgSequenceMode) {
            duration = movie.duration();

            uic.setSeekRange(duration);
          }

          movieDims = scaledDims(
            curFrame.width,
            curFrame.height
          );

          movieOffset[0] = (viewBounds[2]-viewBounds[0]-movieDims[0])/2 + viewBounds[0];
          movieOffset[1] = (viewBounds[3]-viewBounds[1]-movieDims[1])/2 + viewBounds[1];

          updateSettings(0f);

          updateExitCenter();
        }
      }
    } else {
      imageMode(CENTER);
      image(
        titleImg,
        (viewBounds[0]+viewBounds[2])*.5f,
        (viewBounds[1]+viewBounds[3])*.5f
      );

      uic.draw(this, settingsTimeStamps);
    }

    //end critical section
    sem.release();
  }

  /**
   * Retrieves the next frame for analysis.
   * @return the new frame as a PImage
   */
  private PImage updateFrame() {
    PImage result = null;

    if(imgSequenceMode) {
      if(imgSequence != null) {
        if(isPlaying) {
          stillFrame = null;

          if(imgIndex < imgSequence.length-1) {
            imgIndex++;
          }

          uic.setSeekTime(((float)imgIndex)/fps);
        }

        while(stillFrame == null && imgIndex < imgSequence.length) {
          updateImgSequence(imgIndex);

          stillFrame = imgSequence[imgIndex];

          if(stillFrame.width <= 0) {
            stillFrame = null;

            if(imgIndex < imgSequence.length-1) {
              imgIndex++;
            }
          }
        }

        result = stillFrame;
      }
    } else if(movie != null) {
      if(movie.available()) {
        movie.read();

        if(!isPlaying) {
          movie.jump(uic.getSeekTime());
          movie.pause();
        } else {
          uic.setSeekTime(movie.time());
        }
      }

      result = movie.get();
    }

    return result;
  }

  /**
   * Loads images in the image sequence into memory.
   * @param index the position of the image in the sequence to load
   */
  private void updateImgSequence(int index) {
    int i;
    for(i = 0; i < index; i++) {
      imgSequence[i] = null;
    }
    if(imgSequence[i] == null) {
      if(debug) {
        println("image sequence \"stream\": " + imgNames[i]);
      }

      imgSequence[i] = loadImage(imgNames[i]);
    }
    i++;
    while(i < index + 10 && i< imgNames.length) {
      if(imgSequence[i] == null) {
        if(debug) {
          println("image sequence \"stream\": " + imgNames[i]);
        }

        imgSequence[i] = requestImage(imgNames[i]);
      }
      i++;
    }
    while(i < imgNames.length) {
      imgSequence[i] = null;
      i++;
    }
  }

  /**
   * Loads a sequence of images.
   * @param dir a File object representing the parent directory of the images
   */
  public void loadImgSequence(final File dir) {
    if(dir != null) {
      final BeeTracker self = this;

      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          imgSequenceMode = true;

          preLoad(dir);

          java.util.LinkedList<String> list = new java.util.LinkedList<>();
          videoName = dir.getName();

          String name;
          for(File file : dir.listFiles()) {
            if(file.isFile()) {
              name = file.getName();

              //only add images
              for(String ext : imgTypes) {
                if(name.toLowerCase(java.util.Locale.ROOT).endsWith(ext)) {
                  list.add(file.getAbsolutePath());

                  break;
                }
              }
            }
          }

          if(list.size() > 0) {
            //begin critical section
            try {
              sem.acquire();
            } catch (InterruptedException e) {
              e.printStackTrace(System.err);

              Thread.currentThread().interrupt();
            }

            imgNames = new String[list.size()];
            list.toArray(imgNames);
            java.util.Arrays.sort(imgNames);

            imgSequence = new PImage[list.size()];
            for(imgIndex = 0; imgIndex < imgSequence.length; imgIndex++) {
              imgSequence[imgIndex] = null;
            }

            if(debug) {
              for(String fileName : imgNames) {
                println(fileName);
              }
            }

            imgIndex = 0;
            updateImgSequence(imgIndex);

            System.out.append("images loaded\n").flush();

            loadSettings(
              System.getProperty("user.dir") + File.separatorChar +
              "output" + File.separatorChar +
              videoName + File.separatorChar +
              "settings.json"
            );

            //end critical section
            sem.release();

            EventQueue.invokeLater(new Runnable() {
              @Override
              public void run() {
                VideoBrowser.setFrameRate(self);
              }
            });

            postLoad();
          } else {
            uic.setSetupGroupVisibility(false);
            uic.setOpenButtonVisibility(true);
            uic.setPlayVisibility(false);
            uic.setThresholdVisibility(false);

            System.out.append("directory contains no images, cancelling\n").flush();
            MessageDialogue.showEmptyDirectoryMessage();
          }
        }
      });
    } else {
      System.out.append("image selection canceled\n").flush();
    }
  }

  /**
   * Updates the coordinates of the exit center on the screen.
   */
  private void updateExitCenter() {
    if(!pip) {
      exitCenter[0] = exitRadial[0]*movieDims[0] + movieOffset[0];
      exitCenter[1] = exitRadial[1]*movieDims[1] + movieOffset[1];
    } else {
      exitCenter[0] = (exitRadial[0]-insetBox[0])*frameDims[0]/
        (insetBox[2]-insetBox[0]) + frameOffset[0];
      exitCenter[1] = (exitRadial[1]-insetBox[1])*frameDims[1]/
        (insetBox[3]-insetBox[1]) + frameOffset[1];
    }

    float[] exitXY = new float[] {
      (exitRadial[0]-insetBox[0])/(insetBox[2]-insetBox[0]),
      (exitRadial[1]-insetBox[1])/(insetBox[3]-insetBox[1])
    };
    float[] exitAxes = new float[] {
      exitRadial[2]/(insetBox[2]-insetBox[0]),
      exitRadial[3]/(insetBox[3]-insetBox[1])
    };

    bdu.setExit(exitXY, exitAxes);
  }

  /**
   * Saves the statistics of the current video to file.
   * @param timeStamps a sorted FloatList of time stamps
   * @param formattedTime a HashMap mapping float time stamps to formatted
   *   time stamp strings
   * @param summary a HashMap mapping float time stamps to string event
   *   descriptions
   * @return the relative path to the new file in the format
   *   "<video name>/dd.mmm.yyyy-hhmm.csv"
   *   OR
   *   null if no points were recorded
   */
  private String saveSummaryResults(
    FloatList timeStamps,
    HashMap<Float, String> formattedTime,
    HashMap<Float, String> summary
  ) {
    String fileName = null;

    if(!allFramePoints.isEmpty()) {
      Calendar date = Calendar.getInstance();

      File dir = new File(System.getProperty("user.dir") +
        File.separatorChar + "output" + File.separatorChar + videoName);
      dir.mkdir();

      fileName = String.format(
        "%s%c%02d.%s.%d-%02d%02d.csv",
        dir.getAbsolutePath(),
        File.separatorChar,
        date.get(Calendar.DAY_OF_MONTH),
        months[date.get(Calendar.MONTH)],
        date.get(Calendar.YEAR),
        date.get(Calendar.HOUR_OF_DAY),
        date.get(Calendar.MINUTE)
      );

      java.io.BufferedWriter writer = null;
      try {
        writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
          new java.io.FileOutputStream(fileName), "UTF-8")
        );

        StringBuilder dateString = new StringBuilder();
        dateString.append('"')
          .append(months[videoDate.get(Calendar.MONTH)])
          .append(' ')
          .append(Integer.toString(videoDate.get(Calendar.DATE)))
          .append(' ')
          .append(Integer.toString(videoDate.get(Calendar.YEAR)))
          .append(',')
          .append(String.format(
            "%02d:%02d:%02d\",",
            videoDate.get(Calendar.HOUR_OF_DAY),
            videoDate.get(Calendar.MINUTE),
            videoDate.get(Calendar.SECOND)
          ));

        writer.append("\"video date\",\"seek time\",\"bee ID\",color,type\n").flush();
        for(float timeStamp : timeStamps) {
          writer.append(dateString.toString())
            .append(formattedTime.get(timeStamp))
            .append(',')
            .append(summary.get(timeStamp))
            .append('\n')
            .flush();
        }
      } catch (IOException e) {
        e.printStackTrace(System.err);
      } finally {
        if(writer != null) {
          try {
            writer.close();
          } catch (IOException e) {
            e.printStackTrace(System.err);
          }
        }
      }
    }

    return fileName;
  }

  /**
   * Copies the inset frame for image processing and blob detection.
   * @param src the source frame to process
   * @return a copy of the inset frame, or null if the inset has not been defined
   */
  private PImage copyInsetFrame(PImage src) {
    PImage result;

    //don't do anything until inset dimensions have stabilized
    if(isDrag && !selectExit) {
      result = null;
    } else {
      int insetWidth = (int)(src.width*(insetBox[2] - insetBox[0]));
      int insetHeight = (int)(src.height*(insetBox[3] - insetBox[1]));

      result = createImage(insetWidth, insetHeight, ARGB);
      result.copy(
        src,
        (int)(src.width*insetBox[0]),
        (int)(src.height*insetBox[1]),
        insetWidth,
        insetHeight,
        0,
        0,
        insetWidth,
        insetHeight
      );
    }

    return result;
  }

  /**
   * ControlP5 callback method.
   */
  public void editColor() {
    ColorPicker.getColor(this);
  }

  /**
   * Edits the selected color in the color list.
   * @param newColor the new 6-digit hexadecimal RGB value
   */
  void setColor(Integer newColor) {
    if(newColor != null) {
      if(listVal < 0) {
        if(!colors.hasValue(newColor)) {
          String code = String.format("%06x", newColor);

          uic.addListItem(code);

          colors.append(newColor);
        }
      } else {
        colors.set(listVal, newColor);

        uic.setListItem(String.format("%06x", newColor), listVal);
      }
    }
  }

  /**
   * ControlP5 callback method.
   */
  public void removeColor() {
    if(listVal >= 0) {
      uic.removeListItem(String.format("%06x", colors.remove(listVal)));

      if(colors.size() == 0) {
        listVal = -1;
      }
    }
  }

  /**
   * ControlP5 callback method.
   */
  public void playButton() {
    if(movie != null || imgSequence != null) {
      boolean status;

      if(!isPlaying) {
        boolean[] errors = {(colors.size() == 0), (exitRadial[2] <= 0f)};

        status = !(errors[0] || errors[1]);

        if(status) {
          isPlaying = true;

          tu.setColors(colors);

          if(!imgSequenceMode) {
            movie.play();
            movie.volume(0f);
          }

          if(debug) {
            print("starting playback...");
          }
        } else {
          MessageDialogue.playButtonErrorMessage(this, errors);

          if(debug) {
            println("error");
          }
        }
      } else {
        isPlaying = false;
        status = true;

        if(debug) {
          print("pausing playback...");
        }

        if(!imgSequenceMode) {
          movie.pause();
        }
      }

      if(status) {
        if(debug) {
          println("done");
        }

        uic.setStatusLabel((isPlaying ? (replay ? 1 : (record ? 2 : 3)) : 0));
        uic.setSetupGroupVisibility(!isPlaying);
        uic.setPlayState(isPlaying);
        uic.setThresholdVisibility(!isPlaying);
      }
    }
  }

  /**
   * ControlP5 callback method.
   */
  public void ejectButton() {
    MessageDialogue.stopButtonWarning(this);
  }

  /**
   * ControlP5 callback method.
   * @param event
   */
  public void controlEvent(controlP5.ControlEvent event) {
    if(debug) {
      println("ControlEvent: " + event.getName());
    }

    switch(event.getName()) {
    case "seekTo":
      ((controlP5.Textfield)event.getController()).setFocus(false);

      break;
    }
  }

  /**
   * ControlP5 callback method.
   * @param index
   */
  public void colorList(int index) {
    listVal = index - 1;

    String label = listVal > -1 ?
      String.format("%06x",colors.get(listVal)) :
      UIControl.listLbl
    ;

    if(debug) {
      println(listVal + " " + label);
    }

    uic.updateColorLabel(label);
  }

  /**
   * ControlP5 callback method.
   * @param value
   */
  public void thresholdSlider(float value) {
    int type = uic.getThresholdType();

    uic.thresholdValueLabelToInt();

    if(debug) {
      println("threshold slider value: " + value);
    }

    //keep lower bounds and upper bounds from crossing
    switch(type) {
    case 1:
      threshold[2] = (int)max(threshold[2], value);

      break;

    case 2:
      threshold[1] = (int)min(threshold[1], value);

      break;

    case 3:
      threshold[4] = (int)max(threshold[4], value);

      break;

    case 4:
      threshold[3] = (int)min(threshold[3], value);

      break;

    default:
    }

    threshold[type] = (int)value;
  }

  /**
   * ControlP5 callback method.
   */
  public void pipToggle() {
    pip = !pip;

    uic.setZoomState(pip);

    updateFrameParams();
    updateExitCenter();
  }

  /**
   * Updates the frame offset and dimensions.
   */
  private void updateFrameParams() {
    if(pip) {
      frameDims = scaledDims(
        (float)movieDims[0] * (insetBox[2]-insetBox[0]),
        (float)movieDims[1] * (insetBox[3]-insetBox[1])
      );
      frameOffset[0] = (viewBounds[2]-viewBounds[0]-frameDims[0])/2 + viewBounds[0];
      frameOffset[1] = (viewBounds[3]-viewBounds[1]-frameDims[1])/2 + viewBounds[1];
    } else {
      frameOffset = unzoomedFrameOffset();
      frameDims = unzoomedFrameDims();
    }
  }

  /**
   * ControlP5 callback method.
   * @param value
   */
  public void thresholdRadios(int value) {
    uic.setThresholdValue(threshold[value]);
  }

  /**
   * ControlP5 callback method.
   * @param value
   */
  public void selectRadios(int value) {
    selectExit = value == 1;
    uic.updateSelectType(selectExit);
  }

  /**
   * ControlP5 callback method.
   * @param value
   */
  public void modeRadios(int value) {
    waggleMode = value == 1;

    if(waggleMode) {
      selectExit = false;
    }

    uic.updateEventType(waggleMode);
    tu.setEventType(waggleMode);
    bdu.setWaggleMode(waggleMode);
  }

  /**
   * ControlP5 callback method.
   * @param value
   */
  public void seek(float value) {
    if(!imgSequenceMode) {
      if(!isPlaying) {
        movie.play();
        movie.volume(0f);
      }

      movie.jump(value);
    } else {
      imgIndex = (int)(10f*value);
      stillFrame = null;
    }

    uic.setSeekTime(value);

    if(debug) {
      println("seek to: " + value + 's');
    }

    updateSettings(value);

    //update playback mode timestamp
    if(replay) {
      float tmpTime;
      int i = 0, start = 0, stop = allFrameTimes.size() - 1;

      while(start <= stop) {
        i = (stop+start)/2;
        tmpTime = allFrameTimes.get(i);

        if(tmpTime - value > 0.000001f) {
          stop = i - 1;
        } else if(value - tmpTime > 0.000001f) {
          start = i + 1;
        } else {
          break;
        }
      }

      //if no match found, get next largest frame time
      if(start > stop && value - allFrameTimes.get(i) > 0.000001f) {
        i++;
      }

      timeStampIndex = i;
    }
  }

  /**
   * ControlP5 callback method.
   */
  public void recordButton() {
    record = !record;
    uic.setStatusLabel((isPlaying ? (record ? 2 : 3) : 0));
    uic.setRecordState(record);
  }

  /**
   * ControlP5 callback method.
   */
  public void openButton() {
    VideoBrowser.getVideoFile(this, currentFile);
  }

  /**
   * ControlP5 callback method.
   */
  public void openButton2() {
    VideoBrowser.getImageSequence(this, currentFile);
  }

  /**
   * Handles all operations necessary for stopping video playback.
   */
  void stopPlayback() {
    saveSettings(
      System.getProperty("user.dir") + File.separatorChar +
      "output" + File.separatorChar +
      videoName + File.separatorChar +
      "settings.json"
    );

    //begin critical section
    try {
      sem.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace(System.err);

      Thread.currentThread().interrupt();
    }

    if(pip) {
      uic.setZoomState(!pip);
    }

    if(imgSequenceMode) {
      imgSequence = null;
      imgNames = null;
      stillFrame = null;
      imgIndex = -1;
    } else if(movie != null){
      movie.stop();
      movie = null;
    }

    time = duration = -1f;

    movieDims = null;
    videoName = null;

    allFrameTimes = null;
    allFramePoints = null;
    timeStampIndex = -1;

    settingIndex = 0;
    settingsTimeStamps = null;

    //end critical section
    sem.release();

    record = false;
    uic.setStatusLabel(0);
    uic.setRecordState(false);

    isPlaying = false;
    uic.setPlayState(false);

    uic.toggleMenuStates();
    uic.setSetupGroupVisibility(false);
    uic.setOpenButtonVisibility(true);
    uic.setPlayVisibility(false);
    uic.setThresholdVisibility(false);
    uic.setThresholdType(0);
    uic.setSeekTime(0f);
    thresholdRadios(0);

    msgs.clear();

    System.out.append("footage closed\n------\n").flush();

    tu.init();
  }

  /**
   * Displays an error message dialogue before exiting the program.
   * @param msg a description of the cause of the crash
   */
  public void crash(String msg) {
    MessageDialogue.crashMessage(this, msg);
  }

  /**
   * Overrides from PApplet.
   */
  @Override
  public void exit() {
    try {
      sem.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      e.printStackTrace(System.err);
    }

    if(movie != null || imgSequence != null) {
      if(movie != null) {
        movie.stop();
        movie = null;
      }

      if(debug) {
        print("saving settings");
      }

      //save current color and selection settings
      saveSettings(
        System.getProperty("user.dir") + File.separatorChar +
        "output" + File.separatorChar +
        videoName + File.separatorChar +
        "settings.json"
      );

      if(debug) {
        println(" - done");
      }
    }

    if(log != null) {
      log.close();
    }

    sem.release();

    super.exit();
  }

  /**
   * Saves a settings file.
   * @param filePath the path to the file
   */
  private void saveSettings(String filePath) {
    JSONObject settings = new JSONObject();
    JSONObject setting = new JSONObject();
    JSONObject sets = new JSONObject();
    JSONObject set;
    int i;

    String lbl;
    for(i = 0; i < colors.size(); i++) {
      lbl = String.format("%06x", colors.get(i));
      setting.setString(Integer.toString(i), lbl);

      uic.removeListItem(lbl);
    }
    settings.setJSONObject("colors", setting);

    settings.setString("eventType", waggleMode ? "waggle" : "exit");

    for(float timeStamp : settingsTimeStamps) {
      set = new JSONObject();

      setting = new JSONObject();
      insetBox = insets.get(timeStamp);
      for(i = 0; i < insetBox.length; i++) {
        setting.setFloat(Integer.toString(i), insetBox[i]);
      }
      set.setJSONObject("insetBox", setting);

      setting = new JSONObject();
      exitRadial = radials.get(timeStamp);
      for(i = 0; i < exitRadial.length; i++) {
        setting.setFloat(Integer.toString(i), exitRadial[i]);
      }
      set.setJSONObject("exitRadial", setting);

      setting = new JSONObject();
      threshold = thresholds.get(timeStamp);
      for(i = 0; i < threshold.length; i++) {
        setting.setInt(thresholdKeys[i], threshold[i]);
      }
      set.setJSONObject("thresholds", setting);

      sets.setJSONObject(String.format("%.8f",timeStamp), set);
    }

    settings.setJSONObject("time", sets);

    saveJSONObject(settings, filePath);

    System.out.append("settings saved to ")
      .append(filePath)
      .append('\n')
      .flush();
  }

  /**
   * Overrides from PApplet.
   */
  @Override
  public void mousePressed() {
    if(debug) {
      println("mousePressed: (" + mouseX + ", " + mouseY + ")");
    }

    uic.closeMenus();

    if(
      !uic.mouseInsideLists() &&
      movieDims != null && !isPlaying &&
      mouseX > viewBounds[0] &&
      mouseX < viewBounds[2] &&
      mouseY > viewBounds[1] &&
      mouseY < viewBounds[3]
    ) {
      int[] mouse = constrainMousePosition(mouseX, mouseY);

      if(!selectExit) {
        if(!pip) {
          insetBox[0] = insetBox[2] =
            ((float)(mouse[0]-movieOffset[0]))/movieDims[0];
          insetBox[1] = insetBox[3] =
            ((float)(mouse[1]-movieOffset[1]))/movieDims[1];

          isDrag = true;
        }
      } else {
        if(!pip) {
          exitRadial[0] = ((float)(mouse[0]-movieOffset[0]))/movieDims[0];
          exitRadial[1] = ((float)(mouse[1]-movieOffset[1]))/movieDims[1];
        } else {
          exitRadial[0] = insetBox[0] + (
            (insetBox[2]-insetBox[0]) *
            (mouse[0]-frameOffset[0]) /
            frameDims[0]
          );
          exitRadial[1] = insetBox[1] + (
            (insetBox[3]-insetBox[1]) *
            (mouse[1]-frameOffset[1]) /
            frameDims[1]
          );
        }

        exitRadial[2] = exitRadial[3] = 0f;

        isDrag = true;
      }
    }
  }

  /**
   * Overrides from PApplet.
   */
  @Override
  public void mouseDragged() {
    if(isDrag) {
      int[] mouse = constrainMousePosition(mouseX, mouseY);

      if(!selectExit) {
        insetBox[2] = ((float)(mouse[0]-movieOffset[0]))/movieDims[0];
        insetBox[3] = ((float)(mouse[1]-movieOffset[1]))/movieDims[1];
      } else {
        float[] tmp = constrainRadius(mouse[0], mouse[1]);

        exitRadial[2] = tmp[0];
        exitRadial[3] = tmp[1];
      }
    }
  }

  /**
   * Constrains the selection circle within the view window.
   * @param mouseX the x-coordinate of the mouse
   * @param mouseY the y-coordinate of the mouse
   * @return a float array containing the normalized circle axes
   */
  private float[] constrainRadius(int mouseX, int mouseY) {
    int[] offset, dims;

    updateExitCenter();

    if(!pip) {
      offset = movieOffset;
      dims = movieDims;
    } else {
      offset = frameOffset;
      dims = frameDims;
    }

    float[] result = new float[2];
    //semi-major axis (x)
    result[0] = exitCenter[0] - mouseX;
    //semi-minor axis (y)
    result[1] = exitCenter[1] - mouseY;

    result[0] = result[1] = mag(result[0], result[1]);

    //constrain semi-major axes
    result[0] = constrain(
      result[0],
      0,
      min(exitCenter[0] - offset[0], dims[0] - exitCenter[0] + offset[0])
    );
    result[1] = constrain(
      result[1],
      0,
      min(exitCenter[1] - offset[1], dims[1] - exitCenter[1] + offset[1])
    );

    //choose smaller axis
    result[0] = result[1] = min(result[0], result[1]);

    //normalize axes
    if(!pip) {
      result[0] /= movieDims[0];
      result[1] /= movieDims[1];
    } else {
      result[0] /= frameDims[0]/(insetBox[2]-insetBox[0]);
      result[1] /= frameDims[1]/(insetBox[3]-insetBox[1]);
    }

    return result;
  }

  /**
   * Constrains the mouse coordinates.
   * @param mouseX the x-coordinate of the mouse
   * @param mouseY the y-coordinate of the mouse
   * @return an integer array containing the adjusted coordinates
   */
  private int[] constrainMousePosition(int mouseX, int mouseY) {
    int[] offset, dims;

    if(pip) {
      offset = frameOffset;
      dims = frameDims;
    } else {
      offset = movieOffset;
      dims = movieDims;
    }

    return new int[] {
      constrain(mouseX, offset[0],  offset[0]+dims[0]),
      constrain(mouseY, offset[1],  offset[1]+dims[1])
    };
  }

  /**
   * Overrides from PApplet.
   */
  @Override
  public void mouseReleased() {
    if(debug) {
      println("mouseReleased: (" + mouseX + ", " + mouseY + ")");
    }

    if(isDrag) {
      if(!selectExit) {
        //selection box has area of zero
        if((
          insetBox[0] - insetBox[2] <= 0.000001f &&
          insetBox[0] - insetBox[2] >= -0.000001f
        ) ||
        (
          insetBox[1] - insetBox[3] <= 0.000001f &&
          insetBox[1] - insetBox[3] >= -0.000001f
        )) {
          insetBox[0] = insetBox[1] = 0f;
          insetBox[2] = insetBox[3] = 1f;
        } else {
          float tmp;

          //ensure coordinates are in ascending order
          if(insetBox[0] > insetBox[2]) {
            tmp = insetBox[0];
            insetBox[0] = insetBox[2];
            insetBox[2] = tmp;
          }

          if(insetBox[1] > insetBox[3]) {
            tmp = insetBox[1];
            insetBox[1] = insetBox[3];
            insetBox[3] = tmp;
          }

          updateFrameParams();
        }
      }

      isDrag = false;

      if(debug) {
        println(
          "inset: " +
          insetBox[0] + ", " + insetBox[1] + ", " +
          insetBox[2] + ", " + insetBox[3] +
          "\nexit: " +
          exitRadial[0] + ", " + exitRadial[1] + ", " +
          exitRadial[2] + ", " + exitRadial[3]
        );
      }
    }
  }

  /**
   * Calculates coordinate offsets for the unzoomed inset frame.
   * @return the an integer array containing the coordinate offsets
   */
  private int[] unzoomedFrameOffset() {
    int[] result = new int[2];

    result[0] = (int)(movieDims[0]*insetBox[0]) + movieOffset[0];
    result[1] = (int)(movieDims[1]*insetBox[1]) + movieOffset[1];

    return result;
  }

  /**
   * Calculates the scaled dimensions of the unzoomed inset frame.
   * @return an integer array containing the dimensions
   */
  private int[] unzoomedFrameDims() {
    int[] result = new int[2];

    result[0] = (int)(movieDims[0] * (insetBox[2]-insetBox[0]));
    result[1] = (int)(movieDims[1] * (insetBox[3]-insetBox[1]));

    return result;
  }

  /**
   * Determines the dimensions of a PImage, scaled to fit within the display
   *   window.
   * @param imgWidth the width to scale
   * @param imgHeight the height to scale
   * @return an integer array containing the scaled dimensions
   */
  private int[] scaledDims(float imgWidth, float imgHeight) {
    int[] result = new int[2];

    float ratio = imgWidth/imgHeight;

    //scale by width
    result[0] = viewBounds[2] - viewBounds[0] + 1;
    result[1] = ceil(((float)result[0])/ratio);

    //scale by height
    int tmp = viewBounds[3] - viewBounds[1] + 1;
    if(result[1] > tmp) {
      result[1] = tmp;
      result[0] = ceil(((float)result[1])*ratio);
    }

    return result;
  }

  /**
   * Reads previously generated blob information associated with a video
   *   from a file.
   * @return true if successful
   */
  private boolean readFramePointsFromJSON() {
    boolean result = false;
    List<float[]> tmpList = new ArrayList<>(1);

    String path = System.getProperty("user.dir") + File.separatorChar +
      "output" + File.separatorChar + videoName + File.separatorChar +
      "points.json";

    if(debug) {
      println("attempting to read points from \"" + path + "\"...");
    }

    if((new File(path)).exists()) {
      try {
        JSONObject json = loadJSONObject(path);

        allFramePoints = new HashMap<>(json.size());
        allFrameTimes = new FloatList(json.size());

        HashMap<Integer, List<float[]>> colorMap;
        Iterator<?> jsonIter = json.keyIterator();
        Iterator<?> colorsIter, blobsIter;
        JSONObject jsonColors, jsonBlobs, jsonCoords;
        String keyString, tmpString;
        List<float[]> pointList;
        int color;
        float[] point;

        float timeStamp;
        while(jsonIter.hasNext()) {
          keyString = (String)jsonIter.next();
          timeStamp = Float.parseFloat(keyString);

          jsonColors = json.getJSONObject(keyString);

          colorMap = new HashMap<>();

          colorsIter = jsonColors.keyIterator();
          while(colorsIter.hasNext()) {
            tmpString = (String)colorsIter.next();
            color = (int)Long.parseLong(tmpString, 16);

            jsonBlobs = jsonColors.getJSONObject(tmpString);

            pointList = new ArrayList<>(jsonBlobs.size());

            blobsIter = jsonBlobs.keyIterator();
            while(blobsIter.hasNext()) {
              jsonCoords = jsonBlobs.getJSONObject((String) blobsIter.next());

              point = new float[2];
              point[0] = jsonCoords.getFloat("x");
              point[1] = jsonCoords.getFloat("y");

              pointList.add(point);
            }

            colorMap.put(color, pointList);
          }

          for(int tmp : colors) {
            colorMap.putIfAbsent(tmp, tmpList);
          }

          allFramePoints.put(timeStamp, colorMap);
          allFrameTimes.append(timeStamp);
        }

        allFrameTimes.sort();

        result = true;

        System.out.append("frame annotations loaded\n").flush();
      } catch(RuntimeException ex) {
        ex.printStackTrace(System.err);

        result = false;
      }
    }

    if(!result) {
      allFramePoints = new HashMap<>();
      allFrameTimes = new FloatList();

      if(debug) {
        println("failure");
      }
    } else if(debug) {
      println("done");
    }

    timeStampIndex = 0;

    centroids = new HashMap<>();
    for(int tmp : colors) {
      centroids.put(tmp, tmpList);
    }

    return result;
  }

  /**
   * Writes the generated blob information to a file.
   */
  void writeFramePointsToJSON() {
    JSONObject json = new JSONObject();
    JSONObject jsonCoords, jsonBlobs, jsonColors;

    HashMap<Integer, List<float[]>> colorMap;
    List<float[]> points;
    int i;
    float[] point;

    allFrameTimes.sort();

    for(Float timeStamp : allFrameTimes) {
      colorMap = allFramePoints.get(timeStamp);

      jsonColors = new JSONObject();

      for(int color : colors) {
        points = colorMap.get(color);

        jsonBlobs = new JSONObject();

        for(i = 0; i < points.size(); i++) {
          point = points.get(i);

          jsonCoords = new JSONObject();
          jsonCoords.setFloat("x", point[0]);
          jsonCoords.setFloat("y", point[1]);

          jsonBlobs.setJSONObject(Integer.toString(i), jsonCoords);
        }

        if(points.size() > 0) {
          jsonColors.setJSONObject(String.format("%06x", color), jsonBlobs);
        }
      }

      if(jsonColors.size() > 0) {
        json.setJSONObject(String.format("%.7f", timeStamp), jsonColors);
      }
    }

    File dir = new File(System.getProperty("user.dir") +
      File.separatorChar + "output" + File.separatorChar + videoName);
    dir.mkdir();

    saveJSONObject(json, dir.getAbsolutePath() + File.separatorChar +
      "points.json");
  }

  /**
   * Performs pre-load operations
   * @param file the selected File object
   */
  private void preLoad(File file) {
    if(debug) {
      println("selected File object: " + file.getName());
    }

    currentFile = file;

    System.out.append("toggling UI elements\n").flush();

    uic.setSetupGroupVisibility(true);
    uic.setOpenButtonVisibility(false);
    uic.setPlayVisibility(true);
    uic.setThresholdVisibility(true);
  }

  /**
   * Loads the selected video file.
   * @param file a File object representing the video to load
   */
  public void loadVideo(final File file) {
    if(file != null) {
      final PApplet self = this;

      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          imgSequenceMode = false;

          preLoad(file);

          String[] nameParts = file.getName().split("\\.");
          StringBuilder builder = new StringBuilder(nameParts[0]);
          for(int i = 1; i < nameParts.length - 1; i++) {
            builder.append('.').append(nameParts[i]);
          }
          videoName = builder.toString();

          loadSettings(
            System.getProperty("user.dir") + File.separatorChar +
            "output" + File.separatorChar +
            videoName + File.separatorChar +
            "settings.json"
          );

          movie = new Movie(self, file.getAbsolutePath());

          System.out.append("video loaded\n").flush();

          movie.play();
          movie.volume(0f);

          postLoad();
        }
      });
    } else {
      System.out.append("file selection canceled\n").flush();
    }
  }

  /**
   * Performs post-load operations.
   */
  private void postLoad() {
    isPlaying = false;

    System.out.append("reading frame annotations... ").flush();

    replay = readFramePointsFromJSON();
    replayCheckForTimeOut = false;
    uic.setRecordVisibility(!replay);
    uic.setRecordState(replay);
    uic.setPlayState(false);
    uic.toggleMenuStates();

    System.out.append(replay ? "success" : "failure").append('\n').flush();
  }

  /**
   * Sets the starting time stamp of the video.
   * @param date a Calendar object representing the new time stamp
   */
  void setTime(Calendar date) {
    videoDate = date;

    if(debug) {
      println("video time stamp: " + date.getTime());
    }
  }

  /**
   * Handles all operations necessary for restarting video playback.
   */
  void rewindVideo() {
    System.out.append("rewinding video\n").flush();

    if(debug) {
      println("rewinding video... ");
    }

    seek(0f);

    if(debug) {
      print("rewind complete\ntoggling item visibility... ");
    }

    replayCheckForTimeOut = false;

    uic.setSetupGroupVisibility(!isPlaying);
    uic.setPlayState(isPlaying);
    uic.setThresholdVisibility(!isPlaying);

    if(debug) {
      println("done");
    }

    if(!allFramePoints.isEmpty()) {
      replay = true;
      uic.setRecordVisibility(!replay);
      uic.setRecordState(replay);
      uic.setStatusLabel(0);

      timeStampIndex = 0;

      centroids = new HashMap<>();
      List<float[]> tmpList = new ArrayList<>(1);
      for(int tmp : colors) {
        centroids.put(tmp, tmpList);
      }

      if(debug) {
        println("replay mode active");
      }
    }
  }

  /**
   * ControlP5 callback method.
   */
  public void addSetting() {
    //current timestamp doesn't already have settings
    if(!settingsTimeStamps.hasValue(time)) {
      //duplicate current settings
      float[] newExit = new float[exitRadial.length],
        newBox = new float[exitRadial.length];
      int[] newThreshold = new int[threshold.length];
      short i;

      for(i = 0; i < exitRadial.length; i++) {
        newExit[i] = exitRadial[i];
        newBox[i] = insetBox[i];
      }

      for(i = 0; i < threshold.length; i++) {
        newThreshold[i] = threshold[i];
      }

      //add new settings to appropriate data structures
      insets.put(time, newBox);
      radials.put(time, newExit);
      thresholds.put(time, newThreshold);

      settingsTimeStamps.append(time);
      settingsTimeStamps.sort();

      updateSettings(time);
    }
  }

  /**
   * Finds the settings index corresponding to the specified time stamp.
   * @param seek the time stamp
   * @return the index
   */
  private void updateSettings(float seek) {
    int i = 0, start = 0, stop = settingsTimeStamps.size() - 1;
    float settingTime;

    //binary search to find current timestamp setting
    while(start <= stop) {
      i = (stop+start)/2;
      settingTime = settingsTimeStamps.get(i);

      if(settingTime - seek > 0.000001f) {
        stop = i - 1;
      } else if(seek - settingTime > 0.000001f) {
        start = i + 1;
      } else {
        break;
      }
    }

    //if no match found, get next smallest setting time
    if(i > 0 && start > stop && settingsTimeStamps.get(i) - seek > 0.000001f) {
      i--;
    }

    settingIndex = i;

    float settingsStamp = settingsTimeStamps.get(i);

    threshold = thresholds.get(settingsStamp);

    uic.setThresholdValue(threshold[uic.getThresholdType()]);

    exitRadial = radials.get(settingsStamp);
    insetBox = insets.get(settingsStamp);

    updateFrameParams();
    updateExitCenter();
  }

  /**
   * ControlP5 callback method.
   */
  public void removeSetting() {
    //more than one settings saved
    if(settingsTimeStamps.size() > 1) {
      float timeStamp;

      //find timestamp of current settings
      int index = settingIndex;

      //replace current settings with next settings
      //remove next settings
      if(settingIndex == 0) {
        timeStamp = settingsTimeStamps.get(index);
        float tmp = settingsTimeStamps.remove(index + 1);

        radials.replace(timeStamp, radials.remove(tmp));
        insets.replace(timeStamp, insets.remove(tmp));
        thresholds.replace(timeStamp, thresholds.remove(tmp));
      } else {  //remove current settings
        timeStamp = settingsTimeStamps.remove(index);

        radials.remove(timeStamp);
        insets.remove(timeStamp);
        thresholds.remove(timeStamp);
      }

      updateSettings(time);
    }
  }

  /**
   * ControlP5 callback method.
   */
  public void eventsButton() {
    if(eventDialog != null) {
      eventDialog.setVisible(false);
      eventDialog.dispose();
      eventDialog = null;
    }

    PGraphics graphic = tu.getEventTimeline(time, duration);
    if(graphic != null) {
      graphic.beginDraw();
      graphic.fill(0xff000000);
      graphic.textAlign(RIGHT);
      graphic.text(
        String.format(
          "current time: %02d:%02d:%02d",
          ((int)time)/3600,
          (((int)time)/60)%60,
          ((int)time)%60
        ),
        395,
        18
      );
      graphic.endDraw();

      MessageDialogue.showEventTimeline(this, graphic.get());
    }
  }

  /**
   * Sets the event timeline dialog.
   * @param dialog the new event timeline dialog
   */
  public void setEventDialog(JDialog dialog) {
    eventDialog = dialog;
  }

  /**
   * ControlP5 callback method.
   */
  public void programMenuButton() {
    uic.setProgramMenuState(!uic.isProgramMenuOpen());
  }

  /**
   * ControlP5 callback method.
   */
  public void footageMenuButton() {
    uic.setFootageMenuOpen(!uic.isFootageMenuOpen());
  }

  /**
   * ControlP5 callback method.
   */
  public void optionMenuButton() {
    uic.setOptionMenuOpen(!uic.isOptionMenuOpen());
  }

  /**
   * ControlP5 callback method.
   */
  public void programMenu() {
    exit();
  }

  /**
   * ControlP5 callback method.
   * @param index
   */
  public void footageMenu(int index) {
    if(uic.checkMenuItemState("footage", index)) {
      switch(index) {
      case 0:
        openButton();
        break;

      case 1:
        openButton2();
        break;

      case 3:
        playButton();
        break;

      case 5:
        ejectButton();
        break;

      default:
        //do nothing
      }
    }
  }

  /**
   * ControlP5 callback method.
   * @param index
   */
  public void optionMenu(int index) {
    if(uic.checkMenuItemState("option", index)) {
      switch(index) {
      case 0:
        recordButton();
        break;

      case 1:
        pipToggle();
        break;

      case 3:
      case 4:
        uic.toggleEventRadio(index-3);
        break;

      case 6:
      case 7:
        uic.toggleSelectRadio(index-6);
        break;

      case 9:
        addSetting();
        break;

      case 10:
        removeSetting();
        break;

      default:
        //do nothing
      }
    }
  }

  /**
   * Main method for executing BeeTracker as a Java application.
   * @param args command line arguments
   */
  public static void main(String[] args) {
    PApplet.main(new String[] { beetracker.BeeTracker.class.getName() });
  }

  /**
   * Specifies the expected frame rate for image sequence mode.
   * @param fps the frame rate
   */
  void setFPS(int fps) {
    duration = ((float)imgSequence.length-1)/fps;
    this.fps = fps;

    uic.setSeekRange(duration);
  }

  /**
   * @return true if currently in image sequence mode
  */
  public boolean isImgSequenceMode() {
    return imgSequenceMode;
  }

  /**
   * @return true if zoom is active
   */
  public boolean isZoomed() {
    return pip;
  }

  /**
   * Adds an event to the notification queue.
   * @param eventType
   * @param eventTime
   */
  void registerEvent(String eventType, float eventTime) {
    int tmp = (int)(eventTime*100f);
    msgs.put(
      time + 5f,
      String.format(
        "%02d:%02d:%02d.%02d, %s",
        (tmp/6000)/60,
        tmp/6000,
        (tmp/100)%60,
        tmp%100,
        eventType
      )
    );
  }

  /**
   * Removes an event from the notification queue.
   * @param eventTime the time stamp of the event to remove
   */
  void removeEvent(float eventTime) {
    msgs.remove(eventTime + 5f);
  }

  /**
   * ControlP5 callback method.
   * @param value
   */
  public void seekTo(String value) {
    Float result;

    try {
      String[] args = value.split(":", 3);
      result = Float.parseFloat(args[args.length-1]);

      for(int i = 0; i < args.length-1; i++) {
        result += Integer.parseInt(args[i])*pow(60,args.length-1-i);
      }
    } catch(NumberFormatException e) {
      result = null;
    }

    if(result != null && duration > 0f) {
      seek(constrain(result, 0f, duration));
    }
  }

  /**
   * Overrides from PApplet
   */
  @Override
  public void keyPressed() {
    if(duration > 0f) {
      if(!uic.isSeekToFocused() && key == ' ') {
        playButton();
      }
    }
  }
}
