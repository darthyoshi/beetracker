/**
 * @file Bee.java
 * @author Kay Choi, 909926828
 * @date 22 Feb 15
 * @description An abstract representation of a bee.
 */

package beetracker;

import java.util.LinkedList;

public class Bee {
    private final LinkedList<Float> departureTimes, arrivalTimes;
    private int x, y;

    /**
     * Class constructor.
     */
    public Bee() {
        x = y = Integer.MIN_VALUE;
        departureTimes = new LinkedList<>();
        arrivalTimes = new LinkedList<>();
    }

    /**
     * @return the timestamps at which the Bee leaves the hive in seconds
     */
    public LinkedList<Float> getDepartureTimes() {
        return departureTimes;
    }

    /**
     * @return the timestamps at which the Bee enters the hive in seconds
     */
    public LinkedList<Float> getArrivalTimes() {
        return arrivalTimes;
    }

    /**
     * @return the x coordinate of the Bee
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the new x coordinate of the Bee.
     * @param x the new x coordinate
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * @return the x coordinate of the Bee
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the new y coordinate of the Bee.
     * @param y the new y coordinate
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Adds a new departure timestamp.
     * @param time the new timestamp in seconds
     */
    public void addDepartureTime(float time) {
        departureTimes.add(time);
    }

    /**
     * Adds a new arrival timestamp.
     * @param time the new timestamp in seconds
     */
    public void addArrivalTime(float time) {
        arrivalTimes.add(time);
    }
}