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





public class FlyDetect3 extends BuildSeries 
{
	public Sequence seqDataRecorded = new Sequence();
	public Sequence seqPositive = new Sequence();
	public Sequence seqBackground = null;
	
	private Viewer vDataRecorded = null;
	private Viewer vPositive = null;
	private Viewer vBackgroundImage = null;

	private FlyDetectTools flyDetectTools = new FlyDetectTools();	

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runFlyDetect3(exp);
		
//		exp.cages.orderFlyPositions();
//		if (!stopFlag)
//			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
//		exp.seqCamData.closeSequence();
    }

	private void closeSequences () 
	{
		closeSequence(seqPositive); 
		closeSequence(seqBackground); 
		closeSequence(seqDataRecorded);
	}
	
	private void closeSequence(Sequence seq) 
	{
		if (seq != null) 
		{
			seq.close();
			seq = null;
		}
	}

	private void closeViewers() 
	{
		closeViewer (vPositive); 
		closeViewer (vBackgroundImage);
		closeViewer(vDataRecorded);
		closeSequences();
	}
	
	private void closeViewer (Viewer v)
	{
		if (v != null) 
		{
			v.close();
			v = null;
		}
	}

	public void openViewers(Experiment exp) 
	{
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
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
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
	
	private Sequence newSequence(String title, IcyBufferedImage image) 
	{
		Sequence seq = new Sequence();
		seq.setName("positiveImage");
		seq.setImage(0, 0, image);
		return seq;
	}
	

	private void runFlyDetect3(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		flyDetectTools.initParametersForDetection(exp, options);
		flyDetectTools.initTempRectROIs(exp, exp.seqCamData.seq, options.detectCage);
		options.threshold = options.thresholdDiff;
		
		boolean flag = options.forceBuildBackground;
		flag |= (!exp.loadReferenceImage());
		flag |= (exp.seqCamData.refImage == null);
		
		openViewers(exp);
		if (flag) 
		{
			try {
				ImageTransformOptions transformOptions = new ImageTransformOptions();
				transformOptions.transformOption = EnumImageTransformations.SUBTRACT; 
				transformOptions.referenceImage = exp.seqCamData.refImage;
				transformOptions.setSingleThreshold(options.threshold, stopFlag);
				buildBackgroundImage(exp, transformOptions);
				exp.saveReferenceImage(seqBackground.getFirstImage());
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
		ImageTransformInterface thresholdDifferenceWithRef = EnumImageTransformations.THRESHOLD_DIFF.getFunction();
		long first_ms = exp.cages.detectFirst_Ms + exp.cages.detectBin_Ms;

		for (long indexms = first_ms ; indexms<= limit && !stopFlag; indexms += exp.cages.detectBin_Ms) 
		{
			int t = (int) ((indexms - exp.cages.detectFirst_Ms)/exp.camBinImage_ms);
			
			IcyBufferedImage currentImage = imageIORead(exp.seqCamData.getFileName(t));
			seqDataRecorded.setImage(0, 0, currentImage);
			
			IcyBufferedImage thresholdImage = thresholdDifferenceWithRef.transformImage(currentImage, transformOptions);
			seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(thresholdImage, flyDetectTools.rectangleAllCages));
			
			seqDataRecorded.fireModelImageChangedEvent();
			seqPositive.fireModelImageChangedEvent();
			seqBackground.fireModelImageChangedEvent();
			
//			System.out.println("t= "+t+ " n pixels changed=" + transformOptions.npixels_changed);
			if (transformOptions.npixels_changed < 10 && t > 0 ) 
				break;
		}
		exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(seqBackground.getFirstImage());
		progress.close();
	}


}