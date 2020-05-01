package plugins.fmp.multicafeTools;

import plugins.fmp.multicafeSequence.ExperimentList;

public class XLSExportOptions {
	
	public boolean 	xyImage 		= true;
	public boolean 	xyTopCage = true;
	public boolean 	distance 		= false;
	public boolean 	alive 			= true;
	
	public boolean 	topLevel 		= true;
	public boolean  topLevelDelta   = false;
	public boolean 	bottomLevel 	= false; 
	public boolean 	derivative 		= false; 
	public boolean 	consumption 	= false; 
	public boolean 	sum 			= true;
	public boolean 	t0				= true;
	public boolean 	onlyalive		= true;

	public boolean 	transpose 		= false;
	public boolean 	duplicateSeries = true;
	public int		buildExcelBinStep	= 1;
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
