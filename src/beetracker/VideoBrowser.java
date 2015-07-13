/**
 * @file VideoBrowser.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description Uses a Java Swing file selector to retrieve a video file.
 */

package beetracker;

import java.awt.FileDialog;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author Kay Choi
 */
public class VideoBrowser {
    private static final String[] videoExts = {"mov", "mpg", "mpeg", "avi", "mp4"};
    private static final String OS = System.getProperty("os.name");

    /**
     * Displays a file browser that filters for video files.
     * @param parent the invoking BeeTracker
     * @param currentFile the most recently selected file
     * @param log a PrintStream object used for logging
     */
    public static void getVideoFile(
        final BeeTracker parent,
        final java.io.File currentFile,
        final java.io.PrintStream log
    ) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                java.io.File selectedFile = null;

                FileDialog fd = new FileDialog(parent.frame, "Select video", FileDialog.LOAD);

                if(currentFile != null) {
                    fd.setDirectory(currentFile.getParentFile().getAbsolutePath());
                }

                //FilenameFilter has no functionality on Windows
                if(OS.toLowerCase(java.util.Locale.ROOT).contains("windows")) {
                    fileCheck:
                    do {
                        fd.setVisible(true);

                        if(fd.getFile() != null) {
                            log.append("selected file: \"")
                                .append(fd.getFile())
                                .append("\"\n")
                                .flush();

                            String nameParts[] = fd.getFiles()[0].getName().split("\\.");

                            for(String ext : videoExts) {
                                if(ext.equalsIgnoreCase(nameParts[nameParts.length-1])) {
                                    selectedFile = fd.getFiles()[0];

                                    break fileCheck;
                                }
                            }

                            MessageDialogue.wrongFileTypeMessage(parent);

                            log.append("invalid file extension: ")
                                .append(nameParts[nameParts.length-1])
                                .append('\n')
                                .flush();
                        }
                    } while(fd.getFile() != null);
                }

                else {
                    fd.setFilenameFilter(new java.io.FilenameFilter() {
                        @Override
                        public boolean accept(java.io.File dir, String name) {
                            boolean result = false;
                            String[] parts = name.split("\\.");

                            for(String type : videoExts) {
                                if(type.equalsIgnoreCase(parts[parts.length-1])) {
                                    result = true;
                                    break;
                                }
                            }

                            return result;
                        }
                    });

                    fd.setVisible(true);

                    if(fd.getFile() != null) {
                        log.append("selected file: \"")
                            .append(fd.getFile())
                            .append("\"\n")
                            .flush();

                        selectedFile = fd.getFiles()[0];
                    }
                }

                if(selectedFile != null) {
                    log.append("setting time stamp\n").flush();

                    parent.setTime(getDateTime(parent));
                }

                parent.loadVideo(selectedFile);
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
