package plugins.fmp.multicafe2.tools.nherve.toolbox.image.feature;

import java.awt.image.BufferedImage;


public abstract class SegmentableImage implements Segmentable {
	private String name;

	public abstract BufferedImage getImage();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}