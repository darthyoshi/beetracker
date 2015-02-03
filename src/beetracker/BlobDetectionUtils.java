/**
 * @file BlobDetectionUtils.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description Handles all blob-related operations.
 */

package beetracker;

import java.util.ArrayList;

import blobDetection.Blob;
import blobDetection.BlobDetection;
import blobDetection.EdgeVertex;
import processing.core.PApplet;
import processing.core.PImage;

public class BlobDetectionUtils {
    final static int hueThreshold = 10, satThreshold = 100;
    private static final float noise = 0.01f;
    private int area;
    private BlobDetection bd;
    
    /**
     * 
     * @param width
     * @param height
     */
    public BlobDetectionUtils(int width, int height) {
        bd = new BlobDetection(width, height);
        bd.setPosDiscrimination(true);
        bd.setThreshold(.2f);
        area = width * height;
    }

    /**
     * Preprocesses a PImage for blob detection. The color values for any pixels
     *   meeting the defined hue and saturation thresholds are set to 0xFFFFFF,
     *   while all other pixels are set to 0x000000.
     * @param parent the calling PApplet
     * @param img the PImage to preprocess
     * @param hues
     */
    public static void preProcessImg(PApplet parent, PImage img, int[] hues) {
        short j;
        boolean match;

        img.loadPixels();

        parent.colorMode(parent.HSB, 255);
        for(int i = 0; i < img.pixels.length; i++) {
            match = false;

            for(j = 0; j < (short)hues.length; j++) {
                if(parent.hue(img.pixels[i]) > hues[j] - hueThreshold &&
                    parent.hue(img.pixels[i]) < hues[j] + hueThreshold &&
                    parent.saturation(img.pixels[i]) > satThreshold)
                {
                    img.pixels[i] = parent.color(
                        parent.hue(img.pixels[i]),
                        255,
                        255
                    );

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
     * @param pixels an array containing the current color values of an image.
     */
    public void computeBlobs(int[] pixels) {
        bd.computeBlobs(pixels);
    }
    
    /**
     * Draws the edges of the currently detected blobs. Blobs with a bounding
     * box of less than 1% of the total image area are ignored as noise.
     * @param parent the calling PApplet
     */
    public void drawEdges(PApplet parent) {
        parent.noFill();
        Blob b;
        EdgeVertex eA,eB;
        for (int n = 0; n < bd.getBlobNb(); n++) {
            b = bd.getBlob(n);
            if (b != null) {
                if((float)(b.xMax-b.xMin)*(b.yMax-b.yMin)/area > noise) {
                    // Edges
                    parent.strokeWeight(3);
                    parent.stroke(0xFFFF00AA);
                    for (int m = 0; m < b.getEdgeNb(); m++) {
                        eA = b.getEdgeVertexA(m);
                        eB = b.getEdgeVertexB(m);

                        if (eA !=null && eB !=null) {
                            parent.line(
                                eA.x*parent.width, eA.y*parent.height,
                                eB.x*parent.width, eB.y*parent.height
                            );
                        }
                    }
                    
                    //bounding boxes
                    parent.strokeWeight(1);
                    parent.stroke(0xFF00FFAA);
                    parent.rectMode(parent.CORNER);
                    parent.rect(
                        b.xMin*parent.width,
                        b.yMin*parent.height,
                        b.w*parent.width,
                        b.h*parent.height
                    );
                }
            }
        }
    }

    /**
     * Retrieves the centroids of the currently detected blobs. Blobs with a
     * bounding box of less than 1% of the total image area are ignored as
     * noise.
     * @return an ArrayList containing the xy coordinates of the detected blob
     *   centroids
     */
    public ArrayList<int[]> getCentroids() {
        ArrayList<int[]> result = new ArrayList<int[]>();
        int[] tmp;
        Blob b;

        for(int i = 0; i < bd.getBlobNb(); i++) {
            b = bd.getBlob(i);

            if((float)(b.xMax-b.xMin)*(b.yMax-b.yMin)/area > noise) {
                tmp = new int[2];
                tmp[0] = b.x;
                tmp[1] = b.y;
                result.add(tmp);
            }
        }

        return result;
    }
}
