package plugins.fmp.multicafe2.tools.nherve.toolbox.image.mask;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.FeatureException;

/**
 * The Class MaskException.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class MaskException extends FeatureException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 5323120388787396190L;

	/**
	 * Instantiates a new mask exception.
	 */
	public MaskException() {
	}

	/**
	 * Instantiates a new mask exception.
	 * 
	 * @param message
	 *            the message
	 */
	public MaskException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new mask exception.
	 * 
	 * @param cause
	 *            the cause
	 */
	public MaskException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new mask exception.
	 * 
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public MaskException(String message, Throwable cause) {
		super(message, cause);
	}

}