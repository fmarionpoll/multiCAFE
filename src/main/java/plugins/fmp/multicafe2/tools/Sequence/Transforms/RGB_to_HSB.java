package plugins.fmp.multicafe2.tools.Sequence.Transforms;

import java.awt.Color;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.VolumetricImage;
//import icy.sequence.VolumetricImageCursor;
import icy.type.collection.array.Array1DUtil;
import icy.type.collection.array.Array2DUtil;
import plugins.fmp.multicafe2.tools.Sequence.SequenceTransformFunction;
import plugins.fmp.multicafe2.tools.Sequence.SequenceTransformInterface;
import plugins.fmp.multicafe2.tools.Sequence.SequenceTransformOptions;

public class RGB_to_HSB extends SequenceTransformFunction implements SequenceTransformInterface
{
	int channelOut = -1;
	int zIn = 0;
	int zOut = 1;
	
	public RGB_to_HSB(int channelOut)
	{
		this.channelOut = channelOut;
	}
	
	@Override
	public void getTransformedSequence(Sequence colorSeq, int t, SequenceTransformOptions options) 
	{ 	        
		VolumetricImage colorVol = colorSeq.getVolumetricImage(t);
		IcyBufferedImage img0 = colorVol.getImage(0);
		IcyBufferedImage img1 = colorVol.getImage(1);
		
//		int[][] tabValues = Array2DUtil.arrayToIntArray(img0.getDataXYCAsInt(), img0.isSignedDataType());
		float[][] tabValues = Array2DUtil.arrayToFloatArray(img0.getDataXYCAsFloat(), img0.isSignedDataType());
		float[][] outValues = Array2DUtil.arrayToFloatArray(img1.getDataXYCAsFloat(), img1.isSignedDataType());
		
		// compute values
		for (int ky = 0; ky < tabValues[0].length; ky++) 
		{
			float[] hsb = Color.RGBtoHSB((int)tabValues[0][ky], (int)tabValues[1][ky], (int)tabValues[2][ky], null) ;
			outValues[0] [ky] = hsb[0] * 100;
			outValues[1] [ky] = hsb[1] * 100;
			outValues[2] [ky] = hsb[2] * 100;
		}
		setPixelsOut(img1, outValues);
	}
	
	void setPixelsOut(IcyBufferedImage bufferedImage, float[][] outValues) {
		if (channelOut < 0) {
			setOneChannel(bufferedImage, 0, outValues[0]);
			setOneChannel(bufferedImage, 1, outValues[1]);
			setOneChannel(bufferedImage, 2, outValues[2]);
			}
		else {
			setOneChannel(bufferedImage, channelOut, outValues[channelOut]);
			for (int c=0; c < 3; c++)
				if (c != channelOut) 
					bufferedImage.setDataXY(c, bufferedImage.getDataXY(channelOut));
		}
	}
	
	void setOneChannel (IcyBufferedImage bufferedImage, int c, float[] outValues) {
		Array1DUtil.floatArrayToSafeArray(outValues,  bufferedImage.getDataXY(c), false); 
		bufferedImage.setDataXY(c, bufferedImage.getDataXY(c));
	}

//	VolumetricImageCursor colorVolCursor = new VolumetricImageCursor(colorVol);
//    
//for (int iy = 0; iy < colorSeq.getSizeY(); iy++) // Y
//{
//for (int ix = 0; ix < colorSeq.getSizeX(); ix++) // X
//{
//	int R = (int) colorVolCursor.get(ix, iy, zIn, 0);
//	int G = (int) colorVolCursor.get(ix, iy, zIn, 1);
//	int B = (int) colorVolCursor.get(ix, iy, zIn, 2);
//	float[] hsb = Color.RGBtoHSB(R, G, B, null) ;
//	
//    setPixelOut(colorVolCursor, ix, iy, hsb);
//}
//}
//colorVolCursor.commitChanges();

//void setPixelOut(VolumetricImageCursor colorVolCursor, int ix, int iy, float[] hsb) 
//{
//if (channelOut < 0)
//for (int c = 0; c < 3; c++)
//	colorVolCursor.setSafe(ix, iy, zOut, c, hsb[c]*100);
//else 
//for (int c = 0; c < 3; c++)
//	colorVolCursor.setSafe(ix, iy, zOut, c, hsb[channelOut]*100);
//}

}
