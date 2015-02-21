/**
 * @file BeeTracker.java
 * @author Kay Choi, 909926828
 * @date 29 Jan 15
 * @description A tool for tracking bees in a video.
 */

package beetracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import controlP5.ControlEvent;

import processing.core.PApplet;
import processing.core.PImage;
import processing.video.Movie;

@SuppressWarnings("serial")
public class BeeTracker extends PApplet {
    private ArrayList<Integer> colors;
    private ArrayList<Integer> departureCount;
    private ArrayList<Integer> returnCount;
    private boolean isPlaying = false, init = false;
    private boolean pip = false, selectExit = false;
    private static final int[] mainBounds = {50, 50, 750, 550};
    private short playbackSpeed = 1;
    private int listVal = 0;
    private int[] movieDims, zoomDims;

    private File currentDir = null;

    private float[] insetBox, exitRadial;
    private boolean isDrag = false;

    private Movie movie = null;

    private PImage blobImg;

    private BlobDetectionUtils bdu;

    private DataMinerUtils dmu;

    private UIControl uic;

    private ArrayList<Cluster> clusters;

    private PrintStream log = null;

    private processing.core.PFont font;
    private PImage title;

    /**
     *
     */
    @Override
    public void setup() {
        size(800, 600);
        frameRate(30);

        if(frame != null) {
            frame.setTitle("BeeTracker");
        }

        try {
            log = new PrintStream(new File("Console.log"));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace(log);
            exit();
        }

        uic = new UIControl(this);

        Scanner scan = null;
        colors = new ArrayList<Integer>();
        try {
            scan = new Scanner(new File("colors.txt"));

            int rgbVal;
            String color, header = "ff";
            while(scan.hasNext()) {
                color = scan.next();
                rgbVal = (int)Long.parseLong(header + color, 16);

                uic.addListItem(color, rgbVal);

                colors.add(rgbVal);
            }
        } catch(NumberFormatException ex) {
            colors.clear();
            ex.printStackTrace(log);
        } catch(FileNotFoundException e) {
            e.printStackTrace(log);
        } finally {
            if(scan != null) {
                scan.close();
            }
        }

        dmu = new DataMinerUtils(this, log, colors);

        background(0x444444);

        blobImg = createImage(width/2, width/2, RGB);

        bdu = new BlobDetectionUtils(width/2, height/2);

        insetBox = new float[4];
        insetBox[0] = insetBox[1] = 0f;
        insetBox[2] = insetBox[3] = 1f;

        exitRadial = new float[4];
        exitRadial[0] = exitRadial[1] = exitRadial[2] = exitRadial[3] = 0f;

        font = this.createDefaultFont(12);
        title = loadImage("data/img/title.png");
    }

    /**
     *
     */
    @Override
    public void draw() {
        background(0x222222);

        noStroke();
        fill(0xff444444);
        rectMode(CORNERS);
        rect(mainBounds[0], mainBounds[1], mainBounds[2], mainBounds[3]);

        textFont(font);
        fill(0xFFFF0099);

        if(movie != null) {

            if((isPlaying || init) && movie.available()) {
                movie.read();

                if(init) {
                    movie.stop();
                }
            }

            imageMode(CENTER);
            movieDims = scaledDims(
        		movie.width,
        		movie.height,
        		width - 100,
        		height - 100
    		);
            image(movie, width/2, height/2, movieDims[0], movieDims[1]);

            if(!init) {            	
            	blobImg.copy(
                    movie,
                    (int)(movie.width*insetBox[0]),
                    (int)(movie.height*insetBox[1]),
                    (int)(movie.width*(insetBox[2] - insetBox[0])),
                    (int)(movie.height*(insetBox[3] - insetBox[1])),
                    0, 0, blobImg.width, blobImg.height
                );

                BlobDetectionUtils.preProcessImg(this, blobImg, colors);

                bdu.computeBlobs(blobImg.pixels);

                if(pip) {
                    blobImg.copy(
                        movie,
                        (int)(movie.width*insetBox[0]),
                        (int)(movie.height*insetBox[1]),
                        (int)(movie.width*(insetBox[2] - insetBox[0])),
                        (int)(movie.height*(insetBox[3] - insetBox[1])),
                        0, 0, blobImg.width, blobImg.height
                    );
                    zoomDims = scaledDims(
                		movieDims[0]*(insetBox[2] - insetBox[0]),
                		movieDims[1]*(insetBox[3] - insetBox[1]),
                		width -100/*movieDims[0]*/,
                		height-100/*movieDims[1]*/
    				);
                    image(blobImg, width/2, height/2, zoomDims[0], zoomDims[1]);
                }

                bdu.drawEdges(this, pip, insetBox);

                clusters = dmu.getClusters(bdu.getCentroids());

    //            dmu.updateCentroids(blobImg, clusters);

                textSize(32);
                textAlign(CENTER, CENTER);
                text("#bees: " + clusters.size(), width/2, 25);

                textAlign(RIGHT, CENTER);
                text("current speed: "+playbackSpeed+'x', 750, 575);
            }

            else {
                textSize(24);
                textAlign(LEFT, CENTER);
                text("Setup Mode", 50, 25);
                
                textAlign(CENTER, CENTER);
                text("Press play to begin.", width/2, 575);

                if(colors.isEmpty()) {
                    textSize(28);
                    text("No colors selected. Please choose a color.", width/2, height/2);
                }
            }

            strokeWeight(1);
            noFill();

            //inset box
            stroke(0xffff0505);
            if(!pip || init) {
            	rectMode(CORNERS);
                rect(
                    insetBox[0]*movieDims[0]+(width-movieDims[0])/2,
                    insetBox[1]*movieDims[1]+(height-movieDims[1])/2,
                    insetBox[2]*movieDims[0]+(width-movieDims[0])/2,
                    insetBox[3]*movieDims[1]+(height-movieDims[1])/2
                );

                ellipseMode(RADIUS);
                ellipse(
            		exitRadial[0]*movieDims[0]+(width-movieDims[0])/2,
                    exitRadial[1]*movieDims[1]+(height-movieDims[1])/2,
                    exitRadial[2]*movieDims[0],
                    exitRadial[3]*movieDims[1]
        		);
            }
            
            else {
            	rectMode(CENTER);
            	rect(width/2, height/2, zoomDims[0], zoomDims[1]);
            }
                
            if(isDrag) {
            	if(!selectExit) {
	                line(
	                    insetBox[0]*movieDims[0]+(width-movieDims[0])/2,
	                    insetBox[1]*movieDims[1]+(height-movieDims[1])/2,
	                    insetBox[2]*movieDims[0]+(width-movieDims[0])/2,
	                    insetBox[3]*movieDims[1]+(height-movieDims[1])/2
	                );
	                line(
	                    insetBox[0]*movieDims[0]+(width-movieDims[0])/2,
	                    insetBox[3]*movieDims[1]+(height-movieDims[1])/2,
	                    insetBox[2]*movieDims[0]+(width-movieDims[0])/2,
	                    insetBox[1]*movieDims[1]+(height-movieDims[1])/2
	                );
            	}

            	else {
            		line(
	            		exitRadial[0]*movieDims[0]+(width-movieDims[0])/2,
	                    exitRadial[1]*movieDims[1]+(height-movieDims[1])/2-exitRadial[3]*movieDims[1],
	            		exitRadial[0]*movieDims[0]+(width-movieDims[0])/2,
	                    exitRadial[1]*movieDims[1]+(height-movieDims[1])/2+exitRadial[3]*movieDims[1]
    				);
            		line(
                        exitRadial[0]*movieDims[0]+(width-movieDims[0])/2-exitRadial[2]*movieDims[0],
                        exitRadial[1]*movieDims[1]+(height-movieDims[1])/2,
                        exitRadial[0]*movieDims[0]+(width-movieDims[0])/2+exitRadial[2]*movieDims[0],
                        exitRadial[1]*movieDims[1]+(height-movieDims[1])/2
    				);
            	}
            }
        }

        else {
        	imageMode(CENTER);
            image(title, width/2, height/2-50);

            textSize(50);
            textAlign(LEFT, CENTER);
            text("Bee", width/2 - 120, height/2);
            text("Tracker", width/2 - 70, height/2 + 50);
        }

        //main window border
        stroke(0xff000000);
        noFill();
        rectMode(CORNERS);
        rect(mainBounds[0], mainBounds[1], mainBounds[2], mainBounds[3]);
    }

    /**
     * Callback method for handling ControlP5 UI events.
     * @param event the initiating ControlEvent
     */
    public void controlEvent(ControlEvent event) {
        String eventName = event.getName();

        switch(eventName) {
        case "openButton":
            File video = VideoBrowser.getVideoFile(this, currentDir);

            String videoPath = null;

            if(video != null) {
                try {
                    videoPath = video.getCanonicalPath();
                } catch (IOException e) {
                    e.printStackTrace(log);
                    exit();
                }

                currentDir = video.getParentFile();
            }

            if(videoPath != null) {
                movie = new Movie(this, videoPath);
                movie.play();
                init = true;
                isPlaying = false;

                uic.toggleSetup();
                uic.toggleOpenButton();
                uic.togglePlay();

                log.append("loaded ").append(videoPath).append('\n');
                log.flush();
            }

            break;

        case "editColor":
            int color = ColorPicker.getColor(this);

            if(listVal == 0) {
                if(!colors.contains(color)) {
                    colors.add(color);

                    String code = Integer.toHexString(color);
                    if(code.length() > 6) {
                        code = code.substring(code.length()-6, code.length());
                    }

                    uic.addListItem(code, color);

                    dmu.initColors(getHues());
                }
            }

            else if(colors.contains(listVal)) {
                colors.set(colors.indexOf(listVal), color);

                uic.clearList();
                for(Integer rgbVal : colors) {
                    uic.addListItem(Integer.toHexString(rgbVal), rgbVal);
                }

                dmu.initColors(getHues());
            }

            break;

        case "removeColor":
            if(listVal != 0 && colors.contains(listVal)) {
                colors.remove(colors.indexOf(listVal));

                if(colors.isEmpty()) {
                    listVal = 0;
                }

                uic.clearList();
                for(Integer rgbVal : colors) {
                    uic.addListItem(Integer.toHexString(rgbVal), rgbVal);
                }

                dmu.initColors(getHues());
            }

            break;

        case "playButton":
            if(!colors.isEmpty()) {
                isPlaying = ((controlP5.Toggle)event.getController()).getState();

                if(init) {
                    uic.toggleSetup();

                    init = false;
                }

                if(movie != null) {
                    if(isPlaying) {
                        movie.play();
                    }

                    else {
                        movie.stop();
                    }
                }
            }

            break;

        case "stopButton":
            if(isPlaying) {
            	isPlaying = !isPlaying;
                uic.setPlayState(isPlaying);
            }

            if(movie != null) {
                movie.stop();
                movie = null;
            }

            uic.toggleOpenButton();
            uic.togglePlay();

            if(init) {
            	uic.toggleSetup();
            }

            insetBox[0] = insetBox[1] = 0f;
            insetBox[2] = insetBox[3] = 1f;

            exitRadial[0] = exitRadial[1] = exitRadial[2] = exitRadial[3] = 0f;

            break;

        case "fastForward":
            if(movie != null) {
                playbackSpeed *= 2;
                if(playbackSpeed > 16) {
                    playbackSpeed = 1;
                }

                movie.frameRate(playbackSpeed*frameRate);
            }

            break;

        case "colorList":
            listVal = (int)event.getValue();

            break;

        case "pipToggle":
            pip = !pip;

            break;

        case "selectToggle":
        	selectExit = !selectExit;
        	uic.updateSelectLbl(selectExit);

        	break;
        }
    }

    /**
     * TODO add method header
     * @return
     */
    private ArrayList<Float> getHues() {
        ArrayList<Float> result = new ArrayList<Float>(colors.size());

        for(Integer color : colors) {
            result.add(hue(color));
        }

        return result;
    }

    /**
     *
     */
    @Override
    public void exit() {
        if(log != null) {
            log.close();
        }

        super.exit();
    }

    /**
     *
     */
    @Override
    public void mousePressed() {
        if(movie != null) {
            if(
        		mouseX > (width-movieDims[0])/2 && 
        		mouseX < (width+movieDims[0])/2 &&
                mouseY > (height-movieDims[1])/2 &&
                mouseY < (height+movieDims[1])/2 && init
            ) {
            	if(!selectExit) {
	                insetBox[0] = insetBox[2] =
                		(float)(mouseX-(width-movieDims[0])/2)/movieDims[0];
	                insetBox[1] = insetBox[3] =
                		(float)(mouseY-(height-movieDims[1])/2)/movieDims[1];
            	}
            	
            	else {
            		exitRadial[0] = (float)(mouseX-(width-movieDims[0])/2)/movieDims[0];
            		exitRadial[1] = (float)(mouseY-(height-movieDims[1])/2)/movieDims[1];
            		exitRadial[2] = exitRadial[3] = 0f;
            	}

                isDrag = true;
            }
        }
    }

    /**
     *
     */
    @Override
    public void mouseDragged() {
        if(isDrag) {
            if(!selectExit) {
            	int tmp[] = constrainMousePosition(mouseX, mouseY);

            	insetBox[2] = (float)(tmp[0]-(width-movieDims[0])/2)/movieDims[0];
	            insetBox[3] = (float)(tmp[1]-(height-movieDims[1])/2)/movieDims[1];
            }

            else {
            	float tmp2[] = constrainRadius(mouseX, mouseY);

            	exitRadial[2] = tmp2[0];
        		exitRadial[3] = tmp2[1];
            }
        }
    }

    /**
     * Constrains the selection circle within the view window.
     * @param mouseX the x-coordinate of the mouse
     * @param mouseY the y-coordinate of the mouse
     * @return a float array containing the normalized circle radius
     */
    private float[] constrainRadius(int mouseX, int mouseY) {
    	int[] tmp = constrainMousePosition(mouseX, mouseY);

    	float[] result = new float[2];
    	//semi-major axis (x)
    	result[0] = exitRadial[0]*movieDims[0] + (width-movieDims[0])/2 - tmp[0];
    	//semi-major axis (y)
    	result[1] = exitRadial[1]*movieDims[1] + (height-movieDims[1])/2 - tmp[1];

		result[0] = result[1] = (float)Math.pow((Math.pow(result[0], 2) + Math.pow(result[1], 2)), .5);

		//constrain semi-major axis (x)
    	if(result[0] > exitRadial[0]*movieDims[0]) {
    		result[0] = exitRadial[0]*movieDims[0];
    	}
    	
    	if(result[0] > movieDims[0]-exitRadial[0]*movieDims[0]) {
    		result[0] = movieDims[0]-exitRadial[0]*movieDims[0];
    	}

		//constrain semi-major axis (y)
    	if(result[1] > exitRadial[1]*movieDims[1]) {
    		result[1] = exitRadial[1]*movieDims[1];
    	}
    	
    	if(result[1] > movieDims[1]-exitRadial[1]*movieDims[1]) {
    		result[1] = movieDims[1]-exitRadial[1]*movieDims[1];
    	}

    	//choose smaller axis
    	result[0] = (result[0] < result[1] ? result[0] : result[1]);

    	//normalize radii
		result[0] /= movieDims[0];
		result[1] /= movieDims[1];

    	return result;
    }
    
    /**
     * Constrains the mouse coordinates within the view window.
     * @param mouseX the x-coordinate of the mouse
     * @param mouseY the y-coordinate of the mouse
     * @return an integer array containing the adjusted coordinates
     */
    private int[] constrainMousePosition(int mouseX, int mouseY) {
        int[] result = {mouseX, mouseY};

        if(mouseX < (width-movieDims[0])/2) {
            result[0] = (width-movieDims[0])/2;
        }
        
        else if (mouseX > (width+movieDims[0])/2) {
            result[0] = (width+movieDims[0])/2;
        }

        if(mouseY < (height-movieDims[1])/2) {
            result[1] = (height-movieDims[1])/2;
        }
        
        else if(mouseY > (height+movieDims[1])/2) {
            result[1] = (height+movieDims[1])/2;
        }
        
        /*else {
        	int radius = (int);
    		if(mouseX - (width-movieDims[0])/2 > 2*((int)(exitRadial[0]*movieDims[0]))) {
    			result[0] = 2*((int)(exitRadial[0]*movieDims[0]))+(width-movieDims[0])/2;
    		}

    		else if(mouseX < (int)(exitRadial[0]*movieDims[0] - width/2 - 3*movieDims[0]/2)) {
    			mouseX = (int)(exitRadial[0]*movieDims[0] - width/2 - 3*movieDims[0]/2);
        	}
	
	        if(mouseY -(height-movieDims[1])/2 > 2*((int)(exitRadial[1]*movieDims[1]))) {
	            result[1] =  2*((int)(exitRadial[1]*movieDims[1]))+(height-movieDims[1])/2;
	        }
	
	        else if(!selectExit && mouseY > (height+movieDims[1])/2) {
	            result[1] = (height+movieDims[1])/2;
	        }
        }
*/
        return result;
    }

    /**
     *
     */
    @Override
    public void mouseReleased() {
        if(isDrag) {
        	if(!selectExit) {
	            if(insetBox[0] == insetBox[2] || insetBox[1] == insetBox[3]) {
	                insetBox[0] = insetBox[1] = 0f;
	                insetBox[2] = insetBox[3] = 1f;
	            }
	
	            else {
	                float tmp;
	
	                if(insetBox[0] > insetBox[2]) {
	                    tmp = insetBox[0];
	                    insetBox[0] = insetBox[2];
	                    insetBox[2] = tmp;
	                }
	
	                if(insetBox[1] > insetBox[3]) {
	                    tmp = insetBox[1];
	                    insetBox[1] = insetBox[3];
	                    insetBox[3] = tmp;
	                }
	            }
            }

            isDrag = false;
        }

    }

    /**
     * Determines the dimensions of a PImage, scaled to fit within the display
     *   window.
     * @param imgWidth the width to scale
     * @param imgHeight the height to scale
     * @return an integer array containing the scaled dimensions
     */
    private int[] scaledDims(
		float imgWidth,
		float imgHeight,
		int maxWidth,
		int maxHeight
	) {
        int[] result = new int[2];

        float ratio = imgWidth/imgHeight;

        //scale by width
        result[0] = maxWidth;
        result[1] = (int)(result[0]/ratio);

        //scale by height
        if(result[1] > maxHeight) {
            result[1] = maxHeight;
            result[0] = (int)(result[1]*ratio);
        }

        return result;
    }

    /**
     * Main method for executing BeeTracker as a Java application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PApplet.main(new String[] { beetracker.BeeTracker.class.getName() });
    }
}
