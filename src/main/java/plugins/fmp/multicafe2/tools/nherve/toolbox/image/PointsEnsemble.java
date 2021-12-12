package plugins.fmp.multicafe2.tools.nherve.toolbox.image;

import java.util.List;

import javax.vecmath.Point3i;

/**
 * The Interface PointsEnsemble.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public interface PointsEnsemble {

	/**
	 * Gets the height.
	 * 
	 * @return the height
	 */
	public abstract int getHeight();

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public abstract int getId();

	/**
	 * Gets the min x.
	 * 
	 * @return the min x
	 */
	public abstract int getMinX();

	/**
	 * Gets the min y.
	 * 
	 * @return the min y
	 */
	public abstract int getMinY();

	/**
	 * Gets the points.
	 * 
	 * @return the points
	 */
	public abstract List<Point3i> getPoints();

	/**
	 * Gets the width.
	 * 
	 * @return the width
	 */
	public abstract int getWidth();

	/**
	 * Gets the x center.
	 * 
	 * @return the x center
	 */
	public abstract int getXCenter();

	/**
	 * Gets the y center.
	 * 
	 * @return the y center
	 */
	public abstract int getYCenter();

}