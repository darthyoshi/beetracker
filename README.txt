BeeTracker README
----

Contents
1. System prerequisites
2. Starting BeeTracker
 A. Video mode
 B. Slideshow mode
3. Tracking modes
 A. Exit event mode
 B. Waggle dance event mode
4. Settings
 A. Saving/loading
 B. Event type
 C. Colors
 D. Detection thresholds
 E. Settings intervals
5. Replay mode
6. Quickstart guide
7. Troubleshooting
----

1. System prerequisites
In order to run BeeTracker, an installation of Java 7 or better is required.

2. Starting BeeTracker
A. Video mode
- Click "Load Video" button
- Navigate to and select the desired video of format: AVI, MOV, MP4, MPG/MPEG
- Click "Open" button

B. Slideshow mode
- Click "Load Images" button
- Navigate to the directory containing the desired image sequence
 - Only images of valid formats (GIF, JPG, PNG, TGA) will be loaded
- Click "Open" button

NOTE: There is a known bug with Swing/AWT and JOGL where the file browser may 
not automatically receive focus and thus appears behind the main window. Use 
the appropriate Cycle Window command for your OS (ie ALT+TAB or Command+~) to 
switch focus to the file browser.

3. Tracking modes
Select the type of event to be tracked using the "Event" radio buttons (the 
upper set of buttons left of the viewing window).

A. Exit event mode
- This event mode uses two boundary types
 - Inset frame: defines the pixels that are analyzed
 - Exit circle: defines the pixels that are considered to be the hive exit
- Select the boundary type using the "Select" radio buttons (lower set of 
  buttons left of the viewing window)
- Click and drag within the viewing window to define selected boundary

NOTE: For meaningful results, the exit circle should be within the boundaries 
of the inset frame.

B. Waggle dance event mode
- This event mode uses a single bounday type
 - Inset frame: defines the pixels that are analyzed
- Boundary type selection is disabled for waggle dance events
- Click and drag within the viewing window to define inset frame boundaries

4. Settings
A. Saving/loading
- BeeTracker automatically checks for a settings file associated with loaded 
  footage
- Default settings are used if footage has no associated settings file
- Stopping playback (either via the "Eject" button or by finishing video 
  playback) automatically saves settings for the loaded footage
- Closing BeeTracker with ESC automatically saves settings for the currently 
  loaded footage if applicable

B. Event type
- Determines event tracking mode
- By default, BeeTracker tracks exit events

C. Colors
- Select color to modify from color dropdown list (upper right corner of screen)
 - "New color" selection adds a new color
- "Edit color" button replaces currently selected color with new color using
  Swing color picker
- "Remove color" button removes currently selected color from tracking
- By default, the color list is empty

D. Detection thresholds
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

E. Settings intervals
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

5. Replay mode
- Uses saved frame annotations rather than real-time blob detection as source 
  of bee position coordinates
- After playback, replay mode can be enabled by choosing to rewind footage 
  rather than closing
- If frame annotations are saved when closing footage at the end of playback, 
  loading the same footage again automatically enters replay mode

6. Quickstart guide
- Choose footage type ("Load Video" or "Load Images")
 - Video:
  - Select desired video
 - Images:
  - Select directory of desired image sequence
 - Click "Open"
- Choose tracking mode ("Event" radio buttons)
- Define boundaries
 - Choose boundary type ("Select" radio buttons)
  - Inset frame
   - Click and drag to define corners
  - Exit circle
   - Only applicable for exit event mode
   - Click to define exit center, drag to define exit radius
- Define colors
 - Add color
 - Edit color
 - Remove color
- Define thresholds
 - Select threshold type ("HSV" radio buttons)
 - Adjust threshold slider value
- Activate analysis ("Record" button)
- Manage settings intervals (optional)
 - Add new interval
  - Seek to beginning of new interval (seekbar slider)
  - Click "Settings: Add" button
  - Define new boundaries and thresholds
 - Remove interval
  - Seek to unwanted interval
  - Click "Settings: Del" button
- Begin playback ("Play" button)

7. Troubleshooting
If at any point BeeTracker unexpectedly closes or otherwise fails to behave as
expected, please send a copy of Console.log (located in the main program 
directory) to kchoi@mail.sfsu.edu.