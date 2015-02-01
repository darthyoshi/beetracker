/**
 * @file BlobDetectionUtils.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description Handles all blob-related operations.
 */

package beetracker;

import blobDetection.Blob;
import blobDetection.BlobDetection;
import blobDetection.EdgeVertex;
import processing.core.PApplet;
import processing.core.PImage;

public class BlobDetectionUtils {
    final static int hueThreshold = 10, satThreshold = 100;
    
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
     * Draws the edges of the currently detected blobs.
     * @param parent the calling PApplet
     */
    public void drawEdges(PApplet parent) {
        parent.noFill();
        Blob b;
        EdgeVertex eA,eB;
        for (int n = 0; n < bd.getBlobNb(); n++) {
            b = bd.getBlob(n);
            if (b != null) {
                // Edges
                parent.strokeWeight(3);
                parent.stroke(0xFFFF00AA);
                for (int m = 0; m < b.getEdgeNb(); m++) {
                    eA = b.getEdgeVertexA(m);
                    eB = b.getEdgeVertexB(m);

                    if (eA !=null && eB !=null) {
                        parent.line(
                            eA.x*width, eA.y*height,
                            eB.x*width, eB.y*height
                        );
                    }
                }
                
                //bounding boxes
                parent.strokeWeight(1);
                parent.stroke(0xFF00FFAA);
                parent.rectMode(parent.CORNER);
                parent.rect(
                    b.xMin*width,
                    b.yMin*height,
                    b.w*width,
                    b.h*height
                );
            }
        }
    }
}
