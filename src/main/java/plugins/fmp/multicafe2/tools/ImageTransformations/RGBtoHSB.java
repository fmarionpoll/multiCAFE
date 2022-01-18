package plugins.fmp.multicafe2.tools.ImageTransformations;

import java.awt.Color;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;

public class RGBtoHSB extends ImageTransformFunction implements ImageTransformInterface
{
	int channelOut = 0;
	RGBtoHSB(int channelOut)
	{
		this.channelOut = channelOut;
	}
	
	@Override
	public IcyBufferedImage run(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		IcyBufferedImage img = functionRGBtoHSB(sourceImage);
		if (channelOut >= 0)
			img = functionRGB_keepOneChan(img, channelOut);
		return img;
	}

	protected IcyBufferedImage functionRGBtoHSB(IcyBufferedImage sourceImage) 
	{
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		
		double[] tabValuesR = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		double[] tabValuesG = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		double[] tabValuesB = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());

		double[] outValues0 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(0), img2.isSignedDataType());
		double[] outValues1 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(1), img2.isSignedDataType());
		double[] outValues2 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(2), img2.isSignedDataType());
		
		// compute values
		for (int ky = 0; ky < tabValuesR.length; ky++) 
		{
			int R = (int) tabValuesR[ky];
			int G = (int) tabValuesG[ky];
			int B = (int) tabValuesB[ky];
		
			float[] hsb = Color.RGBtoHSB(R, G, B, null) ;
			outValues0 [ky] = (double) hsb[0] * 100;;
			outValues1 [ky] = (double) hsb[1] * 100;;
			outValues2 [ky] = (double) hsb[2] * 100;;
		}
		int c= 0;
		Array1DUtil.doubleArrayToSafeArray(outValues0,  img2.getDataXY(c), false); //  img2.isSignedDataType());
		img2.setDataXY(c, img2.getDataXY(c));
		c++;
		Array1DUtil.doubleArrayToSafeArray(outValues1,  img2.getDataXY(c), false); //  img2.isSignedDataType());
		img2.setDataXY(c, img2.getDataXY(c));
		c++;
		Array1DUtil.doubleArrayToSafeArray(outValues2,  img2.getDataXY(c), false); //  img2.isSignedDataType());
		img2.setDataXY(c, img2.getDataXY(c));
		return img2;
	}
}
