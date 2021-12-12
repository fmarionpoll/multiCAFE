package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.util.ArrayList;
import java.util.List;

import plugins.fmp.multicafe2.tools.nherve.matrix.EigenvalueDecomposition;
import plugins.fmp.multicafe2.tools.nherve.matrix.Matrix;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.DenseVectorSignature;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.SignatureException;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature.VectorSignature;

/**
 * The Class PCA.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class PCA extends DimensionReductionAlgorithm {
	
	/** The evd. */
	private EigenvalueDecomposition evd;

	/**
	 * Instantiates a new pCA.
	 * 
	 * @param signatures
	 *            the signatures
	 */
	public PCA(List<VectorSignature> signatures) {
		super(signatures);
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.DimensionReductionAlgorithm#compute()
	 */
	@Override
	public void compute()  throws SignatureException {
		check();
		
		Matrix m = getCenteredMatrix(signatures);
		
		log("Building variance/covariance matrix ...");
		Matrix varcov = getVarCovMatrix(m);
		m = null;
		
		log("Launching EigenvalueDecomposition ... " + varcov.getRowDimension() + " x " + varcov.getColumnDimension());
		evd = varcov.eig();
		if (isLogEnabled()) {
			evd.getV().print(8, 3);
		}
		log("Done");		
	}
	
	/**
	 * Gets the projection matrix.
	 * 
	 * @return the projection matrix
	 */
	public Matrix getProjectionMatrix() {
		return evd.getV();
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.DimensionReductionAlgorithm#project(java.util.List)
	 */
	@Override
	public List<VectorSignature> project(List<VectorSignature> toProject) throws SignatureException {
		Matrix tp = getCenteredMatrix(toProject);
		Matrix p = tp.times(evd.getV());
		ArrayList<VectorSignature> proj = new ArrayList<VectorSignature>();
		for (int s = 0; s < toProject.size(); s++) {
			DenseVectorSignature vs = new DenseVectorSignature(p.getColumnDimension());
			for (int d = 0; d < p.getColumnDimension(); d++) {
				vs.set(d, p.get(s, d));
			}
			proj.add(vs);
		}
		return proj;
	}
	
	/**
	 * Project.
	 * 
	 * @param toProject
	 *            the to project
	 * @param upToDim
	 *            the up to dim
	 * @return the list
	 * @throws SignatureException
	 *             the signature exception
	 */
	public List<VectorSignature> project(List<VectorSignature> toProject, int upToDim) throws SignatureException {
		Matrix tp = getCenteredMatrix(toProject);
		Matrix p = tp.times(evd.getV());
		
		if (upToDim > p.getColumnDimension()) {
			throw new SignatureException("Can not project up to dimension " + upToDim + " / " + p.getColumnDimension());
		}
		
		ArrayList<VectorSignature> proj = new ArrayList<VectorSignature>();
		for (int s = 0; s < toProject.size(); s++) {
			DenseVectorSignature vs = new DenseVectorSignature(upToDim);
			for (int d = 0; d < upToDim; d++) {
				vs.set(d, p.get(s, d));
			}
			vs.normalizeSumToOne(true);
			proj.add(vs);
		}
		return proj;
	}
}
