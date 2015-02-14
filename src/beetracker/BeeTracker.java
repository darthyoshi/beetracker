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
import java.util.Hashtable;
import java.util.Scanner;

import controlP5.Button;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;
import controlP5.Group;
import processing.core.PApplet;
import processing.core.PImage;
import processing.video.Movie;
import weka.clusterers.XMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

@SuppressWarnings("serial")
public class BeeTracker extends PApplet {
    private int[] departureCount;
    private int[] returnCount;
    private Hashtable<Float, Centroid> centroids;
    private ArrayList<Float> colors;
    private int[] newDims;
    static final short[] beeActions = {0, 1}; //0 = depart, 1 = return

    private ControlP5 cp5;
    private Group group;
    private Button openButton;
    private Button colorsButton;

    private Movie movie = null;
private PImage test = null;
    private PImage blobImg;

    private BlobDetectionUtils bdu;

    private XMeans clusterer;
    private Instances dataSet;
    private static final String options[] = {
        "-I", "5",          //max iterations (overall)
        "-L", "1",          //min #clusters
        "-H", "10",         //max #clusters
        "-C", "0.5",        //cutoff factor
        "-D", "weka.core.EuclideanDistance -R first-last", //distance function
        "-S", "5"           //random seed
    };
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

        centroids = new Hashtable<Float,Centroid>();
        colors = new ArrayList<Float>();

        Scanner scan = null;
        try {
            scan = new Scanner(new java.io.File("colors.txt"));

            Float tmp;
            while(scan.hasNext()) {
                tmp = hue(Integer.valueOf(scan.next(), 16));

                centroids.put(tmp, new Centroid());

                colors.add(tmp);
            }
        } catch(NumberFormatException ex) {
            centroids.clear();
            colors.clear();
        } catch(FileNotFoundException e) {}

        background(0x444444);

        cp5 = new ControlP5(this);
        cp5.setFont(cp5.getFont().getFont(), 15);

        group = cp5.addGroup("group").setLabel("").setVisible(true);

        openButton = cp5.addButton("openButton")
            .setSize(120, 20)
            .setPosition(25, 25)
            .setCaptionLabel("Open video file")
            .setGroup(group);
        openButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);

        colorsButton = cp5.addButton("colorsButton")
            .setSize(150, 20)
            .setPosition(150, 25)
            .setCaptionLabel("Set tracking colors")
            .setGroup(group);
        colorsButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);

        //TODO add buttons for control during playback

        blobImg = new PImage(width/4, height/4);

        bdu = new BlobDetectionUtils(width/4, height/4);

        clusterer = new XMeans();

        try {
            clusterer.setOptions(options);

            dataSet = new Instances(new java.io.BufferedReader(
                new java.io.FileReader("header.arff"))
            );
        } catch(Exception e) {
            try {
                writer.append(e.getMessage()).append('\n');
                writer.flush();
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                exit();
            }
        }
    }

    @Override
    public void draw() {
        textSize(32);
        textAlign(CENTER);
        fill(0xFFFF0099);

   //     if(movie != null) {
       /*     if(movie.available()) {
                movie.read();
            }*/if(test != null) {

            image(/*movie*/test, 0, 0, width, height);

            newDims = resizeDims(/*movie*/test);

            blobImg.copy(
             /*   movie*/test,
                0, 0, test/*movie*/.width, test/*movie*/.height,
                0, 0, /*newDims[0]*/blobImg.width, /*newDims[1]*/blobImg.height
            );

            BlobDetectionUtils.preProcessImg(this, blobImg, colors);

            image(blobImg, 0, 0, blobImg.width*4, blobImg.height*4);

            bdu.computeBlobs(blobImg.pixels);

            bdu.drawEdges(this);

            ArrayList<float[]> points = bdu.getCentroids();
            clusters = getClusters(points);
 //           updateCentroids();

            text("#bees: " + clusters.size(), width/2, 50);
        }

        if(colors.isEmpty()) {
            text("No colors selected!", width/2, height/2);
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
   /*         String videoPath = VideoBrowser.getVideoName(this);

            if(videoPath != null) {
//                movie = new Movie(this, videoName);
//                movie.play();
*/
                group.setVisible(!group.isVisible());
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
        }
    }

    /**
     * Calculates the dimensions of a resized PImage.
     * @param img the PImage to resize
     * @return an array containing the new dimensions
     */
    private int[] resizeDims(PImage img) {
        int[] result = new int[2];

        float aspect = 1f * img.width / img.height;

        //scale by width
        result[0] = blobImg.width;
        result[1] = (int)(result[0] * aspect);

        //scale by height if necessary
        if(result[1] > blobImg.height) {
            result[1] = blobImg.height;
            result[0] = (int)(result[1] / aspect);
        }

        return result;
    }

    /**
     * Uses the X-means algorithm to determine the
     * @param points an ArrayList containing the points to process.
     * @return
     */
    private ArrayList<ArrayList<int[]>> getClusters(ArrayList<float[]> points) {
        ArrayList<ArrayList<int[]>> result = null;
        Instance row;
        float[] point;
        int[] tmp;
        int i;

        //clear old points from data set
        dataSet.delete();

        //add new points to data set
        for(i = 0; i < points.size(); i++) {
            point = points.get(i);

            row = new SparseInstance(2);
            row.setValue(0, point[0]*width);
            row.setValue(1, point[1]*height);
            row.setDataset(dataSet);

            dataSet.add(row);
        }

        try {
            //invoke weka XMeans clusterer with Instances
            clusterer.buildClusterer(dataSet);

            //create list of clusters
            int numClusters = clusterer.numberOfClusters();
            result = new ArrayList<ArrayList<int[]>>(numClusters);
            for(i = 0; i < numClusters; i++) {
                result.add(new ArrayList<int[]>());
            }

            for(i = 0; i < dataSet.numInstances(); i++) {
                row = dataSet.instance(i);
                point = points.get(i);

                //group points in a cluster together in list
                tmp = new int[2];
                tmp[0] = (int)row.value(0);
                tmp[1] = (int)row.value(1);
                result.get(clusterer.clusterInstance(row)).add(tmp);
            }
        } catch (Exception e) {
            try {
                writer.append(e.getMessage()).append('\n');
                writer.flush();
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                exit();
            }
        }

        return result;
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
     * @param points
     */
    private void updateCentroids() {
        int pixel;
        float hueVal = -1, tmpHue;
        Centroid centroid;
        double[] point;
        ArrayList<int[]> cluster;
        Instances centers = clusterer.getClusterCenters();
        Instance center;

        colorMode(HSB, 255);

        blobImg.loadPixels();

        for(int i = 0; i < centers.numInstances(); i++) {
            center = centers.instance(i);
            point = new double[2];

            point[0] = center.value(0);
            point[1] = center.value(1);

            //grab centroid pixel from blob image
            pixel = blobImg.pixels[
                (int)(point[1]*blobImg.height*blobImg.width/height) +
                (int)(point[0]*blobImg.width/width)
            ];
            //-case: centroid is not in a blob (pixel is black)
            if(brightness(pixel) == 0) {
                cluster = clusters.get(i);

                //iterate through cluster centroids for valid hue
                for(int[] tmp : cluster) {
                    hueVal = (int)(((float)(tmp[1]))*blobImg.height*
                        blobImg.width/height) +
                        (int)(((float)(tmp[0]))*blobImg.width/width);

                    if(brightness(pixel) > 0) {
                        hueVal = hue(pixel);

                        break;
                    }
                }
            }
            //-case: centroid is in a blob (pixel has valid hue)
            else {
                hueVal = hue(pixel);
            }

            //update centroid position
            centroid = centroids.get(hueVal);
            if(centroid == null) {
                centroid = new Centroid();
                centroids.put(hueVal, centroid);
            }
            centroid.x = (int)(point[0]*width);
            centroid.y = (int)(point[1]*height);
        }
    }

    /**
     * Main method for executing BeeTracker as a Java application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PApplet.main(new String[] { beetracker.BeeTracker.class.getName() });
    }

    private class Centroid {
        float x, y;
        boolean updated = false;
    }
}
