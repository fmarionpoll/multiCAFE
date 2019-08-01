package plugins.fmp.multicafeSequence;



import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import plugins.fmp.multicafeTools.EnumArrayListType;
import plugins.fmp.multicafe.MCBuildDetect_GulpsOptions;
import plugins.fmp.multicafe.MCBuildDetect_LimitsOptions;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Capillary {

	public int							indexImage 	= 0;
	public ROI2DShape 					roi 		= null;	// the capillary
	
	public MCBuildDetect_LimitsOptions 	limitsOptions;
	public MCBuildDetect_GulpsOptions 	gulpsOptions;
	public List<Point2D> 				ptsTop  = new ArrayList<>(); 
	public List<Point2D> 				ptsBottom = new ArrayList<>();
	public ArrayList<Integer> 			derivedValuesArrayList = new ArrayList<Integer>(); // (derivative) result of the detection of the capillary level
	
	
	Capillary(ROI2DShape roi) {
		this.roi = roi;
	}
	
	public String getName() {
		return roi.getName();
	}
	
	public ArrayList<Integer> getYFromPtArray(List<Point2D> ptsList) {
		ArrayList<Integer> arrayInt = new ArrayList<Integer> ();
		for (Point2D pt: ptsList) {
			int value = (int) pt.getY();
			arrayInt.add(value);
		}
		return arrayInt;
	}
	
	public ArrayList<Integer> getArrayListFromRois(EnumArrayListType option) {
		ArrayList<Integer> datai = null;
		
		switch (option) {
		case derivedValues:
			datai = derivedValuesArrayList;
			break;
		case cumSum:
			datai = new ArrayList<Integer>(Collections.nCopies(this.getWidth(), 0));
			addRoisMatchingFilterToCumSumDataArray("gulp", datai);
			break;
		case bottomLevel:
			datai = getYFromPtArray(ptsBottom);
			break;
		case topLevel:
		default:
			datai = getYFromPtArray(ptsTop);
			break;
		}
		return datai;
	}
}
