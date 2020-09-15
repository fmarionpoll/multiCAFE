package plugins.fmp.multicafe.tools.detect;

import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.tools.OverlayThreshold;
import plugins.fmp.multicafe.tools.ProgressChrono;




public class DetectFlies1_series extends SwingWorker<Integer, Integer> {

	private Viewer 				viewerCamData 	= null;
	private OverlayThreshold 	ov 				= null;
	
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	public DetectFlies_Options	options			= null;
	public DetectFlies_Find 	find_flies 		= new DetectFlies_Find();
	

	// -----------------------------------------------------
	
	@Override
	protected Integer doInBackground() throws Exception  {
		if (options == null)
			return 0;
		System.out.println("start detect flies thread (v1)");
        threadRunning = true;
        int nbiterations = 0;
		ExperimentList expList = options.expList;
		ProgressFrame progress = new ProgressFrame("Detect flies");
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag) 
				break;
			Experiment exp = expList.getExperiment(index);
			System.out.println((index+1)+": " +exp.getExperimentFileName());
			progress.setMessage("Processing file: " + (index+1) + "//" + (expList.index1+1));
			
			exp.resultsSubPath = options.resultsSubPath;
			exp.getResultsDirectory(); 
			
			exp.xmlLoadExperiment();
			exp.seqCamData.loadSequence(exp.getExperimentFileName()) ;
			exp.xmlReadDrosoTrack(null);
			exp.setCagesFrameStep (options.df_stepFrame);
			if (options.isFrameFixed) {
				exp.setCagesFrameStart (options.df_startFrame);
				exp.setCagesFrameEnd (options.df_endFrame);
				if (exp.getCagesFrameEnd() > (exp.getSeqCamSizeT() - 1))
					exp.setCagesFrameEnd (exp.getSeqCamSizeT() - 1);
			} else {
				exp.setCagesFrameStart (0);
				exp.setCagesFrameEnd (exp.seqCamData.seq.getSizeT() - 1);
			}
			
			if (exp.cages.cageList.size() < 1 ) {
				System.out.println("! skipped experiment with no cage: " + exp.getExperimentFileName());
				continue;
			}
			System.out.println((index+1) + " - "+ exp.getExperimentFileName() + " " + exp.resultsSubPath);
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
		find_flies.initParametersForDetection(exp, options);
		find_flies.initTempRectROIs(exp, exp.seqCamData.seq);
		exp.cleanPreviousFliesDetections();
		ProgressChrono progressBar = new ProgressChrono("Detecting flies...");
		progressBar.initChrono(exp.getCagesFrameEnd()-exp.getCagesFrameStart()+1);
		try {
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				viewerCamData = new Viewer(exp.seqCamData.seq, true);
				Rectangle rectv = viewerCamData.getBoundsInternal();
				rectv.setLocation(options.parent0Rect.x+ options.parent0Rect.width, options.parent0Rect.y);
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
			IcyBufferedImage workImage = exp.seqCamData.getImageAndSubtractReference(t, options.transformop); 
			if (workImage == null)
				continue;
			exp.seqCamData.currentFrame = t;
			viewerCamData.setPositionT(t);
			viewerCamData.setTitle(exp.seqCamData.getDecoratedImageName(t));
			find_flies.findFlies (workImage, t, it);
		}
		exp.seqCamData.seq.endUpdate();
		
		find_flies.removeTempRectROIs(exp);
		find_flies.copyDetectedROIsToSequence(exp);
		find_flies.copyDetectedROIsToCages(exp);
		progressBar.close();
	}
	
	
}