package plugins.fmp.multicafe.series;

import java.awt.Rectangle;
import java.util.ArrayList;

import icy.roi.ROI2D;
import plugins.fmp.multicafe.sequence.ExperimentList;

public class BuildKymographs_Options {
	public int 				stepFrame 			= 1;
	public int 				startFrame 			= 0;
	public int				endFrame 			= 99999999;
	public boolean			isFrameFixed		= false;
	
	public int 				diskRadius 			= 5;
	public boolean 			doRegistration 		= false;
	public boolean			doCreateResults_bin	= false;
	public ArrayList<ROI2D> listROIStoBuildKymos= new ArrayList<ROI2D> ();
	public ExperimentList	expList;
	public Rectangle 		parent0Rect 		= null;
	public String 			resultsSubPath 		= null;
	
	public boolean 			loopRunning 		= false;	
}
