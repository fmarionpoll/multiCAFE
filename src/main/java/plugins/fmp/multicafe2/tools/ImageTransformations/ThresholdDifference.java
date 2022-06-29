package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;

public class ThresholdDifference extends ImageTransformFunction implements ImageTransformInterface
{
	@Override
	public IcyBufferedImage transformImage(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		if (options.referenceImage == null)
			return null;
		
		IcyBufferedImage resultImage = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(),sourceImage.getSizeC(), sourceImage.getDataType_());
		options.npixels_changed = 0;
		
		for (int c = 0; c < sourceImage.getSizeC(); c++) 
		{
			int changed = 0;
			int [] sourceImageInt = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
			int [] referenceImageInt = Array1DUtil.arrayToIntArray(options.referenceImage.getDataXY(c), options.referenceImage.isSignedDataType());	
			int [] resultImageInt = Array1DUtil.arrayToIntArray(resultImage.getDataXY(0), resultImage.isSignedDataType());
			
			for (int i = 0; i< sourceImageInt.length; i++) 
			{
				int val = sourceImageInt[i] - referenceImageInt[i];
				if (val < options.simplethreshold) 
				{
					resultImageInt[i] = 0xff;
				}
				else 
				{
					resultImageInt[i] = 0;
					changed ++;
					referenceImageInt[i] = sourceImageInt[i];
				}
			}
			Array1DUtil.intArrayToSafeArray(resultImageInt, resultImage.getDataXY(c), true, resultImage.isSignedDataType());
			resultImage.setDataXY(c, resultImage.getDataXY(c));
			
			if (changed > 0) 
			{
				options.npixels_changed += changed;
				Array1DUtil.intArrayToSafeArray(referenceImageInt, options.referenceImage.getDataXY(c), true, options.referenceImage.isSignedDataType());
				options.referenceImage.setDataXY(c, options.referenceImage.getDataXY(c));
			}
		}
		return resultImage;
	}

}
