package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.BooleanMask2D;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.kernel.roi.roi2d.ROI2DArea;


public class DetectFlies2  implements Runnable {
	
	private List<Boolean>		initialflyRemoved = new ArrayList<Boolean> ();
	private Viewer 				viewerCamData;
	private Viewer 				vPositive = null;
	private Viewer 				vReference = null;
		
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	
	public DetectFlies_Options 	detect 			= new DetectFlies_Options();
	public SequenceCamData 		seqNegative 	= new SequenceCamData();
	public SequenceCamData 		seqPositive 	= new SequenceCamData();
	public SequenceCamData		seqReference	= new SequenceCamData();
	public boolean				viewInternalImages = false;
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * parameters:
	 * 		vSequence
	 * 		detect
	 *
	 * change stopFlag to stop process
	 */
	
	@Override
	public void run() {
		threadRunning = true;

		// create arrays for storing position and init their value to zero
		if (seqNegative == null) seqNegative 	= new SequenceCamData();
		if (seqPositive == null) seqPositive 	= new SequenceCamData();
		if (seqReference == null) seqReference	= new SequenceCamData();
		detect.initParametersForDetection();
		System.out.println("Computation over frames: " + detect.startFrame + " - " + detect.endFrame );

		if (buildBackground || detect.seqCamData.refImage == null)
			buildBackgroundImage();

		if (detectFlies)
			detectFlies();
			
		threadRunning = false;

		System.out.println("Computation finished.");
		if (seqNegative  != null) {
			seqNegative.seq.close();
			seqNegative.seq.closed();
			seqNegative = null;
		}
		
		if (seqPositive != null ) {
			if (vPositive != null) vPositive.close();
			seqPositive.seq.close();
			seqPositive.seq.closed();
			seqPositive = null;
		}
		
		if (!buildBackground && seqReference != null ) {
			if (vReference != null) vReference.close();
			seqReference.seq.close();
			seqReference.seq.closed();
			seqReference = null;
		}
	}

	private void detectFlies () {
		ProgressChrono progressBar = new ProgressChrono("Detecting flies...");
		progressBar.initChrono(detect.endFrame-detect.startFrame+1);
		
		detect.btrackWhite = true;
		seqReference.seq.close();
		seqPositive.seq.close();
		if (vPositive != null) { vPositive.close(); vPositive = null;}
		if (vReference != null) { vReference.close(); vReference = null;}	
		detect.initTempRectROIs(seqNegative.seq);
		
		try {
			viewerCamData = detect.seqCamData.seq.getFirstViewer();	
			detect.seqCamData.seq.beginUpdate();
						
			if (viewInternalImages) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						displayDetectViewer();
					}
				});
			}
			
			// ----------------- loop over all images of the stack
			int it = 0;
			for (int t = detect.startFrame; t <= detect.endFrame && !stopFlag; t  += detect.analyzeStep, it++ ) {				
				progressBar.updatePositionAndTimeLeft(t);

				// load next image and compute threshold
				IcyBufferedImage workImage = detect.seqCamData.getImage(t, 0);
				if (workImage == null)
					continue;
				IcyBufferedImage currentImage = IcyBufferedImageUtil.getCopy(workImage);
				detect.seqCamData.currentFrame = t;
				seqNegative.seq.beginUpdate();
				IcyBufferedImage negativeImage = detect.seqCamData.subtractImages (detect.seqCamData.refImage, currentImage);
				
				detect.findFlies (negativeImage, t, it);
				seqNegative.seq.setImage(0,  0, negativeImage); 
				seqNegative.seq.endUpdate();
			}
		} finally {
			progressBar.close();
			detect.seqCamData.seq.endUpdate();
			seqNegative.seq.close();
		}

		//	 copy created ROIs to inputSequence
		detect.copyDetectedROIsToSequence(detect.seqCamData.seq);
	}
	
	private void patchRectToReferenceImage(IcyBufferedImage currentImage, Rectangle2D rect) {
		int cmax = currentImage.getSizeC();
		for (int c=0; c< cmax; c++) {
			int[] intCurrentImage = Array1DUtil.arrayToIntArray(currentImage.getDataXY(c), currentImage.isSignedDataType());
			int[] intRefImage = Array1DUtil.arrayToIntArray(detect.seqCamData.refImage.getDataXY(c), detect.seqCamData.refImage.isSignedDataType());
			int xwidth = currentImage.getSizeX();
			for (int x = 0; x < rect.getWidth(); x++) {
				for (int y=0; y< rect.getHeight(); y++) {
					int xi = (int) (rect.getX() + x);
					int yi = (int) (rect.getY() + y);
					int coord = xi + yi*xwidth;
					intRefImage[coord] = intCurrentImage[coord];
				}
			}
			Object destArray = detect.seqCamData.refImage.getDataXY(c);
			Array1DUtil.intArrayToSafeArray(intRefImage, destArray, detect.seqCamData.refImage.isSignedDataType(), detect.seqCamData.refImage.isSignedDataType());
			detect.seqCamData.refImage.setDataXY(c, destArray);
		}
		detect.seqCamData.refImage.dataChanged();
	}
	
	private void displayDetectViewer () {
		Viewer vNegative = new Viewer(seqNegative.seq, false);
		seqNegative.seq.setName("detectionImage");
		seqNegative.seq.setImage(0,  0, detect.seqCamData.refImage);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Point pt = viewerCamData.getLocation();
		if (vNegative != null) {
			vNegative.setLocation(pt);
			vNegative.setVisible(true);
		}
	}

	public void displayRefViewers () {

		if (seqPositive == null) seqPositive 	= new SequenceCamData();
		if (seqReference == null) seqReference	= new SequenceCamData();
		
		if (vPositive == null)
			vPositive = new Viewer(seqPositive.seq, false);
		seqPositive.seq.setName("positiveImage");
		
		if (vReference == null)
			vReference = new Viewer(seqReference.seq, false);
		seqReference.seq.setName("referenceImage");

		seqReference.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(detect.seqCamData.refImage, detect.rectangleAllCages));
		seqPositive.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(detect.seqCamData.refImage, detect.rectangleAllCages));
		
		viewerCamData = detect.seqCamData.seq.getFirstViewer();
		Point pt = viewerCamData.getLocation();
		int height = viewerCamData.getHeight();
		pt.y += height;
		
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
				
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
		for (int i=0; i < detect.cages.cageList.size(); i++)
			initialflyRemoved.add(false);
		
		viewerCamData = detect.seqCamData.seq.getFirstViewer();
//		if (viewInternalImages) {	
//			SwingUtilities.invokeLater(new Runnable() {
//				public void run() {
					displayRefViewers();
//				}
//			});
//		}
		
		for (int t = detect.startFrame +1 ; t <= detect.endFrame && !stopFlag; t  += detect.analyzeStep ) {				
			IcyBufferedImage currentImage = detect.seqCamData.getImage(t, 0);
			detect.seqCamData.currentFrame = t;
			viewerCamData.setPositionT(t);
			viewerCamData.setTitle(detect.seqCamData.getDecoratedImageName(t));
			
			IcyBufferedImage positiveImage = detect.seqCamData.subtractImages (currentImage, detect.seqCamData.refImage);
			seqPositive.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(positiveImage, detect.rectangleAllCages));
			ROI2DArea roiAll = detect.findFly (positiveImage );

			for ( int iroi = 1; iroi < detect.cages.cageList.size()-1; iroi++ ) {
				BooleanMask2D bestMask = detect.findLargestComponent(roiAll, iroi);		
				if ( bestMask != null ) {
					ROI2DArea flyROI = new ROI2DArea( bestMask );
					if (!initialflyRemoved.get(iroi)) {
						Rectangle2D rect = flyROI.getBounds2D();
						patchRectToReferenceImage(currentImage, rect);
						initialflyRemoved.set(iroi, true);
						nfliesRemoved ++;
						if (seqReference != null)
							seqReference.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(detect.seqCamData.refImage, detect.rectangleAllCages));
						progress.setMessage( "Build background image: n flies removed =" + nfliesRemoved);
					}
				}
			}
			if (nfliesRemoved == detect.cages.cageList.size())
				break;
		}
		progress.close();
	}


}