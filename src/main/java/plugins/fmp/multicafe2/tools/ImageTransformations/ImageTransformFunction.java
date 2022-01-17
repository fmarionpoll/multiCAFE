package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;


public abstract class ImageTransformFunction 
{
	String label = null;
	public String getName() 
	{
		return label;
	}
	
	protected void copyExGIntToIcyBufferedImage(int[] ExG, IcyBufferedImage img2 )
	{
		Array1DUtil.intArrayToSafeArray(ExG,  img2.getDataXY(0), false, false); //true);
		img2.setDataXY(0, img2.getDataXY(0));
		for (int c = 1; c < 3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
	}
	
	protected void copyExGDoubleToIcyBufferedImage(double[] ExG, IcyBufferedImage img2 )
	{
		Array1DUtil.doubleArrayToSafeArray(ExG, img2.getDataXY(0), false); 
		img2.setDataXY(0, img2.getDataXY(0));
		for (int c = 1; c < 3; c++ ) 
		{
			img2.copyData(img2, 0, c);
			img2.setDataXY(c, img2.getDataXY(c));
		}
	}
	
}
