package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.descriptor;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Segmentable;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Signature;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.FullImageSupportRegion;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.IcyPixel;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;

/**
 * The Class GlobalAndLocalDescriptor.
 * 
 * @param <T>
 *            the generic type
 * @param <S>
 *            the generic type
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public abstract class GlobalAndLocalDescriptor<T extends Segmentable, S extends Signature> extends DefaultDescriptorImpl<T, S> implements GlobalDescriptor<T, S>, LocalDescriptor<T, S, IcyPixel> {
	
	/**
	 * Instantiates a new global and local descriptor.
	 * 
	 * @param display
	 *            the display
	 */
	public GlobalAndLocalDescriptor(boolean display) {
		super(display);
	}
	
	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.descriptor.GlobalDescriptor#extractGlobalSignature(plugins.nherve.toolbox.image.feature.Segmentable)
	 */
	@Override
	public S extractGlobalSignature(T img) throws SignatureException {
		return extractLocalSignature(img, new FullImageSupportRegion(img));
	}
}