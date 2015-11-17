BeeTracker quick start guide

1. System prerequisites
In order to run BeeTracker, an installation of Java 7 or better is required.

2. Using BeeTracker
When the program first starts up, there will be two buttons. One is for 
loading a video file, and the other is for loading a sequence of images.
Pressing either button will bring up a file browser for selecting a video file
or image directory as appropriate. Once the selection has been made, you will
be prompted to enter a date and time. This timestamp represents the beginning
of the video, and defines the timestamps generated in the final output.

Once the video has been loaded the program switches to config mode. On the left 
side of the screen are a number of switches. These allow you to choose the type 
of event being tracked (waggle dances or arrivals/departures), whether dragging 
the mouse in the main window defines the inset frame or exit circle boundaries, 
and whether or not to zoom into the inset frame. The slider on the right side of 
the screen allows you to adjust the detection thresholds; choose between the 
different threshold types (hue, saturation, value) using the radio buttons.

When a video is loaded the program will attempt to find and load previously 
generated frame annotations for the video in the "output" subdirectory. If 
annotations are present, the program use replay mode for playback. Otherwise 
the playback proceeds in annotation mode. In annotation mode, image processing
(specifically, blob detection) is performed to generate the point data used 
for tracking. In replay mode the point data already exists, so no image 
processing is performed. Image processing is CPU and GPU intensive; it is best 
to leave the computer as free as possible in order to generate the most accurate 
point data.

In annotation mode, recording can be toggled any time by pressing the record 
button. Only frame annotations generated while recording is active will be 
saved. At the end of video playback, any bee events detected using the 
annotations are saved to a CSV file in the "output" subdirectory. Additionally, 
a graphical timeline showing all detected bee activity and events is saved as 
a PNG file in the same subdirectory. At this point you can either rewind the 
video or close it. If any frame annotations were saved, then rewinding the video 
will automatically switch the program to replay mode, while closing the video 
will give you the choice of whether or not to save the annotations to file.

3. Troubleshooting
If at any point BeeTracker unexpectedly closes or otherwise fails to behave as
expected, please send a copy of Console.log (located in the main program 
directory) to kchoi@mail.sfsu.edu.