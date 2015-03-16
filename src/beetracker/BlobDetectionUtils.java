/**
 * @file BlobDetectionUtils.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description Handles all BeeTracker blob-related operations.
 */

package beetracker;

import java.util.LinkedList;

import blobDetection.Blob;
import blobDetection.BlobDetection;
import blobDetection.EdgeVertex;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.data.IntList;

public class BlobDetectionUtils {
    final static int hueThreshold = 10, satThreshold = 100, lumThreshold = 10;
    private static final float noise = 0.001f;
    private final BlobDetection bd;

    /**
     * Class constructor.
     * @param width the width of the images to process
     * @param height the height of the images to process
     */
    public BlobDetectionUtils(int width, int height) {
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
     */
    public static void preProcessImg(PApplet parent, PImage img,
        IntList colors)
    {
        boolean match;
        float tmp, hue;

        img.loadPixels();

        parent.colorMode(PConstants.HSB, 255);
        for(int i = 0; i <img.pixels.length; i++) {
            match = false;
            tmp = parent.hue(img.pixels[i]);

            for(int color : colors) {
                hue = parent.hue(color);

                if(tmp > hue - hueThreshold &&
                    tmp < hue + hueThreshold &&
                    parent.saturation(img.pixels[i]) > satThreshold &&
                    parent.brightness(img.pixels[i]) > lumThreshold)
                {
                    img.pixels[i] = parent.color(hue, 255, 255);

                    match = true;

                    break;
                }
            }

            if(!match) {
                img.pixels[i] = parent.color(0xFF000000);
            }
        }

        img.updatePixels();
    }

    /**
     * Computes the blobs in an image.
     * @param img the PImage to process
     */
    public void computeBlobs(PImage img) {
        img.loadPixels();
        bd.computeBlobs(img.pixels);
    }

    /**
     * Draws the currently detected blobs. Blobs with a bounding box of less
     *   than 1% of the total image area are ignored as noise.
     * @param parent the calling PApplet
     * @param frameDims the dimensions of the image frame for which blob
     *   detection is being performed, in pixels
     * @param offset the xy coordinates of the image frame, in pixels
     */
    public void drawBlobs(PApplet parent, int[] frameDims, int[] offset) {
        parent.noFill();
        Blob b;
        EdgeVertex eA,eB;

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
     * Retrieves the centroids of the currently detected blobs. Blobs with a
     *   bounding box of less than 1% of the total image area are ignored as
     *   noise.
     * @return an LinkedList containing the normalized xy coordinates of the
     *  detected blob centroids
     */
    public LinkedList<float[]> getCentroids() {
        LinkedList<float[]> result = new LinkedList<float[]>();
        float[] tmp;
        Blob b;

        for(int i = 0; i < bd.getBlobNb(); i++) {
            b = bd.getBlob(i);

            if((b.xMax-b.xMin)*(b.yMax-b.yMin) > noise) {
                tmp = new float[2];
                tmp[0] = b.x;
                tmp[1] = b.y;
                result.add(tmp);
            }
        }

        return result;
    }
}
