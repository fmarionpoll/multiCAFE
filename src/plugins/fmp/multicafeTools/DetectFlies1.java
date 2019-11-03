package plugins.fmp.multicafeTools;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Cages;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.XYTaSeries;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class DetectFlies1 implements Runnable {

	private List<BooleanMask2D> cageMaskList 	= new ArrayList<BooleanMask2D>();
	private int					analyzeStep ;
	private int 				startFrame;
	private int 				endFrame;
	private int 				nbframes;
	private Viewer 				viewer;
	public Rectangle			rectangleAllCages = null;
		
	public SequenceCamData 		seqCamData 		= null;	
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public boolean				buildBackground	= true;
	public boolean				detectFlies		= true;
	
	public DetectFlies_Options 	detect 			= new DetectFlies_Options();
	public Cages 				cages 			= new Cages();
	public SequenceCamData 		seqNegative 	= null;
	public SequenceCamData 		seqPositive 	= null;
	public SequenceCamData		seqReference	= null;
	public boolean				viewInternalImages = false;

	
	@Override
	public void run() {
		threadRunning = true;
		
		// create arrays for storing position and init their value to zero
		initParametersForDetection();
		System.out.println("Computation over frames: " + startFrame + " - " + endFrame );
		ProgressChrono progressBar = new ProgressChrono("Detecting flies...");
		progressBar.initStuff(endFrame-startFrame+1);
		
		int nbcages = cages.cageList.size();
		ROI2DRectangle [] tempRectROI = new ROI2DRectangle [nbcages];
		//int minCapacity = (endFrame - startFrame + 1) / analyzeStep;		
		for (int i=0; i < nbcages; i++) {
			tempRectROI[i] = new ROI2DRectangle(0, 0, 10, 10);
			tempRectROI[i].setName("fly_"+i);
			seqCamData.seq.addROI(tempRectROI[i]);
			Cage cage = cages.cageList.get(i);
			XYTaSeries positions = new XYTaSeries(cage.cageLimitROI);
			cage.flyPositions = positions;
		}

		// create array for the results - 1 point = 1 slice
		ROI [][] resultFlyPositionArrayList = new ROI[nbframes][nbcages];
		int lastFrameAnalyzed = endFrame;

		try {
			viewer = seqCamData.seq.getFirstViewer();	
			seqCamData.seq.beginUpdate();

		
			// ----------------- loop over all images of the stack
			int it = 0;
			for (int t = startFrame ; t <= endFrame && !stopFlag; t  += analyzeStep, it++ ) {				
				progressBar.updatePositionAndTimeLeft(t);

				// load next image and compute threshold
				IcyBufferedImage workImage = seqCamData.getImageAndSubtractReference(t, detect.transformop1); 
				seqCamData.currentFrame = t;
				viewer.setPositionT(t);
				viewer.setTitle(seqCamData.getDecoratedImageName(t));
				if (workImage == null) {
					// try another time
					System.out.println("Error reading image: " + t + " ... trying again"  );
					seqCamData.seq.removeImage(t, 0);
					workImage = seqCamData.getImageAndSubtractReference(t, detect.transformop1); 
					if (workImage == null) {
						System.out.println("Fatal error occurred while reading image: " + t + " : Procedure stopped"  );
						return;
					}
				}
				ROI2DArea roiAll = findFly (workImage, detect);

				// ------------------------ loop over all the cages of the stack
				for ( int iroi = 0; iroi < cages.cageList.size(); iroi++ ) {		
					BooleanMask2D bestMask = findLargestComponent(roiAll, iroi);
					ROI2DArea flyROI = null;
					if ( bestMask != null ) {
						flyROI = new ROI2DArea( bestMask );
						flyROI.setName("det"+iroi +" " + t );
						flyROI.setT( t );
						resultFlyPositionArrayList[it][iroi] = flyROI;
						
						// tempROI
						Rectangle2D rect = flyROI.getBounds2D();
						tempRectROI[iroi].setRectangle(rect);
						
						// compute center and distance (square of)
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
					
			}
		

		} finally {
			progressBar.close();
			seqCamData.seq.endUpdate();
			for (int i=0; i < nbcages; i++)
				seqCamData.seq.removeROI(tempRectROI[i]);
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
		}
		System.out.println("Computation finished.");
		threadRunning = false;
	}
	
	private void initParametersForDetection() {
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

	private ROI2DArea findFly(IcyBufferedImage img, DetectFlies_Options detect ) {
		int threshold = detect.threshold;
		int chan = detect.videoChannel;
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