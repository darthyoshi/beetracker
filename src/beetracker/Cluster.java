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
        points = new LinkedList<double[]>();
        x = y = width = height = 0f;
    }

    /**
     * @return the list of points in this Cluster
     */
    public LinkedList<double[]> getPoints() {
        return points;
    }

    /**
     * @return the x coordinate of the Cluster centroid
     */
    public double getX() {
        return x;
    }

    /**
     * Sets the x coordinate of the Cluster centroid.
     * @param x the new x coordinate
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return the y coordinate of the Cluster centroid
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the y coordinate of the Cluster centroid.
     * @param y the new y coordinate
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * @return the width of the Cluster
     */
    public double getWidth() {
        return width;
    }

    /**
     * Sets the width of the Cluster.
     * @param width the new width
     */
    public void setWidth(double width) {
        this.width = width;
    }

    /**
     * @return the height of the Cluster
     */
    public double getHeight() {
        return height;
    }

    /**
     * Sets the height of the Cluster.
     * @param height the new width
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * Adds a new point to the Cluster.
     * @param point a float array containing the xy coordinates to add
     */
    public void addPoint(double[] point) {
        points.add(point);
    }

    /**
     * Calculates the Cartesian width and height of the cluster.
     */
    public void calcBounds() {
        double[] max = {Double.MIN_VALUE, Double.MIN_VALUE};
        double[] min = {Double.MAX_VALUE, Double.MAX_VALUE};

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
