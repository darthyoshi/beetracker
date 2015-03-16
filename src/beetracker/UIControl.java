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
import controlP5.Toggle;

import processing.core.PImage;

public class UIControl {
    private final Group setupGroup, playGroup;
    private final DropdownList colorList;
    private final Toggle selectToggle;
    private final Button playButton, openButton;
    private final PImage[] playIcons;

    private static final String listLbl = "New color";
    private static final String[] selectMode = {"Inset Frame", "Hive Exit"};

    /**
     * Class constructor.
     * @param parent the instantiating PApplet object
     */
    public UIControl(processing.core.PApplet parent) {
        ControlP5 cp5 = new ControlP5(parent);
        cp5.setFont(cp5.getFont().getFont(), 15);

        setupGroup = cp5.addGroup("setup").setLabel("").setVisible(false);
        playGroup = cp5.addGroup("playback").setLabel("").setVisible(false);

        Button editColor = cp5.addButton("editColor").setSize(90, 20);
        editColor.setPosition(
                parent.width - 270,
                25
            ).setCaptionLabel("Edit color")
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
        colorList.getCaptionLabel().
            alignY(ControlP5Constants.CENTER).
            setPaddingX(10);

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
        playButton = cp5.addButton("playButton")//new NewToggle(cp5, "playButton")
            .setCaptionLabel("")
            .setPosition(50, parent.height - 40)
            .setSize(30, 30)
            .setGroup(playGroup)
            .setImage(playIcons[0]);

        cp5.addButton("fastForward")
            .setCaptionLabel("")
            .setPosition(90, parent.height - 40)
            .setSize(30, 30)
            .setGroup(playGroup)
            .setImage(parent.loadImage("data/img/fastforward.png"));

        cp5.addButton("stopButton")
            .setCaptionLabel("")
            .setPosition(130, parent.height - 40)
            .setSize(30, 30)
            .setGroup(playGroup)
            .setImage(parent.loadImage("data/img/stopbutton.png"));

        Toggle pipToggle = cp5.addToggle("pipToggle").setSize(15, 15);
        pipToggle.setCaptionLabel("Inset Zoom")
            .setGroup(playGroup)
            .setPosition(parent.width - 80, parent.height - 20)
            .getCaptionLabel()
            .align(ControlP5Constants.LEFT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        cp5.addTextarea("selectLbl")
            .setPosition(parent.width - 270, parent.height - 40)
            .setGroup(setupGroup)
            .setText("SELECT MODE - ");

        selectToggle = cp5.addToggle("selectToggle").setSize(30, 15);
        selectToggle.setMode(ControlP5Constants.SWITCH)
            .setPosition(parent.width - 80, parent.height - 40)
            .setCaptionLabel(selectMode[1])
            .setGroup(setupGroup)
            .getCaptionLabel()
            .align(ControlP5Constants.LEFT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);
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
        playButton.setImage(playIcons[(state ? 1 : 0)]);
    }

    /**
     * Updates the label of the selection mode switch.
     */
    public void toggleSelectLbl() {
        selectToggle.setCaptionLabel(selectMode[(selectToggle.getState()?0:1)]);
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
}