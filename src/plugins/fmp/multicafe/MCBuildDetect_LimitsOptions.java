package plugins.fmp.multicafe;

import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class MCBuildDetect_LimitsOptions {
	
	boolean 		detectTop 				= true;
	boolean 		detectBottom 			= true;
	boolean 		detectAllImages 		= true;
	int				firstImage				= 0;
	boolean			directionUp				= true;
	int				detectLevelThreshold 	= 35;
	TransformOp		transformForLevels 		= TransformOp.R2MINUS_GB;

	
	void copy(MCBuildDetect_LimitsOptions destination) {

		destination.detectTop 				= detectTop; 
		destination.detectBottom 			= detectBottom; 
		destination.transformForLevels 		= transformForLevels;
		destination.directionUp 			= directionUp;
		destination.detectLevelThreshold 	= detectLevelThreshold;
		destination.detectAllImages 		= detectAllImages;
	}

}
