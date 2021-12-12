package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.util.List;

import plugins.fmp.multicafe2.tools.nherve.toolbox.AbleToLogMessages;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region.SupportRegionException;


/**
 * The Interface SupportRegionFactory.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public interface SupportRegionFactory<T extends SupportRegion> extends AbleToLogMessages {

	/**
	 * Extract regions.
	 * 
	 * @param img
	 *            the img
	 * @return the list
	 * @throws SupportRegionException
	 *             the support region exception
	 */
	public abstract List<T> extractRegions(Segmentable img) throws SupportRegionException;

}
