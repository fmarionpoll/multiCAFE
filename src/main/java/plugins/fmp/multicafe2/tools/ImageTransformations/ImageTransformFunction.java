package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;


abstract class ImageTransformFunction 
{
	String label = null;
	public String getName() 
	{
		return label;
	}
	
	public IcyBufferedImage transformImage (IcyBufferedImage sourceImage, ImageTransformOptions options)
	{
		return null;
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
}
