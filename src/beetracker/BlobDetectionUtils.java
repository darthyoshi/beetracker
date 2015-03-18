/**
 * @file BlobDetectionUtils.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description Handles all BeeTracker blob-related operations.
 */

package beetracker;

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
    final static int hueThreshold = 10, satThreshold = 100, lumThreshold = 50;
    private static final float noise = 0.00005f;
    private final BlobDetection bd;

    private final boolean debug;

    /**
     * Class constructor.
     * @param width the width of the images to process
     * @param height the height of the images to process
     * @param debug whether debug mode is enabled
     */
    public BlobDetectionUtils(int width, int height, boolean debug) {
        this.debug = debug;

        bd = new BlobDetection(width, height);
        bd.setPosDiscrimination(true);
        bd.setThreshold(.2f);
    }

    /**
     * Preprocesses a PImage for blob detection. Any pixels meeting the defined
     *   hue and saturation thresholds have the color values maxed out, while
     *   all other pixels are set to 0x000000.
     * @param parent the calling PApplet
     * @param img the PImage to preprocess
     * @param colors a list of the integer RGB values to scan for
     * @return an array of filtered frames, one for each color
     */
    public static PImage[] filterImg(PApplet parent, PImage img,
        IntList colors)
    {
        PImage[] result = new PImage[colors.size()];
        boolean[] pixelMatch = new boolean[result.length];
        float tmp, hue;
        int i, j, k;

        img.loadPixels();

        for(i = 0; i < result.length; i++) {
            result[i] = parent.createImage(img.width, img.height, PConstants.HSB);
            result[i].loadPixels();
        }

        parent.colorMode(PConstants.HSB, 255);

        for(i = 0; i < img.pixels.length; i++) {
            tmp = parent.hue(img.pixels[i]);

            for(j = 0; j < pixelMatch.length; j++) {
                pixelMatch[j] = false;
            }

            for(j = 0; j < result.length; j++) {
                hue = parent.hue(colors.get(j));

                if(tmp > hue - hueThreshold &&
                    tmp < hue + hueThreshold &&
                    parent.saturation(img.pixels[i]) > satThreshold &&
                    parent.brightness(img.pixels[i]) > lumThreshold)
                {
                    result[j].pixels[i] = img.pixels[i]
                        = parent.color(hue, 255, 255);

                    pixelMatch[j] = true;
                }
            }

            for(j = 0, k = 0; j < pixelMatch.length; j++) {
                if(!pixelMatch[j]) {
                    result[j].pixels[i] = parent.color(0);

                    k++;
                }
            }
            
            if(k == pixelMatch.length) {
                img.pixels[i] = parent.color(0);
            }
        }

        img.updatePixels();

        for(j = 0; j < result.length; j++) {
            result[j].updatePixels();
        }

        return result;
    }

    /**
     * Draws the blobs in the current frame. Blobs with a bounding box of less
     *   than 0.005% of the total image area are ignored as noise.
     * @param parent the calling PApplet
     * @param img a frame containing the blobs to draw
     * @param frameDims the dimensions of the image frame for which blob
     *   detection is being performed, in pixels
     * @param offset the xy coordinates of the image frame, in pixels
     */
    public void drawBlobs(PApplet parent, PImage img, int[] frameDims,
        int[] offset)
    {
        EdgeVertex eA,eB;
        Blob b;

        img.loadPixels();
        bd.computeBlobs(img.pixels);

        parent.noFill();
        parent.strokeWeight(1);

        for (int n = 0; n < bd.getBlobNb(); n++) {
            b = bd.getBlob(n);
            if (b != null) {
                if((b.xMax-b.xMin)*(b.yMax-b.yMin) >= noise) {
                    // Edges
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
/*
                    //bounding boxes
                    parent.stroke(0xFF00FFAA);
                    parent.rectMode(PConstants.CORNER);
                    parent.rect(
                        b.xMin*frameDims[0] + offset[0],
                        b.yMin*frameDims[1] + offset[1],
                        b.w*frameDims[0],
                        b.h*frameDims[1]
                    );*/
                }
            }
        }
    }

    /**
     * Retrieves the centroids of the blobs in the current frame. Blobs with a
     *   bounding box of less than 0.005% of the total frame area are ignored as
     *   noise.
     * @param imgs the filtered versions of the frame
     * @return a LinkedList containing Lists of normalized xy coordinates of the
     *   detected blob centroids, where each list is associated with a specific
     *   color
     */
    public LinkedList<List<float[]>> getCentroids(PImage[] imgs) {
        LinkedList<List<float[]>> result = new LinkedList<>();
        List<float[]> tmp;
        float[] point;
        Blob b;

        if(debug) {
            PApplet.println("blobs:");
        }

        for(PImage img : imgs) {
            img.loadPixels();
            bd.computeBlobs(img.pixels);

            tmp = new LinkedList<>();

            for(int i = 0; i < bd.getBlobNb(); i++) {
                b = bd.getBlob(i);
                if((b.xMax-b.xMin)*(b.yMax-b.yMin) > noise) {
                    point = new float[2];
                    point[0] = b.x;
                    point[1] = b.y;
                    tmp.add(point);

                    if(debug) {
                        PApplet.println(String.format("%f, %f", point[0], point[1]));
                    }
                }
            }

            result.add(tmp);
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
     * 
     * @return the height of the images to process
     */
    public int getImageHeight() {
        return bd.imgHeight;
    }
}
