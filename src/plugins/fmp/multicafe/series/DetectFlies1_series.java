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
			return;
		}
		
		runDetectFlies(exp);
		
		if (!stopFlag)
			exp.xmlSaveFlyPositionsForAllCages();
		exp.seqCamData.closeSequence();
    }
		
	private void runDetectFlies(Experiment exp) {
		find_flies.initParametersForDetection(exp, options);
		find_flies.initTempRectROIs(exp, exp.seqCamData.seq);
		exp.cleanPreviousFliesDetections();
		
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
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
		
		int nframes = (exp.getKymoFrameEnd() - exp.getKymoFrameStart()) / exp.getKymoFrameStep() +1;
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detectFlies1");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		exp.seqCamData.seq.beginUpdate();
		
		int it = 0;
		for (int frame = exp.getCagesFrameStart() ; frame <= exp.getCagesFrameEnd(); frame  += exp.getCagesFrameStep(), it++ ) {				
			final int t_from = frame;
			final int t_it = it;
			futures.add(processor.submit(new Runnable () {
			@Override
			public void run() {	
				IcyBufferedImage workImage = exp.seqCamData.getImageAndSubtractReference(t_from, options.transformop); 
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
		
		find_flies.removeTempRectROIs(exp);
		find_flies.copyDetectedROIsToSequence(exp);
		find_flies.copyDetectedROIsToCages(exp);
		progressBar.close();
		processor.shutdown();
	}
	
	
}