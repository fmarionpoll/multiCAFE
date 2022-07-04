package plugins.fmp.multicafe2.series;


import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.sequence.Sequence;
import plugins.fmp.multicafe2.experiment.Experiment;

import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;





public class BuildBackground extends BuildSeries 
{
	public Sequence seqData = new Sequence();
	public Sequence seqThreshold = new Sequence();
	public Sequence seqReference = null;
	
	private Viewer vData = null;
	private Viewer vThreshold = null;
	private Viewer vReference = null;

	private FlyDetectTools flyDetectTools = new FlyDetectTools();	

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runBuildBackground(exp);
		
    }

	private void closeSequences () 
	{
		closeSequence(seqThreshold); 
		closeSequence(seqReference); 
		closeSequence(seqData);
	}
	
	private void closeViewers() 
	{
		closeViewer(vData);
		closeViewer(vThreshold); 
		closeViewer(vReference);
		closeSequences();
	}
	
	public void openViewers(Experiment exp) 
	{
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() 
				{			
					seqData = newSequence("data recorded", exp.seqCamData.getSeqImage(0, 0));
					vData = new Viewer(seqData, true);
					
					seqReference = newSequence("referenceImage", exp.seqCamData.refImage);
					exp.seqReference = seqReference;
					vReference = new Viewer(seqReference, true);

					seqThreshold = newSequence("positiveImage", exp.seqCamData.refImage);
					vThreshold = new Viewer(seqThreshold, true);
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void runBuildBackground(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		flyDetectTools.initParametersForDetection(exp, options);
		flyDetectTools.initTempRectROIs(exp, exp.seqCamData.seq, options.detectCage);
		options.threshold = options.thresholdDiff;
		
		openViewers(exp);
//		if (flag) 
//		{
			try {
				ImageTransformOptions transformOptions = new ImageTransformOptions();
				transformOptions.transformOption = EnumImageTransformations.SUBTRACT; 
//				transformOptions.referenceImage = exp.seqCamData.refImage;
				transformOptions.setSingleThreshold(options.threshold, stopFlag);
				buildBackgroundImage(exp, transformOptions);
				exp.saveReferenceImage(seqReference.getFirstImage());
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
//		}
		closeViewers();
	}
	
	private void buildBackgroundImage(Experiment exp, ImageTransformOptions transformOptions) throws InterruptedException 
	{
		ProgressFrame progress = new ProgressFrame("Build background image...");
		flyDetectTools.initParametersForDetection(exp, options);

		int t_from = (int) ((exp.cages.detectFirst_Ms - exp.camFirstImage_ms)/exp.camBinImage_ms);
		long limit = 50 ;
		if (limit > exp.seqCamData.nTotalFrames)
			limit = exp.seqCamData.nTotalFrames;
		limit = limit * exp.cages.detectBin_Ms +exp.cages.detectFirst_Ms ;
		
		exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.getSeqImage(t_from, 0));
		transformOptions.referenceImage = exp.seqCamData.refImage;
		ImageTransformInterface thresholdDifferenceWithRef = EnumImageTransformations.THRESHOLD_DIFF.getFunction();
		long first_ms = exp.cages.detectFirst_Ms + exp.cages.detectBin_Ms;
		int t0 = (int) ((first_ms - exp.cages.detectFirst_Ms)/exp.camBinImage_ms);

		for (long indexms = first_ms ; indexms <= limit && !stopFlag; indexms += exp.cages.detectBin_Ms) 
		{
			int t = (int) ((indexms - exp.cages.detectFirst_Ms)/exp.camBinImage_ms);
			if (t == t0)
				continue;
			
			IcyBufferedImage currentImage = imageIORead(exp.seqCamData.getFileName(t));
			seqData.setImage(0, 0, currentImage);
			
			IcyBufferedImage thresholdImage = thresholdDifferenceWithRef.transformImage(currentImage, transformOptions);
			seqThreshold.setImage(0, 0, thresholdImage);
			seqReference.setImage(0, 0, transformOptions.referenceImage);
			
//			System.out.println("t= "+t+ " n pixels changed=" + transformOptions.npixels_changed);
			if (transformOptions.npixels_changed < 10 && t > 0 ) 
				break;
		}
		exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(seqReference.getFirstImage());
		progress.close();
	}


}