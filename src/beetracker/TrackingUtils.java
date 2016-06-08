/*
* BeeTracker
* Copyright (C) 2015 Kay Choi
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package beetracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import processing.core.PGraphics;
import processing.data.FloatList;
import processing.data.IntList;

/**
 * @class TrackingUtils
 * @author Kay Choi
 * @date 6 Jun 16
 * @description Handles all BeeTracker tracking-related operations.
 */
class TrackingUtils {
  private HashMap<Integer, List<List<float[]>>> allPaths;
  private HashMap<Integer, List<Boolean>> allWaggleStatus;
  private HashMap<Integer, FloatList> departureTimes, arrivalTimes, waggleTimes;
  private HashMap<Integer, FloatList> allTimeOuts;
  private HashMap<Integer, Stack<float[]>> allIntervals;
  private IntList colors;
  private static final float distThreshold = 0.1f;
  private boolean waggleMode = false;
  private final ShapeRecognizer rec;
  private static final float timeOutThreshold = 1f;

  /**
   * Class constructor.
   * @param parent the instantiating object
   */
  TrackingUtils(BeeTracker parent) {
    rec = new ShapeRecognizer(parent);
    rec.loadTemplates(parent);

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
  void trackCentroids(
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
    List<Boolean> waggleStates;
    java.util.ListIterator<Boolean> waggleIter;
    FloatList departures, arrivals, waggles, timeOuts;
    IntList checkedIndicesOld, checkedIndicesNew;
    float oldX, oldY, newX, newY, minDist;
    float[] point;
    float[][] distances;
    int i, j, k, numPairs, minI, minJ;
    int[][] validPairs = null;
    boolean isOldPointInExit, isNewPointInExit;
    int timeOutIndex = parent.isImgSequenceMode() ? 1 : 0;

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

      if(waggleMode) {
        waggleStates = allWaggleStatus.get(color);
      } else {
        waggleStates = null;
      }

      checkedIndicesOld = new IntList();
      checkedIndicesNew = new IntList();

      k = 0;

      if(BeeTracker.debug) {
        System.out.println(String.format(
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
            oldX = (point[0] - newPoint[0])*movieDims[0];
            oldY = (point[1] - newPoint[1])*movieDims[1];

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
          if(minDist < distThreshold*0.5f*(movieDims[0]+movieDims[1])) {
            validPairs[k][0] = minI;
            validPairs[k][1] = minJ;

            k++;

            if(BeeTracker.debug) {
              System.out.println("points (" + minI + ", " + minJ + ") paired");
            }
          } else {  //if the closest pair of points is not within the distance
            break;  //  threshold, no other points will be, so break loop
          }
        }
      }

      if(BeeTracker.debug) {
        System.out.println(k + " point(s) paired");
      }

      //check for waggle dances
      if(waggleStates != null) {
        waggles = waggleTimes.get(color);

        waggleIter = waggleStates.listIterator();
        i = 0;
        while(waggleIter.hasNext()) {
          if(!waggleIter.next()) {
            rec.recognize(oldPaths.get(i), frameDims);

            if(rec.isCandidateRecognized()) {
              waggleIter.set(true);

              waggles.append(time);
            }
          }

          i++;
        }
      } else {  //check all paired points for arrivals/departures
        departures = departureTimes.get(color);
        arrivals = arrivalTimes.get(color);

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

          if(BeeTracker.debug) {
            System.out.println(
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
          } else if(isNewPointInExit) {
            arrivals.append(time);
          }
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
        timeOuts.set(validPairs[i][0], time);
      }

      //add new points for next frame
      j = 1;
      for(float[] newPoint : newPoints) {
        if(newPoint != null) {
          path = new ArrayList<>();
          path.add(newPoint);
          oldPaths.add(path);
          timeOuts.append(time);

          if(waggleStates != null) {
            waggleStates.add(false);
          }

          j++;  //index offset for updating timeout values later
        }
      }

      if(BeeTracker.debug) {
        System.out.println(String.format("all paths for %06x:",color));
        for(i = 0; i < oldPaths.size(); i++) {
          System.out.println(i + ":");
          path = oldPaths.get(i);

          for(float[] tmpPoint : path) {
            System.out.println(tmpPoint[0] + "," + tmpPoint[1]);
          }
        }
      }

      int numOldPoints = timeOuts.size() - j;
      if(waggleStates != null) {
        waggleIter = waggleStates.listIterator(numOldPoints + 1);
      } else {
        waggleIter = null;
      }
      for(i = numOldPoints; i >= 0; i--) {
        if(waggleIter != null) {
          waggleIter.previous();
        }

        //remove points that have been missing for too long
        if(time - timeOuts.get(i) > timeOutThreshold) {
          timeOuts.remove(i);
          oldPaths.remove(i);

          if(waggleIter != null) {
            waggleIter.remove();
          }
        }
      }
    }

    updateEventTimeline(time, duration);
  }

  /**
   * Sets the colors to track.
   * @param newColors an IntList containing six-digit hexadecimal RGB values
   */
  void setColors(IntList newColors) {
    for(int color : newColors) {
      if(!colors.hasValue(color)) {
        colors.append(color);

        allPaths.put(color, new ArrayList<List<float[]>>());

        Stack<float[]> tmp = new Stack<>();
        tmp.add(new float[]{Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY});
        allIntervals.put(color, tmp);

        //waggle dance events
        allWaggleStatus.put(color, new java.util.LinkedList<Boolean>());
        waggleTimes.put(color, new FloatList());

        //arrival/departure events
        departureTimes.put(color, new FloatList());
        arrivalTimes.put(color, new FloatList());

        allTimeOuts.put(color, new FloatList());
      }
    }
  }

  /**
   * Retrieves the arrival and departure timestamps for all tracked colors.
   * @return a HashMap mapping Strings to HashMaps mapping six-digit
   *   hexadecimal RGB values to Lists of floating point timestamps
   */
  HashMap<Float, String> getSummary() {
    HashMap<Float, String> result = new HashMap<>();

    if(waggleMode) {
      for(int color : colors) {
        for(Float time : waggleTimes.get(color)) {
          result.put(time, String.format("#%06x,waggle", color));
        }
      }
    } else {
      for(int color : colors) {
        for(Float time : arrivalTimes.get(color)) {
          result.put(time, String.format("#%06x,arrival", color));
        }
        for(Float time : departureTimes.get(color)) {
          result.put(time, String.format("#%06x,departure", color));
        }
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
  final void init() {
    allPaths = new HashMap<>();
    allWaggleStatus = new HashMap<>();
    departureTimes = new HashMap<>();
    arrivalTimes = new HashMap<>();
    waggleTimes = new HashMap<>();
    colors = new IntList();
    allTimeOuts = new HashMap<>();
    allIntervals = new HashMap<>();
  }

  /**
   * @return a HashMap mapping 6-digit hexadecimal RGB values to Lists of
   *   timestamps
   */
  HashMap<Integer, FloatList> getDepartureTimes() {
    return departureTimes;
  }

  /**
   * @return a HashMap mapping 6-digit hexadecimal RGB values to Lists of
   *   timestamps
   */
  HashMap<Integer, FloatList> getArrivalTimes() {
    return arrivalTimes;
  }

  /**
   * @return a HashMap mapping 6-digit hexadecimal RGB values to Lists of
   *   timestamps
   */
  HashMap<Integer, FloatList> getWaggleTimes() {
    return waggleTimes;
  }

  /**
   * Draws the recorded paths.
   * @param buf the buffer image to draw to
   * @param frameDims the dimensions of the image frame for which blob
   *   detection is being performed, in pixels
   * @param frameOffset the xy coordinates of the inset frame origin, in pixels
   */
  void drawPaths(
    PGraphics buf,
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
            point[0]*frameDims[0]+frameOffset[0]-BeeTracker.viewBounds[0],
            point[1]*frameDims[1]+frameOffset[1]-BeeTracker.viewBounds[1],
            point2[0]*frameDims[0]+frameOffset[0]-BeeTracker.viewBounds[0],
            point2[1]*frameDims[1]+frameOffset[1]-BeeTracker.viewBounds[1]
          );
        }
      }
    }
  }

  /**
   * Retrieves the visual summary of the currently recorded events.
   * @param parent the invoking BeeTracker
   * @param time the current playback time in seconds
   * @param duration the video duration in seconds
   * @return a PGraphics image of the event timeline
   */
  PGraphics getEventTimeline(
    BeeTracker parent,
    float time,
    float duration
  ) {
    if(BeeTracker.debug) {
      System.out.append("retrieving event timeline... ");
    }

    int color, yOffset;
    float xOffset = time/duration*369f + 26f;

    PGraphics img = parent.createGraphics(400, colors.size() * 75);
    img.beginDraw();

    img.background(0xffeeeeee);

    int halfDuration = (int)(duration*.5f);

    String begin = "00:00";
    String middle = String.format("%02d:%02d", halfDuration/60, halfDuration%60);
    String end = String.format("%02d:%02d", ((int)duration)/60, ((int)duration)%60);

    //timeline backgrounds
    for(int i = 1; i <= colors.size(); i++) {
      color = colors.get(i-1);
      yOffset = 75*i;

      img.fill(0xff000000);

      img.textAlign(BeeTracker.RIGHT);
      img.text(end, 395, yOffset-10);
      img.line(25, yOffset-30, 25, yOffset-25);

      img.textAlign(BeeTracker.CENTER);
      img.text(middle, 210, yOffset-10);
      img.line(210, yOffset-30, 210, yOffset-25);

      img.textAlign(BeeTracker.LEFT);
      img.text(begin, 25, yOffset-10);
      img.line(395, yOffset-30, 395, yOffset-25);

      img.text("color:", 25, yOffset-57);

      if(waggleMode) {
        img.text("W", 7, yOffset-40);
      } else {
        img.text("A", 7, yOffset-40);
        img.text("D", 7, yOffset-30);
      }

      img.strokeWeight(1);
      img.stroke(0xff000000);
      img.fill(0xffcccccc);
      img.rectMode(BeeTracker.CORNER);
      img.rect(25, yOffset-50, 370, 20);

      img.fill(0xff000000 + color);
      img.text(String.format("%06x", color), 65, yOffset-57);
    }

    img.ellipseMode(BeeTracker.CENTER);

    Stack<float[]> intervals;

    for(int i = 1; i <= colors.size(); i++) {
      color = colors.get(i-1);
      yOffset = 75*i;

      img.rectMode(BeeTracker.CORNERS);
      img.noStroke();

      //mark intervals with detected bees
      intervals = allIntervals.get(color);
      for(float[] xBounds : intervals) {
        img.fill(0xff000000 + color);
        img.rect(
          xBounds[0],
          yOffset-45,
          xBounds[1],
          yOffset-35
        );
      }

      img.strokeWeight(1);
      img.stroke(0xff000000);
      img.fill(0xff000000 + color);
      img.rectMode(BeeTracker.CENTER);

      if(waggleMode) {
        //mark waggle dance detections
        for(float stamp : waggleTimes.get(color)) {
          img.triangle(
            stamp/duration*369 + 26,
            yOffset-42.5f,
            stamp/duration*369 + 23.5f,
            yOffset-37.5f,
            stamp/duration*369 + 28.5f,
            yOffset-37.5f
          );
        }
      } else {
        //mark arrivals
        for(float stamp : arrivalTimes.get(color)) {
          img.rect(
            stamp/duration*369 + 26,
            yOffset-45,
            5,
            5
          );
        }

        //mark departures
        for(float stamp : departureTimes.get(color)) {
          img.ellipse(
            stamp/duration*369 + 26,
            yOffset-35,
            5,
            5
          );
        }
      }

      img.stroke(0xff000000);
      img.line(25, yOffset-40, 395, yOffset-40);

      //mark current timestamp
      img.line(
        xOffset,
        yOffset-49,
        xOffset,
        yOffset-31
      );
      img.line(0, yOffset, 400, yOffset);
    }

    img.endDraw();

    if(BeeTracker.debug) {
      System.out.append("done\n").flush();
    }

    return img;
  }

  /**
   * Performs update operations for the visual summary of events.
   * @param time the current playback time in seconds
   * @param duration the video duration in seconds
   */
  private void updateEventTimeline(float time, float duration) {
    if(BeeTracker.debug) {
      System.out.print("updating event timeline... ");
    }

    int color;
    Stack<float[]> intervals;
    float[] intervalXBounds;

    for(int i = 1; i <= colors.size(); i++) {
      color = colors.get(i-1);

      intervals = allIntervals.get(color);

      intervalXBounds = intervals.peek();

      if(allPaths.get(color).isEmpty()) {
        if(intervalXBounds[1] != Float.NEGATIVE_INFINITY) {
          intervals.push(new float[]{Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY});
        }
      } else {
        intervalXBounds[1] = time/duration*369f + 26f;

        if(intervalXBounds[0] == Float.NEGATIVE_INFINITY) {
          intervalXBounds[0] = intervalXBounds[1];
        }
      }
    }

    if(BeeTracker.debug) {
      System.out.println("done");
    }
  }

  /**
   * Sets the event detection type.
   * @param type true for waggle dance detection
   */
  void setEventType(boolean type) {
    waggleMode = type;
  }
}
