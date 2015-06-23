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

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.data.IntList;

public class BlobDetectionUtils {
    private final BeeTracker parent;
    private int hueThreshold, satThreshold, valThreshold;
    private static final int filterRadius = 5;
    private final BlobDetection bd;
    private IntList blobColors;

    private final boolean debug;

    /**
     * Class constructor.
     * @param parent the instantiating object
     * @param width the width of the images to process
     * @param height the height of the images to process
     * @param debug whether debug mode is enabled
     */
    public BlobDetectionUtils(BeeTracker parent, int width, int height, boolean debug) {
        this.parent = parent;
        this.debug = debug;

        if(debug) {
            blobColors = new IntList();
        }

        bd = new BlobDetection(width, height);
        bd.setPosDiscrimination(true);
        bd.setThreshold(.2f);
    }

    /**
     * Preprocesses a PImage for blob detection. Any pixels meeting the defined
     *   hue and saturation thresholds have the saturation and brightness values
     *   maxed out, while all other pixels are set to 0x000000.
     * @param img the PImage to preprocess
     * @param colors a list of the integer RGB values to scan for
     */
    public void filterImg(PImage img, IntList colors) {
        int pixelHue, listHue, pixelSat, pixelVal;
        int i, j;

        img.loadPixels();

        parent.colorMode(PConstants.HSB, 255);

        //scan every pixel in image
        for(i = 0; i < img.pixels.length; i++) {
            pixelHue = (int)parent.hue(img.pixels[i]);
            pixelSat = (int)parent.saturation(img.pixels[i]);
            pixelVal = (int)parent.brightness(img.pixels[i]);

            //for color matches, brighten pixel
            for(j = 0; j < colors.size(); j++) {
                listHue = (int)parent.hue(colors.get(j));

                if(pixelHue > listHue - hueThreshold &&
                    pixelHue < listHue + hueThreshold &&
                    pixelSat > satThreshold &&
                    pixelVal > valThreshold)
                {
                    img.pixels[i] = parent.color(listHue, 255, 255);

                    break;
                }
            }

            //if no matches found, darken pixel
            if(j == colors.size()) {
                img.pixels[i] = parent.color(0);
            }
        }

        //remove noise
        erodeImage(img.pixels, colors);
        dilateImage(img.pixels);

        //fill blob holes
        dilateImage(img.pixels);
        erodeImage(img.pixels, colors);

        img.updatePixels();
    }

    /**
     * Draws the blobs in the current frame.
     * @param frameDims the dimensions of the image frame for which blob
     *   detection is being performed, in pixels
     * @param offset the xy coordinates of the image frame origin, in pixels
     * @param exitXY the xy coordinates of the exit center, in pixels
     */
    public void drawBlobs(int[] frameDims, int[] offset, float[] exitXY) {
        EdgeVertex eA,eB;
        Blob b;

        parent.noFill();

        for (int n = 0; n < bd.getBlobNb(); n++) {
            b = bd.getBlob(n);
            if (b != null) {
                parent.strokeWeight(1);

                //mark edges all blobs
                parent.stroke(0xFFFF00AA);
                for (int m = 0; m < b.getEdgeNb(); m++) {
                    eA = b.getEdgeVertexA(m);
                    eB = b.getEdgeVertexB(m);

                    if (eA !=null && eB !=null) {
                        parent.line(
                            eA.x*frameDims[0] + offset[0],
                            eA.y*frameDims[1] + offset[1],
                            eB.x*frameDims[0] + offset[0],
                            eB.y*frameDims[1] + offset[1]
                        );
                    }
                }

                //bounding boxes
                parent.stroke(blobColors.get(n));
                parent.rectMode(PConstants.CORNER);
                parent.rect(
                    b.xMin*frameDims[0] + offset[0],
                    b.yMin*frameDims[1] + offset[1],
                    b.w*frameDims[0],
                    b.h*frameDims[1]
                );

                //line to exit center
                parent.strokeWeight(2);
                parent.stroke(255, 0, 255);
                parent.line(
                    b.x*frameDims[0]+offset[0],
                    b.y*frameDims[1]+offset[1],
                    exitXY[0],
                    exitXY[1]
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
    public HashMap<Integer, List<float[]>> getCentroids(PImage frame,
        IntList colors)
    {
        HashMap<Integer, List<float[]>> result = new HashMap<>();
        float[] point;
        Blob b;
        int i, j, color, pixel, hue;
        boolean added;

        frame.loadPixels();
        bd.computeBlobs(frame.pixels);

        if(debug) {
            blobColors = new IntList();

            PApplet.println("blobs: " + bd.getBlobNb());
        }

        for(int tmpColor : colors) {
            result.put(tmpColor, new ArrayList<float[]>());
        }

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

                    if(debug) {
                        blobColors.append(parent.color(hue, 255, 255));
                    }

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

                                if(debug) {
                                    blobColors.append(parent.color(hue, 255, 255));
                                }

                                added = true;
                                break loop;
                            }
                        }
                    }
                }
            }

            if(debug && added) {
                PApplet.println(point[0] + ", " + point[1]);
            }
        }

        return result;
    }

    /**
     * @return the width of the images to process
     */
    public int getImageWidth() {
        return bd.imgWidth;
    }

    /**
     * @return the height of the images to process
     */
    public int getImageHeight() {
        return bd.imgHeight;
    }

    /**
     * Sets the filter threshold values.
     * @param type the threshold type:
     *   0 = hue
     *   1 = saturation
     *   2 = brightness
     * @param loVal the new value
     */
    public void setThresholdValue(int type, int val) {
        switch(type) {
        case 0: hueThreshold = val; break;

        case 1: satThreshold = val; break;

        case 2: valThreshold = val;
        }
    }

    /**
     * Performs a morphological erosion operation. Any blobs consisting of the
     *   specified colors will shrink. All other pixels will be set to black.
     * @param pixels the operand pixel array
     * @param colors a list of the integer RGB values to scan for
     */
    private void erodeImage(int[] pixels, IntList colors) {
        int[] tmp = new int[pixels.length];
        int i, j, k, l, m, offset;

        for(i = 0; i < tmp.length; i++) {
            tmp[i] = 0xff000000;
        }

        for(i = filterRadius; i < bd.imgWidth - filterRadius; i++) {
            for(j = filterRadius; j < bd.imgHeight - filterRadius; j++) {
                erodeProbe:
                for(m = 0; m < colors.size(); m++) {
                    for(k = -filterRadius; k <= filterRadius; k++) {
                        for(l = -filterRadius; l <= filterRadius; l++) {
                            if(Math.abs(k) + Math.abs(l) <= filterRadius) {
                                offset = (i+k) + (j+l)*bd.imgWidth;

                                if(!(
                                    (int)parent.hue(pixels[offset]) ==
                                    (int)parent.hue(colors.get(m)) &&
                                    parent.brightness(pixels[offset]) > 0f
                                )) {
                                    continue erodeProbe;
                                }
                            }
                        }
                    }

                    offset = i + j*bd.imgWidth;
                    tmp[offset] = pixels[offset];
                }
            }
        }

        for(i = 0; i < tmp.length; i++) {
            pixels[i] = tmp[i];
        }
    }

    /**
     * Performs a morphological dilation operation. Any non-black blobs will
     *   grow.
     * @param pixels the operand pixel array
     */
    private void dilateImage(int[] pixels) {
        int[] tmp = new int[pixels.length];
        int i, j, k, l, m, n, offset;

        for(i = 0; i < bd.imgWidth; i++) {
            dilateProbe:
            for(j = 0; j < bd.imgHeight; j++) {
                for(k = -filterRadius; k <= filterRadius; k++) {
                    for(l = -filterRadius; l <= filterRadius; l++) {
                        m = i + k;
                        n = j + l;

                        if(
                            m >= 0 && m < bd.imgWidth &&
                            n >= 0 && n < bd.imgHeight &&
                            Math.abs(k) + Math.abs(l) <= filterRadius
                        ) {
                            offset = m + n*bd.imgWidth;

                            if(parent.brightness(pixels[offset]) > 0) {
                                tmp[i + j*bd.imgWidth] = pixels[offset];

                                continue dilateProbe;
                            }
                        }
                    }
                }

                tmp[i + j*bd.imgWidth] = 0xff000000;
            }
        }

        for(i = 0; i < tmp.length; i++) {
            pixels[i] = tmp[i];
        }
    }

    /**
     * Retrieves the values of the specified threshold.
     * @param type the threshold type:
     *   0 = hue
     *   1 = saturation
     *   2 = brightness
     * @return an integer array
     */
    public int getThresholdValue(int type) {
        int result = 0;
        switch(type) {
        case 0: result = hueThreshold; break;

        case 1: result = satThreshold; break;

        case 2: result = valThreshold;
        }

        return result;
    }
}
