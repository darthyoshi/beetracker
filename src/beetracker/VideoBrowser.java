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

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.io.File;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @class VideoBrowser
 * @author Kay Choi
 * @date 13 Nov 15
 * @description Uses a Java Swing file selector to retrieve a video file.
 */
class VideoBrowser {
  private static final String[] videoExts = {"mov", "mpg", "mpeg", "avi", "mp4"};
  private static final String OS = System.getProperty("os.name");

  private static final FileNameExtensionFilter movFilter =
    new FileNameExtensionFilter("*.mov", "mov");
  private static final FileNameExtensionFilter allFilter =
    new FileNameExtensionFilter("All video files", "mov", "mpg", "mpeg", "avi", "mp4");
  private static final FileNameExtensionFilter aviFilter =
    new FileNameExtensionFilter("*.avi", "avi");
  private static final FileNameExtensionFilter mpgFilter =
    new FileNameExtensionFilter("*.mpg, *.mpeg", "mpg", "mpeg");
  private static final FileNameExtensionFilter mp4Filter =
    new FileNameExtensionFilter("*.mp4", "mp4");

  /**
   * Displays a file browser that filters for video files.
   * @param parent the invoking BeeTracker
   * @param currentFile the most recently selected file
   */
  static void getVideoFile(
    final BeeTracker parent,
    final File currentFile
  ) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        File selectedFile = null;

        JDialog dialog = new JDialog();
        dialog.setAlwaysOnTop(true);

        //Use AWT FileDialog on Mac OS X
        if(OS.toLowerCase(java.util.Locale.ROOT).contains("mac")) {
          System.setProperty("apple.awt.fileDialogForDirectories", "false");

          FileDialog fd = new FileDialog(
            dialog,
            "Select video",
            FileDialog.LOAD
          );
          fd.requestFocusInWindow();
          dialog.pack();

          if(currentFile != null) {
            fd.setDirectory(currentFile.getParentFile().getAbsolutePath());
          }

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
            selectedFile = fd.getFiles()[0];
          }
        } else {  //Use Swing JFileChooser on Linux/Windows
          JFileChooser chooser = new JFileChooser();
          chooser.setDialogTitle("Select video");
          chooser.setCurrentDirectory(currentFile);

          chooser.setFileFilter(allFilter);
          chooser.addChoosableFileFilter(aviFilter);
          chooser.addChoosableFileFilter(mpgFilter);
          chooser.addChoosableFileFilter(mp4Filter);
          chooser.addChoosableFileFilter(movFilter);
          chooser.removeChoosableFileFilter(chooser.getAcceptAllFileFilter());

          chooser.requestFocusInWindow();

          dialog.add(chooser);
          dialog.pack();

          if(chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
          }
        }

        parent.loadVideo(selectedFile);

        if(selectedFile != null) {
          System.out.append("selected directory: \"")
            .append(selectedFile.getAbsolutePath())
            .append("\"\nsetting time stamp\n")
            .flush();

          setDateTime(parent);
        }
      }
    });
  }

  /**
   * Prompts the user to set a time stamp.
   * @param parent the invoking BeeTracker
   */
  private static void setDateTime(final BeeTracker parent) {
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

  /**
   * Displays a file browser for image directories.
   * @param parent the invoking BeeTracker
   * @param currentDir the previously selected directory
   */
  static void getImageSequence(
    final BeeTracker parent,
    final File currentDir
  ) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        File selectedDir = null;

        JDialog dialog = new JDialog();
        dialog.setAlwaysOnTop(true);

        //Use AWT FileDialog on Mac OS X
        if(OS.toLowerCase(java.util.Locale.ROOT).contains("mac")) {
          System.setProperty("apple.awt.fileDialogForDirectories", "true");

          FileDialog fd = new FileDialog(
            dialog,
            "Select image directory",
            FileDialog.LOAD
          );
          dialog.pack();

          if(currentDir != null) {
            fd.setDirectory(currentDir.getParentFile().getAbsolutePath());
          }

          fd.setVisible(true);

          if(fd.getFile() != null) {
            selectedDir = fd.getFiles()[0];
          }
        } else {  //Use Swing JFileChooser on Linux/Windows
          JFileChooser chooser = new JFileChooser();
          chooser.setDialogTitle("Select image directory");
          chooser.setCurrentDirectory(currentDir);
          chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          chooser.requestFocusInWindow();

          dialog.add(chooser);
          dialog.pack();

          if(chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            selectedDir = chooser.getSelectedFile();
          }
        }

        parent.loadImgSequence(selectedDir);

        if(selectedDir != null) {
          System.out.append("selected directory: \"")
            .append(selectedDir.getAbsolutePath())
            .append("\"\nsetting time stamp\n")
            .flush();

          setDateTime(parent);
        }
      }
    });
  }
}
