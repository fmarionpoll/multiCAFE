package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.awt.geom.Rectangle2D;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.Pixel;


/**
 * The Interface SupportRegion.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
@SuppressWarnings("rawtypes")
public interface SupportRegion<T extends Pixel> extends Iterable<T> {
	
	/**
	 * Gets the center.
	 * 
	 * @return the center
	 */
	T getCenter();
	
	/**
	 * Contains.
	 * 
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 * @return true, if successful
	 */
	boolean contains(double x, double y);
	
	/**
	 * Gets the bounding box.
	 * 
	 * @return the bounding box
	 */
	Rectangle2D getBoundingBox();
}
