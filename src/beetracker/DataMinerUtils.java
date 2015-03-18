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

import weka.clusterers.SimpleKMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class DataMinerUtils {
    private final PrintStream log;

    private final BeeTracker parent;

    private SimpleKMeans clusterer;
    private Instances dataSet;
    private static final String options[] = {
        "-N", "1",          //k
        "-A", "weka.core.EuclideanDistance -R first-last",  //distance function
        "-I", "500",        //max iterations
        "-S", "10"          //random seed
    };

    private final boolean debug;

    /**
     * Class constructor.
     * @param parent the instantiating PApplet
     * @param log the output log
     * @param header the File object containing the Weka header definitions
     * @param debug whether or not debug mode is enabled
     */
    public DataMinerUtils(
        BeeTracker parent,
        PrintStream log,
        java.io.File header,
        boolean debug
    ) {
        this.parent = parent;
        this.log = log;
        this.debug = debug;

        clusterer = new SimpleKMeans();

        try {
            clusterer.setOptions(options);

            dataSet = new Instances(new java.io.BufferedReader(
                new java.io.FileReader(header))
            );
        } catch(Exception e) {
            e.printStackTrace(log);
            parent.crash(e.toString());
        }
    }


    /**
     * Uses the X-means algorithm to sort a set of points into clusters.
     * @param pointSets a List containing the normalized points to process.
     * @return an LinkedList of Cluster objects
     */
    public List<Cluster> getClusters(List<List<float[]>> pointSets) {
        List<Cluster> result = new ArrayList<>();
        Cluster c;
        Instance row;
        double[] tmp;
        int i;

        for(List<float[]> points : pointSets) {
            //clear old points from data set
            dataSet.delete();

            //add new points to data set
            for(float[] point : points) {
                row = new SparseInstance(2);
                row.setValue(0, point[0]);
                row.setValue(1, point[1]);
                row.setDataset(dataSet);

                dataSet.add(row);
            }

            try {
                if(dataSet.numInstances() > 0) {
                    //invoke weka SimpleKMeans clusterer for Instances
                    clusterer.buildClusterer(dataSet);

                    //add to list of clusters
                    c = new Cluster();
                    result.add(c);

                    for(i = 0; i < dataSet.numInstances(); i++) {
                        row = dataSet.instance(i);

                        //group points in a cluster together
                        tmp = new double[2];
                        tmp[0] = row.value(0);
                        tmp[1] = row.value(1);
                        c.addPoint(tmp);

                        if(debug) {
                            PApplet.println("instance " + i + '(' + tmp[0] + ','
                                + tmp[1] + "): cluster " +
                                clusterer.clusterInstance(row));
                        }
                    }

                    if(debug) {
                        PApplet.println("clusters (DataMinerUtils.getClusters()):");
                    }

                    Instance center = clusterer.getClusterCentroids()
                        .firstInstance();

                    c.setX(center.value(0));
                    c.setY(center.value(1));
                    c.calcDims();

                    if(debug) {
                        PApplet.println(String.format("%f, %f, %f, %f",
                            c.getX(),c.getY(), c.getWidth(), c.getHeight()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(log);
                parent.crash(e.toString());
            }
        }

        ((ArrayList)result).trimToSize();

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
        List<Float> invisBees = new java.util.LinkedList<>();
        for(int col : colors) {
            invisBees.add(parent.hue(col));
        }

        parent.colorMode(PConstants.HSB, 255);

        blobImg.loadPixels();

        //iterate through cluster centroids
        Iterator<Cluster> clusterIter = clusters.iterator();
        while(clusterIter.hasNext()) {
            cluster = clusterIter.next();
            point = new double[2];

            point[0] = cluster.getX();
            point[1] = cluster.getY();

            //grab centroid pixel from blob image
            pixel = blobImg.pixels[
                (int)(point[1]*blobImg.height*blobImg.width/parent.height) +
                (int)(point[0]*blobImg.width/parent.width)
            ];

            //-case: centroid is not in a blob (pixel is black)
            if(parent.brightness(pixel) == 0) {
                //iterate through cluster members for valid hue
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
            if(Math.pow(beeX-exitXY[0], 2) + Math.pow(beeY-exitXY[1], 2) <
                Math.pow(semiMajAxes[0], 2) + Math.pow(semiMajAxes[0], 2))
            {
                //if last known position was Integer.MIN_VALUE,
                //bee was not previously visible and bee has left hive
                if(bee.getX() == Integer.MIN_VALUE &&
                    bee.getY() == Integer.MIN_VALUE)
                {
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
            if(Math.pow(beeX-exitXY[0], 2) + Math.pow(beeY-exitXY[1], 2) <
                Math.pow(semiMajAxes[0], 2) + Math.pow(semiMajAxes[0], 2))
            {
                bee.addArrivalTime(time);
            }

            //update bee position
            bee.setX(Integer.MIN_VALUE);
            bee.setY(Integer.MIN_VALUE);
        }
    }
}