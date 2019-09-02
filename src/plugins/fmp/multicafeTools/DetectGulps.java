package plugins.fmp.multicafeTools;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class DetectGulps {
	
	private DetectGulps_Options options 		= null;
	private List <Integer> 		topLevelArray 	= null;
	private SequenceKymos 		seqkymo 		= null;
	
	public void detectGulps(DetectGulps_Options options, SequenceKymos seqkymo) {	
		
		ProgressChrono progressBar = new ProgressChrono("Detection of gulps started");
		progressBar.initStuff(seqkymo.seq.getSizeT() );
		
		this.options = options;
		this.seqkymo = seqkymo;
		
		int jitter = 5;
		int firstkymo = 0;
		int lastkymo = seqkymo.seq.getSizeT() -1;
		if (! options.detectAllGulps) {
			firstkymo = options.firstkymo;
			lastkymo = firstkymo;
		}
		
		seqkymo.seq.beginUpdate();
		for (int indexkymo=firstkymo; indexkymo <= lastkymo; indexkymo++) {
			progressBar.updatePositionAndTimeLeft(indexkymo);
			Capillary cap = seqkymo.capillaries.capillariesArrayList.get(indexkymo);
			cap.gulpsOptions.copy(options);
			
			if (options.buildDerivative) {
				topLevelArray = cap.getIntegerArrayFromPolyline2D(cap.ptsTop);
				seqkymo.removeRoisContainingString(indexkymo, "derivative");
				getDerivativeProfile(indexkymo, cap, jitter);	
			}
			
			if (options.buildGulps) {
				seqkymo.removeRoisContainingString(indexkymo, "gulp");
				getGulps(indexkymo, cap);
			}
		}
		seqkymo.seq.endUpdate();

		System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
		progressBar.close();
	}	

	private void getDerivativeProfile(int indexkymo, Capillary cap, int jitter) {
		
		int z = seqkymo.seq.getSizeZ() -1;
		IcyBufferedImage image = seqkymo.seq.getImage(indexkymo, z, 0);
		List<Point2D> listOfMaxPoints = new ArrayList<>();
		int[] kymoImageValues = Array1DUtil.arrayToIntArray(image.getDataXY(0), image.isSignedDataType());	// channel 0 - RED
		int xwidth = image.getSizeX();
		int yheight = image.getSizeY();
		int ix = 0;
		int iy = 0;
		for (ix = 1; ix < topLevelArray.size(); ix++) {
			// for each point of topLevelArray, define a bracket of rows to look at ("jitter" = 10)
			int low = topLevelArray.get(ix)- jitter;
			int high = low + 2*jitter;
			if (low < 0) 
				low = 0;
			if (high >= yheight) 
				high = yheight-1;

			int max = kymoImageValues [ix + low*xwidth];
			for (iy = low+1; iy < high; iy++) 
			{
				int val = kymoImageValues [ix  + iy*xwidth];
				if (max < val) 
					max = val;
			}
			listOfMaxPoints.add(new Point2D.Double((double) ix, (double) max));
		}
		
		ROI2DPolyLine roiDerivative = new ROI2DPolyLine ();
		roiDerivative.setName(cap.getLast2ofCapillaryName()+"_derivative");
		roiDerivative.setColor(Color.yellow);
		roiDerivative.setStroke(1);
		roiDerivative.setPoints(listOfMaxPoints);
		roiDerivative.setT(indexkymo);
		seqkymo.seq.addROI(roiDerivative, false);
		
		cap.ptsDerivative = roiDerivative.getPolyline2D();
	}

	private void getGulps(int indexkymo, Capillary cap) {
		int indexpixel = 0;
		if (cap.gulpsRois == null)
			cap.gulpsRois = new ArrayList <> ();

		int start = 1;
		int end = topLevelArray.size();
		if (options.analyzePartOnly) {
			ROI2DUtilities.removeROIsWithinPixelInterval(cap.gulpsRois, options.startPixel, options.endPixel);
			start = options.startPixel;
			end = options.endPixel;
		} else {
			cap.gulpsRois.clear();
		}

		ROI2DPolyLine roiTrack = new ROI2DPolyLine ();
		List<Point2D> gulpPoints = new ArrayList<>();
		Point2D.Double singlePoint = null;
		for (indexpixel = start; indexpixel < end; indexpixel++) {
			int max = (int) cap.ptsDerivative.ypoints[indexpixel-1];
			if (max < cap.gulpsOptions.detectGulpsThreshold)
				continue;
			
			if (gulpPoints.size() > 0) {
				Point2D prevPt = gulpPoints.get(gulpPoints.size() -1);
				if ((int) prevPt.getX() !=  (indexpixel-1)) {
					roiTrack.setColor(Color.red);
					roiTrack.setStroke(1);
					roiTrack.setName(cap.getLast2ofCapillaryName()+"_gulp"+String.format("%07d", indexpixel));
					roiTrack.setPoints(gulpPoints);
					roiTrack.setT(indexkymo);
					cap.gulpsRois.add(roiTrack);
					
					roiTrack = new ROI2DPolyLine ();
					gulpPoints = new ArrayList<>();
					singlePoint = new Point2D.Double (indexpixel-1, topLevelArray.get(indexpixel-1));
					gulpPoints.add(singlePoint);
				}
			} 
			singlePoint = new Point2D.Double (indexpixel, topLevelArray.get(indexpixel));
			gulpPoints.add(singlePoint);
		}

		if (gulpPoints.size() > 0) {
			roiTrack.setPoints(gulpPoints);
			roiTrack.setColor(Color.red);
			roiTrack.setStroke(1);
			roiTrack.setT(indexkymo);
			roiTrack.setName(cap.getLast2ofCapillaryName()+"_gulp"+String.format("%07d", indexpixel));
			cap.gulpsRois.add(roiTrack);
		}
		seqkymo.seq.addROIs(cap.gulpsRois, false);
	}
}
