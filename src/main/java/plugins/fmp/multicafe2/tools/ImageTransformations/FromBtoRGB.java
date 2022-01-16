package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;

public class FromBtoRGB extends ImageTransformFunction 
{
	String label = "B(RGB)";	
	
	@Override
	public IcyBufferedImage transformImage(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		IcyBufferedImage resultImage = functionRGB_keepOneChan(sourceImage, 2); 
		CopyRtoGB.functionTransferRedToGreenAndBlue(resultImage);
		return resultImage;
	}
	
	private IcyBufferedImage functionRGB_keepOneChan (IcyBufferedImage sourceImage, int keepChan) 
	{
		IcyBufferedImage resultImage = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		resultImage.copyData(sourceImage, keepChan, 0);
		resultImage.setDataXY(0, resultImage.getDataXY(0));
		for (int c = 1; c < 3; c++ ) 
		{
			resultImage.copyData(resultImage, 0, c);
			resultImage.setDataXY(c, resultImage.getDataXY(c));
		}
		return resultImage;
	}

}
