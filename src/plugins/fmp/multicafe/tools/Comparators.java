package plugins.fmp.multicafe.tools;

import java.util.Comparator;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.sequence.Cage;
import plugins.fmp.multicafe.sequence.XYTaSeries;
import plugins.fmp.multicafe.tools.toExcel.XLSResults;


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
	
	public static class XLSResultsComparator implements Comparator <XLSResults> {
		@Override
		public int compare (XLSResults o1, XLSResults o2) {
			return o1.name.compareTo(o2.name);
		}
	}
	
	public static class XYTaSeriesComparator implements Comparator <XYTaSeries> {
		@Override
		public int compare (XYTaSeries o1, XYTaSeries o2) {
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
