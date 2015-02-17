/**
 * @file ColorPicker.java
 * @author Kay Choi, 909926828
 * @date 15 Feb 15
 * @description
 */

package beetracker;

import java.awt.Color;
import javax.swing.JColorChooser;
import processing.core.PApplet;


public class ColorPicker {
    /**
     * TODO add method header
     * @param parent the instantiating PApplet
     * @return
     */
    public static int getColor(PApplet parent) {
        Color newColor = JColorChooser.showDialog(null, "Select Color", Color.BLACK);

        return (newColor != null ? newColor.getRGB() : 0);
    }
}
