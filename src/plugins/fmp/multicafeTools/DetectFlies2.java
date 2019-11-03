package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Cages;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.XYTaSeries;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class DetectFlies2  implements Runnable {
	
	private List<BooleanMask2D> cageMaskList 	= new ArrayList<BooleanMask2D>();
	private List<Boolean>		initialflyRemoved = new ArrayList<Boolean> ();
	private int					analyzeStep ;
	private int 				startFrame;
	private int 				endFrame;
	private int 				nbframes;
	private Viewer 				viewerCamData;
	private Viewer 				vPositive = null;
	private Viewer 				vReference = null;
	public Rectangle			rectangleAllCages = null;
		
	public SequenceCamData 		seqCamData 		= null;	
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	
	public DetectFlies_Options 	detect 			= new DetectFlies_Options();
	public Cages 				cages 			= new Cages();
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
		initParametersForDetection();
		System.out.println("Computation over frames: " + startFrame + " - " + endFrame );

		if (buildBackground || seqCamData.refImage == null)
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
		progressBar.initStuff(endFrame-startFrame+1);
		
		int nbcages = cages.cageList.size();
		ROI2DRectangle [] tempRectROI = new ROI2DRectangle [nbcages];
		detect.btrackWhite = true;
		seqReference.seq.close();
		seqPositive.seq.close();
		if (vPositive != null) { vPositive.close(); vPositive = null;}
		if (vReference != null) { vReference.close(); vReference = null;}	
		
		for (int i=0; i < nbcages; i++) {
			tempRectROI[i] = new ROI2DRectangle(0, 0, 0, 0);
			tempRectROI[i].setName("fly_"+i);
			seqNegative.seq.addROI(tempRectROI[i]);
			Cage cage = cages.cageList.get(i);
			XYTaSeries positions = new XYTaSeries(cage.cageLimitROI);
			cage.flyPositions = positions;
		}

		// create array for the results - 1 point = 1 slice
		ROI [][] resultFlyPositionArrayList = new ROI[nbframes][nbcages];
		int lastFrameAnalyzed = endFrame;
		
		try {
			viewerCamData = seqCamData.seq.getFirstViewer();	
			seqCamData.seq.beginUpdate();
						
			if (viewInternalImages) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						displayDetectViewer();
					}
				});
			}
			
			// ----------------- loop over all images of the stack
			int it = 0;
			for (int t = startFrame; t <= endFrame && !stopFlag; t  += analyzeStep, it++ ) {				
				progressBar.updatePositionAndTimeLeft(t);

				// load next image and compute threshold
				IcyBufferedImage img = seqCamData.getImage(t, 0);
				if (img == null)
					continue;
				IcyBufferedImage currentImage = IcyBufferedImageUtil.getCopy(img);
				seqCamData.currentFrame = t;
				seqNegative.seq.beginUpdate();
				IcyBufferedImage negativeImage = seqCamData.subtractImages (seqCamData.refImage, currentImage);
				//seqNegative.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(negativeImage, rectangleAllCages));
				
				ROI2DArea roiAll = findFly (negativeImage, detect);
				
				// ------------------------ loop over all the cages of the stack
				for ( int iroi = 0; iroi < cages.cageList.size(); iroi++ ) {		
					BooleanMask2D bestMask = findLargestComponent(roiAll, iroi);
					ROI2DArea flyROI = null;
					if ( bestMask != null ) {
						flyROI = new ROI2DArea( bestMask );
						flyROI.setName("det"+iroi +" " + t );
						flyROI.setT( t );
						resultFlyPositionArrayList[it][iroi] = flyROI;
			
						Rectangle2D rect = flyROI.getBounds2D();
						tempRectROI[iroi].setRectangle(rect);
						
						Cage cage = cages.cageList.get(iroi);
						Point2D flyPosition = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
						int npoints = cage.flyPositions.pointsList.size();
						cage.flyPositions.add(flyPosition, t);
						if (it > 0 && npoints > 0) {
							double distance = flyPosition.distance(cage.flyPositions.getPoint(npoints-1));
							if (distance > detect.jitter)
								cage.flyPositions.lastTimeAlive = t;
						}
					}
				}
				seqNegative.seq.setImage(0,  0, negativeImage); 
				seqNegative.seq.endUpdate();
			}
		} finally {
			progressBar.close();
			seqCamData.seq.endUpdate();
			seqNegative.seq.close();
		}

		//	 copy created ROIs to inputSequence
		System.out.println("Copy results to input sequence");
		try {
			seqCamData.seq.beginUpdate();
			seqCamData.cages = cages;
			int nrois = cages.cageList.size();
			int it = 0;
			for ( int t = startFrame ; t <= lastFrameAnalyzed ; t  += analyzeStep, it++ )
				for (int iroi=0; iroi < nrois; iroi++) 
					seqCamData.seq.addROI( resultFlyPositionArrayList[it][iroi] );
		}
		finally
		{
			seqCamData.seq.endUpdate();
			seqReference.seq.close();
		}
	}

	private ROI2DArea findFly(IcyBufferedImage img, DetectFlies_Options detect ) {
		int threshold = detect.threshold;
		int chan = detect.ichanselected;
		boolean white = detect.btrackWhite;
		if (img == null)
			return null;

		boolean[] mask = new boolean[ img.getSizeX() * img.getSizeY() ];

		if (white) {
			byte[] arrayRed 	= img.getDataXYAsByte( 0);
			byte[] arrayGreen 	= img.getDataXYAsByte( 1);
			byte[] arrayBlue 	= img.getDataXYAsByte( 2);

			for ( int i = 0 ; i < arrayRed.length ; i++ ) {
				float r = ( arrayRed[i] 	& 0xFF );
				float g = ( arrayGreen[i] 	& 0xFF );
				float b = ( arrayBlue[i] 	& 0xFF );
				float intensity = (r+g+b)/3f;

				mask[i] = ( intensity ) > threshold ;
			}
		}
		else {

			byte[] arrayChan = img.getDataXYAsByte( chan);
			for ( int i = 0 ; i < arrayChan.length ; i++ ) {
				mask[i] = ( ((int) arrayChan[i] ) & 0xFF ) < threshold ;
			}
		}
		BooleanMask2D bmask = new BooleanMask2D( img.getBounds(), mask); 
		ROI2DArea roiResult = new ROI2DArea( bmask );
		return roiResult;
	}
	
	private void patchRectToReferenceImage(IcyBufferedImage currentImage, Rectangle2D rect) {
		int cmax = currentImage.getSizeC();
		for (int c=0; c< cmax; c++) {
			int[] intCurrentImage = Array1DUtil.arrayToIntArray(currentImage.getDataXY(c), currentImage.isSignedDataType());
			int[] intRefImage = Array1DUtil.arrayToIntArray(seqCamData.refImage.getDataXY(c), seqCamData.refImage.isSignedDataType());
			int xwidth = currentImage.getSizeX();
			for (int x = 0; x < rect.getWidth(); x++) {
				for (int y=0; y< rect.getHeight(); y++) {
					int xi = (int) (rect.getX() + x);
					int yi = (int) (rect.getY() + y);
					int coord = xi + yi*xwidth;
					intRefImage[coord] = intCurrentImage[coord];
				}
			}
			Object destArray = seqCamData.refImage.getDataXY(c);
			Array1DUtil.intArrayToSafeArray(intRefImage, destArray, seqCamData.refImage.isSignedDataType(), seqCamData.refImage.isSignedDataType());
			seqCamData.refImage.setDataXY(c, destArray);
		}
		seqCamData.refImage.dataChanged();
	}
	
	private void displayDetectViewer () {
		Viewer vNegative = new Viewer(seqNegative.seq, false);
		seqNegative.seq.setName("detectionImage");
		seqNegative.seq.setImage(0,  0, seqCamData.refImage);
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

		seqReference.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(seqCamData.refImage, rectangleAllCages));
		seqPositive.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(seqCamData.refImage, rectangleAllCages));
		
		viewerCamData = seqCamData.seq.getFirstViewer();
		Point pt = viewerCamData.getLocation();
		int height = viewerCamData.getHeight();
		pt.y += height;
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
				
		if (vPositive != null) {
			vPositive.setVisible(true);
			vPositive.setLocation(pt);
		}
		if (vReference != null) {
			vReference.setVisible(true);
			vReference.setLocation(pt);
		}
	}
	
	public void initParametersForDetection() {
		analyzeStep = seqCamData.analysisStep;
		startFrame 	= (int) seqCamData.analysisStart;
		endFrame 	= (int) seqCamData.analysisEnd;
		if ( seqCamData.seq.getSizeT() < endFrame+1 )
			endFrame = (int) seqCamData.nTotalFrames - 1;
		nbframes = (endFrame - startFrame +1)/analyzeStep +1;
		
		cages.clear();
		cages.cageList = ROI2DUtilities.getCagesFromSequence(seqCamData);
		cageMaskList = ROI2DUtilities.getMask2DFromROIs(cages.cageList);
		rectangleAllCages = null;
		for (Cage cage: cages.cageList) {
			Rectangle rect = cage.cageLimitROI.getBounds();
			if (rectangleAllCages == null)
				rectangleAllCages = new Rectangle(rect);
			else
				rectangleAllCages.add(rect);
		}
	}
	
	private void buildBackgroundImage() {
		ProgressFrame progress = new ProgressFrame("Build background image...");
		int nfliesRemoved = 0;
		seqCamData.refImage = IcyBufferedImageUtil.getCopy(seqCamData.getImage(startFrame, 0));
		initParametersForDetection();
		initialflyRemoved.clear();
		for (int i=0; i < cages.cageList.size(); i++)
			initialflyRemoved.add(false);
		
		viewerCamData = seqCamData.seq.getFirstViewer();
		if (viewInternalImages) {	
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					displayRefViewers();
				}
			});
		}
		
		for (int t = startFrame +1 ; t <= endFrame && !stopFlag; t  += analyzeStep ) {				
			IcyBufferedImage currentImage = seqCamData.getImage(t, 0);
			seqCamData.currentFrame = t;
			viewerCamData.setPositionT(t);
			viewerCamData.setTitle(seqCamData.getDecoratedImageName(t));
			
			IcyBufferedImage positiveImage = seqCamData.subtractImages (currentImage, seqCamData.refImage);
			seqPositive.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(positiveImage, rectangleAllCages));
			ROI2DArea roiAll = findFly (positiveImage, detect );

			for ( int iroi = 1; iroi < cages.cageList.size()-1; iroi++ ) {
				BooleanMask2D bestMask = findLargestComponent(roiAll, iroi);		
				if ( bestMask != null ) {
					ROI2DArea flyROI = new ROI2DArea( bestMask );
					if (!initialflyRemoved.get(iroi)) {
						Rectangle2D rect = flyROI.getBounds2D();
						patchRectToReferenceImage(currentImage, rect);
						initialflyRemoved.set(iroi, true);
						nfliesRemoved ++;
						if (seqReference != null)
							seqReference.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(seqCamData.refImage, rectangleAllCages));
						progress.setMessage( "Build background image: n flies removed =" + nfliesRemoved);
					}
				}
			}
			if (nfliesRemoved == cages.cageList.size())
				break;
		}
		progress.close();
	}

	private BooleanMask2D findLargestComponent(ROI2DArea roiAll, int iroi) {
		
		ROI cageLimitROI = cages.cageList.get(iroi).cageLimitROI;
		if ( cageLimitROI == null )
			return null;
		
		BooleanMask2D cageMask = cageMaskList.get(iroi);
		if (cageMask == null)
			return null;
		
		ROI2DArea roi = new ROI2DArea(roiAll.getBooleanMask( true ).getIntersection( cageMask ) );

		// find largest component in the threshold
		int max = 0;
		BooleanMask2D bestMask = null;
		for ( BooleanMask2D mask : roi.getBooleanMask( true ).getComponents() ) {
			int len = mask.getPoints().length;
			if (detect.blimitLow && len < detect.limitLow)
				len = 0;
			if (detect.blimitUp && len > detect.limitUp)
				len = 0;
				
			if ( len > max )
			{
				bestMask = mask;
				max = len;
			}
		}
		return bestMask;
	}
}