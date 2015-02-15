/**
 * @file DataMiner.java
 * @author Kay Choi, 909926828
 * @date 14 Feb 15
 * @description
 */

package beetracker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import processing.core.PApplet;
import processing.core.PConstants;
import weka.clusterers.XMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class DataMinerUtils {
    private Hashtable<Float, Centroid> centroids;

    private BufferedWriter writer;

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

    public DataMinerUtils(PApplet parent, BufferedWriter writer, ArrayList<Float> colors) {
        this.parent = parent;
        this.writer = writer;

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
                parent.exit();
            }
        }

        centroids = new Hashtable<Float, Centroid>();
        for(Float color: colors) {
            centroids.put(color, new Centroid());
        }
    }


    /**
     * Uses the X-means algorithm to determine the
     * @param points an ArrayList containing the points to process.
     * @return
     */
    public ArrayList<ArrayList<int[]>> getClusters(ArrayList<float[]> points) {
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
            row.setValue(0, point[0]*parent.width);
            row.setValue(1, point[1]*parent.height);
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
                parent.exit();
            }
        }

        return result;
    }

    /**
     *
     * @param clusters
     * @param parent
     * @param bolbImg
     */
    public void updateCentroids(processing.core.PImage blobImg, ArrayList<ArrayList<int[]>> clusters) {
        int pixel;
        float hueVal = -1;
        Centroid centroid;
        double[] point;
        ArrayList<int[]> cluster;
        Instances centers = clusterer.getClusterCenters();
        Instance center;

        parent.colorMode(PConstants.HSB, 255);

        blobImg.loadPixels();

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
                cluster = clusters.get(i);

                //iterate through cluster centroids for valid hue
                for(int[] tmp : cluster) {
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
                centroid = new Centroid();
                centroids.put(hueVal, centroid);
            }
            centroid.x = (int)(point[0]*parent.width);
            centroid.y = (int)(point[1]*parent.height);
        }
    }

    /**
     *
     * @author Kay Choi
     *
     */
    private class Centroid {
        float x, y;
        boolean updated = false;
    }
}