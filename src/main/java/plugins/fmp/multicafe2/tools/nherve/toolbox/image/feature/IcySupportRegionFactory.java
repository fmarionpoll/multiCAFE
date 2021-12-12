package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.util.List;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.SupportRegionException;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.mask.Mask;


/**
 * The Interface SupportRegionFactory.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public interface IcySupportRegionFactory extends SupportRegionFactory<IcySupportRegion> {
	/**
	 * Extract regions.
	 * 
	 * @param img
	 *            the img
	 * @param mask
	 *            the mask
	 * @return the list
	 * @throws SupportRegionException
	 *             the support region exception
	 */
	public abstract List<IcySupportRegion> extractRegions(Segmentable img, Mask mask) throws SupportRegionException;

}
