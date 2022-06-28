package plugins.fmp.multicafe2.series;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.BooleanMask2D;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.experiment.Cage;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;
import plugins.kernel.roi.roi2d.ROI2DArea;

public class FlyDetect3 extends BuildSeries 
{
	private Viewer viewerCamData;
	
	public Sequence seqPositive = new Sequence();
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
		closeSequences (exp);
	}
	
	private void closeSequences (Experiment exp) 
	{
		
		closeSequence (seqPositive); 
		closeSequence(exp.seqBackgroundImage); 
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

	public void displayRefViewers(Experiment exp) 
	{
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() 
				{
					if (exp.seqBackgroundImage == null)
						exp.seqBackgroundImage = new Sequence();
					if (vBackgroundImage == null)
						vBackgroundImage = new Viewer(exp.seqBackgroundImage, false);
					exp.seqBackgroundImage.setName("referenceImage");
					exp.seqBackgroundImage.setImage(0, 0,IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage, flyDetectTools.rectangleAllCages));
					
					if (seqPositive == null)
						seqPositive = new Sequence();
					if (vPositive == null)
						vPositive = new Viewer(seqPositive, false);
					seqPositive.setName("positiveImage");
					seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage, flyDetectTools.rectangleAllCages));
			
					viewerCamData = exp.seqCamData.seq.getFirstViewer();
					Point pt = viewerCamData.getLocation();
					int height = viewerCamData.getHeight();
					pt.y += height;
			
					if (vPositive != null) 
					{
						vPositive.setVisible(true);
						vPositive.setLocation(pt);
					}
					
					if (vBackgroundImage != null) 
					{
						vBackgroundImage.setVisible(true);
						vBackgroundImage.setLocation(pt);
					}
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}
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
		viewerCamData = exp.seqCamData.seq.getFirstViewer();
	
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();
		
		displayRefViewers(exp);
		
		int t_previous = -1;
		for (long indexms = exp.cages.detectFirst_Ms ; indexms<= limit && !stopFlag; indexms += exp.cages.detectBin_Ms) 
		{
			int t = (int) ((indexms - exp.cages.detectFirst_Ms)/exp.camBinImage_ms);
			if (t == t_previous)
				continue;
			t_previous = t;
			
			IcyBufferedImage currentImage = imageIORead(exp.seqCamData.getFileName(t));
			IcyBufferedImage positiveImage = transformFunction.transformImage(currentImage, transformOptions);
			seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(positiveImage, flyDetectTools.rectangleAllCages));
//			seqPositive.fireModelImageChangedEvent();
			
		}
		progress.close();
	}
	
//	public IcyBufferedImage getTransformedImage(int t) 
//	{
//		IcyBufferedImage img = localSeq.getImage(t, 0);
//		IcyBufferedImage img2 = imageTransformFunction.transformImage(img, imageTransformOptions);
//		return imageThresholdFunction.transformImage(img2, imageTransformOptions);
//	}


}