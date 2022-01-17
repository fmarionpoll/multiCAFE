package plugins.fmp.multicafe2.tools.ImageTransformations;

import icy.image.IcyBufferedImage;

public class ImageTransformOptions {
	public int xfirst;
	public int xlast;
	public int yfirst;
	public int ylast;
	public int channel0;
	public int channel1;
	public int channel2;
	public int w0 = 1;
	public int w1 = 1;
	public int w2 = 1;
	public int spanDiff = 3;
	public IcyBufferedImage referenceImage = null;
}
