package plugins.fmp.multicafe2.experiment;

import java.util.ArrayList;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class CapillaryWithTime {
	public ROI2DShape 					roi 			= null;	// the capillary (source)
	public long 						start			= -1;
	public long							end				= -1;
	public ArrayList<ArrayList<int[]>> 	masksList 		= null;
	
	public CapillaryWithTime(ROI2DShape roi) {
		this.roi = roi;
	}
	
	public boolean IsIntervalWithinLimits(long index) {
		if (start < 0)
			return true;
		if (index > end || index < start)
			return false;
		else
			return true;
	}

}
