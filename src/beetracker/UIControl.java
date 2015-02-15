/**
 * @file UIControl.java
 * @author Kay Choi, 909926828
 * @date 14 Feb 15
 * @description
 */

package beetracker;

import controlP5.Button;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;
import controlP5.Group;
import controlP5.Toggle;

public class UIControl {
    private Group group1, group2;
    private Button openButton;
    private Button colorsButton;

    /**
     * Class constructor.
     * @param parent the instantiating PApplet object
     */
    public UIControl(processing.core.PApplet parent) {
        ControlP5 cp5 = new ControlP5(parent);
        cp5.setFont(cp5.getFont().getFont(), 15);

        group1 = cp5.addGroup("setup").setLabel("").setVisible(true);
        group2 = cp5.addGroup("playback").setLabel("").setVisible(false);

        openButton = cp5.addButton("openButton")
            .setSize(120, 20)
            .setPosition(25, 25)
            .setCaptionLabel("Open video file")
            .setGroup(group1);
        openButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);

        colorsButton = cp5.addButton("colorsButton")
            .setSize(150, 20)
            .setPosition(150, 25)
            .setCaptionLabel("Set tracking colors")
            .setGroup(group1);
        colorsButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);

        new NewToggle(cp5, "playButton")
            .setCaptionLabel("")
            .setPosition(10, 10)
            .setSize(30, 30)
            .setGroup(group2)
            .setImages(
                parent.loadImage("data/img/playbutton.png"),
                null,
                parent.loadImage("data/img/pausebutton.png")
            );

        cp5.addButton("fastForward")
            .setCaptionLabel("")
            .setPosition(50, 10)
            .setSize(30, 30)
            .setGroup(group2)
            .setImage(parent.loadImage("data/img/fastforward.png"));

        cp5.addButton("stopButton")
            .setCaptionLabel("")
            .setPosition(90, 10)
            .setSize(30, 30)
            .setGroup(group2)
            .setImage(parent.loadImage("data/img/stopbutton.png"));
    }

    /**
     *
     */
    public void toggleGroup() {
        group1.setVisible(!group1.isVisible());
    }

    /**
     * An extension of the ControlP5 Toggle class that does not react in
     *   response to mouse onEnter events.
     * @author Kay Choi
     */
    private class NewToggle extends Toggle {
        /**
         * Class constructor.
         * @param control the instantiating ControlP5 object
         * @param label the label for the NewToggle
         */
        private NewToggle(ControlP5 control, String label) {
            super(control, label);
        }

        public void onEnter() {}
    }

    /**
     *
     */
    public void togglePlay() {
        group2.setVisible(!group2.isVisible());
    }
}