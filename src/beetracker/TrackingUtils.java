/**
 * @file TrackingUtils.java
 * @author Kay Choi, 909926828
 * @date 14 Feb 15
 * @description Handles all BeeTracker tracking-related operations.
 */

package beetracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.data.FloatList;
import processing.data.IntList;

/**
 *
 * @author Kay Choi
 */
public class TrackingUtils {
    private final boolean debug;
    private HashMap<Integer, List<List<float[]>>> allPaths;
    private HashMap<Integer, FloatList> departureTimes, arrivalTimes;
    private HashMap<Integer, IntList> allTimeOuts;
    private IntList colors;
    private final static float distThreshold = 0.25f;
    private PGraphics eventTimeline;

    /**
     * Class constructor.
     * @param debug whether or not debug mode is enabled
     */
    public TrackingUtils(boolean debug) {
        this.debug = debug;

        init();
    }

    /**
     * Updates the centroid positions for the current frame.
     * @param parent the invoking BeeTracker
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
     * @param time timestamp of the current frame in seconds
     * @param duration the video duration in seconds
     */
    public void trackCentroids(
        BeeTracker parent,
        HashMap<Integer, List<float[]>> newPointMap,
        int[] frameDims,
        int[] frameOffset,
        float[] exitRadial,
        int[] movieDims,
        int[] movieOffset,
        float time,
        float duration
    ) {
        List<float[]> newPoints, path;
        List<List<float[]>> oldPaths;
        FloatList departures, arrivals;
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

        for(int color : colors) {
            oldPaths = allPaths.get(color);
            newPoints = new ArrayList<>(newPointMap.get(color));
            timeOuts = allTimeOuts.get(color);

            checkedIndicesOld = new IntList();
            checkedIndicesNew = new IntList();

            k = 0;

            if(debug) {
                BeeTracker.println(String.format(
                    "---checking blobs colored %06x---%s %d%s %d",
                    color,
                    "\npoints in last frame:",
                    oldPaths.size(),
                    "\npoints in current frame:",
                    newPoints.size()
                ));
            }

            if(oldPaths.size() > 0 && newPoints.size() > 0) {
                distances = new float[oldPaths.size()][newPoints.size()];

                //calc distances between all old and all new points
                for(i = 0; i < oldPaths.size(); i++) {
                    path = oldPaths.get(i);

                    j = 0;
                    for(float[] newPoint : newPoints) {
                        point = path.get(path.size() - 1);
                        oldX = point[0] - newPoint[0];
                        oldY = point[1] - newPoint[1];

                        distances[i][j] = (float)Math.pow(oldX*oldX + oldY*oldY,
                            0.5);

                        j++;
                    }
                }

                minI = minJ = -1;

                numPairs = (
                    oldPaths.size() > newPoints.size() ?
                    newPoints.size() :
                    oldPaths.size()
                );
                validPairs = new int[numPairs][2];

                //until all valid old and new point pairs have been assigned
                while(checkedIndicesOld.size() < numPairs) {
                    minDist = Float.MAX_VALUE;

                    //pair points with minimum distance
                    for(i = 0; i < oldPaths.size(); i++) {
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
                            BeeTracker.println("points (" + minI + ", " + minJ + ") paired");
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
                BeeTracker.println(k + " point(s) paired");
            }

            departures = departureTimes.get(color);
            arrivals = arrivalTimes.get(color);

            //check all paired points
            for(i = 0; i < k; i++) {
                path = oldPaths.get(validPairs[i][0]);
                point = path.get(path.size() - 1);

                oldX = point[0]*frameDims[0]+frameOffset[0];
                oldY = point[1]*frameDims[1]+frameOffset[1];

                point = newPoints.get(validPairs[i][1]);

                newX = point[0]*frameDims[0]+frameOffset[0];
                newY = point[1]*frameDims[1]+frameOffset[1];

                isOldPointInExit = isInExit(oldX, oldY, exitCenterXY, exitAxes);
                isNewPointInExit = isInExit(newX, newY, exitCenterXY, exitAxes);

                if(debug) {
                    BeeTracker.println(
                        "pair " + i + ":\nold point " + validPairs[i][0] +
                        " is inside exit: " + (isOldPointInExit ? "true" : "false") +
                        "\nnew point " + validPairs[i][1] +" is inside exit: " +
                        (isNewPointInExit ? "true" : "false")
                    );
                }

                if(isOldPointInExit) {
                    if(!isNewPointInExit) {
                        departures.append(time);
                    }
                }

                else if(isNewPointInExit) {
                    arrivals.append(time);
                }
            }

            //update old points for next frame
            for(i = 0; i < k; i++) {
                path = oldPaths.get(validPairs[i][0]);
                point = newPoints.get(validPairs[i][1]);
                if(!path.contains(point)) {
                    path.add(point);
                }
                newPoints.set(validPairs[i][1], null);
                timeOuts.set(validPairs[i][0], -1);
            }

            //add new points for next frame
            j = 1;
            for(float[] newPoint : newPoints) {
                if(newPoint != null) {
                    path = new ArrayList<>();
                    path.add(newPoint);
                    oldPaths.add(path);
                    timeOuts.append(0);

                    j++;    //index offset for updating timeout values later
                }
            }

            if(debug) {
                BeeTracker.println("all paths:");
                for(i = 0; i < oldPaths.size(); i++) {
                    BeeTracker.println(i + ":");
                    path = oldPaths.get(i);

                    for(float[] tmpPoint : path) {
                        BeeTracker.println(tmpPoint[0] + "," + tmpPoint[1]);
                    }
                }
            }

            //update timeout values for old missing points
            for(i = timeOuts.size() - j; i >= 0; i--) {
                timeOuts.increment(i);

                //remove points that have been missing for too long
                if(timeOuts.get(i) > 5) {
                    timeOuts.remove(i);
                    oldPaths.remove(i);
                }
            }
        }
        
        updateEventTimeline(parent, time, duration);
    }

    /**
     * Sets the colors to track.
     * @param newColors an IntList containing six-digit hexadecimal RGB values
     */
    public void setColors(IntList newColors) {
        for(int color : newColors) {
            if(!colors.hasValue(color)) {
                colors.append(color);

                allPaths.put(color, new ArrayList<List<float[]>>());

                departureTimes.put(color, new FloatList());
                arrivalTimes.put(color, new FloatList());

                allTimeOuts.put(color, new IntList());
            }
        }
    }

    /**
     * Retrieves the arrival and departure timestamps for all tracked colors.
     * @return a HashMap mapping Strings to HashMaps mapping six-digit
     *   hexadecimal RGB values to Lists of floating point timestamps
     */
    public HashMap<Float, String> getSummary() {
        HashMap<Float, String> result = new HashMap<>();

        for(int color : colors) {
            for(Float time : arrivalTimes.get(color)) {
                result.put(time, String.format("#%06x,arrival", color));
            }
            for(Float time : departureTimes.get(color)) {
                result.put(time, String.format("#%06x,departure", color));
            }
        }

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
    public final void init() {
        allPaths = new HashMap<>();
        departureTimes = new HashMap<>();
        arrivalTimes = new HashMap<>();
        colors = new IntList();
        allTimeOuts = new HashMap<>();
        eventTimeline = null;
    }

    /**
     * @return a HashMap mapping 6-digit hexadecimal RGB values to Lists of
     *   timestamps
     */
    public HashMap<Integer, FloatList> getDepartureTimes() {
        return departureTimes;
    }

    /**
     * @return a HashMap mapping 6-digit hexadecimal RGB values to Lists of
     *   timestamps
     */
    public HashMap<Integer, FloatList> getArrivalTimes() {
        return arrivalTimes;
    }

    /**
     * Draws the recorded paths. 
     * @param buf the buffer image to draw to
     * @param bufOffset the xy coordinates of the buffer image
     * @param frameDims the dimensions of the image frame for which blob
     *   detection is being performed, in pixels
     * @param frameOffset the xy coordinates of the inset frame origin, in pixels
     */
    public void drawPaths(
        PGraphics buf,
        int[] bufOffset,
        int[] frameDims,
        int[] frameOffset
    ) {
        int i;
        float[] point, point2;

        buf.strokeWeight(2);
        for(int color : colors) {
            buf.stroke(0xff000000 + color);

            for(List<float[]> path : allPaths.get(color)) {
                for(i = 0; i < path.size()-1; i++) {
                    point = path.get(i);
                    point2 = path.get(i+1);
                    
                    buf.line(
                        point[0]*frameDims[0]+frameOffset[0]-bufOffset[0],
                        point[1]*frameDims[1]+frameOffset[1]-bufOffset[0],
                        point2[0]*frameDims[0]+frameOffset[0]-bufOffset[0],
                        point2[1]*frameDims[1]+frameOffset[1]-bufOffset[1]
                    );
                }
            }
        }
    }
    
    /**
     * Generates a visual summary of the currently recorded events.
     * @param parent the invoking BeeTracker
     * @param time the current playback time in seconds
     * @param duration the video duration in seconds
     */
    private void updateEventTimeline(
        BeeTracker parent,
        float time,
        float duration
    ) {
        BeeTracker.print("updating event timeline... ");

        int color, yOffset;
        float xOffset = time/duration*369f + 26f;

        if(eventTimeline == null) {
            eventTimeline = parent.createGraphics(400, colors.size() * 50);

            eventTimeline.beginDraw();

            eventTimeline.background(0xffeeeeee);

            eventTimeline.textAlign(PConstants.LEFT);

            for(int i = 1; i <= colors.size(); i++) {
                color = colors.get(i-1);
                yOffset = 50*i;

                eventTimeline.strokeWeight(1);
                eventTimeline.stroke(0xff000000);
                eventTimeline.fill(0xffcccccc);
                eventTimeline.rectMode(PConstants.CORNER);
                eventTimeline.rect(25, yOffset-25, 370, 20);
                
                eventTimeline.fill(0xff000000);
                eventTimeline.text("A", 7, yOffset-15);
                eventTimeline.text("D", 7, yOffset-5);
                eventTimeline.text("color:", 25, yOffset-32);

                eventTimeline.fill(0xff000000 + color);
                eventTimeline.text(String.format("%06x", color), 65, yOffset-32);
            }
        }

        else {
            eventTimeline.beginDraw();
        }

        eventTimeline.ellipseMode(PConstants.CENTER);
        eventTimeline.rectMode(PConstants.CENTER);
        eventTimeline.stroke(0xff000000);

        for(int i = 1; i <= colors.size(); i++) {
            color = colors.get(i-1);
            yOffset = 50*i;

            if(!allPaths.get(color).isEmpty()) {
                eventTimeline.stroke(0xff000000 + color);
                eventTimeline.line(
                    xOffset,
                    yOffset-20,
                    xOffset,
                    yOffset-10
                );
                eventTimeline.stroke(0xff000000);
            }

            eventTimeline.fill(0xff000000 + color);

            for(float stamp : departureTimes.get(color)) {
              eventTimeline.rect(
                stamp/duration*369 + 26,
                yOffset-20,
                5,
                5
              );
            }

            for(float stamp : arrivalTimes.get(color)) {
              eventTimeline.ellipse(
                stamp/duration*369 + 26,
                yOffset-10,
                5,
                5
              );
            }
        }

        eventTimeline.endDraw();

        BeeTracker.println("done");
    }
    
    /**
     * Retrieves the visual summary of the currently recorded events.
     * @param parent the invoking BeeTracker
     * @param time the current playback time in seconds
     * @param duration the video duration in seconds
     * @return a PGraphics image of the event timeline
     */
    public PGraphics getEventTimeline(
        BeeTracker parent,
        float time,
        float duration
    ) {
        BeeTracker.print("retrieving event timeline... ");

        PGraphics result = parent.createGraphics(eventTimeline.width,
            eventTimeline.height);

        float xOffset = time/duration*369 + 26;
        int yOffset;
        
        result.beginDraw();

        result.copy(
            eventTimeline,
            0, 0,
            eventTimeline.width, eventTimeline.height,
            0, 0,
            result.width, result.height
        );

        result.stroke(0xff555555);

        for(int i = 1; i <= colors.size(); i++) {
            yOffset = 50*i;
            
            result.line(
              xOffset,
              yOffset-24,
              xOffset,
              yOffset-6
            );
            result.line(0, yOffset, 400, yOffset);
        }

        result.endDraw();

        BeeTracker.println("done");

        return result;
    }
}
