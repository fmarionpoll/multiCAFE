package plugins.fmp.multicafeTools;

import java.awt.Rectangle;
import java.util.ArrayList;

import icy.roi.ROI2D;
import plugins.fmp.multicafeSequence.ExperimentList;

public class BuildKymographs_Options {
	public int 				analyzeStep 		= 1;
	public int 				startFrame 			= 0;
	public int				endFrame 			= 99999999;
	public boolean			isFrameFixed		= false;
	
	public int 				diskRadius 			= 5;
	public boolean 			doRegistration 		= false;
	public ArrayList<ROI2D> listROIStoBuildKymos= new ArrayList<ROI2D> ();
	public boolean			updateViewerDuringComputation = false;
	public ExperimentList	expList;
	public Rectangle 		parent0Rect 		= null;
	
	public boolean 			loopRunning 		= false;	
}
