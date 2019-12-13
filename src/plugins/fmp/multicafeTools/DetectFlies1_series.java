package plugins.fmp.multicafeTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;

public class DetectFlies1_series implements Runnable {

	private Viewer 				viewerCamData;
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	public DetectFlies_Options 	detect 			= new DetectFlies_Options();


	
	@Override
	public void run() {
		threadRunning = true;
		ExperimentList expList = detect.expList;
		int nbexp = expList.index1 - expList.index0 +1;
		ProgressChrono progressBar = new ProgressChrono("Detect limits");
		progressBar.initStuff(nbexp);
		progressBar.setMessageFirstPart("Analyze series ");
		for (int index = expList.index0; index <= expList.index1; index++) {
			if (stopFlag)
				break;
			Experiment exp = expList.experimentList.get(index);
			System.out.println(exp.experimentFileName);
			progressBar.updatePosition(index-expList.index0+1);
			exp.loadExperimentCamData();
			detectFlies1(exp);
			saveComputation(exp);
		}
		progressBar.close();
		threadRunning = false;
	}
	
	private void saveComputation(Experiment exp) {			
		Path dir = Paths.get(exp.seqCamData.getDirectory());
		dir = dir.resolve("results");
		String directory = dir.toAbsolutePath().toString();
		if (Files.notExists(dir))  {
			try {
				Files.createDirectory(dir);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Creating directory failed: "+ directory);
				return;
			}
		}
		ProgressFrame progress = new ProgressFrame("Save kymograph measures");		
		exp.saveFlyPositions();
		progress.close();
	}
	
	private void detectFlies1(Experiment exp) {
		
		// create arrays for storing position and init their value to zero
		detect.seqCamData = exp.seqCamData;
		detect.initParametersForDetection();
		detect.initTempRectROIs(detect.seqCamData.seq);
		
		System.out.println("Computation over frames: " + detect.startFrame + " - " + detect.endFrame );
		ProgressChrono progressBar = new ProgressChrono("Detecting flies...");
		progressBar.initStuff(detect.endFrame-detect.startFrame+1);
		
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
		threadRunning = false;
	}
	
	
}