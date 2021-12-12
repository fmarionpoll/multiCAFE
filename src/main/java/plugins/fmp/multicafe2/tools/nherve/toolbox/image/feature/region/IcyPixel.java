package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

import javax.vecmath.Point2d;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.IcySupportRegion;
import plugins.fmp.multicafe2.tools.nherve.toolbox.image.mask.Mask;


/**
 * The Class Pixel.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class IcyPixel extends Point2d implements Pixel<IcyPixel>, IcySupportRegion {
	private static final long serialVersionUID = -8821643927878896700L;

	/**
	 * Instantiates a new pixel.
	 * 
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 */
	public IcyPixel(double x, double y) {
		super(x, y);
	}
	
	/**
	 * Plus.
	 * 
	 * @param other
	 *            the other
	 * @return the pixel
	 */
	public IcyPixel plus(IcyPixel other) {
		return new IcyPixel(this.x + other.x, this.y + other.y);
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.SupportRegion#getCenter()
	 */
	@Override
	public IcyPixel getCenter() {
		return this;
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<IcyPixel> iterator() {
		ArrayList<IcyPixel> px = new ArrayList<IcyPixel>();
		px.add(this);
		return px.iterator();
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.SupportRegion#intersects(plugins.nherve.toolbox.image.mask.Mask)
	 */
	@Override
	public boolean intersects(Mask mask) throws SupportRegionException {
		return mask.contains(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "("+x+", "+y+")";
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.SupportRegion#contains(double, double)
	 */
	@Override
	public boolean contains(double x, double y) {
		return (this.x == x) && (this.y == y);
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.SupportRegion#getBoundingBox()
	 */
	@Override
	public Rectangle2D getBoundingBox() {
		return new Rectangle2D.Double(x, y, x, y);
	}
	
}
