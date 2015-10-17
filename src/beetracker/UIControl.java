/**
 * @file UIControl.java
 * @author Kay Choi, 909926828
 * @date 14 Feb 15
 * @description Manages the UI elements for BeeTracker.
 */

package beetracker;

import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import controlP5.Button;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;
import controlP5.DropdownList;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Slider;
import controlP5.Toggle;
import controlP5.Tooltip;

import processing.core.PImage;

/**
 *
 * @author Kay Choi
 */
public class UIControl {
    private final Group setupGroup, playGroup, thresholdGroup;
    private final DropdownList colorList;
    private final Toggle selectToggle, pipToggle;
    private final Button playButton, recordButton;
    private final PImage[] playIcons, recordIcons;
    private final Slider thresholdSlider, seekBar;
    private final Button eventLineButton;
    private final RadioButton radioButtons;
    private final Tooltip toolTip;
    private final Button statusLabel;
    private final Button[] openButtons;

    private static final java.awt.MenuBar mbar;
    private static final Menu loadMenu, optionMenu;
    private static final MenuItem loadVideo, loadImages;
    private static final MenuItem closeItem;
    private static final MenuItem exitItem;
    private static final MenuItem addSettingItem, removeSettingItem;
    private static final MenuItem playItem;
    private static final CheckboxMenuItem recordItem, zoomItem;
    static {
        mbar = new java.awt.MenuBar();

        final Menu programMenu = new Menu("Program");

        exitItem = new MenuItem("Exit");

        programMenu.add(exitItem);

        final Menu footageMenu = new Menu("Footage");

        loadMenu = new Menu("Load...");
        loadVideo = new MenuItem("Video");
        loadImages = new MenuItem("Image Sequence");

        loadMenu.add(loadVideo);
        loadMenu.add(loadImages);

        footageMenu.add(loadMenu);
        footageMenu.addSeparator();

        playItem = new MenuItem("Play");
        playItem.setEnabled(false);

        footageMenu.add(playItem);

        closeItem = new MenuItem("Close");
        closeItem.setEnabled(false);

        footageMenu.addSeparator();
        footageMenu.add(closeItem);

        optionMenu = new Menu("Options");
        optionMenu.setEnabled(false);
        recordItem = new CheckboxMenuItem("Record");
        zoomItem = new CheckboxMenuItem("Zoom");

        optionMenu.add(recordItem);
        optionMenu.add(zoomItem);
        optionMenu.addSeparator();

        final Menu settingsMenu = new Menu("Settings");
        addSettingItem = new MenuItem("Add to current timestamp");
        removeSettingItem = new MenuItem("Remove current settings");

        settingsMenu.add(addSettingItem);
        settingsMenu.add(removeSettingItem);

        optionMenu.add(settingsMenu);

        mbar.add(programMenu);
        mbar.add(footageMenu);
        mbar.add(optionMenu);
    }

    private static final String listLbl = "New color";
    private static final String[] selectMode = {"Inset Frame", "Hive Exit"};
    private static final String[] recordTips = {"Enable tracking", "Disable tracking"};
    private static final String[] playTips = {
        "Begin playback without tracking",
        "Begin playback with tracking",
        "Pause playback"
    };
    private static final String[] modes = {
        "Config Mode",
        "Replay Mode",
        "Annotation Mode",
        "Playback Mode"
    };

    private boolean isPlaying = false, isRecord = false;

    /**
     * Class constructor.
     * @param parent the instantiating object
     * @param cp5 the ControlP5 object
     */
    public UIControl(final BeeTracker parent, ControlP5 cp5) {
        cp5.disableShortcuts();
        cp5.setAutoDraw(false);

        toolTip = cp5.getTooltip().setPositionOffset(0f, -15f).setAlpha(0);
        toolTip.setDelay(100)
            .getLabel()
            .setFont(cp5.getFont())
            .setColorBackground(0xffffffff)
            .getStyle()
            .setPadding(5, 5, 5, 5);

        setupGroup = cp5.addGroup("setup").setLabel("").close();
        playGroup = cp5.addGroup("playback").setLabel("").close();
        thresholdGroup = cp5.addGroup("threshold").setLabel("").close();

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

        openButtons = new Button[2];
        openButtons[0] = cp5.addButton("openButton").setSize(120, 50);
        openButtons[0].setPosition(
                (parent.width - openButtons[0].getWidth())*.5f,
                parent.height/2 + 100
            ).setCaptionLabel("Load video")
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        openButtons[1] = cp5.addButton("openButton2").setSize(120, 50);
        openButtons[1].setPosition(
                openButtons[0].getPosition().x,
                openButtons[0].getPosition().y + 60
            ).setCaptionLabel("Load images")
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
            .showTickMarks(true)
            .setSliderMode(Slider.FLEXIBLE)
            .setBroadcast(true);
        seekBar.setCaptionLabel("");

        eventLineButton = cp5.addButton("eventsButton")
            .setSize(150, 30)
            .setPosition(parent.width - 200, 10)
            .setVisible(false)
            .lock()
            .setCaptionLabel("Show Event Timeline");
        eventLineButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);

        pipToggle = cp5.addToggle("pipToggle").setSize(15, 15);
        pipToggle.setCaptionLabel("Inset Zoom")
            .setGroup(playGroup)
            .setPosition(parent.width - 82, parent.height - 20)
            .getCaptionLabel()
            .align(ControlP5Constants.LEFT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        selectToggle = cp5.addToggle("selectToggle").setSize(32, 15);
        selectToggle.setMode(ControlP5Constants.SWITCH)
            .setBroadcast(false)
            .setState(true)
            .setBroadcast(true)
            .setPosition(parent.width - 82, parent.height - 40)
            .setCaptionLabel(selectMode[0])
            .setGroup(setupGroup)
            .getCaptionLabel()
            .align(ControlP5Constants.LEFT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        toolTip.register(selectToggle, "Selection type");

        Toggle hue = cp5.addToggle("Hue").setBroadcast(false).toggle();
        hue.getCaptionLabel()
            .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        toolTip.register(hue, "Set hue tolerance threshold");

        Toggle sat = cp5.addToggle("Sat").setBroadcast(false);
        sat.getCaptionLabel()
            .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        toolTip.register(sat, "Set minimum saturation threshold");

        Toggle val = cp5.addToggle("Val").setBroadcast(false);
        val.getCaptionLabel()
            .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);

        toolTip.register(val, "Set minimum value threshold");

        radioButtons = cp5.addRadioButton("radioButtons")
            .setPosition(355, 27)
            .setItemsPerRow(3)
            .setSpacingColumn(40)
            .addItem(hue, 0)
            .addItem(sat, 1)
            .addItem(val, 2)
            .setSize(15, 15)
            .setNoneSelectedAllowed(false)
            .setGroup(thresholdGroup);

        thresholdSlider = cp5.addSlider("thresholdSlider").setBroadcast(false);
        thresholdSlider.setSize(255, 15)
            .setRange(0, 255)
            .setValue(40f)
            .setPosition(
                radioButtons.getPosition().x - 100,
                radioButtons.getPosition().y - 20
            ).setCaptionLabel("Threshold:")
            .setGroup(thresholdGroup)
            .setBroadcast(true)
            .getCaptionLabel()
            .align(ControlP5Constants.LEFT, ControlP5Constants.BOTTOM_OUTSIDE)
            .setPadding(0, 6);

        toolTip.register(thresholdSlider, "Adjust the selected threshold");

        Button removeSetting = cp5.addButton("removeSetting")
            .setCaptionLabel("Del")
            .setSize(50, 18)
            .setPosition(
                seekBar.getPosition().x + seekBar.getWidth() - 50,
                seekBar.getPosition().y + 17
            ).setGroup(setupGroup);
        removeSetting.getCaptionLabel()
            .align(ControlP5Constants.CENTER, ControlP5Constants.CENTER);

        toolTip.register(removeSetting, "Remove the current threshold and selection settings");

        cp5.addButton("addSetting")
            .setCaptionLabel("Add")
            .setSize(50, 18)
            .setPosition(
                removeSetting.getPosition().x - 55,
                removeSetting.getPosition().y
            )
            .setGroup(setupGroup)
            .getCaptionLabel()
            .align(ControlP5Constants.CENTER, ControlP5Constants.CENTER);

        toolTip.register("addSetting", "Add new threshold and selection settings to this point");

        cp5.addTextlabel("settingsLabel")
            .setPosition(
                removeSetting.getPosition().x - 130,
                removeSetting.getPosition().y + 2
            ).setGroup(setupGroup)
            .setText("SETTINGS:");

        statusLabel = cp5.addButton("status")
           .lock()
           .setSize(190, 40)
           .setPosition(50, 5)
           .setGroup(playGroup);
        statusLabel.getCaptionLabel()
           .setFont(new controlP5.ControlFont(cp5.getFont().getFont(), 24))
           .toUpperCase(false)
           .alignX(ControlP5Constants.CENTER)
           .set(modes[0]);

        if(parent.frame != null) {
            parent.frame.setMenuBar(mbar);

            loadVideo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    parent.openButton();
                }
            });
            loadImages.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    parent.openButton2();
                }
            });
            closeItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    parent.ejectButton();
                }
            });
            exitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    parent.exit();
                }
            });
            recordItem.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent arg0) {
                    parent.recordButton();
                }
            });
            zoomItem.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent arg0) {
                    parent.pipToggle();
                }
            });
            playItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    parent.playButton();
                }
            });
            addSettingItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    parent.addSetting();
                }
            });
            removeSettingItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    parent.removeSetting();
                }
            });
        }
    }

    /**
     * Toggles the visibility of the playback controls.
     * @param visible the visibility state
     */
    public void setPlayVisibility(boolean visible) {
        if(visible) {
            playGroup.open();
        }
        else {
            playGroup.close();
        }
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

        int i, j;
        if(state) {
            i = 1;
            j = 2;
        }
        else {
            i = 0;
            j = isRecord || !recordButton.isVisible() ? 1 : 0;
        }

        playButton.setImage(playIcons[i]);
        toolTip.register(playButton, playTips[j]);

        playItem.setLabel((state ? "Pause" : "Play"));

        updateEventButtonVisibility();
    }

    /**
     * Updates the label of the selection mode switch.
     */
    public void toggleSelectLbl() {
        selectToggle.setCaptionLabel(selectMode[(selectToggle.getState() ? 0 : 1)]);
    }

    /**
     * Toggles the visibility of the setup mode components.
     * @param visible the visibility state
     */
    public void setSetupGroupVisibility(boolean visible) {
        if(visible) {
            setupGroup.open();
        }
        else {
            setupGroup.close();
        }
    }

    /**
     * Toggles the visibility of the title screen buttons.
     * @param visible the visibility state
     */
    public void setOpenButtonVisibility(boolean visible) {
        for(Button button : openButtons) {        
            button.setVisible(visible).setBroadcast(visible).setLock(!visible);
        }
    }

    /**
     * Toggles the visibility of the "threshold" slider.
     * @param visible the visibility state
     */
    public void setThresholdVisibility(boolean visible) {
        if(visible) {
            thresholdGroup.open();
        }
        else {
            thresholdGroup.close();
        }
    }

    /**
     * Updates the slider values.
     * @param val the new slider value
     */
    public void setThresholdValue(int val) {
        thresholdSlider.setBroadcast(false).setValue(val).setBroadcast(true);
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
     * Changes the value of the seek bar.
     * @param time the new time in seconds
     * @param imgSequenceMode whether the footage is a sequence of images or a
     *   video
     */
    public void setSeekTime(float time, boolean imgSequenceMode) {
        seekBar.changeValue(time);
        formatSeekLabel(imgSequenceMode);
    }

    /**
     * Changes the seek bar label format.
     * @param imgSequenceMode whether the footage is a sequence of images or a
     *   video
     */
    private void formatSeekLabel(boolean imgSequenceMode) {
        int tmp;

        if(imgSequenceMode) {
            tmp = (int)seekBar.getValue();
            seekBar.setValueLabel(tmp + " of " + (int)seekBar.getMax())
                .getValueLabel()
                .align(ControlP5Constants.LEFT, ControlP5Constants.BOTTOM_OUTSIDE)
                .setPaddingX(0)
                .setPaddingY(5);
        }

        else {
            tmp = (int)(seekBar.getValue()*100);

            seekBar.setValueLabel(String.format(
                    "%02d:%02d:%02d.%02d",
                    (tmp/6000)/60,
                    tmp/6000,
                    (tmp/100)%60,
                    tmp%100
                )).getValueLabel()
                .align(ControlP5Constants.LEFT, ControlP5Constants.BOTTOM_OUTSIDE)
                .setPaddingX(0)
                .setPaddingY(5);
        }
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
     * @param imgSequenceMode whether the footage is a sequence of images or a
     *   video
     */
    public void setSeekRange(float duration, boolean imgSequenceMode) {
        seekBar.setBroadcast(false)
            .setRange(0f, duration)
            .setBroadcast(true);
        formatSeekLabel(imgSequenceMode);
    }

    /**
     * Sets the state of the record button.
     * @param state the new recording state
     */
    public void setRecordState(boolean state) {
        isRecord = state;
        int index = (state ? 1 : 0);
        recordButton.setImage(recordIcons[index]);
        recordItem.setState(state);
        toolTip.register(recordButton, recordTips[index]);

        updateEventButtonVisibility();

        if(!isPlaying) {
            toolTip.register(playButton, playTips[index]);
        }
    }

    /**
     * @return the current values of the threshold slider
     */
    public float[] getRangeValues() {
        return thresholdSlider.getArrayValue();
    }

    /**
     * Forces activation of a radio button.
     * @param val the value of the radio button to activate
     */
    public void selectRadioButton(int val) {
        radioButtons.activate(0);
    }

    /**
     * Toggles the visibility of the record button.
     * @param state the new visibility state
     */
    public void setRecordVisibility(boolean state) {
        recordButton.setVisible(state);
    }

    /**
     * @return the position of the seekbar in pixels
     */
    public processing.core.PVector getSeekBarPosition() {
       return seekBar.getPosition();
    }

    /**
     * @return the width of the seekbar in pixels
     */
    public int getSeekBarWidth() {
       return seekBar.getWidth();
    }

    /**
     * Changes the status box text.
     * @param type the mode flag
     *   0 - config
     *   1 - replay
     *   2 - record
     *   3 - playback
     */
    public void setStatusLabel(int type) {
       statusLabel.setCaptionLabel(modes[type]);
    }

    /**
     * Updates the visibility of the event timeline button.
     */
    private void updateEventButtonVisibility() {
        boolean tmp = isPlaying && isRecord;
        eventLineButton.setVisible(tmp).setLock(!tmp);
    }
    
    /**
     * Sets the state of the zoom toggle.
     * @param state the new zoom state
     */
    public void setZoomState(boolean state) {
        pipToggle.setBroadcast(false).setState(state).setBroadcast(true);
        zoomItem.setState(state);
    }

    /**
     * Toggles the activation states of the "Footage" and "Options" menu items.
     */
    public void toggleMenuStates() {
        boolean state = loadMenu.isEnabled();
        loadMenu.setEnabled(!state);
        closeItem.setEnabled(state);
        playItem.setEnabled(state);
        optionMenu.setEnabled(state);
    }
}
