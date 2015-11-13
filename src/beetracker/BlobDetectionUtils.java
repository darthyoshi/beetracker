/**
 * @file BlobDetectionUtils.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description Handles all BeeTracker blob-related operations.
 */

package beetracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import blobDetection.Blob;
import blobDetection.BlobDetection;
import blobDetection.EdgeVertex;

import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PGraphics;
import processing.data.IntList;
import processing.opengl.PShader;

/**
 *
 * @author Kay Choi
 */
class BlobDetectionUtils {
    private final BeeTracker parent;
    private static final int filterRadius = 5;
    private final BlobDetection bd;
    private IntList blobColors;
    private final PShader thresholdShader, morphoShader, alphaShader;
    private final PGraphics buf;

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

        blobColors = new IntList();

        bd = new BlobDetection(width, height);
        bd.setPosDiscrimination(true);
        bd.setThreshold(.2f);

        thresholdShader = parent.loadShader("shaders/thresholdshader.glsl");
        morphoShader = parent.loadShader("shaders/morphoshader.glsl");
        morphoShader.set("filterRadius", (float)filterRadius);
        alphaShader = parent.loadShader("shaders/alphashader.glsl");

        buf = parent.createGraphics(width, height, BeeTracker.P2D);
        buf.beginDraw();
        buf.colorMode(BeeTracker.HSB, 1);
        buf.endDraw();
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
        morphImage(true);
        morphImage(false);

        //remove noise
        morphImage(false);
        morphImage(true);

        img.copy(buf, 0, 0, buf.width, buf.height, 0, 0, img.width, img.height);
    }

    /**
     * Draws the blobs in the current frame.
     * @param buf the buffer image to draw to
     * @param bufOffset the xy coordinates of the buffer image
     * @param frameDims the dimensions of the image frame for which blob
     *   detection is being performed, in pixels
     * @param frameOffset the xy coordinates of the inset frame origin, in pixels
     * @param exitXY the xy coordinates of the exit center, in pixels
     */
    void drawBlobs(
        processing.core.PGraphics buf,
        int[] bufOffset,
        int[] frameDims,
        int[] frameOffset,
        float[] exitXY
    ) {
        EdgeVertex eA,eB;
        Blob b;

        buf.noFill();
        buf.strokeWeight(1);

        for (int n = 0; n < bd.getBlobNb(); n++) {
            b = bd.getBlob(n);
            if (b != null) {

                //mark edges all blobs
                buf.stroke(0xFFFF00AA);
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
                buf.stroke(blobColors.get(n));
                buf.rectMode(PConstants.CORNER);
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
    HashMap<Integer, List<float[]>> getCentroids(PImage frame,
        IntList colors)
    {
        HashMap<Integer, List<float[]>> result = new HashMap<>(colors.size());
        float[] point;
        Blob b;
        int i, j, color, pixel, hue;
        boolean added;

        frame.loadPixels();
        bd.computeBlobs(frame.pixels);

        blobColors = new IntList();

        for(int tmpColor : colors) {
            result.put(tmpColor, new ArrayList<float[]>());
        }

        parent.colorMode(BeeTracker.HSB, 255);

        //iterate through blobs
        for(i = 0; i < bd.getBlobNb(); i++) {
            added = false;

            b = bd.getBlob(i);

            point = new float[2];
            point[0] = b.x;
            point[1] = b.y;

            for(j = 0; j < colors.size(); j++) {
                color = colors.get(j);
                hue = (int)parent.hue(color);
                pixel = frame.pixels[
                     (int)(b.y*frame.height)*frame.width +
                     (int)(b.x*frame.width)
                ];

                //case: centroid is in blob
                if(parent.brightness(pixel) > 0f && (int)parent.hue(pixel) == hue) {
                    result.get(color).add(point);

                    blobColors.append(parent.color(hue, 255, 255));

                    added = true;
                    break;
                }

                //case: centroid is not in blob
                else {
                    loop:
                    for(
                        int k = (int)(b.yMin*frame.height);
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

                                blobColors.append(parent.color(hue, 255, 255));

                                added = true;
                                break loop;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * @return the width of the images to process
     */
    int getImageWidth() {
        return bd.imgWidth;
    }

    /**
     * @return the height of the images to process
     */
    int getImageHeight() {
        return bd.imgHeight;
    }

    /**
     * Performs a morphological operation. Any non-transparent blobs in the
     *   the buffer will either grow or shrink.
     * @param dilateMode true for dilation, false for erosion
     */
    private void morphImage(boolean dilateMode) {
        morphoShader.set("dilateMode", dilateMode);
        buf.filter(morphoShader);
    }
}
