BeeTracker README
----

Contents
1. Use cases
 A. John
 B. Sarah
2. Indepth guide
 A. System prerequisites
 B. Starting BeeTracker
  I. Video mode
  II. Slideshow mode
 C. Tracking modes
  I. Exit event mode
  II. Waggle dance event mode
 D. Settings
  I. Saving/loading
  II. Event type
  III. Colors
  IV. Detection thresholds
  V. Settings intervals
 E. Replay mode
 F. Issue reporting
----

1. Use cases
A. John
John has footage of marked bees that he wants to analyze in order to quantify 
what bees enter and leave the hive and when. He starts the BeeTracker program 
and clicks the "Load Video" button. He selects his video file in the BeeTracker 
file browser and clicks the "Open" button to load his footage. John is then 
prompted to enter a date and time for the video.

Once John's video has loaded, he selects the "Exit" option in the "Event" radio 
buttons to specify that BeeTracker will analyze ingress and egress events. John 
then selects the "Frame" option in the "Select" radio buttons on the left side 
of the screen, and then drags the mouse within the main viewing window to define 
the inset frame boundaries for analysis. To define the actual hive exit within 
the inset frame, John selects the "Exit" option in the "Select" radio buttons 
and drags the mouse within the main window again.

John selects the "New Color" entry in the color list in the upper right corner 
of the screen and clicks the "Edit Color" button. He selects a color to track 
using the color picker. John sees that areas in the inset frame that are not 
bee markings have been highlighted, so he adjusts the detection thresholds by 
selecting the "H" (hue), "S" (saturation), and "V" (value) options from the 
threshold radio buttons on the right side of the screen, and then adjusting the 
threshold slider up and down.

John remembers that the lighting in the video changes partway through, potentially 
affecting color detection. Using the seekbar at the bottom of the screen, he 
jumps forward in the video, and sees that once again areas in the inset frame 
that are bee markings have been highlighted. To compensate, John uses the 
seekbar to jump to an earlier point in the video where nothing in the inset 
frame is highlighted, and clicks the "Add Setting" button. John then uses the 
seekbar to jump forward in the video again, and adjusts the detection thresholds 
again using the threshold radio buttons and slider.

Now that John has completed the preparatory work, he is ready to actually begin 
analyzing his footage and clicks the record button at the bottom left corner of 
the screen to enable analysis, and then clicks the adjacent play button to begin 
playback. While BeeTracker analyzes John's footage, John himself leaves to work 
on other projects, and returns to BeeTracker once playback has finished. He views 
both the text and visual summaries of the recorded bee activity, before choosing 
to not rewind the video. He does, however choose to save the recorded points 
and timestamps.

After BeeTracker has returned to the title screen, John exits the program. 
Finally, John navigates to the "output" subdirectory in the main BeeTracker 
installation directory on his computer, and then into the directory with the 
same name as his video file. Inside, he finds "points.json" (containing all the 
recorded point and timestamp data), "settings.json" (containing his chosen 
tracking color, event type, and detection threshold settings), and a CSV and PNG 
both with filenames corresponding to the date and time of his video.

B. Sarah
Sarah has footage of marked bees that she wants to analyze in order to quantify 
what bees enter and leave the hive and wshen. She starts the BeeTracker program 
and clicks the "Load Video" button. She selects her video file in the BeeTracker 
file browser and clicks the "Open" button to load her footage. Sarah is then 
prompted to enter a date and time for the video.

Once Sarah's video has loaded, she selects the "Waggle" option in the "Event" 
radio buttons to specify that BeeTracker will analyze waggle dance events. Sarah 
then drags the mouse within the main viewing window to define the inset frame 
boundaries for analysis.

Sarah selects the "New Color" entry in the color list in the upper right corner 
of the screen and clicks the "Edit Color" button. She selects a color to track 
using the color picker. Realizing that she has selected the wrong color by 
mistake, Sarah selects the color in the color list and clicks the "Edit Color" 
button again to pick a new replacement color. Having made her correction, Sarah 
adds an additional color for tracking by selecting the "New Color" entry again 
and clicking the "Edit Color" button.

Now that Sarah has completed the preparatory work, she is ready to actually 
begin analyzing her footage and clicks the record button at the bottom left 
corner of the screen to enable analysis, and then clicks the adjacent play 
button to begin playback. After playback is finished, Sarah views both the text 
and visual summaries of the recorded bee activity, before choosing to not rewind t
he video. When prompted, she also chooses not to save the recorded points and 
timestamps.

After BeeTracker has returned to the title screen, Sarah exits the program. 
Finally, Sarah navigates to the "output" subdirectory in the main BeeTracker 
installation directory on her computer, and then into the directory with the 
same name as her video file. Inside, she finds "settings.json" (containing her 
chosen tracking color, event type, and detection threshold settings), and a CSV 
and PNG both with filenames corresponding to the date and time of her video.

2. Indepth guide
A. System prerequisites
In order to run BeeTracker, an installation of Java 7 or better is required.

B. Starting BeeTracker
I. Video mode
- Click "Load Video" button
- Navigate to and select the desired video of format: AVI, MOV, MP4, MPG/MPEG
- Click "Open" button

II. Slideshow mode
- Click "Load Images" button
- Navigate to the directory containing the desired image sequence
 - Only images of valid formats (GIF, JPG, PNG, TGA) will be loaded
- Click "Open" button

NOTE: There is a known bug with Swing/AWT and JOGL where the file browser may 
not automatically receive focus and thus appears behind the main window. Use 
the appropriate Cycle Window command for your OS (ie ALT+TAB or Command+~) to 
switch focus to the file browser.

C. Tracking modes
Select the type of event to be tracked using the "Event" radio buttons (the 
upper set of buttons left of the viewing window).

I. Exit event mode
- This event mode uses two boundary types
 - Inset frame: defines the pixels that are analyzed
 - Exit circle: defines the pixels that are considered to be the hive exit
- Select the boundary type using the "Select" radio buttons (lower set of 
  buttons left of the viewing window)
- Click and drag within the viewing window to define selected boundary

NOTE: For meaningful results, the exit circle should be within the boundaries 
of the inset frame.

II. Waggle dance event mode
- This event mode uses a single bounday type
 - Inset frame: defines the pixels that are analyzed
- Boundary type selection is disabled for waggle dance events
- Click and drag within the viewing window to define inset frame boundaries

D. Settings
I. Saving/loading
- BeeTracker automatically checks for a settings file associated with loaded 
  footage
- Default settings are used if footage has no associated settings file
- Stopping playback (either via the "Eject" button or by finishing video 
  playback) automatically saves settings for the loaded footage
- Closing BeeTracker with ESC automatically saves settings for the currently 
  loaded footage if applicable

II. Event type
- Determines event tracking mode
- By default, BeeTracker tracks exit events

III. Colors
- Select color to modify from color dropdown list (upper right corner of screen)
 - "New color" selection adds a new color
- "Edit color" button replaces currently selected color with new color using
  Swing color picker
- "Remove color" button removes currently selected color from tracking
- By default, the color list is empty

IV. Detection thresholds
- Colors are tracked in the HSV (hue, saturation, value) color space, with 
  values in all dimensions ranging from 0-255
- Hue threshold defines tolerance
 - Hue(color)-T_H < Hue(pixel) < Hue(color)+T_H
 - By default, the hue threshold is +-40
- Saturation and value thresholds define minimum
 - T_S < Sat(pixel)
 - T_V < Val(pixel)
 - By default, the saturation threshold is 90
 - By default, the value threshold is 20
- Thresholds are applied globally (to all selected colors)
- Adjusting thresholds:
 - Select threshold type from the "HSV" radio buttons (buttons right of the 
   viewing window)
 - Adjust threshold slider value (slider right of viewing window)

V. Settings intervals
- A settings interval is an interval during which a particular set of settings
  are applied
 - Settings intervals apply to detection thresholds
 - Settings intervals apply to boundary selections
- By default, the initial settings interval starts from 0s and spans the 
  entire duration of playback
- Clicking the "Settings: Add" button (below the seek bar at the bottom of the
  screen) creates a new settings interval starting
  from the current timestamp
 - New detection thresholds and boundary selections can be applies without 
   affecting other intervals
 - If the current timestamp already defines the start of an interval, nothing 
   happens
- Clicking the "Settings: Del" button (below the seek bar at the bottom of the
  screen) removes a settings interval
 - If only the default interval is defined, nothing happens
 - If the removed interval was the first interval, the old second interval 
   becomes the new first interval
  - The starting timestamp of the new first interval is set to 0s
  - The values of the new first interval are applied
 - Otherwise, the previous settings interval is applied

E. Replay mode
- Uses saved frame annotations rather than real-time blob detection as source 
  of bee position coordinates
- After playback, replay mode can be enabled by choosing to rewind footage 
  rather than closing
- If frame annotations are saved when closing footage at the end of playback, 
  loading the same footage again automatically enters replay mode

F. Issue reporting
If at any point BeeTracker unexpectedly closes or otherwise fails to behave as
expected, please send a copy of Console.log (located in the main program 
directory) to kchoi@mail.sfsu.edu.