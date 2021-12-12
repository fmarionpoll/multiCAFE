package plugins.fmp.multicafe2.tools.nherve.toolbox.concurrent;

/**
 * The Class TaskException.
 * 
 * @author Nicolas HERVE - nherve@ina.fr
 */
public class TaskException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 3450113020103536010L;

	/**
	 * Instantiates a new task exception.
	 */
	public TaskException() {
	}

	/**
	 * Instantiates a new task exception.
	 * 
	 * @param message
	 *            the message
	 */
	public TaskException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new task exception.
	 * 
	 * @param cause
	 *            the cause
	 */
	public TaskException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new task exception.
	 * 
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public TaskException(String message, Throwable cause) {
		super(message, cause);
	}

}
