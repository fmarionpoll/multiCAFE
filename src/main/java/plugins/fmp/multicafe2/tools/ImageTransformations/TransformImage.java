package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;

public interface TransformImage 
{
	public IcyBufferedImage run (IcyBufferedImage sourceImage, ImageTransformOptions options);
}
