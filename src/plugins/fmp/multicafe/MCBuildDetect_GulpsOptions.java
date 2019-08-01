package plugins.fmp.multicafe;


import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class MCBuildDetect_GulpsOptions {
	
	int 			detectGulpsThreshold	= 90;
	TransformOp 	transformForGulps 		= TransformOp.XDIFFN;
	boolean 		detectAllGulps 			= true;
	boolean			computeDiffnAndDetect	= true;
	int				firstkymo 				= 0;
	
	public void copy(MCBuildDetect_GulpsOptions destination) {
		destination.detectGulpsThreshold 	= detectGulpsThreshold;
		destination.transformForGulps 		= transformForGulps;
		destination.detectAllGulps 			= detectAllGulps;
	}

}
