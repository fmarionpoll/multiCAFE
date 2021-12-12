package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import plugins.fmp.multicafe2.tools.nherve.toolbox.Algorithm;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.descriptor.DefaultDescriptorImpl;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.descriptor.GlobalDescriptor;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.GridFactory;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.SupportRegionException;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.VectorSignature;


/**
 * The Class SignatureExtractor.
 * 
 * @param <T>
 *            the generic type
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public abstract class SignatureExtractor<T extends Segmentable> extends Algorithm {
	
	/** The descriptor. */
	private DefaultDescriptorImpl<T, ? extends Signature> descriptor;
	
	/** The display. */
	private boolean display;

	/**
	 * Cast.
	 * 
	 * @param l
	 *            the l
	 * @return the list
	 */
	public static List<VectorSignature> cast(List<Signature> l) {
		if (l == null) {
			return null;
		}
		List<VectorSignature> l2 = new ArrayList<VectorSignature>();
		for (Signature s : l) {
			l2.add((VectorSignature)s);
		}
		return l2;
	}
	
	/**
	 * Instantiates a new signature extractor.
	 * 
	 * @param descriptor
	 *            the descriptor
	 */
	public SignatureExtractor(DefaultDescriptorImpl<T, ? extends Signature> descriptor) {
		super();
		this.descriptor = descriptor;
	}
	
	/**
	 * Extract signature.
	 * 
	 * @param img
	 *            the img
	 * @return the signature
	 * @throws SignatureException
	 *             the signature exception
	 */
	@SuppressWarnings("unchecked")
	public Signature extractSignature(T img) throws SignatureException {
		if (!(descriptor instanceof GlobalDescriptor)) {
			throw new SignatureException("Unable to extract a global signature with this descriptor");
		}
		GlobalDescriptor<T, ? extends Signature> gd = (GlobalDescriptor<T, ? extends Signature>) descriptor;
		getDescriptor().preProcess(img);
		Signature globalSignature = gd.extractGlobalSignature(img);
		getDescriptor().postProcess(img);
		return globalSignature;
	}
	
	/**
	 * Extract signatures.
	 * 
	 * @param img
	 *            the img
	 * @param regions
	 *            the regions
	 * @param doPreprocess
	 *            the do preprocess
	 * @return the signature[]
	 * @throws SignatureException
	 *             the signature exception
	 */
	public abstract Signature[] extractSignatures(T img, IcySupportRegion[] regions, boolean doPreprocess) throws SignatureException;
	
	/**
	 * Extract signatures.
	 * 
	 * @param img
	 *            the img
	 * @param regions
	 *            the regions
	 * @return the signature[]
	 * @throws SignatureException
	 *             the signature exception
	 */
	public Signature[] extractSignatures(T img, IcySupportRegion[] regions) throws SignatureException {
		return extractSignatures(img, regions, true);
	}
	
	/**
	 * Extract signatures.
	 * 
	 * @param img
	 *            the img
	 * @param regions
	 *            the regions
	 * @return the list
	 * @throws SignatureException
	 *             the signature exception
	 */
	public List<Signature> extractSignatures(T img, List<? extends IcySupportRegion> regions) throws SignatureException {
		IcySupportRegion[] aRegions = (IcySupportRegion[])regions.toArray(new IcySupportRegion[regions.size()]);
		Signature[] sigs = extractSignatures(img, aRegions);
		if (sigs == null) {
			return null;
		}
		return Arrays.asList(sigs);
	}

	/**
	 * Extract signatures.
	 * 
	 * @param img
	 *            the img
	 * @return the signature[]
	 * @throws SignatureException
	 *             the signature exception
	 */
	public Signature[] extractSignatures(T img) throws SignatureException {
		try {
			GridFactory factory = new GridFactory(GridFactory.ALGO_ONLY_PIXELS);
			List<IcySupportRegion> regions = factory.extractRegions(img);
			IcySupportRegion[] aRegions = (IcySupportRegion[])regions.toArray(new IcySupportRegion[regions.size()]);
			return extractSignatures(img, aRegions);
		} catch (SupportRegionException e) {
			throw new SignatureException(e);
		}
	}

	/**
	 * Gets the descriptor.
	 * 
	 * @return the descriptor
	 */
	public DefaultDescriptorImpl<T, ? extends Signature> getDescriptor() {
		return descriptor;
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.Algorithm#isDisplayEnabled()
	 */
	public boolean isLogEnabled() {
		return display;
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.Algorithm#setDisplayEnabled(boolean)
	 */
	public void setLogEnabled(boolean display) {
		this.display = display;
	}
}

