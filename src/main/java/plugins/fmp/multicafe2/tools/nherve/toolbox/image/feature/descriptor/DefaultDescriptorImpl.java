package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.descriptor;

import plugins.fmp.multicafe2.tools.nherve.toolbox.Algorithm;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.db.ImageDatabase;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Descriptor;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Segmentable;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Signature;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.VectorSignature;

/**
 * The Class DefaultDescriptorImpl.
 * 
 * @param <T>
 *            the generic type
 * @param <S>
 *            the generic type
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public abstract class DefaultDescriptorImpl<T extends Segmentable, S extends Signature> extends Algorithm implements Descriptor<T> {
	
	/** The vector signature type. */
	private int vectorSignatureType;
	
	/**
	 * Instantiates a new default descriptor impl.
	 * 
	 * @param display
	 *            the display
	 */
	public DefaultDescriptorImpl(boolean display) {
		super(display);
		setVectorSignatureType(VectorSignature.DENSE_VECTOR_SIGNATURE);
	}

	/**
	 * Gets the signature size.
	 * 
	 * @return the signature size
	 */
	public abstract int getSignatureSize();
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public abstract String toString();

	/**
	 * Gets the vector signature type.
	 * 
	 * @return the vector signature type
	 */
	public int getVectorSignatureType() {
		return vectorSignatureType;
	}

	/**
	 * Sets the vector signature type.
	 * 
	 * @param vectorSignatureType
	 *            the new vector signature type
	 */
	public void setVectorSignatureType(int vectorSignatureType) {
		this.vectorSignatureType = vectorSignatureType;
	}
	
	/**
	 * Gets the empty signature.
	 * 
	 * @param size
	 *            the size
	 * @return the empty signature
	 */
	public VectorSignature getEmptySignature(int size) {
		return VectorSignature.getEmptySignature(vectorSignatureType, size);
	}
	
	/**
	 * Gets the empty signature.
	 * 
	 * @return the empty signature
	 */
	public VectorSignature getEmptySignature() {
		return getEmptySignature(getSignatureSize());
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.Descriptor#initForDatabase(plugins.nherve.toolbox.image.db.ImageDatabase)
	 */
	@Override
	public void initForDatabase(ImageDatabase db) throws SignatureException {
		// Nothing to do by default
	}
}

