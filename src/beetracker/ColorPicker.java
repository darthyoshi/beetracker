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

/**
 * @class ColorPicker
 * @author Kay Choi
 * @date 13 Nov 15
 * @description Uses a Java Swing color picker to select colors for BeeTracker
 *   object detection.
 */
class ColorPicker {
    /**
     * Displays a color picker.
     * @param parent the instantiating BeeTracker
     */
    static void getColor(final BeeTracker parent) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                java.awt.Color newColor = javax.swing.JColorChooser.showDialog(
                    null/*((PSurfaceAWT.SmoothCanvas)parent.getSurface().getNative()).getFrame()*/,
                    "Select Color",
                    java.awt.Color.BLACK
                );
                Integer result;

                if(newColor != null) {
                    result = (newColor.getRed() << 16) +
                        (newColor.getGreen() << 8) + newColor.getBlue();
                }

                else {
                    result = null;
                }

                parent.setColor(result);
            }
        });
    }
}
