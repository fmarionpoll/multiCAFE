package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;

public class FromOneChantoRGB extends ImageTransformFunction implements TransformImage 
{
	int channel = 0;
	FromOneChantoRGB(int channel) 
	{
		this.channel = channel;
	}
	
	@Override
	public IcyBufferedImage run(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		IcyBufferedImage resultImage = functionRGB_keepOneChan(sourceImage, channel); 
		return resultImage;
	}

}
