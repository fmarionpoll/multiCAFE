package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region;

import java.awt.Color;
import java.awt.Graphics2D;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Segmentable;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.IcySupportRegion;


/**
 * The Class DefaultSupportRegion.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public abstract class DefaultSupportRegion implements IcySupportRegion {
	
	/** The obj. */
	private final Segmentable obj;
	
	/**
	 * Instantiates a new default support region.
	 * 
	 * @param obj
	 *            the obj
	 */
	public DefaultSupportRegion(Segmentable obj) {
		super();
		this.obj = obj;
	}
	

	/**
	 * Paint.
	 * 
	 * @param g2
	 *            the g2
	 * @param borderColor
	 *            the border color
	 * @param fillColor
	 *            the fill color
	 * @param opacity
	 *            the opacity
	 */
	public abstract void paint(Graphics2D g2, Color borderColor, Color fillColor, float opacity);
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public abstract String toString();

	/**
	 * Gets the overall width.
	 * 
	 * @return the overall width
	 */
	public int getOverallWidth() {
		return obj.getWidth();
	}

	/**
	 * Gets the overall height.
	 * 
	 * @return the overall height
	 */
	public int getOverallHeight() {
		return obj.getHeight();
	}
}
