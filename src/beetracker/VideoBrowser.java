package beetracker;

import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import processing.core.PApplet;

public class VideoBrowser {
    private JFileChooser browser;
    private PApplet parent;

    /**
     * 
     * @return 
     */
    public static String getVideoName(PApplet parent) {
        String result = null;

        JFileChooser browser = new JFileChooser();
        browser.setFileFilter(new FileNameExtensionFilter("*.mov", "mov"));
        browser.addChoosableFileFilter(new FileNameExtensionFilter("*.avi", "avi"));
        browser.addChoosableFileFilter(new FileNameExtensionFilter("*.mpg, *.mpeg", "mpg", "mpeg"));
        browser.addChoosableFileFilter(new FileNameExtensionFilter("*.mp4", "mp4"));
        browser.addChoosableFileFilter(new FileNameExtensionFilter("All video files", "mov", "mpg", "mpeg", "avi", "mp4"));
        browser.removeChoosableFileFilter(browser.getAcceptAllFileFilter());

        if(browser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                result = browser.getSelectedFile().getCanonicalPath();
            }
            
            catch (IOException e) {
                parent.println(e.getMessage());
            }
        }

        return result;
    }

}
