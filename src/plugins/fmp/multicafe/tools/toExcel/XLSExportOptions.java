package plugins.fmp.multicafe.tools.toExcel;

import plugins.fmp.multicafe.sequence.ExperimentList;

public class XLSExportOptions {
	
	public boolean 	xyImage 		= true;
	public boolean 	xyTopCage 		= true;
	public boolean 	xyTipCapillaries= true;
	
	public boolean 	distance 		= false;
	public boolean 	alive 			= true;
	public boolean  sleep			= true;
	public int		sleepThreshold  = 5;
	
	public boolean 	topLevel 		= true;
	public boolean  topLevelDelta   = false;
	public boolean 	bottomLevel 	= false; 
	public boolean 	derivative 		= false; 
	public boolean 	sum_ratio_LR 	= true;
	public boolean 	cage 			= true;
	public boolean 	t0				= true;
	public boolean 	onlyalive		= true;
	public boolean  subtractEvaporation = true;
	
	public boolean 	sumGulps 		= false;
	public boolean  isGulps			= false;
	public boolean	tToNextGulp		= false;
	public boolean	tToNextGulp_LR	= false;


	public boolean 	transpose 		= false;
	public boolean 	duplicateSeries = true;
	public int		buildExcelMilliSecStep	= 1;
	public boolean 	exportAllFiles 	= true;
	public boolean 	absoluteTime	= false;
	public boolean 	collateSeries	= false;
	public boolean  padIntervals	= true;
	
	public int 		firstExp 		= -1;
	public int 		lastExp 		= -1;
	public ExperimentList expList 	= null;

	// internal parameter
	public	boolean	trim_alive		= false;
}
