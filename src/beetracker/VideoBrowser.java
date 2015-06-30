/**
 * @file VideoBrowser.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description Uses a Java Swing file selector to retrieve a video file.
 */

package beetracker;

import java.util.Calendar;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import processing.core.PApplet;

public class VideoBrowser {
    private static final FileNameExtensionFilter movType =
        new FileNameExtensionFilter("*.mov", "mov");
    private static final FileNameExtensionFilter allType =
        new FileNameExtensionFilter("All video files", "mov", "mpg", "mpeg", "avi", "mp4");
    private static final FileNameExtensionFilter aviType =
        new FileNameExtensionFilter("*.avi", "avi");
    private static final FileNameExtensionFilter mpgType =
        new FileNameExtensionFilter("*.mpg, *.mpeg", "mpg", "mpeg");
    private static final FileNameExtensionFilter mp4Type =
        new FileNameExtensionFilter("*.mp4", "mp4");

    /**
     * Displays a file browser that filters for video files.
     * @param parent the invoking BeeTracker
     * @param currentDir the initial directory to browse from
     * @return a Java File object
     */
    public static void getVideoFile(
        final BeeTracker parent,
        final java.io.File currentDir,
        final java.io.PrintStream log
    ) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                java.io.File selectedFile = null;
                Calendar dateTime = null;

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select video");

                log.append("setting directory\n").flush();
                if(currentDir != null) {
                    fileChooser.setCurrentDirectory(currentDir);
                }
                log.append("setting filters\n").flush();
                fileChooser.setFileFilter(allType);
                fileChooser.addChoosableFileFilter(aviType);
                fileChooser.addChoosableFileFilter(mpgType);
                fileChooser.addChoosableFileFilter(mp4Type);
                fileChooser.addChoosableFileFilter(movType);
                fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());

                if(fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                }

                if(selectedFile != null) {
                    log.append("setting time stamp\n").flush();
                    dateTime = getDateTime(parent);
                }

                parent.loadVideo(selectedFile, dateTime);
            }
        });
    }

    /**
     * Prompts the user to set a time stamp.
     * @param parent the invoking BeeTracker
     * @return a Calendar object representing the selected time stamp
     */
    private static Calendar getDateTime(BeeTracker parent) {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.YEAR, -100);
        Date start = calendar.getTime();
        calendar.add(Calendar.YEAR, 200);
        Date stop = calendar.getTime();
    
        javax.swing.JSpinner dateTimeSpinner = new javax.swing.JSpinner(
            new javax.swing.SpinnerDateModel(
                now,
                start,
                stop,
                Calendar.DATE
            )
        );
    
        javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.add(dateTimeSpinner);
    
        javax.swing.JOptionPane.showMessageDialog(
            parent,
            panel,
            "Set the initial video time stamp",
            javax.swing.JOptionPane.PLAIN_MESSAGE
        );
    
        calendar.setTime((Date)dateTimeSpinner.getValue());

        return calendar;
    }
}
