package plugins.fmp.multicafe2.tools.TransformImage;

import icy.image.IcyBufferedImage;

public class None extends ImageTransformFunction implements TransformImageInterface
{
	@Override
	public IcyBufferedImage getTransformedImage(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		return sourceImage;
	}


}
