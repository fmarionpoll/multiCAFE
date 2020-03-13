package plugins.fmp.multicafeTools;

import java.util.Comparator;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.kernel.roi.roi2d.ROI2DLine;

public class Comparators {

	public static class ROI2DLineLeftXComparator implements Comparator<ROI2DLine> {
		@Override
		public int compare(ROI2DLine o1, ROI2DLine o2) {
			if (o1.getBounds().x == o2.getBounds().x)
				return 0;
			else if (o1.getBounds().x > o2.getBounds().x)
				return 1;
			else 
				return -1;
		}
	}
	
	public static class ROI2DLineLeftYComparator implements Comparator<ROI2DLine> {
		@Override
		public int compare(ROI2DLine o1, ROI2DLine o2) {
			if (o1.getBounds().y == o2.getBounds().y)
				return 0;
			else if (o1.getBounds().y > o2.getBounds().y)
				return 1;
			else 
				return -1;
		}
	}
	
	public static class ROI2DNameComparator implements Comparator<ROI2D> {
		@Override
		public int compare(ROI2D o1, ROI2D o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class CapillaryROINameComparator implements Comparator<Capillary> {
		@Override
		public int compare(Capillary o1, Capillary o2) {
			return o1.capillaryRoi.getName().compareTo(o2.capillaryRoi.getName());
		}
	}

	public static class CapillaryNameComparator implements Comparator<Capillary> {
		@Override
		public int compare(Capillary o1, Capillary o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class CapillaryIndexImageComparator implements Comparator<Capillary> {
		@Override
		public int compare(Capillary o1, Capillary o2) {
			return o1.indexImage -o2.indexImage;
		}
	}
	
	public static class ROINameComparator implements Comparator<ROI> {
		@Override
		public int compare(ROI o1, ROI o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	public static class SequenceNameComparator implements Comparator<Sequence> {
		@Override
		public int compare(Sequence o1, Sequence o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

}
