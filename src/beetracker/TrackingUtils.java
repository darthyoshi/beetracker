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
    private HashMap<Integer, IntList> allTimeOuts;
    private IntList colors;
    private final static float distThreshold = 0.25f;

    /**
     * Class constructor.
     * @param debug whether or not debug mode is enabled
     */
    public TrackingUtils(boolean debug) {
        this.debug = debug;

        initAll();
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
        IntList checkedIndicesOld, checkedIndicesNew, timeOuts;
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

        for(int color : newPointMap.keySet()) {
            oldPoints = allPoints.get(color);
            newPoints = newPointMap.get(color);
            timeOuts = allTimeOuts.get(color);

            checkedIndicesOld = new IntList();
            checkedIndicesNew = new IntList();

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
                        oldX = oldPoint[0] - newPoint[0];
                        oldY = oldPoint[1] - newPoint[1];

                        distances[i][j] = (float)Math.pow(oldX*oldX + oldY*oldY,
                            0.5);

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

            //update old points for next frame
            for(i = 0; i < k; i++) {
                oldPoints.set(validPairs[i][0], newPoints.get(validPairs[i][1]));
                newPoints.set(validPairs[i][1], null);
                timeOuts.set(validPairs[i][0], -1);
            }

            //add new points for next frame
            j = 1;
            for(float[] newPoint : newPoints) {
                if(newPoint != null) {
                    oldPoints.add(newPoint);
                    timeOuts.append(0);

                    j++;
                }
            }

            if(debug) {
                PApplet.println(j + "\n" + timeOuts +"\nold points:");
                for(float[] oldPoint : oldPoints) {
                    PApplet.print("("+oldPoint[0]+" "+oldPoint[1]+")");
                }
                PApplet.println("\ntotal: "+oldPoints.size()+"\nnew points:");
                for(float[] newPoint : newPoints) {
                    if(newPoint != null) {
                        PApplet.print("("+newPoint[0]+" "+newPoint[1]+")");
                    }

                    else {
                        PApplet.print("(point is an old point)");
                    }
                }
                PApplet.println("\ntotal: "+newPoints.size());
            }

            //update timeout values for old missing points
            for(i = timeOuts.size() - j; i >= 0; i--) {
                timeOuts.increment(i);

                //remove points that have been missing for too long
                if(timeOuts.get(i) > 5) {
                    timeOuts.remove(i);
                    oldPoints.remove(i);
                }
            }
        }
    }

    /**
     * Sets the colors to track.
     * @param newColors an IntList containing six-digit hexadecimal RGB values
     */
    public void setColors(IntList newColors) {
        for(int color : newColors) {
            if(!colors.hasValue(color)) {
                colors.append(color);

                allPoints.put(color, new ArrayList<float[]>());

                departureTimes.put(color, new LinkedList<Float>());
                arrivalTimes.put(color, new LinkedList<Float>());

                allTimeOuts.put(color, new IntList());
            }
        }
    }

    /**
     * Retrieves the arrival and departure timestamps for all tracked colors.
     * @return a HashMap mapping Strings to HashMaps mapping six-digit
     *   hexadecimal RGB values to Lists of floating point timestamps
     */
    public HashMap<String, HashMap<Integer, List<Float>>> getTimeStamps() {
        HashMap<String, HashMap<Integer, List<Float>>> result = new HashMap<>();
        result.put("arrivals", arrivalTimes);
        result.put("departures", departureTimes);

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
        float a = (x - exitXY[0])/axes[0];
        float b = (y - exitXY[1])/axes[1];
        return a*a + b*b <= 1f;
    }

    /**
     * Initializes all tracking data structures.
     */
    public void initAll() {
        allPoints = new HashMap<>();
        departureTimes = new HashMap<>();
        arrivalTimes = new HashMap<>();
        colors = new IntList();
        allTimeOuts = new HashMap<>();
    }
}
