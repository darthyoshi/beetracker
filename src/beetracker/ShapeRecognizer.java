package beetracker;

import $N.*;

import java.util.List;
import java.util.Vector;

class ShapeRecognizer {
    NDollarRecognizer rec;
    private final boolean debug;

    ShapeRecognizer(BeeTracker parent, boolean debug) {
        this.debug = debug;
        
        rec = new NDollarRecognizer();

        rec.LoadGesture(parent.createInputRaw("paths/waggle.xml"));
        rec.LoadGesture(parent.createInputRaw("paths/waggle-vert.xml"));
    }
    
    void recognize(List<float[]> path) {
        Vector<PointR> vec = new Vector<>();
        
        for(float[] point : path) {
            vec.add(new PointR(point[0], point[1]));
        }
        
        NBestList score = rec.Recognize(vec, 1);
        
        if(score.getScore() >= 0.75) {
            
        }
    }
}
