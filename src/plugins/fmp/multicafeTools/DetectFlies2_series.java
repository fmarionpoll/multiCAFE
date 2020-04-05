package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.BooleanMask2D;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.kernel.roi.roi2d.ROI2DArea;



public class DetectFlies2_series extends SwingWorker<Integer, Integer> {

	private List<Boolean> initialflyRemoved = new ArrayList<Boolean>();
	private Viewer viewerCamData;
	private Viewer vPositive = null;
	private Viewer vBackgroundImage = null;
	private OverlayThreshold ov = null;

	public boolean stopFlag = false;
	public boolean threadRunning = false;
	public boolean buildBackground = true;
	public boolean detectFlies = true;

	public DetectFlies_Options detect = new DetectFlies_Options();
	public Sequence seqNegative = new Sequence();
	public Sequence seqPositive = new Sequence();
	public boolean viewInternalImages = false;

	// -----------------------------------------
	
	@Override
	protected Integer doInBackground() throws Exception {
		System.out.println("start detect flies thread (v2)");
        threadRunning = true;
		int nbiterations = 0;
		ExperimentList expList = detect.expList;
		int nbexp = expList.index1 - expList.index0 + 1;
		ProgressFrame progress = new ProgressFrame("Detect flies");
		detect.btrackWhite = true;
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag) 
				break;
			Experiment exp = expList.getExperiment(index);
			System.out.println(exp.experimentFileName);
			progress.setMessage("Processing file: " + (index-expList.index0 +1) + "//" + nbexp);

			exp.loadExperimentCamData();
			exp.xmlReadDrosoTrackDefault();
			exp.stepFrame = detect.stepFrame;
			if (detect.isFrameFixed) {
				exp.startFrame = detect.startFrame;
				exp.endFrame = detect.endFrame;
				if (exp.endFrame > (exp.seqCamData.seq.getSizeT() - 1))
					exp.endFrame = exp.seqCamData.seq.getSizeT() - 1;
			} else {
				exp.startFrame = 0;
				exp.endFrame = exp.seqCamData.seq.getSizeT() - 1;
			}
			runDetectFlies(exp);
			if (!stopFlag)
				exp.saveComputation();
			exp.seqCamData.closeSequence();
		}
		progress.close();
		threadRunning = false;
		return nbiterations;
	}
	
	@Override
	protected void done() {
		int statusMsg = 0;
		try {
			statusMsg = get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} 
//		System.out.println("iterations done: "+statusMsg);
		if (!threadRunning || stopFlag) {
			firePropertyChange("thread_ended", null, statusMsg);
		}
		else {
			firePropertyChange("thread_done", null, statusMsg);
		}
    }

	private void runDetectFlies(Experiment exp) {
		if (seqNegative == null)
			seqNegative = new Sequence();
		if (seqPositive == null)
			seqPositive = new Sequence();
		
		detect.initParametersForDetection(exp);
		detect.threshold = detect.thresholdDiff;
		exp.cleanPreviousDetections();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					viewerCamData = new Viewer(exp.seqCamData.seq, true);
					Rectangle rectv = viewerCamData.getBoundsInternal();
					rectv.setLocation(detect.parent0Rect.x+ detect.parent0Rect.width, detect.parent0Rect.y);
					viewerCamData.setBounds(rectv);
					ov = new OverlayThreshold(exp.seqCamData);
					exp.seqCamData.seq.addOverlay(ov);
					ov.setThresholdSingle(exp.cages.detect.threshold);
					ov.painterChanged();

				}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		if (buildBackground || exp.seqCamData.refImage == null) {
			if (!exp.loadReferenceImage()) {
				buildBackgroundImage(exp);
				exp.saveReferenceImage();
			}
		}
		if (detectFlies)
			findFlies(exp);
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
		if (!buildBackground && exp.seqBackgroundImage != null) {
			exp.seqBackgroundImage.close();
			exp.seqBackgroundImage = null;
		}
	}

	private void findFlies(Experiment exp) {
		ProgressChrono progressBar = new ProgressChrono("Detecting flies...");
		progressBar.initChrono(detect.endFrame - detect.startFrame + 1);

		exp.seqBackgroundImage.close();
		if (vPositive != null) {
			vPositive.close();
			vPositive = null;
		}
		if (vBackgroundImage != null) {
			vBackgroundImage.close();
			vBackgroundImage = null;
		}
		detect.initTempRectROIs(seqNegative);

		try {
			viewerCamData = exp.seqCamData.seq.getFirstViewer();
			exp.seqCamData.seq.beginUpdate();
			if (viewInternalImages)
				displayDetectViewer(exp);

			// ----------------- loop over all images of the stack
			int it = 0;
			for (int t = exp.startFrame; t <= exp.endFrame; t += exp.stepFrame, it++) {
				if (stopFlag)
					break;
				progressBar.updatePosition(t);
				IcyBufferedImage workImage = exp.seqCamData.getImage(t, 0);
				if (workImage == null)
					continue;
				IcyBufferedImage currentImage = IcyBufferedImageUtil.getCopy(workImage);
				exp.seqCamData.currentFrame = t;
				seqNegative.beginUpdate();
				IcyBufferedImage negativeImage = exp.seqCamData.subtractImages(exp.seqCamData.refImage, currentImage);
				detect.findFlies(negativeImage, t, it);
				seqNegative.setImage(0, 0, negativeImage);
				seqNegative.endUpdate();
			}
		} finally {
			exp.seqCamData.seq.endUpdate();
			seqNegative.close();
			detect.copyDetectedROIsToSequence(exp);
		}
		progressBar.close();

	}

	private void patchRectToReferenceImage(SequenceCamData seqCamData, IcyBufferedImage currentImage, Rectangle2D rect) {
		int cmax = currentImage.getSizeC();
		for (int c = 0; c < cmax; c++) {
			int[] intCurrentImage = Array1DUtil.arrayToIntArray(currentImage.getDataXY(c),
					currentImage.isSignedDataType());
			int[] intRefImage = Array1DUtil.arrayToIntArray(seqCamData.refImage.getDataXY(c),
					seqCamData.refImage.isSignedDataType());
			int xwidth = currentImage.getSizeX();
			for (int x = 0; x < rect.getWidth(); x++) {
				for (int y = 0; y < rect.getHeight(); y++) {
					int xi = (int) (rect.getX() + x);
					int yi = (int) (rect.getY() + y);
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
					exp.seqBackgroundImage.setImage(0, 0,IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage, detect.rectangleAllCages));
					
					if (seqPositive == null)
						seqPositive = new Sequence();
					if (vPositive == null)
						vPositive = new Viewer(seqPositive, false);
					seqPositive.setName("positiveImage");
					seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage, detect.rectangleAllCages));
			
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
		int nfliesRemoved = 0;
		detect.initParametersForDetection(exp);
		exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.getImage(detect.startFrame, 0));
		initialflyRemoved.clear();
		for (int i = 0; i < detect.cages.cageList.size(); i++)
			initialflyRemoved.add(false);

		viewerCamData = exp.seqCamData.seq.getFirstViewer();
		displayRefViewers(exp);

		for (int t = exp.startFrame + 1; t <= exp.endFrame && !stopFlag; t += exp.stepFrame) {
			IcyBufferedImage currentImage = exp.seqCamData.getImage(t, 0);
			exp.seqCamData.currentFrame = t;
			viewerCamData.setPositionT(t);
			viewerCamData.setTitle(exp.seqCamData.getDecoratedImageName(t));

			IcyBufferedImage positiveImage = exp.seqCamData.subtractImages(currentImage, exp.seqCamData.refImage);
			seqPositive.setImage(0, 0, IcyBufferedImageUtil.getSubImage(positiveImage, detect.rectangleAllCages));
			ROI2DArea roiAll = detect.findFly(positiveImage, detect.thresholdBckgnd);

			for (int iroi = 1; iroi < detect.cages.cageList.size() - 1; iroi++) {
				BooleanMask2D bestMask = detect.findLargestComponent(roiAll, iroi);
				if (bestMask != null) {
					ROI2DArea flyROI = new ROI2DArea(bestMask);
					if (!initialflyRemoved.get(iroi)) {
						Rectangle2D rect = flyROI.getBounds2D();
						patchRectToReferenceImage(exp.seqCamData, currentImage, rect);
						initialflyRemoved.set(iroi, true);
						nfliesRemoved++;
						if (exp.seqBackgroundImage != null)
							exp.seqBackgroundImage.setImage(0, 0, IcyBufferedImageUtil.getSubImage(exp.seqCamData.refImage,
									detect.rectangleAllCages));
						progress.setMessage("Build background image: n flies removed =" + nfliesRemoved);
					}
				}
			}
			if (nfliesRemoved == detect.cages.cageList.size())
				break;
		}
		progress.close();
	}


}