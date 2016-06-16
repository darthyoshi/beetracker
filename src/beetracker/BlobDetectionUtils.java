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

import blobDetection.Blob;
import blobDetection.BlobDetection;
import blobDetection.EdgeVertex;

import processing.core.PImage;
import processing.core.PGraphics;
import processing.data.IntList;
import processing.opengl.PShader;

/**
 * @class BlobDetectionUtils
 * @author Kay Choi
 * @date 15 Jun 16
 * @description Handles all BeeTracker blob-related operations.
 */
class BlobDetectionUtils {
  private final BeeTracker parent;
  private static final float filterRadius = 6f;
  private final BlobDetection bd;
  private final PShader thresholdShader, morphoShader, alphaShader;
  private PGraphics buf = null;
  private IntList validBlobs;

  /**
   * Class constructor.
   * @param parent the instantiating object
   * @param width the width of the images to process
   * @param height the height of the images to process
   * @param thresholdShader the shader to be used for color filtering
   * @param morphoShader the shader to be used for morphological opening and
   *   closing
   */
  BlobDetectionUtils(BeeTracker parent, int width, int height) {
    this.parent = parent;

    bd = new BlobDetection(width, height);
    bd.setPosDiscrimination(true);
    bd.setThreshold(.2f);

    thresholdShader = parent.loadShader("shaders/thresholdshader.glsl");
    morphoShader = parent.loadShader("shaders/morphoshader.glsl");
    morphoShader.set("filterRadius", filterRadius);
    alphaShader = parent.loadShader("shaders/alphashader.glsl");
  }

  /**
   * Preprocesses a PImage for blob detection. Any pixels meeting the defined
   *   thresholds will have the hue values set to the nominal hue value and
   *   the saturation and brightness values maxed out, while all other pixels
   *   are set to 0.
   * @param img the PImage to preprocess
   * @param colors a list of the integer RGB values to scan for
   * @param threshold an array containing the HSV thresholds
   */
  void filterImg(PImage img, IntList colors, int[] threshold) {
    thresholdShader.set(
      "threshold",
      ((float)threshold[0])/255f,
      ((float)threshold[1])/255f,
      ((float)threshold[2])/255f
    );

    if(buf == null || buf.width != img.width || buf.height != img.height) {
      buf = parent.createGraphics(img.width, img.height, BeeTracker.P2D);
    }

    buf.beginDraw();
    buf.colorMode(BeeTracker.HSB, 1);
    buf.endDraw();

    buf.copy(img, 0, 0, img.width, img.height, 0, 0, buf.width, buf.height);

    alphaShader.set("init", true);
    buf.filter(alphaShader);

    for(int i = 0; i < colors.size(); i++) {
      thresholdShader.set("basehue", buf.hue(colors.get(i)));
      buf.filter(thresholdShader);
    }

    alphaShader.set("init", false);
    buf.filter(alphaShader);

    //fill blob holes
    morphImage(buf, true);
    morphImage(buf, false);

    //remove noise
    morphImage(buf, false);
    morphImage(buf, true);

    img.copy(buf, 0, 0, buf.width, buf.height, 0, 0, img.width, img.height);
  }

  /**
   * Draws the blobs in the current frame.
   * @param buf the buffer image to draw to
   * @param bufOffset the xy coordinates of the buffer image
   * @param frameDims the dimensions of the image frame for which blob
   *   detection is being performed, in pixels
   * @param frameOffset the xy coordinates of the inset frame origin, in pixels
   */
  void drawBlobs(
    PGraphics buf,
    int[] bufOffset,
    int[] frameDims,
    int[] frameOffset
  ) {
    EdgeVertex eA,eB;
    Blob b;

    buf.noFill();
    buf.strokeWeight(1);

    for (int n : validBlobs) {
      if ((b = bd.getBlob(n)) != null) {
        //mark edges all blobs
        buf.stroke(0xFFFFFFFF);
        for (int m = 0; m < b.getEdgeNb(); m++) {
          eA = b.getEdgeVertexA(m);
          eB = b.getEdgeVertexB(m);

          if (eA != null && eB != null) {
            buf.line(
              eA.x*frameDims[0] + frameOffset[0] - bufOffset[0],
              eA.y*frameDims[1] + frameOffset[1] - bufOffset[1],
              eB.x*frameDims[0] + frameOffset[0] - bufOffset[0],
              eB.y*frameDims[1] + frameOffset[1] - bufOffset[1]
            );
          }
        }

        //bounding boxes
        buf.rectMode(BeeTracker.CORNER);
        buf.rect(
          b.xMin*frameDims[0] + frameOffset[0] - bufOffset[0],
          b.yMin*frameDims[1] + frameOffset[1] - bufOffset[1],
          b.w*frameDims[0],
          b.h*frameDims[1]
        );
      }
    }
  }

  /**
   * Retrieves the centroids of the blobs in the current frame.
   * @param frame the filtered frame
   * @param colors the list of color values
   * @return a HashMap mapping RGB integer values to Lists of normalized xy
   *   coordinates of the detected blob centroids
   */
  HashMap<Integer, List<float[]>> getCentroids(PImage frame, IntList colors) {
    HashMap<Integer, List<float[]>> result = new HashMap<>(colors.size());
    float[] point;
    Blob b, b2;
    int i, j, k, color, pixel, hue;

    frame.loadPixels();
    bd.computeBlobs(frame.pixels);

    for(int tmpColor : colors) {
      result.put(tmpColor, new ArrayList<float[]>());
    }

    parent.colorMode(BeeTracker.HSB, 255);

    //index of unchecked blobs
    validBlobs = new IntList(bd.getBlobNb());

    IntList indices = new IntList(bd.getBlobNb());
    for(i = 0; i < bd.getBlobNb(); i++) {
      indices.append(i);
    }

    //iterate through colors
    for(j = 0; j < colors.size(); j++) {
      color = colors.get(j);
      hue = (int)parent.hue(color);

      //iterate through remaining blobs
      for(i = 0; i < indices.size(); i++) {
        if((b = bd.getBlob(indices.get(i))) != null) {
/*          //TODO discard small blobs
          if(blob is too small) {
            continue;
          }
*/
          //skip blobs that are too close to each other
          for(k = 0; k < indices.size(); k++) {
            if(i != k && (b2 = bd.getBlob(indices.get(k))) != null) {
              if(BeeTracker.dist(b.x, b.y, b2.x, b2.y) <
                0.5f*(BeeTracker.mag(b.w, b.h) + BeeTracker.mag(b2.w, b2.h))) {
                indices.remove(k);
              }
            }
          }

          point = new float[] {b.x, b.y};
          pixel = frame.pixels[
           (int)(b.y*frame.height)*frame.width +
           (int)(b.x*frame.width)
          ];

          //case: centroid is in blob
          if(parent.brightness(pixel) > 0f) {
            if((int)parent.hue(pixel) <= hue+5 && (int)parent.hue(pixel) >= hue-5) {
              result.get(color).add(point);

              //remove blob from further consideration
              validBlobs.append(indices.get(i));
              indices.remove(i--);
            }
          } else {  //case: centroid is not in blob
            loop:
            for(
              k = (int)(b.yMin*frame.height);
              k < (int)(b.yMax*frame.height);
              k++
            ) {
              for(
                int l = (int)(b.xMin*frame.width);
                l < (int)(b.xMax*frame.width);
                l++
              ) {
                pixel = frame.pixels[k*frame.width + l];

                if(parent.brightness(pixel) > 0f &&
                  (int)parent.hue(pixel) == hue)
                {
                  result.get(color).add(point);

                  validBlobs.append(indices.get(i));
                  indices.remove(i--);

                  break loop;
                }
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * @return the expected width of the images to process
   */
  int getImageWidth() {
    return bd.imgWidth;
  }

  /**
   * @return the expected height of the images to process
   */
  int getImageHeight() {
    return bd.imgHeight;
  }

  /**
   * Performs a morphological operation. Any non-transparent blobs in the
   *   the buffer will either grow or shrink.
   * @param buf the operand image
   * @param dilateMode true for dilation, false for erosion
   */
  private void morphImage(PGraphics buf, boolean dilateMode) {
    morphoShader.set("dilateMode", dilateMode);
    buf.filter(morphoShader);
  }
}
