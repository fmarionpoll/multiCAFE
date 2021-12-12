package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.db.ImageDatabase;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;

/**
 * The Interface Descriptor.
 * 
 * @param <T>
 *            the generic type
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public interface Descriptor<T extends Segmentable> {
	
	/**
	 * Inits the for database.
	 * 
	 * @param db
	 *            the db
	 * @throws SignatureException
	 *             the signature exception
	 */
	void initForDatabase(ImageDatabase db) throws SignatureException;
	
	/**
	 * Need to load segmentable.
	 * 
	 * @return true, if successful
	 */
	boolean needToLoadSegmentable();
	
	/**
	 * Pre process.
	 * 
	 * @param img
	 *            the img
	 * @throws SignatureException
	 *             the signature exception
	 */
	void preProcess(T img) throws SignatureException;
	
	/**
	 * Post process.
	 * 
	 * @param img
	 *            the img
	 * @throws SignatureException
	 *             the signature exception
	 */
	void postProcess(T img) throws SignatureException;
}

