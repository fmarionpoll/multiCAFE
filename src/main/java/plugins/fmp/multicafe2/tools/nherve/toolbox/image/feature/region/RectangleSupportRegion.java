package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.region;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

import plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature.Segmentable;



/**
 * The Class RectangleSupportRegion.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class RectangleSupportRegion extends AreaSupportRegion {
	
	/** The center x. */
	private final int centerX;
	
	/** The center y. */
	private final int centerY;
	
	/** The width. */
	private final int width;
	
	/** The height. */
	private final int height;

	/**
	 * Instantiates a new rectangle support region.
	 * 
	 * @param relatedImage
	 *            the related image
	 * @param centerX
	 *            the center x
	 * @param centerY
	 *            the center y
	 * @param length
	 *            the length
	 */
	public RectangleSupportRegion(Segmentable relatedImage, int centerX, int centerY, int length) {
		super(relatedImage);
		this.centerX = centerX;
		this.centerY = centerY;
		this.width = length;
		this.height = length;
		initArea();
	}
	
	/**
	 * Instantiates a new rectangle support region.
	 * 
	 * @param relatedImage
	 *            the related image
	 * @param centerX
	 *            the center x
	 * @param centerY
	 *            the center y
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 */
	public RectangleSupportRegion(Segmentable relatedImage, int centerX, int centerY, int width, int height) {
		super(relatedImage);
		this.centerX = centerX;
		this.centerY = centerY;
		this.width = width;
		this.height = height;
		initArea();
	}
	
	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.region.AreaSupportRegion#initArea()
	 */
	@Override
	protected void initArea() {
		int halfWidth = width / 2;
		int halfHeight = height / 2;
		area = new Area(new Rectangle2D.Float(centerX - halfWidth, centerY - halfHeight, width, height));
	}

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.region.DefaultSupportRegion#toString()
	 */
	@Override
	public String toString() {
		return "[" + centerX + ", " + centerY + "]";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<IcyPixel> iterator() {
		ArrayList<IcyPixel> px = new ArrayList<IcyPixel>();
		int startX = centerX - width / 2;
		int startY = centerY - height / 2;
		for (int x = startX; x < startX + width; x++) {
			for (int y = startY; y < startY + height; y++) {
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
		return new IcyPixel(centerX, centerY);
	}
}

