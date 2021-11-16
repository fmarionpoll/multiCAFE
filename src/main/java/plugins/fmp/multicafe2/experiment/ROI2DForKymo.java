package plugins.fmp.multicafe2.experiment;

import java.util.ArrayList;

import icy.roi.ROI2D;

public class ROI2DForKymo {
	private ROI2D 	roi 			= null;	
	private long 	start 			= 0;
	private long 	end 			= -1;
	public ArrayList<ArrayList<int[]>> 	masksList = null;
	
	
	public ROI2DForKymo(long start, long end, ROI2D roi) {
		setRoi(roi);
		this.start = start;
		this.end = end;
	}
	
	public long getStart() {
		return start;
	}
	public long getEnd() {
		return end;
	}
	public ROI2D getRoi() {
		return roi;
	}
	
	public void setStart(long start) {
		this.start = start;
	}
	public void setEnd(long end) {
		this.end = end;
	}
	public void setRoi(ROI2D roi) {
		this.roi = (ROI2D) roi.getCopy();
	}
}
