/**
 * @file BeeTracker.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description A tool for tracking bees in a video.
 */

package beetracker;

import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import controlP5.ControlEvent;

import processing.core.PApplet;
import processing.core.PImage;
import processing.data.FloatList;
import processing.data.IntDict;
import processing.data.JSONObject;
import processing.video.Movie;

/**
 *
 * @author Kay Choi
 */
@SuppressWarnings("serial")
public class BeeTracker extends PApplet {
    private static final int[] viewBounds = {50, 50, 749, 549};
    private static final String months[] = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    private static final String[] thresholdKeys = {"hue", "sat", "val"};

    private processing.data.IntList colors;
    private int[] movieDims = null, movieOffset, frameDims, frameOffset;
    private float[] exitCenter;

    private final java.util.concurrent.Semaphore sem =
        new java.util.concurrent.Semaphore(1, true);

    private boolean isPlaying = false;
    private boolean record = false, replay = false;
    private boolean pip = false, selectExit = false;
    private int listVal = -1;

    private IntDict crossingCounts;

    private File currentFile = null;

    private float[] insetBox, exitRadial;
    private boolean isDrag = false;

    private Movie movie = null;

    private controlP5.ControlP5 cp5;
    private UIControl uic;
    private BlobDetectionUtils bdu;
    private TrackingUtils tu;

    private PrintStream log = null;

    private final PImage titleImg = loadImage("data/img/title.png");

    private static final boolean debug = true;

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

    /**
     * Overrides from PApplet.
     */
    @Override
    public void setup() {
        size(800, 600);
        frameRate(60);
        strokeWeight(1);

        File outputDir = new File(System.getProperty("user.dir") +
            File.separatorChar + "output");
        outputDir.mkdir();

        cp5 = new controlP5.ControlP5(this);

        textFont(cp5.getFont().getFont());

        if(frame != null) {
            frame.setIconImage((java.awt.Image)loadImage("data/img/icon.png").getNative());
            frame.setTitle("BeeTracker");
            frame.addWindowListener(new java.awt.event.WindowListener() {
                @Override
                public void windowActivated(WindowEvent arg0) {}

                @Override
                public void windowClosed(WindowEvent arg0) {}

                @Override
                public void windowClosing(WindowEvent arg0) {
                    exit();
                }

                @Override
                public void windowDeactivated(WindowEvent arg0) {}

                @Override
                public void windowDeiconified(WindowEvent arg0) {}

                @Override
                public void windowIconified(WindowEvent arg0) {}

                @Override
                public void windowOpened(WindowEvent arg0) {}
            });
        }

        //create log file
        try {
            log = new PrintStream(new File("Console.log"), "UTF-8");
        } catch (java.io.FileNotFoundException |
            java.io.UnsupportedEncodingException ex)
        {
            java.util.logging.Logger.getLogger(BeeTracker.class.getName())
                .log(java.util.logging.Level.SEVERE, null, ex);
            crash(ex.toString());
        }

        uic = new UIControl(this, cp5);

        bdu = new BlobDetectionUtils(this, width/2, height/2, debug);

        tu = new TrackingUtils(debug);

        movieOffset = new int[2];
        frameDims = new int[2];
        frameOffset = new int[2];

        exitCenter = new float[2];
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

        log.append("initializing settings... ").flush();

        if(!settingsErrors[0]) {
            try {
                JSONObject jsonSettings = loadJSONObject(settings.getAbsolutePath());

                int tmp;
                String jsonKey, timeKey;
                JSONObject jsonSetting, timeSetting, setting;
                Iterator<?> jsonIter, settingIter;

                log.append("reading from file - ").flush();

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

                    e1.printStackTrace(log);
                }

                thresholds = new HashMap<>();
                radials = new HashMap<>();
                insets = new HashMap<>();

                try {
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

                            e2.printStackTrace(log);
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

                            e3.printStackTrace(log);
                        } finally {
                            radials.put(timeStamp, exitRadial);
                        }

                        //set thresholds
                        threshold = new int[3];
                        try {

                            setting = timeSetting.getJSONObject("thresholds");

                            for(tmp = 0; tmp < thresholdKeys.length; tmp++) {
                                threshold[tmp] = setting.getInt(thresholdKeys[tmp]);
                            }
                        } catch(Exception e4) {
                            threshold[0] = 40;
                            threshold[1] = 90;
                            threshold[2] = 20;

                            e4.printStackTrace(log);
                        } finally {
                            thresholds.put(timeStamp, threshold);

                            uic.setThresholdValue(threshold[uic.getThresholdType()]);
                        }
                    }
                } catch(RuntimeException e5) {
                    settingsErrors[2] = true;

                    e5.printStackTrace(log);
                }
            } catch(RuntimeException ex) {
                ex.printStackTrace(log);
            }
        }

        else {
            log.append("file not found - ").flush();
        }

        if(settingsErrors[0] || settingsErrors[1]) {
            colors.clear();
        }

        if(settingsErrors[0] || settingsErrors[2]) {
            settingsTimeStamps.clear();
            settingsTimeStamps.append(0f);

            thresholds = new HashMap<>();
            threshold = new int[3];
            threshold[0] = 40;
            threshold[1] = 90;
            threshold[2] = 20;
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

        log.append("done\n").flush();
    }

    /**
     * Overrides from PApplet.
     */
    @Override
    public void draw() {
        background(0x222222);

        textAlign(CENTER, CENTER);
        textSize(24);

        fill(0xff444444);
        rectMode(CORNERS);
        rect(viewBounds[0]-1, viewBounds[1]-1, viewBounds[2]+1, viewBounds[3]+1);

        //begin critical section
        try {
            sem.acquire();
        } catch (InterruptedException ex) {
            ex.printStackTrace(log);

            Thread.currentThread().interrupt();
        }

        if(movie != null) {
            float time = movie.time();

            if(movie.available()) {
                movie.read();

                if(!isPlaying) {
                    movie.pause();
                    movie.jump(uic.getSeekTime());
                }

                else {
                    uic.setSeekTime(time);
                }
            }

            if(movieDims != null) {
                processing.core.PGraphics viewFrame = createGraphics(
                    viewBounds[2] - viewBounds[1] + 1,
                    viewBounds[3] - viewBounds[1] + 1
                );
                viewFrame.beginDraw();
                viewFrame.copy(
                    movie,
                    0,
                    0,
                    movie.width,
                    movie.height,
                    (viewFrame.width-movieDims[0])/2,
                    (viewFrame.height-movieDims[1])/2,
                    movieDims[0],
                    movieDims[1]
                );

                PImage insetFrame = copyInsetFrame();

                if(insetFrame != null) {
                    viewFrame.noSmooth();

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
                        }
                    }

                    if(replay) {
                        if(timeStampIndex >= 0 &&
                            timeStampIndex < allFrameTimes.size())
                        {
                            timeStamp = allFrameTimes.get(timeStampIndex);

                            if(time - timeStamp > 0.000001f) {
                                if(debug) {
                                    println("frame info for " + timeStamp +
                                        "s, actual time " + time + 's');
                                }

                                replayCheckForTimeOut = true;

                                centroids = allFramePoints.get(timeStamp);

                                timeStampIndex++;
                            }

                            //clear point data after 1s
                            else if(
                                replayCheckForTimeOut &&
                                timeStampIndex > 0 &&
                                time - allFrameTimes.get(timeStampIndex-1) > 1f
                            ) {
                                replayCheckForTimeOut = false;

                                centroids = new HashMap<>(colors.size());

                                for(int color : colors) {
                                    centroids.put(color, new ArrayList<float[]>(1));
                                }
                            }
                        }
                    }

                    else {
                        //BlobDetection expects certain image size
                        insetFrame.resize(bdu.getImageWidth(), bdu.getImageHeight());
                        bdu.filterImg(insetFrame, colors, threshold);

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
                                println(
                                    "---------BEGIN FRAME (" +
                                    String.format("%.2f", time) +
                                    "s)---------\npip: " + pip
                                );
                            }

                            IntDict counts = tu.trackCentroids(
                                centroids,
                                frameDims, frameOffset,
                                exitRadial,
                                movieDims, movieOffset,
                                time
                            );

                            crossingCounts.add("departures", counts.get("departures"));
                            crossingCounts.add("arrivals", counts.get("arrivals"));
                        }
                    }

                    int[] insetOffset = new int[2];

                    //zoomed
                    if(pip) {
                        PImage zoomedInset = copyInsetFrame();

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
                    }

                    else {
                        insetOffset[0] = (int)(insetBox[0]*movieDims[0]) +
                            movieOffset[0] - viewBounds[0];
                        insetOffset[1] = (int)(insetBox[1]*movieDims[1]) +
                            movieOffset[1] - viewBounds[1];
                    }

                    if(!replay) {
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
                    }

                    //draw detected blobs
                    if(!replay) {
                        bdu.drawBlobs(viewFrame, viewBounds,
                            frameDims, frameOffset, exitCenter);
                    }

                    //mark bees
                    else {
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
                                        .02f*frameDims[1],
                                        .02f*frameDims[1]
                                    );
                                }
                            }
                        }
                    }
                }

                viewFrame.endDraw();

                imageMode(CORNER);
                image(viewFrame, viewBounds[0], viewBounds[1]);

                //bee count
                if(isPlaying && (record || replay)) {
                    rectMode(CENTER);
                    noStroke();
                    fill(0xff02344d);
                    rect(535, 25, 430, 40);

                    fill(0xffffffff);
                    text(
                        "total #arrivals: " + crossingCounts.get("arrivals") +
                        ", total #departures: " + crossingCounts.get("departures"),
                        534,
                        25
                    );

                    if(record && debug) {
                        println("---------END FRAME---------");
                    }
                }

                stroke(0xffffa600);
                ellipseMode(RADIUS);
                noFill();
                rectMode(CORNERS);

                //unzoomed
                if(!pip) {
                    //inset box
                    rect(
                        insetBox[0]*movieDims[0] + movieOffset[0],
                        insetBox[1]*movieDims[1] + movieOffset[1],
                        insetBox[2]*movieDims[0] + movieOffset[0] - 1,
                        insetBox[3]*movieDims[1] + movieOffset[1] - 1
                    );

                    //exit circle
                    ellipse(
                        exitRadial[0]*movieDims[0] + movieOffset[0],
                        exitRadial[1]*movieDims[1] + movieOffset[1],
                        exitRadial[2]*movieDims[0],
                        exitRadial[3]*movieDims[1]
                    );
                }

                //zoomed
                else {
                    //inset box
                    rect(
                        frameOffset[0],
                        frameOffset[1],
                        frameDims[0] - 1 + frameOffset[0],
                        frameDims[1] - 1 + frameOffset[1]
                    );

                    //exit circle
                    ellipse(
                        exitCenter[0],
                        exitCenter[1],
                        exitRadial[2]*frameDims[0]/(insetBox[2]-insetBox[0]),
                        exitRadial[3]*frameDims[1]/(insetBox[3]-insetBox[1])
                    );
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
                }

                else {
                    int[] tmpDims;

                    if(pip) {
                        tmpDims = new int[2];
                        tmpDims[0] = (int)(frameDims[0]/(insetBox[2]-insetBox[0]));
                        tmpDims[1] = (int)(frameDims[1]/(insetBox[3]-insetBox[1]));
                    }

                    else {
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
                if(isPlaying && movie.duration() - time < 1f/movie.frameRate) {
                    isPlaying = false;

                    StringBuilder msg = new StringBuilder("End of video reached.\n\n");

                    Calendar date = Calendar.getInstance();
                    HashMap<Float, String> formattedTime = new HashMap<>();
                    HashMap<Float, String> summary = tu.getSummary();
                    FloatList timeStamps = new FloatList(summary.size());

                    if(allFramePoints.isEmpty()) {
                        msg.append("No points saved!")
                            .append(" Enable recording to generate events.\n");
                    }

                    else {
                        msg.append("Summary of events: \n");

                        for(Float timeStamp : summary.keySet()) {
                            timeStamps.append(timeStamp);
                        }
                        timeStamps.sort();

                        StringBuilder builder;

                        int sec;
                        for(float timeStamp : timeStamps) {
                            sec = (int)timeStamp;
                            date.setTime(videoDate.getTime());
                            date.add(Calendar.SECOND, sec);

                            builder = new StringBuilder();
                            builder.append(months[date.get(Calendar.MONTH)])
                                .append(' ')
                                .append(Integer.toString(date.get(Calendar.DATE)))
                                .append(' ')
                                .append(Integer.toString(date.get(Calendar.YEAR)))
                                .append(',')
                                .append(String.format(
                                    "%02d:%02d:%.5f",
                                    date.get(Calendar.HOUR_OF_DAY),
                                    date.get(Calendar.MINUTE),
                                    (date.get(Calendar.SECOND)-sec)+timeStamp
                                ));

                            formattedTime.put(timeStamp, builder.toString());

                            msg.append(timeStamp)
                                .append(": ")
                                .append(summary.get(timeStamp))
                                .append('\n');
                        }
                    }

                    if(debug) {
                        println('\n' + msg.toString());
                    }

                    MessageDialogue.endVideoMessage(
                        this,
                        msg.toString(),
                        saveSummaryResults(
                            date,
                            timeStamps,
                            formattedTime,
                            summary
                        )
                    );
                }

                cp5.draw();

                //mark settings time stamps
                textAlign(LEFT, TOP);
                textSize(10);
                fill(0xffffffff);
                for(float stamp: settingsTimeStamps) {
                    text(
                        "l",
                        stamp/movie.duration()*(uic.getSeekBarWidth()-5) +
                            (uic.getSeekBarPosition().x+2),
                        uic.getSeekBarPosition().y + 5
                    );
                }
            }

            else {
                text("Loading...", width*.5f, height*.5f);

                cp5.draw();

                if(movie.height > 0) {
                    uic.setSeekRange(movie.duration());

                    movieDims = scaledDims(
                        movie.width,
                        movie.height
                    );

                    movieOffset[0] = (int)((width-movieDims[0])*.5f);
                    movieOffset[1] = (int)((height-movieDims[1])*.5f);

                    frameOffset[0] = (int)(movieDims[0]*insetBox[0]) + movieOffset[0];
                    frameOffset[1] = (int)(movieDims[1]*insetBox[1]) + movieOffset[1];

                    frameDims[0] = (int)(movieDims[0] * (insetBox[2]-insetBox[0]));
                    frameDims[1] = (int)(movieDims[1] * (insetBox[3]-insetBox[1]));

                    updateExitCenter();
                }
            }
        }

        else {
            imageMode(CENTER);
            image(titleImg, .5f*width, .5f*height-50);

            cp5.draw();
        }

        //end critical section
        sem.release();

        //main window border
        stroke(0xffffffff);
        noFill();
        rectMode(CORNERS);
        rect(viewBounds[0]-1, viewBounds[1]-1, viewBounds[2]+1, viewBounds[3]+1);
    }

    /**
     * Updates the coordinates of the exit center on the screen.
     */
    private void updateExitCenter() {
        if(!pip) {
            exitCenter[0] = exitRadial[0]*movieDims[0] + movieOffset[0];
            exitCenter[1] = exitRadial[1]*movieDims[1] + movieOffset[1];
        }

        else {
            exitCenter[0] = (exitRadial[0]-insetBox[0])*frameDims[0]/
                (insetBox[2]-insetBox[0]) + frameOffset[0];
            exitCenter[1] = (exitRadial[1]-insetBox[1])*frameDims[1]/
                (insetBox[3]-insetBox[1]) + frameOffset[1];
        }
    }

    /**
     * Saves the statistics of the current video to file.
     * @param date a Calendar object
     * @param timeStamps a sorted FloatList of time stamps
     * @param formattedTime a HashMap mapping float time stamps to formatted
     *   time stamp strings
     * @param summary a HashMap mapping float time stamps to string event
     *   descriptions
     * @return the name of the new file in the format
     *   "<video filename>-dd.mmm.yyyy-hhmm.json"
     *   OR
     *   null if no points were recorded
     */
    private String saveSummaryResults(
        Calendar date,
        FloatList timeStamps,
        HashMap<Float, String> formattedTime,
        HashMap<Float, String> summary
    ) {
        String fileName = null;

        if(!allFramePoints.isEmpty()) {
            date.setTimeInMillis(System.currentTimeMillis());

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

                writer.append("date,time,color,type\n").flush();
                for(float timeStamp : timeStamps) {
                    writer.append(formattedTime.get(timeStamp))
                        .append(',')
                        .append(summary.get(timeStamp))
                        .append('\n')
                        .flush();
                }
            } catch (IOException e) {
                e.printStackTrace(log);
            } finally {
                if(writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace(log);
                    }
                }
            }
        }

        return fileName;
    }

    /**
     * Copies the inset frame for image processing and blob detection.
     */
    private PImage copyInsetFrame() {
        PImage result;

        //don't do anything until inset dimensions have stabilized
        if(isDrag && !selectExit) {
            result = null;
        }

        else {
            //for best results, copy source and destination should be same size
            result = createImage(
                (int)(movie.width*(insetBox[2] - insetBox[0])),
                (int)(movie.height*(insetBox[3] - insetBox[1])),
                ARGB
            );
            result.copy(
                movie,
                (int)(movie.width*insetBox[0]),
                (int)(movie.height*insetBox[1]),
                (int)(movie.width*(insetBox[2] - insetBox[0])),
                (int)(movie.height*(insetBox[3] - insetBox[1])),
                0,
                0,
                result.width,
                result.height
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
    void setColor(int newColor) {
        if(listVal < 0) {
            if(!colors.hasValue(newColor)) {
                String code = String.format("%06x", newColor);

                uic.addListItem(code);

                colors.append(newColor);
            }
        }

        else {
            colors.set(listVal, newColor);

            uic.setListItem(String.format("%06x", newColor), listVal);
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
        if(movie != null) {
            boolean status;

            if(!isPlaying) {
                boolean[] errors = {(colors.size() == 0), (exitRadial[2] <= 0f)};

                status = !(errors[0] || errors[1]);

                if(status) {
                    isPlaying = true;

                    tu.setColors(colors);

                    movie.play();

                    if(debug) {
                        print("starting playback...");
                    }
                }

                else {
                    MessageDialogue.playButtonErrorMessage(this, errors);

                    if(debug) {
                        println("error");
                    }
                }
            }

            else {
                isPlaying = false;
                status = true;

                if(debug) {
                    print("pausing playback...");
                }

                movie.pause();
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
     * ControlP5 callback method. Used for control group events.
     * @param event the originating event
     */
    public void controlEvent(ControlEvent event) {
        if(debug) {
            println("ControlEvent: " + event.getName());
        }

        switch(event.getName()) {
        case "colorList":
            listVal = (int)event.getValue();

            if(debug) {
                println(listVal + " " +
                    (
                        listVal > -1 ?
                        String.format("%06x",colors.get(listVal)) :
                        "new color"
                    )
                );
            }

            break;

        default: //do nothing
        }
    }

    /**
     * ControlP5 callback method.
     * @param value
     */
    public void thresholdSlider(float value) {
        int type = uic.getThresholdType();

        if(debug) {
            println("threshold slider value: " + value);
        }

        threshold[type] = (int)value;
    }

    /**
     * ControlP5 callback method.
     */
    public void pipToggle() {
        pip = !pip;

        if(pip) {
            frameDims = scaledDims(
                movieDims[0]*(insetBox[2] - insetBox[0]),
                movieDims[1]*(insetBox[3] - insetBox[1])
            );
            frameOffset[0] = (int)((width-frameDims[0])*.5f);
            frameOffset[1] = (int)((height-frameDims[1])*.5f);
        }

        else {
            frameOffset[0] = (int)(movieDims[0]*insetBox[0]) + movieOffset[0];
            frameOffset[1] = (int)(movieDims[1]*insetBox[1]) + movieOffset[1];

            frameDims[0] = (int)(movieDims[0] * (insetBox[2]-insetBox[0]));
            frameDims[1] = (int)(movieDims[1] * (insetBox[3]-insetBox[1]));
        }

        updateExitCenter();
    }

    /**
     * ControlP5 callback method.
     */
    public void selectToggle() {
        selectExit = !selectExit;
        uic.toggleSelectLbl();
    }

    /**
     * ControlP5 callback method.
     * @param value
     */
    public void radioButtons(int value) {
        uic.setThresholdValue(threshold[value]);
    }

    /**
     * ControlP5 callback method.
     * @param value
     */
    public void seek(float value) {
        if(!isPlaying) {
            movie.play();
        }

        if(debug) {
            println("seek to: " + value + 's');
        }

        updateSettings(value);

        //update playback mode timestamp
        if(replay) {
            float time;
            int i = 0, start = 0, stop = allFrameTimes.size() - 1;

            while(start <= stop) {
                i = (stop+start)/2;
                time = allFrameTimes.get(i);

                if(time - value > 0.000001f) {
                    stop = i - 1;
                }

                else if(value - time > 0.000001f) {
                    start = i + 1;
                }

                else {
                    break;
                }
            }

            //if no match found, get next largest frame time
            if(start > stop && value - allFrameTimes.get(i) > 0.000001f) {
                i++;
            }

            timeStampIndex = i;
        }

        movie.jump(value);
        uic.setSeekTime(value);
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
        VideoBrowser.getVideoFile(this, currentFile, log);
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
            e.printStackTrace(log);

            Thread.currentThread().interrupt();
        }

        movie.stop();
        movie = null;

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
        uic.setRecordState(false);

        isPlaying = false;
        uic.setPlayState(false);

        uic.setSetupGroupVisibility(false);
        uic.setOpenButtonVisibility(true);
        uic.setPlayVisibility(false);
        uic.setThresholdVisibility(false);
        uic.selectRadioButton(0);
        uic.setSeekTime(0f);
        radioButtons(0);

        log.append("video closed\n------\n").flush();

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
        if(log != null) {
            log.close();
        }

        if(movie != null) {
            movie.stop();
            movie = null;

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

        log.append("settings saved to ")
	    	.append(filePath)
	    	.append('\n')
	    	.flush();
    }

    /**
     * Overrides from PApplet.
     */
    @Override
    public void mousePressed() {
        if(
            movieDims != null && !isPlaying &&
            mouseX > (width-movieDims[0])/2 &&
            mouseX < (width+movieDims[0])/2 &&
            mouseY > (height-movieDims[1])/2 &&
            mouseY < (height+movieDims[1])/2
        ) {
            if(!selectExit) {
                if(!pip) {
                    insetBox[0] = insetBox[2] =
                        1f*(mouseX-movieOffset[0])/movieDims[0];
                    insetBox[1] = insetBox[3] =
                        1f*(mouseY-movieOffset[1])/movieDims[1];

                    isDrag = true;
                }
            }

            else {
                if(!pip) {
                    exitRadial[0] = 1f*(mouseX-movieOffset[0])/movieDims[0];
                    exitRadial[1] = 1f*(mouseY-movieOffset[1])/movieDims[1];
                }

                else {
                    exitRadial[0] = insetBox[0] + (
                        (insetBox[2]-insetBox[0]) *
                        (mouseX-frameOffset[0]) /
                        frameDims[0]
                    );
                    exitRadial[1] = insetBox[1] + (
                        (insetBox[3]-insetBox[1]) *
                        (mouseY-frameOffset[1]) /
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
            if(!selectExit) {
                int[] mouse = constrainMousePosition(mouseX, mouseY);

                insetBox[2] = 1f*(mouse[0]-movieOffset[0])/movieDims[0];
                insetBox[3] = 1f*(mouse[1]-movieOffset[1])/movieDims[1];
            }

            else {
                float[] tmp = constrainRadius(mouseX, mouseY);

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
        int[] mouse = constrainMousePosition(mouseX, mouseY);
        int[] offset, dims;

        updateExitCenter();

        if(!pip) {
            offset = movieOffset;
            dims = movieDims;
        }

        else {
            offset = frameOffset;
            dims = frameDims;
        }

        float[] result = new float[2];
        //semi-major axis (x)
        result[0] = exitCenter[0] - mouse[0];
        //semi-minor axis (y)
        result[1] = exitCenter[1] - mouse[1];

        result[0] = result[1] = (float)Math.pow((Math.pow(result[0], 2) +
            Math.pow(result[1], 2)), .5);

        //constrain semi-major axis (x)
        if(result[0] > exitCenter[0] - offset[0]) {
            result[0] = exitCenter[0] - offset[0];
        }

        if(result[0] > dims[0] - exitCenter[0] + offset[0]) {
            result[0] = dims[0] - exitCenter[0] + offset[0];
        }

        //constrain semi-minor axis (y)
        if(result[1] > exitCenter[1] - offset[1]) {
            result[1] = exitCenter[1] - offset[1];
        }

        if(result[1] > dims[1] - exitCenter[1] + offset[1]) {
            result[1] = dims[1] - exitCenter[1] + offset[1];
        }

        //choose smaller axis
        if(result[0] < result[1]) {
            result[1] = result[0];
        }

        else {
            result[0] = result[1];
        }

        //normalize axes
        if(!pip) {
            result[0] /= movieDims[0];
            result[1] /= movieDims[1];
        }

        else {
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
        int[] result = {mouseX, mouseY};
        int[] offset = (pip ? frameOffset : movieOffset);

        if(mouseX < offset[0]) {
            result[0] = offset[0];
        }

        else if (mouseX > width-offset[0]) {
            result[0] = width-offset[0];
        }

        if(mouseY < offset[1]) {
            result[1] = offset[1];
        }

        else if(mouseY > height-offset[1]) {
            result[1] = height-offset[1];
        }

        return result;
    }

    /**
     * Overrides from PApplet.
     */
    @Override
    public void mouseReleased() {
        if(isDrag) {
            if(!selectExit) {
                //selection box has area of zero
                if(
                    (
                        insetBox[0] - insetBox[2] <= 0.000001f &&
                        insetBox[0] - insetBox[2] >= -0.000001f
                    ) ||
                    (
                        insetBox[1] - insetBox[3] <= 0.000001f &&
                        insetBox[1] - insetBox[3] >= -0.000001f
                    )
                ) {
                    insetBox[0] = insetBox[1] = 0f;
                    insetBox[2] = insetBox[3] = 1f;
                }

                else {
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

                    frameOffset[0] = (int)(movieDims[0]*insetBox[0]) + movieOffset[0];
                    frameOffset[1] = (int)(movieDims[1]*insetBox[1]) + movieOffset[1];

                    frameDims[0] = (int)(movieDims[0] * (insetBox[2]-insetBox[0]));
                    frameDims[1] = (int)(movieDims[1] * (insetBox[3]-insetBox[1]));
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
        result[0] = width - 100;
        result[1] = (int)(result[0]/ratio);

        //scale by height
        if(result[1] > height - 100) {
            result[1] = height - 100;
            result[0] = (int)(result[1]*ratio);
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

                    colorMap = new HashMap<>(jsonColors.size());

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

                log.append("frame annotations loaded\n").flush();
            } catch(RuntimeException ex) {
                ex.printStackTrace(log);

                result = false;
            }
        }

        if(!result) {
            allFramePoints = new HashMap<>();
            allFrameTimes = new FloatList();

            if(debug) {
                println("failure");
            }
        }

        else if(debug) {
            println("done");
        }

        timeStampIndex = 0;

        centroids = new HashMap<>(colors.size());
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
     * Loads the selected video file.
     * @param file a File object representing the video to load
     */
    public void loadVideo(File file) {
        if(file != null) {
            currentFile = file;

            log.append("toggling UI elements\n").flush();

            uic.setSetupGroupVisibility(true);
            uic.setOpenButtonVisibility(false);
            uic.setPlayVisibility(true);
            uic.setThresholdVisibility(true);

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

            movie = new Movie(this, file.getAbsolutePath());

            log.append("video loaded\n").flush();

            movie.volume(0f);
            movie.play();
            isPlaying = false;

            crossingCounts = new IntDict(2);

            log.append("reading frame annotations... ").flush();

            replay = readFramePointsFromJSON();
            uic.setRecordVisibility(!replay);

            log.append(replay ? "success" : "failure").append('\n').flush();
        }

        else {
            log.append("file selection canceled\n").flush();
        }

        if(frame != null) {
            requestFocusInWindow();
        }
    }

    /**
     * Sets the starting time stamp of the video.
     * @param date a Calendar object representing the new time stamp
     */
    void setTime(Calendar date) {
        videoDate = date;
    }

    /**
     * Handles all operations necessary for restarting video playback.
     */
    void rewindVideo() {
        log.append("rewinding video\n").flush();

        if(debug) {
            print("rewinding video... ");
        }

        seek(0f);

        uic.setSetupGroupVisibility(!isPlaying);
        uic.setPlayState(isPlaying);
        uic.setThresholdVisibility(!isPlaying);

        if(!allFramePoints.isEmpty()) {
            replay = true;
            recordButton();
            uic.setRecordVisibility(false);

            timeStampIndex = 0;

            centroids = new HashMap<>(colors.size());
            List<float[]> tmpList = new ArrayList<>(1);
            for(int tmp : colors) {
                centroids.put(tmp, tmpList);
            }
        }

        if(debug) {
            println("done");
        }
    }

    /**
     * ControlP5 callback method.
     */
    public void addSetting() {
        float time = movie.time();

        //duplicate current settings
        float[] newExit = new float[4], newBox = new float[4];
        int[] newThreshold = new int[3];
        short i;

        for(i = 0; i < 4; i++) {
            newExit[i] = exitRadial[i];
            newBox[i] = insetBox[i];
        }

        for(i = 0; i < 3; i++) {
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

    /**
     * Finds the settings index corresponding to the specified time stamp.
     * @param seek the time stamp
     * @return the index
     */
    private void updateSettings(float seek) {
        int i = 0, start = 0, stop = settingsTimeStamps.size() - 1;
        float time;

        while(start <= stop) {
            i = (stop+start)/2;
            time = settingsTimeStamps.get(i);

            if(time - seek > 0.000001f) {
                stop = i - 1;
            }

            else if(seek - time > 0.000001f) {
                start = i + 1;
            }

            else {
                break;
            }
        }

        //if no match found, get next smallest setting time
        if(start > stop && settingsTimeStamps.get(i) - seek > 0.000001f) {
            i--;
        }

        settingIndex = i;

        float settingsStamp = settingsTimeStamps.get(i);

        threshold = thresholds.get(settingsStamp);

        uic.setThresholdValue(threshold[uic.getThresholdType()]);

        exitRadial = radials.get(settingsStamp);
        insetBox = insets.get(settingsStamp);
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
            }

            //remove current settings
            else {
                timeStamp = settingsTimeStamps.remove(index);

                radials.remove(timeStamp);
                insets.remove(timeStamp);
                thresholds.remove(timeStamp);
            }

            updateSettings(movie.time());
        }
    }

    /**
     * Main method for executing BeeTracker as a Java application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PApplet.main(new String[] { beetracker.BeeTracker.class.getName() });
    }
}
