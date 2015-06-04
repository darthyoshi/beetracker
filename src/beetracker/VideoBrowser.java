/**
 * @file VideoBrowser.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description Uses a Java Swing file selector to retrieve a video file.
 */

package beetracker;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import processing.core.PApplet;

public class VideoBrowser {
    private static final FileNameExtensionFilter movType = new FileNameExtensionFilter("*.mov", "mov");
    private static final FileNameExtensionFilter allType = new FileNameExtensionFilter("All video files", "mov", "mpg", "mpeg", "avi", "mp4");
    private static final FileNameExtensionFilter aviType = new FileNameExtensionFilter("*.avi", "avi");
    private static final FileNameExtensionFilter mpgType = new FileNameExtensionFilter("*.mpg, *.mpeg", "mpg", "mpeg");
    private static final FileNameExtensionFilter mp4Type = new FileNameExtensionFilter("*.mp4", "mp4");

    /**
     * Displays a file browser that filters for video files.
     * @param parent the invoking PApplet
     * @param currentDir the initial directory to browse from
     * @return a Java File object
     */
    public static File getVideoFile(PApplet parent, File currentDir) {
        File result = null;

        JFileChooser browser = new JFileChooser();
        browser.setFileFilter(allType);
        browser.addChoosableFileFilter(aviType);
        browser.addChoosableFileFilter(mpgType);
        browser.addChoosableFileFilter(mp4Type);
        browser.addChoosableFileFilter(movType);
        browser.removeChoosableFileFilter(browser.getAcceptAllFileFilter());
        if(currentDir != null) {
            browser.setCurrentDirectory(currentDir);
        }

        if(browser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            result = browser.getSelectedFile();
        }

        return result;
    }

}
