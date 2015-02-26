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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import controlP5.ControlEvent;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.IntList;
import processing.data.JSONObject;
import processing.video.Movie;

@SuppressWarnings("serial")
public class BeeTracker extends PApplet {
    private IntList colors;
    private HashMap<Float, Bee> bees;
    private boolean isPlaying = false, init = false;
    private boolean pip = false, selectExit = true;
    private static final int[] mainBounds = {50, 50, 750, 550};
    private short playbackSpeed = 1;
    private int listVal = 0;
    private int[] movieDims, zoomDims;

    private static final String errorMsg[] = {
        "No colors have been selected.\n",
        "No hive exit has been defined.\n"
    };

    private File currentDir = null;

    private float[] insetBox, exitRadial;
    private boolean isDrag = false;

    private Movie movie = null;

    private PImage blobImg;

    private BlobDetectionUtils bdu;

    private DataMinerUtils dmu;

    private UIControl uic;

    private PrintStream log = null;

    private processing.core.PFont font;
    private PImage titleImg;

    /**
     *
     */
    @Override
    public void setup() {
        size(800, 600);
        frameRate(30);

        if(frame != null) {
            frame.setTitle("BeeTracker");
        }

        try {
            log = new PrintStream(new File("Console.log"));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            exit();
        }

        uic = new UIControl(this);

        colors = new IntList();
        insetBox = new float[4];
        exitRadial = new float[4];
        JSONObject jsonSettings = null;
        try {
            jsonSettings = loadJSONObject("settings.json");
            int tmp;
            String jsonKey;
            JSONObject json;
            java.util.Iterator<?> jsonIter;

            try {
                json = jsonSettings.getJSONObject("colors");
                jsonIter = json.keyIterator();
                while(jsonIter.hasNext()) {
                    jsonKey = json.getString((String) jsonIter.next());

                    tmp = (int)Long.parseLong(jsonKey, 16);

                    uic.addListItem(jsonKey, tmp);

                    colors.append(tmp);
                }
            } catch(NumberFormatException e1) {
                colors.clear();
                e1.printStackTrace(log);
            }

            try {
                json = jsonSettings.getJSONObject("insetBox");
                jsonIter = json.keyIterator();
                while(jsonIter.hasNext()) {
                    jsonKey = (String) jsonIter.next();
                    tmp = Integer.parseInt(jsonKey);

                    insetBox[tmp] = json.getFloat(jsonKey, (tmp/2 < 1 ? 0f: 1f));
                }
            } catch(Exception e2) {
                insetBox[0] = insetBox[1] = 0f;
                insetBox[2] = insetBox[3] = 1f;
                e2.printStackTrace(log);
            }

            try {
                json = jsonSettings.getJSONObject("exitRadial");
                jsonIter = json.keyIterator();
                while(jsonIter.hasNext()) {
                    jsonKey = (String) jsonIter.next();
                    tmp = Integer.parseInt(jsonKey);

                    exitRadial[tmp] = json.getFloat(jsonKey, 0f);
                    println(tmp+" "+exitRadial[tmp]);
                }
            } catch(Exception e3) {
                exitRadial[0] = exitRadial[1] = 0f;
                exitRadial[2] = exitRadial[3] = 0f;
                e3.printStackTrace(log);
            }
        } catch(RuntimeException ex) {
            ex.printStackTrace(log);
        }

        bees = new HashMap<Float, Bee>(colors.size());
        for(Integer color : colors) {
            bees.put(hue(color), new Bee());
        }

        dmu = new DataMinerUtils(this, log);

        background(0x444444);

        blobImg = createImage(width, width, RGB);

        bdu = new BlobDetectionUtils(width, height);

        font = this.createDefaultFont(12);
        titleImg = loadImage("data/img/title.png");
    }

    /**
     *
     */
    @Override
    public void draw() {
        background(0x222222);

        noStroke();
        fill(0xff444444);
        rectMode(CORNERS);
        rect(mainBounds[0], mainBounds[1], mainBounds[2], mainBounds[3]);

        textFont(font);
        fill(0xFFFF0099);

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
            int[] offset = {(width-movieDims[0])/2, (height-movieDims[1])/2};
            int[] frameOffset = new int[2];
            frameOffset[0] = (int)(movieDims[0]*insetBox[0]) + offset[0];
            frameOffset[1] = (int)(movieDims[1]*insetBox[1]) + offset[1];
            int[] frameDims = new int[2];
            frameDims[0] = (int)(movieDims[0]*(insetBox[2] - insetBox[0]));
            frameDims[1] = (int)(movieDims[1]*(insetBox[3] - insetBox[1]));

            imageMode(CENTER);
            image(movie, width/2, height/2, movieDims[0], movieDims[1]);

            if(!init) {
                blobImg.resize(
                    (int)(movie.width*(insetBox[2] - insetBox[0])),
                    (int)(movie.height*(insetBox[3] - insetBox[1]))
                );
                blobImg.copy(
                    movie,
                    (int)(movie.width*insetBox[0]),
                    (int)(movie.height*insetBox[1]),
                    (int)(movie.width*(insetBox[2] - insetBox[0])),
                    (int)(movie.height*(insetBox[3] - insetBox[1])),
                    0, 0, blobImg.width, blobImg.height
                );
                blobImg.resize(width, height);

                BlobDetectionUtils.preProcessImg(this, blobImg, colors);

                bdu.computeBlobs(blobImg);

                List<Cluster> clusters = dmu.getClusters(bdu.getCentroids());

                dmu.updateBeePositions(blobImg, clusters, colors, bees, exitRadial);

                if(pip) {
                    blobImg.resize(
                        (int)(movie.width*(insetBox[2] - insetBox[0])),
                        (int)(movie.height*(insetBox[3] - insetBox[1]))
                    );
                    blobImg.copy(
                        movie,
                        (int)(movie.width*insetBox[0]),
                        (int)(movie.height*insetBox[1]),
                        (int)(movie.width*(insetBox[2] - insetBox[0])),
                        (int)(movie.height*(insetBox[3] - insetBox[1])),
                        0, 0, blobImg.width, blobImg.height
                    );
                    blobImg.resize(width, height);

                    zoomDims = scaledDims(
                        movieDims[0]*(insetBox[2] - insetBox[0]),
                        movieDims[1]*(insetBox[3] - insetBox[1])
                    );
                    frameOffset[0] = (width-zoomDims[0])/2;
                    frameOffset[1] = (height-zoomDims[1])/2;
                    frameDims = zoomDims;

                    image(blobImg, width/2, height/2, zoomDims[0], zoomDims[1]);
                }

                bdu.drawBlobs(this, frameDims, frameOffset);

                textSize(32);
                textAlign(CENTER, CENTER);
                text("#bees: " + clusters.size(), width/2, 25);

                textAlign(RIGHT, CENTER);
                text("current speed: "+/*playbackSpeed*/movie.frameRate+'x', 750, 575);
            }

            else {
                textSize(24);
                textAlign(LEFT, CENTER);
                text("Setup Mode", 50, 25);

                textAlign(CENTER, CENTER);
                text("Press play to begin.", width/2, 575);
            }

            strokeWeight(1);
            stroke(0xffffa600);
            noFill();
            ellipseMode(RADIUS);

            //inset box
            if(!pip || init) {
                rectMode(CORNERS);
                rect(
                    insetBox[0]*movieDims[0] + offset[0],
                    insetBox[1]*movieDims[1] + offset[1],
                    insetBox[2]*movieDims[0] + offset[0],
                    insetBox[3]*movieDims[1] + offset[1]
                );

                ellipse(
                    exitRadial[0]*movieDims[0] + offset[0],
                    exitRadial[1]*movieDims[1] + offset[1],
                    exitRadial[2]*movieDims[0],
                    exitRadial[3]*movieDims[1]
                );
            }

            else {
                rectMode(CENTER);
                rect(width/2, height/2, zoomDims[0], zoomDims[1]);

                ellipse(
                    (exitRadial[0]-insetBox[0])*zoomDims[0]/(insetBox[2]-insetBox[0]) + frameOffset[0],
                    (exitRadial[1]-insetBox[1])*zoomDims[1]/(insetBox[3]-insetBox[1]) + frameOffset[1],
                    exitRadial[2]*zoomDims[0]/(insetBox[2]-insetBox[0]),
                    exitRadial[3]*zoomDims[1]/(insetBox[3]-insetBox[1])
                );
            }

            if(isDrag) {
                if(!selectExit) {
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
        }

        else {
            imageMode(CENTER);
            image(titleImg, width/2, height/2-50);
        }

        //main window border
        stroke(0xff000000);
        noFill();
        rectMode(CORNERS);
        rect(mainBounds[0], mainBounds[1], mainBounds[2], mainBounds[3]);
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
                    exit();
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

            if(listVal == 0) {
                if(!colors.hasValue(editColor)) {
                    colors.append(editColor);

                    String code = Integer.toHexString(editColor);
                    if(code.length() > 6) {
                        code = code.substring(code.length()-6, code.length());
                    }

                    uic.addListItem(code, editColor);
                }
            }

            else if(colors.hasValue(listVal)) {
                colors.set(colors.index(listVal), editColor);

                uic.clearList();
                for(Integer rgbVal : colors) {
                    uic.addListItem(Integer.toHexString(rgbVal), rgbVal);
                }
            }

            break;

        case "removeColor":
            if(listVal != 0 && colors.hasValue(listVal)) {
                colors.remove(colors.index(listVal));

                if(colors.size() == 0) {
                    listVal = 0;
                }

                uic.clearList();
                for(Integer rgbVal : colors) {
                    uic.addListItem(Integer.toHexString(rgbVal), rgbVal);
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
                        movie.stop();
                    }
                }
            }

            else {
                StringBuilder msg = new StringBuilder();

                if(colors.size() == 0) {
                    msg.append(errorMsg[0]);
                }

                if(exitRadial[2] == 0f) {
                    msg.append(errorMsg[1]);
                }

                JOptionPane.showMessageDialog(this, msg.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            }

            break;

        case "stopButton":
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

            insetBox[0] = insetBox[1] = 0f;
            insetBox[2] = insetBox[3] = 1f;

            exitRadial[0] = exitRadial[1] = exitRadial[2] = exitRadial[3] = 0f;

            break;

        case "fastForward":
            if(movie != null) {
                playbackSpeed *= 2;
                if(playbackSpeed > 16) {
                    playbackSpeed = 1;
                }

                movie.speed((float)playbackSpeed);
            }

            break;

        case "colorList":
            listVal = (int)event.getValue();

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
     *
     */
    @Override
    public void exit() {
        if(log != null) {
            log.close();
        }

        super.exit();
    }

    /**
     *
     */
    @Override
    public void mousePressed() {
        if(
            movie != null && init &&
            mouseX > (width-movieDims[0])/2 &&
            mouseX < (width+movieDims[0])/2 &&
            mouseY > (height-movieDims[1])/2 &&
            mouseY < (height+movieDims[1])/2
        ) {
            if(!selectExit) {
                insetBox[0] = insetBox[2] =
                    (float)(mouseX-(width-movieDims[0])/2)/movieDims[0];
                insetBox[1] = insetBox[3] =
                    (float)(mouseY-(height-movieDims[1])/2)/movieDims[1];
            }

            else {
                exitRadial[0] = (float)(mouseX-(width-movieDims[0])/2)/movieDims[0];
                exitRadial[1] = (float)(mouseY-(height-movieDims[1])/2)/movieDims[1];
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

                insetBox[2] = (float)(mouse[0]-(width-movieDims[0])/2)/movieDims[0];
                insetBox[3] = (float)(mouse[1]-(height-movieDims[1])/2)/movieDims[1];
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
        result[0] = exitRadial[0]*movieDims[0] + (width-movieDims[0])/2 - mouse[0];
        //semi-major axis (y)
        result[1] = exitRadial[1]*movieDims[1] + (height-movieDims[1])/2 - mouse[1];

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
                if(insetBox[0] == insetBox[2] || insetBox[1] == insetBox[3]) {
                    insetBox[0] = insetBox[1] = 0f;
                    insetBox[2] = insetBox[3] = 1f;
                }

                else {
                    float tmp;

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
