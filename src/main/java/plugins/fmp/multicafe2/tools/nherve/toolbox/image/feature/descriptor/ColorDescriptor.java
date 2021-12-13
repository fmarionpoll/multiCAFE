package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.descriptor;

import icy.image.IcyBufferedImage;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.SegmentableIcyBufferedImage;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Signature;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.IcyPixel;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.toolboxes.ColorSpaceTools;



public abstract class ColorDescriptor<S extends Signature> 
		extends GlobalAndLocalDescriptor<SegmentableIcyBufferedImage, S> {
	

	private int colorSpace;
	private boolean useBounds;
	
	public ColorDescriptor(boolean display) {
		super(display);
		setColorSpace(ColorSpaceTools.RGB);
		setUseBounds(false);
	}

	public int getColorSpace() {
		return colorSpace;
	}

	public void setColorSpace(int colorSpace) {
		this.colorSpace = colorSpace;
	}
	
	public double[] getColorComponents_0_1(IcyBufferedImage icyb, int x, int y) throws SignatureException {
		if (useBounds) {
			return ColorSpaceTools.getBoundedColorComponentsD_0_1(icyb, getColorSpace(), x, y);
		}
		return ColorSpaceTools.getColorComponentsD_0_1(icyb, getColorSpace(), x, y);
	}
	
	public int getNbColorChannels() {
		return ColorSpaceTools.NB_COLOR_CHANNELS;
	}

	@Override
	public String toString() {
		return "ColorDescriptor " + ColorSpaceTools.COLOR_SPACES[getColorSpace()];
	}

	public double[] getColorComponentsManageBorders(IcyBufferedImage img, IcyPixel npx, int w, int h) throws SignatureException {
		double[] col = null;
		int x = (int)npx.x;
		int y = (int)npx.y;
	
		if (x < 0) {
			x = Math.abs(x);
		} else if (x >= w) {
			x -= 2 * (x - w + 1); 
		}
		
		if (y < 0) {
			y = Math.abs(y);
		} else if (y >= h) {
			y -= 2 * (y - h + 1); 
		}
	
		col = getColorComponents_0_1(img, x, y);
		return col;
	}

	public boolean isUseBounds() {
		return useBounds;
	}

	public void setUseBounds(boolean useBounds) {
		this.useBounds = useBounds;
	}
	
	@Override
	public boolean needToLoadSegmentable() {
		return true;
	}
}
