package plugins.fmp.multicafe2.series;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.sequence.Sequence;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;
import plugins.fmp.multicafe2.tools.Overlay.OverlayThreshold;




public class FlyDetect3 extends BuildSeries 
{
	private Viewer viewerCamData;
	
	public Sequence seqPositive = new Sequence();
	public Sequence seqBackground = null;
	private Viewer vPositive = null;
	
	private Viewer vBackgroundImage = null;
	private OverlayThreshold ov = null;
	
	private FlyDetectTools flyDetectTools = new FlyDetectTools();	

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runFlyDetect3(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
		exp.seqCamData.closeSequence();
    }
	
	private void openViewer(Experiment exp) 
	{
		try 
		{
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() 
				{
					viewerCamData = new Viewer(exp.seqCamData.seq, true);
					Rectangle rectv = viewerCamData.getBoundsInternal();
					rectv.setLocation(options.parent0Rect.x+ options.parent0Rect.width, options.parent0Rect.y);
					viewerCamData.setBounds(rectv);
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}
		
	}
	
	private void closeSequences () 
	{
		
		closeSequence(seqPositive); 
		closeSequence(seqBackground); 
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
	}
	
	private void closeViewer (Viewer v)
	{
		if (v != null) 
		{
			v.close();
			v = null;
		}
	}

	public void displayViewers(Experiment exp) 
	{
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() 
				{
					viewerCamData = exp.seqCamData.seq.getFirstViewer();
					Point pt = viewerCamData.getLocation();
					int height = viewerCamData.getHeight();
					pt.y += height;
					
					if (exp.seqBackground == null)
						exp.seqBackground = new Sequence();
					exp.seqBackground.setName("referenceImage");
					exp.seqBackground.setImage(0, 0,IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage, flyDetectTools.rectangleAllCages));
					if (vBackgroundImage == null)
						vBackgroundImage = new Viewer(exp.seqBackground, false);
					if (vBackgroundImage != null) 
					{
						vBackgroundImage.setVisible(true);
						vBackgroundImage.setLocation(pt);
					}
					seqBackground = exp.seqBackground;
					
					if (seqPositive == null)
						seqPositive = new Sequence();
					seqPositive.setName("positiveImage");
					seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage, flyDetectTools.rectangleAllCages));
					if (vPositive == null)
						vPositive = new Viewer(seqPositive, false);
					if (vPositive != null) 
					{
						vPositive.setVisible(true);
						vPositive.setLocation(pt);
					}
					
					
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
	
	private Viewer newSequence() {
		
	}
	
	private Sequence newSequence() {
		
	}

	private void runFlyDetect3(Experiment exp) 
	{
		if (seqPositive == null)
			seqPositive = new Sequence();
		
		exp.cleanPreviousDetectedFliesROIs();
		flyDetectTools.initParametersForDetection(exp, options);
		flyDetectTools.initTempRectROIs(exp, exp.seqCamData.seq, options.detectCage);
		options.threshold = options.thresholdDiff;
		openViewer(exp);
		
		boolean flag = options.forceBuildBackground;
		flag |= (!exp.loadReferenceImage());
		flag |= (exp.seqCamData.refImage == null);
		if (flag) 
		{
			try {
				ImageTransformOptions transformOptions = new ImageTransformOptions();
				transformOptions.transformOption = EnumImageTransformations.SUBTRACT; //SUBTRACT_REF;
				//transformOptions.referenceImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.refImage);
				transformOptions.referenceImage = exp.seqCamData.refImage;
				transformOptions.setSingleThreshold(options.threshold, stopFlag);
				buildBackgroundImage(exp, transformOptions);
				exp.saveReferenceImage();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		closeViewers();
		closeSequences();
	}
	
	private void buildBackgroundImage(Experiment exp, ImageTransformOptions transformOptions) throws InterruptedException 
	{
		ProgressFrame progress = new ProgressFrame("Build background image...");
		flyDetectTools.initParametersForDetection(exp, options);
		displayViewers(exp);
		
		
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
			IcyBufferedImage thresholdImage = thresholdDifferenceWithRef.transformImage(currentImage, transformOptions);
			seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(thresholdImage, flyDetectTools.rectangleAllCages));
			
			seqPositive.fireModelImageChangedEvent();
			seqBackground.fireModelImageChangedEvent();
			
			System.out.println("t= "+t+ " n pixels changed=" + transformOptions.npixels_changed);
			if (transformOptions.npixels_changed < 10 && t > 0 ) 
				break;

		}
		progress.close();
	}


}