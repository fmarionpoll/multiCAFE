package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Signature;


/**
 * The Class BagOfSignatures.
 * 
 * @param <T>
 *            the generic type
 * @author Nicolas HERVE - nherve@ina.fr
 */
public class BagOfSignatures<T extends Signature> implements Signature, Iterable<T> {
	
	/** The signatures. */
	private List<T> signatures;

	/**
	 * Instantiates a new bag of signatures.
	 */
	public BagOfSignatures() {
		super();
		signatures = new ArrayList<T>();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public BagOfSignatures<T> clone() throws CloneNotSupportedException {
		BagOfSignatures<T> c = new BagOfSignatures<T>();
		for (T s : this) {
			c.add((T)s.clone());
		}
		return c;
	}

	/**
	 * Adds the.
	 * 
	 * @param e
	 *            the e
	 * @return true, if successful
	 */
	public boolean add(T e) {
		return signatures.add(e);
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<T> iterator() {
		return signatures.iterator();
	}

	/**
	 * Size.
	 * 
	 * @return the int
	 */
	public int size() {
		return signatures.size();
	}

	/**
	 * Gets the signatures.
	 * 
	 * @return the signatures
	 */
	public List<T> getSignatures() {
		return signatures;
	}

}