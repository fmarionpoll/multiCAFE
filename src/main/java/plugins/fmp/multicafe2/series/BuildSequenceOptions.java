package plugins.fmp.multicafe2.series;

import icy.sequence.Sequence;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;

public class BuildSequenceOptions {

	public  Experiment exp;
	public Sequence seq;
	public	boolean		pass1 = true;
	public 	boolean		directionUp1			= true;
	public 	int			detectLevel1Threshold 	= 35;
	public 	EnumImageTransformations transform01 = EnumImageTransformations.R_RGB;
	
	public boolean 		pass2 = false;
	public 	boolean		directionUp2			= true;
	public 	int			detectLevel2Threshold 	= 35;
	public EnumImageTransformations transform02 = EnumImageTransformations.L1DIST_TO_1RSTCOL;
	
	public int			spanDiff				= 0;
	
}
