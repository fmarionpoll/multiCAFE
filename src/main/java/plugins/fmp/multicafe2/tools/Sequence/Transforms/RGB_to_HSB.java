package plugins.fmp.multicafe2.tools.Sequence.Transforms;

import java.awt.Color;

import icy.sequence.Sequence;
import icy.sequence.VolumetricImage;
import icy.sequence.VolumetricImageCursor;
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
		VolumetricImageCursor colorVolCursor = new VolumetricImageCursor(colorVol);
	    	    
	    for (int iy = 0; iy < colorSeq.getSizeY(); iy++) // Y
		{
		    for (int ix = 0; ix < colorSeq.getSizeX(); ix++) // X
		    {
		    	int R = (int) colorVolCursor.get(ix, iy, zIn, 0);
				int G = (int) colorVolCursor.get(ix, iy, zIn, 1);
				int B = (int) colorVolCursor.get(ix, iy, zIn, 2);
				float[] hsb = Color.RGBtoHSB(R, G, B, null) ;
				
		        setPixelOut(colorVolCursor, ix, iy, hsb);
		    }
		}
	    colorVolCursor.commitChanges();
	}
	
	void setPixelOut(VolumetricImageCursor colorVolCursor, int ix, int iy, float[] hsb) 
	{
		if (channelOut < 0)
			for (int c = 0; c < 3; c++)
				colorVolCursor.setSafe(ix, iy, zOut, c, hsb[c]);
		else 
			for (int c = 0; c < 3; c++)
				colorVolCursor.setSafe(ix, iy, zOut, c, hsb[channelOut]);
	}


}
