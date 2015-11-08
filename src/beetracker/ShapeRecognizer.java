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
        oneDollar = new OneDollar(root);
        readOneDollar(root, "waggle");
        readOneDollar(root, "waggle-vert");
        oneDollar.bind("waggle waggle-vert", "oneDollar");
    }

    /**
     * Reads a gesture template into the $1 object.
     * @param root the root BeeTracker
     * @param fileName the template file name
     */
    private void readOneDollar(BeeTracker root, String fileName) {
        LinkedList<Integer> path = new LinkedList<>();
        java.io.BufferedReader reader = null;
        String line;
        String[] split;
        try {
            reader = new java.io.BufferedReader(new java.io
                .InputStreamReader(root.createInputRaw("paths/"+fileName+".xml"), "UTF-8"));
            while((line = reader.readLine()) != null) {
                if(line.startsWith("<Point")) {
                    split = line.split("\\\"");
                    path.add((int)Float.parseFloat(split[1]));
                    path.add((int)Float.parseFloat(split[3]));
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
        int[] array = new int[path.size()];
        ListIterator<Integer> intIter = path.listIterator(path.size());
        for(int i = 0; i < path.size(); i++) {
            array[i] = intIter.previous();
        }
        oneDollar.learn(fileName, array);
    }

    /**
     * Checks a path for the waggle dance.
     * @param path a List of float pairs representing a path
     * @return true if the waggle dance shape is part of the path
     */
    boolean recognize(java.util.List<float[]> path) {
        boolean result = false;

        java.util.Vector<PointR> list = new java.util.Vector<>();
        ListIterator<float[]> iter = path.listIterator(path.size());
        float[] point;

        //check path in reverse
        while(iter.hasPrevious()) {
            point = iter.previous();
/*
            //$N
            list.add(new PointR(point[0], point[1]));
            if(nDollar.Recognize(list, 1).getScore() >= shapeScore) {
                result = true;
                break;
            }
*/
            //$1
            oneDollar.track(point[0], point[1]);
        }

        return result;
    }
}
