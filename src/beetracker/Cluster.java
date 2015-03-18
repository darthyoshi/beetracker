/**
 * @file Cluster.java
 * @author Kay Choi, 909926828
 * @date 20 Feb 2015
 * @description A data structure storing information about a cluster of points.
 */

package beetracker;

import java.util.LinkedList;

public final class Cluster {
    private double x, y, width, height;
    private final LinkedList<double[]> points;

    /**
     * Class constructor.
     */
    public Cluster() {
        points = new LinkedList<>();
        x = y = width = height = 0f;
    }

    /**
     * @return the list of points in this Cluster
     */
    public LinkedList<double[]> getPoints() {
        return points;
    }

    /**
     * @return the normalized x coordinate of the Cluster centroid
     */
    public double getX() {
        return x;
    }

    /**
     * Sets the normalized x coordinate of the Cluster centroid.
     * @param x the new normalized x coordinate
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return the normalized y coordinate of the Cluster centroid
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the normalized y coordinate of the Cluster centroid.
     * @param y the new normalized y coordinate
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * @return the normalized width of the Cluster
     */
    public double getWidth() {
        return width;
    }

    /**
     * @return the normalized height of the Cluster
     */
    public double getHeight() {
        return height;
    }

    /**
     * Adds a new point to the Cluster.
     * @param point a float array containing the normalized xy coordinates to add
     */
    public void addPoint(double[] point) {
        points.add(point);
    }

    /**
     * Calculates the normalized width and height of the cluster.
     */
    public void calcDims() {
        double[] max = {points.get(0)[0], points.get(0)[1]};
        double[] min = {points.get(0)[0], points.get(0)[1]};

        for(double[] point : points) {
            if(point[0] > max[0]) {
                max[0] = point[0];
            }

            else if(point[0] < min[0]) {
                min[0] = point[0];
            }

            if(point[1] > max[1]) {
                max[1] = point[1];
            }

            else if(point[1] < min[1]) {
                min[1] = point[1];
            }
        }

        width = max[0] - min[0];
        height = max[1] - min[1];
    }
}
