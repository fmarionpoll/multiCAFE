package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import icy.image.IcyBufferedImage;


/**
 * The Class SegmentableBufferedImage.
 * 
 * @author Nicolas HERVE - nherve@ina.fr
 */
public class SegmentableIcyBufferedImage extends SegmentableImage {
	
	/** The image. */
	private final IcyBufferedImage image;
	
	/**
	 * Instantiates a new segmentable buffered image.
	 * 
	 * @param image
	 *            the image
	 */
	public SegmentableIcyBufferedImage(IcyBufferedImage image) {
		super();
		
		this.image = image;
	}

	/**
	 * Gets the image.
	 * 
	 * @return the image
	 */
	public IcyBufferedImage getImage() {
		return image;
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.Segmentable#getHeight()
	 */
	public int getHeight() {
		return image.getHeight();
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.Segmentable#getWidth()
	 */
	public int getWidth() {
		return image.getWidth();
	}
}
