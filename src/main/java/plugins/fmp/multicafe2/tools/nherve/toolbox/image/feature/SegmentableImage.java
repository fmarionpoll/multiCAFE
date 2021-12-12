package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.awt.image.BufferedImage;

import plugins.nherve.toolbox.image.feature.Segmentable;

/**
 * The Interface SegmentableImage.
 * 
 * @author Nicolas HERVE - nherve@ina.fr
 */
public abstract class SegmentableImage implements Segmentable {
	private String name;

	public abstract BufferedImage getImage();

	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the new name
	 */
	public void setName(String name) {
		this.name = name;
	}
}