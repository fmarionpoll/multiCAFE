package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;

public class CopyRtoGB extends ImageTransformFunction 
{
	String label = "R to G,B";	
	
	@Override
	public IcyBufferedImage transformImage(IcyBufferedImage sourceImage, ImageTransformOptions options) {
		return functionTransferRedToGreenAndBlue(sourceImage);
	}

	public static IcyBufferedImage functionTransferRedToGreenAndBlue(IcyBufferedImage sourceImage) 
	{
		int sourceChannel = 0;
		int numberChannels = 3;
		IcyBufferedImage resultImage = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		for (int c = 0; c < numberChannels; c++ ) 
		{ 
			if (c == sourceChannel)
				continue;
			resultImage.copyData(sourceImage, sourceChannel, c);
			resultImage.setDataXY(c, resultImage.getDataXY(c));
		}
		return resultImage;
	}
}
