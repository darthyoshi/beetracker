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
import java.util.Arrays;
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
 * @date 22 Jun 16
 * @description Handles all BeeTracker blob-related operations.
 */
class BlobDetectionUtils {
  private final BeeTracker parent;
  private static final float[] filterRadius = {4f, 6f};
  private final BlobDetection bd;
  private final PShader thresholdShader, morphoShader, alphaShader, maskShader;
  private PGraphics buf = null, exitBuf = null;
  private IntList validBlobs;
  private int[] threshold = null;
  private int[] bufParams = null;
  private float[] exitBufParams = null;
  private boolean waggleMode = false;

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
    maskShader = parent.loadShader("shaders/maskshader.glsl");
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
    if(!Arrays.equals(this.threshold, threshold)) {
      this.threshold = Arrays.copyOf(threshold, threshold.length);
      thresholdShader.set(
        "threshold",
        new float[] {
          ((float)threshold[0])/255f,
          ((float)threshold[1])/255f,
          ((float)threshold[2])/255f,
          ((float)threshold[3])/255f,
          ((float)threshold[4])/255f
        }
      );
    }

    if(buf == null || buf.width != img.width || buf.height != img.height) {
      buf = parent.createGraphics(img.width, img.height, BeeTracker.P2D);
      buf.beginDraw();
      buf.colorMode(BeeTracker.HSB, 1);
      buf.endDraw();
      bufParams = new int[] {0, 0, buf.width, buf.height};

      exitBuf = parent.createGraphics(img.width, img.height, BeeTracker.P2D);
      exitBuf.beginDraw();
      exitBuf.colorMode(BeeTracker.HSB, 1);
      exitBuf.endDraw();
    }

    applyShader(img, buf, bufParams, false, colors);

    if(!waggleMode) {
      applyShader(
        img,
        exitBuf,
        new int[]{
          (int)((exitBufParams[0]-exitBufParams[2])*img.width),
          (int)((exitBufParams[1]-exitBufParams[3])*img.height),
          2*(int)(exitBufParams[2]*img.width),
          2*(int)(exitBufParams[3]*img.height)
        },
        true,
        colors
      );
      exitBuf.filter(maskShader);
    }

    img.copy(buf, 0, 0, buf.width, buf.height, 0, 0, img.width, img.height);
    img.blend(exitBuf, 0, 0, exitBuf.width, exitBuf.height, 0, 0, img.width, img.height, BeeTracker.LIGHTEST);
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
    int i, j, k, l, index1, index2, color, pixel, hue, numBlobPixels;

    frame.loadPixels();
    bd.computeBlobs(frame.pixels);

    for(int tmpColor : colors) {
      result.put(tmpColor, new ArrayList<float[]>());
    }

    parent.colorMode(BeeTracker.HSB, 255);

    //index of accepted blobs
    validBlobs = new IntList(bd.getBlobNb());

    IntList unCheckedIndices = new IntList(bd.getBlobNb());
    for(i = 0; i < bd.getBlobNb(); i++) {
      unCheckedIndices.append(i);
    }

    IntList skipIndices;

    //iterate through colors
    for(j = 0; j < colors.size(); j++) {
      color = colors.get(j);
      hue = (int)parent.hue(color);

      //discard problematic blobs
      for(i = unCheckedIndices.size() - 1; i >= 0; i--) {
        index1 = unCheckedIndices.get(i);

        if(BeeTracker.debug) {
          System.out.println("#blobs left: " + unCheckedIndices.size());
        }

        if((b = bd.getBlob(index1)) != null) {
          //blob is too thin
          if(b.h/b.w*frame.height/frame.width < 0.5f || b.h/b.w*frame.height/frame.width > 2f) {
            unCheckedIndices.remove(i);
            continue;
          }
/*
          if((float)(numBlobPixels = getBlobArea(frame, b, hue))/
            (frame.width*frame.height*b.w*b.h) < 0.40f) {
            indices.remove(i);
            continue;
          }
*/
          skipIndices = new IntList(unCheckedIndices.size());

          //blob is too close to a larger blob
          for(k = 0; k < unCheckedIndices.size(); k++) {
            index2 = unCheckedIndices.get(k);
            if(index1 != index2 && (b2 = bd.getBlob(index2)) != null) {
              if(isOverlap(b, b2) &&
                getBlobArea(frame, b2, hue) < getBlobArea(frame, b, hue)) {
                if(!skipIndices.hasValue(index2)) {
                  skipIndices.append(index2);
                }
              }
            }
          }

          for(int skip : skipIndices) {
            if(unCheckedIndices.hasValue(skip)) {
              unCheckedIndices.removeValue(skip);
              i--;
            }
          }
        }
      }

      //iterate through unchecked blobs
      for(i = unCheckedIndices.size() - 1; i >= 0; i--) {
        index1 = unCheckedIndices.get(i);
        if((b = bd.getBlob(index1)) != null) {
          point = new float[] {b.x, b.y};
          pixel = frame.pixels[
           (int)(b.y*frame.height)*frame.width +
           (int)(b.x*frame.width)
          ];

          //case: centroid is in blob
          if(parent.brightness(pixel) > 0f) {
            if((int)parent.hue(pixel) <= hue+5 &&
              (int)parent.hue(pixel) >= hue-5) {
              result.get(color).add(point);

              //remove blob from further consideration
              validBlobs.append(index1);
              unCheckedIndices.remove(i);
            }
          } else {  //case: centroid is not in blob
            loop:
            for(
              k = (int)(b.yMin*frame.height);
              k < (int)(b.yMax*frame.height);
              k++
            ) {
              for(
                l = (int)(b.xMin*frame.width);
                l < (int)(b.xMax*frame.width);
                l++
              ) {
                pixel = frame.pixels[k*frame.width + l];

                if(
                  parent.brightness(pixel) > 0f &&
                  (int)parent.hue(pixel) <= hue+5 &&
                  (int)parent.hue(pixel) >= hue-5
                ) {
                  result.get(color).add(point);

                  validBlobs.append(index1);
                  unCheckedIndices.remove(i);

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

  /**
   * Sets the exit boundary parameters for noise filtering.
   * @param exitCenter the exit center, normalized to the inset frame
   * @param exitAxes the exit semi-major axes, normalized to the inset frame
   */
  void setExit(float[] exitCenter, float[] exitAxes) {
    maskShader.set("exitParams", exitCenter[0], exitCenter[1],
      exitAxes[0], exitAxes[1]);
    exitBufParams = new float[] {exitCenter[0], exitCenter[1],
      exitAxes[0], exitAxes[1]};
  }

  /**
   * Uses shaders to extract colored pixels from an image. 
   * @param src the source image
   * @param dst the destination buffer
   * @param dstParams the area to copy
   * @param isExitFilter true if the filter is being applied to the exit circle
   * @param colors a list of the RGB values to scan for
   */
  private void applyShader(
    PImage src,
    PGraphics dst,
    int[] dstParams, 
    boolean isExitFilter,
    IntList colors
  ) {
    dst.beginDraw();
    dst.clear();
    dst.copy(
      src,
      dstParams[0], dstParams[1],
      dstParams[2], dstParams[3],
      dstParams[0], dstParams[1],
      dstParams[2], dstParams[3]
    );

    alphaShader.set("init", true);
    dst.filter(alphaShader);

    for(int i = 0; i < colors.size(); i++) {
      thresholdShader.set("basehue", buf.hue(colors.get(i)));
      dst.filter(thresholdShader);
    }

    alphaShader.set("init", false);
    dst.filter(alphaShader);

    morphoShader.set("exitMode", isExitFilter);

    //fill blob holes
    morphImage(dst, true);
    morphImage(dst, false);

    //remove noise
    morphImage(dst, false);
    morphImage(dst, true);

    dst.endDraw();
  }

  /**
   * Calculates the area, in pixels, of a blob. 
   * @param frame the source image
   * @param b the blob
   * @param hue the hue value of the blob
   * @return the number of pixels within the blob bounding box with the proper
   *   color
   */
  private int getBlobArea(PImage frame, Blob b, int hue) {
    int result = 0;
    int i, j, pixel;
    
    for(
      j = ((int)(b.yMin*frame.height));
      j < ((int)(b.yMax*frame.height));
      j++
    ) {
      for(
        i = ((int)(b.xMin*frame.width));
        i < ((int)(b.xMax*frame.width));
        i++
      ) {
        pixel = frame.pixels[j*frame.width + i];

        if(
          parent.brightness(pixel) > 0f &&
          (int)parent.hue(pixel) <= hue+5 &&
          (int)parent.hue(pixel) >= hue-5
        ) {
          result++;
        }
      }
    }

    if(BeeTracker.debug) {
      System.out.println("blob area: " + result);
    }

    return result;
  }

  /**
   * Sets the filter behavior based on event detection type.
   * @param waggleMode true for waggle dance detection
   */
  void setWaggleMode(boolean waggleMode) {
    this.waggleMode = waggleMode;
  }

  /**
   * @param b1 the first blob
   * @param b2 the second blob
   * @return true if the bounding boxes of b1 and b2 overlap
   */
  private boolean isOverlap(Blob b1, Blob b2) {
    boolean xOverlap = b1.x < b2.x ? b1.xMax > b2.xMin : b1.xMin < b2.xMax;
    boolean yOverlap = b1.y < b2.y ? b1.yMax > b2.yMin : b1.yMin < b2.yMax;

    if(BeeTracker.debug) {
      System.out.append("checking overlap").append('\n')
        .append("B1 (x): ").append(Float.toString(b1.xMin)).append(' ')
        .append(Float.toString(b1.xMax)).append('\n')
        .append("B1 (y): ").append(Float.toString(b1.yMin)).append(' ')
        .append(Float.toString(b1.yMax)).append('\n')
        .append("B2 (x): ").append(Float.toString(b2.xMin)).append(' ')
        .append(Float.toString(b2.xMax)).append('\n')
        .append("B2 (y): ").append(Float.toString(b2.yMin)).append(' ')
        .append(Float.toString(b2.yMax)).append('\n')
        .append("overlap: ").append(Boolean.toString(xOverlap && yOverlap))
        .append('\n').flush();
    }

    return xOverlap && yOverlap;
  }
}
