package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.util.List;

import plugins.fmp.multicafe2.tools.nherve.matrix.Matrix;
import plugins.fmp.multicafe2.tools.nherve.toolbox.Algorithm;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.DenseVectorSignature;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.VectorSignature;

/**
 * The Class DimensionReductionAlgorithm.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public abstract class DimensionReductionAlgorithm extends Algorithm {
	
	/** The signatures. */
	protected List<VectorSignature> signatures;
	
	/** The mean. */
	protected VectorSignature mean;
	
	/** The dim. */
	protected int dim;

	/**
	 * Instantiates a new dimension reduction algorithm.
	 * 
	 * @param signatures
	 *            the signatures
	 */
	public DimensionReductionAlgorithm(List<VectorSignature> signatures) {
		super();
		this.signatures = signatures;
		this.mean = null;
	}

	/**
	 * Compute.
	 * 
	 * @throws SignatureException
	 *             the signature exception
	 */
	public abstract void compute() throws SignatureException;

	/**
	 * Project.
	 * 
	 * @param toProject
	 *            the to project
	 * @return the list
	 * @throws SignatureException
	 *             the signature exception
	 */
	public abstract List<VectorSignature> project(List<VectorSignature> toProject) throws SignatureException;

	/**
	 * Check.
	 * 
	 * @throws SignatureException
	 *             the signature exception
	 */
	protected void check() throws SignatureException {
		if (signatures == null) {
			throw new SignatureException("Signatures == null, can't run " + getClass().getSimpleName());
		}

		int n = signatures.size();

		if (n < 2) {
			throw new SignatureException("Not enough signatures (" + n + ") to run " + getClass().getSimpleName());
		}

		VectorSignature aSignature = signatures.get(0);
		dim = aSignature.getSize();
	}

	/**
	 * Gets the mean.
	 * 
	 * @return the mean
	 * @throws SignatureException
	 *             the signature exception
	 */
	public VectorSignature getMean() throws SignatureException {
		if (mean == null) {
			mean = new DenseVectorSignature(dim);
			for (VectorSignature s : signatures) {
				mean.add(s);
			}
			mean.multiply(1.0 / signatures.size());
			log(mean.toString());
		}
		
		return mean;
	}
	
	/**
	 * Gets the mean.
	 * 
	 * @param m
	 *            the m
	 * @return the mean
	 * @throws SignatureException
	 *             the signature exception
	 */
	public Matrix getMean(Matrix m) throws SignatureException {
		int n = m.getRowDimension();
		int dim = m.getColumnDimension();
		
		Matrix mean = new Matrix(1, dim, 0);
		for (int i = 0; i < n; i++) {
			for (int d = 0; d < dim; d++) {
				mean.set(0, d, mean.get(0, d) + m.get(i, d));
			}
		}
		
		mean.timesEquals(1.0 / (double)n);
		
		return mean;
	}
	
	/**
	 * Gets the centered matrix.
	 * 
	 * @param sigs
	 *            the sigs
	 * @return the centered matrix
	 * @throws SignatureException
	 *             the signature exception
	 */
	public Matrix getCenteredMatrix(List<VectorSignature> sigs) throws SignatureException {
		int ln = sigs.size();

		if (ln < 1) {
			throw new SignatureException("Not enough signatures (" + ln + ") to getCenteredMatrix");
		}

		VectorSignature aSignature = signatures.get(0);
		int ldim = aSignature.getSize();
		
		Matrix m = new Matrix(ln, ldim);
		VectorSignature mean = getMean();

		int c = 0;
		for (VectorSignature s : sigs) {
			for (int d = 0; d < ldim; d++) {
				m.set(c, d, s.get(d) - mean.get(d));
			}
			c++;
		}

		return m;
	}
	
	/**
	 * Gets the matrix.
	 * 
	 * @param sigs
	 *            the sigs
	 * @return the matrix
	 * @throws SignatureException
	 *             the signature exception
	 */
	public Matrix getMatrix(List<VectorSignature> sigs) throws SignatureException {
		int ln = sigs.size();

		if (ln < 1) {
			throw new SignatureException("Not enough signatures (" + ln + ") to getMatrix");
		}

		VectorSignature aSignature = signatures.get(0);
		int ldim = aSignature.getSize();
		
		Matrix m = new Matrix(ln, ldim);

		int c = 0;
		for (VectorSignature s : sigs) {
			for (int d = 0; d < ldim; d++) {
				m.set(c, d, s.get(d));
			}
			c++;
		}

		return m;
	}

	/**
	 * Gets the var cov matrix.
	 * 
	 * @param m
	 *            the m
	 * @return the var cov matrix
	 * @throws SignatureException
	 *             the signature exception
	 */
	public Matrix getVarCovMatrix(Matrix m) throws SignatureException {
		Matrix varcov = m.transpose().times(m);
		varcov.timesEquals(1.0 / (double)m.getRowDimension());
		return varcov;
	}

}
