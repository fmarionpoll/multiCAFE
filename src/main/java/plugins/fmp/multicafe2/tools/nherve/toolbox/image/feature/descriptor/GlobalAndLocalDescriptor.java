package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.descriptor;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Segmentable;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Signature;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.FullImageSupportRegion;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.IcyPixel;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;


public abstract class GlobalAndLocalDescriptor<T extends Segmentable, S extends Signature> 
				extends DefaultDescriptorImpl<T, S> 
				implements GlobalDescriptor<T, S>, LocalDescriptor<T, S, IcyPixel> {
	
	public GlobalAndLocalDescriptor(boolean display) {
		super(display);
	}
	
	@Override
	public S extractGlobalSignature(T img) throws SignatureException {
		return extractLocalSignature(img, new FullImageSupportRegion(img));
	}
}