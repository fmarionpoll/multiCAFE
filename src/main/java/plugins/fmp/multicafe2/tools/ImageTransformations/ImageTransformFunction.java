package plugins.fmp.multicafe2.tools.ImageTransformations;


import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;


public abstract class ImageTransformFunction 
{
	protected void copyExGIntToIcyBufferedImage(int[] ExG, IcyBufferedImage img2 )
	{
		Array1DUtil.intArrayToSafeArray(ExG,  img2.getDataXY(0), false, false); //true);
		img2.setDataXY(0, img2.getDataXY(0));
		for (int c = 1; c < 3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
	}
	
	protected void copyExGDoubleToIcyBufferedImage(double[] ExG, IcyBufferedImage img2 )
	{
		Array1DUtil.doubleArrayToSafeArray(ExG, img2.getDataXY(0), false); 
		img2.setDataXY(0, img2.getDataXY(0));
		for (int c = 1; c < 3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
	}
	
	protected IcyBufferedImage functionRGB_keepOneChan (IcyBufferedImage sourceImage, int keepChan) 
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
