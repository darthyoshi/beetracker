package beetracker;

import processing.core.PApplet;
import processing.video.Movie;
import weka.clusterers.SimpleKMeans;
import controlP5.Button;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.ControlP5Constants;

public class BeeTracker extends PApplet {
    private int[] departureCount;
    private int[] returnCount;
    private static int numColors;
    private int[] colors;
    static final short[] beeActions = {0, 1}; //0 = depart, 1 = return
    
    private VideoBrowser fb;
    
    private ControlP5 cp5;
    private Button openButton;
    private Button colorsButton;
    
    private Movie movie;

    @Override
	public void setup() {
	    size(800, 600);
	    frameRate(30);
	    background(0x444444);

	    cp5 = new ControlP5(this);
        cp5.setFont(cp5.getFont().getFont(), 15);
	    
        openButton = cp5.addButton("openButton")
            .setSize(120, 20)
            .setPosition(25, 25)
            .setCaptionLabel("Open video file");
	    openButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);
	    
	    colorsButton = cp5.addButton("colorsButton")
            .setSize(150, 20)
            .setPosition(150, 25)
            .setCaptionLabel("Set tracking colors");
	    colorsButton.getCaptionLabel().alignX(ControlP5Constants.CENTER);
	}

	@Override
	public void draw() {
	    
	}

	/**
	 * Callback method for handling ControlP5 UI events.
	 * @param event the initiating ControlEvent
	 */
	public void controlEvent(ControlEvent event) {
	    String eventName = event.getName();
	    
	    switch(eventName) {
	    case "openButton":
	        String videoPath = VideoBrowser.getVideoName(this);

	        if(videoPath != null) {
//	            movie = new Movie(this, videoName);
//	            movie.play();
	            println(videoPath);
	        }
	        
	        break;
	        
	    case "colorsButton":
	        
	    }
	}
	
    /**
     * Main method for executing BeeTracker as a Java application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PApplet.main(new String[] { beetracker.BeeTracker.class.getName() });
    }
}
