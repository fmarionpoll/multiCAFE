package plugins.fmp.multicafe.series;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.multicafe.sequence.Cage;
import plugins.fmp.multicafe.sequence.Cages;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.XYTaSeries;
import plugins.fmp.multicafe.tools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DRectangle;


public class DetectFlies_Find {
	
	public List<BooleanMask2D> cageMaskList = new ArrayList<BooleanMask2D>();
	public Rectangle 	rectangleAllCages 	= null;
	public BuildSeries_Options	options		= null;
	
	private Cages 		cages 				= null;
	ROI2DRectangle [] 	tempRectROI;
	ROI [][] 			resultFlyPositionArrayList;
	
	// -----------------------------------------------------
	

	public BooleanMask2D findLargestBlob(ROI2DArea roiAll, int iroi) {
		ROI cageLimitROI = cages.cageList.get(iroi).roi;
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
			if (options.blimitLow && len < options.limitLow)
				len = 0;
			if (options.blimitUp && len > options.limitUp)
				len = 0;
			// trap condition where a line is found
			int width = mask.bounds.width;
			int height = mask.bounds.height;
			int ratio = width / height;
			if (width < height)
				ratio = height/width;
			if (ratio > 4)
				len = 0;
			
			if ( len > max ) {
				bestMask = mask;
				max = len;
			}
		}
		return bestMask;
	}
	
	public ROI2DArea binarizeImage(IcyBufferedImage img, int threshold) {
		if (img == null)
			return null;
		boolean[] mask = new boolean[ img.getSizeX() * img.getSizeY() ];
		if (options.btrackWhite) {
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
			byte[] arrayChan = img.getDataXYAsByte( options.videoChannel);
			for ( int i = 0 ; i < arrayChan.length ; i++ ) {
				mask[i] = ( ((int) arrayChan[i] ) & 0xFF ) < threshold ;
			}
		}
		BooleanMask2D bmask = new BooleanMask2D( img.getBounds(), mask); 
		return new ROI2DArea( bmask );
	}
	
	public void findFlies (IcyBufferedImage workimage, int t, int it) {
		ROI2DArea binarizedImageRoi = binarizeImage (workimage, options.threshold);
		for ( int icage = 0; icage < cages.cageList.size(); icage++ ) {		
			BooleanMask2D bestMask = findLargestBlob(binarizedImageRoi, icage);
			ROI2DArea flyROI = null;
			Cage cage = cages.cageList.get(icage);
			if (cage.cageNFlies < 1)
				continue;
			if ( bestMask != null ) {
				flyROI = new ROI2DArea( bestMask ); 
				Rectangle2D rect = flyROI.getBounds2D();
				ROI2DRectangle flyRect = new ROI2DRectangle(rect.getX(), rect.getY(), rect.getX()+rect.getWidth(), rect.getY()+rect.getHeight());
				flyRect.setName("det"+cage.getCageNumber() +"_" + t );
				flyRect.setT( t );
				resultFlyPositionArrayList[icage][it] = flyRect;
				
				tempRectROI[icage].setRectangle(rect);
				Point2D flyPosition = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
				//Point2D flyPosition = ROIUtil.getMassCenter( flyROI); //ROIMassCenterXDescriptor
				int npoints = cage.flyPositions.pointsList.size();
				cage.flyPositions.add(flyPosition, t);
				if (it > 0 && npoints > 0) {
					Point2D prevPoint = cage.flyPositions.getValidPointAtOrBefore(npoints);
					if (prevPoint.getX() >= 0) {
						double distance = flyPosition.distance(prevPoint);
						if (distance > options.jitter)
							cage.flyPositions.lastTimeAlive = t;
					}
				}
			}
			else {
				Point2D flyPosition = new Point2D.Double(-1, -1);
				cage.flyPositions.add(flyPosition, t);
			}
		}
	}
	
	public void initTempRectROIs(Experiment exp, Sequence seq) {
		cages = exp.cages;
		int nbcages = cages.cageList.size();
		tempRectROI = new ROI2DRectangle [nbcages];
		for (int i=0; i < nbcages; i++) {
			tempRectROI[i] = new ROI2DRectangle(0, 0, 10, 10);
			tempRectROI[i].setName("fly_"+i);
			Cage cage = cages.cageList.get(i);
			XYTaSeries positions = new XYTaSeries();
			cage.flyPositions = positions;
			seq.addROI(tempRectROI[i]);	
		}
		// create array for the results - 1 point = 1 slice
		resultFlyPositionArrayList = new ROI[nbcages][options.nbframes];
	}
	
	public void initParametersForDetection(Experiment exp, BuildSeries_Options	options) {
		this.options = options;
		options.nbframes = (exp.getCagesFrameEnd() - exp.getCagesFrameStart() +1)/options.df_stepFrame +1;
		exp.cages.clearAllMeasures();
		cages = exp.cages;
		cageMaskList = ROI2DUtilities.getMask2DFromROIs(cages.cageList);
		rectangleAllCages = null;
		for (Cage cage: cages.cageList) {
			Rectangle rect = cage.roi.getBounds();
			if (rectangleAllCages == null)
				rectangleAllCages = new Rectangle(rect);
			else
				rectangleAllCages.add(rect);
		}
	}
	
	public void removeTempRectROIs(Experiment exp) {
		for (int i=0; i < tempRectROI.length; i++)
			exp.seqCamData.seq.removeROI(tempRectROI[i]);
	}
	
	public void copyDetectedROIsToSequence(Experiment exp) {
		try {
			int ncages = cages.cageList.size();
			for (int icage=0; icage < ncages; icage++) {
				for ( int it = 0; it < resultFlyPositionArrayList[icage].length ; it++ ) {
					exp.seqCamData.seq.addROI( resultFlyPositionArrayList[icage][it] );
				}
			}
		}
		finally {
			exp.seqCamData.seq.endUpdate();
		}
	}
	
	public void copyDetectedROIsToCages(Experiment exp) {
		int ncages = cages.cageList.size();
		for (int icage=0; icage < ncages; icage++) {
			Cage cage = cages.cageList.get(icage);
			for ( int it = 0; it < resultFlyPositionArrayList[icage].length ; it++ ) {
				cage.detectedFliesList.add((ROI2D) resultFlyPositionArrayList[icage][it] );
			}
		}
	}
	
}
