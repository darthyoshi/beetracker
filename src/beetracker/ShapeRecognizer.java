/**
 * @file ShapeRecognizer.java
 * @author Kay Choi, 909926828
 * @date 21 Oct 15
 * @description Provides shape recognition for waggle dance detection.
 */

package beetracker;

import $N.NDollarRecognizer;
import $N.PointR;

import de.voidplus.dollar.OneDollar;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 *
 * @author Kay Choi
 */
class ShapeRecognizer {
    NDollarRecognizer nDollar;
    private static final float shapeScore = 0.8f;
    private boolean status = false;

    OneDollar oneDollar;

    /**
     * Class constructor.
     * @param root the BeeTracker object
     */
    ShapeRecognizer(BeeTracker root) {
/*        nDollar = new NDollarRecognizer();
        nDollar.LoadGesture(root.createInputRaw("paths/waggle.xml"));
        nDollar.LoadGesture(root.createInputRaw("paths/waggle-vert.xml"));
*/
        oneDollar = new OneDollar(root).setMinSimilarity((int)(shapeScore*100f))
            .enableMinSimilarity();
        readOneDollarTemplate(root, "waggle");
        readOneDollarTemplate(root, "waggle-vert");
        oneDollar.bind("waggle waggle-vert", this, "oneDollarCallback");
    }

    /**
     * Reads a gesture template into the $1 object.
     * @param root the root BeeTracker
     * @param fileName the template file name
     */
    private void readOneDollarTemplate(BeeTracker root, String fileName) {
        LinkedList<int[]> path = new LinkedList<>();
        int[] point;
        java.io.BufferedReader reader = null;
        String line;
        String[] split;
        try {
            reader = new java.io.BufferedReader(new java.io
                .InputStreamReader(root.createInputRaw("paths/"+fileName+".xml"), "UTF-8"));
            while((line = reader.readLine()) != null) {
                if(line.startsWith("<Point")) {
                    split = line.split("\\\"");
                    point = new int[] {(int)Float.parseFloat(split[1]),
                        (int)Float.parseFloat(split[3])};
                    path.add(point);
                }
            }
        } catch (java.io.IOException ex) {
            ex.printStackTrace(System.err);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (java.io.IOException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }
        int[] array = new int[path.size()*2];
        ListIterator<int[]> intIter = path.listIterator(path.size());
        for(int i = 0; i < array.length; i += 2) {
            point = intIter.previous();
            array[i] = point[0];
            array[i+1] = point[1];
        }
        oneDollar.learn(fileName, array);
    }

    /**
     * Checks a path for the waggle dance.
     * @param path a List of normalized float pairs representing a path
     * @param frameDims the dimensions of the inset frame
     * @return true if the waggle dance shape is part of the path
     */
    void recognize(java.util.List<float[]> path, int[] frameDims) {
        ListIterator<float[]> iter = path.listIterator(path.size());
        float[] point;

        //$N
        java.util.Vector<PointR> list = new java.util.Vector<>();

        //$1
        LinkedList<Integer> candidate = new LinkedList<>();
        int[] array;
        int i;

        status = false;

        //check path in reverse
        while(iter.hasPrevious()) {
            point = iter.previous();
/*
            //$N
            list.add(new PointR(point[0]*frameDims[0], point[1]*frameDims[1]));
            if(nDollar.Recognize(list, 1).getScore() >= shapeScore) {
                status = true;
            }
*/
            //$1
            candidate.add((int)(point[0]*frameDims[0]));
            candidate.add((int)(point[1]*frameDims[1]));
            array = new int[candidate.size()];
            i = 0;
            for(Integer c : candidate) {
                array[i] = c;
                i++;
            }
            oneDollar.check(array);

            //current path contains recognized gesture, no need to continue
            if(status) {
                break;
            }
        }
    }

    /**
     * Callback method for storing candidate recognition state.
     * @param state true it the current gesture candidate matches a template
     */
    void setCandidateRecognized(boolean state) {
        status = state;
    }

    /**
     * @return true if the current gesture candidate matches a template
     */
    boolean isCandidateRecognized() {
        return status; 
    }

    /**
     * $1 recognizer callback method.
     * @param gestureName
     * @param percentOfSimilarity
     * @param startX
     * @param startY
     * @param centroidX
     * @param centroidY
     * @param endX
     * @param endY
     */
    void oneDollarCallback(
        String gestureName,
        float percentOfSimilarity,
        int startX,
        int startY,
        int centroidX,
        int centroidY,
        int endX,
        int endY
    ) {
        //System.err.println("Need to implement - " + percentOfSimilarity);
        status = true;
    }
}
