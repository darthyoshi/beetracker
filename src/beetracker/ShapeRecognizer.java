/**
 * @file ShapeRecognizer.java
 * @author Kay Choi, 909926828
 * @date 21 Oct 15
 * @description Provides shape recognition for waggle dance detection.
 */

package beetracker;

import $N.*;

import java.util.LinkedList;
import java.util.Vector;

/**
 *
 * @author Kay Choi
 */
class ShapeRecognizer {
    NDollarRecognizer rec;
    private final boolean debug;
    private static final float shapeScore = 0.8f;

    /**
     * Class constructor.
     */
    ShapeRecognizer(BeeTracker parent, boolean debug) {
        this.debug = debug;
        
        rec = new NDollarRecognizer();

        rec.LoadGesture(parent.createInputRaw("paths/waggle.xml"));
        rec.LoadGesture(parent.createInputRaw("paths/waggle-vert.xml"));
    }
    
    /**
     * Checks a path for the waggle dance.
     * @param path a List of float pairs representing a path
     * @return true if the waggle dance shape is part of the path
     */
    boolean recognize(java.util.List<float[]> path) {
        boolean result = false;

        LinkedList<PointR> list = new LinkedList<>();

        for(float[] point : path) {
            list.add(new PointR(point[0], point[1]));
        }

        //check path
        while(!list.isEmpty()) {
            //TODO overload NDollarRecognizer.Recognize() to accept List instead of only Vector?
            if(rec.Recognize(new Vector<PointR>(list), 1).getScore() >= shapeScore) {
                result = true;
                break;
            }

            //discard path head
            list.pop();
        }

        return result;
    }
}
