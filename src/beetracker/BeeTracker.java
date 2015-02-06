/**
 * @file BeeTracker.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description A tool for tracking bees in a video.
 */

package beetracker;

import java.io.BufferedWriter;
import java.util.Hashtable;
import java.util.Scanner;

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
import weka.clusterers.XMeans;
import weka.core.Instances;
import weka.core.SparseInstance;

public class BeeTracker extends PApplet {
    private int[] departureCount;
    private int[] returnCount;
    private Hashtable<Integer,Centroid> centroids;
    private ArrayList<Integer> colors;
    private int[] newDims;
    static final short[] beeActions = {0, 1}; //0 = depart, 1 = return

    private VideoBrowser fb;

    private ControlP5 cp5;
    private Button openButton;
    private Button colorsButton;

    private Movie movie = null;
    private PImage blobImg;

    private BlobDetectionUtils bdu;

    private XMeans clusterer;
    private Instances dataSet;
    private static final String options = {
        "-I", "5",          //max iterations (overall)
        "-L", "1",          //min #clusters
        "-H", "10",         //max #clusters
        "-C", "0.5",        //cutoff factor
        "-D", "weka.core.EuclideanDistance -R first-last", //distance function
        "-S", "5"           //random seed
    };
    private ArrayList<int[]>[] clusters;

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

        centroids = new Hashtable<Integer,Centroid>();
        colors = new ArrayList<Integer>();

        Scanner scan = null;
        try {
            scan = new Scanner(new java.io.File("hues.txt"));

            Integer tmp;
            while(scan.hasNext()) {
                tmp = Integer.valueOf(scan.next());

                centroids.add(tmp, null);

                colors.add(tmp);
            }
        } catch(NumberFormatException ex) {
            centroids.clear();
            colors.clear();
        } catch(FileNotFoundException e) {}

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

            image(blobImg, 0, 0, blobImg.width*4, blobImg.height*4);

            bdu.computeBlobs(blobImg.pixels);

            bdu.drawEdges(this);

            ArrayList<float[]> points = bdu.getCentroids();
            clusters = getClusters(points);
            updateCentroids(points);

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
            String videoPath = VideoBrowser.getVideoName(this);

            if(videoPath != null) {
//                movie = new Movie(this, videoName);
//                movie.play();
                println(videoPath);
            }

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

        float aspect = 1.0 * img.width / img.height;

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
    private ArrayList<int[]>[] getClusters(ArrayList<float[]> points) {
        ArrayList<int[]>[] result;
        SparseInstance row;
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
            result = new ArrayList<int[]>[numClusters];
            for(i = 0; i < numClusters; i++) {
                result[i] = new ArrayList<int[]>();
            }

            for(i = 0; i < dataSet.numInstances(); i++) {
                row = dataSet.instance(i);

                //group points in a cluster together in list
                tmp = new int[2];
                tmp[0] = (int)row.value(0);
                tmp[1] = (int)row.value(1);
                result[clusterer.clusterInstance(row)].add(tmp);
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
    private void updateCentroids(ArrayList<float[]> points) {
/*        int hueVal;
        Centroid centroid;
        float[] point;

        blobImg.loadPixels();

        for(int i = 0; i < points.size(); i++) {
            point = points.get(i);

            //grab centroid pixel from blob image
            hueVal = blobImg.pixels[
                (int)(point[1]*blobImg.height) * blobImg.width + 
                (int)(point[0]*blobImg.width)
            ]
            //-case: centroid is not in a blob (pixel is black)
            if(brightness(hueVal) == 0) {
                //iterate through cluster centroids for valid hue
            }
            //-case: centroid is in a blob (pixel has valid hue)
            else {
                hueVal = hue(hueVal);
            }

            //update centroid position
            centroid = centroids.get(hueVal);
            if(centroid == null) {
                centroid = new Centroid();
                centroids.add(hueVal, centroid);
            }
            centroid.x = (int)(point[0]*width);
            centroid.y = (int)(point[1]*height);
        }
*/
    }

    /**
     * Main method for executing BeeTracker as a Java application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PApplet.main(new String[] { beetracker.BeeTracker.class.getName() });
    }
    
    private class Centroid {
        int x, y;
        boolean updated = false;
    }
}
