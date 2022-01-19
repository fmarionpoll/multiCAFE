package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;

public class None extends ImageTransformFunction implements ImageTransformInterface
{
	@Override
	public IcyBufferedImage run(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		return sourceImage;
	}


}
