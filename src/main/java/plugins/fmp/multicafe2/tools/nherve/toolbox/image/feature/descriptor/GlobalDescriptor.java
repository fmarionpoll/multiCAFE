package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.descriptor;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Descriptor;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Segmentable;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Signature;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;

/**
 * The Interface GlobalDescriptor.
 * 
 * @param <T>
 *            the generic type
 * @param <S>
 *            the generic type
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public interface GlobalDescriptor<T extends Segmentable, S extends Signature> extends Descriptor<T> {
	
	/**
	 * Extract global signature.
	 * 
	 * @param img
	 *            the img
	 * @return the s
	 * @throws SignatureException
	 *             the signature exception
	 */
	S extractGlobalSignature(T img) throws SignatureException;
}
