package plugins.fmp.multicafeTools;

import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;


public class DetectFlies1 implements Runnable {

	private Viewer 				viewerCamData;
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	public DetectFlies_Options 	detect 			= new DetectFlies_Options();


	
	@Override
	public void run() {
		threadRunning = true;
		
		// create arrays for storing position and init their value to zero
		detect.initParametersForDetection();
		detect.initTempRectROIs(detect.seqCamData.seq);
		
		System.out.println("Computation over frames: " + detect.startFrame + " - " + detect.endFrame );
		ProgressChrono progressBar = new ProgressChrono("Detecting flies...");
		progressBar.initChrono(detect.endFrame-detect.startFrame+1);
		
		try {
			viewerCamData = detect.seqCamData.seq.getFirstViewer();	
			detect.seqCamData.seq.beginUpdate();

			// ----------------- loop over all images of the stack
			int it = 0;
			for (int t = detect.startFrame ; t <= detect.endFrame && !stopFlag; t  += detect.analyzeStep, it++ ) {				
				progressBar.updatePositionAndTimeLeft(t);
				// load next image and compute threshold
				IcyBufferedImage workImage = detect.seqCamData.getImageAndSubtractReference(t, detect.transformop); 
				if (workImage == null)
					continue;
				detect.seqCamData.currentFrame = t;
				viewerCamData.setPositionT(t);
				viewerCamData.setTitle(detect.seqCamData.getDecoratedImageName(t));
				detect.findFlies (workImage, t, it);		
			}
		} finally {
			progressBar.close();
			detect.seqCamData.seq.endUpdate();
			detect.removeTempRectROIs(detect.seqCamData.seq);
		}

		//	 copy created ROIs to inputSequence
		detect.copyDetectedROIsToSequence(detect.seqCamData.seq);
		System.out.println("Computation finished.");
		threadRunning = false;
	}
	
	
}