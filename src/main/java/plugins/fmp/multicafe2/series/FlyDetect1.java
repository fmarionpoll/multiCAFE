package plugins.fmp.multicafe2.series;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;





public class FlyDetect1 extends BuildSeries 
{
	public boolean buildBackground	= true;
	public boolean	detectFlies = true;
	private Viewer vNegative = null;
	private Sequence seqNegative = null;
	public FlyDetectTools 		find_flies 		= new FlyDetectTools();
	
	// -----------------------------------------------------
	
	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;
		
		runFlyDetect1(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
		exp.seqCamData.closeSequence();
    }
	
	private void openViewerWithOverlay(Experiment exp) 
	{
		try 
		{
			SwingUtilities.invokeAndWait(new Runnable() 
			{ 
				public void run() 
				{
					seqNegative = newSequence("detectionImage", exp.seqCamData.refImage);
					vNegative = new Viewer (seqNegative, false);
					vNegative.setVisible(true);
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		} 
	}
	
	private void runFlyDetect1(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initTempRectROIs(exp, exp.seqCamData.seq, options.detectCage);
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		openViewerWithOverlay(exp);
		
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = options.transformop;
		getReferenceImage (exp, 0, transformOptions);
		ImageTransformInterface transformFunction = options.transformop.getFunction();
		
//		int nframes = (int) ((exp.cages.detectLast_Ms - exp.cages.detectFirst_Ms) / exp.cages.detectBin_Ms +1);
//	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
//	    processor.setThreadName("detectFlies1");
//	    processor.setPriority(Processor.NORM_PRIORITY);
//        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
//		futures.clear();
		
		exp.seqCamData.seq.beginUpdate();
//		seqNegative.beginUpdate();
		
		int t_current = 0;
	
		long last_ms = exp.cages.detectLast_Ms + exp.cages.detectBin_Ms ;
		for (long index_ms = exp.cages.detectFirst_Ms; index_ms <= last_ms; index_ms += exp.cages.detectBin_Ms ) 
		{
			final int t_previous = t_current;
			final int t_from = (int) ((index_ms - exp.camFirstImage_ms)/exp.camBinImage_ms);
			if (t_from >= exp.seqCamData.nTotalFrames)
				continue;
			
			t_current = t_from;
			progressBar.setMessage("Processing image: " + (t_from +1));
			
//			futures.add(processor.submit(new Runnable () 
//			{
//				@Override
//				public void run() 
//				{	
					IcyBufferedImage sourceImage = imageIORead(exp.seqCamData.getFileName(t_from));
					getReferenceImage (exp, t_previous, transformOptions);
					IcyBufferedImage workImage = transformFunction.transformImage(sourceImage, transformOptions); 
					if (workImage == null)
						return;

					try 
					{
						seqNegative.setImage(0, 0, workImage);
						find_flies.findFlies1 (workImage, t_from);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}					
//				}}));
		}
		
//		waitFuturesCompletion(processor, futures, progressBar);
		
		exp.seqCamData.seq.endUpdate();
//		seqNegative.endUpdate();

		progressBar.close();
//		processor.shutdown();
	}
	
	private void getReferenceImage (Experiment exp, int t, ImageTransformOptions options) 
	{
		switch (options.transformOption) 
		{
			case SUBTRACT_TM1: 
				options.referenceImage = imageIORead(exp.seqCamData.getFileName(t));
				break;
				
			case SUBTRACT_T0:
			case SUBTRACT_REF:
				if (options.referenceImage == null)
					options.referenceImage = imageIORead(exp.seqCamData.getFileName(0));
				break;
				
			case NONE:
			default:
				break;
		}
	}
	
	
}