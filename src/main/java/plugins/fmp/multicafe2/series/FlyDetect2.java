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
	private Viewer vDataRecorded = null;
	private Viewer vPositive = null;
	private Viewer vBackgroundImage = null;
	private Viewer vNegative = null;
	private FlyDetectTools find_flies = new FlyDetectTools();	
	
	public Sequence seqDataRecorded = new Sequence();
	public Sequence seqNegative = new Sequence();
	public Sequence seqPositive = new Sequence();
	public Sequence seqBackground = null;
	
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
					seqDataRecorded = newSequence("data recorded", exp.seqCamData.getSeqImage(0, 0));
					vDataRecorded = new Viewer(seqDataRecorded, false);
					vDataRecorded.setVisible(true);
					
					seqBackground = newSequence("referenceImage", exp.seqCamData.refImage);
					exp.seqBackground = seqBackground;
					vBackgroundImage = new Viewer(exp.seqBackground, false);
					vBackgroundImage.setVisible(true);
					
					seqPositive = newSequence("positiveImage", exp.seqCamData.refImage);
					vPositive = new Viewer(seqPositive, false);
					vPositive.setVisible(true);
					
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
		if (seqNegative == null)
			seqNegative = new Sequence();
		if (seqPositive == null)
			seqPositive = new Sequence();
		
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initTempRectROIs(exp, exp.seqCamData.seq, options.detectCage);
		options.threshold = options.thresholdDiff;
		openViewers(exp);
		
		boolean flag = options.forceBuildBackground;
		flag |= (!exp.loadReferenceImage());
		flag |= (exp.seqCamData.refImage == null);
		if (!flag) 
		{
			closeViewers();
			findFliesInAllFrame(exp);
			exp.cages.orderFlyPositions();
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
		}
		closeSequences (exp);
	}
	
	private void closeSequences (Experiment exp) 
	{
		closeSequence(seqNegative); 
		closeSequence(seqPositive); 
		closeSequence(seqNegative);
		closeSequence(exp.seqBackground); 
	}

	private void closeViewers() 
	{
		closeViewer(vDataRecorded);
		closeViewer(vPositive); 
		closeViewer(vNegative);
		closeViewer(vBackgroundImage); 
	}


	private void findFliesInAllFrame(Experiment exp) 
	{
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		find_flies.initTempRectROIs(exp, seqNegative, options.detectCage);

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
		
		for (long indexms = exp.cages.detectFirst_Ms ; indexms <= exp.cages.detectLast_Ms; indexms += exp.cages.detectBin_Ms ) 
		{
			final int t_from = (int) ((indexms - exp.camFirstImage_ms)/exp.camBinImage_ms);
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{	
					IcyBufferedImage workImage = imageIORead(exp.seqCamData.getFileName(t_from));
					if (workImage == null)
						return;
					
					IcyBufferedImage currentImage = IcyBufferedImageUtil.getCopy(workImage);				
					IcyBufferedImage negativeImage = transformFunction.transformImage(currentImage, transformOptions);
					try {
						find_flies.findFlies(negativeImage, t_from);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}}));
		}
		waitFuturesCompletion(processor, futures, progressBar);
		
		exp.seqCamData.seq.endUpdate();
		seqNegative.close();
		
		progressBar.close();
		processor.shutdown();
	}

}