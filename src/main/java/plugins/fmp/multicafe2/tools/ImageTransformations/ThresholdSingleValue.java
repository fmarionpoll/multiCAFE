package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;

public class ThresholdSingleValue extends ImageTransformFunction implements ImageTransformInterface
{
	@Override
	public IcyBufferedImage transformImage(IcyBufferedImage sourceImage, ImageTransformOptions options) 
	{
		IcyBufferedImage binaryMap = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(), 1, DataType.UBYTE);
		byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);
		int [] imageSourceDataBuffer = null;
		DataType datatype = sourceImage.getDataType_();
		if (datatype != DataType.INT) 
		{
			Object sourceArray = sourceImage.getDataXY(0);
			imageSourceDataBuffer = Array1DUtil.arrayToIntArray(sourceArray, sourceImage.isSignedDataType());
		}
		else
		{
			imageSourceDataBuffer = sourceImage.getDataXYAsInt(0);
		}
		
//		System.out.println("threshold= "+ options.simplethreshold+ "  ifGreater=" + options.ifGreater);
//		System.out.println("true= "+ options.byteFALSE+ "  false=" + options.byteTRUE);
		
		byte on = options.byteTRUE;
		byte off = options.byteFALSE;
		if (!options.ifGreater) 
		{
			off = options.byteTRUE;
			on = options.byteFALSE;
		}
		
		for (int x = 0; x < binaryMapDataBuffer.length; x++)  
		{
			int val = imageSourceDataBuffer[x] & 0xFF;
			if (val > options.simplethreshold)
				binaryMapDataBuffer[x] = off;
			else
				binaryMapDataBuffer[x] = on;
		}
		
		return binaryMap;
	}

}


