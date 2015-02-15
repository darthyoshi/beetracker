/**
 * @file BeeTracker.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description A tool for tracking bees in a video.
 */

package beetracker;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import processing.core.PApplet;
import processing.core.PImage;
import processing.video.Movie;
import controlP5.ControlEvent;

@SuppressWarnings("serial")
public class BeeTracker extends PApplet {
    private ArrayList<Float> colors;
    private int[] departureCount;
    private int[] returnCount;
    private boolean isPlaying = false, init = false, pip = false;
    private static final int[] mainBounds = {50, 50, 750, 550};

    private float[] dragBox;
    private boolean isDrag = false;

    private Movie movie = null;
private PImage test = null;
    private PImage blobImg;

    private BlobDetectionUtils bdu;

    private DataMinerUtils dmu;

    private UIControl uic;

    private ArrayList<ArrayList<int[]>> clusters;

    private BufferedWriter writer = null;

    @Override
    public void setup() {
        size(800, 600);
        frameRate(30);

        try {
            writer = new BufferedWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("Console.log"))
            );
        } catch(IOException e) {
            e.printStackTrace();
            exit();
        }

        Scanner scan = null;
        colors = new ArrayList<Float>();
        try {
            scan = new Scanner(new java.io.File("colors.txt"));

            Float tmp;
            while(scan.hasNext()) {
                tmp = hue(Integer.valueOf(scan.next(), 16));

                colors.add(tmp);
            }
        } catch(NumberFormatException ex) {
            colors.clear();
        } catch(FileNotFoundException e) {}

        uic = new UIControl(this);

        dmu = new DataMinerUtils(this, writer, colors);

        background(0x444444);

        blobImg = createImage(width/4, height/4, RGB);

        bdu = new BlobDetectionUtils(width/4, height/4);

        dragBox = new float[4];
        dragBox[0] = dragBox[1] = 0f;
        dragBox[2] = dragBox[3] = 1f;
    }

    @Override
    public void draw() {
        background(0x222222);

        noStroke();
        fill(0xff444444);
        rectMode(CORNERS);
        rect(mainBounds[0], mainBounds[1], mainBounds[2], mainBounds[3]);

        textSize(32);
        textAlign(CENTER);
        fill(0xFFFF0099);

        if(/*movie*/test != null) {
       /*     if((isPlaying || init) && movie.available()) {
                movie.read();

                if(init) {
                    movie.stop();
                    init = false;
                }
            }*/

            imageMode(CENTER);
            image(/*movie*/test, width/2, height/2, width-100, height-100);

            if(!init) {
                blobImg.copy(
                 /*   movie*/test,
                    (int)(/*movie*/test.width*dragBox[0]),
                    (int)(/*movie*/test.height*dragBox[1]),
                    (int)(/*movie*/test.width*dragBox[2]),
                    (int)(/*movie*/test.height*dragBox[3]),
                    0, 0, blobImg.width, blobImg.height
                );

                BlobDetectionUtils.preProcessImg(this, blobImg, colors);

                bdu.computeBlobs(blobImg.pixels);

                if(pip) {
                    image(blobImg, width/2, height/2, width-100, height-100);
                }

                bdu.drawEdges(this, pip, dragBox);

                clusters = dmu.getClusters(bdu.getCentroids());

     //           dm.updateCentroids(blobImg, clusters);

                text("#bees: " + clusters.size(), width/2, 50);
            }
        }

        if(colors.isEmpty()) {
            text("No colors selected!", width/2, height/2);
        }

        strokeWeight(1);
        noFill();

        //inset box
        stroke(0xffff0505);
        rectMode(CORNERS);
        rect(
            dragBox[0]*(width-100)+50, dragBox[1]*(height-100)+50,
            dragBox[2]*(width-100)+50, dragBox[3]*(height-100)+50
        );
        if(isDrag) {
            line(
                dragBox[0]*(width-100)+50, dragBox[1]*(height-100)+50,
                dragBox[2]*(width-100)+50, dragBox[3]*(height-100)+50
            );
            line(
                dragBox[0]*(width-100)+50, dragBox[3]*(height-100)+50,
                dragBox[2]*(width-100)+50, dragBox[1]*(height-100)+50
            );
        }

        //main window border
        stroke(0xff000000);
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
   /*         String videoPath = VideoBrowser.getVideoName(this);

            if(videoPath != null) {
//                movie = new Movie(this, videoName);
//                movie.play();
                init = true;
*/
                uic.toggleGroup();
                uic.togglePlay();
/*
                try {
                    writer.append(videoPath);
                    writer.flush();
                } catch(IOException ex) {
                    ex.printStackTrace();
                    exit();
                }
            }*/
test = this.loadImage("test.jpg");
            break;

        case "colorsButton":


            break;

        case "playButton":
            isPlaying = !isPlaying;

            if(movie != null) {
                movie.stop();
            }

            break;

        case "stopButton":
            isPlaying = false;

            if(/*movie*/test != null) {
//              movie.stop();
                test = null;
            }

            uic.toggleGroup();
            uic.togglePlay();

            break;
        }
    }

    @Override
    public void exit() {
        if(writer != null) {
            try {
                writer.close();
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }

        super.exit();
    }

    /**
     * Hnndler for mouse press.
     * @param mouseX the x-coordinate of the mouse
     * @param mouseY the y-coordinate of the mouse
     */
    public void mousePressed(int mouseX, int mouseY) {
        if(mouseX > mainBounds[0] && mouseX < mainBounds[2] &&
            mouseY > mainBounds[1] && mouseY < mainBounds[3])
        {
            dragBox[0] = dragBox[2] = (mouseX-50)/700f;
            dragBox[1] = dragBox[3] = (mouseY-50)/500f;

            isDrag = true;
        }
    }

    /**
     * Handler for mouse drag.
     * @param mouseX the x-coordinate of the mouse
     * @param mouseY the y-coordinate of the mouse
     */
    public void mouseDragged(int mouseX, int mouseY) {
        if(isDrag) {
            int tmp[] = constrainMouse(mouseX, mouseY);

            dragBox[2] = (tmp[0]-50)/700f;
            dragBox[3] = (tmp[1]-50)/500f;
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

        if(mouseX < mainBounds[0]) {
            result[0] = mainBounds[0];
        }

        else if(mouseX > mainBounds[2]) {
            result[0] = mainBounds[2];
        }

        else {
            result[0] = mouseX;
        }

        if(mouseY < mainBounds[1]) {
            result[1] = mainBounds[1];
        }

        else if(mouseY > mainBounds[3]) {
            result[1] = mainBounds[3];
        }

        else {
            result[1] = mouseY;
        }

        return result;
    }

    /**
     * Handler for mouse release.
     * @param mouseX the x-coordinate of the mouse
     * @param mouseY the y-coordinate of the mouse
     */
    public void mouseReleased(int mouseX, int mouseY) {
        if(isDrag) {
            if(dragBox[0] == dragBox[2] || dragBox[1] == dragBox[3]) {
                dragBox[0] = dragBox[1] = 0f;
                dragBox[2] = dragBox[3] = 1f;
            }

            isDrag = false;
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
