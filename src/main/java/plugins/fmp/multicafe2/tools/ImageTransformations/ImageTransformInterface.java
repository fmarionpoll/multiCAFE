package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;

public interface ImageTransformInterface 
{
	public IcyBufferedImage transformImage (IcyBufferedImage sourceImage, ImageTransformOptions options);
}
