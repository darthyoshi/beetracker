/**
 * @file MessageDialogue.java
 * @author Kay Choi, 909926828
 * @date 11 Mar 15
 * @description Displays Java Swing message dialogues.
 */

package beetracker;

import javax.swing.JOptionPane;

import processing.core.PApplet;

public class MessageDialogue {
    private static final String errorMsg[] = {
        "No colors have been selected.\n",
        "No hive exit has been defined.\n"
    };

    /**
     * Displays a warning message if setup parameters have not been set.
     * @param parent the instantiating PApplet
     * @param errors the setup error flags
     */
    public static void playButtonError(PApplet parent, boolean[] errors) {
        StringBuilder msg = new StringBuilder();

        if(errors[0]) {
            msg.append(errorMsg[0]);
        }

        if(errors[1]) {
            msg.append(errorMsg[1]);
        }

        JOptionPane.showMessageDialog(parent, msg.toString(), "Error",
            JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays a dialogue to confirm whether or not to prematurely end playback. 
     * @param parent the instantiating PApplet
     * @return JOptionPane.YES_OPTION, JOptionPane.NO_OPTION, JOoptionPane.CANCEL_OPTION
     */
    public static int stopButtonWarning(PApplet parent) {
    	return JOptionPane.showConfirmDialog(
            parent,
            "Cancel playback? Current video statistics will not be saved.",
            "Warning",
            JOptionPane.YES_NO_OPTION
        );
    }

    /**
     * Displays a message at the end of video playback. 
     * @param parent the instantiating PApplet
     * @param msg the message to display
     */
	public static void endVideoMessage(PApplet parent, String msg) {
		JOptionPane.showMessageDialog(parent, msg);
	}
}
