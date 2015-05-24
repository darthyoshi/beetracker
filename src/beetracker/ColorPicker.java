/**
 * @file ColorPicker.java
 * @author Kay Choi, 909926828
 * @date 15 Feb 15
 * @description Uses a Java Swing color picker to select colors for BeeTracker
 *   object detection.
 */

package beetracker;

import java.awt.Color;
import javax.swing.JColorChooser;
import processing.core.PApplet;

public class ColorPicker {
    /**
     * Displays a color picker.
     * @param parent the instantiating PApplet
     * @return the six-digit hexadecimal RGB value of the selected color or 0
     *   for a non-valid selection
     */
    public static int getColor(PApplet parent) {
        Color newColor = JColorChooser.showDialog(parent, "Select Color", Color.BLACK);
        int result = 0;

        if(newColor != null) {
            result = (newColor.getRed() << 16) + (newColor.getGreen() << 8) + newColor.getBlue();
        }

        return result;
    }
}
