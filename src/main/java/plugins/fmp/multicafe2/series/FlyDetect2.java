package plugins.fmp.multicafe2.series;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;




public class FlyDetect2 extends BuildSeries 
{
	private Viewer vNegative = null;
	private FlyDetectTools find_flies = new FlyDetectTools();	
	private Sequence seqNegative = null;
	public boolean viewInternalImages = true;

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runFlyDetect2(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
		exp.seqCamData.closeSequence();
    }
	
	private void openViewers(Experiment exp) 
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
	
	private void runFlyDetect2(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initTempRectROIs(exp, exp.seqCamData.seq, options.detectCage);
		options.threshold = options.thresholdDiff;

		if (exp.loadReferenceImage()) 
		{
			openViewers(exp);
			findFliesInAllFrame(exp);
			exp.cages.orderFlyPositions();
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
			closeViewers();
		}
		
		closeSequences (exp);
	}
	
	private void closeSequences (Experiment exp) 
	{
		closeSequence(seqNegative); 
	}

	private void closeViewers() 
	{
		closeViewer(vNegative);
	}

	private void findFliesInAllFrame(Experiment exp) 
	{
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		find_flies.initTempRectROIs(exp, seqNegative, options.detectCage);
		seqNegative.removeAllROI();

		int nframes = (int) ((exp.cages.detectLast_Ms - exp.cages.detectFirst_Ms) / exp.cages.detectBin_Ms +1);
		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detectFlies1");
	    processor.setPriority(Processor.NORM_PRIORITY);
	    ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = EnumImageTransformations.SUBTRACT_REF;
		transformOptions.referenceImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.refImage);
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();
		long last_ms = exp.cages.detectLast_Ms + exp.cages.detectBin_Ms ;
		
		for (long indexms = exp.cages.detectFirst_Ms ; indexms <= last_ms; indexms += exp.cages.detectBin_Ms ) 
		{
			final int t_from = (int) ((indexms - exp.camFirstImage_ms)/exp.camBinImage_ms);
			if (t_from >= exp.seqCamData.nTotalFrames)
				continue;
			
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{	
					IcyBufferedImage workImage = imageIORead(exp.seqCamData.getFileName(t_from));
					IcyBufferedImage negativeImage = transformFunction.transformImage(workImage, transformOptions);
					try {
						seqNegative.setImage(0, 0, negativeImage);
						find_flies.findFlies2(seqNegative, negativeImage, t_from);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}}));
		}
		waitFuturesCompletion(processor, futures, progressBar);
		
		progressBar.close();
		processor.shutdown();
	}

}