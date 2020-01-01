package plugins.fmp.multicafeTools;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.SwingUtilities;

import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;




public class DetectFlies1_series implements Runnable {

	private Viewer 				viewerCamData 	= null;
	private OverlayThreshold 	ov 				= null;
	
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	public DetectFlies_Options 	detect 			= new DetectFlies_Options();

	// -----------------------------------------------------
	
	
	@Override
	public void run() {
		threadRunning = true;
		ExperimentList expList = detect.expList;
		int nbexp = expList.index1 - expList.index0 +1;
		ProgressChrono progressBar = new ProgressChrono("Detect flies");
		progressBar.initChrono(nbexp);
		progressBar.setMessageFirstPart("Analyze series ");
		for (int index = expList.index0; index <= expList.index1 && !stopFlag; index++) {
			if (stopFlag) 
				break;
			Experiment exp = expList.experimentList.get(index);
			System.out.println(exp.experimentFileName);
			progressBar.updatePosition(index-expList.index0+1);
			exp.loadExperimentCamData();
			exp.seqCamData.xmlReadDrosoTrackDefault();
			runDetectFlies(exp);
			if (!stopFlag)
				saveComputation(exp);
			exp.seqCamData.seq.close();
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
		exp.saveFlyPositions();
	}
	
	private void runDetectFlies(Experiment exp) {
		detect.seqCamData = exp.seqCamData;
		detect.initParametersForDetection();
		detect.initTempRectROIs(detect.seqCamData.seq);
		ProgressChrono progressBar = new ProgressChrono("Detecting flies...");
		progressBar.initChrono(detect.endFrame-detect.startFrame+1);
		try {
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				viewerCamData = new Viewer(exp.seqCamData.seq, true);
				if (ov == null) 
					ov = new OverlayThreshold(exp.seqCamData);
				else {
					detect.seqCamData.seq.removeOverlay(ov);
					ov.setSequence(exp.seqCamData);
				}
				detect.seqCamData.seq.addOverlay(ov);	
				ov.setThresholdSingle(detect.seqCamData.cages.detect.threshold);
				ov.painterChanged();
				
			}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		} 
		
		detect.seqCamData.seq.beginUpdate();
		int it = 0;
		for (int t = detect.startFrame ; t <= detect.endFrame; t  += detect.analyzeStep, it++ ) {				
			if (stopFlag)
				break;
			progressBar.updatePositionAndTimeLeft(t);
			IcyBufferedImage workImage = detect.seqCamData.getImageAndSubtractReference(t, detect.transformop); 
			if (workImage == null)
				continue;
			detect.seqCamData.currentFrame = t;
			viewerCamData.setPositionT(t);
			viewerCamData.setTitle(detect.seqCamData.getDecoratedImageName(t));
			detect.findFlies (workImage, t, it);
		}
	
		detect.seqCamData.seq.endUpdate();
		detect.removeTempRectROIs(detect.seqCamData.seq);
		if (!stopFlag)
			detect.copyDetectedROIsToSequence(detect.seqCamData.seq);
		progressBar.close();
	}
	
	
}