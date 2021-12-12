package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.FeatureException;

/**
 * The Class SignatureException.
 * 
 * @author Nicolas HERVE - nherve@ina.fr
 */
public class SignatureException extends FeatureException {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 8090763616946786075L;

	/**
	 * Instantiates a new signature exception.
	 */
	public SignatureException() {
	}

	/**
	 * Instantiates a new signature exception.
	 * 
	 * @param message
	 *            the message
	 */
	public SignatureException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new signature exception.
	 * 
	 * @param cause
	 *            the cause
	 */
	public SignatureException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new signature exception.
	 * 
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public SignatureException(String message, Throwable cause) {
		super(message, cause);
	}

}
