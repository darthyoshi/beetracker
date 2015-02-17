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
import java.util.ArrayList;
import java.util.Scanner;

import controlP5.ControlEvent;

import processing.core.PApplet;
import processing.core.PImage;
import processing.video.Movie;

@SuppressWarnings("serial")
public class BeeTracker extends PApplet {
    private ArrayList<Integer> colors;
    private ArrayList<Integer> departureCount;
    private ArrayList<Integer> returnCount;
    private boolean isPlaying = false, init = false, pip = false;
    private static final int[] mainBounds = {50, 50, 750, 550};
    private short playbackSpeed = 1;
    private int listVal = 0;
    private int[] newDims;

    private File currentDir = null;

    private float[] dragBox;
    private boolean isDrag = false;

    private Movie movie = null;
private PImage test = null;
    private PImage blobImg;

    private BlobDetectionUtils bdu;

    private DataMinerUtils dmu;

    private UIControl uic;

    private ArrayList<ArrayList<int[]>> clusters;

    private PrintStream log = null;

    private processing.core.PFont font;

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
            e1.printStackTrace(log);
            exit();
        }

        uic = new UIControl(this);

        Scanner scan = null;
        colors = new ArrayList<Integer>();
        try {
            scan = new Scanner(new File("colors.txt"));

            int rgbVal;
            String color, header = "ff";
            while(scan.hasNext()) {
                color = scan.next();
                rgbVal = (int)Long.parseLong(header + color, 16);

                uic.addListItem(color, rgbVal);

                colors.add(rgbVal);
            }
        } catch(NumberFormatException ex) {
            colors.clear();
            ex.printStackTrace(log);
        } catch(FileNotFoundException e) {
            e.printStackTrace(log);
        } finally {
            if(scan != null) {
                scan.close();
            }
        }

        dmu = new DataMinerUtils(this, log, colors);

        background(0x444444);

        blobImg = createImage(width/4, height/4, RGB);

        bdu = new BlobDetectionUtils(width/4, height/4);

        dragBox = new float[4];
        dragBox[0] = dragBox[1] = 0f;
        dragBox[2] = dragBox[3] = 1f;

        font = this.createDefaultFont(12);
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

        if(movie/*test*/ != null) {
            if((isPlaying || init) && movie.available()) {
                movie.read();

                if(init) {
                    movie.stop();
                }
            }

            imageMode(CENTER);
            newDims = scaledDims(movie/*test*/.width, movie/*test*/.height);
            image(movie/*test*/, width/2, height/2, newDims[0], newDims[1]);

            if(!init) {
                blobImg.resize(
                    (int)(movie/*test*/.width*(dragBox[2] - dragBox[0])),
                    (int)(movie/*test*/.height*(dragBox[3] - dragBox[1]))
                );
                blobImg.copy(
                    movie/*test*/,
                    (int)(movie/*test*/.width*dragBox[0]),
                    (int)(movie/*test*/.height*dragBox[1]),
                    (int)(movie/*test*/.width*(dragBox[2] - dragBox[0])),
                    (int)(movie/*test*/.height*(dragBox[3] - dragBox[1])),
                    0, 0, blobImg.width, blobImg.height
                );

                BlobDetectionUtils.preProcessImg(this, blobImg, colors);

                bdu.computeBlobs(blobImg.pixels);

                if(pip) {
                    blobImg.copy(
                        movie/*test*/,
                        (int)(movie/*test*/.width*dragBox[0]),
                        (int)(movie/*test*/.height*dragBox[1]),
                        (int)(movie/*test*/.width*(dragBox[2] - dragBox[0])),
                        (int)(movie/*test*/.height*(dragBox[3] - dragBox[1])),
                        0, 0, blobImg.width, blobImg.height
                    );
                    newDims = scaledDims(blobImg.width, blobImg.height);
                    image(blobImg, width/2, height/2, newDims[0], newDims[1]);

                    stroke(0xffff0505);
                    rectMode(CENTER);
                    noFill();
                    rect(width/2, height/2, newDims[0], newDims[1]);
                }

                bdu.drawEdges(this, pip, dragBox);

                clusters = dmu.getClusters(bdu.getCentroids());

                dmu.updateCentroids(blobImg, clusters);

                textSize(32);
                textAlign(CENTER, CENTER);
                text("#bees: " + clusters.size(), width/2, 25);

                textAlign(RIGHT, CENTER);
                text("current speed: "+playbackSpeed+'x', 750, 575);
            }

            else {
                textSize(16);
                textAlign(CENTER, CENTER);
                text(
                    "Drag the mouse to define the area to process.\n" +
                    "The entire image will be processed by default.",
                    width/2, 25
                );
                text("Press play to begin.", width/2, 575);
            }

            strokeWeight(1);
            noFill();

            //inset box
            stroke(0xffff0505);
            rectMode(CORNERS);

            if(!pip || init) {
                rect(
                    dragBox[0]*newDims[0]+(width-newDims[0])/2,
                    dragBox[1]*newDims[1]+(height-newDims[1])/2,
                    dragBox[2]*newDims[0]+(width-newDims[0])/2,
                    dragBox[3]*newDims[1]+(height-newDims[1])/2
                );
            }
            if(isDrag) {
                line(
                    dragBox[0]*newDims[0]+(width-newDims[0])/2,
                    dragBox[1]*newDims[1]+(height-newDims[1])/2,
                    dragBox[2]*newDims[0]+(width-newDims[0])/2,
                    dragBox[3]*newDims[1]+(height-newDims[1])/2
                );
                line(
                    dragBox[0]*newDims[0]+(width-newDims[0])/2,
                    dragBox[3]*newDims[1]+(height-newDims[1])/2,
                    dragBox[2]*newDims[0]+(width-newDims[0])/2,
                    dragBox[1]*newDims[1]+(height-newDims[1])/2
                );
            }
        }

        else {
            //TODO title elements
        }

        if(colors.isEmpty()) {
            textSize(28);
            textAlign(CENTER, CENTER);
            text("No colors selected. Please choose a color.", width/2, height/2);
        }

        //main window border
        stroke(0xff000000);
        noFill();
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

                uic.toggleOpenButton();
                uic.togglePlay();

                log.append("loaded ").append(videoPath).append('\n');
                log.flush();
            }
//test = this.loadImage("test.jpg");
            break;

        case "editColor":
            int color = ColorPicker.getColor(this);

            if(listVal == 0) {
                if(!colors.contains(color)) {
                    colors.add(color);

                    String code = Integer.toHexString(color);
                    if(code.length() > 6) {
                        code = code.substring(code.length()-6, code.length());
                    }

                    uic.addListItem(code, color);

                    dmu.initColors(getHues());
                }
            }

            else if(colors.contains(listVal)) {
                colors.set(colors.indexOf(listVal), color);

                uic.clearList();
                for(Integer rgbVal : colors) {
                    uic.addListItem(Integer.toHexString(rgbVal), rgbVal);
                }

                dmu.initColors(getHues());
            }

            break;

        case "removeColor":
            if(listVal != 0 && colors.contains(listVal)) {
                colors.remove(colors.indexOf(listVal));

                if(colors.isEmpty()) {
                    listVal = 0;
                }

                uic.clearList();
                for(Integer rgbVal : colors) {
                    uic.addListItem(Integer.toHexString(rgbVal), rgbVal);
                }

                dmu.initColors(getHues());
            }

            break;

        case "playButton":
            if(!colors.isEmpty()) {
                isPlaying = ((controlP5.Toggle)event.getController()).getState();

                if(init) {
                    uic.toggleColors();

                    init = false;
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

            break;

        case "stopButton":
            if(isPlaying) {
                uic.togglePlayState();
            }

            if(movie/*test*/ != null) {
                movie.stop();
                movie/*test*/ = null;
            }

            uic.toggleColors();
            uic.togglePlay();

            dragBox[0] = dragBox[1] = 0f;
            dragBox[2] = dragBox[3] = 1f;

            break;

        case "fastForward":
            if(movie != null) {
                playbackSpeed *= 2;
                if(playbackSpeed > 16) {
                    playbackSpeed = 1;
                }

                movie.frameRate(playbackSpeed*frameRate);
            }

            break;

        case "colorList":
            listVal = (int)event.getValue();

            break;

        case "pipToggle":
            pip = !pip;

            break;
        }
    }

    /**
     * TODO add method header
     * @return
     */
    private ArrayList<Float> getHues() {
        ArrayList<Float> result = new ArrayList<Float>(colors.size());

        for(Integer color : colors) {
            result.add(hue(color));
        }

        return result;
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
        if(movie != null) {
            if(mouseX > (width-newDims[0])/2 && mouseX < (width+newDims[0])/2 &&
                mouseY > (height-newDims[1])/2 && mouseY < (height+newDims[1])/2 && init)
            {
                dragBox[0] = dragBox[2] = (float)(mouseX-(width-newDims[0])/2)/newDims[0];
                dragBox[1] = dragBox[3] = (float)(mouseY-(height-newDims[1])/2)/newDims[1];

                isDrag = true;
            }
        }
    }

    /**
     *
     */
    @Override
    public void mouseDragged() {
        if(isDrag) {
            int tmp[] = constrainMouse(mouseX, mouseY);

            dragBox[2] = (float)(tmp[0]-(width-newDims[0])/2)/newDims[0];
            dragBox[3] = (float)(tmp[1]-(height-newDims[1])/2)/newDims[1];

        }
    }

    /**
     * Constrains the mouse coordinates within the item window.
     * @param mouseX the x-coordinate of the mouse
     * @param mouseY the y-coordinate of the mouse
     * @return an integer array containing the adjusted coordinates
     */
    private int[] constrainMouse(int mouseX, int mouseY) {
        int[] result = new int[2];

        if(mouseX < (width-newDims[0])/2) {
            result[0] = (width-newDims[0])/2;
        }

        else if(mouseX > (width+newDims[0])/2) {
            result[0] = (width+newDims[0])/2;
        }

        else {
            result[0] = mouseX;
        }

        if(mouseY < (height-newDims[1])/2) {
            result[1] = (height-newDims[1])/2;
        }

        else if(mouseY > (height+newDims[1])/2) {
            result[1] = (height+newDims[1])/2;
        }

        else {
            result[1] = mouseY;
        }

        return result;
    }

    /**
     *
     */
    @Override
    public void mouseReleased() {
        if(isDrag) {
            if(dragBox[0] == dragBox[2] || dragBox[1] == dragBox[3]) {
                dragBox[0] = dragBox[1] = 0f;
                dragBox[2] = dragBox[3] = 1f;
            }

            else {
                float tmp;

                if(dragBox[0] > dragBox[2]) {
                    tmp = dragBox[0];
                    dragBox[0] = dragBox[2];
                    dragBox[2] = tmp;
                }

                if(dragBox[1] > dragBox[3]) {
                    tmp = dragBox[1];
                    dragBox[1] = dragBox[3];
                    dragBox[3] = tmp;
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
    private int[] scaledDims(int imgWidth, int imgHeight) {
        int[] result = new int[2];

        float ratio = (float)imgWidth/imgHeight;

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
