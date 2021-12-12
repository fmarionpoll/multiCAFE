package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.signature;

import java.util.Iterator;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.SignatureDistance;

/**
 * The Class L2Distance.
 * 
 * @author Nicolas HERVE - nherve@ina.fr
 */
public class L2Distance extends SignatureDistance<VectorSignature> {

	/**
	 * Instantiates a new l2 distance.
	 */
	public L2Distance() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.nherve.toolbox.image.feature.SignatureDistance#computeDistance
	 * (plugins.nherve.toolbox.image.feature.Signature,
	 * plugins.nherve.toolbox.image.feature.Signature)
	 */
	@Override
	public double computeDistance(VectorSignature vs1, VectorSignature vs2) throws SignatureException {
		if ((vs1 == null) || (vs2 == null)) {
			throw new SignatureException("Null signature in L2Distance.compute()");
		}
		if (vs1.getSize() != vs2.getSize()) {
			throw new SignatureException("VectorSignature dimensions mismatch");
		}

		double e = 0.0;
		double d = 0.0;

		if (vs1 instanceof SparseVectorSignature && vs2 instanceof SparseVectorSignature) {
			SparseVectorSignature s1 = (SparseVectorSignature) vs1;
			SparseVectorSignature s2 = (SparseVectorSignature) vs2;
			
			Iterator<Integer> idx1it = s1.iterator();
			Iterator<Integer> idx2it = s2.iterator();

			int idx1 = Integer.MAX_VALUE;
			int idx2 = Integer.MAX_VALUE;

			if (idx1it.hasNext()) {
				idx1 = idx1it.next();
			}

			if (idx2it.hasNext()) {
				idx2 = idx2it.next();
			}

			while ((idx1 < Integer.MAX_VALUE) || (idx2 < Integer.MAX_VALUE)) {
				if (idx1 == idx2) {
					e = s1.get(idx1) - s2.get(idx2);
					d += e * e;
					if (idx1it.hasNext()) {
						idx1 = idx1it.next();
					} else {
						idx1 = Integer.MAX_VALUE;
					}
					if (idx2it.hasNext()) {
						idx2 = idx2it.next();
					} else {
						idx2 = Integer.MAX_VALUE;
					}
				} else if (idx1 > idx2) {
					e = s2.get(idx2);
					d += e * e;
					if (idx2it.hasNext()) {
						idx2 = idx2it.next();
					} else {
						idx2 = Integer.MAX_VALUE;
					}
				} else {
					e = s1.get(idx1);
					d += e * e;
					if (idx1it.hasNext()) {
						idx1 = idx1it.next();
					} else {
						idx1 = Integer.MAX_VALUE;
					}
				}
			}
		} else if (vs1 instanceof DenseVectorSignature && vs2 instanceof DenseVectorSignature) {
			double[] s1 = ((DenseVectorSignature)vs1).getData();
			double[] s2 = ((DenseVectorSignature)vs2).getData();
			for (int dim = 0; dim < s1.length; dim++) {
				e = s1[dim] - s2[dim];
				d += e * e;
			}
		} else {
			for (int dim = 0; dim < vs1.getSize(); dim++) {
				e = vs1.get(dim) - vs2.get(dim);
				d += e * e;
			}
		}

		return Math.sqrt(d);
	}

}
