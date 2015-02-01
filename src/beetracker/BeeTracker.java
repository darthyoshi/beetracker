/**
 * @file BeeTracker.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description A tool for tracking bees in a video.
 */

 package beetracker;

import blobDetection.Blob;
import blobDetection.BlobDetection;
import blobDetection.EdgeVertex;
import controlP5.Button;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;
import processing.core.PApplet;
import processing.core.PImage;
import processing.video.Movie;
import weka.clusterers.SimpleKMeans;

public class BeeTracker extends PApplet {
    private int[] departureCount;
    private int[] returnCount;
    private static int numColors;
    private int[] colors;
    private int[] newDims;
    static final short[] beeActions = {0, 1}; //0 = depart, 1 = return

    private VideoBrowser fb;

    private ControlP5 cp5;
    private Button openButton;
    private Button colorsButton;

    private Movie movie = null;
    private PImage blobImg;

    private BlobDetectionUtils bdu;

    @Override
    public void setup() {
        size(800, 600);
        frameRate(30);
        background(0x444444);

        cp5 = new ControlP5(this);
        cp5.setFont(cp5.getFont().getFont(), 15);

        openButton = cp5.addButton("openButton")
            .setSize(120, 20)
            .setPosition(25, 25)
            .setCaptionLabel("Open video file");
        openButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);

        colorsButton = cp5.addButton("colorsButton")
            .setSize(150, 20)
            .setPosition(150, 25)
            .setCaptionLabel("Set tracking colors");
        colorsButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);

        blobImg = new PImage(width/4, height/4);

        bdu = new BlobDetectionUtils(width/4, height/4);
    }

    @Override
    public void draw() {
        if(movie != null) {
            if(movie.available()) {
                movie.read();
            }

//            image(movie, 0, 0, width, height);

            newDims = resizeDims(movie);

            blobImg.copy(
                movie,
                0, 0, movie.width, movie.height,
                0, 0, newDims[0], newDims[1]
            );

            BlobDetectionUtils.preProcessImg(this, blobImg, colors);

            image(blobImg, 0, 0, blobImg.width * 4, blobImg.height * 4);

            bdu.computeBlobs(blobImg.pixels);
            
            bdu.drawEdges(this);
        }
    }

    /**
     * Callback method for handling ControlP5 UI events.
     * @param event the initiating ControlEvent
     */
    public void controlEvent(ControlEvent event) {
        String eventName = event.getName();

        switch(eventName) {
        case "openButton":
            String videoPath = VideoBrowser.getVideoName(this);

            if(videoPath != null) {
//              movie = new Movie(this, videoName);
//              movie.play();
                println(videoPath);
            }

            break;

        case "colorsButton":

        }
    }

    /**
     * Calculates the dimensions of a resized PImage.
     * @param img the PImage to resize
     * @return an array containing the new dimensions
     */
    private int[] resizeDims(PImage img) {
        int[] result = new int[2];

        float aspect = 1.0 * img.width / img.height;

        result[0] = blobImg.width;
        result[1] = (int)(result[0] * aspect);

        if(result[1] > blobImg.height) {
            result[1] = blobImg.height;
            result[0] = (int)(result[1] / aspect);
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
