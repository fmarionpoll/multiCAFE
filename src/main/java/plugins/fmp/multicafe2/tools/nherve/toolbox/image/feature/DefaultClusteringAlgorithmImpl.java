package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.util.Arrays;

import plugins.fmp.multicafe2.tools.nherve.toolbox.Algorithm;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.clustering.ClusteringException;


/**
 * The Class DefaultClusteringAlgorithmImpl.
 * 
 * @param <T>
 *            the generic type
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public abstract class DefaultClusteringAlgorithmImpl<T extends Signature> extends Algorithm implements ClusteringAlgorithm<T> {
	
	/**
	 * Instantiates a new default clustering algorithm impl.
	 * 
	 * @param display
	 *            the display
	 */
	public DefaultClusteringAlgorithmImpl(boolean display) {
		super(display);
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.ClusteringAlgorithm#getAffectations(T[])
	 */
	public int[] getAffectations(T[] points) throws ClusteringException {
		return getAffectations(Arrays.asList(points));
	}
	
	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.ClusteringAlgorithm#compute(T[])
	 */
	public void compute(T[] points) throws ClusteringException {
		compute(Arrays.asList(points));
	}
}
