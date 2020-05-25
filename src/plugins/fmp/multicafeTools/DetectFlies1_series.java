package plugins.fmp.multicafeTools;

import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;




public class DetectFlies1_series extends SwingWorker<Integer, Integer> {

	private Viewer 				viewerCamData 	= null;
	private OverlayThreshold 	ov 				= null;
	
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	public DetectFlies_Options 	detect 			= new DetectFlies_Options();

	// -----------------------------------------------------
	
	@Override
	protected Integer doInBackground() throws Exception  {
		System.out.println("start detect flies thread (v1)");
        threadRunning = true;
        int nbiterations = 0;
		ExperimentList expList = detect.expList;
		int nbexp = expList.index1 - expList.index0 +1;
		ProgressFrame progress = new ProgressFrame("Detect flies");
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag) 
				break;
			Experiment exp = expList.getExperiment(index);
			System.out.println(exp.experimentFileName);
			progress.setMessage("Processing file: " + (index-expList.index0 +1) + "//" + nbexp);
			
			exp.loadExperimentCamData();
			exp.xmlReadDrosoTrackDefault();
			exp.setCagesFrameStep (detect.stepFrame);
			if (detect.isFrameFixed) {
				exp.setCagesFrameStart (detect.startFrame);
				exp.setCagesFrameEnd (detect.endFrame);
				if (exp.getCagesFrameEnd() > (exp.getSeqCamSizeT() - 1))
					exp.setCagesFrameEnd (exp.getSeqCamSizeT() - 1);
			} else {
				exp.setCagesFrameStart (0);
				exp.setCagesFrameEnd (exp.seqCamData.seq.getSizeT() - 1);
			}
			
			if (exp.cages.cageList.size() < 1 ) {
				System.out.println("! skipped experiment with no cage: " + exp.experimentFileName);
				continue;
			}
			runDetectFlies(exp);
			if (!stopFlag)
				exp.xmlSaveFlyPositionsForAllCages();
			exp.seqCamData.closeSequence();
		}
		progress.close();
		threadRunning = false;
		return nbiterations;
    }

	@Override
	protected void done() {
		int statusMsg = 0;
		try {
			statusMsg = get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} 
		if (!threadRunning || stopFlag) {
			firePropertyChange("thread_ended", null, statusMsg);
		}
		else {
			firePropertyChange("thread_done", null, statusMsg);
		}
    }
		
	private void runDetectFlies(Experiment exp) {
		detect.initParametersForDetection(exp);
		detect.initTempRectROIs(exp, exp.seqCamData.seq);
		exp.cleanPreviousDetections();
		ProgressChrono progressBar = new ProgressChrono("Detecting flies...");
		progressBar.initChrono(exp.getCagesFrameEnd()-exp.getCagesFrameStart()+1);
		try {
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				viewerCamData = new Viewer(exp.seqCamData.seq, true);
				Rectangle rectv = viewerCamData.getBoundsInternal();
				rectv.setLocation(detect.parent0Rect.x+ detect.parent0Rect.width, detect.parent0Rect.y);
				viewerCamData.setBounds(rectv);
				ov = new OverlayThreshold(exp.seqCamData);
				exp.seqCamData.seq.addOverlay(ov);	
				ov.setThresholdSingle(exp.cages.detect.threshold, true);
				ov.painterChanged();
			}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		} 
		
		exp.seqCamData.seq.beginUpdate();
		int it = 0;
		for (int t = exp.getCagesFrameStart() ; t <= exp.getCagesFrameEnd(); t  += exp.getCagesFrameStep(), it++ ) {				
			if (stopFlag)
				break;
			progressBar.updatePosition(t);
			IcyBufferedImage workImage = exp.seqCamData.getImageAndSubtractReference(t, detect.transformop); 
			if (workImage == null)
				continue;
			exp.seqCamData.currentFrame = t;
			viewerCamData.setPositionT(t);
			viewerCamData.setTitle(exp.seqCamData.getDecoratedImageName(t));
			detect.findFlies (workImage, t, it);
		}
		exp.seqCamData.seq.endUpdate();
		
		detect.removeTempRectROIs(exp);
		detect.copyDetectedROIsToSequence(exp);
		detect.copyDetectedROIsToCages(exp);
		progressBar.close();
	}
	
	
}