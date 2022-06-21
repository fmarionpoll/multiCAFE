package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;

public class None extends ImageTransformFunction implements ImageTransformInterface
{
	@Override
	public IcyBufferedImage transformImage(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		return sourceImage;
	}


}
