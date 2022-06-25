package plugins.fmp.multicafe2.series;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.Future;
import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.BooleanMask2D;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.experiment.Cage;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;
import plugins.kernel.roi.roi2d.ROI2DArea;



public class FlyDetect2 extends BuildSeries 
{
	private Viewer viewerCamData;
	private Viewer vPositive = null;
	private Viewer vBackgroundImage = null;;
	private FlyDetectTools find_flies = new FlyDetectTools();	
	public Sequence seqNegative = new Sequence();
	public Sequence seqPositive = new Sequence();
	public boolean viewInternalImages = false;

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) 
	{
		if (!loadExperimentData(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runDetectFlies2(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
		exp.seqCamData.closeSequence();
    }
	
	private boolean loadExperimentData(Experiment exp) 
	{
		exp.seqCamData.seq = exp.seqCamData.initSequenceFromFirstImage(exp.seqCamData.getImagesList(true));
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
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}
		
	}
	
	private void runDetectFlies2(Experiment exp) 
	{
		if (seqNegative == null)
			seqNegative = new Sequence();
		if (seqPositive == null)
			seqPositive = new Sequence();
		
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initTempRectROIs(exp, exp.seqCamData.seq, options.detectCage);
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
		
		if (options.detectFlies) 
		{
			exp.cleanPreviousDetectedFliesROIs();
			findFlies(exp);
			exp.cages.orderFlyPositions();
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
		}
		closeSequences (exp);
	}
	
	private void closeSequences (Experiment exp) 
	{
		if (seqNegative != null) 
		{
			seqNegative.close();
			seqNegative = null;
		}
		
		if (seqPositive != null) 
		{
			seqPositive.close();
			seqPositive = null;
		}
		
		if (exp.seqBackgroundImage != null) 
		{
			exp.seqBackgroundImage.close();
			exp.seqBackgroundImage = null;
		}
	}

	private void closeViewers() 
	{
		if (vPositive != null) 
		{
			vPositive.close();
			vPositive = null;
		}
		if (vBackgroundImage != null) 
		{
			vBackgroundImage.close();
			vBackgroundImage = null;
		}
	}

	private void findFlies(Experiment exp) 
	{
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		closeViewers();
		find_flies.initTempRectROIs(exp, seqNegative, options.detectCage);

		int nframes = (int) ((exp.cages.detectLast_Ms - exp.cages.detectFirst_Ms) / exp.cages.detectBin_Ms +1);
		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detectFlies1");
	    processor.setPriority(Processor.NORM_PRIORITY);
	    ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		viewerCamData = exp.seqCamData.seq.getFirstViewer();
		exp.seqCamData.seq.beginUpdate();
		
		if (viewInternalImages)
			displayDetectViewer(exp);
		
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

	private void patchRectToReferenceImage(SequenceCamData seqCamData, IcyBufferedImage currentImage, Rectangle rect) 
	{
		int cmax = currentImage.getSizeC();
		for (int c = 0; c < cmax; c++) 
		{
			int[] intCurrentImage = Array1DUtil.arrayToIntArray(currentImage.getDataXY(c),
					currentImage.isSignedDataType());
			int[] intRefImage = Array1DUtil.arrayToIntArray(seqCamData.refImage.getDataXY(c),
					seqCamData.refImage.isSignedDataType());
			int xwidth = currentImage.getSizeX();
			for (int x = 0; x < rect.width; x++) 
			{
				for (int y = 0; y < rect.height; y++) 
				{
					int xi = rect.x + x;
					int yi = rect.y + y;
					int coord = xi + yi * xwidth;
					intRefImage[coord] = intCurrentImage[coord];
				}
			}
			Object destArray = seqCamData.refImage.getDataXY(c);
			Array1DUtil.intArrayToSafeArray(intRefImage, destArray, seqCamData.refImage.isSignedDataType(),
					seqCamData.refImage.isSignedDataType());
			seqCamData.refImage.setDataXY(c, destArray);
		}
		seqCamData.refImage.dataChanged();
	}

	private void displayDetectViewer(Experiment exp) 
	{
		try 
		{
			SwingUtilities.invokeAndWait(new Runnable() 
			{
				public void run() 
				{
					Viewer vNegative = new Viewer(seqNegative, false);
					seqNegative.setName("detectionImage");
					seqNegative.setImage(0, 0, exp.seqCamData.refImage);
					Point pt = viewerCamData.getLocation();
					if (vNegative != null) 
					{
						vNegative.setLocation(pt);
						vNegative.setVisible(true);
					}
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}

	}

	public void displayRefViewers(Experiment exp) 
	{
		try {
			SwingUtilities.invokeAndWait(new Runnable() 
				{
					public void run() 
					{
						if (exp.seqBackgroundImage == null)
							exp.seqBackgroundImage = new Sequence();
						if (vBackgroundImage == null)
							vBackgroundImage = new Viewer(exp.seqBackgroundImage, false);
						exp.seqBackgroundImage.setName("referenceImage");
						exp.seqBackgroundImage.setImage(0, 0,IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage, find_flies.rectangleAllCages));
						
						if (seqPositive == null)
							seqPositive = new Sequence();
						if (vPositive == null)
							vPositive = new Viewer(seqPositive, false);
						seqPositive.setName("positiveImage");
						seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage, find_flies.rectangleAllCages));
				
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
		find_flies.initParametersForDetection(exp, options);
		
		int t_from = (int) ((exp.cages.detectFirst_Ms - exp.camFirstImage_ms)/exp.camBinImage_ms);
		long limit = 50 ;
		if (limit > exp.seqCamData.nTotalFrames)
			limit = exp.seqCamData.nTotalFrames;
		limit = limit * exp.cages.detectBin_Ms +exp.cages.detectFirst_Ms ;
		exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.getSeqImage(t_from, 0));
		viewerCamData = exp.seqCamData.seq.getFirstViewer();
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = EnumImageTransformations.SUBTRACT_REF;
		transformOptions.referenceImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.refImage);
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();
		
		displayRefViewers(exp);
		
		int nFliesToRemove = 0;
		for (Cage cage: exp.cages.cagesList) 
		{
			cage.initialflyRemoved = false;
			if (cage.cageNFlies > 0)
				nFliesToRemove += cage.cageNFlies;
		}
		
		int t_previous = -1;
		for (long indexms = exp.cages.detectFirst_Ms ; indexms<= limit && !stopFlag; indexms += exp.cages.detectBin_Ms) 
		{
			int t = (int) ((indexms - exp.cages.detectFirst_Ms)/exp.camBinImage_ms);
			if (t == t_previous)
				continue;
			t_previous = t;
			
			IcyBufferedImage currentImage = imageIORead(exp.seqCamData.getFileName(t));
			IcyBufferedImage positiveImage = transformFunction.transformImage(currentImage, transformOptions);
			
			seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(positiveImage, find_flies.rectangleAllCages));
			ROI2DArea roiAll = find_flies.binarizeImage(positiveImage, options.thresholdBckgnd); 
			for (Cage cage: exp.cages.cagesList) 
			{
				if (cage.cageNFlies <1)
					continue;
				BooleanMask2D bestMask = find_flies.findLargestBlob(roiAll, cage);
				if (bestMask != null) 
				{
					ROI2DArea flyROI = new ROI2DArea(bestMask);
					if (!cage.initialflyRemoved) 
					{
						Rectangle rect = flyROI.getBounds();
						patchRectToReferenceImage(exp.seqCamData, currentImage, rect);

						cage.initialflyRemoved = true;
						nFliesToRemove--;
						if (exp.seqBackgroundImage != null)
							exp.seqBackgroundImage.setImage(0, 0, IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage,
									find_flies.rectangleAllCages));
					}
				}
			}
			if (nFliesToRemove < 1)
				break;
		}
		progress.close();
	}


}