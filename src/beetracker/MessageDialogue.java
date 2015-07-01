/**
 * @file MessageDialogue.java
 * @author Kay Choi, 909926828
 * @date 11 Mar 15
 * @description Displays Java Swing message dialogues.
 */

package beetracker;

import java.awt.EventQueue;

import javax.swing.JOptionPane;

/**
 *
 * @author Kay Choi
 */
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
     * @param parent the invoking BeeTracker
     * @param errors the setup error flags
     */
    public static void playButtonErrorMessage(BeeTracker parent, boolean[] errors) {
        EventQueue.invokeLater(() -> {
            StringBuilder msg = new StringBuilder();

            if(errors[0]) {
                msg.append(errorMsg[0]);
            }

            if(errors[1]) {
                msg.append(errorMsg[1]);
            }

            JOptionPane.showMessageDialog(parent, msg.toString(), "Error",
                JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * Displays a dialogue to confirm whether or not to prematurely end playback.
     * @param parent the invoking BeeTracker
     */
    public static void stopButtonWarning(BeeTracker parent) {
        EventQueue.invokeLater(() -> {
            if(
                JOptionPane.showConfirmDialog(
                    parent,
                    "Cancel playback? Current video statistics will not be saved.",
                    "Warning",
                    JOptionPane.YES_NO_OPTION
                ) == JOptionPane.YES_OPTION
            ) {
                parent.stopPlayback();
            }
        });
    }

    /**
     * Displays a message at the end of video playback.
     * @param parent the invoking BeeTracker
     * @param msg the message to display
     * @param fileName the name of the summary file
     */
    public static void endVideoMessage(
        BeeTracker parent,
        String msg,
        String fileName
    ) {
        EventQueue.invokeLater(() -> {
            javax.swing.JTextArea textArea = new javax.swing.JTextArea(msg);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(textArea);
            scrollPane.setPreferredSize(new java.awt.Dimension(400, 250));

            if(
                JOptionPane.showOptionDialog(
                    parent,
                    scrollPane,
                    "Session Summary",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    endOptions,
                    null
                ) == JOptionPane.YES_OPTION
            ) {
                parent.rewindVideo();
            }

            else {
                if(MessageDialogue.saveStatisticsMessage(parent, fileName) ==
                    JOptionPane.YES_OPTION)
                {
                    parent.writeFramePointsToJSON();
                }

                parent.stopPlayback();
            }
        });
    }

    /**
     * Displays a program crash message.
     * @param parent the invoking BeeTracker
     * @param msg the cause of the crash
     */
    public static void crashMessage(BeeTracker parent, String msg) {
        EventQueue.invokeLater(() -> {
            StringBuilder builder = new StringBuilder(crashMsg[0]);
            builder.append(msg).append(crashMsg[1]);

            JOptionPane.showMessageDialog(parent, builder.toString(),
                "Fatal Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * Displays a message listing the location of the saved video statistics
     *   and prompts the user for confirmation on whether or not to save the
     *   individual frame data.
     * @param parent the invoking BeeTracker
     * @param filePath the path to the saved statistics file
     * @return JOptionPane.YES_OPTION, JOptionPane.NO_OPTION,
     *   JOptionPane.CANCEL_OPTION
     */
    private static int saveStatisticsMessage(BeeTracker parent, String filePath) {
        return JOptionPane.showConfirmDialog(
            parent,
            "Video statistics have been saved to \"" + filePath +
                "\"\nSave frame annotations?",
            "Results Saved",
            JOptionPane.YES_NO_OPTION
        );
    }
}
