/**
 * @file Cluster.java
 * @author Kay Choi, 909926828
 * @date 20 Feb 2015
 * @description A data structure storing information about a cluster of points.
 */

package beetracker;

import java.util.ArrayList;

public class Cluster {
    private float x, y, width, height;
    private boolean updated = false;
    private ArrayList<int[]> points;

    /**
     * Class constructor.
     */
    public Cluster() {
    	points = new ArrayList<int[]>();
    	x = y = width = height = 0f;
    }

    /**
     * TODO add method header
     * @param c
     */
    public void assign(Cluster c) {
    	setX(c.getX());
    	setY(c.getY());
    	setWidth(c.getWidth());
    	setHeight(c.getHeight());
    	setUpdated(c.isUpdated());
    	setPoints(c.getPoints());
    }

    /**
     * TODO add method header
     * @param p
     */
    private void setPoints(ArrayList<int[]> p) {
		points.clear();
		points.addAll(p);
	}

	/**
     * TODO add method header
     * @return
     */
    public ArrayList<int[]> getPoints() {
		return points;
	}

	/**
     * TODO add method header
     * @return
     */
	public float getX() {
		return x;
	}

	/**
	 * TODO add method header
	 * @param x
	 */
	public void setX(float x) {
		this.x = x;
	}

	/**
	 * TODO add method header
	 * @return
	 */
	public float getY() {
		return y;
	}

	/**
	 * TODO add method header
	 * @param y
	 */
	public void setY(float y) {
		this.y = y;
	}

	/**
	 * TODO add method header
	 * @return
	 */
	public float getWidth() {
		return width;
	}

	/**
	 * TODO add method header
	 * @param width
	 */
	public void setWidth(float width) {
		this.width = width;
	}

	/**
	 * TODO add method header
	 * @return
	 */
	public float getHeight() {
		return height;
	}

	/**
	 * TODO add method header
	 * @param height
	 */
	public void setHeight(float height) {
		this.height = height;
	}

	/**
	 * TODO add method header
	 * @return
	 */
	public boolean isUpdated() {
		return updated;
	}

	/**
	 * TODO add method header
	 * @param state
	 */
	public void setUpdated(boolean state) {
		updated = state;
	}

	/**
	 * TODO add method header
	 * @param point
	 */
	public void addPoint(int[] point) {
		points.add(point);
	}

	/**
	 * Calculates the Cartesian width and height of the cluster.
	 */
	public void calcBounds() {
		int[] max = {Integer.MIN_VALUE, Integer.MIN_VALUE};
		int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE};

		for(int[] point : points) {
			if(point[0] > max[0]) {
				max[0] = point[0];
			}

			else if(point[0] < min[0]) {
				min[0] = point[0];
			}

			if(point[1] > max[1]) {
				max[1] = point[1];
			}

			else if(point[1] < min[1]) {
				min[1] = point[1];
			}
		}

		width = max[0] - min[0];
		height = max[1] - min[1];
	}
}
