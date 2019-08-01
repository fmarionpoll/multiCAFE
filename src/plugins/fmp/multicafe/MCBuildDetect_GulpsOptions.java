package plugins.fmp.multicafe;


import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class MCBuildDetect_GulpsOptions {
	
	int 			detectGulpsThreshold	= 90;
	TransformOp 	transformForGulps 		= TransformOp.XDIFFN;
	boolean 		detectAllGulps 			= true;
	boolean			computeDiffnAndDetect	= true;
	int				firstkymo 				= 0;
	
	void copyDetectionParametersToSequenceHeader(SequencePlus seq) {
		seq.detectGulpsThreshold 	= detectGulpsThreshold;
		seq.transformForGulps 		= transformForGulps;
		seq.detectAllGulps 			= detectAllGulps;
		
		seq.bStatusChanged = true;
	}

}
