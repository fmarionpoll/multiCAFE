package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.descriptor;

import java.awt.Shape;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Descriptor;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Segmentable;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Signature;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.SupportRegion;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.Pixel;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;


/**
 * The Interface LocalDescriptor.
 * 
 * @param <T>
 *            the generic type
 * @param <S>
 *            the generic type
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
@SuppressWarnings("rawtypes")
public interface LocalDescriptor<T extends Segmentable, S extends Signature, P extends Pixel> extends Descriptor<T> {
	
	/**
	 * Extract local signature.
	 * 
	 * @param img
	 *            the img
	 * @param reg
	 *            the reg
	 * @return the s
	 * @throws SignatureException
	 *             the signature exception
	 */
	S extractLocalSignature(T img, SupportRegion<P> reg) throws SignatureException;
	
	/**
	 * Extract local signature.
	 * 
	 * @param img
	 *            the img
	 * @param shp
	 *            the shp
	 * @return the s
	 * @throws SignatureException
	 *             the signature exception
	 */
	S extractLocalSignature(T img, Shape shp) throws SignatureException;
}
