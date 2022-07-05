package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageCursor;
import icy.type.collection.array.Array1DUtil;

// this routine is used to build a reference image with "invariant" pixels
// create a binary result image for pixels which value is lower than a threshold
// if the value is lower, transfer these pixels to a reference image

public class ThresholdDifference extends ImageTransformFunction implements ImageTransformInterface
{
	@Override
	public IcyBufferedImage transformImage(IcyBufferedImage sourceImage, ImageTransformOptions transformOptions) 
	{
		if (transformOptions.referenceImage == null)
			return null;
		
		int width = sourceImage.getSizeX();
		int height = sourceImage.getSizeY();
		int planes = sourceImage.getSizeC();
		IcyBufferedImage resultImage = new IcyBufferedImage(width, height, planes, sourceImage.getDataType_());
		transformOptions.npixels_changed = 0;
		int changed = 0;
		
		IcyBufferedImageCursor sourceCursor = new IcyBufferedImageCursor(sourceImage);
		IcyBufferedImageCursor resultCursor = new IcyBufferedImageCursor(resultImage);
		IcyBufferedImageCursor referenceCursor = new IcyBufferedImageCursor(transformOptions.referenceImage);
		
		try 
		{
			for (int y = 0; y < height; y++ ) 
			{
				for (int x = 0; x < width; x++) 
				{
					for (int c = 0; c < planes; c++) 
					{
						double val = sourceCursor.get(x, y, c) - referenceCursor.get(x, y, c);
						if (val < transformOptions.simplethreshold) 
						{
							resultCursor.set(x, y, c, -1);
						}
						else 
						{
							resultCursor.set(x, y, c, 0);
							changed ++;
							int delta = 10;
							for (int yy = y-delta; yy < y+delta; yy++ ) 
							{
								if (yy < 0 || yy >= height)
									continue;
								for (int xx = x-delta; xx < x+delta; xx++) 
								{
									if (xx < 0 || xx >= width)
										continue;
									for (int cc = 0; cc < planes; cc++) 
									{
										referenceCursor.set(xx, yy, cc, sourceCursor.get(xx, yy, cc));
									}
								}
							}

						}
					}
				}
			}
		} 
		finally 
		{
			resultCursor.commitChanges();
			referenceCursor.commitChanges();
			transformOptions.npixels_changed = changed;

		}

		return resultImage;
	}
	
	public IcyBufferedImage transformImage_old(IcyBufferedImage sourceImage, ImageTransformOptions options) 
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
