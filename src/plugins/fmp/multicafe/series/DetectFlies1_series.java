package plugins.fmp.multicafe.series;

import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;

import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.tools.OverlayThreshold;



public class DetectFlies1_series extends BuildSeries {

	private Viewer 				viewerCamData 	= null;
	private OverlayThreshold 	ov 				= null;
	
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	public DetectFlies_Find 	find_flies 		= new DetectFlies_Find();
	
	// -----------------------------------------------------
	
	void analyzeExperiment(Experiment exp) {
		exp.openSequenceCamData();
		exp.xmlReadDrosoTrack(null);
		if (options.isFrameFixed) {
			exp.cages.detectFirst_Ms = options.t_firstMs;
			exp.cages.detectLast_Ms = options.t_lastMs;
			if (exp.cages.detectLast_Ms > exp.camLastImage_Ms)
				exp.cages.detectLast_Ms = exp.camLastImage_Ms;
		} else {
			exp.cages.detectFirst_Ms = exp.camFirstImage_Ms;
			exp.cages.detectLast_Ms = exp.camLastImage_Ms;
		}
		exp.cages.detectBin_Ms = options.t_binMs;
		exp.cages.detect_threshold = options.threshold;
		
		if (exp.cages.cageList.size() < 1 ) {
			System.out.println("! skipped experiment with no cage: " + exp.getExperimentFileName());
			return;
		}
		
		runDetectFlies(exp);
		
		if (!stopFlag)
			exp.xmlSaveFlyPositionsForAllCages();
		exp.seqCamData.closeSequence();
    }
		
	private void runDetectFlies(Experiment exp) {
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initTempRectROIs(exp, exp.seqCamData.seq);
		
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		try {
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				viewerCamData = new Viewer(exp.seqCamData.seq, true);
				Rectangle rectv = viewerCamData.getBoundsInternal();
				rectv.setLocation(options.parent0Rect.x+ options.parent0Rect.width, options.parent0Rect.y);
				viewerCamData.setBounds(rectv);
				ov = new OverlayThreshold(exp.seqCamData);
				exp.seqCamData.seq.addOverlay(ov);	
				ov.setThresholdSingle(exp.cages.detect_threshold, true);
				ov.painterChanged();
			}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		} 
		
		int nframes = (int) ((exp.cages.detectLast_Ms - exp.cages.detectFirst_Ms) / exp.cages.detectBin_Ms +1);
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detectFlies1");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		exp.seqCamData.seq.beginUpdate();
		
		int it = 0;
		for (long indexms = exp.cages.detectFirst_Ms ; indexms <= exp.cages.detectLast_Ms; indexms += exp.cages.detectBin_Ms, it++ ) {
			final int t_from = (int) ((indexms - exp.camFirstImage_Ms)/exp.camBinImage_Ms);
			final int t_it = it;
			futures.add(processor.submit(new Runnable () {
			@Override
			public void run() {	
				// TODO: getImageDirect (getImageDirectAndSubtractReference) does not communicate with viewer - remove visualization?
				IcyBufferedImage workImage = exp.seqCamData.subtractReference(exp.seqCamData.getImage(t_from, 0), t_from, options.transformop); 
				if (workImage == null)
					return;
				exp.seqCamData.currentFrame = t_from;
				viewerCamData.setPositionT(t_from);
				viewerCamData.setTitle(exp.seqCamData.getDecoratedImageName(t_from));
				find_flies.findFlies (workImage, t_from, t_it);
			}
			}));
		}
		
		waitAnalyzeExperimentCompletion(processor, futures, progressBar);
		exp.seqCamData.seq.endUpdate();

		progressBar.close();
		processor.shutdown();
	}
	
	
}