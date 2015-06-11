/**
 * @file UIControl.java
 * @author Kay Choi, 909926828
 * @date 14 Feb 15
 * @description Manages the ControlP5 elements for BeeTracker.
 */

package beetracker;

import controlP5.Button;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;
import controlP5.DropdownList;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Range;
import controlP5.Slider;
import controlP5.Toggle;
import controlP5.Tooltip;

import processing.core.PImage;

public class UIControl {
    private final Group setupGroup, playGroup, thresholdGroup;
    private final DropdownList colorList;
    private final Toggle selectToggle, filterToggle;
    private final Button playButton, openButton, recordButton;
    private final PImage[] playIcons, recordIcons;
    private final Range range;
    private final RadioButton radioButtons;
    private final Slider seekBar;
    private final Tooltip toolTip;

    private static final String listLbl = "New color";
    private static final String[] selectMode = {"Inset Frame", "Hive Exit"};
    private static final String[] recordTips = {"Begin tracking", "Pause tracking"};
    private static final String[] playTips = {"Begin playback without tracking", "Begin playback with tracking", "Pause playback"};

    private boolean isPlaying = false, isRecord = false;

    /**
     * Class constructor.
     * @param parent the instantiating object
     * @param cp5 the ControlP5 object\
     */
    public UIControl(BeeTracker parent, ControlP5 cp5) {
        cp5.setFont(cp5.getFont().getFont(), 15);

        toolTip = cp5.getTooltip()
            .setPositionOffset(0f, -20f)
            .setAlpha(0)
            .setColorLabel(0xffffffff);

        setupGroup = cp5.addGroup("setup").setLabel("").setVisible(false);
        playGroup = cp5.addGroup("playback").setLabel("").setVisible(false);
        thresholdGroup = cp5.addGroup("threshold").setLabel("").setVisible(false);

        Button editColor = cp5.addButton("editColor").setSize(90, 20);
        editColor.setPosition(parent.width - 270, 25)
            .setCaptionLabel("Edit color")
            .setGroup(setupGroup)
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        Button removeColor = cp5.addButton("removeColor").setSize(120, 20);
        removeColor.setPosition(
                editColor.getPosition().x + 95,
                editColor.getPosition().y
            ).setCaptionLabel("Remove color")
            .setGroup(setupGroup)
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        colorList = cp5.addDropdownList("colorList").setSize(215, 560);
        colorList.setPosition(
                editColor.getPosition().x,
                editColor.getPosition().y
            ).setCaptionLabel("Colors")
            .actAsPulldownMenu(true)
            .setGroup(setupGroup)
            .setCaptionLabel(listLbl)
            .setBarHeight(20)
            .addItem(listLbl, -1);
        colorList.getCaptionLabel()
            .alignY(ControlP5Constants.CENTER)
            .setPaddingX(10);

        openButton = cp5.addButton("openButton").setSize(120, 50);
        openButton.setPosition(
                (parent.width - openButton.getWidth())/2,
                parent.height - 150
            ).setCaptionLabel("Open video file")
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        playIcons = new PImage[2];
        playIcons[0] = parent.loadImage("data/img/playbutton.png");
        playIcons[1] = parent.loadImage("data/img/pausebutton.png");
        playButton = cp5.addButton("playButton")
            .setCaptionLabel("")
            .setPosition(90, parent.height - 40)
            .setSize(30, 30)
            .setGroup(playGroup)
            .setImage(playIcons[0]);

        toolTip.register(playButton, playTips[0]);

        cp5.addButton("ejectButton")
            .setCaptionLabel("")
            .setPosition(
                playButton.getPosition().x + 40,
                playButton.getPosition().y
            ).setSize(30, 30)
            .setGroup(playGroup)
            .setImage(parent.loadImage("data/img/ejectbutton.png"));

        toolTip.register("ejectButton", "Close the current video");

        recordIcons = new PImage[2];
        recordIcons[0] = parent.loadImage("data/img/recordbutton1.png");
        recordIcons[1] = parent.loadImage("data/img/recordbutton2.png");
        recordButton = cp5.addButton("recordButton")
            .setCaptionLabel("")
            .setPosition(
                playButton.getPosition().x - 40,
                playButton.getPosition().y
            ).setSize(30, 30)
            .setGroup(playGroup)
            .setImage(recordIcons[0]);

        toolTip.register(recordButton, recordTips[0]);

        seekBar = cp5.addSlider("seek").setBroadcast(false)
            .setSize(
                (int)(parent.width - 200 - (playButton.getPosition().x + 120)),
                15
            ).setPosition(
                playButton.getPosition().x + 120,
                playButton.getPosition().y
            ).setGroup(playGroup)
            .setBroadcast(true);

        Toggle pipToggle = cp5.addToggle("pipToggle").setSize(15, 15);
        pipToggle.setCaptionLabel("Inset Zoom")
            .setGroup(playGroup)
            .setPosition(parent.width - 82, parent.height - 20)
            .getCaptionLabel()
            .align(ControlP5Constants.LEFT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        selectToggle = cp5.addToggle("selectToggle").setSize(32, 15);
        selectToggle.setMode(ControlP5Constants.SWITCH)
            .setPosition(parent.width - 82, parent.height - 40)
            .setCaptionLabel(selectMode[1])
            .setGroup(setupGroup)
            .getCaptionLabel()
            .align(ControlP5Constants.LEFT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        toolTip.register(selectToggle, "Boundary selection");

        filterToggle = cp5.addToggle("filterToggle").setSize(15, 15);
        filterToggle.setCaptionLabel("filter")
            .setPosition(pipToggle.getPosition().x + 17, pipToggle.getPosition().y)
            .setVisible(false)
            .getCaptionLabel()
            .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        Toggle tmp = cp5.addToggle("Hue").setBroadcast(false).toggle();
        tmp.getCaptionLabel()
            .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        radioButtons = cp5.addRadioButton("radioButtons")
            .setPosition(250, 27)
            .setItemsPerRow(3)
            .setSpacingColumn(50)
            .addItem(tmp, 0)
            .addItem("Sat", 1)
            .addItem("Val", 2)
            .setSize(15,15)
            .setNoneSelectedAllowed(false)
            .setGroup(thresholdGroup);

        range = cp5.addRange("thresholdSlider").setBroadcast(false);
        range.setSize(265, 15)
            .setRange(0, 255)
            .setHandleSize(5)
            .setRangeValues(0f, 40f)
            .setPosition(
                radioButtons.getPosition().x,
                radioButtons.getPosition().y - 20
            ).setCaptionLabel("Threshold")
            .setGroup(thresholdGroup)
            .setLowValueLabel("")
            .setHighValueLabel("+/-40.00")
            .setBroadcast(true)
            .getCaptionLabel()
            .align(ControlP5.RIGHT, ControlP5.BOTTOM_OUTSIDE)
            .setPaddingX(5)
            .setPaddingY(6);
    }

    /**
     * Toggles the visibility of the playback controls.
     */
    public void togglePlay() {
        playGroup.setVisible(!playGroup.isVisible());
    }

    /**
     * Adds a new item to the color selection list.
     * @param label the item label
     */
    public void addListItem(String label) {
        colorList.addItem(label, colorList.getListBoxItems().length-1);
    }

    /**
     * Removes an item from the color selection list.
     * @param lbl the label of the item to remove
     */
    public void removeListItem(String lbl) {
        String[][] oldLbls = colorList.getListBoxItems();
        String[] newLbls = new String[oldLbls.length-1];

        int j = 0;
        for(int i = 0; i < oldLbls.length && j < newLbls.length; i++) {
            if(!oldLbls[i][0].equalsIgnoreCase(lbl)) {
                newLbls[j] = oldLbls[i][0];
                j++;
            }
        }

        colorList.clear();

        for(j = 0; j < newLbls.length; j++) {
            colorList.addItem(newLbls[j], j-1);
        }
        colorList.setCaptionLabel(listLbl);
    }

    /**
     * Sets the label of a color selection list item.
     * @param newLbl the new label of the item
     * @param index the index of the item
     */
    public void setListItem(String newLbl, int index) {
        colorList.getItem(index+1).setText(newLbl);
        colorList.setCaptionLabel(newLbl);
    }

    /**
     * Sets the state of the play/pause button.
     * @param state the new play state
     */
    public void setPlayState(boolean state) {
        isPlaying = state;
        playButton.setImage(playIcons[(state ? 1 : 0)]);
        toolTip.register(playButton, playTips[(state ? 2 : (isRecord ? 1 : 0))]);
    }

    /**
     * Updates the label of the selection mode switch.
     */
    public void toggleSelectLbl() {
        selectToggle.setCaptionLabel(selectMode[(selectToggle.getState() ? 0 : 1)]);
    }

    /**
     * Toggles the visibility of the setup mode components.
     */
    public void toggleSetup() {
        setupGroup.setVisible(!setupGroup.isVisible());
    }

    /**
     * Toggles the visibility of the "open video" button.
     */
    public void toggleOpenButton() {
        openButton.setVisible(!openButton.isVisible());
    }

    /**
     * Toggles the visibility of the "show filtered image" checkbox.
     * @param visible the new visibility state
     */
    public void setFilterToggleVisibility(boolean visible) {
        filterToggle.setVisible(visible);
    }

    /**
     * Toggles the visibility of the "threshold" slider.
     * @param visible the visibility state
     */
    public void setRangeVisibility(boolean visible) {
        thresholdGroup.setVisible(visible);
    }

    /**
     * Updates the slider values.
     * @param val the new slider values
     */
    public void setRangeValues(int[] val) {
        range.setBroadcast(false).setRangeValues(val[0], val[1]).setBroadcast(true);
    }

    /**
     * Retrieves the currently selected threshold.
     * @return the current radio button integer value:
     *   0 = hue
     *   1 = saturation
     *   2 = luminosity
     */
    public int getThresholdType() {
        return (int)radioButtons.getValue();
    }

    /**
     * Sets the leftmost slider label.
     * @param lbl the new label
     */
    public void setHueRangeLabel() {
        range.setLowValueLabel("");
        range.setHighValueLabel("+/-"+range.getHighValue());
    }

    /**
     * Changes the value of the seek bar.
     * @param time the new time in seconds
     */
    public void setSeekTime(float time){
        seekBar.changeValue(time);

        setSeekLabel(time);
    }

    /**
     * Changes the seek bar label to mm:ss.mm format.
     * @param time the new time in seconds
     */
    public void setSeekLabel(float time){
        int tmp = (int)(time*100);
        seekBar.setValueLabel(String.format("%02d:%02d.%02d", tmp/6000, (tmp/100)%60, tmp%100));
    }

    /**
     * @return the current value of the seek bar
     */
    public float getSeekTime() {
        return seekBar.getValue();
    }

    /**
     * Sets the seek bar range.
     * @param duration the video duration
     */
    public void setSeekRange(float duration) {
        seekBar.setRange(0f, duration)
            .getCaptionLabel()
            .align(ControlP5.RIGHT, ControlP5.BOTTOM_OUTSIDE)
            .setPaddingX(0)
            .setPaddingY(5);
    }

    /**
     * Sets the state of the record button.
     * @param state the new recording state
     */
    public void setRecordState(boolean state) {
        isRecord = state;
        int index = (state ? 1 : 0);
        recordButton.setImage(recordIcons[index]);
        toolTip.register(recordButton, recordTips[index]);

        if(!isPlaying) {
            toolTip.register(playButton, playTips[index]);
        }
    }

    /**
     * @return the current values of the threshold slider
     */
    public float[] getRangeValues() {
        return range.getArrayValue();
    }

    /**
     * Forces activation of a radio button.
     * @param val the value of the radio button to activate
     */
    public void selectRadioButton(int val) {
        radioButtons.activate(0);
    }
}
