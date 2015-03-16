/**
 * @file DataMinerUtils.java
 * @author Kay Choi, 909926828
 * @date 14 Feb 15
 * @description Handles all BeeTracker clustering-related operations.
 */

package beetracker;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import processing.core.PApplet;
import processing.core.PConstants;

import weka.clusterers.XMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class DataMinerUtils {
    private final PrintStream log;

    private final PApplet parent;

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
     * @param log the output log
     * @param header the File object containing the Weka header definitions
     */
    public DataMinerUtils(PApplet parent, PrintStream log, java.io.File header) {
        this.parent = parent;
        this.log = log;

        clusterer = new XMeans();

        try {
            clusterer.setOptions(options);

            dataSet = new Instances(new java.io.BufferedReader(
                new java.io.FileReader(header))
            );
        } catch(Exception e) {
            e.printStackTrace(log);
            parent.exit();
        }
    }


    /**
     * Uses the X-means algorithm to sort a set of points into clusters.
     * @param points an List containing the normalized points to process.
     * @return an LinkedList of Cluster objects
     */
    public List<Cluster> getClusters(List<float[]> points) {
        List<Cluster> result = null;
        Instance row;
        double[] tmp;
        int i;

        //clear old points from data set
        dataSet.delete();

        //add new points to data set
        for(float[] point : points) {
            row = new SparseInstance(2);
            row.setValue(0, point[0]*parent.width);
            row.setValue(1, point[1]*parent.height);
            row.setDataset(dataSet);

            dataSet.add(row);
        }

        try {
            if(points.isEmpty()) {
                result = new ArrayList<Cluster>(1);
            }

            else {
                //invoke weka XMeans clusterer for Instances
                clusterer.buildClusterer(dataSet);

                result = new ArrayList<Cluster>(clusterer.numberOfClusters());

                //populate list of clusters
                for(i = 0; i < clusterer.numberOfClusters(); i++) {
                    result.add(new Cluster());
                }
            }

            for(i = 0; i < dataSet.numInstances(); i++) {
                row = dataSet.instance(i);

                //group points in a cluster together in list
                tmp = new double[2];
                tmp[0] = row.value(0);
                tmp[1] = row.value(1);
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
     * Updates the positions of the Bees for the current frame.
     * TODO check bee position, add arrival/departure timestamp as necessary
     * @param blobImg the filtered PImage
     * @param clusters an List of Cluster objects
     * @param colors a list of the integer RGB values to scan for
     * @param bees a HashMap mapping hue values to Bees
     * @param exitRadial a float array containing the following:
     *   normalized x coordinate of the exit center,
     *   normalized y coordinate of the exit center,
     *   normalized horizontal semi-major axis of the exit,
     *   normalized vertical semi-major axis of the exit
     * @param dims
     * @param offset
     * @param time timestamp of the current frame
     */
    public void updateBeePositions(
        processing.core.PImage blobImg,
        List<Cluster> clusters,
        processing.data.IntList colors,
        HashMap<Float, Bee> bees,
        float[] exitRadial,
        int[] dims,
        int[] offset,
        float time
    ) {
        Instances centers = clusterer.getClusterCenters();

        //data set can be clustered
        if(centers != null) {
            int pixel, beeX, beeY;
            float hueVal = -1f;
            Bee bee;
            double[] point;
            Cluster cluster;
            Instance center;

            float[] exitXY = new float[2];
            exitXY[0] = exitRadial[0]*dims[0] + offset[0];
            exitXY[1] = exitRadial[1]*dims[1] + offset[1];
            float[] semiMajAxes = new float[2];
            semiMajAxes[0] = exitRadial[2]*dims[0];
            semiMajAxes[1] = exitRadial[3]*dims[1];

            //list of bees that are not in the current frame
            List<Float> invisBees = new java.util.LinkedList<Float>();
            for(int col : colors) {
                invisBees.add(parent.hue(col));
            }

            parent.colorMode(PConstants.HSB, 255);

            blobImg.loadPixels();

//System.out.println(centers.numInstances()+" "+clusters.size());
            Iterator<Cluster> clusterIter = clusters.iterator();
            for(
                int i = 0;
                i < centers.numInstances() && clusterIter.hasNext();
                i++
            ) {
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
                    //TODO handle case where #clusters!=#centers (??)
                    cluster = clusterIter.next();

                    //iterate through cluster centroids for valid hue
                    for(double[] tmp : cluster.getPoints()) {
                        hueVal = (int)(tmp[1]*blobImg.height*
                            blobImg.width/parent.height) +
                            (int)(tmp[0]*blobImg.width/parent.width);

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

                invisBees.remove(hueVal);

                bee = bees.get(hueVal);
                if(bee == null) {
                    bee = new Bee();
                    bees.put(hueVal, bee);
                }

                //if current bee position within exit
                beeX = (int)(point[0]*parent.width);
                beeY = (int)(point[1]*parent.height);
                if(
                    Math.pow(beeX-exitXY[0], 2) + Math.pow(beeY-exitXY[1], 2) <
                    Math.pow(semiMajAxes[0], 2) + Math.pow(semiMajAxes[0], 2)
                ) {
                    //if last known position was Integer.MIN_VALUE,
                    //bee was not previously visible and bee has left hive
                    if(bee.getX() == Integer.MIN_VALUE && bee.getY() == Integer.MIN_VALUE) {
                        bee.addDepartureTime(time);
                    }
                }

                //update bee position
                bee.setX(beeX);
                bee.setY(beeY);
            }

            //check tracked bees that are not in current frame
            for(Float hue : invisBees) {
                bee = bees.get(hue);
                beeX = bee.getX();
                beeY = bee.getY();

                //if last known position within exit, bee has entered hive
                if(
                    Math.pow(beeX-exitXY[0], 2) + Math.pow(beeY-exitXY[1], 2) <
                    Math.pow(semiMajAxes[0], 2) + Math.pow(semiMajAxes[0], 2)
                ) {
                    bee.addArrivalTime(time);
                }

                //update bee position
                bee.setX(Integer.MIN_VALUE);
                bee.setY(Integer.MIN_VALUE);
            }
        }
    }
}