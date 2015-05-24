/**
 * @file BlobDetectionUtils.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description Handles all BeeTracker blob-related operations.
 */

package beetracker;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import blobDetection.Blob;
import blobDetection.BlobDetection;
import blobDetection.EdgeVertex;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.data.IntList;

public class BlobDetectionUtils {
    private int hueThreshold = 10, satThreshold = 50, lumThreshold = 30;
    private static final int filterRadius = 3;
    private final BlobDetection bd;
    private IntList blobColors;

    private final boolean debug;

    /**
     * Class constructor.
     * @param width the width of the images to process
     * @param height the height of the images to process
     * @param debug whether debug mode is enabled
     */
    public BlobDetectionUtils(int width, int height, boolean debug) {
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
     *   hue and saturation thresholds have the saturation and luminosity values
     *   maxed out, while all other pixels are set to 0x000000.
     * @param parent the calling PApplet
     * @param img the PImage to preprocess
     * @param colors a list of the integer RGB values to scan for
     */
    public void filterImg(PApplet parent, PImage img, IntList colors) {
        float pixelHue, listHue;
        int i, j;

        img.loadPixels();

        parent.colorMode(PConstants.HSB, 255);

        //scan every pixel in image
        for(i = 0; i < img.pixels.length; i++) {
            pixelHue = parent.hue(img.pixels[i]);

            //for color matches, brighten pixel
            for(j = 0; j < colors.size(); j++) {
                listHue = parent.hue(colors.get(j));

                if(pixelHue > listHue - hueThreshold &&
                    pixelHue < listHue + hueThreshold &&
                    parent.saturation(img.pixels[i]) > satThreshold &&
                    parent.brightness(img.pixels[i]) > lumThreshold)
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

        img.updatePixels();
    }

    /**
     * Draws the blobs in the current frame.
     * @param parent the calling PApplet
     * @param frameDims the dimensions of the image frame for which blob
     *   detection is being performed, in pixels
     * @param offset the xy coordinates of the image frame origin, in pixels
     * @param exitXY the xy coordinates of the exit center, in pixels
     */
    public void drawBlobs(PApplet parent, int[] frameDims, int[] offset,
        float[] exitXY)
    {
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
             /*   
                //line to exit center
                parent.strokeWeight(2);
                parent.stroke(255, 0, 255);
                parent.line(
                    b.x*frameDims[0]+offset[0],
                    b.y*frameDims[1]+offset[1],
                    exitXY[0],
                    exitXY[1]
                );*/
            }
        }
    }

    /**
     * Retrieves the centroids of the blobs in the current frame.
     * @param parent the calling PApplet
     * @param frame the filtered frame
     * @param colors the list of color values
     * @return a HashMap mapping RGB integer values to Lists of normalized xy
     *   coordinates of the detected blob centroids
     */
    public HashMap<Integer, List<float[]>> getCentroids(
        PApplet parent,
        PImage frame,
        IntList colors
    ) {
        HashMap<Integer, List<float[]>> result = new HashMap<>();
        float[] point;
        Blob b;
        int i, j, color, pixel, hue;
        boolean added;

        //remove noise
        openImage(parent, frame, colors);

        frame.loadPixels();
        bd.computeBlobs(frame.pixels);

        if(debug) {
            blobColors.clear();

            PApplet.println("blobs: " + bd.getBlobNb());
        }

        for(i = 0; i < colors.size(); i++) {
            result.put(Integer.valueOf(colors.get(i)), new LinkedList<float[]>());
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
                                break;
                            }
                        }

                        if(added) {
                            break;
                        }
                    }
                }
            }

            if(debug && added) {
                PApplet.println(String.format("%f, %f", point[0], point[1]));
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
     * @param threshold the new threshold value
     */
    public void setThreshold(int threshold) {
//        satThreshold = 255 - threshold;
     //   lumThreshold = 255 - threshold;
    //    hueThreshold = lumThreshold/2;
    }

    /**
     * Performs a morphological opening operation to remove noise. Any blob not
     *   meeting the radius threshold will be erased.
     * @param parent the calling PApplet
     * @param img the PImage to preprocess
     * @param colors a list of the integer RGB values to scan for
     */
    private void openImage(PApplet parent, PImage img, IntList colors) {
        img.loadPixels();

        int[] tmp = new int[img.pixels.length];
        int i, j, k, l, m, offset;
        boolean hit = false;

        for(i = 0; i < tmp.length; i++) {
            tmp[i] = 0;
        }

        //erode filtered image
        for(i = filterRadius; i < img.width - filterRadius; i++) {
            for(j = filterRadius; j < img.height - filterRadius; j++) {
                for(m = 0; m < colors.size(); m++) {
                    erodeLoop:
                    for(k = 0 - filterRadius; k <= filterRadius; k++) {
                        for(l = 0 - filterRadius; l <= filterRadius; l++) {
                            if(Math.abs(k) + Math.abs(l) <= filterRadius) {
                                offset = (i+k) + (j+l)*img.width;
                                hit = (int)parent.hue(img.pixels[offset]) ==
                                    (int)parent.hue(colors.get(m));

                                if(!hit) {
                                    break erodeLoop;
                                }
                            }
                        }
                    }

                    if(hit) {
                        offset = i + j*img.width;
                        tmp[offset] = img.pixels[offset];
                    }
                }
            }
        }

        //dilate eroded image
        int n, colorVal = 0;
        for(i = 0; i < img.width; i++) {
            for(j = 0; j < img.height; j++) {
                dilateLoop:
                for(k = 0 - filterRadius; k <= filterRadius; k++) {
                    for(l = 0 - filterRadius; l <= filterRadius; l++) {
                        m = i + k;
                        n = j + l;

                        if(
                            m >= 0 && m < img.width &&
                            n >= 0 && n < img.height &&
                            Math.abs(k) + Math.abs(l) <= filterRadius
                        ) {
                            offset = m + n*img.width;
                            hit = parent.brightness(tmp[offset]) > 0;

                            if(hit) {
                                colorVal = tmp[offset];

                                break dilateLoop;
                            }
                        }
                    }
                }

                if(hit) {
                    img.pixels[i + j*img.width] = colorVal;
                }
            }
        }

        img.updatePixels();
    }
}
