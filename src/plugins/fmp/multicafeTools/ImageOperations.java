package plugins.fmp.multicafeTools;

import java.awt.Color;
import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class ImageOperations {
	
	private SequenceCamData 		seqCamData 	= null;
	private ImageOperationsStruct 	opTransf 	= new ImageOperationsStruct();
	private ImageOperationsStruct 	opThresh 	= new ImageOperationsStruct();
	private ImageTransformTools 	imgTransf 	= new ImageTransformTools();
	private ImageThresholdTools 	imgThresh 	= new ImageThresholdTools();
	
	public ImageOperations (SequenceCamData seq) {
		setSequence(seq);
	}
	
	public void setSequence(SequenceCamData seq) {
		this.seqCamData = seq;
		imgTransf.setSequence(seq);
	}
	
	public void setTransform (TransformOp transformop) {
		opTransf.transformop = transformop;
	}
	
	public void setThresholdSingle( int threshold, boolean ifGreater) {
		opThresh.thresholdtype = EnumThresholdType.SINGLE;
		opThresh.simplethreshold = threshold;
		imgThresh.setSingleThreshold(threshold, ifGreater);
	}
	
	public void setColorArrayThreshold (ArrayList <Color> colorarray, int distanceType, int colorthreshold) {
		opThresh.thresholdtype = EnumThresholdType.COLORARRAY;
		opThresh.colorarray = colorarray;
		opThresh.colordistanceType = distanceType;
		opThresh.colorthreshold = colorthreshold;
		imgThresh.setColorArrayThreshold(distanceType, colorthreshold, colorarray);
	}
	
	public IcyBufferedImage run() {
		return run (seqCamData.currentFrame);
	}
	
	public IcyBufferedImage run (int frame) {	
		// step 1
		opTransf.fromFrame = frame;
		if (!opTransf.isValidTransformCache(seqCamData.cacheTransformOp)) {
			seqCamData.cacheTransformedImage = imgTransf.transformImageFromVirtualSequence(frame, opTransf.transformop);
			if (seqCamData.cacheTransformedImage == null) 
				return null;
			opTransf.copyTransformOpTo(seqCamData.cacheTransformOp);
			seqCamData.cacheThresholdOp.fromFrame = -1;
		}
		
		// step 2
		opThresh.fromFrame = frame;
		if (!opThresh.isValidThresholdCache(seqCamData.cacheThresholdOp)) {
			if (opThresh.thresholdtype == EnumThresholdType.COLORARRAY) 
				seqCamData.cacheThresholdedImage = imgThresh.getBinaryInt_FromColorsThreshold(seqCamData.cacheTransformedImage); 
			else 
				seqCamData.cacheThresholdedImage = imgThresh.getBinaryInt_FromThreshold(seqCamData.cacheTransformedImage);
			opThresh.copyThresholdOpTo(seqCamData.cacheThresholdOp) ;
		}
		return seqCamData.cacheThresholdedImage;
	}
	
	public IcyBufferedImage run_nocache() {
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

	public boolean[] convertToBoolean(IcyBufferedImage binaryMap) {
		return imgThresh.getBoolMap_FromBinaryInt(binaryMap);
	}
}
