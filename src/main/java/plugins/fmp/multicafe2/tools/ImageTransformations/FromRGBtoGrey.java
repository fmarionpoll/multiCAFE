package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;

public class FromRGBtoGrey extends ImageTransformFunction implements TransformImage 
{
	@Override
	public IcyBufferedImage run(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		return functionRGB_grey(sourceImage); 
	}
	
	private IcyBufferedImage functionRGB_grey (IcyBufferedImage sourceImage) 
	{
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		int[] tabValuesR = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		int[] tabValuesG = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		int[] tabValuesB = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());
		int[] outValues0 = Array1DUtil.arrayToIntArray(img2.getDataXY(0), sourceImage.isSignedDataType());
		
		for (int ky =0; ky < outValues0.length; ky++) 
			outValues0 [ky] = (tabValuesR[ky]+tabValuesG[ky]+tabValuesB[ky])/3;
		
		copyExGIntToIcyBufferedImage(outValues0, img2);
		return img2;
	}

}
