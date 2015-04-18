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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import controlP5.ControlEvent;

import processing.core.PApplet;
import processing.core.PImage;
import processing.data.IntList;
import processing.data.JSONObject;
import processing.video.Movie;

@SuppressWarnings("serial")
public class BeeTracker extends PApplet {
    private static final int[] mainBounds = {50, 50, 750, 550};
    private static final String months[] = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private IntList colors;
    private HashMap<Float, Bee> bees;
    private int[] movieDims, zoomDims;

    private boolean isPlaying = false, init = false;
    private boolean pip = false, selectExit = true;
    private short playbackSpeed = 1;
    private int listVal = -1;

    private File currentDir = null;

    private float[] insetBox, exitRadial;
    private boolean isDrag = false;

    private Movie movie = null;

    private PImage insetFrame;

    private BlobDetectionUtils bdu;

    private TrackingUtils tu;

    private UIControl uic;

    private PrintStream log = null;

    private PImage titleImg;

    private final boolean debug = true;

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
            Logger.getLogger(BeeTracker.class.getName()).log(Level.SEVERE, null, ex);
            crash(ex.toString());
        }

        uic = new UIControl(this, cp5);

        //read settings file
        colors = new IntList();
        insetBox = new float[4];
        exitRadial = new float[4];
        try {
            JSONObject jsonSettings = loadJSONObject(
                (new File(".")).getAbsolutePath() +
                File.separatorChar +
                "settings.json"
            );
            int tmp;
            String jsonKey;
            JSONObject json;
            java.util.Iterator<?> jsonIter;

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
                colors.clear();
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
                insetBox[0] = insetBox[1] = 0f;
                insetBox[2] = insetBox[3] = 1f;
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
                exitRadial[0] = exitRadial[1] = 0f;
                exitRadial[2] = exitRadial[3] = 0f;
                e3.printStackTrace(log);
            }
        } catch(RuntimeException ex) {
            ex.printStackTrace(log);
        }

        bees = new HashMap<>(colors.size());
        for(Integer color : colors) {
            bees.put(hue(color), new Bee());
        }

        tu = new TrackingUtils(this, log, new File("header.arff"), debug);

        insetFrame = createImage((int)(width*.5f), (int)(height*.5f), RGB);

        bdu = new BlobDetectionUtils((int)(width*.5f), (int)(height*.5f), debug);

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
        rect(mainBounds[0], mainBounds[1], mainBounds[2], mainBounds[3]);

        if(movie != null) {
            if((isPlaying || init) && movie.available()) {
                movie.read();

                if(init) {
                    movie.stop();
                }
            }

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
            frameDims[0] = (int)(movieDims[0]*(insetBox[2] - insetBox[0]));
            frameDims[1] = (int)(movieDims[1]*(insetBox[3] - insetBox[1]));

            imageMode(CENTER);
            image(movie, width*.5f, height*.5f, movieDims[0], movieDims[1]);

            //status box boundary
            strokeWeight(0);
            fill(0xff02344d);
            rectMode(CENTER);
            rect(275, 25, 450, 40);

            if(!init) {
                if(debug && isPlaying) {
                    println("pip: " + pip);
                }

                copyInsetFrame();

                BlobDetectionUtils.filterImg(this, insetFrame, colors);

                HashMap<Integer, Cluster> clusters = tu.getClusters(bdu
                    .getCentroids(this, insetFrame, colors));

                tu.updateBeePositions(insetFrame, clusters, colors, bees,
                    exitRadial, movieDims, movie.time());

                //zoomed
                if(pip) {
                    //in debug mode, show filtered image
                    if(!debug) {
                        copyInsetFrame();
                    }

                    zoomDims = scaledDims(
                        movieDims[0]*(insetBox[2] - insetBox[0]),
                        movieDims[1]*(insetBox[3] - insetBox[1])
                    );
                    frameOffset[0] = (int)((width-zoomDims[0])*.5f);
                    frameOffset[1] = (int)((height-zoomDims[1])*.5f);
                    frameDims = zoomDims;

                    image(insetFrame, .5f*width, .5f*height, zoomDims[0], zoomDims[1]);
                }

                if(debug) {
                    bdu.drawBlobs(this, insetFrame, frameDims, frameOffset);
                }

                //mark bees
                stroke(0xffffffff);
                strokeWeight(.02f*frameDims[1]);

                float cX, cY;
                for(Cluster c : clusters.values()) {
                    cX = (float)c.getX()*frameDims[0] + frameOffset[0];
                    cY = (float)c.getY()*frameDims[1] + frameOffset[1];

                    line(
                        cX + .5f*(float)c.getWidth()*frameDims[0],
                        cY + .5f*(float)c.getHeight()*frameDims[1],
                        cX - .5f*(float)c.getWidth()*frameDims[0],
                        cY - .5f*(float)c.getHeight()*frameDims[1]
                    );
                }

                textAlign(CENTER, CENTER);

                //status box
                fill(0xffffffff);
                text(
                    String.format(
                        "%02d:%02d - %02d:%02d (%dx)",
                        (int)movie.time()/60,
                        (int)movie.time()%60,
                        (int)movie.duration()/60,
                        (int)movie.duration()%60,
                        playbackSpeed
                    ), 275, 25
                );

                //bee count
                rectMode(CENTER);
                strokeWeight(0);
                fill(0xff02344d);
                rect(628, 25, 245, 40);

                fill(0xffffffff);
                textAlign(CENTER, CENTER);
                text("#bees visible: " + clusters.size(), 627, 25);
            }

            else {
                //status box
                textAlign(CENTER, CENTER);
                fill(0xffffffff);
                text("Setup Mode", 275, 25);

                text("Press play to begin.", .5f*width, 575);
            }

            strokeWeight(1);
            stroke(0xffffa600);
            ellipseMode(RADIUS);
            noFill();

            //unzoomed
            if(!pip || init) {
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
                rect(.5f*width, .5f*height, zoomDims[0], zoomDims[1]);

                //exit circle
                ellipse(
                    (exitRadial[0]-insetBox[0])*zoomDims[0]/
                        (insetBox[2]-insetBox[0]) + frameOffset[0],
                    (exitRadial[1]-insetBox[1])*zoomDims[1]/
                        (insetBox[3]-insetBox[1]) + frameOffset[1],
                    exitRadial[2]*zoomDims[0]/(insetBox[2]-insetBox[0]),
                    exitRadial[3]*zoomDims[1]/(insetBox[3]-insetBox[1])
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
            if(isPlaying && movie.duration() - movie.time() < 1f/movie.frameRate) {
                StringBuilder msg = new StringBuilder("End of video reached.\n\n");
                msg.append("bees tracked: ").append(colors.size()).append('\n');

                Bee bee;
                for(Integer color : colors) {
                    bee = bees.get(hue(color));
                    msg.append("color: ").append(String.format("%06x", color))
                        .append("\n-arrival times:\n");

                    for(float arriveTime : bee.getArrivalTimes()) {
                        msg.append("--").append(arriveTime).append('\n');
                    }

                    msg.append("number of arrivals: ")
                        .append(bee.getArrivalTimes().size())
                        .append("\n-departure times:\n");

                    for(float departTime : bee.getArrivalTimes()) {
                        msg.append("--").append(departTime).append('\n');
                    }

                    msg.append('\n');
                }

                msg.append("Video statistics have been saved to \"")
                    .append(resultsToJSON())
                    .append('\"');

                if(debug) {
                    println("\n" + msg.toString());
                }

                MessageDialogue.endVideoMessage(this, msg.toString());

                resultsToJSON();

                stopPlayback();
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
        rect(mainBounds[0], mainBounds[1], mainBounds[2], mainBounds[3]);
    }

    /**
     * Saves the statistics of the current video to file.
     * @return the name of the new file in the format "dd.mmm.yyyy-hhmm.json"
     */
    private String resultsToJSON() {
        JSONObject stats = new JSONObject();
        JSONObject beeStat, tmp;

        stats.setString("file", movie.filename);

        Bee bee;
        List<Float> departure, arrival;
        int i;
        for(int color : colors) {
            bee = bees.get(hue(color));
            departure = bee.getDepartureTimes();
            arrival = bee.getArrivalTimes();

            beeStat = new JSONObject();

            //list arrival timestamps
            tmp = new JSONObject();
            i = 0;
            for(Float arrive : arrival) {
                tmp.setFloat(String.valueOf(i), arrive);
                i++;
            }
            beeStat.setJSONObject("arrivals", tmp);

            //list departure timestamps
            tmp = new JSONObject();
            i = 0;
            for(Float depart : departure) {
                tmp.setFloat(String.valueOf(i), depart);
                i++;
            }
            beeStat.setJSONObject("departures", tmp);

            //associate arrivals and departures with hexadecimal color key
            stats.setJSONObject(String.format("%06x", color), beeStat);
        }

        Calendar date = Calendar.getInstance();
        String fileName = String.format("%02d.%s.%d-%02d%02d.json",
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
    private void copyInsetFrame() {
        //for best results, copy source and destination should be same size
        insetFrame.resize(
            (int)(movie.width*(insetBox[2] - insetBox[0])),
            (int)(movie.height*(insetBox[3] - insetBox[1]))
        );
        insetFrame.copy(
            movie,
            (int)(movie.width*insetBox[0]),
            (int)(movie.height*insetBox[1]),
            (int)(movie.width*(insetBox[2] - insetBox[0])),
            (int)(movie.height*(insetBox[3] - insetBox[1])),
            0, 0, insetFrame.width, insetFrame.height
        );

        //BlobDetection expects certain image size
        insetFrame.resize(bdu.getImageWidth(), bdu.getImageHeight());
    }

    /**
     * Callback method for handling ControlP5 UI events.
     * @param event the initiating ControlEvent
     */
    public void controlEvent(ControlEvent event) {
        String eventName = event.getName();

        switch(eventName) {
        case "openButton":
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
            }

            if(videoPath != null) {
                movie = new Movie(this, videoPath);
                movie.play();
                init = true;
                isPlaying = false;

                uic.toggleSetup();
                uic.toggleOpenButton();
                uic.togglePlay();
                log.append("loaded ").append(videoPath).append('\n');
                log.flush();
            }

            break;

        case "editColor":
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

            break;

        case "removeColor":
            if(listVal >= 0) {
                uic.removeListItem(String.format("%06x", colors.remove(listVal)));

                if(colors.size() == 0) {
                    listVal = -1;
                }
            }

            break;

        case "playButton":
            if(colors.size() > 0 && exitRadial[2] != 0f) {
                isPlaying = !isPlaying;
                uic.setPlayState(isPlaying);

                if(init) {
                    uic.toggleSetup();

                    init = false;

                    colors.resize(colors.size());
                }

                if(movie != null) {
                    if(isPlaying) {
                        movie.play();
                    }

                    else {
                        movie.pause();
                    }
                }
            }

            else {
                boolean[] errors = {(colors.size() == 0), (exitRadial[2] <= 0f)};
                MessageDialogue.playButtonError(this, errors);
            }

            break;

        case "stopButton":
            if(MessageDialogue.stopButtonWarning(this) ==
                javax.swing.JOptionPane.YES_OPTION)
            {
                stopPlayback();
            }

            break;

        case "fastForward":
            //TODO: currently bugged
            if(movie != null) {
                playbackSpeed *= 2;
                if(playbackSpeed > 8) {
                    playbackSpeed = 1;
                }

                movie.speed((float)playbackSpeed);
            }

            break;

        case "colorList":
            listVal = (int)event.getValue();

            if(debug) {
                println(String.format("%d %s", listVal,
                    (
                        listVal > -1 ?
                        String.format("%06x",colors.get(listVal)) :
                        "new color")
                    )
                );
            }

            break;

        case "pipToggle":
            pip = !pip;

            break;

        case "selectToggle":
            selectExit = !selectExit;
            uic.toggleSelectLbl();

            break;
        }
    }

    /**
     * Handles all operations necessary for stopping video playback.
     */
    private void stopPlayback() {
        if(isPlaying) {
            isPlaying = !isPlaying;
            uic.setPlayState(isPlaying);
        }

        if(movie != null) {
            movie.stop();
            movie = null;
        }

        uic.toggleOpenButton();
        uic.togglePlay();

        if(init) {
            uic.toggleSetup();
        }
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

        saveJSONObject(settings, "settings.json");

        super.exit();
    }

    /**
     *
     */
    @Override
    public void mousePressed() {
        if(
            movieDims != null && init &&
            mouseX > (width-movieDims[0])*.5f &&
            mouseX < (width+movieDims[0])*.5f &&
            mouseY > (height-movieDims[1])*.5f &&
            mouseY < (height+movieDims[1])*.5f
        ) {
            if(!selectExit) {
                insetBox[0] = insetBox[2] =
                    (float)(mouseX-(width-movieDims[0])*.5f)/movieDims[0];
                insetBox[1] = insetBox[3] =
                    (float)(mouseY-(height-movieDims[1])*.5f)/movieDims[1];
            }

            else {
                exitRadial[0] = (float)(mouseX-(width-movieDims[0])*.5f)/movieDims[0];
                exitRadial[1] = (float)(mouseY-(height-movieDims[1])*.5f)/movieDims[1];
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

                insetBox[2] = (float)(mouse[0]-(width-movieDims[0])*.5f)/movieDims[0];
                insetBox[3] = (float)(mouse[1]-(height-movieDims[1])*.5f)/movieDims[1];
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
        //semi-major axis (y)
        result[1] = exitRadial[1]*movieDims[1] + (height-movieDims[1])*.5f - mouse[1];

        result[0] = result[1] = (float)Math.pow((Math.pow(result[0], 2) + Math.pow(result[1], 2)), .5);

        //constrain semi-major axis (x)
        if(result[0] > exitRadial[0]*movieDims[0]) {
            result[0] = exitRadial[0]*movieDims[0];
        }

        if(result[0] > movieDims[0]-exitRadial[0]*movieDims[0]) {
            result[0] = movieDims[0]-exitRadial[0]*movieDims[0];
        }

        //constrain semi-major axis (y)
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

        if(mouseX < (width-movieDims[0])*.5f) {
            result[0] = (int)((width-movieDims[0])*.5f);
        }

        else if (mouseX > (width+movieDims[0])*.5f) {
            result[0] = (int)((width+movieDims[0])*.5f);
        }

        if(mouseY < (height-movieDims[1])*.5f) {
            result[1] = (int)((height-movieDims[1])*.5f);
        }

        else if(mouseY > (height+movieDims[1])*.5f) {
            result[1] = (int)((height+movieDims[1])*.5f);
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
                println(String.format(
                    "inset: %f, %f, %f, %f\nexit: %f, %f, %f, %f\n",
                    insetBox[0], insetBox[1], insetBox[2], insetBox[3],
                    exitRadial[0], exitRadial[1], exitRadial[2], exitRadial[3]
                ));
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
    private int[] scaledDims(
        float imgWidth,
        float imgHeight
    ) {
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
     * Main method for executing BeeTracker as a Java application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PApplet.main(new String[] { beetracker.BeeTracker.class.getName() });
    }
}
