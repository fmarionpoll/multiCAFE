package plugins.fmp.multicafe2.tools.nherve.toolbox.image.segmentation;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.FeatureException;

/**
 * The Class SegmentationException.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class SegmentationException extends FeatureException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -4795618827983192679L;

	/**
	 * Instantiates a new segmentation exception.
	 */
	public SegmentationException() {
	}

	/**
	 * Instantiates a new segmentation exception.
	 * 
	 * @param message
	 *            the message
	 */
	public SegmentationException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new segmentation exception.
	 * 
	 * @param cause
	 *            the cause
	 */
	public SegmentationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new segmentation exception.
	 * 
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public SegmentationException(String message, Throwable cause) {
		super(message, cause);
	}

}
