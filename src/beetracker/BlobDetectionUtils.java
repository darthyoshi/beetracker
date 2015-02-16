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
import processing.core.PConstants;
import processing.core.PImage;

public class BlobDetectionUtils {
    final static int hueThreshold = 10, satThreshold = 100;
    private static final float noise = 0.0001f;
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
     * Preprocesses a PImage for blob detection. Any pixels meeting the defined
     *   hue and saturation thresholds have the color values maxed out, while
     *   all other pixels are set to 0x000000.
     * @param parent the calling PApplet
     * @param img the PImage to preprocess
     * @param colors
     */
    public static void preProcessImg(
        PApplet parent,
        PImage img,
        ArrayList<Float> colors
    ) {
        short j;
        boolean match;
        float tmp, hue;

        img.loadPixels();

        parent.colorMode(PConstants.HSB, 255);
        for(int i = 0; i < img.pixels.length; i++) {
            match = false;
            tmp = parent.hue(img.pixels[i]);

            for(j = 0; j < (short)colors.size(); j++) {
                hue = colors.get(j);

                if(tmp > hue - hueThreshold &&
                    tmp < hue + hueThreshold &&
                    parent.saturation(img.pixels[i]) > satThreshold)
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
     * @param pixels an array containing the current color values of an image.
     */
    public void computeBlobs(int[] pixels) {
        bd.computeBlobs(pixels);
    }

    /**
     * Draws the edges of the currently detected blobs. Blobs with a bounding
     * box of less than 1% of the total image area are ignored as noise.
     * @param parent the calling PApplet
     * @param pip
     * @param box
     */
    public void drawEdges(PApplet parent, boolean pip, float[] box) {
        parent.noFill();
        Blob b;
        EdgeVertex eA,eB;
        int[] offset = new int[2];
        int[] dims = new int[2];

        if(pip) {
            offset[0] = offset[1] = 50;
            dims[0] = parent.width-100;
            dims[1] = parent.height-100;
        }

        else {
            offset[0] = 50 + (int)(box[0]*(parent.width-100));
            offset[1] = 50 + (int)(box[1]*(parent.height-100));
            dims[0] = (int)(Math.abs(box[2]-box[0])*(parent.width-100));
            dims[1] = (int)(Math.abs(box[3]-box[1])*(parent.height-100));
        }

        for (int n = 0; n < bd.getBlobNb(); n++) {
            b = bd.getBlob(n);
            if (b != null) {
                if((b.xMax-b.xMin)*(b.yMax-b.yMin) >= noise) {
                    // Edges
                    parent.strokeWeight(2);
                    parent.stroke(0xFFFF00AA);
                    for (int m = 0; m < b.getEdgeNb(); m++) {
                        eA = b.getEdgeVertexA(m);
                        eB = b.getEdgeVertexB(m);

                        if (eA !=null && eB !=null) {
                            parent.line(
                                eA.x*dims[0] + offset[0],
                                eA.y*dims[1] + offset[1],
                                eB.x*dims[0] + offset[0],
                                eB.y*dims[1] + offset[1]
                            );
                        }
                    }

                    //bounding boxes
                    parent.strokeWeight(1);
                    parent.stroke(0xFF00FFAA);
                    parent.rectMode(PConstants.CORNER);
                    parent.rect(
                        b.xMin*dims[0] + offset[0],
                        b.yMin*dims[1] + offset[1],
                        b.w*dims[0],
                        b.h*dims[1]
                    );
                }
            }
        }
    }

    /**
     * Retrieves the centroids of the currently detected blobs. Blobs with a
     * bounding box of less than 1% of the total image area are ignored as
     * noise.
     * @return an ArrayList containing the normalized xy coordinates of the
     *  detected blob centroids
     */
    public ArrayList<float[]> getCentroids() {
        ArrayList<float[]> result = new ArrayList<float[]>();
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
