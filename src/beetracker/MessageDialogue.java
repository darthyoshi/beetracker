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
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * @class MessageDialogue
 * @author Kay Choi
 * @date 28 Jul 16
 * @description Displays Java Swing message dialogues.
 */
class MessageDialogue {
  private static final String errorMsg[] = {
    "No colors have been selected.\n",
    "No hive exit has been defined.\n"
  };
  private static final String crashMsg[] = {
    "The application has encountered the following error:\n",
    "\nand will now close. Please check Console.log for details."
  };
  private static final String endOptions[] = {"Replay video", "Close video"};
  private static final String eventOptions[] = {"Close"};

  /**
   * Displays an error message if setup parameters have not been set.
   * @param parent the invoking BeeTracker
   * @param errors the setup error flags
   */
  static void playButtonErrorMessage(final BeeTracker parent,
    final boolean[] errors)
  {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        StringBuilder msg = new StringBuilder();

        if(errors[0]) {
          msg.append(errorMsg[0]);
        }

        if(errors[1]) {
          msg.append(errorMsg[1]);
        }

        JOptionPane.showMessageDialog(
          null,
          msg.toString(),
          "Error",
          JOptionPane.ERROR_MESSAGE
        );
      }
    });
  }

  /**
   * Displays a dialogue to confirm whether or not to prematurely end playback.
   * @param parent the invoking BeeTracker
   */
  static void stopButtonWarning(final BeeTracker parent) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        if(
          JOptionPane.showConfirmDialog(
            null,
            "Cancel playback? Current video statistics will not be saved.",
            "Warning",
            JOptionPane.YES_NO_OPTION
          ) == JOptionPane.YES_OPTION
        ) {
          if(
            parent.isReplay() &&
            !(
              new File(
                System.getProperty("user.dir") + File.separatorChar +
                "output" + File.separatorChar + parent.getVideoName() +
                File.separatorChar + "points.json"
              ).exists()
            ) &&
            saveAnnotationsMessage(parent) == JOptionPane.YES_OPTION
          ) {
            parent.writeFramePointsToJSON();
          }
          parent.stopPlayback();
        }
      }
    });
  }

  /**
   * Displays a message at the end of video playback.
   * @param parent the invoking BeeTracker
   * @param msg the message to display
   * @param events the event timeline image
   * @param fileName the name of the summary file
   */
  static void endVideoMessage(
    final BeeTracker parent,
    final String msg,
    final processing.core.PImage events,
    final String fileName
  ) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        javax.swing.JTextArea textArea = new javax.swing.JTextArea(msg);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(400, 300));

        javax.swing.JTabbedPane tabPane;

        if(events != null) {
          JScrollPane scrollPane2 = new JScrollPane(
            new javax.swing.JLabel(new javax.swing.ImageIcon(
              (java.awt.image.BufferedImage)events.getNative()))
          );

          tabPane = new javax.swing.JTabbedPane();
          tabPane.add("Text summary", scrollPane);
          tabPane.add("Visual summary", scrollPane2);
        } else {
          tabPane = null;
        }

        if(
          JOptionPane.showOptionDialog(
            null,
            (events == null ? scrollPane : tabPane),
            "Session Summary",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            endOptions,
            null
          ) == JOptionPane.YES_OPTION
        ) {
          parent.rewindVideo();
        } else {
          if(MessageDialogue.saveStatisticsMessage(parent, fileName) ==
            JOptionPane.YES_OPTION)
          {
            parent.writeFramePointsToJSON();
          }

          parent.stopPlayback();
        }
      }
    });
  }

  /**
   * Displays a program crash message.
   * @param parent the invoking BeeTracker
   * @param msg the cause of the crash
   */
  static void crashMessage(final BeeTracker parent, final String msg) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        StringBuilder builder = new StringBuilder(crashMsg[0]);
        builder.append(msg).append(crashMsg[1]);

        JOptionPane.showMessageDialog(
          null,
          builder.toString(),
          "Fatal Error",
          JOptionPane.ERROR_MESSAGE
        );

        parent.exit();
      }
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
    int result = JOptionPane.NO_OPTION;

    if(filePath != null) {
      String msg = "Video statistics have been saved to \"" + filePath +
        '\"';

      File pointsFile = new File((new File(filePath))
        .getParentFile().getAbsolutePath()+File.separatorChar+"points.json");

      if(pointsFile.exists()) {
        JOptionPane.showMessageDialog(null, msg);
      } else {
        result = JOptionPane.showConfirmDialog(
          null,
          msg + "\nSave frame annotations?",
          "Results Saved",
          JOptionPane.YES_NO_OPTION
        );
      }
    }

    return result;
  }

  /**
   * Displays a window containing the current event timeline.
   * @param parent the invoking BeeTracker
   * @param graphic an PGraphics object depicting the event timeline
   */
  static void showEventTimeline(
    final BeeTracker parent,
    final processing.core.PImage graphic
  ) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        int width, height;
        if(graphic.height > 300) {
          height = 300;
          width = graphic.width + 18;
        } else {
          height = graphic.height + 3;
          width = graphic.width + 3;
        }

        JScrollPane scrollPane = new JScrollPane(
          new javax.swing.JLabel(new javax.swing.ImageIcon(
            (java.awt.image.BufferedImage)graphic.getNative()))
        );
        scrollPane.setSize(width, height);

        javax.swing.JDialog dialog = (new JOptionPane(
          scrollPane,
          JOptionPane.PLAIN_MESSAGE,
          JOptionPane.DEFAULT_OPTION,
          null,
          eventOptions
        )).createDialog(null, "Event Timeline");
        dialog.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
        dialog.pack();
        dialog.setVisible(true);
        dialog.toFront();
        dialog.requestFocus();

        parent.setEventDialog(dialog);
      }
    });
  }

  /**
   * Displays a message that the selected directory contains no images.
   */
  static void showEmptyDirectoryMessage() {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        JOptionPane.showMessageDialog(
          null,
          "Selected directory contains no images!",
          "Empty directory!",
          JOptionPane.ERROR_MESSAGE
        );
      }
    });
  }

  /**
   * Prompts the user to save frame annotations.
   * @param parent the invoking BeeTracker
   * @return JOptionPane.YES_OPTION, JOptionPane.NO_OPTION,
   *   JOptionPane.CANCEL_OPTION 
   */
  static int saveAnnotationsMessage(BeeTracker parent) {
    return JOptionPane.showOptionDialog(
      null,
      "Save frame annotations?",
      "Unsaved annotations!",
      JOptionPane.YES_NO_OPTION,
      JOptionPane.WARNING_MESSAGE,
      null,
      null,
      null
    );
  }
}
