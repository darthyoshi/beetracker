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
    private static final String crashMsg[] = {
        "The application has encountered the following error:\n",
        "\nand will now close. Please check Console.log for details."
    };
    private static final String endOptions[] = {"Replay video", "Close video"};

    /**
     * Displays a warning message if setup parameters have not been set.
     * @param parent the invoking PApplet
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
     * @param parent the invoking PApplet
     * @return JOptionPane.YES_OPTION, JOptionPane.NO_OPTION,
     *   JOoptionPane.CANCEL_OPTION
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
     * @param parent the invoking PApplet
     * @param msg the message to display
     * @return JOptionPane.YES_OPTION, JOptionPane.NO_OPTION,
     *   JOptionPane.CANCEL_OPTION
     */
    public static int endVideoMessage(PApplet parent, String msg) {
        javax.swing.JTextArea textArea = new javax.swing.JTextArea(msg);  
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true); 

        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(400, 250));

        return JOptionPane.showOptionDialog(parent, scrollPane, "Session Summary",
            JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, endOptions, null);
    }

    /**
     * Displays a program crash message.
     * @param parent the invoking PApplet
     * @param msg the cause of the crash
     */
    public static void crashMessage(PApplet parent, String msg) {
        StringBuilder builder = new StringBuilder(crashMsg[0]);
        builder.append(msg).append(crashMsg[1]);

        JOptionPane.showMessageDialog(parent, builder.toString(), "Fatal Error",
            JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays a message listing the location of the saved video statistics
     *   and prompts the user for confirmation on whether or not to save the
     *   individual frame data.
     * @param parent the invoking PApplet
     * @param filePath the path to the saved statistics file
     * @return JOptionPane.YES_OPTION, JOptionPane.NO_OPTION,
     *   JOptionPane.CANCEL_OPTION
     */
    public static int saveStatisticsmessage(PApplet parent, String filePath) {
        return JOptionPane.showConfirmDialog(
            parent,
            "Video statistics have been saved to \"" + filePath +
                "\"\nSave frame annotations?",
            "Results Saved",
            JOptionPane.YES_NO_OPTION
        );
    }
}
