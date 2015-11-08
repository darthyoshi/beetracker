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
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.ScrollableList;
import controlP5.Slider;
import controlP5.Toggle;
import controlP5.Tooltip;

import processing.core.PImage;

/**
 *
 * @author Kay Choi
 */
class UIControl {
    private final ControlP5 cp5;

    private final Group setupGroup, playGroup, thresholdGroup;
    private final ScrollableList colorList;
    private final Toggle pipToggle;
    private final Button playButton, recordButton;
    private final PImage[] playIcons, recordIcons;
    private final Slider thresholdSlider, seekBar;
    private final Button eventLineButton;
    private final RadioButton thresholdRadios, selectRadios, modeRadios;
 //   private final Tooltip toolTip;
    private final Button statusLabel;
    private final Button[] openButtons;

    private static final java.awt.MenuBar mbar;
    private static final Menu loadMenu, optionMenu;
    private static final MenuItem loadVideo, loadImages;
    private static final MenuItem closeItem;
    private static final MenuItem exitItem;
    private static final MenuItem addSettingItem, removeSettingItem;
    private static final CheckboxMenuItem exitEventItem, waggleEventItem;
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

        final Menu eventMenu = new Menu("Events");
        exitEventItem = new CheckboxMenuItem("Arrivals/Departures", true);
        waggleEventItem = new CheckboxMenuItem("Waggle Dances");

        eventMenu.add(exitEventItem);
        eventMenu.add(waggleEventItem);

        optionMenu.add(eventMenu);
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
     * @param font the program font
     */
    UIControl(BeeTracker parent, processing.core.PFont font) {
        cp5 = new ControlP5(parent);
        cp5.setFont(font);

        cp5.disableShortcuts();
        cp5.setAutoDraw(false);
//TODO tooltips broken in ControlP5 2.2.5
/*
        toolTip = cp5.getTooltip().setPositionOffset(0f, -15f).setAlpha(0);
        toolTip.setDelay(100)
            .getLabel()
            .setFont(cp5.getFont())
            .setColorBackground(0xffffffff)
            .getStyle()
            .setPadding(5, 5, 5, 5);
*/
        setupGroup = cp5.addGroup("setup").setLabel("").close();
        playGroup = cp5.addGroup("playback").setLabel("").close();
        thresholdGroup = cp5.addGroup("threshold").setLabel("").close();

        Button editColor = cp5.addButton("editColor").setSize(90, 20);
        editColor.setPosition(parent.width - 265, 27)
            .setCaptionLabel("Edit color")
            .setGroup(setupGroup)
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        Button removeColor = cp5.addButton("removeColor").setSize(120, 19);
        removeColor.setPosition(
                editColor.getPosition()[0] + 95,
                editColor.getPosition()[1]
            ).setCaptionLabel("Remove color")
            .setGroup(setupGroup)
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        colorList = cp5.addScrollableList("colorList").setSize(215, 560);
        colorList.setPosition(
                editColor.getPosition()[0],
                editColor.getPosition()[1] - editColor.getHeight() - 3
            ).setCaptionLabel(listLbl)
            .setType(ControlP5Constants.DROPDOWN)
            .setGroup(setupGroup)
            .setBarHeight(20)
            .setItemHeight(20)
            .setOpen(false)
            .addItem(listLbl, -1);

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
                openButtons[0].getPosition()[0],
                openButtons[0].getPosition()[1] + 60
            ).setCaptionLabel("Load images")
            .getCaptionLabel()
            .alignX(ControlP5Constants.CENTER);

        recordIcons = new PImage[2];
        recordIcons[0] = parent.loadImage("img/recordbutton1.png");
        recordIcons[1] = parent.requestImage("img/recordbutton2.png");
        recordButton = cp5.addButton("recordButton")
            .setLabelVisible(false)
            .setPosition(50, parent.height - 40)
            .setSize(30, 30)
            .setGroup(playGroup)
            .setImage(recordIcons[0]);
  //      toolTip.register(recordButton, recordTips[0]);

        playIcons = new PImage[2];
        playIcons[0] = parent.loadImage("img/playbutton.png");
        playIcons[1] = parent.requestImage("img/pausebutton.png");
        playButton = cp5.addButton("playButton")
            .setLabelVisible(false)
            .setPosition(
                recordButton.getPosition()[0] + 40,
                recordButton.getPosition()[1]
            ).setSize(30, 30)
            .setGroup(playGroup)
            .setImage(playIcons[0]);
 //       toolTip.register(playButton, playTips[0]);

        cp5.addButton("ejectButton")
            .setLabelVisible(false)
            .setPosition(
                playButton.getPosition()[0] + 40,
                playButton.getPosition()[1]
            ).setSize(30, 30)
            .setGroup(playGroup)
            .setImage(parent.loadImage("img/ejectbutton.png"));

        seekBar = cp5.addSlider("seek").setBroadcast(false)
            .setSize(
                (int)(parent.width - 50 - (playButton.getPosition()[0] + 120)),
                15
            ).setPosition(
                playButton.getPosition()[0] + 120,
                playButton.getPosition()[1]
            ).setGroup(playGroup)
            .showTickMarks(true)
            .setSliderMode(Slider.FLEXIBLE)
            .setBroadcast(true);
        seekBar.setCaptionLabel("");

        Button removeSetting = cp5.addButton("removeSetting")
            .setCaptionLabel("Del")
            .setSize(50, 18)
            .setPosition(
                seekBar.getPosition()[0] + seekBar.getWidth() - 50,
                seekBar.getPosition()[1] + 17
            ).setGroup(setupGroup);
        removeSetting.getCaptionLabel()
            .align(ControlP5Constants.CENTER, ControlP5Constants.CENTER);
//        toolTip.register(removeSetting, "Remove the current threshold and selection settings");

        cp5.addButton("addSetting")
            .setCaptionLabel("Add")
            .setSize(50, 18)
            .setPosition(
                removeSetting.getPosition()[0] - 55,
                removeSetting.getPosition()[1]
            )
            .setGroup(setupGroup)
            .getCaptionLabel()
            .align(ControlP5Constants.CENTER, ControlP5Constants.CENTER);
  //      toolTip.register("addSetting", "Add new threshold and selection settings to this point");

        cp5.addTextlabel("settingsLabel")
            .setPosition(
                removeSetting.getPosition()[0] - 128,
                removeSetting.getPosition()[1]
            ).setGroup(setupGroup)
            .setText("SETTINGS:");

        eventLineButton = cp5.addButton("eventsButton")
            .setSize(150, 30)
            .setPosition(parent.width - 200, 10)
            .setVisible(false)
            .lock()
            .setCaptionLabel("Show Event Timeline");
        eventLineButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);

        Toggle normalMode = cp5.addToggle("N")
            .setBroadcast(false)
            .toggle()
            .setCaptionLabel("Event");
        normalMode.getCaptionLabel().setPaddingX(-5);
//        toolTip.register(normalMode, "Track Arrivals/Departures");

        Toggle waggleMode = cp5.addToggle("W")
            .setBroadcast(false)
            .setLabelVisible(false);
     //   toolTip.register(waggleMode, "Track Waggle Dances");

        modeRadios = cp5.addRadioButton("modeRadios")
            .setPosition(8, (parent.height - 180)/2)
            .setItemsPerRow(2)
            .addItem(normalMode, 0)
            .addItem(waggleMode, 1)
            .setSize(15, 15)
            .setSpacingColumn(0)
            .setNoneSelectedAllowed(false)
            .setGroup(setupGroup);

        Toggle selectFrame = cp5.addToggle("F")
            .setBroadcast(false)
            .toggle()
            .setCaptionLabel("Select");
        selectFrame.getCaptionLabel().setPaddingX(-8);
   //     toolTip.register(selectFrame, "Inset Frame");

        Toggle selectExit = cp5.addToggle("Ex")
            .setBroadcast(false)
            .setLabelVisible(false);
    //    toolTip.register(selectExit, "Exit Circle");

        selectRadios = cp5.addRadioButton("selectRadios")
            .setPosition(
                modeRadios.getPosition()[0],
                modeRadios.getPosition()[1] + 60
            ).setItemsPerRow(2)
            .addItem(selectFrame, 0)
            .addItem(selectExit, 1)
            .setSize(15, 15)
            .setSpacingColumn(0)
            .setNoneSelectedAllowed(false)
            .setGroup(setupGroup);

        pipToggle = cp5.addToggle("pipToggle")
            .setSize(15, 15)
            .setCaptionLabel("Zoom")
            .setGroup(playGroup)
            .setPosition(
                15,
                selectRadios.getPosition()[1] + 60
            );
        pipToggle.getCaptionLabel().setPaddingX(-11);

        thresholdSlider = cp5.addSlider("thresholdSlider")
            .setBroadcast(false)
            .setSize(15, 255)
            .setRange(0, 255)
            .changeValue(40f)
            .setPosition(
                parent.width - 43,
                (parent.height - 307)/2
            ).setCaptionLabel("")
            .setGroup(thresholdGroup)
            .setBroadcast(true);
        valueLabelToInt(thresholdSlider);

        Toggle hue = cp5.addToggle("H").setBroadcast(false).toggle();
        hue.getCaptionLabel()
            .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);
  //      toolTip.register(hue, "Set hue tolerance threshold");

        Toggle sat = cp5.addToggle("S").setBroadcast(false);
        sat.getCaptionLabel()
            .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);
  //      toolTip.register(sat, "Set minimum saturation threshold");

        Toggle val = cp5.addToggle("V").setBroadcast(false);
        val.getCaptionLabel()
            .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
            .setPaddingX(5);
  //      toolTip.register(val, "Set minimum value threshold");

        thresholdRadios = cp5.addRadioButton("thresholdRadios")
            .setPosition(
                thresholdSlider.getPosition()[0],
                thresholdSlider.getPosition()[1] + 260
            )
            .setItemsPerRow(1)
            .addItem(hue, 0)
            .addItem(sat, 1)
            .addItem(val, 2)
            .setSize(15, 15)
            .setNoneSelectedAllowed(false)
            .setGroup(thresholdGroup);
    //    toolTip.register(thresholdSlider, "Adjust the selected threshold");

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
//TODO use ScrollableList to create menu
//        java.awt.Frame frame = ((processing.awt.PSurfaceAWT.SmoothCanvas)parent.getSurface().getNative()).getFrame();
//        frame.setMenuBar(mbar);
    }

    /**
     * Initializes the menu bar item listeners.
     */
    void initListeners(final BeeTracker parent) {
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

        ItemListener listener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                String lbl = ((String)arg0.getItem());

                if(BeeTracker.debug) {
                    BeeTracker.println(lbl);
                }

                modeRadios.getItem(
                    lbl.equals(waggleEventItem.getLabel()) ?
                    1 :
                    0
                ).toggle();
            }
        };
        exitEventItem.addItemListener(listener);
        waggleEventItem.addItemListener(listener);
    }

    /**
     * Toggles the visibility of the playback controls.
     * @param visible the visibility state
     */
    void setPlayVisibility(boolean visible) {
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
    void addListItem(String label) {
        colorList.addItem(label, colorList.getItems().size()-1);
    }

    /**
     * Removes an item from the color selection list.
     * @param lbl the label of the item to remove
     */
    void removeListItem(String lbl) {
        colorList.removeItem(lbl);

        colorList.setCaptionLabel(listLbl);
    }

    /**
     * Sets the label of a color selection list item.
     * @param newLbl the new label of the item
     * @param index the index of the item
     */
    void setListItem(String newLbl, int index) {
        colorList.getItem(index+1).put("name", newLbl);
        colorList.setCaptionLabel(newLbl);
    }

    /**
     * Sets the state of the play/pause button.
     * @param state the new play state
     */
    void setPlayState(boolean state) {
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
//        toolTip.register(playButton, playTips[j]);

        playItem.setLabel((state ? "Pause" : "Play"));

        updateEventButtonVisibility();
    }

    /**
     * Toggles the visibility of the setup mode components.
     * @param visible the visibility state
     */
    void setSetupGroupVisibility(boolean visible) {
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
    void setOpenButtonVisibility(boolean visible) {
        for(Button button : openButtons) {
            button.setVisible(visible).setBroadcast(visible).setLock(!visible);
        }
    }

    /**
     * Toggles the visibility of the "threshold" slider.
     * @param visible the visibility state
     */
    void setThresholdVisibility(boolean visible) {
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
    void setThresholdValue(int val) {
        thresholdSlider.changeValue(val);
        thresholdValueLabelToInt();
    }

    /**
     * Retrieves the currently selected threshold.
     * @return the current radio button integer value:
     *   0 = hue
     *   1 = saturation
     *   2 = luminosity
     */
    int getThresholdType() {
        return (int)thresholdRadios.getValue();
    }

    /**
     * Changes the value of the seek bar.
     * @param time the new time in seconds
     * @param imgSequenceMode whether the footage is a sequence of images or a
     *   video
     */
    void setSeekTime(float time, boolean imgSequenceMode) {
        seekBar.changeValue(time);
        formatSeekLabel(imgSequenceMode);
    }

    /**
     * Sets the precision of a controller value label from <i>float</i> to
     *   <i>int</i>.
     * @param controller the controller to change
     */
    static void valueLabelToInt(controlP5.Controller<?> controller) {
        controller.setValueLabel(Integer.toString((int)controller.getValue()));
    }

    /**
     * Sets the threshold slider value label precision from <i>float</i> to
     *   <i>int</i>.
     */
    void thresholdValueLabelToInt() {
        valueLabelToInt(thresholdSlider);
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
            seekBar.setValueLabel((tmp+1) + " of " + ((int)seekBar.getMax()+1))
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
    float getSeekTime() {
        return seekBar.getValue();
    }

    /**
     * Sets the seek bar range.
     * @param duration the video duration
     * @param imgSequenceMode whether the footage is a sequence of images or a
     *   video
     */
    void setSeekRange(float duration, boolean imgSequenceMode) {
        seekBar.setBroadcast(false)
            .setRange(0f, duration)
            .setBroadcast(true);
        formatSeekLabel(imgSequenceMode);
    }

    /**
     * Sets the state of the record button.
     * @param state the new recording state
     */
    void setRecordState(boolean state) {
        isRecord = state;
        int index = (state ? 1 : 0);
        recordButton.setImage(recordIcons[index]);
        recordItem.setState(state);
  //      toolTip.register(recordButton, recordTips[index]);

        updateEventButtonVisibility();

        if(!isPlaying) {
 //          toolTip.register(playButton, playTips[index]);
        }
    }

    /**
     * @return the current values of the threshold slider
     */
    float[] getRangeValues() {
        return thresholdSlider.getArrayValue();
    }

    /**
     * Forces activation of a threshold radio button.
     * @param val the value of the radio button to activate
     */
    void setThresholdType(int val) {
        thresholdRadios.activate(val);
    }

    /**
     * Toggles the visibility of the record button.
     * @param state the new visibility state
     */
    void setRecordVisibility(boolean state) {
        recordButton.setVisible(state);
    }

    /**
     * @return the position of the seekbar in pixels
     */
    float[] getSeekBarPosition() {
       return seekBar.getPosition();
    }

    /**
     * @return the width of the seekbar in pixels
     */
    int getSeekBarWidth() {
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
    void setStatusLabel(int type) {
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
    void setZoomState(boolean state) {
        pipToggle.setBroadcast(false).setState(state).setBroadcast(true);
        zoomItem.setState(state);
    }

    /**
     * Toggles the activation states of the "Footage" and "Options" menu items.
     */
    void toggleMenuStates() {
        boolean state = loadMenu.isEnabled();
        loadMenu.setEnabled(!state);
        closeItem.setEnabled(state);
        playItem.setEnabled(state);
        optionMenu.setEnabled(state);
    }

    /**
     * Draws the ControlP5 elements.
     */
    void draw() {
        cp5.draw();
    }

    /**
     * Manually activates an event detection type radio button.
     * @param type true for waggle dance detection
     */
    void activateEventRadio(boolean type) {
        modeRadios.activate(type ? 1 : 0);
    }

    /**
     * Updates UI elements based on the event detection type.
     * @param type true for waggle dance detection
     */
    void updateEventType(boolean type) {
        waggleEventItem.setState(type);
        exitEventItem.setState(!type);

        if(type) {
            selectRadios.activate(0);
        }

        selectRadios.getItem(0).setLock(type);
        selectRadios.getItem(1).setVisible(!type);
    }
}
