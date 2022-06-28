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
				buildBackgroundImage(exp);
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

	private void buildBackgroundImage(Experiment exp) throws InterruptedException 
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
		
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = EnumImageTransformations.SUBTRACT; //SUBTRACT_REF;
		//transformOptions.referenceImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.refImage);
		transformOptions.referenceImage = exp.seqCamData.refImage;
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();
		
		displayRefViewers(exp);
		
//		int nFliesToRemove = 0;
//		for (Cage cage: exp.cages.cagesList) 
//		{
//			cage.initialflyRemoved = false;
//			if (cage.cageNFlies > 0)
//				nFliesToRemove += cage.cageNFlies;
//		}
		
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
			
//			ROI2DArea roiAll = flyDetectTools.binarizeImage(positiveImage, options.thresholdBckgnd); 
//			for (Cage cage: exp.cages.cagesList) 			
//			{
//				if (cage.cageNFlies <1)
//					continue;
//				BooleanMask2D bestMask = flyDetectTools.findLargestBlob(roiAll, cage);
//				if (bestMask != null) 
//				{
//					ROI2DArea flyROI = new ROI2DArea(bestMask);
//					exp.seqBackgroundImage.addROI(flyROI);
//					if (!cage.initialflyRemoved) 
//					{
//						Rectangle rect = flyROI.getBounds();
//						patchRectToReferenceImage(exp.seqCamData, currentImage, rect);
//
//						cage.initialflyRemoved = true;
//						nFliesToRemove--;
//						if (exp.seqBackgroundImage != null)
//							exp.seqBackgroundImage.setImage(0, 0, IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage,
//									flyDetectTools.rectangleAllCages));
//					}
//				}
//			}
//			if (nFliesToRemove < 1)
//				break;
		}
//		exp.seqBackgroundImage.removeAllROI();
		progress.close();
	}
//
//	private void patchRectToReferenceImage(SequenceCamData seqCamData, IcyBufferedImage currentImage, Rectangle rect) 
//	{
//		int cmax = currentImage.getSizeC();
//		for (int c = 0; c < cmax; c++) 
//		{
//			int[] intCurrentImage = Array1DUtil.arrayToIntArray(currentImage.getDataXY(c),
//					currentImage.isSignedDataType());
//			int[] intRefImage = Array1DUtil.arrayToIntArray(seqCamData.refImage.getDataXY(c),
//					seqCamData.refImage.isSignedDataType());
//			int xwidth = currentImage.getSizeX();
//			for (int x = 0; x < rect.width; x++) 
//			{
//				for (int y = 0; y < rect.height; y++) 
//				{
//					int xi = rect.x + x;
//					int yi = rect.y + y;
//					int coord = xi + yi * xwidth;
//					intRefImage[coord] = intCurrentImage[coord];
//				}
//			}
//			Object destArray = seqCamData.refImage.getDataXY(c);
//			Array1DUtil.intArrayToSafeArray(intRefImage, destArray, seqCamData.refImage.isSignedDataType(),
//					seqCamData.refImage.isSignedDataType());
//			seqCamData.refImage.setDataXY(c, destArray);
//		}
//		seqCamData.refImage.dataChanged();
//	}

}