package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.SupportRegion;


/**
 * The Class Pixel.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */

@SuppressWarnings("rawtypes")
public interface Pixel<T extends Pixel> extends SupportRegion<T> {
	public  T plus(T other);
}

