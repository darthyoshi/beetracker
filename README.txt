BeeTracker quick start guide

1. System prerequisites
In order to run BeeTracker, an installation of Java 7 or better is required.

2. Using BeeTracker
When the program first starts up, there will only be a single button for 
loading a video file. Pressing this button will bring up a file browser for 
selecting a video file. Once the file has been selected, you will be prompted 
to enter a date and time. This timestamp represents the beginning of the 
video, and defines the timestamps generated in the final output.

Once the video has been loaded the program switches to config mode. Here you 
can define the hive exit; for simplicity this is assumed to be a circle. You 
can also define the boundaries of the inset frame, which for performance 
reasons is the only portion of the video frame that is actually analyzed. 
Switch between the two selection modes with a toggle at the bottom right 
corner of the screen. Config mode also allows you to define the colors that 
the program will track, as well as the HSV color space detection sensitivity 
thresholds.

When a video is loaded the program will attempt to find and load previously 
generated frame annotations for the video in the "output" subdirectory. If 
annotations are present, the program use replay mode for playback. Otherwise 
the playback proceeds in annotation mode. In annotation mode, image processing
(specifically, blob detection) is performed to generate the point data used 
for tracking. In replay mode the point data already exists, so no image 
processing is performed. Image processing is CPU intensive; it is best to 
leave the CPU as free as possible in order to generate the most accurate point
data.

In annotation mode, recording can be toggled any time by pressing the record 
button. Only frame annotations generated while recording is active will be 
saved. At the end of video playback, any bee events detected using the 
annotations are saved to a CSV file in the "output" subdirectory. At this 
point you can either rewind the video or close it. If any frame annotations 
were saved, then rewinding the video will automatically switch the program to 
replay mode, while closing the video will give you the choice of whether or 
not to save the annotations to file.

3. Troubleshooting
If at any point BeeTracker unexpectedly closes or otherwise fails to behave as
expected, please send a copy of Console.log (located in the main program 
directory) to kchoi@mail.sfsu.edu.