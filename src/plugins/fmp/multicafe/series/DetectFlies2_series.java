package plugins.fmp.multicafe.series;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
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
import plugins.fmp.multicafe.sequence.Cage;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.SequenceCamData;
import plugins.kernel.roi.roi2d.ROI2DArea;



public class DetectFlies2_series extends BuildSeries {

	private List<Boolean> initialflyRemovedList = new ArrayList<Boolean>();
	private Viewer viewerCamData;
	private Viewer vPositive = null;
	private Viewer vBackgroundImage = null;;
	private DetectFlies_Find find_flies = new DetectFlies_Find();
	
	public Sequence seqNegative = new Sequence();
	public Sequence seqPositive = new Sequence();
	public boolean viewInternalImages = false;

	// -----------------------------------------
	
	void analyzeExperiment(Experiment exp) {
		exp.openSequenceCamData();
		exp.xmlReadDrosoTrack(null);
		if (options.isFrameFixed) {
			exp.cages.detectFirst_Ms = options.t_firstMs;
			exp.cages.detectLast_Ms = options.t_lastMs;
			if (exp.cages.detectLast_Ms > exp.camLastImage_Ms)
				exp.cages.detectLast_Ms = exp.camLastImage_Ms;
		} else {
			exp.cages.detectFirst_Ms = exp.camFirstImage_Ms;
			exp.cages.detectLast_Ms = exp.camLastImage_Ms;
		}
		exp.cages.detectBin_Ms = options.t_binMs;
		exp.cages.detect_threshold = options.threshold;
		
		if (exp.cages.cageList.size() < 1 ) {
			System.out.println("! skipped experiment with no cage: " + exp.getExperimentFileName());
		} else {
			runDetectFlies(exp);
		}
		exp.seqCamData.closeSequence();
	}
	
	private void runDetectFlies(Experiment exp) {
		if (seqNegative == null)
			seqNegative = new Sequence();
		if (seqPositive == null)
			seqPositive = new Sequence();
		
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initTempRectROIs(exp, exp.seqCamData.seq);
		options.threshold = options.thresholdDiff;
		
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					viewerCamData = new Viewer(exp.seqCamData.seq, true);
					Rectangle rectv = viewerCamData.getBoundsInternal();
					rectv.setLocation(options.parent0Rect.x+ options.parent0Rect.width, options.parent0Rect.y);
					viewerCamData.setBounds(rectv);
				}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		
		boolean flag = options.forceBuildBackground;
		flag |= (!exp.loadReferenceImage());
		flag |= (exp.seqCamData.refImage == null);
		if (flag) {
			System.out.println(" buildbackground");
			buildBackgroundImage(exp);
			exp.saveReferenceImage();
		}
		
		if (options.detectFlies) {
			exp.cleanPreviousDetectedFliesROIs();
			findFlies(exp);
			exp.xmlSaveFlyPositionsForAllCages();
		}
		closeViewersAndSequences (exp);
	}
	
	private void closeViewersAndSequences (Experiment exp) {
		if (seqNegative != null) {
			seqNegative.close();
			seqNegative = null;
		}
		if (seqPositive != null) {
			if (vPositive != null)
				vPositive.close();
			seqPositive.close();
			seqPositive = null;
		}
		if (exp.seqBackgroundImage != null) {
			exp.seqBackgroundImage.close();
			exp.seqBackgroundImage = null;
		}
	}

	private void findFlies(Experiment exp) {
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");

		exp.seqBackgroundImage.close();
		if (vPositive != null) {
			vPositive.close();
			vPositive = null;
		}
		if (vBackgroundImage != null) {
			vBackgroundImage.close();
			vBackgroundImage = null;
		}
		find_flies.initTempRectROIs(exp, seqNegative);

		final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detectFlies1");
	    processor.setPriority(Processor.NORM_PRIORITY);
        try {
			int nframes = (int) ((exp.cages.detectLast_Ms - exp.cages.detectFirst_Ms) / exp.cages.detectBin_Ms +1);
			ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
			futures.clear();
			
			viewerCamData = exp.seqCamData.seq.getFirstViewer();
			exp.seqCamData.seq.beginUpdate();
			if (viewInternalImages)
				displayDetectViewer(exp);

			// ----------------- loop over all images of the stack
			int it = 0;
			for (long indexms = exp.cages.detectFirst_Ms ; indexms <= exp.cages.detectLast_Ms; indexms += exp.cages.detectBin_Ms, it++ ) {
				final int t_from = (int) ((indexms - exp.camFirstImage_Ms)/exp.camBinImage_Ms);
				final int t_it = it;
				futures.add(processor.submit(new Runnable () {
				@Override
				public void run() {	
					IcyBufferedImage workImage = exp.seqCamData.getImageDirect(t_from);
					if (workImage == null)
						return;
					IcyBufferedImage currentImage = IcyBufferedImageUtil.getCopy(workImage);
					exp.seqCamData.currentFrame = t_from;
					seqNegative.beginUpdate();
					IcyBufferedImage negativeImage = exp.seqCamData.subtractImages(exp.seqCamData.refImage, currentImage);
					find_flies.findFlies(negativeImage, t_from, t_it);
					seqNegative.setImage(0, 0, negativeImage);
					seqNegative.endUpdate();
				}
				}));
			}
			waitAnalyzeExperimentCompletion(processor, futures, progressBar);
			
		} finally {
			exp.seqCamData.seq.endUpdate();
			seqNegative.close();
//			find_flies.copyDetectedROIsToSequence(exp);
//			find_flies.copyDetectedROIsToCages(exp);
		}
		progressBar.close();
		processor.shutdown();
	}

	private void patchRectToReferenceImage(SequenceCamData seqCamData, IcyBufferedImage currentImage, Rectangle rect) {
		int cmax = currentImage.getSizeC();
		for (int c = 0; c < cmax; c++) {
			int[] intCurrentImage = Array1DUtil.arrayToIntArray(currentImage.getDataXY(c),
					currentImage.isSignedDataType());
			int[] intRefImage = Array1DUtil.arrayToIntArray(seqCamData.refImage.getDataXY(c),
					seqCamData.refImage.isSignedDataType());
			int xwidth = currentImage.getSizeX();
			for (int x = 0; x < rect.width; x++) {
				for (int y = 0; y < rect.height; y++) {
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

	private void displayDetectViewer(Experiment exp) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					Viewer vNegative = new Viewer(seqNegative, false);
					seqNegative.setName("detectionImage");
					seqNegative.setImage(0, 0, exp.seqCamData.refImage);
					Point pt = viewerCamData.getLocation();
					if (vNegative != null) {
						vNegative.setLocation(pt);
						vNegative.setVisible(true);
					}
				}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	public void displayRefViewers(Experiment exp) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
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
			
					if (vPositive != null) {
						vPositive.setVisible(true);
						vPositive.setLocation(pt);
					}
					if (vBackgroundImage != null) {
						vBackgroundImage.setVisible(true);
						vBackgroundImage.setLocation(pt);
					}
				}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void buildBackgroundImage(Experiment exp) {
		ProgressFrame progress = new ProgressFrame("Build background image...");
		int nfliesRemoved = 0; //
		find_flies.initParametersForDetection(exp, options);
		
		int t_from = (int) ((exp.cages.detectFirst_Ms - exp.camFirstImage_Ms)/exp.camBinImage_Ms);
		exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.getImage(t_from, 0));
		initialflyRemovedList.clear();
		int ndetectcages = exp.cages.cageList.size();
		for (int i = 0; i < ndetectcages; i++)
			initialflyRemovedList.add(false);

		viewerCamData = exp.seqCamData.seq.getFirstViewer();
		displayRefViewers(exp);
		long limit = 50 * exp.cages.detectBin_Ms;
		if (limit > exp.getSeqCamSizeT())
			limit = exp.getSeqCamSizeT();
		
		for (long indexms = exp.cages.detectFirst_Ms + exp.cages.detectBin_Ms; indexms<= limit && !stopFlag; indexms += exp.cages.detectBin_Ms) {
			int t = (int) ((indexms - exp.camFirstImage_Ms)/exp.camBinImage_Ms);
			IcyBufferedImage currentImage = exp.seqCamData.getImage(t, 0);
			exp.seqCamData.currentFrame = t;
			viewerCamData.setPositionT(t);
			viewerCamData.setTitle(exp.seqCamData.getDecoratedImageName(t));

			IcyBufferedImage positiveImage = exp.seqCamData.subtractImages(currentImage, exp.seqCamData.refImage);
			seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(positiveImage, find_flies.rectangleAllCages));
			ROI2DArea roiAll = find_flies.binarizeImage(positiveImage, options.thresholdBckgnd);
			 
			for (int icage = 0; icage <= ndetectcages - 1; icage++) {
				Cage cage = exp.cages.cageList.get(icage);
				if (cage.cageNFlies <1)
					continue;
				BooleanMask2D bestMask = find_flies.findLargestBlob(roiAll, icage);
				if (bestMask != null) {
					ROI2DArea flyROI = new ROI2DArea(bestMask);
					if (!initialflyRemovedList.get(icage)) {
						Rectangle rect = flyROI.getBounds();
						patchRectToReferenceImage(exp.seqCamData, currentImage, rect);
						initialflyRemovedList.set(icage, true);
						nfliesRemoved++;
						if (exp.seqBackgroundImage != null)
							exp.seqBackgroundImage.setImage(0, 0, IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage,
									find_flies.rectangleAllCages));
						progress.setMessage("Build background image: n flies removed =" + nfliesRemoved);
					}
				}
			}
			if (nfliesRemoved == ndetectcages)
				break;
		}
		progress.close();
	}


}