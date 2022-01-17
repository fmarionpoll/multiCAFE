package plugins.fmp.multicafe2.tools;


import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.experiment.SequenceCamData;



public class ImageTransform 
{
	public 	IcyBufferedImage 	referenceImage 	= null;
	private int 				spanDiff 		= 5;
	private SequenceCamData 	seqCamData 		= null;
	
	// -------------------------------------
	public void setReferenceImage(IcyBufferedImage img) 
	{
		referenceImage = IcyBufferedImageUtil.getCopy(img);
	}
	
	public void setSpanDiff(int spanDiff) 
	{
		this.spanDiff = spanDiff;
	}
	
	public int getSpanDiff () 
	{
		return spanDiff;
	}
	
	public void setSequence (SequenceCamData vinputSeq) 
	{
		seqCamData = vinputSeq;
	}
		
	public IcyBufferedImage transformImage (IcyBufferedImage inputImage, EnumTransformOp transformop) 
	{
		IcyBufferedImage transformedImage = null;
		switch (transformop) 
		{		

		case REF_T0: 	
			transformedImage = functionSubtractRef(inputImage); 
			break;
		case REF: 		
			transformedImage = functionSubtractRef(inputImage); 
			break;
		case REF_PREVIOUS: 
			int t = seqCamData.currentFrame;
			if (t > 0) 
			{
				referenceImage = seqCamData.getSeqImage(t-1, 0); 
				transformedImage = functionSubtractRef(inputImage);
			} 
			break;	
			
		case NONE: 
		default:
			transformedImage = inputImage;
			break;
		}	
		return transformedImage;
	}
	
	public IcyBufferedImage transformImageFromVirtualSequence (int t, EnumTransformOp transformop) 
	{
		return transformImage(seqCamData.getSeqImage(t, 0), transformop);
	}

	private IcyBufferedImage functionSubtractRef(IcyBufferedImage sourceImage) {	
		if (referenceImage == null)
			referenceImage = seqCamData.getSeqImage(0, 0);
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(),sourceImage.getSizeC(), sourceImage.getDataType_());
		for (int c = 0; c < sourceImage.getSizeC(); c++) 
		{
			int [] imgSourceInt = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
			int [] img2Int = Array1DUtil.arrayToIntArray(img2.getDataXY(0), img2.isSignedDataType());
			int [] imgReferenceInt = Array1DUtil.arrayToIntArray(referenceImage.getDataXY(0), referenceImage.isSignedDataType());	
			for (int i=0; i< imgSourceInt.length; i++) 
			{
				int val = imgSourceInt[i] - imgReferenceInt[i];
				if (val < 0) 
					val = -val;
				img2Int[i] = 0xFF - val;
			}
			Array1DUtil.intArrayToSafeArray(img2Int,  img2.getDataXY(c), true, img2.isSignedDataType());
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}
	
	
	
}
