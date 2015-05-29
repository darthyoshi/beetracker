/**
 * @file DataMinerUtils.java
 * @author Kay Choi, 909926828
 * @date 14 Feb 15
 * @description Handles all BeeTracker clustering-related operations.
 */

package beetracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import processing.core.PApplet;
import processing.data.IntList;

public class TrackingUtils {
    private final boolean debug;
    private HashMap<Integer, List<float[]>> allPoints;
    private HashMap<Integer, List<Float>> departureTimes, arrivalTimes;
    private IntList colors;
    private final static float distThreshold = 10f;

    /**
     * Class constructor.
     * @param parent the instantiating PApplet
     * @param debug whether or not debug mode is enabled
     */
    public TrackingUtils(boolean debug) {
        this.debug = debug;

        allPoints = new HashMap<>();
        departureTimes = new HashMap<>();
        arrivalTimes = new HashMap<>();
        colors = new IntList();
    }

    /**
     * Updates the centroid positions for the current frame.
     * @param newPointMap a HashMap mapping six-digit hexadecimal RGB values
     *   to Lists of normalized xy coordinates
     * @param frameDims the dimensions of the inset frame
     * @param frameOffset the offset of the inset frame
     * @param exitRadial a float array containing the following (normalized
     *   within the view window):
     *   x coordinate of the exit center,
     *   y coordinate of the exit center,
     *   horizontal semi-major axis of the exit,
     *   vertical semi-major axis of the exit
     * @param movieDims the dimensions of the video
     * @param movieOffset the offset of the video
     * @param time timestamp of the current frame
     */
    public void trackCentroids(
        HashMap<Integer, List<float[]>> newPointMap,
        int[] frameDims,
        int[] frameOffset,
        float[] exitRadial,
        int[] movieDims,
        int[] movieOffset,
        float time
    ) {
        List<float[]> newPoints, oldPoints;
        List<Float> departures, arrivals;
        IntList checkedIndicesOld, checkedIndicesNew;
        float oldX, oldY, newX, newY, minDist;
        float[] point;
        float[][] distances;
        int i, j, k, numPairs, minI, minJ;
        int[][] validPairs = null;
        boolean isOldPointInExit, isNewPointInExit;

        float[] exitCenterXY = new float[2];
        exitCenterXY[0] = exitRadial[0]*movieDims[0]+movieOffset[0];
        exitCenterXY[1] = exitRadial[1]*movieDims[1]+movieOffset[1];

        float[] exitAxes = new float[2];
        exitAxes[0] = exitRadial[2]*movieDims[0];
        exitAxes[1] = exitRadial[3]*movieDims[1];

        for(int color : colors) {
            oldPoints = allPoints.get(color);
            newPoints = newPointMap.get(color);

            checkedIndicesOld = new IntList(oldPoints.size());
            checkedIndicesNew = new IntList(newPoints.size());

            k = 0;

            if(debug) {
                PApplet.println(String.format(
                    "---checking blobs colored %06x---\n%s %d\n%s %d",
                    color,
                    "points in last frame:",
                    oldPoints.size(),
                    "points in current frame:",
                    newPoints.size()
                ));
            }

            if(oldPoints.size() > 0 && newPoints.size() > 0) {
                distances = new float[oldPoints.size()][newPoints.size()];

                //calc distances between all old and all new points
                i = 0;
                for(float[] oldPoint : oldPoints) {
                    j = 0;
                    for(float[] newPoint : newPoints) {
                        distances[i][j] = (float)Math.pow(Math.pow(oldPoint[0] - newPoint[0], 2) +
                            Math.pow(oldPoint[1] - newPoint[1], 2), 0.5);

                        j++;
                    }

                    i++;
                }

                minI = minJ = -1;

                numPairs = (
                    oldPoints.size() > newPoints.size() ?
                    newPoints.size() :
                    oldPoints.size()
                );
                validPairs = new int[numPairs][2];

                //until all valid old and new point pairs have been assigned
                while(checkedIndicesOld.size() < numPairs) {
                    minDist = Float.MAX_VALUE;

                    //pair points with minimum distance
                    for(i = 0; i < oldPoints.size(); i++) {
                        //oldPoints.get(i) not already paired
                        if(!checkedIndicesOld.hasValue(i)) {
                            for(j = 0; j < newPoints.size(); j++) {
                                //newPoints.get(j) not already paired
                                if(!checkedIndicesNew.hasValue(j)) {
                                    if(distances[i][j] < minDist) {
                                        minI = i;
                                        minJ = j;
                                        minDist = distances[i][j];
                                    }
                                }
                            }
                        }
                    }

                    //mark paired points
                    checkedIndicesOld.append(minI);
                    checkedIndicesNew.append(minJ);

                    //mark pairs with valid distance
                    if(minDist < distThreshold) {
                        validPairs[k][0] = minI;
                        validPairs[k][1] = minJ;

                        k++;

                        if(debug) {
                            PApplet.println("points (" + minI + ", " + minJ + ") paired");
                        }
                    }

                    //if the closest pair of points is not within the distance
                    //  threshold, no other points will be, so break loop
                    else {
                        break;
                    }
                }
            }

            if(debug) {
                PApplet.println(k + " point(s) paired");
            }

            departures = departureTimes.get(color);
            arrivals = arrivalTimes.get(color);

            //check all paired points
            for(i = 0; i < k; i++) {
                point = oldPoints.get(validPairs[i][0]);

                oldX = point[0]*frameDims[0]+frameOffset[0];
                oldY = point[1]*frameDims[1]+frameOffset[1];

                point = newPoints.get(validPairs[i][1]);

                newX = point[0]*frameDims[0]+frameOffset[0];
                newY = point[1]*frameDims[1]+frameOffset[1];

                isOldPointInExit = isInExit(oldX, oldY, exitCenterXY, exitAxes);
                isNewPointInExit = isInExit(newX, newY, exitCenterXY, exitAxes);

                if(debug) {
                    PApplet.println(String.format(
                        "pair " + i + ":\nold point " + validPairs[i][0] +
                        " is inside exit: " + (isOldPointInExit ? "true" : "false") +
                        "\nnew point " + validPairs[i][1] +" is inside exit: " +
                        (isNewPointInExit ? "true" : "false")
                    ));
                }

                if(isOldPointInExit) {
                    if(!isNewPointInExit) {
                        departures.add(time);
                    }
                }

                else {
                    if(isNewPointInExit) {
                        arrivals.add(time);
                    }
                }
            }

            //store current points for next frame
            allPoints.put(color, newPoints);
        }
    }

    /**
     * Sets the colors to track.
     * @param newColors an IntList containing six-digit hexadecimal RGB values
     */
    public void setColors(IntList newColors) {
        colors = new IntList();
        allPoints = new HashMap<>();
        departureTimes = new HashMap<>();
        arrivalTimes = new HashMap<>();

        for(int color : newColors) {
            colors.append(color);

            allPoints.put(color, new LinkedList<float[]>());

            departureTimes.put(color, new LinkedList<Float>());
            arrivalTimes.put(color, new LinkedList<Float>());
        }
    }

    /**
     * Retrieves the departure and arrival timestamps for all tracked colors.
     * @return an array containing HashMaps mapping six-digit hexadecimal RGB
     *   values to Lists of floating point timestamps
     */
    public ArrayList<HashMap<Integer, List<Float>>> getTimeStamps() {
        ArrayList<HashMap<Integer, List<Float>>> result = new ArrayList<>(2);
        result.add(departureTimes);
        result.add(arrivalTimes);

        return result;
    }
    
    /**
     * Determines if a point is within the exit.
     * @param x the x coordinate of the point in pixels
     * @param y the y coordinate of the point in pixels
     * @param exitXY the coordinates of the exit center in pixels
     * @param axes the axes of the exit in pixels
     * @return true if (dX/A)^2 + (dY/B)^2 <= 1, where
     *   dX is the distance between the point and the exit center on the x axis
     *   dY is the distance between the point and the exit center on the y axis
     *   A is the length of the ellipse along the x axis
     *   B is the length of the ellipse along the y axis
     */
    private boolean isInExit(float x, float y, float[] exitXY, float[] axes) {
        return Math.pow((x - exitXY[0])/axes[0], 2) + Math.pow((y - exitXY[1])/axes[1], 2) <= 1;
    }
}
