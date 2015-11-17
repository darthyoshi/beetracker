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

import java.awt.FileDialog;
import java.io.File;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JDialog;
import javax.swing.JFileChooser;

/**
 * @class VideoBrowser
 * @author Kay Choi
 * @date 13 Nov 15
 * @description Uses a Java Swing file selector to retrieve a video file.
 */
class VideoBrowser {
    private static final String[] videoExts = {"mov", "mpg", "mpeg", "avi", "mp4"};
    private static final String OS = System.getProperty("os.name");

    /**
     * Displays a file browser that filters for video files.
     * @param parent the invoking BeeTracker
     * @param currentFile the most recently selected file
     */
    static void getVideoFile(
        final BeeTracker parent,
        final File currentFile
    ) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                File selectedFile = null;

                JDialog dialog = new JDialog();
                dialog.setAlwaysOnTop(true);

                FileDialog fd = new FileDialog(
                    dialog,
                    "Select video",
                    FileDialog.LOAD
                );

                if(currentFile != null) {
                    fd.setDirectory(currentFile.getParentFile().getAbsolutePath());
                }

                //FilenameFilter has no functionality on Windows
                if(OS.toLowerCase(java.util.Locale.ROOT).contains("windows")) {
                    fileCheck:
                    do {
                        fd.setVisible(true);

                        if(fd.getFile() != null) {
                            System.out.append("selected file: \"")
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

                            System.out.append("invalid file extension: ")
                                .append(nameParts[nameParts.length-1])
                                .append('\n')
                                .flush();
                        }
                    } while(fd.getFile() != null);
                }

                else {
                    fd.setFilenameFilter(new java.io.FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
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
                        System.out.append("selected file: \"")
                            .append(fd.getFile())
                            .append("\"\n")
                            .flush();

                        selectedFile = fd.getFiles()[0];
                    }
                }

                if(selectedFile != null) {
                    System.out.append("setting time stamp\n").flush();

                    setDateTime(parent);
                }

                parent.loadVideo(selectedFile);
            }
        });

        thread.start();
    }

    /**
     * Prompts the user to set a time stamp.
     * @param parent the invoking BeeTracker
     */
    private static void setDateTime(final BeeTracker parent) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
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
                    null/*((PSurfaceAWT.SmoothCanvas)parent.getSurface().getNative()).getFrame()*/,
                    panel,
                    "Set the video time stamp",
                    javax.swing.JOptionPane.PLAIN_MESSAGE
                );

                calendar.setTime((Date)dateTimeSpinner.getValue());

                parent.setTime(calendar);
            }
        });
    }

    /**
     * Displays a file browser for image directories.
     * @param parent the invoking BeeTracker
     * @param currentDir the previously selected directory
     */
    static void getImageSequence(
        final BeeTracker parent,
        final File currentDir
    ) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                File selectedDir = null;

                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(currentDir);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.requestFocusInWindow();

                JDialog dialog = new JDialog();
                dialog.add(chooser);
                dialog.pack();
                dialog.setAlwaysOnTop(true);

                int returnVal = chooser.showOpenDialog(dialog);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    selectedDir = chooser.getSelectedFile();
                }

                if(selectedDir != null) {
                    System.out.append("setting time stamp\n").flush();

                    setDateTime(parent);
                }

                parent.loadImgSequence(selectedDir);
            }
        });
    }
}
