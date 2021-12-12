package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Segmentable;



/**
 * The Class FullImageSupportRegion.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class FullImageSupportRegion extends AreaSupportRegion {

	/**
	 * Instantiates a new full image support region.
	 * 
	 * @param obj
	 *            the obj
	 */
	public FullImageSupportRegion(Segmentable obj) {
		super(obj);
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.region.AreaSupportRegion#initArea()
	 */
	@Override
	public void initArea() {
		area = new Area(new Rectangle2D.Float(0, 0, getOverallWidth(), getOverallHeight()));
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.region.DefaultSupportRegion#toString()
	 */
	@Override
	public String toString() {
		return "[Full]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<IcyPixel> iterator() {
		if (area == null) {
			initArea();
		}
		ArrayList<IcyPixel> px = new ArrayList<IcyPixel>();
		for (int x = 0; x < getOverallWidth(); x++) {
			for (int y = 0; y < getOverallHeight(); y++) {
				px.add(new IcyPixel(x, y));
			}
		}
		return px.iterator();
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.SupportRegion#getCenter()
	 */
	@Override
	public IcyPixel getCenter() {
		if (area == null) {
			initArea();
		}
		return new IcyPixel(getOverallWidth() / 2, getOverallHeight() / 2);
	}
}

