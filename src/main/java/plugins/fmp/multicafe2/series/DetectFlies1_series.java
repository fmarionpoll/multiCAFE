package plugins.fmp.multicafe2.series;

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
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.OverlayThreshold;




public class DetectFlies1_series extends BuildSeries 
{
	private Viewer 				viewerCamData 	= null;
	private OverlayThreshold 	ov 				= null;
	
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	public DetectFlies_Find 	find_flies 		= new DetectFlies_Find();
	
	// -----------------------------------------------------
	
	void analyzeExperiment(Experiment exp) 
	{
		if (!loadExperimentData(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;
		
		runDetectFlies1(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
		exp.seqCamData.closeSequence();
    }
	
	private boolean loadExperimentData(Experiment exp) 
	{
		exp.seqCamData.seq = exp.seqCamData.initV2SequenceFromFirstImage(exp.seqCamData.getImagesList());
		boolean flag = exp.xmlReadDrosoTrack(null);
		return flag;
	}
	
	private void openViewer(Experiment exp) 
	{
		try 
		{
			SwingUtilities.invokeAndWait(new Runnable() 
			{ 
				public void run() 
				{
					viewerCamData = new Viewer(exp.seqCamData.seq, true);
					Rectangle rectv = viewerCamData.getBoundsInternal();
					rectv.setLocation(options.parent0Rect.x+ options.parent0Rect.width, options.parent0Rect.y);
					viewerCamData.setBounds(rectv);
					ov = new OverlayThreshold(exp.seqCamData);
					exp.seqCamData.seq.addOverlay(ov);	
					ov.setThresholdSingle(exp.cages.detect_threshold, true);
					ov.painterChanged();
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		} 
		
	}
	
	private void runDetectFlies1(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initTempRectROIs(exp, exp.seqCamData.seq, options.detectCage);
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		openViewer(exp);
		
		int nframes = (int) ((exp.cages.detectLast_Ms - exp.cages.detectFirst_Ms) / exp.cages.detectBin_Ms +1);
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detectFlies1");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		exp.seqCamData.seq.beginUpdate();
	
		for (long indexms = exp.cages.detectFirst_Ms ; indexms <= exp.cages.detectLast_Ms; indexms += exp.cages.detectBin_Ms ) 
		{
			final int t_from = (int) ((indexms - exp.camFirstImage_Ms)/exp.camBinImage_Ms);
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{	
					final IcyBufferedImage  sourceImage = exp.seqCamData.getSeqImage(t_from, 0);
					IcyBufferedImage workImage = exp.seqCamData.subtractReference(sourceImage, t_from, options.transformop); 
					if (workImage == null)
						return;

					exp.seqCamData.currentFrame = t_from;
					viewerCamData.setPositionT(t_from);
					viewerCamData.setTitle(exp.seqCamData.getDecoratedImageName(t_from));
					find_flies.findFlies (workImage, t_from);					
				}}));
		}
		
		waitAnalyzeExperimentCompletion(processor, futures, progressBar);
		exp.seqCamData.seq.endUpdate();

		progressBar.close();
		processor.shutdown();
	}
	
	
}