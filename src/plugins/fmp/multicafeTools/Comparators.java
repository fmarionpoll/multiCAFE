package plugins.fmp.multicafeTools;

import java.util.Comparator;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.multicafeSequence.Cage;


public class Comparators {

	public static class ROINameComparator implements Comparator<ROI> {
		@Override
		public int compare(ROI o1, ROI o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class ROI2DNameComparator implements Comparator<ROI2D> {
		@Override
		public int compare(ROI2D o1, ROI2D o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	public static class SequenceNameComparator implements Comparator<Sequence> {
		@Override
		public int compare(Sequence o1, Sequence o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class XLSCapillaryResultsComparator implements Comparator <XLSCapillaryResults> {
		@Override
		public int compare (XLSCapillaryResults o1, XLSCapillaryResults o2) {
			return o1.name.compareTo(o2.name);
		}
	}
	
	public static class CageNameComparator implements Comparator <Cage> {
		@Override
		public int compare (Cage o1, Cage o2) {
			return o1.roi.getName().compareTo(o2.roi.getName());
		}
	}
	
}
