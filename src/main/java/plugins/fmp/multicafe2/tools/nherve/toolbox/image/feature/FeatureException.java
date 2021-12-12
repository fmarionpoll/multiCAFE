package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

/**
 * The Class FeatureException.
 * 
 * @author Nicolas HERVE - nherve@ina.fr
 */
public class FeatureException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 7211467602505697543L;

	/**
	 * Instantiates a new feature exception.
	 */
	public FeatureException() {
	}

	/**
	 * Instantiates a new feature exception.
	 * 
	 * @param message
	 *            the message
	 */
	public FeatureException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new feature exception.
	 * 
	 * @param cause
	 *            the cause
	 */
	public FeatureException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new feature exception.
	 * 
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public FeatureException(String message, Throwable cause) {
		super(message, cause);
	}

}
