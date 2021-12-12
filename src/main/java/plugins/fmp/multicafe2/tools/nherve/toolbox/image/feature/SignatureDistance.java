package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import plugins.fmp.multicafe2.tools.nherve.toolbox.Algorithm;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;

/**
 * The Class SignatureDistance.
 * 
 * @param <T>
 *            the generic type
 * @author Nicolas HERVE - nherve@ina.fr
 */
public abstract class SignatureDistance<T extends Signature> extends Algorithm implements Distance<T> {
	
	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.Distance#computeDistance(java.lang.Object, java.lang.Object)
	 */
	public abstract double computeDistance(T s1, T s2) throws SignatureException;
}
