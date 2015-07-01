/**
 * @file ColorPicker.java
 * @author Kay Choi, 909926828
 * @date 15 Feb 15
 * @description Uses a Java Swing color picker to select colors for BeeTracker
 *   object detection.
 */

package beetracker;

/**
 *
 * @author Kay Choi
 */
public class ColorPicker {
    /**
     * Displays a color picker.
     * @param parent the instantiating BeeTracker
     */
    public static void getColor(BeeTracker parent) {
        java.awt.EventQueue.invokeLater(() -> {
            java.awt.Color newColor = javax.swing.JColorChooser
                .showDialog(parent, "Select Color", java.awt.Color.BLACK);
            int result = 0;

            if(newColor != null) {
                result = (newColor.getRed() << 16) +
                    (newColor.getGreen() << 8) + newColor.getBlue();
            }

            parent.setColor(result);
        });
    }
}
