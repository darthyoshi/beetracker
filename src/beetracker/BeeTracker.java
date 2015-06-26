/**
 * @file BeeTracker.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description A tool for tracking bees in a video.
 */

package beetracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

import controlP5.ControlEvent;

import processing.core.PApplet;
import processing.core.PImage;
import processing.data.FloatList;
import processing.data.JSONObject;
import processing.video.Movie;

@SuppressWarnings("serial")
public class BeeTracker extends PApplet {
    private static final int[] mainBounds = {50, 50, 750, 550};
    private static final String months[] = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    private static final String[] thresholdKeys = {"hue", "sat", "val"};

    private processing.data.IntList colors;
    private int[] movieDims;

    private boolean isPlaying = false, movieLoaded = false;
    private boolean record = false, replay = false;
    private boolean pip = false, selectExit = true;
    private int listVal = -1;

    private int[] crossingCounts;

    private File currentDir = null;

    private float[] insetBox, exitRadial;
    private boolean isDrag = false;

    private Movie movie = null;

    private BlobDetectionUtils bdu;

    private TrackingUtils tu;

    private UIControl uic;

    private PrintStream log = null;

    private PImage titleImg;

    private final boolean debug = true;

    private HashMap<Float, HashMap<Integer, List<float[]>>> allFramePoints = null;
    private HashMap<Integer, List<float[]>> centroids;
    private FloatList allFrameTimes = null;
    private int timeStampIndex = -1;

    private String videoName = null;

    /**
     *
     */
    @Override
    public void setup() {
        size(800, 600);
        frameRate(60);
        strokeWeight(1);

        controlP5.ControlP5 cp5 = new controlP5.ControlP5(this);

        textFont(cp5.getFont().getFont(), 24);

        if(frame != null) {
            frame.setTitle("BeeTracker");
        }

        //create log file
        try {
            log = new PrintStream(new File("Console.log"), "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            java.util.logging.Logger.getLogger(BeeTracker.class.getName())
                .log(java.util.logging.Level.SEVERE, null, ex);
            crash(ex.toString());
        }

        uic = new UIControl(this, cp5);

        bdu = new BlobDetectionUtils(this, width/2, height/2, debug);

        //read settings file
        colors = new processing.data.IntList();
        insetBox = new float[4];
        exitRadial = new float[4];
        boolean[] settingsErrors = {true, false, false, false, false};
        try {
            JSONObject jsonSettings = loadJSONObject(
                System.getProperty("user.dir") +
                File.separatorChar +
                "settings.json"
            );

            settingsErrors[0] = false;

            int tmp;
            String jsonKey;
            JSONObject json;
            Iterator<?> jsonIter;

            //initialize color list
            try {
                json = jsonSettings.getJSONObject("colors");
                jsonIter = json.keyIterator();
                while(jsonIter.hasNext()) {
                    tmp = (int)Long.parseLong(
                        json.getString((String) jsonIter.next()), 16);

                    uic.addListItem(String.format("%06x", tmp));

                    colors.append(tmp);
                }
            } catch(NumberFormatException e1) {
                settingsErrors[1] = true;
                e1.printStackTrace(log);
            }

            //initialize selection box
            try {
                json = jsonSettings.getJSONObject("insetBox");
                jsonIter = json.keyIterator();
                while(jsonIter.hasNext()) {
                    jsonKey = (String) jsonIter.next();
                    tmp = Integer.parseInt(jsonKey);

                    insetBox[tmp] = json.getFloat(jsonKey, (tmp*.5f < 1 ? 0f: 1f));
                }
            } catch(Exception e2) {
                settingsErrors[2] = true;
                e2.printStackTrace(log);
            }

            //initialize exit circle
            try {
                json = jsonSettings.getJSONObject("exitRadial");
                jsonIter = json.keyIterator();
                while(jsonIter.hasNext()) {
                    jsonKey = (String) jsonIter.next();
                    tmp = Integer.parseInt(jsonKey);

                    exitRadial[tmp] = json.getFloat(jsonKey, 0f);
                }
            } catch(Exception e3) {
                settingsErrors[3] = true;
                e3.printStackTrace(log);
            }

            //set thresholds
            try {
                json = jsonSettings.getJSONObject("thresholds");
                int i = 0;

                for(String keyString : thresholdKeys) {
                    switch(keyString) {
                    case "hue": i = 0; break;

                    case "sat": i = 1; break;

                    case "val": i = 2;
                    }

                    bdu.setThresholdValue(i, json.getInt(keyString));
                }
            } catch(Exception e4) {
                settingsErrors[4] = true;
                e4.printStackTrace(log);
            }
        } catch(RuntimeException ex) {
            ex.printStackTrace(log);
        }

        if(settingsErrors[0] || settingsErrors[1]) {
            colors.clear();
        }
        if(settingsErrors[0] || settingsErrors[2]) {
            insetBox[0] = insetBox[1] = 0f;
            insetBox[2] = insetBox[3] = 1f;
        }
        if(settingsErrors[0] || settingsErrors[3]) {
            exitRadial[0] = exitRadial[1] = 0f;
            exitRadial[2] = exitRadial[3] = 0f;
        }
        if(settingsErrors[0] || settingsErrors[4]) {
            bdu.setThresholdValue(0, 40);
            bdu.setThresholdValue(1, 90);
            bdu.setThresholdValue(2, 20);
        }

        tu = new TrackingUtils(debug);

        titleImg = loadImage("data/img/title.png");
    }

    /**
     *
     */
    @Override
    public void draw() {
        background(0x222222);

        fill(0xff444444);
        rectMode(CORNERS);
        rect(mainBounds[0]-1, mainBounds[1]-1, mainBounds[2]+1, mainBounds[3]+1);

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

            if(movieLoaded) {
                movieDims = scaledDims(
                    movie.width,
                    movie.height
                );

                int[] offset = {
                    (int)((width-movieDims[0])*.5f),
                    (int)((height-movieDims[1])*.5f)
                };
                int[] frameOffset = new int[2];
                frameOffset[0] = (int)(movieDims[0]*insetBox[0]) + offset[0];
                frameOffset[1] = (int)(movieDims[1]*insetBox[1]) + offset[1];
                int[] frameDims = new int[2];
                frameDims[0] = (int)(movieDims[0] * (insetBox[2]-insetBox[0]));
                frameDims[1] = (int)(movieDims[1] * (insetBox[3]-insetBox[1]));

                imageMode(CENTER);
                image(movie, width*.5f, height*.5f, movieDims[0], movieDims[1]);

                noSmooth();

                PImage insetFrame = copyInsetFrame();

                if(insetFrame != null) {
                    if(replay) {
                        float timeStamp = allFrameTimes.get(timeStampIndex);

                        if(
                            timeStampIndex >= 0 &&
                            timeStampIndex < allFrameTimes.size() &&
                            timeStamp < time
                        ) {
                            if(debug) {
                                println("frame info for " + timeStamp +
                                    "s, actual time " + time + 's');
                            }

                            if(allFramePoints.containsKey(timeStamp)) {
                                centroids = allFramePoints.get(timeStamp);
                            }
                            timeStampIndex++;
                        }
                    }

                    else {
                        bdu.filterImg(insetFrame, colors);

                        centroids = bdu.getCentroids(insetFrame, colors);
                    }

                    if(isPlaying) {
                        if(record) {
                            allFramePoints.put(time, centroids);
                            allFrameTimes.append(time);
                        }

                        if(record || replay) {
                            if(debug) {
                                println(
                                    "---------BEGIN FRAME (" +
                                    String.format("%.2f", time) +
                                    "s)---------\npip: " + pip
                                );
                            }

                            int[] counts = tu.trackCentroids(
                                centroids,
                                frameDims, frameOffset,
                                exitRadial,
                                movieDims, offset,
                                time
                            );
                            crossingCounts[0] += counts[0];
                            crossingCounts[1] += counts[1];
                        }
                    }

                    float[] center = new float[2];
                    //zoomed
                    if(pip) {
                    	PImage zoomedInset = copyInsetFrame();

                    	frameDims = scaledDims(
                            movieDims[0]*(insetBox[2] - insetBox[0]),
                            movieDims[1]*(insetBox[3] - insetBox[1])
                        );
                        frameOffset[0] = (int)((width-frameDims[0])*.5f);
                        frameOffset[1] = (int)((height-frameDims[1])*.5f);

                        center[0] = width*.5f;
                        center[1] = height*.5f;

                        image(
                    		zoomedInset,
                            center[0],
                            center[1],
                            frameDims[0],
                            frameDims[1]
                        );
                    }

                    else {
                        center[0] = (insetBox[0]+insetBox[2])*movieDims[0]*.5f + offset[0];
                        center[1] = (insetBox[1]+insetBox[3])*movieDims[1]*.5f + offset[1];
                    }

                    image(
                        insetFrame,
                        center[0],
                        center[1],
                        frameDims[0],
                        frameDims[1]
                    );

                    smooth();

                    //draw detected blobs
                    if(!replay) {
                        float[] exitXY = new float[2];

                        if(pip) {
                            exitXY[0] = (exitRadial[0]-insetBox[0])*frameDims[0]/
                                (insetBox[2]-insetBox[0]) + frameOffset[0];
                            exitXY[1] = (exitRadial[1]-insetBox[1])*frameDims[1]/
                                (insetBox[3]-insetBox[1]) + frameOffset[1];
                        }

                        else {
                            exitXY[0] = exitRadial[0]*movieDims[0] + offset[0];
                            exitXY[1] = exitRadial[1]*movieDims[1] + offset[1];
                        }

                        bdu.drawBlobs(frameDims, frameOffset, exitXY);
                    }

                    //mark bees
                    else {
                        strokeWeight(1);
                        stroke(0xffdddd00);
                        ellipseMode(CENTER);
                        colorMode(RGB, 255);

                        List<float[]> centroidList;
                        for(int color : colors) {
                            fill(0xff000000 + color);

                            centroidList = centroids.get(color);
                            if(centroidList != null) {
                                for(float[] centroid : centroidList) {
                                    ellipse(
                                        centroid[0]*frameDims[0] + frameOffset[0],
                                        centroid[1]*frameDims[1] + frameOffset[1],
                                        .02f*frameDims[1],
                                        .02f*frameDims[1]
                                    );
                                }
                            }
                        }
                    }
                }

                textAlign(CENTER, CENTER);
                rectMode(CENTER);
                noStroke();
                fill(0xff02344d);
                rect(145, 25, 190, 40);

                //bee count
                if(isPlaying) {
                    rect(535, 25, 430, 40);

                    fill(0xffffffff);

                    text("total #arrivals: " + crossingCounts[1] +
                        ", total #departures: " + crossingCounts[0], 534, 25);

                    if(record) {
                        text("Annotation Mode", 145, 25);

                        if(debug) {
                            println("---------END FRAME---------");
                        }
                    }

                    else {
                        text("Replay Mode", 145, 25);
                    }
                }

                //status box
                else {
                    fill(0xffffffff);
                    text("Config Mode", 145, 25);
                }

                strokeWeight(1);
                stroke(0xffffa600);
                ellipseMode(RADIUS);
                noFill();

                //unzoomed
                if(!pip) {
                    //inset box
                    rectMode(CORNERS);
                    rect(
                        insetBox[0]*movieDims[0] + offset[0],
                        insetBox[1]*movieDims[1] + offset[1],
                        insetBox[2]*movieDims[0] + offset[0],
                        insetBox[3]*movieDims[1] + offset[1]
                    );

                    //exit circle
                    ellipse(
                        exitRadial[0]*movieDims[0] + offset[0],
                        exitRadial[1]*movieDims[1] + offset[1],
                        exitRadial[2]*movieDims[0],
                        exitRadial[3]*movieDims[1]
                    );
                }

                //zoomed
                else {
                    //inset box
                    rectMode(CENTER);
                    rect(.5f*width, .5f*height, frameDims[0], frameDims[1]);

                    //exit circle
                    ellipse(
                        (exitRadial[0]-insetBox[0])*frameDims[0]/
                            (insetBox[2]-insetBox[0]) + frameOffset[0],
                        (exitRadial[1]-insetBox[1])*frameDims[1]/
                            (insetBox[3]-insetBox[1]) + frameOffset[1],
                        exitRadial[2]*frameDims[0]/(insetBox[2]-insetBox[0]),
                        exitRadial[3]*frameDims[1]/(insetBox[3]-insetBox[1])
                    );
                }

                if(isDrag) {
                    if(!selectExit) {
                        //draw selection box diagonals
                        line(
                            insetBox[0]*movieDims[0] + offset[0],
                            insetBox[1]*movieDims[1] + offset[1],
                            insetBox[2]*movieDims[0] + offset[0],
                            insetBox[3]*movieDims[1] + offset[1]
                        );
                        line(
                            insetBox[0]*movieDims[0] + offset[0],
                            insetBox[3]*movieDims[1] + offset[1],
                            insetBox[2]*movieDims[0] + offset[0],
                            insetBox[1]*movieDims[1] + offset[1]
                        );
                    }

                    else {
                        //draw selection circle interior lines
                        line(
                            exitRadial[0]*movieDims[0] + offset[0],
                            exitRadial[1]*movieDims[1] + offset[1] -
                                exitRadial[3]*movieDims[1],
                            exitRadial[0]*movieDims[0] + offset[0],
                            exitRadial[1]*movieDims[1] + offset[1] +
                                exitRadial[3]*movieDims[1]
                        );
                        line(
                            exitRadial[0]*movieDims[0] + offset[0] -
                                exitRadial[2]*movieDims[0],
                            exitRadial[1]*movieDims[1] + offset[1],
                            exitRadial[0]*movieDims[0] + offset[0] +
                                exitRadial[2]*movieDims[0],
                            exitRadial[1]*movieDims[1] + offset[1]
                        );
                    }
                }

                //end of movie reached
                if(isPlaying && movie.duration() - time < 1f/movie.frameRate) {
                    StringBuilder msg = new StringBuilder("End of video reached.\n\n");
                    msg.append("bees tracked: ").append(colors.size()).append('\n');

                    HashMap<String, HashMap<Integer, List<Float>>> timeStamps = tu.getTimeStamps();

                    for(int color : colors) {
                        msg.append("color: ").append(String.format("%06x", color))
                            .append("\n-arrival times:\n");

                        for(float arriveTime : timeStamps.get("arrivals").get(color)) {
                            msg.append("--").append(arriveTime).append('\n');
                        }

                        msg.append("number of arrivals: ")
                            .append(timeStamps.get("arrivals").get(color).size())
                            .append("\n-departure times:\n");

                        for(float departTime : timeStamps.get("departures").get(color)) {
                            msg.append("--").append(departTime).append('\n');
                        }

                        msg.append("number of departures: ")
                            .append(timeStamps.get("departures").get(color).size())
                            .append('\n');
                    }

                    if(debug) {
                        println("\n" + msg.toString());
                    }

                    if(MessageDialogue.endVideoMessage(this, msg.toString()) ==
                        JOptionPane.YES_OPTION)
                    {
                        movie.jump(0f);

                        if(record) {
                            replay = true;
                            recordButton();
                            uic.setRecordVisibility(false);

                            timeStampIndex = 0;

                            centroids = new HashMap<>();
                            List<float[]> tmpList = new ArrayList<>();
                            for(int tmp : colors) {
                                centroids.put(tmp, tmpList);
                            }
                        }
                    }

                    else {
                        if(
                            MessageDialogue.saveStatisticsmessage(
                                this, writeTimeStampsToJSON(timeStamps)
                            ) == JOptionPane.YES_OPTION
                        ) {
                            writeFramePointsToJSON();
                        }

                        stopPlayback();
                    }
                }
            }

            else if(movie.height > 0) {
                movieLoaded = true;
                uic.setSeekRange(movie.duration());
            }
        }

        else {
            imageMode(CENTER);
            image(titleImg, .5f*width, .5f*height-50);
        }

        //main window border
        stroke(0xffffffff);
        noFill();
        rectMode(CORNERS);
        rect(mainBounds[0]-1, mainBounds[1]-1, mainBounds[2]+1, mainBounds[3]+1);
    }

    /**
     * Saves the statistics of the current video to file.
     * @param times an array containing HashMaps mapping six-digit hexadecimal
     *   RGB values to Lists of floating point timestamps
     * @return the name of the new file in the format "dd.mmm.yyyy-hhmm.json"
     */
    private String writeTimeStampsToJSON(
        HashMap<String, HashMap<Integer, List<Float>>> times)
    {
        JSONObject stats = new JSONObject();
        JSONObject beeStat, tmp;

        stats.setString("file", movie.filename);

        String[] keys = {"arrivals", "departures"};

        int i;
        for(int color : colors) {
            beeStat = new JSONObject();

            //list arrival timestamps
            tmp = new JSONObject();
            i = 0;
            for(Float arrive : times.get(keys[0]).get(color)) {
                tmp.setFloat(String.valueOf(i), Float.parseFloat(String.format("%.3f", arrive)));
                i++;
            }
            beeStat.setJSONObject(keys[0], tmp);

            //list departure timestamps
            tmp = new JSONObject();
            i = 0;
            for(Float depart : times.get(keys[1]).get(color)) {
                tmp.setFloat(String.valueOf(i), Float.parseFloat(String.format("%.3f", depart)));
                i++;
            }
            beeStat.setJSONObject(keys[1], tmp);

            //associate arrivals and departures with hexadecimal color key
            stats.setJSONObject(String.format("%06x", color), beeStat);
        }

        Calendar date = Calendar.getInstance();
        String fileName = System.getProperty("user.dir") +
            File.separatorChar + videoName +
            String.format("-%02d.%s.%d-%02d%02d.json",
                date.get(Calendar.DAY_OF_MONTH),
                months[date.get(Calendar.MONTH)],
                date.get(Calendar.YEAR),
                date.get(Calendar.HOUR_OF_DAY),
                date.get(Calendar.MINUTE)
            );

        saveJSONObject(stats, fileName);

        return fileName;
    }

    /**
     * Copies the inset frame for image processing and blob detection.
     */
    private PImage copyInsetFrame() {
        PImage result;

        if(isDrag) {
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
                0, 0, result.width, result.height
            );

            //BlobDetection expects certain image size
            result.resize(bdu.getImageWidth(), bdu.getImageHeight());
        }

        return result;
    }

    /**
     * ControlP5 callback method.
     */
    public void editColor() {
        int editColor = ColorPicker.getColor(this);

        if(listVal < 0) {
            if(!colors.hasValue(editColor)) {
                String code = String.format("%06x", editColor);

                uic.addListItem(code);

                colors.append(editColor);
            }
        }

        else {
            colors.set(listVal, editColor);

            uic.setListItem(String.format("%06x", editColor), listVal);
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
                    MessageDialogue.playButtonError(this, errors);
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

                uic.toggleSetup();
                uic.setPlayState(isPlaying);
                uic.setRangeVisibility(!isPlaying);
            }
        }

    }
    /**
     * ControlP5 callback method.
     */
    public void ejectButton() {
        if(MessageDialogue.stopButtonWarning(this) ==
            JOptionPane.YES_OPTION)
        {
            if(debug) {
                println("stopping playback...");
            }

            stopPlayback();
        }
    }

    /**
     * ControlP5 callback method for group events.
     * @param event the originating event
     */
    public void controlEvent(ControlEvent event) {
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
        }
    }

    /**
     * ControlP5 callback method.
     * @param value
     */
    public void thresholdSlider(float value) {
        int thresholdType = uic.getThresholdType();

        if(debug) {
            println("slider value: " + value);
        }

        bdu.setThresholdValue(thresholdType, (int)value);
    }

    /**
     * ControlP5 callback method.
     */
    public void pipToggle() {
        pip = !pip;
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
        uic.setRangeValues(bdu.getThresholdValue(value));
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

        //update playback mode timestamp
        if(replay) {
            int i = 0, start = 0, stop = allFrameTimes.size() - 1;
            float time;

            while(start < stop) {
                i = (stop+start)/2;
                time = allFrameTimes.get(i);

                if(time == value) {
                    break;
                }

                else if(time > value) {
                    stop = i - 1;
                }

                else {
                    start = i + 1;
                }
            }

            timeStampIndex = i + 1;
          }

        movie.jump(value);
        uic.setSeekTime(value);
    }

    /**
     * ControlP5 callback method.
     */
    public void recordButton() {
        record = !record;
        uic.setRecordState(record);

        if(!isPlaying) {
            playButton();
        }
    }

    /**
     * ControlP5 callback method.
     */
    public void openButton() {
        File video = VideoBrowser.getVideoFile(this, currentDir);

        String videoPath = null;

        if(video != null) {
            try {
                videoPath = video.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace(log);
                crash(e.toString());
            }

            currentDir = video.getParentFile();

            if(videoPath != null) {
                movie = new Movie(this, videoPath);

                movie.volume(0f);
                movie.play();
                isPlaying = false;

                uic.toggleSetup();
                uic.toggleOpenButton();
                uic.togglePlay();
                uic.setRangeVisibility(true);

                videoName = video.getName();

                crossingCounts = new int[2];
                crossingCounts[0] = crossingCounts[1] = 0;

                replay = readFramePointsFromJSON();
                uic.setRecordVisibility(!replay);

                String msg = "loaded " + videoPath + '\n';

                if(debug) {
                    print(msg);
                }
                log.append(msg).flush();
            }
        }
    }

    /**
     * Handles all operations necessary for stopping video playback.
     */
    private void stopPlayback() {
        if(record) {
            record = false;
            uic.setRecordState(false);
        }

        if(isPlaying) {
            isPlaying = false;
            uic.setPlayState(false);
        }

        else {
            uic.toggleSetup();
        }

        if(movie != null) {
            movie.stop();
            movie = null;
            movieLoaded = false;
            videoName = null;

            allFrameTimes = null;
            allFramePoints = null;
            timeStampIndex = -1;
        }

        uic.toggleOpenButton();
        uic.togglePlay();
        uic.setRangeVisibility(false);
        uic.selectRadioButton(0);
        uic.setSeekTime(0f);
        radioButtons(0);

        tu.init();
    }

    /**
     * Displays an error message dialogue before exiting the program.
     * @param msg a description of the cause of the crash
     */
    public void crash(String msg) {
        MessageDialogue.crashMessage(this, msg);

        this.exit();
    }

    /**
     *
     */
    @Override
    public void exit() {
        if(log != null) {
            log.close();
        }

        if(movie != null) {
            movie.stop();
            movie = null;
        }

        if(debug) {
            print("saving settings");
        }

        //save current color and selection settings
        JSONObject settings = new JSONObject();
        JSONObject setting = new JSONObject();
        int i;

        for(i = 0; i < colors.size(); i++) {
            setting.setString(Integer.toString(i),
                String.format("%06x", colors.get(i)));
        }
        settings.setJSONObject("colors", setting);

        setting = new JSONObject();
        for(i = 0; i < insetBox.length; i++) {
            setting.setFloat(Integer.toString(i), insetBox[i]);
        }
        settings.setJSONObject("insetBox", setting);

        setting = new JSONObject();
        for(i = 0; i < exitRadial.length; i++) {
            setting.setFloat(Integer.toString(i), exitRadial[i]);
        }
        settings.setJSONObject("exitRadial", setting);

        setting = new JSONObject();
        i = 0;
        for(String keyString : thresholdKeys) {
            setting.setInt(keyString, bdu.getThresholdValue(i));

            i++;
        }
        settings.setJSONObject("thresholds", setting);

        saveJSONObject(settings, "settings.json");

        if(debug) {
            println(" - done");
        }

        super.exit();
    }

    /**
     *
     */
    @Override
    public void mousePressed() {
        if(
            movieDims != null && !isPlaying && !pip &&
            mouseX > (width-movieDims[0])/2 &&
            mouseX < (width+movieDims[0])/2 &&
            mouseY > (height-movieDims[1])/2 &&
            mouseY < (height+movieDims[1])/2
        ) {
            if(!selectExit) {
                insetBox[0] = insetBox[2] =
                    (mouseX-(width-movieDims[0])*.5f)/movieDims[0];
                insetBox[1] = insetBox[3] =
                    (mouseY-(height-movieDims[1])*.5f)/movieDims[1];
            }

            else {
                exitRadial[0] = (mouseX-(width-movieDims[0])*.5f)/movieDims[0];
                exitRadial[1] = (mouseY-(height-movieDims[1])*.5f)/movieDims[1];
                exitRadial[2] = exitRadial[3] = 0f;
            }

            isDrag = true;
        }
    }

    /**
     *
     */
    @Override
    public void mouseDragged() {
        if(isDrag) {
            if(!selectExit) {
                int mouse[] = constrainMousePosition(mouseX, mouseY);

                insetBox[2] = (mouse[0]-(width-movieDims[0])*.5f)/movieDims[0];
                insetBox[3] = (mouse[1]-(height-movieDims[1])*.5f)/movieDims[1];
            }

            else {
                float tmp[] = constrainRadius(mouseX, mouseY);

                exitRadial[2] = tmp[0];
                exitRadial[3] = tmp[1];
            }
        }
    }

    /**
     * Constrains the selection circle within the view window.
     * @param mouseX the x-coordinate of the mouse
     * @param mouseY the y-coordinate of the mouse
     * @return a float array containing the normalized circle radius
     */
    private float[] constrainRadius(int mouseX, int mouseY) {
        int mouse[] = constrainMousePosition(mouseX, mouseY);

        float[] result = new float[2];
        //semi-major axis (x)
        result[0] = exitRadial[0]*movieDims[0] + (width-movieDims[0])*.5f - mouse[0];
        //semi-minor axis (y)
        result[1] = exitRadial[1]*movieDims[1] + (height-movieDims[1])*.5f - mouse[1];

        result[0] = result[1] = (float)Math.pow((Math.pow(result[0], 2) + Math.pow(result[1], 2)), .5);

        //constrain semi-major axis (x)
        if(result[0] > exitRadial[0]*movieDims[0]) {
            result[0] = exitRadial[0]*movieDims[0];
        }

        if(result[0] > movieDims[0]-exitRadial[0]*movieDims[0]) {
            result[0] = movieDims[0]-exitRadial[0]*movieDims[0];
        }

        //constrain semi-minor axis (y)
        if(result[1] > exitRadial[1]*movieDims[1]) {
            result[1] = exitRadial[1]*movieDims[1];
        }

        if(result[1] > movieDims[1]-exitRadial[1]*movieDims[1]) {
            result[1] = movieDims[1]-exitRadial[1]*movieDims[1];
        }

        //choose smaller axis
        if(result[0] < result[1]) {
            result[1] = result[0];
        }

        else {
            result[0] = result[1];
        }

        //normalize axes
        result[0] /= movieDims[0];
        result[1] /= movieDims[1];

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

        if(mouseX < (width-movieDims[0])/2) {
            result[0] = (width-movieDims[0])/2;
        }

        else if (mouseX > (width+movieDims[0])/2) {
            result[0] = (width+movieDims[0])/2;
        }

        if(mouseY < (height-movieDims[1])/2) {
            result[1] = (height-movieDims[1])/2;
        }

        else if(mouseY > (height+movieDims[1])/2) {
            result[1] = (height+movieDims[1])/2;
        }

        return result;
    }

    /**
     *
     */
    @Override
    public void mouseReleased() {
        if(isDrag) {
            if(!selectExit) {
                //selection box has area of zero
                if(insetBox[0] == insetBox[2] || insetBox[1] == insetBox[3]) {
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
        result[0] = width - 102;
        result[1] = (int)(result[0]/ratio);

        //scale by height
        if(result[1] > height - 102) {
            result[1] = height - 102;
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

        String path = System.getProperty("user.dir") +
            File.separatorChar + videoName + "-points.json";

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

        centroids = new HashMap<>();
        for(int tmp : colors) {
            centroids.put(tmp, tmpList);
        }

        return result;
    }

    /**
     * Writes the generated blob information to a file.
     */
    private void writeFramePointsToJSON() {
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

                jsonColors.setJSONObject(String.format("%06x", color), jsonBlobs);
            }

            json.setJSONObject(Float.toString(timeStamp), jsonColors);
        }

        saveJSONObject(json, System.getProperty("user.dir") +
            File.separatorChar + videoName + "-points.json");
    }

    /**
     * Main method for executing BeeTracker as a Java application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PApplet.main(new String[] { beetracker.BeeTracker.class.getName() });
    }
}
