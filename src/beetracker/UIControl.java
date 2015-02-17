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
import controlP5.DropdownList;
import controlP5.Group;
import controlP5.Toggle;

public class UIControl {
    private final Group group1, group2;
    private DropdownList colorList;
    private Toggle playToggle;
    private Button openButton;

    private final String listLbl = "New color";

    /**
     * Class constructor.
     * TODO move color editing components
     * @param parent the instantiating PApplet object
     */
    public UIControl(processing.core.PApplet parent) {
        ControlP5 cp5 = new ControlP5(parent);
        cp5.setFont(cp5.getFont().getFont(), 15);

        group1 = cp5.addGroup("setup").setLabel("").setVisible(true);
        group2 = cp5.addGroup("playback").setLabel("").setVisible(false);

        openButton = cp5.addButton("openButton").setSize(120, 20);
        openButton.setPosition(
                (parent.width - openButton.getWidth())/2,
                parent.height/2 -20
            ).setCaptionLabel("Open video file")
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        Button editColor = cp5.addButton("editColor").setSize(90, 20);
        editColor.setPosition(
                (parent.width - editColor.getWidth())/2,
                parent.height/2 + 40
            ).setCaptionLabel("Edit color")
            .setGroup(group1)
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        colorList = cp5.addDropdownList("colorList")
            .setCaptionLabel("Colors")
            .actAsPulldownMenu(true)
            .setGroup(group1)
            .setCaptionLabel(listLbl)
            .setBarHeight(20)
            .setSize(90, 60);
        colorList.setPosition(
                (parent.width - colorList.getWidth())/2,
                editColor.getPosition().y - 5
            ).addItem(listLbl, 0);
        colorList.getCaptionLabel().alignY(ControlP5Constants.CENTER);

        Button removeColor = cp5.addButton("removeColor").setSize(120, 20);
        removeColor.setPosition(
                (parent.width - removeColor.getWidth())/2,
                400
            ).setCaptionLabel("Remove color")
            .setGroup(group1)
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        playToggle = new NewToggle(cp5, "playButton")
            .setCaptionLabel("")
            .setPosition(50, parent.height - 40)
            .setSize(30, 30)
            .setGroup(group2)
            .setImages(
                parent.loadImage("data/img/playbutton.png"),
                null,
                parent.loadImage("data/img/pausebutton.png")
            );

        cp5.addButton("fastForward")
            .setCaptionLabel("")
            .setPosition(90, parent.height - 40)
            .setSize(30, 30)
            .setGroup(group2)
            .setImage(parent.loadImage("data/img/fastforward.png"));

        cp5.addButton("stopButton")
            .setCaptionLabel("")
            .setPosition(130, parent.height - 40)
            .setSize(30, 30)
            .setGroup(group2)
            .setImage(parent.loadImage("data/img/stopbutton.png"));

        Toggle pipToggle = cp5.addToggle("pipToggle").setSize(15, 15);
        pipToggle.setCaptionLabel(" Zoom")
            .setGroup(group2)
            .setPosition(170, parent.height - 30)
            .getCaptionLabel()
            .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER);
    }

    /**
     * TODO add method header
     */
    public void toggleColors() {
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

        @Override
        public void onEnter() {}
    }

    /**
     * TODO add method header
     */
    public void togglePlay() {
        group2.setVisible(!group2.isVisible());
    }

    /**
     * TODO add method header
     * @param color
     * @param val
     */
    public void addListItem(String color, int val) {
        colorList.addItem(color, val);
    }

    /**
     * TODO add method header
     */
    public void clearList() {
        colorList.clear();
        colorList.addItem(listLbl, 0);
        colorList.setCaptionLabel(listLbl);
    }

    /**
     * TODO add method header
     */
    public void togglePlayState() {
        playToggle.toggle();
    }

    /**
     * TODO add method header
     */
    public void toggleOpenButton() {
        openButton.setVisible(!openButton.isVisible());
    }
}