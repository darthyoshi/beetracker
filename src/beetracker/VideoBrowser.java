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
    /**
     * Displays a file browser that filters for video files.
     * @param parent the invoking PApplet
     * @param currentDir the initial directory to browse from
     * @return a Java File object
     */
    public static File getVideoFile(PApplet parent, File currentDir) {
        File result = null;

        JFileChooser browser = new JFileChooser();
        browser.setFileFilter(new FileNameExtensionFilter("All video files", "mov", "mpg", "mpeg", "avi", "mp4"));
        browser.addChoosableFileFilter(new FileNameExtensionFilter("*.avi", "avi"));
        browser.addChoosableFileFilter(new FileNameExtensionFilter("*.mpg, *.mpeg", "mpg", "mpeg"));
        browser.addChoosableFileFilter(new FileNameExtensionFilter("*.mp4", "mp4"));
        browser.addChoosableFileFilter(new FileNameExtensionFilter("*.mov", "mov"));
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
