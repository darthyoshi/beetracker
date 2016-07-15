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

import controlP5.Button;
import controlP5.CColor;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;
import controlP5.ControllerInterface;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.ScrollableList;
import controlP5.Slider;
import controlP5.Textlabel;
import controlP5.Toggle;
//import controlP5.Tooltip;

import processing.core.PImage;

/**
 * @class UIControl
 * @author Kay Choi
 * @date 14 Jul 16
 * @description Manages the UI elements for BeeTracker.
 */
class UIControl {
  private final ControlP5 cp5;

  private final Group setupGroup, playGroup, thresholdGroup;
  private final ScrollableList colorList;
  private final Toggle[] allToggles;
  private final Toggle pipToggle;
  private final Button playButton, recordButton;
  private final PImage[] playIcons, recordIcons;
  private final PImage checkmark;
  private final Slider thresholdSlider, seekBar;
  private final Button eventLineButton;
  private final RadioButton thresholdRadios, selectRadios, modeRadios;
 // private final Tooltip toolTip;
  private final Button statusLabel;
//  private final Button[] openButtons;
  private final Textlabel modeLabel, selectLabel;

  private final processing.core.PGraphics buf;

  private int color = 0;
  private final float[] colorBoxPos;

  private final ScrollableList programMenu, footageMenu, optionMenu;
  private final Button programButton, footageButton, optionButton;
  private static final CColor menuColor = new CColor(
    0xffcccccc,
    0xffeeeeee,
    0xffcccccc,
    0,
    0
  );
  private static final CColor disabledMenuColor = new CColor(
    0xffcccccc,
    0xffaaaaaa,
    0xffcccccc,
    0,
    0
  );
  private static final String menuSeparator = "------";
  private static final String[] playLabels = {"* Play", "  Play"};
  private static final String[] recordLabels = {"* Record", "  Record"};
  private static final String[] zoomLabels = {"* Zoom", "  Zoom"};
  private static final String[] exitEventLabels = {
    "* Arrival/Departure Events",
    "  Arrival/Departure Events"
  };
  private static final String[] waggleEventLabels = {
    "* Waggle Dance Events",
    "  Waggle Dance Events"
  };
  private static final String[] insetSelectLabels = {
    "* Select Inset Frame",
    "  Select Inset Frame"
  };
  private static final String[] exitSelectLabels = {
    "* Select Exit Circle",
    "  Select Exit Circle"
  };

  static final String listLbl = "New color";
/*  private static final String[] recordTips = {"Enable tracking", "Disable tracking"};
  private static final String[] playTips = {
    "Begin playback without tracking",
    "Begin playback with tracking",
    "Pause playback"
  };*/
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
   */
  UIControl(BeeTracker parent) {
    cp5 = new ControlP5(parent);
    cp5.setFont(parent.font);

    cp5.setBroadcast(false);

    cp5.disableShortcuts();
    cp5.setAutoDraw(false);
/*  TODO tooltips broken in ControlP5 2.2.5
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
    editColor.setPosition(
        BeeTracker.viewBounds[2] - 214,
        BeeTracker.viewBounds[1] - 24
      ).setCaptionLabel("Edit color")
      .setGroup(setupGroup)
      .getCaptionLabel()
      .alignX(ControlP5Constants.CENTER);

    float[] editColorPos = editColor.getPosition();
    Button removeColor = cp5.addButton("removeColor").setSize(120, 20);
    removeColor.setPosition(editColorPos[0] + 95, editColorPos[1])
      .setCaptionLabel("Remove color")
      .setGroup(setupGroup)
      .getCaptionLabel()
      .alignX(ControlP5Constants.CENTER);

    colorList = cp5.addScrollableList("colorList").setSize(215, 560);
    colorList.setPosition(editColorPos[0], editColorPos[1] - editColor.getHeight() - 3)
      .setCaptionLabel(listLbl)
      .setType(ControlP5Constants.DROPDOWN)
      .setGroup(setupGroup)
      .setBarHeight(20)
      .setItemHeight(20)
      .setOpen(false)
      .addItem(listLbl, -1);

    colorBoxPos = new float[]{editColorPos[0] - 45, BeeTracker.viewBounds[1] - 45};
/*
    openButtons = new Button[2];
    openButtons[0] = cp5.addButton("openButton").setSize(120, 50);
    openButtons[0].setPosition(
        (BeeTracker.viewBounds[0] + BeeTracker.viewBounds[2] - openButtons[0].getWidth())*.5f,
        (BeeTracker.viewBounds[1] + BeeTracker.viewBounds[3])*.5f + 100
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
*/
    recordIcons = new PImage[2];
    recordIcons[0] = parent.loadImage("img/recordbutton1.png");
    recordIcons[1] = parent.requestImage("img/recordbutton2.png");
    recordButton = cp5.addButton("recordButton")
      .setLabelVisible(false)
      .setPosition(BeeTracker.viewBounds[0], BeeTracker.viewBounds[3] + 11)
      .setSize(30, 30)
      .setGroup(playGroup)
      .setImage(recordIcons[0]);
  //    toolTip.register(recordButton, recordTips[0]);

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
 //     toolTip.register(playButton, playTips[0]);

    cp5.addButton("ejectButton")
      .setLabelVisible(false)
      .setPosition(
        playButton.getPosition()[0] + 40,
        playButton.getPosition()[1]
      ).setSize(30, 30)
      .setGroup(playGroup)
      .setImage(parent.loadImage("img/ejectbutton.png"));

    seekBar = cp5.addSlider("seek")
      .setSize(
        BeeTracker.viewBounds[2] - (int)playButton.getPosition()[0] - 119,
        15
      ).setPosition(
        playButton.getPosition()[0] + 120,
        playButton.getPosition()[1]
      ).setGroup(playGroup)
      .showTickMarks(true)
      .setSliderMode(Slider.FLEXIBLE);
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
//    toolTip.register(removeSetting, "Remove the current threshold and selection settings");

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
  //    toolTip.register("addSetting", "Add new threshold and selection settings to this point");

    cp5.addTextlabel("settingsLabel")
      .setPosition(
        removeSetting.getPosition()[0] - 128,
        removeSetting.getPosition()[1]
      ).setGroup(setupGroup)
      .setValueLabel("SETTINGS:");

    eventLineButton = cp5.addButton("eventsButton")
      .setSize(150, 30)
      .setPosition(BeeTracker.viewBounds[2] - 149, BeeTracker.viewBounds[1] - 40)
      .setVisible(false)
      .setCaptionLabel("Show Event Timeline");
    eventLineButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);

    checkmark = parent.requestImage("img/checkmark.png");

    Toggle normalMode = cp5.addToggle("N")
      .setBroadcast(false)
      .toggle()
      .setCaptionLabel("Exit");
    normalMode.getCaptionLabel()
      .alignX(ControlP5Constants.CENTER)
      .toUpperCase(false);
//    toolTip.register(normalMode, "Track Arrivals/Departures");

    Toggle waggleMode = cp5.addToggle("W")
      .setBroadcast(false)
      .setCaptionLabel("Waggle");
    waggleMode.getCaptionLabel()
      .alignX(ControlP5Constants.CENTER)
      .toUpperCase(false);
//    toolTip.register(waggleMode, "Track Waggle Dances");

    modeRadios = cp5.addRadioButton("modeRadios")
      .setPosition(
        (BeeTracker.viewBounds[0] - 15)*.5f,
        (BeeTracker.viewBounds[1] + BeeTracker.viewBounds[3] - 250)*.5f
      ).setItemsPerRow(1)
      .addItem(normalMode, 0)
      .addItem(waggleMode, 1)
      .setSize(15, 15)
      .setSpacingRow(25)
      .setNoneSelectedAllowed(false)
      .setGroup(setupGroup);

    modeLabel = cp5.addLabel("eventType")
      .setPosition(
        BeeTracker.viewBounds[0] - 125,
        modeRadios.getPosition()[1] - 20
      ).setGroup(setupGroup)
      .setValueLabel("EVENT");
    modeLabel.getValueLabel().alignX(ControlP5.CENTER);

    Toggle selectFrame = cp5.addToggle("F")
      .setBroadcast(false)
      .toggle()
      .setCaptionLabel("Frame");
    selectFrame.getCaptionLabel()
      .alignX(ControlP5Constants.CENTER)
      .toUpperCase(false);
//    toolTip.register(selectFrame, "Inset Frame");

    Toggle selectExit = cp5.addToggle("Ex")
      .setBroadcast(false)
      .setCaptionLabel("Exit");
    selectExit.getCaptionLabel()
      .alignX(ControlP5Constants.CENTER)
      .toUpperCase(false);
//    toolTip.register(selectExit, "Exit Circle");

    selectRadios = cp5.addRadioButton("selectRadios")
      .setPosition(
        modeRadios.getPosition()[0],
        modeRadios.getPosition()[1] + 130
      ).setItemsPerRow(1)
      .addItem(selectFrame, 0)
      .addItem(selectExit, 1)
      .setSize(15, 15)
      .setSpacingRow(25)
      .setNoneSelectedAllowed(false)
      .setGroup(setupGroup);

    selectLabel = cp5.addTextlabel("selectType")
      .setValueLabel("SELECT")
      .setPosition(
        modeLabel.getPosition()[0],
        selectRadios.getPosition()[1] - 20
      ).setGroup(setupGroup);
    selectLabel.getValueLabel().alignX(ControlP5.CENTER).setFixedSize(true).setPaddingX(0).setWidth(50);

    pipToggle = cp5.addToggle("pipToggle")
      .setSize(15, 15)
      .setCaptionLabel("Zoom")
      .setGroup(playGroup)
      .setPosition(
        selectRadios.getPosition()[0],
        selectRadios.getPosition()[1] + 110
      );
    pipToggle.getCaptionLabel().alignX(ControlP5Constants.CENTER);

    thresholdSlider = cp5.addSlider("thresholdSlider")
      .setSize(15, 255)
      .setRange(0, 255)
      .changeValue(40f)
      .setPosition(
        BeeTracker.viewBounds[2] + 9,
        (BeeTracker.viewBounds[1] + BeeTracker.viewBounds[3] - 307)*.5f
      ).setCaptionLabel("")
      .setGroup(thresholdGroup);
    valueLabelToInt(thresholdSlider);

    Toggle hue = cp5.addToggle("H").setBroadcast(false).toggle();
    hue.getCaptionLabel()
      .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
      .setPaddingX(5);
//    toolTip.register(hue, "Set hue tolerance threshold");

    Toggle sat1 = cp5.addToggle("S1").setBroadcast(false);
    sat1.getCaptionLabel()
      .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
      .setPaddingX(5);
//    toolTip.register(sat1, "Set minimum saturation threshold");

    Toggle val1 = cp5.addToggle("V1").setBroadcast(false);
    val1.getCaptionLabel()
      .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
      .setPaddingX(5);
//    toolTip.register(val1, "Set minimum value threshold");

    Toggle sat2 = cp5.addToggle("S2").setBroadcast(false);
    sat2.getCaptionLabel()
      .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
      .setPaddingX(5);
//    toolTip.register(sat2, "Set maximum saturation threshold");

    Toggle val2 = cp5.addToggle("V2").setBroadcast(false);
    val2.getCaptionLabel()
      .align(ControlP5Constants.RIGHT_OUTSIDE, ControlP5Constants.CENTER)
      .setPaddingX(5);
//    toolTip.register(val2, "Set maximum value threshold");

    thresholdRadios = cp5.addRadioButton("thresholdRadios")
      .setPosition(
        thresholdSlider.getPosition()[0],
        thresholdSlider.getPosition()[1] + 261
      )
      .setItemsPerRow(1)
      .addItem(hue, 0)
      .addItem(sat1, 1)
      .addItem(sat2, 2)
      .addItem(val1, 3)
      .addItem(val2, 4)
      .setSize(15, 15)
      .setNoneSelectedAllowed(false)
      .setGroup(thresholdGroup);
//    toolTip.register(thresholdSlider, "Adjust the selected threshold");

    statusLabel = cp5.addButton("status")
      .setLock(true)
      .setSize(190, 40)
      .setPosition(BeeTracker.viewBounds[0], BeeTracker.viewBounds[1] - 45)
      .setGroup(playGroup);
    statusLabel.getCaptionLabel()
      .setFont(new controlP5.ControlFont(cp5.getFont().getFont(), 24))
      .toUpperCase(false)
      .alignX(ControlP5Constants.CENTER)
      .set(modes[0]);

    programButton = cp5.addButton("programMenuButton")
      .setPosition(0,0)
      .setCaptionLabel("Program")
      .setSize(54,20)
      .setColor(menuColor);
    programButton.getCaptionLabel().toUpperCase(false);
    programMenu = cp5.addScrollableList("programMenu")
      .setPosition(programButton.getPosition()[0], 20)
      .setOpen(false)
      .addItem("  Exit", 0)
      .setSize(40, 20)
      .setBarHeight(0)
      .setBarVisible(false)
      .setItemHeight(20)
      .setColor(menuColor);
    programMenu.getValueLabel().toUpperCase(false);

    String[] footageItems = {
      "  Load Video",
      "  Load Image Sequence",
      menuSeparator,
      playLabels[1],
      menuSeparator,
      "  Close"
    };
    footageButton = cp5.addButton("footageMenuButton")
      .setPosition(programButton.getPosition()[0]+programButton.getWidth(),0)
      .setCaptionLabel("Footage")
      .setSize(52,20)
      .setColor(menuColor);
    footageButton.getCaptionLabel().toUpperCase(false);
    footageMenu = cp5.addScrollableList("footageMenu")
      .setPosition(footageButton.getPosition()[0], 20)
      .setOpen(false)
      .addItems(footageItems)
      .setSize(145, footageItems.length*20)
      .setBarHeight(0)
      .setBarVisible(false)
      .setItemHeight(20)
      .setColor(disabledMenuColor);
    footageMenu.getValueLabel().toUpperCase(false);
    footageMenu.getItem(0).put("color", menuColor);
    footageMenu.getItem(1).put("color", menuColor);

    String[] optionItems = {
      recordLabels[1],
      zoomLabels[1],
      menuSeparator,
      exitEventLabels[0],
      waggleEventLabels[1],
      menuSeparator,
      insetSelectLabels[0],
      exitSelectLabels[1],
      menuSeparator,
      "  Add new settings to current timestamp",
      "  Remove current timestamp settings"
    };
    optionButton = cp5.addButton("optionMenuButton")
      .setPosition(footageButton.getPosition()[0]+footageButton.getWidth(),0)
      .setCaptionLabel("Options")
      .setSize(50,20)
      .setColor(menuColor);
    optionButton.getCaptionLabel().toUpperCase(false);
    optionMenu = cp5.addScrollableList("optionMenu")
      .setPosition(optionButton.getPosition()[0], 20)
      .setOpen(false)
      .addItems(optionItems)
      .setSize(225, optionItems.length*20)
      .setBarHeight(0)
      .setBarVisible(false)
      .setItemHeight(20)
      .setColor(disabledMenuColor);
    optionMenu.getValueLabel().toUpperCase(false);

    buf = parent.createGraphics(parent.width, parent.height);
    buf.beginDraw();
    buf.textAlign(BeeTracker.LEFT, BeeTracker.TOP);
    buf.textSize(10);
    buf.noStroke();
    buf.endDraw();

    allToggles = new Toggle[]{
      hue, sat1, val1, sat2, val2,
      selectFrame, selectExit,
      normalMode, waggleMode
    };

    cp5.setBroadcast(true);
  }

  /**
   * Toggles the visibility of the playback controls.
   * @param visible the visibility state
   */
  void setPlayVisibility(boolean visible) {
    if(visible) {
      playGroup.open();
    } else {
      playGroup.close();
    }
  }

  /**
   * Adds a new item to the color selection list.
   * @param label the item label
   */
  void addListItem(String label) {
    colorList.addItem(label, colorList.getItems().size()-1);

    updateColorLabel(label);
  }

  /**
   * Removes an item from the color selection list.
   * @param lbl the label of the item to remove
   */
  void removeListItem(String lbl) {
    colorList.removeItem(lbl);

    updateColorLabel(listLbl);
  }

  /**
   * Sets the label of a color selection list item.
   * @param newLbl the new label of the item
   * @param index the index of the item
   */
  void setListItem(String newLbl, int index) {
    java.util.Map<String, Object> item = colorList.getItem(index+1);

    item.put("name", newLbl);
    item.put("text", newLbl);

    updateColorLabel(newLbl);
  }

  /**
   * Sets the state of the play/pause button.
   * @param state the new play state
   */
  void setPlayState(boolean state) {
    isPlaying = state;

    int i, j, k;
    if(state) {
      i = 1;
//      j = 2;
      k = 0;
    } else {
      i = 0;
//      j = isRecord || !recordButton.isVisible() ? 1 : 0;
      k = 1;
    }

    footageMenu.getItem(3).put("text", playLabels[k]);

    playButton.setImage(playIcons[i]);
//    toolTip.register(playButton, playTips[j]);

    updateTimelineButtonVisibility();
  }

  /**
   * Toggles the visibility of the setup mode components.
   * @param visible the visibility state
   */
  void setSetupGroupVisibility(boolean visible) {
    if(visible) {
      setupGroup.open();
    } else {
      setupGroup.close();
    }
  }

  /**
   * Toggles the visibility of the title screen buttons.
   * @param visible the visibility state
   */
  void setOpenButtonVisibility(boolean visible) {
/*    for(Button button : openButtons) {
      button.setVisible(visible);
    }
*/  }

  /**
   * Toggles the visibility of the "threshold" slider.
   * @param visible the visibility state
   */
  void setThresholdVisibility(boolean visible) {
    if(visible) {
      thresholdGroup.open();
    } else {
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
   */
  void setSeekTime(float time) {
    seekBar.changeValue(time);
    formatSeekLabel();
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
  private void formatSeekLabel() {
    int tmp = (int)(seekBar.getValue()*100);

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
  void setSeekRange(float duration) {
    seekBar.setBroadcast(false)
      .setRange(0f, duration)
      .setBroadcast(true);
    formatSeekLabel();
  }

  /**
   * Sets the state of the record button.
   * @param state the new recording state
   */
  void setRecordState(boolean state) {
    isRecord = state;
    int index = (state ? 1 : 0);
    recordButton.setImage(recordIcons[index]);
//    toolTip.register(recordButton, recordTips[index]);

    updateTimelineButtonVisibility();

    optionMenu.getItem(0).put("text", recordLabels[state ? 0 : 1]);
/*
    if(!isPlaying) {
       toolTip.register(playButton, playTips[index]);
    }
*/  }

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
  private void updateTimelineButtonVisibility() {
    eventLineButton.setVisible(isPlaying && isRecord);
  }

  /**
   * Sets the state of the zoom toggle.
   * @param state the new zoom state
   */
  void setZoomState(boolean state) {
    pipToggle.setBroadcast(false).setState(state).setBroadcast(true);

    optionMenu.getItem(1).put("text", zoomLabels[state ? 0 : 1]);
  }

  /**
   * Toggles the activation states of the "Footage" and "Options" menu items.
   */
  void toggleMenuStates() {
    java.util.Map<String, Object> loadVideoItem = footageMenu.getItem(0);
    CColor loadItemColor, playItemColor;

    if((CColor)loadVideoItem.get("color") == disabledMenuColor) {
      loadItemColor = menuColor;
      playItemColor = disabledMenuColor;
    } else {
      loadItemColor = disabledMenuColor;
      playItemColor = menuColor;
    }

    //footage menu
    loadVideoItem.put("color", loadItemColor);  //"load video" item
    footageMenu.getItem(1).put("color", loadItemColor); //"load image sequence" item
    footageMenu.getItem(3).put("color", playItemColor); //"play" item
    footageMenu.getItem(5).put("color", playItemColor); //"close" item

    //option menu
    optionMenu.getItem(0).put("color", playItemColor);  //"record" item
    optionMenu.getItem(1).put("color", playItemColor);  //"zoom" item
    optionMenu.getItem(3).put("color", playItemColor);  //"exit event" item
    optionMenu.getItem(4).put("color", playItemColor);  //"waggle event" item
    optionMenu.getItem(6).put("color", playItemColor);  //"select inset" item
    optionMenu.getItem(7).put("color", playItemColor);  //"select exit" item
    optionMenu.getItem(9).put("color", playItemColor);  //"add setting" item
    optionMenu.getItem(10).put("color", playItemColor); //"remove setting" item
  }

  /**
   * Draws the ControlP5 elements.
   * @param parent the instantiating BeeTracker
   * @param settingsTimeStamps a FloatList containing the settings time stamps
   */
  void draw(BeeTracker parent, processing.data.FloatList settingsTimeStamps) {
    cp5.draw();

    buf.beginDraw();
    buf.clear();

    float[] pos;

    //mark settings time stamps
    if(settingsTimeStamps != null) {
      buf.fill(0xffffffff);
      pos = seekBar.getPosition();
      for(float stamp : settingsTimeStamps) {
        buf.text(
          "l",
          stamp/seekBar.getMax()*(seekBar.getWidth()-5) + (pos[0]+2),
          pos[1] + 5
        );
      }
    }

    float[] parentPos;
    if(setupGroup.isOpen()) {
      //draw checkbox overlays
      ControllerInterface<?> parentController;
      for(Toggle toggle : allToggles) {
        parentController = toggle.getParent();
        if(parentController.isVisible() && toggle.getState()) {
          pos = toggle.getPosition();
          parentPos = parentController.getPosition();

          buf.copy(
            checkmark,
            0, 0,
            checkmark.width, checkmark.height,
            (int)(pos[0]+parentPos[0]), (int)(pos[1]+parentPos[1]),
            checkmark.width, checkmark.height
          );
        }
      }

      //color preview box
      buf.fill(color);
      buf.rect(colorBoxPos[0], colorBoxPos[1], 40, 40);
    }
    if(playGroup.isOpen() && pipToggle.getState()) {
      pos = pipToggle.getPosition();
      parentPos = pipToggle.getParent().getPosition();

      buf.copy(
        checkmark,
        0, 0,
        checkmark.width, checkmark.height,
        (int)(pos[0]+parentPos[0]), (int)(pos[1]+parentPos[1]),
        checkmark.width, checkmark.height
      );
    }

    buf.endDraw();
    parent.imageMode(BeeTracker.CENTER);
    parent.image(buf, parent.width*.5f, parent.height*.5f);
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
    updateEventMenuItems(type);

    if(type) {
      selectRadios.activate(0);
    }

    selectRadios.setVisible(!type);
    selectLabel.setVisible(!type);
  }

  /**
   * Updates UI elements based on the boundary selection type.
   * @param type true for exit circle selection
   */
  void updateSelectType(boolean type) {
    updateSelectMenuItems(type);
  }

  /**
   * Updates the labels for the boundary selection type menu items.
   * @param type true for exit selection mode
   */
  private void updateSelectMenuItems(boolean type) {
    int i, j;

    if(type) {
      i = 0;
      j = 1;
    } else {
      i = 1;
      j = 0;
    }

    optionMenu.getItem(6).put("text", insetSelectLabels[j]);
    optionMenu.getItem(7).put("text", exitSelectLabels[i]);
  }

  /**
   * Updates the labels for the event type menu items.
   * @param type true for waggle dance mode
   */
  private void updateEventMenuItems(boolean type) {
    int i, j;
    CColor cc;

    if(type) {
      i = 0;
      j = 1;
      cc = disabledMenuColor;
    } else {
      i = 1;
      j = 0;
      cc = menuColor;
    }

    optionMenu.getItem(3).put("text", exitEventLabels[j]);
    optionMenu.getItem(4).put("text", waggleEventLabels[i]);

    //disable boundary selection menu items in waggle mode
    optionMenu.getItem(6).put("color", cc);
    optionMenu.getItem(7).put("color", cc);
  }

  /**
   * Opens or closes the program menu.
   * @param the new menu state
   */
  void setProgramMenuState(boolean state) {
    programMenu.setOpen(state);
  }

  /**
   * @return true if the program menu is open
   */
  boolean isProgramMenuOpen() {
    return programMenu.isOpen();
  }

  /**
   * Opens or closes the footage menu.
   * @param the new menu state
   */
  void setFootageMenuOpen(boolean state) {
    footageMenu.setOpen(state);
  }

  /**
   * @return true if the footage menu is open
   */
  boolean isFootageMenuOpen() {
    return footageMenu.isOpen();
  }

  /**
   * Opens or closes the option menu.
   * @param the new menu state
   */
  void setOptionMenuOpen(boolean state) {
    optionMenu.setOpen(state);
  }

  /**
   * @return true if the option menu is open
   */
  boolean isOptionMenuOpen() {
    return optionMenu.isOpen();
  }

  /**
   * Closes all menus.
   */
  void closeMenus() {
    if(!programMenu.isInside() && !programButton.isInside()) {
      programMenu.setOpen(false);
    }

    if(!footageMenu.isInside() && !footageButton.isInside()) {
      footageMenu.setOpen(false);
    }

    if(!optionMenu.isInside() && !optionButton.isInside()) {
      optionMenu.setOpen(false);
    }
  }

  /**
   * Forces the selection of am event type radio button.
   * @param index the index of the button to activate
   */
  void toggleEventRadio(int index) {
    modeRadios.getItem(index).toggle();
  }

  /**
   * Forces the selection of a boundary selection type radio button.
   * @param index the index of the button to activate
   */
  void toggleSelectRadio(int index) {
    selectRadios.getItem(index).toggle();
  }

  /**
   * Checks if a menu item is currently enabled.
   * @param menuType the containing menu of the item to check
   * @param index the index of the item to check
   * @return true if the menu item is activated
   */
  boolean checkMenuItemState(String menuType, int index) {
    boolean result = false;

    switch (menuType) {
    case "footage":
      result = (CColor)footageMenu.getItem(index).get("color") == menuColor;
      break;

    case "option":
      result = (CColor)optionMenu.getItem(index).get("color") == menuColor;
      break;

    default:
      //do nothing
    }

    return result;
  }

  /**
   * @return true if the mouse is currently inside any open ScrollableList
   */
  boolean mouseInsideLists() {
    return (programMenu.isOpen() && programMenu.isInside()) ||
      (footageMenu.isOpen() && footageMenu.isInside()) ||
      (optionMenu.isOpen() && optionMenu.isInside()) ||
      (colorList.isOpen() && colorList.isInside());
  }

  /**
   * Updates the color list caption and color preview box.
   * @param label the new label text
   */
  void updateColorLabel(String label) {
    colorList.setCaptionLabel(label);

    color = 0xff000000 + (label.equals(listLbl) ? 0 : Integer.parseInt(label, 16));
  }
}
