/**
 * @file DataMinerUtils.java
 * @author Kay Choi, 909926828
 * @date 14 Feb 15
 * @description Handles all BeeTracker clustering-related operations.
 */

package beetracker;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;

import processing.core.PApplet;
import processing.core.PConstants;

import weka.clusterers.XMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class DataMinerUtils {
    private Hashtable<Float, Cluster> centroids;

    private PrintStream log;

    private PApplet parent;

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

    /**
     * Class constructor.
     * @param parent the instantiating PApplet
     * @param log a PrintStream object pointing to the output log
     * @param colors the list of colors to scan for
     */
    public DataMinerUtils(PApplet parent, PrintStream log, ArrayList<Integer> colors) {
        this.parent = parent;
        this.log = log;

        clusterer = new XMeans();

        try {
            clusterer.setOptions(options);

            dataSet = new Instances(new java.io.BufferedReader(
                new java.io.FileReader("header.arff"))
            );
        } catch(Exception e) {
            e.printStackTrace(log);
            parent.exit();
        }

        centroids = new Hashtable<Float, Cluster>();

        ArrayList<Float> hues = new ArrayList<Float>(colors.size());

        for(Integer color : colors) {
            hues.add(parent.hue(color));
        }

        initColors(hues);
    }


    /**
     * Uses the X-means algorithm to sort a set of points into clusters.
     * @param points an ArrayList containing the points to process.
     * @return an ArrayList of clusters, where each cluster is an ArrayList of
     *   integer coordinates
     */
    public ArrayList<Cluster> getClusters(ArrayList<float[]> points) {
        ArrayList<Cluster> result = new ArrayList<Cluster>();
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
            row.setValue(0, point[0]*parent.width);
            row.setValue(1, point[1]*parent.height);
            row.setDataset(dataSet);

            dataSet.add(row);
        }

        try {
            if(!points.isEmpty()) {
                //invoke weka XMeans clusterer with Instances
                clusterer.buildClusterer(dataSet);

                //populate list of clusters
                for(i = 0; i < clusterer.numberOfClusters(); i++) {
                    result.add(new Cluster());
                }
            }
            result.trimToSize();

            for(i = 0; i < dataSet.numInstances(); i++) {
                row = dataSet.instance(i);

                //group points in a cluster together in list
                tmp = new int[2];
                tmp[0] = (int)row.value(0);
                tmp[1] = (int)row.value(1);
                result.get(clusterer.clusterInstance(row)).addPoint(tmp);
            }

            for(Cluster c : result) {
            	c.calcBounds();
            }
        } catch (Exception e) {
            e.printStackTrace(log);
            parent.exit();
        }

        return result;
    }

    /**
     * TODO add method header
     * @param blobImg
     * @param clusters
     */
    public void updateCentroids(processing.core.PImage blobImg, ArrayList<Cluster> clusters) {
        int pixel;
        float hueVal = -1;
        Cluster centroid;
        double[] point;
        Cluster cluster;
        Instances centers = clusterer.getClusterCenters();
        Instance center;

        parent.colorMode(PConstants.HSB, 255);

        blobImg.loadPixels();

        if(centers != null) {
            for(int i = 0; i < centers.numInstances(); i++) {
                center = centers.instance(i);
                point = new double[2];

                point[0] = center.value(0);
                point[1] = center.value(1);

                //grab centroid pixel from blob image
                pixel = blobImg.pixels[
                    (int)(point[1]*blobImg.height*blobImg.width/parent.height) +
                    (int)(point[0]*blobImg.width/parent.width)
                ];
                //-case: centroid is not in a blob (pixel is black)
                if(parent.brightness(pixel) == 0) {
                    //TODO need to handle case where #clusters!=#centers
                	cluster = clusters.get(i);

                    //iterate through cluster centroids for valid hue
                    for(int[] tmp : cluster.getPoints()) {
                        hueVal = (int)(((float)(tmp[1]))*blobImg.height*
                            blobImg.width/parent.height) +
                            (int)(((float)(tmp[0]))*blobImg.width/parent.width);

                        if(parent.brightness(pixel) > 0) {
                            hueVal = parent.hue(pixel);

                            break;
                        }
                    }
                }
                //-case: centroid is in a blob (pixel has valid hue)
                else {
                    hueVal = parent.hue(pixel);
                }

                //update centroid position
                centroid = centroids.get(hueVal);
                if(centroid == null) {
                    centroid = new Cluster();
                    centroids.put(hueVal, centroid);
                }
                centroid.setX((int)(point[0]*parent.width));
                centroid.setY((int)(point[1]*parent.height));
            }
        }
    }

    /**
     * TODO add method header
     * @param colors
     */
    public final void initColors(ArrayList<Float> colors) {
        centroids.clear();

        for(Float color: colors) {
            centroids.put(color, new Cluster());
        }
    }
}