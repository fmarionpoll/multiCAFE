package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import icy.image.IcyBufferedImage;


public class SegmentableIcyBufferedImage extends SegmentableImage {
	
	private final IcyBufferedImage image;
	
	public SegmentableIcyBufferedImage(IcyBufferedImage image) {
		super();
		this.image = image;
	}

	public IcyBufferedImage getImage() {
		return image;
	}

	public int getHeight() {
		return image.getHeight();
	}

	public int getWidth() {
		return image.getWidth();
	}
}
