package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.IcyPixel;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.SupportRegionException;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.mask.Mask;


/**
 * The Interface SupportRegion.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public interface IcySupportRegion extends SupportRegion<IcyPixel> {
	
	/**
	 * Intersects.
	 * 
	 * @param mask
	 *            the mask
	 * @return true, if successful
	 * @throws SupportRegionException
	 *             the support region exception
	 */
	boolean intersects(Mask mask) throws SupportRegionException;
	
}
