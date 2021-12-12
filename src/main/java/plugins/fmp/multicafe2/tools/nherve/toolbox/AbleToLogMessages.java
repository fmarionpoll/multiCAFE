package plugins.fmp.multicafe2.tools.nherve.toolbox;

/*
 * **
* The Interface AbleToLogMessages.
* 
* @author Nicolas HERVE - nherve@ina.fr
*/
public interface AbleToLogMessages {

	public boolean isLogEnabled();
	public void setLogEnabled(boolean logEnabled);

	/**
	 * Log.
	 * 
	 * @param message
	 *            the message
	 */
	public void log(String message);
	
	/**
	 * Log warning.
	 * 
	 * @param message
	 *            the message
	 */
	public void logWarning(String message);
	
	/**
	 * Log error.
	 * 
	 * @param message
	 *            the message
	 */
	public void logError(String message);
	
	public void logError(Throwable e);

	
	public boolean isUIDisplayEnabled();
	public void setUIDisplayEnabled(boolean uiDisplay);
	public void clearDisplay();
	public void displayMessage(String message);

}
