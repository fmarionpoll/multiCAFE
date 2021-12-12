package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.util.List;

import plugins.fmp.multicafe2.tools.nherve.toolbox.AbleToLogMessages;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.clustering.ClusteringException;


/**
 * The Interface ClusteringAlgorithm.
 * 
 * @param <T>
 *            the generic type
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public interface ClusteringAlgorithm<T extends Signature> extends AbleToLogMessages {
	
	/**
	 * Gets the nb classes.
	 * 
	 * @return the nb classes
	 */
	int getNbClasses();
	
	/**
	 * Compute.
	 * 
	 * @param points
	 *            the points
	 * @throws ClusteringException
	 *             the clustering exception
	 */
	void compute(List<T> points) throws ClusteringException;
	
	/**
	 * Compute.
	 * 
	 * @param points
	 *            the points
	 * @throws ClusteringException
	 *             the clustering exception
	 */
	void compute(T[] points) throws ClusteringException;
	
	/**
	 * Gets the centroids.
	 * 
	 * @return the centroids
	 * @throws ClusteringException
	 *             the clustering exception
	 */
	List<T> getCentroids() throws ClusteringException;
	
	/**
	 * Gets the affectations.
	 * 
	 * @param points
	 *            the points
	 * @return the affectations
	 * @throws ClusteringException
	 *             the clustering exception
	 */
	int[] getAffectations(List<T> points) throws ClusteringException;
	
	/**
	 * Gets the affectations.
	 * 
	 * @param points
	 *            the points
	 * @return the affectations
	 * @throws ClusteringException
	 *             the clustering exception
	 */
	int[] getAffectations(T[] points) throws ClusteringException;
}

