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
	private ImageTransform 			imgTransf 	= new ImageTransform();
	private ImageThreshold 			imgThresh 	= new ImageThreshold();
	public ImageOperationsStruct 	cacheThresholdOp 		= new ImageOperationsStruct();
	public ImageOperationsStruct 	cacheTransformOp 		= new ImageOperationsStruct();
	public IcyBufferedImage 		cacheTransformedImage 	= null;
	public IcyBufferedImage 		cacheThresholdedImage 	= null;
	
	public ImageOperations (SequenceCamData seq) 
	{
		setSequence(seq);
	}
	
	public void setSequence(SequenceCamData seq) 
	{
		this.seqCamData = seq;
		imgTransf.setSequence(seq);
	}
	
	public void setTransform (EnumImageTransformations transformop) 
	{
		opTransf = transformop.getFunction();
	}
	
	public void setThresholdSingle( int threshold, boolean ifGreater) 
	{
		opThresh.thresholdtype = EnumThresholdType.SINGLE;
		opThresh.simplethreshold = threshold;
		imgThresh.setSingleThreshold(threshold, ifGreater);
	}
	
	public void setColorArrayThreshold (ArrayList <Color> colorarray, int distanceType, int colorthreshold) 
	{
		opThresh.thresholdtype = EnumThresholdType.COLORARRAY;
		opThresh.colorarray = colorarray;
		opThresh.colordistanceType = distanceType;
		opThresh.colorthreshold = colorthreshold;
		imgThresh.setColorArrayThreshold(distanceType, colorthreshold, colorarray);
	}
	
	public IcyBufferedImage runImageOperation() 
	{
		return runImageOperationFrame (seqCamData.currentFrame);
	}
	
	private IcyBufferedImage runImageOperationFrame (int frame) 
	{	
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
	
	public IcyBufferedImage run_nocache() 
	{
		// step 1
		int frame = seqCamData.currentFrame;
		IcyBufferedImage transformedImage = imgTransf.transformImageFromVirtualSequence(frame, opTransf.transformop);
		if (transformedImage == null)
			return null;
		
		// step 2
		IcyBufferedImage thresholdedImage;
		if (opThresh.thresholdtype == EnumThresholdType.COLORARRAY)
			thresholdedImage = imgThresh.getBinaryInt_FromColorsThreshold(transformedImage); 
		else 
			thresholdedImage = imgThresh.getBinaryInt_FromThreshold(transformedImage);
		return thresholdedImage;
	}

	public boolean[] convertToBoolean(IcyBufferedImage binaryMap) 
	{
		return imgThresh.getBoolMap_FromBinaryInt(binaryMap);
	}
	
	protected final byte byteFALSE = 0;
	
	public boolean[] getBoolMap_FromBinaryInt(IcyBufferedImage img) 
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
