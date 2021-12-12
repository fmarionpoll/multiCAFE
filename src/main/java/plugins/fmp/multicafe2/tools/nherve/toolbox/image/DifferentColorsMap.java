package plugins.fmp.multicafe2.tools.nherve.toolbox.image;

import java.awt.Color;

/**
 * The Class DifferentColorsMap.
 * 
 * @author Nicolas HERVE - nherve@ina.fr
 */
public class DifferentColorsMap {
	
	/** The Constant COLORMAP_SIZE. */
	public final static int COLORMAP_SIZE = 256;

	/** The b. */
	private float[] b;
	
	/** The g. */
	private float[] g;
	
	/** The nb colors. */
	private int nbColors;
	
	/** The r. */
	private float[] r;
	
	/**
	 * Instantiates a new different colors map.
	 */
	public DifferentColorsMap() {
		this(COLORMAP_SIZE);
	}

	/**
	 * Instantiates a new different colors map.
	 * 
	 * @param nbColors
	 *            the nb colors
	 */
	public DifferentColorsMap(int nbColors) {
		this(nbColors, 3);
	}
	
	/**
	 * Instantiates a new different colors map.
	 * 
	 * @param nbColors
	 *            the nb colors
	 * @param nbCycles
	 *            the nb cycles
	 */
	public DifferentColorsMap(int nbColors, int nbCycles) {
		this.nbColors = nbColors;
		r = new float[nbColors];
		g = new float[nbColors];
		b = new float[nbColors];
		
		float hue = 0;
		float dhue = 6f / nbColors;
		float sat = 1f;
		float val = 1f;
		
		for(int cycle = 0; cycle < nbCycles; cycle++) {
			if (cycle % 2 == 0) {
				val = 1f;
			} else {
				val = 0.7f;
			}
			if (cycle % 3 == 0) {
				sat = 1f;
			} else if (cycle % 3 == 1) {
				sat = 0.8f;
			} else {
				sat = 0.6f;
			}
			for (int i = cycle * nbColors / nbCycles; i < (cycle + 1) * nbColors / nbCycles; i++) {
				hue = cycle * dhue + (i - cycle * nbColors / nbCycles) * nbCycles * dhue;
				toRGB(i, hue, sat, val);
			}
		}
	}
	
	/**
	 * Gets the.
	 * 
	 * @param idx
	 *            the idx
	 * @return the color
	 */
	public Color get(int idx) {
		// System.out.println(idx + " - r:" + r[idx] + ", g:" + g[idx] + ", b:" + b[idx]);
		return new Color(r[idx], g[idx], b[idx]);
	}

	public String getHtml(int idx) {
		Color c = get(idx);
		String html = "#";
		html += Integer.toHexString(c.getRed());
		html += Integer.toHexString(c.getGreen());
		html += Integer.toHexString(c.getBlue());
		return html;
	}
	
	/**
	 * Gets the nb colors.
	 * 
	 * @return the nb colors
	 */
	public int getNbColors() {
		return nbColors;
	}

	/**
	 * To rgb.
	 * 
	 * @param idx
	 *            the idx
	 * @param h
	 *            the h
	 * @param s
	 *            the s
	 * @param v
	 *            the v
	 */
	private void toRGB(int idx, float h, float s, float v) {
//		System.out.println(idx + " - " + h);
		int region = (int) Math.floor(h);
		float c1 = v * (1 - s);
		float c2 = v * (1 - s * (h - region));
		float c3 = v * (1 - s * (1 - (h - region)));

		switch (region) {
		case 0: {
			r[idx] = v;
			g[idx] = c3;
			b[idx] = c1;
			break;
		}
		case 1: {
			r[idx] = c2;
			g[idx] = v;
			b[idx] = c1;
			break;
		}
		case 2: {
			r[idx] = c1;
			g[idx] = v;
			b[idx] = c3;
			break;
		}
		case 3: {
			r[idx] = c1;
			g[idx] = c2;
			b[idx] = v;
			break;
		}
		case 4: {
			r[idx] = c3;
			g[idx] = c1;
			b[idx] = v;
			break;
		}
		default: {
			r[idx] = v;
			g[idx] = c1;
			b[idx] = c2;
		}
		}
	}
}

