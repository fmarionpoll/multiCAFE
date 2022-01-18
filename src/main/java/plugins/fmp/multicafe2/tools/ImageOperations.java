package plugins.fmp.multicafe2.tools;

import java.awt.Color;
import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;



public class ImageOperations 
{
	private SequenceCamData 		seqCamData 	= null;
	private ImageTransformInterface	opTransf 	= null;
	private ImageTransformInterface opThresh 	= null;

	public ImageOperationsStruct 	cacheThresholdOp 		= new ImageOperationsStruct();
	public ImageOperationsStruct 	cacheTransformOp 		= new ImageOperationsStruct();
	public IcyBufferedImage 		cacheTransformedImage 	= null;
	public IcyBufferedImage 		cacheThresholdedImage 	= null;
	

	
	/*
	public IcyBufferedImage runImageOperation() 
	{
		int frame = seqCamData.currentFrame;	
		if (frame < 0)
			frame = 0;
		// step 1
		opTransf.fromFrame = frame;
		if (!opTransf.isValidTransformCache(cacheTransformOp)) 
		{
			cacheTransformedImage = imgTransf.transformImageFromVirtualSequence(frame, opTransf.transformop);
			if (cacheTransformedImage == null) 
				return null;
			opTransf.copyTransformOpTo(cacheTransformOp);
			cacheThresholdOp.fromFrame = -1;
		}
		
		// step 2
		opThresh.fromFrame = frame;
		if (!opThresh.isValidThresholdCache(cacheThresholdOp)) 
		{
			if (opThresh.thresholdtype == EnumThresholdType.COLORARRAY) 
				cacheThresholdedImage = imgThresh.getBinaryInt_FromColorsThreshold(cacheTransformedImage); 
			else 
				cacheThresholdedImage = imgThresh.getBinaryInt_FromThreshold(cacheTransformedImage);
			opThresh.copyThresholdOpTo(cacheThresholdOp) ;
		}
		return cacheThresholdedImage;
	}
	*/
	protected final byte byteFALSE = 0;
	
	public boolean[] convertToBoolean(IcyBufferedImage img) 
	{
		boolean[]	boolMap = new boolean[ img.getSizeX() * img.getSizeY() ];
		byte [] imageSourceDataBuffer = null;
		DataType datatype = img.getDataType_();
		if (datatype != DataType.BYTE && datatype != DataType.UBYTE) 
		{
			Object sourceArray = img.getDataXY(0);
			imageSourceDataBuffer = Array1DUtil.arrayToByteArray(sourceArray);
		}
		else 
		{
			imageSourceDataBuffer = img.getDataXYAsByte(0);
		}
		
		for (int x = 0; x < boolMap.length; x++)  
		{
			if (imageSourceDataBuffer[x] == byteFALSE)
				boolMap[x] =  false;
			else
				boolMap[x] =  true;
		}
		return boolMap;
	}
}
