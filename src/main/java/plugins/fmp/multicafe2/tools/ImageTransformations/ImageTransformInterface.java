package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;

public interface ImageTransformInterface 
{
	public IcyBufferedImage run (IcyBufferedImage sourceImage, ImageTransformOptions options);
}
