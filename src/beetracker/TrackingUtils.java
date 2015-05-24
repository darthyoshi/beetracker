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
	private final BeeTracker parent;
    private final boolean debug;
    private final HashMap<Integer, List<float[]>> allPoints;
    private final HashMap<Integer, List<Float>> departureTimes, arrivalTimes;
    private final IntList colors;
    private final static float distThreshold = 10f;

    /**
     * Class constructor.
     * @param parent the instantiating PApplet
     * @param debug whether or not debug mode is enabled
     */
    public TrackingUtils(BeeTracker parent, boolean debug) {
        this.parent = parent;
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
        List<float[]> newPoints = new ArrayList<>();
        List<float[]> oldPoints = new ArrayList<>();
        List<Float> departures, arrivals;
        IntList checkedIndicesOld = new IntList();
        IntList checkedIndicesNew = new IntList();
        float minDist;
        float[] point;
        float[][] distances;
        int oldX, oldY, newX, newY, i, j, k, numPairs, minI, minJ;
        int[][] validPairs = null;
        double rSquared;
        boolean isOldPointInExit, isNewPointInExit;

        //exit center coordinates
        float[] exitXY = new float[2];
        exitXY[0] = exitRadial[0]*movieDims[0]+movieOffset[0];
        exitXY[1] = exitRadial[1]*movieDims[1]+movieOffset[1];

        //exit axes
        float[] semiMajAxes = new float[2];
        semiMajAxes[0] = exitRadial[2]*movieDims[0];
        semiMajAxes[1] = exitRadial[3]*movieDims[1];
        rSquared = Math.pow(semiMajAxes[0], 2) + Math.pow(semiMajAxes[1], 2);

        for(int color : colors) {
        	oldPoints.clear();
            newPoints.clear();

            checkedIndicesOld.clear();
            checkedIndicesNew.clear();

            oldPoints.addAll(allPoints.get(color));
            newPoints.addAll(newPointMap.get(color));

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
                    oldX = (int)(oldPoint[0]*frameDims[0]+frameOffset[0]);
                    oldY = (int)(oldPoint[1]*frameDims[1]+frameOffset[1]);

                    j = 0;
                    for(float[] newPoint : newPoints) {
                        newX = (int)(newPoint[0]*frameDims[0]+frameOffset[0]);
                        newY = (int)(newPoint[1]*frameDims[1]+frameOffset[1]);

                        distances[i][j] = (float)Math.pow(Math.pow(oldX - newX, 2) +
                            Math.pow(oldY - newY, 2), 0.5);

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
                        	PApplet.println(String.format("points (%d, %d) paired", minI, minJ));
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
            	PApplet.println(String.format("%d point(s) paired", k));
            }

            departures = departureTimes.get(color);
            arrivals = arrivalTimes.get(color);

            //check all paired points
            for(i = 0; i < k; i++) {
                point = oldPoints.get(validPairs[i][0]);

                oldX = (int)(point[0]*frameDims[0]+frameOffset[0]);
                oldY = (int)(point[1]*frameDims[1]+frameOffset[1]);

                point = newPoints.get(validPairs[i][1]);

                newX = (int)(point[0]*frameDims[0]+frameOffset[0]);
                newY = (int)(point[1]*frameDims[1]+frameOffset[1]);

                isOldPointInExit = rSquared > Math.pow(oldX - exitXY[0], 2) + Math.pow(oldY - exitXY[1], 2);
                isNewPointInExit = rSquared > Math.pow(newX - exitXY[0], 2) + Math.pow(newY - exitXY[1], 2);

                //TODO figure out why transitions across exit circumference aren't registering
                if(debug) {
                	PApplet.println(String.format(
            			"pair %d:\n%s %d %s %s\n%s %d %s %s",
            			i,
            			"old point",
            			validPairs[i][0],
            			"is inside exit:",
            			(isOldPointInExit ? "true" : "false"),
            			"new point",
            			validPairs[i][1],
            			"is inside exit:",
            			(isNewPointInExit ? "true" : "false")
        			));

	                //line to exit center
	                parent.strokeWeight(2);
	                parent.stroke(255, 0, 255);
	                parent.line(newX, newY, exitXY[0], exitXY[1]);
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
            allPoints.get(color).clear();
            allPoints.get(color).addAll(newPoints);
        }
    }

    /**
     * Sets the colors to track.
     * @param newColors an IntList containing six-digit hexadecimal RGB values
     */
    public void setColors(IntList newColors) {
        colors.clear();
        allPoints.clear();
        departureTimes.clear();
        arrivalTimes.clear();

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
    	ArrayList<HashMap<Integer, List<Float>>> result = new ArrayList<>();
        result.add(departureTimes);
        result.add(arrivalTimes);
        result.trimToSize();

        return result;
    }
}