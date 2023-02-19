package plugins.fmp.multicafe2.tools.TransformImage;

import icy.image.IcyBufferedImage;

public interface TransformImageInterface 
{
	public IcyBufferedImage getTransformedImage (IcyBufferedImage sourceImage, ImageTransformOptions options);
}
