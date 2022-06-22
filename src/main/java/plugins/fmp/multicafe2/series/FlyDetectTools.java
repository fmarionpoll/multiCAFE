package plugins.fmp.multicafe2.series;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;

import icy.sequence.Sequence;
import plugins.fmp.multicafe2.experiment.Cage;
import plugins.fmp.multicafe2.experiment.Cages;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.XYTaSeriesArrayList;
import plugins.kernel.roi.roi2d.ROI2DArea;




public class FlyDetectTools 
{	
	public List<BooleanMask2D> 	cageMaskList 		= new ArrayList<BooleanMask2D>();
	public Rectangle 			rectangleAllCages 	= null;
	public BuildSeriesOptions	options				= null;
	private Cages 				cages 				= null;
	
	// -----------------------------------------------------
	
	public BooleanMask2D findLargestBlob(ROI2DArea roiAll, Cage cage) throws InterruptedException 
	{
		ROI cageLimitROI = cage.cageRoi;
		if ( cageLimitROI == null )
			return null;
		BooleanMask2D cageMask = cage.cageMask;
		if (cageMask == null)
			return null;
		ROI2DArea roi = new ROI2DArea(roiAll.getBooleanMask( true ).getIntersection( cageMask ) );

		// find largest component in the threshold
		int max = 0;
		BooleanMask2D bestMask = null;
		BooleanMask2D roiBooleanMask = roi.getBooleanMask( true );
		for ( BooleanMask2D mask : roiBooleanMask.getComponents() ) 
		{
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
			if ( len > max ) 
			{
				bestMask = mask;
				max = len;
			}
		}

		
		return bestMask;
	}
	
	public ROI2DArea binarizeImage(IcyBufferedImage img, int threshold) 
	{
		if (img == null)
			return null;
		boolean[] mask = new boolean[ img.getSizeX() * img.getSizeY() ];
		if (options.btrackWhite) 
		{
			byte[] arrayRed 	= img.getDataXYAsByte( 0);
			byte[] arrayGreen 	= img.getDataXYAsByte( 1);
			byte[] arrayBlue 	= img.getDataXYAsByte( 2);
			for ( int i = 0 ; i < arrayRed.length ; i++ ) 
			{
				float r = ( arrayRed[i] 	& 0xFF );
				float g = ( arrayGreen[i] 	& 0xFF );
				float b = ( arrayBlue[i] 	& 0xFF );
				float intensity = (r+g+b)/3f;
				mask[i] = ( intensity ) > threshold ;
			}
		}
		else 
		{
			byte[] arrayChan = img.getDataXYAsByte( options.videoChannel);
			for ( int i = 0 ; i < arrayChan.length ; i++ ) 
				mask[i] = ( ((int) arrayChan[i] ) & 0xFF ) < threshold ;
		}
		BooleanMask2D bmask = new BooleanMask2D( img.getBounds(), mask); 
		return new ROI2DArea( bmask );
	}
	
	public void findFlies (IcyBufferedImage workimage, int t) throws InterruptedException 
	{
		ROI2DArea binarizedImageRoi = binarizeImage (workimage, options.threshold);
		Point2D flyPositionMissed = new Point2D.Double(-1, -1);
 		for (Cage cage: cages.cagesList) 
 		{		
			if (options.detectCage != -1 && cage.getCageNumberInteger() != options.detectCage)
				continue;
			if (cage.cageNFlies <1)
				continue;
			
			BooleanMask2D bestMask = findLargestBlob(binarizedImageRoi, cage);
			if ( bestMask != null ) 
			{
				ROI2DArea flyROI = new ROI2DArea( bestMask ); 
				Rectangle2D rect = flyROI.getBounds2D();
				Point2D flyPosition = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
				cage.flyPositions.addPoint(t, flyPosition);
			}
			else 
			{
				cage.flyPositions.addPoint(t, flyPositionMissed);
			}	
		}
	}
	
	public void initTempRectROIs(Experiment exp, Sequence seq, int option_cagenumber) 
	{
		cages = exp.cages;
		int nbcages = cages.cagesList.size();
		for (int i=0; i < nbcages; i++) 
		{
			Cage cage = cages.cagesList.get(i);
			if (options.detectCage != -1 && cage.getCageNumberInteger() != option_cagenumber)
				continue;
			if (cage.cageNFlies > 0) 
			{
				XYTaSeriesArrayList positions = new XYTaSeriesArrayList();
				positions.ensureCapacity(exp.cages.detect_nframes);
				cage.flyPositions = positions;
			}
		}

	}
	
	public void initParametersForDetection(Experiment exp, BuildSeriesOptions	options) 
	{
		this.options = options;
		exp.cages.detect_nframes = (int) (((exp.cages.detectLast_Ms - exp.cages.detectFirst_Ms) / exp.cages.detectBin_Ms) +1);
		exp.cages.clearAllMeasures(options.detectCage);
		cages = exp.cages;
		cages.computeBooleanMasksForCages();
		rectangleAllCages = null;
		for (Cage cage: cages.cagesList) 
		{
			if (cage.cageNFlies < 1) 
				continue;
			Rectangle rect = cage.cageRoi.getBounds();
			if (rectangleAllCages == null)
				rectangleAllCages = new Rectangle(rect);
			else
				rectangleAllCages.add(rect);
		}
	}
	
	
}
