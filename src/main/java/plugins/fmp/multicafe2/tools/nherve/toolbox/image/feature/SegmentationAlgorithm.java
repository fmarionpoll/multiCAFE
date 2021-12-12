package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import plugins.fmp.multicafe2.tools.nherve.toolbox.Algorithm;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.segmentation.Segmentation;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.segmentation.SegmentationException;

/**
 * The Class SegmentationAlgorithm.
 * 
 * @param <T>
 *            the generic type
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public abstract class SegmentationAlgorithm<T extends Segmentable> extends Algorithm {
	
	/**
	 * Segment.
	 * 
	 * @param img
	 *            the img
	 * @return the segmentation
	 * @throws SegmentationException
	 *             the segmentation exception
	 */
	public abstract Segmentation segment(T img) throws SegmentationException;
}

