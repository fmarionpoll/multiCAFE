package plugins.fmp.multicafe2.tools;

import java.awt.Color;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.experiment.SequenceCamData;



public class ImageTransformTools 
{
	public 	IcyBufferedImage 	referenceImage = null;
	private int 				spanDiff = 3;
	private SequenceCamData 	seqCamData 	= null;
	
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
		case NONE: 
		case COLORARRAY1: /*System.out.println("transform image - " + transformop);*/
			transformedImage = inputImage;
			break;
		
		case R_RGB: 	
			transformedImage= functionRGB_keepOneChan(inputImage, 0); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case G_RGB: 	
			transformedImage= functionRGB_keepOneChan(inputImage, 1); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case B_RGB: 	
			transformedImage= functionRGB_keepOneChan(inputImage, 2); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case RGB: 		
			transformedImage= functionRGB_grey (inputImage); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		
		case H_HSB: 	
			transformedImage= functionRGBtoHSB(inputImage, 0); 
			transformImage(transformedImage, EnumTransformOp.RTOGB);
			break;
		case S_HSB: 	
			transformedImage= functionRGBtoHSB(inputImage, 1); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case B_HSB: 	
			transformedImage= functionRGBtoHSB(inputImage, 2); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;

		case R2MINUS_GB: 
			transformedImage= functionRGB_2C3MinusC1C2 (inputImage, 1, 2, 0); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case G2MINUS_RB: 
			transformedImage= functionRGB_2C3MinusC1C2 (inputImage, 0, 2, 1); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case B2MINUS_RG: 
			transformedImage= functionRGB_2C3MinusC1C2 (inputImage, 0, 1, 2); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case GBMINUS_2R: 
			transformedImage= functionRGB_C1C2minus2C3 (inputImage, 1, 2, 0); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case RBMINUS_2G: 
			transformedImage= functionRGB_C1C2minus2C3 (inputImage, 0, 2, 1); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case RGMINUS_2B: 
			transformedImage= functionRGB_C1C2minus2C3 (inputImage, 0, 1, 2); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
			
		case RGB_DIFFS:
			transformedImage= functionRGB_sumDiff (inputImage);
			break;

		case NORM_BRMINUSG: 
			transformedImage= functionNormRGB_sumC1C2Minus2C3(inputImage, 1, 2, 0); 
			transformImage(transformedImage, EnumTransformOp.RTOGB); 
			break;
		case RTOGB: 	
			transformedImage= functionTransferRedToGreenAndBlue(inputImage); 
			break;
			
		case REF_T0: 	
			transformedImage= functionSubtractRef(inputImage); 
			break;
		case REF: 		
			transformedImage= functionSubtractRef(inputImage); 
			break;
		case REF_PREVIOUS: 
			int t = seqCamData.currentFrame;
			if (t > 0) 
			{
				referenceImage = seqCamData.getSeqImage(t-1, 0); 
				transformedImage= functionSubtractRef(inputImage);
				} 
			break;	
		case XDIFFN: 	
			transformedImage= computeXDiffn (inputImage); 
			break;
		case YDIFFN: 	
			transformedImage= computeYDiffn (inputImage); 
			break;		
		case XYDIFFN: 	
			transformedImage= computeXYDiffn (inputImage); 
			break;

		case RGB_TO_HSV: 
			transformedImage= functionRGBtoHSV(inputImage); 
			break;
		case RGB_TO_H1H2H3: 
			transformedImage= functionRGBtoH1H2H3(inputImage); 
			break;
		case SUBFIRSTCOL: 
			transformedImage= functionSubtractCol(inputImage, 0); 
			break;
		}	
		return transformedImage;
	}
	
	public IcyBufferedImage transformImageFromVirtualSequence (int t, EnumTransformOp transformop) {
		return transformImage(seqCamData.getSeqImage(t, 0), transformop);
	}
	
	private IcyBufferedImage functionSubtractCol(IcyBufferedImage sourceImage, int column) 
	{
		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();
		IcyBufferedImage img2 = new IcyBufferedImage(imageSizeX, imageSizeY, chan1, sourceImage.getDataType_());
		for (int c=chan0; c < chan1; c++) 
		{
			int[] tabValues = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(c), sourceImage.isSignedDataType());
			int[] outValues = Array1DUtil.arrayToIntArray(img2.getDataXY(c), img2.isSignedDataType());			
			for (int iy = 0; iy < imageSizeY; iy++) 
			{	
				int deltay = iy* imageSizeX;
				int kx = column + deltay;
				int refVal = tabValues [kx];
				for (int ix =spanDiff; ix < imageSizeX -spanDiff; ix++) 
				{
					kx = ix + deltay;
					int outVal = tabValues [kx] - refVal;
					outValues [kx] = (int) Math.abs(outVal);
				}
			}
			Array1DUtil.intArrayToSafeArray(outValues, img2.getDataXY(c), sourceImage.isSignedDataType(), img2.isSignedDataType());
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}
	
	// function proposed by Fran�ois Rebaudo
	private IcyBufferedImage functionNormRGB_sumC1C2Minus2C3 (IcyBufferedImage sourceImage, int Rlayer, int Glayer, int Blayer) 
	{
		IcyBufferedImage img2 = new IcyBufferedImage (sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		double[] Rn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Rlayer), sourceImage.isSignedDataType());
		double[] Gn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Glayer), sourceImage.isSignedDataType());
		double[] Bn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Blayer), sourceImage.isSignedDataType());
		double[] ExG = (double[]) Array1DUtil.createArray(DataType.DOUBLE, Rn.length);
		for (int i=0; i< Rn.length; i++) 
		{
			double sum = (Rn[i] / 255) + (Gn[i] / 255) + (Bn [i] / 255);
			ExG[i] = ((Gn[i] *2 / 255 / sum) - (Rn[i] / 255/sum) - (Bn [i] / 255/sum)) * 255;
		}
		
		Array1DUtil.doubleArrayToSafeArray(ExG,  img2.getDataXY(0), false); //true);
		img2.setDataXY(0, img2.getDataXY(0));
		for (int c= 1; c<3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}
	
	private IcyBufferedImage functionTransferRedToGreenAndBlue(IcyBufferedImage sourceImage) 
	{
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		for (int c= 0; c<3; c++ ) 
		{
			img2.copyData(sourceImage, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}
	
	private IcyBufferedImage functionRGB_2C3MinusC1C2 (IcyBufferedImage sourceImage, int addchan1, int addchan2, int subtractchan3) 
	{	
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		double[] tabSubtract = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(subtractchan3), sourceImage.isSignedDataType());
		double[] tabAdd1 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(addchan1), sourceImage.isSignedDataType());
		double[] tabAdd2 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(addchan2), sourceImage.isSignedDataType());
		double[] tabResult =  (double[]) Array1DUtil.createArray(DataType.DOUBLE, tabSubtract.length);
		for (int i = 0; i < tabResult.length; i++) 
		{	
			double val = tabSubtract[i]* 2 - tabAdd1[i] - tabAdd2[i] ;
			tabResult [i] = val;
		}
		Array1DUtil.doubleArrayToSafeArray(tabResult, img2.getDataXY(0), false); //  true);
		img2.setDataXY(0, img2.getDataXY(0));
		for (int c= 1; c<3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2; 
	}
	
	private IcyBufferedImage functionRGB_C1C2minus2C3 (IcyBufferedImage sourceImage, int addchan1, int addchan2, int subtractchan3) 
	{
		if (sourceImage.getSizeC() < 3)
			return null;
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		double[] tabSubtract = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(subtractchan3), sourceImage.isSignedDataType());
		double[] tabAdd1 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(addchan1), sourceImage.isSignedDataType());
		double[] tabAdd2 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(addchan2), sourceImage.isSignedDataType());
		double[] tabResult = (double[]) Array1DUtil.createArray(DataType.DOUBLE, tabSubtract.length);

		for (int i = 0; i < tabResult.length; i++) 
			tabResult [i] =  tabAdd1[i] + tabAdd2[i] - tabSubtract[i]* 2;
		
		Array1DUtil.doubleArrayToSafeArray(tabResult, img2.getDataXY(0), false); //  true);
		img2.setDataXY(0, img2.getDataXY(0));
		for (int c= 1; c<3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}
	
private IcyBufferedImage functionRGB_sumDiff (IcyBufferedImage sourceImage) 
{
		if (sourceImage.getSizeC() < 3)
			return null;
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		int c = 0;
		int Rlayer = c;
		int[] Rn = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(Rlayer), sourceImage.isSignedDataType());
		int Glayer = c+1;
		int[] Gn = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(Glayer), sourceImage.isSignedDataType());
		int Blayer = c+2;
		int[] Bn = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(Blayer), sourceImage.isSignedDataType());
		int[] ExG = (int[]) Array1DUtil.createArray(DataType.INT, Rn.length);
	
		for (int i=0; i< Rn.length; i++) 
		{
			int diff1 = Math.abs(Rn[i]-Bn[i]);
			int diff2 = Math.abs(Rn[i]-Gn[i]);
			int diff3 = Math.abs(Bn[i]-Gn[i]);
			ExG[i] = diff1+diff2+diff3; //Math.max(diff3, Math.max(diff1,  diff2));
		}
		
		Array1DUtil.intArrayToSafeArray(ExG,  img2.getDataXY(0), false, false); //true);
		img2.setDataXY(0, img2.getDataXY(0));
		for (c= 1; c<3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}
	
	private IcyBufferedImage computeXDiffn(IcyBufferedImage sourceImage) 
	{
		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();
		IcyBufferedImage img2 = new IcyBufferedImage(imageSizeX, imageSizeY, 3, sourceImage.getDataType_());
		for (int c=chan0; c < chan1; c++) 
		{
			int[] tabValues = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(c), sourceImage.isSignedDataType());
			int[] outValues = Array1DUtil.arrayToIntArray(img2.getDataXY(c), img2.isSignedDataType());			
			for (int iy = 0; iy < imageSizeY; iy++) 
			{	
				// erase border values
				for (int ix = 0; ix < spanDiff; ix++) 
					outValues[ix + iy* imageSizeX] = 0;
				// compute values
				int deltay = iy* imageSizeX;
				for (int ix =spanDiff; ix < imageSizeX -spanDiff; ix++) 
				{
					int kx = ix + deltay;
					int deltax =  0;
					double outVal = 0;
					for (int ispan = 1; ispan < spanDiff; ispan++) 
					{
						deltax += 1; 
						outVal += tabValues [kx+deltax] - tabValues[kx-deltax];
					}
					outValues [kx] = (int) Math.abs(outVal);
				}
				// erase border values
				for (int ix = imageSizeX-spanDiff; ix < imageSizeX; ix++) 
					outValues[ix + iy* imageSizeX] = 0;
			}
			Array1DUtil.intArrayToSafeArray(outValues, img2.getDataXY(c), true, img2.isSignedDataType());
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}
	
	private IcyBufferedImage computeYDiffn(IcyBufferedImage sourceImage) 
	{
		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();
		IcyBufferedImage img2 = new IcyBufferedImage(imageSizeX, imageSizeY, 3, sourceImage.getDataType_());
		
		for (int c=chan0; c < chan1; c++) 
		{
			int [] tabValues = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(c), sourceImage.isSignedDataType());
			int [] outValues = Array1DUtil.arrayToIntArray(img2.getDataXY(c), img2.isSignedDataType());			
			for (int ix = spanDiff; ix < imageSizeX - spanDiff; ix++) 
			{	
				for (int iy =spanDiff; iy < imageSizeY -spanDiff; iy++) 
				{
					int kx = ix +  iy* imageSizeX;
					int deltax =  0;
					double outVal = 0;
					for (int ispan = 1; ispan < spanDiff; ispan++) 
					{
						deltax += imageSizeX; 
						outVal += tabValues [kx+deltax] - tabValues[kx-deltax];
					}
					outValues [kx] = (int) Math.abs(outVal);
				}
			}
			Array1DUtil.intArrayToSafeArray(outValues, img2.getDataXY(c), true, img2.isSignedDataType());
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}

	private IcyBufferedImage computeXYDiffn(IcyBufferedImage sourceImage) 
	{
		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();
		IcyBufferedImage img2 = new IcyBufferedImage(imageSizeX, imageSizeY, 1, sourceImage.getDataType_());
		
		for (int c=chan0; c < chan1; c++) 
		{
			int[] tabValues = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(c), sourceImage.isSignedDataType());
			int[] outValues = Array1DUtil.arrayToIntArray(img2.getDataXY(c), img2.isSignedDataType());				
			for (int ix =0; ix < imageSizeX; ix++) 
			{	
				for (int iy = spanDiff; iy < imageSizeY-spanDiff; iy++) 
				{
					int ky = ix + iy* imageSizeX;
					int deltay =  0;
					double outVal = 0;
					// loop vertically
					for (int ispan = 1; ispan < spanDiff; ispan++) 
					{
						deltay += imageSizeX;
						outVal += tabValues [ky+deltay] - tabValues[ky-deltay];
					}

					// loop horizontally
					int deltax = 0;
					int yspan2 = 10;
					if (ix >yspan2 && ix < imageSizeX - yspan2) 
					{
						for (int ispan = 1; ispan < yspan2; ispan++) 
						{
							deltax += 1;
							outVal += tabValues [ky+deltax] - tabValues[ky-deltax];
						}
					}
					outValues [ky] = (int) Math.abs(outVal);
				}

				// erase out-of-bounds points
				for (int iy = 0; iy < spanDiff; iy++) 
					outValues[ix + iy* imageSizeX] = 0;

				for (int iy = imageSizeY-spanDiff; iy < imageSizeY; iy++) 
					outValues[ix + iy* imageSizeX] = 0;
			}
			Array1DUtil.intArrayToSafeArray(outValues,  img2.getDataXY(c), true, img2.isSignedDataType());
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}
	
	private IcyBufferedImage functionRGB_keepOneChan (IcyBufferedImage sourceImage, int keepChan) 
	{
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		img2.copyData(sourceImage, keepChan, 0);
		img2.setDataXY(0, img2.getDataXY(0));
		for (int c= 1; c<3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
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
		
		int c= 0;
		Object dataArray = img2.getDataXY(c);
		Array1DUtil.intArrayToSafeArray(outValues0, dataArray, sourceImage.isSignedDataType(), img2.isSignedDataType());
		img2.setDataXY(c, img2.getDataXY(c));
		for (c= 1; c<3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
		return img2;
	}
	
	private IcyBufferedImage functionRGBtoHSB(IcyBufferedImage sourceImage, int xHSB) 
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
			double val = (double) hsb[xHSB] * 100;
			outValues0 [ky] = val;
			outValues1 [ky] = val;
			outValues2 [ky] = val;
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
	
	private IcyBufferedImage functionSubtractRef(IcyBufferedImage sourceImage) {	
		if (referenceImage == null)
			referenceImage = seqCamData.getSeqImage(0, 0);
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(),sourceImage.getSizeC(), sourceImage.getDataType_());
		for (int c=0; c<sourceImage.getSizeC(); c++) 
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
	
	private IcyBufferedImage functionRGBtoHSV (IcyBufferedImage sourceImage) 
	{
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		
		double[] tabValuesR = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		double[] tabValuesG = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		double[] tabValuesB = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());

		double[] outValues0 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(0), img2.isSignedDataType());
		double[] outValues1 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(1), img2.isSignedDataType());
		double[] outValues2 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(2), img2.isSignedDataType());
		
		// compute values
		float [] hsb = new float [3];
		for (int ky = 0; ky < tabValuesR.length; ky++) 
		{
			int R = (int) tabValuesR[ky];
			int G = (int) tabValuesG[ky];
			int B = (int) tabValuesB[ky];			
			hsb = Color.RGBtoHSB(R, G, B, hsb) ;
			outValues0 [ky] = (double) hsb[0] ;
			outValues1 [ky] = (double) hsb[1] ;
			outValues2 [ky] = (double) hsb[2] ;
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

	private IcyBufferedImage functionRGBtoH1H2H3 (IcyBufferedImage sourceImage) 
	{
		
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		
		double[] tabValuesR = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		double[] tabValuesG = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		double[] tabValuesB = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());

		double[] outValues0 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(0), img2.isSignedDataType());
		double[] outValues1 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(1), img2.isSignedDataType());
		double[] outValues2 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(2), img2.isSignedDataType());
		
		// compute values
		final double VMAX = 255.0;
		for (int ky = 0; ky < tabValuesR.length; ky++) 
		{
			int r = (int) tabValuesR[ky];
			int g = (int) tabValuesG[ky];
			int b = (int) tabValuesB[ky];
			
			outValues0 [ky] = (r + g) / 2.0;
			outValues1 [ky] = (VMAX + r - g) / 2.0;
			outValues2 [ky] = (VMAX + b - (r + g) / 2.0) / 2.0;
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
