package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.clustering;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.FeatureException;

/**
 * The Class ClusteringException.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class ClusteringException extends FeatureException {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -656820396420409772L;

	/**
	 * Instantiates a new clustering exception.
	 */
	public ClusteringException() {
	}

	/**
	 * Instantiates a new clustering exception.
	 * 
	 * @param message
	 *            the message
	 */
	public ClusteringException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new clustering exception.
	 * 
	 * @param cause
	 *            the cause
	 */
	public ClusteringException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new clustering exception.
	 * 
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public ClusteringException(String message, Throwable cause) {
		super(message, cause);
	}

}
