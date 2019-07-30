package plugins.fmp.multicafe;

import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class MCBuildDetect_LimitsOptions {
	
	boolean 		detectTop 				= true;
	boolean 		detectBottom 			= true;
	boolean 		detectAllLevel 			= true;
	int				firstkymo				= 0;
	boolean			directionUp				= true;
	int				detectLevelThreshold 	= 35;
	TransformOp		transformForLevels 		= TransformOp.R2MINUS_GB;

	
	void copyDetectionParametersToSequenceHeader(SequencePlus seq) {

		seq.detectTop 				= detectTop; 
		seq.detectBottom 			= detectBottom; 
		seq.transformForLevels 		= transformForLevels;
		seq.directionUp 			= directionUp;
		seq.detectLevelThreshold 	= detectLevelThreshold;
		seq.detectAllLevel 			= detectAllLevel;
	
		seq.bStatusChanged = true;
	}

}
