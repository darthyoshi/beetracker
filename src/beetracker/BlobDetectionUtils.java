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

/**
 *
 * @author Kay Choi
 */
public class BlobDetectionUtils {
    private final BeeTracker parent;
    private static final int filterRadius = 5;
    private final BlobDetection bd;
    private IntList blobColors;

    /**
     * Class constructor.
     * @param parent the instantiating object
     * @param width the width of the images to process
     * @param height the height of the images to process
     */
    public BlobDetectionUtils(BeeTracker parent, int width, int height) {
        this.parent = parent;

        blobColors = new IntList();

        bd = new BlobDetection(width, height);
        bd.setPosDiscrimination(true);
        bd.setThreshold(.2f);
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
    public void filterImg(PImage img, IntList colors, int[] threshold) {
        int pixelHue, pixelSat, pixelVal;
        int i, j;

        int[] listHues = new int[colors.size()];
        for(i = 0; i < listHues.length; i++) {
            listHues[i] = (int)parent.hue(colors.get(i));
        }

        img.loadPixels();

        parent.colorMode(PConstants.HSB, 255);

        //scan every pixel in image
        scanPixel:
        for(i = 0; i < img.pixels.length; i++) {
            pixelHue = (int)parent.hue(img.pixels[i]);
            pixelSat = (int)parent.saturation(img.pixels[i]);
            pixelVal = (int)parent.brightness(img.pixels[i]);

            //for color matches, brighten pixel
            for(j = 0; j < colors.size(); j++) {
                if(pixelHue > listHues[j] - threshold[0] &&
                    pixelHue < listHues[j] + threshold[0] &&
                    pixelSat > threshold[1] &&
                    pixelVal > threshold[2])
                {
                    img.pixels[i] = parent.color(listHues[j], 255, 255);

                    continue scanPixel;
                }
            }

            //if no matches found, darken pixel
            img.pixels[i] = 0;
        }

        //fill blob holes
        dilateImage(img.pixels);
        erodeImage(img.pixels, colors);

        //remove noise
        erodeImage(img.pixels, colors);
        dilateImage(img.pixels);

        img.updatePixels();
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
    public void drawBlobs(
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
    public HashMap<Integer, List<float[]>> getCentroids(PImage frame,
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
     * Performs a morphological erosion operation. Any blobs consisting of the
     *   specified colors will shrink. All other pixels will be set to black.
     * @param pixels the operand pixel array
     * @param colors a list of the integer RGB values to scan for
     */
    private void erodeImage(int[] pixels, IntList colors) {
        int[] tmp = new int[pixels.length];
        int i, j, k, l, offset;

        for(i = 0; i < tmp.length; i++) {
            tmp[i] = 0;
        }

        //iterate image x-axis
        for(i = filterRadius; i < bd.imgWidth - filterRadius; i++) {
            //iterate image y-axis
            for(j = filterRadius; j < bd.imgHeight - filterRadius; j++) {
                erodeProbe:
                //iterate colors
                for(Integer color : colors) {
                    //iterate filter x-axis
                    for(k = -filterRadius; k <= filterRadius; k++) {
                        //iterate filter y-axis
                        for(l = -filterRadius; l <= filterRadius; l++) {
                            if(Math.abs(k) + Math.abs(l) <= (double)filterRadius) {
                                offset = (i+k) + (j+l)*bd.imgWidth;

                                if(!(
                                    (int)parent.hue(pixels[offset]) ==
                                    (int)parent.hue(color) &&
                                    parent.brightness(pixels[offset]) > 0f
                                )) {
                                    //current pixel is miss, go to next color
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

        //iterate image x-axis
        for(i = 0; i < bd.imgWidth; i++) {
            dilateProbe:
            //iterate image y-axis
            for(j = 0; j < bd.imgHeight; j++) {
                //iterate filter x-axis
                for(k = -filterRadius; k <= filterRadius; k++) {
                    //iterate filter y-axis
                    for(l = -filterRadius; l <= filterRadius; l++) {
                        m = i + k;
                        n = j + l;

                        if(
                            m >= 0 && m < bd.imgWidth &&
                            n >= 0 && n < bd.imgHeight &&
                            Math.abs(k) + Math.abs(l) <= (double)filterRadius
                        ) {
                            offset = m + n*bd.imgWidth;

                            if(parent.brightness(pixels[offset]) > 0) {
                                tmp[i + j*bd.imgWidth] = pixels[offset];

                                //current image pixel is hit, go to next pixel
                                continue dilateProbe;
                            }
                        }
                    }
                }

                tmp[i + j*bd.imgWidth] = 0;
            }
        }

        for(i = 0; i < tmp.length; i++) {
            pixels[i] = tmp[i];
        }
    }
}
