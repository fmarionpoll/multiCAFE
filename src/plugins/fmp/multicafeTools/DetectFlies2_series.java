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
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.kernel.roi.roi2d.ROI2DArea;



public class DetectFlies2_series extends SwingWorker<Integer, Integer> {

	private List<Boolean> initialflyRemoved = new ArrayList<Boolean>();
	private Viewer viewerCamData;
	private Viewer vPositive = null;
	private Viewer vReference = null;
	private OverlayThreshold ov = null;

	public boolean stopFlag = false;
	public boolean threadRunning = false;
	public boolean buildBackground = true;
	public boolean detectFlies = true;

	public DetectFlies_Options detect = new DetectFlies_Options();
	public SequenceCamData seqNegative = new SequenceCamData();
	public SequenceCamData seqPositive = new SequenceCamData();
	public SequenceCamData seqReference = new SequenceCamData();
	public boolean viewInternalImages = false;

	// -----------------------------------------
	
	@Override
	protected Integer doInBackground() throws Exception {
		threadRunning = true;
		int nbiterations = 0;
		ExperimentList expList = detect.expList;
		int nbexp = expList.index1 - expList.index0 + 1;
		ProgressChrono progressBar = new ProgressChrono("Detect flies");
		progressBar.initChrono(nbexp);
		progressBar.setMessageFirstPart("Analyze series ");
		detect.btrackWhite = true;
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag) 
				break;
			Experiment exp = expList.experimentList.get(index);
			System.out.println(exp.experimentFileName);
			progressBar.updatePosition(index - expList.index0 + 1);

			exp.loadExperimentCamData();
			exp.seqCamData.xmlReadDrosoTrackDefault();
			runDetectFlies(exp);
			if (!stopFlag)
				exp.saveComputation();
			exp.seqCamData.seq.close();
		}
		progressBar.close();
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
			seqNegative = new SequenceCamData();
		if (seqPositive == null)
			seqPositive = new SequenceCamData();
		if (seqReference == null)
			seqReference = new SequenceCamData();
		detect.seqCamData = exp.seqCamData;
		detect.initParametersForDetection();
		exp.cleanPreviousDetections();
		System.out.println("Computation over frames: " + detect.startFrame + " - " + detect.endFrame);

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					viewerCamData = new Viewer(exp.seqCamData.seq, true);
					Rectangle rectv = viewerCamData.getBoundsInternal();
					rectv.setLocation(detect.parent0Rect.x+ detect.parent0Rect.width, detect.parent0Rect.y);
					viewerCamData.setBounds(rectv);
					ov = new OverlayThreshold(exp.seqCamData);
					detect.seqCamData.seq.addOverlay(ov);
					ov.setThresholdSingle(detect.seqCamData.cages.detect.threshold);
					ov.painterChanged();

				}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}

		if (buildBackground || detect.seqCamData.refImage == null) {
			boolean flag = exp.loadReferenceImage();
			if (!flag) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							buildBackgroundImage();
						}});
				} catch (InvocationTargetException | InterruptedException e) {
					e.printStackTrace();
				}
				exp.saveReferenceImage();
			}
		}

		if (detectFlies)
			findFlies();

		if (seqNegative != null) {
			seqNegative.seq.close();
			seqNegative.seq.closed();
			seqNegative = null;
		}

		if (seqPositive != null) {
			if (vPositive != null)
				vPositive.close();
			seqPositive.seq.close();
			seqPositive.seq.closed();
			seqPositive = null;
		}

		if (!buildBackground && seqReference != null) {
			if (vReference != null)
				vReference.close();
			seqReference.seq.close();
			seqReference.seq.closed();
			seqReference = null;
		}
	}

	private void findFlies() {
		ProgressChrono progressBar = new ProgressChrono("Detecting flies...");
		progressBar.initChrono(detect.endFrame - detect.startFrame + 1);

		seqReference.seq.close();
		seqPositive.seq.close();
		if (vPositive != null) {
			vPositive.close();
			vPositive = null;
		}
		if (vReference != null) {
			vReference.close();
			vReference = null;
		}
		detect.initTempRectROIs(seqNegative.seq);

		try {
			viewerCamData = detect.seqCamData.seq.getFirstViewer();
			detect.seqCamData.seq.beginUpdate();
			if (viewInternalImages) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							displayDetectViewer();
						}});
				} catch (InvocationTargetException | InterruptedException e) {
					e.printStackTrace();
				}
			}

			// ----------------- loop over all images of the stack
			int it = 0;
			for (int t = detect.startFrame; t <= detect.endFrame; t += detect.analyzeMoveStep, it++) {
				if (stopFlag)
					break;
				progressBar.updatePositionAndTimeLeft(t);
				IcyBufferedImage workImage = detect.seqCamData.getImage(t, 0);
				if (workImage == null)
					continue;
				IcyBufferedImage currentImage = IcyBufferedImageUtil.getCopy(workImage);
				detect.seqCamData.currentFrame = t;
				seqNegative.seq.beginUpdate();
				IcyBufferedImage negativeImage = detect.seqCamData.subtractImages(detect.seqCamData.refImage,
						currentImage);

				detect.findFlies(negativeImage, t, it);
				seqNegative.seq.setImage(0, 0, negativeImage);
				seqNegative.seq.endUpdate();
			}
		} finally {
			detect.seqCamData.seq.endUpdate();
			seqNegative.seq.close();
			detect.copyDetectedROIsToSequence(detect.seqCamData.seq);
		}
		progressBar.close();

	}

	private void patchRectToReferenceImage(IcyBufferedImage currentImage, Rectangle2D rect) {
		int cmax = currentImage.getSizeC();
		for (int c = 0; c < cmax; c++) {
			int[] intCurrentImage = Array1DUtil.arrayToIntArray(currentImage.getDataXY(c),
					currentImage.isSignedDataType());
			int[] intRefImage = Array1DUtil.arrayToIntArray(detect.seqCamData.refImage.getDataXY(c),
					detect.seqCamData.refImage.isSignedDataType());
			int xwidth = currentImage.getSizeX();
			for (int x = 0; x < rect.getWidth(); x++) {
				for (int y = 0; y < rect.getHeight(); y++) {
					int xi = (int) (rect.getX() + x);
					int yi = (int) (rect.getY() + y);
					int coord = xi + yi * xwidth;
					intRefImage[coord] = intCurrentImage[coord];
				}
			}
			Object destArray = detect.seqCamData.refImage.getDataXY(c);
			Array1DUtil.intArrayToSafeArray(intRefImage, destArray, detect.seqCamData.refImage.isSignedDataType(),
					detect.seqCamData.refImage.isSignedDataType());
			detect.seqCamData.refImage.setDataXY(c, destArray);
		}
		detect.seqCamData.refImage.dataChanged();
	}

	private void displayDetectViewer() {
		Viewer vNegative = new Viewer(seqNegative.seq, false);
		seqNegative.seq.setName("detectionImage");
		seqNegative.seq.setImage(0, 0, detect.seqCamData.refImage);
		Point pt = viewerCamData.getLocation();
		if (vNegative != null) {
			vNegative.setLocation(pt);
			vNegative.setVisible(true);
		}
	}

	public void displayRefViewers() {
		if (seqPositive == null)
			seqPositive = new SequenceCamData();
		if (seqReference == null)
			seqReference = new SequenceCamData();

		if (vPositive == null)
			vPositive = new Viewer(seqPositive.seq, false);
		seqPositive.seq.setName("positiveImage");

		if (vReference == null)
			vReference = new Viewer(seqReference.seq, false);
		seqReference.seq.setName("referenceImage");

		seqReference.seq.setImage(0, 0,IcyBufferedImageUtil.getSubImage(detect.seqCamData.refImage, detect.rectangleAllCages));
		seqPositive.seq.setImage(0, 0, IcyBufferedImageUtil.getSubImage(detect.seqCamData.refImage, detect.rectangleAllCages));

		viewerCamData = detect.seqCamData.seq.getFirstViewer();
		Point pt = viewerCamData.getLocation();
		int height = viewerCamData.getHeight();
		pt.y += height;

		if (vPositive != null) {
			vPositive.setVisible(true);
			vPositive.setLocation(pt);
		}
		if (vReference != null) {
			vReference.setVisible(true);
			vReference.setLocation(pt);
		}
	}

	private void buildBackgroundImage() {
		ProgressFrame progress = new ProgressFrame("Build background image...");
		int nfliesRemoved = 0;
		detect.seqCamData.refImage = IcyBufferedImageUtil.getCopy(detect.seqCamData.getImage(detect.startFrame, 0));
		detect.initParametersForDetection();
		initialflyRemoved.clear();
		for (int i = 0; i < detect.cages.cageList.size(); i++)
			initialflyRemoved.add(false);

		viewerCamData = detect.seqCamData.seq.getFirstViewer();
		displayRefViewers();

		for (int t = detect.startFrame + 1; t <= detect.endFrame && !stopFlag; t += detect.analyzeMoveStep) {
			IcyBufferedImage currentImage = detect.seqCamData.getImage(t, 0);
			detect.seqCamData.currentFrame = t;
			viewerCamData.setPositionT(t);
			viewerCamData.setTitle(detect.seqCamData.getDecoratedImageName(t));

			IcyBufferedImage positiveImage = detect.seqCamData.subtractImages(currentImage, detect.seqCamData.refImage);
			seqPositive.seq.setImage(0, 0, IcyBufferedImageUtil.getSubImage(positiveImage, detect.rectangleAllCages));
			ROI2DArea roiAll = detect.findFly(positiveImage);

			for (int iroi = 1; iroi < detect.cages.cageList.size() - 1; iroi++) {
				BooleanMask2D bestMask = detect.findLargestComponent(roiAll, iroi);
				if (bestMask != null) {
					ROI2DArea flyROI = new ROI2DArea(bestMask);
					if (!initialflyRemoved.get(iroi)) {
						Rectangle2D rect = flyROI.getBounds2D();
						patchRectToReferenceImage(currentImage, rect);
						initialflyRemoved.set(iroi, true);
						nfliesRemoved++;
						if (seqReference != null)
							seqReference.seq.setImage(0, 0, IcyBufferedImageUtil.getSubImage(detect.seqCamData.refImage,
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