/*
* BeeTracker
* Copyright (C) 2015 Kay Choi
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package beetracker;

import $N.PointR;

import java.util.LinkedList;

/**
 * @class ShapeRecognizer
 * @author Kay Choi
 * @date 13 Nov 15
 * @description Provides shape recognition for waggle dance detection.
 */
public class ShapeRecognizer {
    $N.NDollarRecognizer nDollar;
//    private static final float shapeScore = 0.75f;
    private static final int shapeScore = 60;
    private boolean status = false;
    private static final int rate = 32;

    de.voidplus.dollar.OneDollar oneDollar;

    /**
     * Class constructor.
     * @param root the BeeTracker object
     */
    ShapeRecognizer(BeeTracker root) {
/*        //$N
        nDollar = new $N.NDollarRecognizer();
        nDollar.LoadGesture(root.createInputRaw("paths/waggle.xml"));
        nDollar.LoadGesture(root.createInputRaw("paths/waggle-vert.xml"));
/*/
        //$1
        oneDollar = new de.voidplus.dollar.OneDollar(root)
            .setMinSimilarity(shapeScore)
            .enableMinSimilarity()
            .setVerbose(BeeTracker.debug)
            .setFragmentationRate(rate);
        readOneDollarTemplate(root, "waggle");
  //      readOneDollarTemplate(root, "waggle-vert");
        oneDollar.bind("waggle", this, "oneDollarCallback");
/*/
//        oneDollar.setMinSimilarity(30);
        readOneDollarTemplate(root, "circle");
        oneDollar.on("circle", this, "oneDollarCallback");//*/
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
        java.util.ListIterator<int[]> iter = path.listIterator(path.size());
        for(int i = 0; i < array.length; i += 2) {
            point = iter.previous();
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
        java.util.ListIterator<float[]> iter = path.listIterator(path.size());
        float[] point, prevCandPoint = null;
        float dx, dy;
        int i;

        //$N
//        java.util.Vector<PointR> candidateVec = new java.util.Vector<>();

        //$1
        LinkedList<float[]> candidateList = new LinkedList<>();
        int[] candidateArray;

        status = false;

        //check path in reverse
        while(iter.hasPrevious()) {
            point = iter.previous();

            //artificially increase candidate sample rate
            if(prevCandPoint != null) {
                dx = (prevCandPoint[0]-point[0])/rate;
                dy = (prevCandPoint[1]-point[1])/rate;

                for(i = 1; i < rate; i++) {
                    //$N
//                    candidateVec.add(new PointR((point[0]+i*dx)*frameDims[0],
//                        (point[1]+i*dy)*frameDims[1]));

                    //$1
                    candidateList.add(new float[] {(point[0]+i*dx)*frameDims[0],
                        (point[1]+i*dy)*frameDims[1]});
                }
            }
/*
            //$N
            candidateVec.add(new PointR(point[0]*frameDims[0], point[1]*frameDims[1]));
            if(candidateVec.size() > 1) {
                if(nDollar.Recognize(candidateVec, 1).getScore() >= shapeScore) {
                    status = true;
                }
            }
/*/
            //$1
            candidateList.add(new float[] {point[0]*frameDims[0],
                point[1]*frameDims[1]});
            candidateArray = new int[candidateList.size()*2];
            i = 0;
            for(float[] tmpPoint : candidateList) {
                candidateArray[i] = (int)tmpPoint[0];
                i++;
                candidateArray[i] = (int)tmpPoint[1];
                i++;
            }
            oneDollar.check(candidateArray);
//*/
            //current path contains recognized gesture, no need to continue
            if(status) {
                break;
            }

            prevCandPoint = point;
        }
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
    public void oneDollarCallback(
        String gestureName,
        float percentOfSimilarity,
        int startX,
        int startY,
        int centroidX,
        int centroidY,
        int endX,
        int endY
    ) {
        status = true;
    }
}
