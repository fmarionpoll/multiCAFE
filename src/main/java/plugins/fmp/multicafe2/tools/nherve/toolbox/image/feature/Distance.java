package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;


/**
 * The Interface Distance.
 * 
 * @param <T>
 *            the generic type
 * @author Nicolas HERVE - nherve@ina.fr
 */
public interface Distance<T> {
	
	/**
	 * Compute distance.
	 * 
	 * @param s1
	 *            the s1
	 * @param s2
	 *            the s2
	 * @return the double
	 * @throws FeatureException
	 *             the feature exception
	 */
	double computeDistance(T s1, T s2) throws FeatureException;
}
