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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @class ShapeRecognizer
 * @author Kay Choi
 * @date 28 Aug 16
 * @description Provides shape recognition for waggle dance detection.
 */
public class ShapeRecognizer {
  private static final int shapeScore = 70;
  private boolean status = false;
  private static final int rate = 32;
  private static final float minWaggleSize = 0.01f;
  private static final int timeOut = 10;
  private final BeeTracker root; 

  de.voidplus.dollar.OneDollar oneDollar;

  /**
   * Class constructor.
   * @param root the BeeTracker object
   */
  ShapeRecognizer(BeeTracker root) {
    this.root = root;
    oneDollar = new de.voidplus.dollar.OneDollar(root)
      .setMinSimilarity(shapeScore)
      .setVerbose(BeeTracker.debug)
      .setMaxTime(timeOut*1000)
      .setFragmentationRate(rate)
      .disableAutoCheck();
  }

  /**
   * Loads the template gestures into the recognizer.
   * @param root the BeeTracker object
   */
  void loadTemplates(BeeTracker root) {
    readOneDollarTemplate(root, "waggle");
    oneDollar.bind("waggle", this, "oneDollarCallback");

//    readOneDollarTemplate(root, "waggle-vert");
//    oneDollar.bind("waggle-vert", this, "oneDollarCallback");

    readOneDollarTemplate(root, "waggle2");
    oneDollar.bind("waggle2", this, "oneDollarCallback");

//    oneDollar.setMinSimilarity(30);
//    readOneDollarTemplate(root, "circle");
//    oneDollar.on("circle", this, "oneDollarCallback");
  }

  /**
   * Reads a gesture template into the $1 object.
   * @param root the root BeeTracker
   * @param fileName the template file name
   */
  private void readOneDollarTemplate(BeeTracker root, String fileName) {
    LinkedList<int[]> path = new LinkedList<>();
    int[] point;
    java.io.BufferedReader reader = null;
    String line;
    String[] split;
    try {
      reader = new java.io.BufferedReader(new java.io
        .InputStreamReader(root.createInputRaw("paths/"+fileName+".xml"), "UTF-8"));

      while((line = reader.readLine()) != null) {
        if(line.startsWith("<Point")) {
          split = line.split("\\\"");
          point = new int[] {(int)Float.parseFloat(split[1]),
            (int)Float.parseFloat(split[3])};
          path.add(point);
        }
      }
    } catch (java.io.IOException ex) {
      ex.printStackTrace(System.err);
    } finally {
      if(reader != null) {
        try {
          reader.close();
        } catch (java.io.IOException ex) {
          ex.printStackTrace(System.err);
        }
      }
    }
    int[] array = new int[path.size()*2];
    Iterator<int[]> iter = path.descendingIterator();
    for(int i = 0; i < array.length; i += 2) {
      point = iter.next();
      array[i] = point[0];
      array[i+1] = point[1];
    }
    oneDollar.learn(fileName, array);
  }

  /**
   * Checks a path for the waggle dance.
   * @param path a Deque of normalized float pairs representing a path
   * @param frameDims the dimensions of the inset frame
   */
  void recognize(java.util.Deque<float[]> path, int[] frameDims) {
    float[] normPoint, absPoint;
    int i;
    float xMin, xMax, yMin, yMax;

    LinkedList<float[]> candidateList = new LinkedList<>();
    int[] candidateArray;

    status = false;

    //check path starting from most recent point
    xMin = yMin = Float.MAX_VALUE;
    xMax = yMax = Float.MIN_VALUE;
    int timer = timeOut*root.fps;
    Iterator<float[]> iter = path.descendingIterator();
    while(iter.hasNext() && timer > 0) {
      normPoint = iter.next();
      absPoint = new float[] {normPoint[0]*frameDims[0], normPoint[1]*frameDims[1]};

      //calc path bounding box
      if(normPoint[0] < xMin) {
        xMin = normPoint[0];
      }
      if(normPoint[0] > xMax) {
        xMax = normPoint[0];
      }
      if(normPoint[1] < yMin) {
        yMin = normPoint[1];
      }
      if(normPoint[1] > yMax) {
        yMax = normPoint[1];
      }

      candidateList.add(absPoint);

      candidateArray = new int[candidateList.size()*2];
      i = 0;
      for(float[] tmpPoint : candidateList) {
        candidateArray[i] = (int)tmpPoint[0];
        i++;
        candidateArray[i] = (int)tmpPoint[1];
        i++;
      }

      float dX = xMax - xMin; 
      float dY = 1f*(yMax-yMin)*frameDims[1]/frameDims[0];
      if(BeeTracker.debug) {
        System.out.print("path bounding box: " + dX + " " + dY + "\ncheck path: ");
      }
      //ignore paths with insufficiently large bounding boxes
      if(dX > minWaggleSize && dY > minWaggleSize) {
        if(BeeTracker.debug) {
          System.out.println(true);
        }
        oneDollar.check(candidateArray);

        //current path contains recognized gesture, no need to continue
        if(status) {
          break;
        }
      } else if(BeeTracker.debug){
        System.out.println(false);
      }

      timer--;
    }
  }

  /**
   * Updates a path for tracking the waggle dance.
   * @param color the hexadecimal color associated with the path
   * @param pathID
   * @param path a Deque of normalized float pairs representing a path
   * @param frameDims the dimensions of the inset frame
   */
  void trackPath(int color, int pathID, java.util.Deque<float[]> path, int[] frameDims) {
    float[] point = path.peekLast();
    oneDollar.track(color*0x100+pathID, point[0]*frameDims[0], point[1]*frameDims[1]);

    //check points from last 5s
    Iterator<float[]> iter = path.descendingIterator();
    int timer = timeOut*root.fps;
    float xMin, xMax, yMin, yMax;
    xMin = yMin = Float.MAX_VALUE;
    xMax = yMax = Float.MIN_VALUE;
    while(iter.hasNext() && timer > 0) {
      point = iter.next();
      if(point[0] < xMin) {
        xMin = point[0];
      }
      if(point[0] > xMax) {
        xMax = point[0];
      }
      if(point[1] < yMin) {
        yMin = point[1];
      }
      if(point[1] > yMax) {
        yMax = point[1];
      }

      timer--;
    }

    float dX = xMax - xMin; 
    float dY = 1f*(yMax-yMin)*frameDims[1]/frameDims[0];
    if(BeeTracker.debug) {
      System.out.print("path bounding box: " + dX + " " + dY + "\ncheck path: ");
    }
    //ignore paths with insufficiently large bounding boxes within last 5s
    if(dX > minWaggleSize && dY > minWaggleSize) {
      if(BeeTracker.debug) {
        System.out.println(true);
      }

      oneDollar.check();
    } else if(BeeTracker.debug) {
      System.out.println(false);
    }
  }

  /**
   * @return true if the current gesture candidate matches a template
   */
  boolean isCandidateRecognized() {
    return status;
  }

  /**
   * $1 recognizer callback method.
   * @param gestureName
   * @param percentOfSimilarity
   * @param startX
   * @param startY
   * @param centroidX
   * @param centroidY
   * @param endX
   * @param endY
   */
  public void oneDollarCallback(
    String gestureName,
    float percentOfSimilarity,
    int startX,
    int startY,
    int centroidX,
    int centroidY,
    int endX,
    int endY
  ) {
    status = true;
  }
}
