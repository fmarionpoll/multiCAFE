package plugins.fmp.multicafeTools;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.CapillaryLimits;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;


public class DetectGulps {
	
	//private DetectGulps_Options options 		= null;
	//private List <Integer> 		topLevelArray 	= null;
	private SequenceKymos 		seqkymo 		= null;
	
	
	public void detectGulps(DetectGulps_Options options, Experiment exp) {			
		//this.options = options;
		this.seqkymo = exp.seqKymos;
		ProgressChrono progressBar = new ProgressChrono("Detection of gulps started");
		progressBar.initStuff(seqkymo.seq.getSizeT() );
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
			Capillary cap = exp.capillaries.capillariesArrayList.get(indexkymo);
			cap.gulpsOptions = options; //.copy(options);
			if (options.buildDerivative) {
				seqkymo.removeRoisContainingString(indexkymo, "derivative");
				getDerivativeProfile(indexkymo, cap, jitter);	
			}
			if (options.buildGulps) {
				cap.getGulps(indexkymo, options);
				if (cap.gulpsRois.rois.size() > 0)
					seqkymo.seq.addROIs(cap.gulpsRois.rois, false);
			}
		}
		seqkymo.seq.endUpdate();
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
		Polyline2D 	polyline = cap.ptsTop.polyline;
		for (ix = 1; ix < polyline.npoints; ix++) {
			// for each point of topLevelArray, define a bracket of rows to look at ("jitter" = 10)
			int low = (int) polyline.ypoints[ix]- jitter;
			int high = low + 2*jitter;
			if (low < 0) 
				low = 0;
			if (high >= yheight) 
				high = yheight-1;
			int max = kymoImageValues [ix + low*xwidth];
			for (iy = low+1; iy < high; iy++) {
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
		cap.ptsDerivative = new CapillaryLimits(roiDerivative.getName(), indexkymo, roiDerivative.getPolyline2D());
	}
	
	
}


