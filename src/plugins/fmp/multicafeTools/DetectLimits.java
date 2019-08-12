package plugins.fmp.multicafeTools;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class DetectLimits {
	List<Point2D> limitTop =null;
	List<Point2D> limitBottom =null;
	
	public void detectCapillaryLevels(DetectLimits_Options options,  SequenceKymos seqkymo) {
		// send some info
		ProgressChrono progressBar = new ProgressChrono("Detection of gulps started");
		progressBar.initStuff(seqkymo.seq.getSizeT() );
		
		int jitter = 10;
		int tfirst = 0;
		int tlast = seqkymo.seq.getSizeT() -1;
		if (! options.detectAllImages) {
			tfirst = options.firstImage;
			tlast = tfirst;
		}
		seqkymo.seq.beginUpdate();
		seqkymo.seq.removeAllROI();
		
		for (int t=tfirst; t <= tlast; t++) {
			progressBar.updatePositionAndTimeLeft(t);
			Capillary cap = seqkymo.capillaries.capillariesArrayList.get(t);
			cap.ptsTop = null;
			cap.ptsBottom = null;
			cap.ptsDerivative = null;
			cap.gulpsRois = null;
			limitTop = new ArrayList<Point2D>();
			limitBottom = new ArrayList<Point2D>();
			
			options.copy(cap.limitsOptions); 
			IcyBufferedImage image = null;
			int c = 0;
			image = seqkymo.seq.getImage(t, 1);
			Object dataArray = image.getDataXY(c);
			double[] tabValues = Array1DUtil.arrayToDoubleArray(dataArray, image.isSignedDataType());
			
			int xwidth = image.getSizeX();
			int yheight = image.getSizeY();
			int oldiytop = 0;		// assume that curve goes from left to right with jitter 
			int oldiybottom = yheight-1;
			boolean flagtop = true;
			boolean flagbottom = true; 

			// scan each image column
			for (int ix = 0; ix < xwidth; ix++) {
				if (flagtop)
					detectTop(ix, oldiytop, jitter, tabValues, cap, xwidth, yheight, options);
				
				if (flagbottom) 
					detectBottom(ix, oldiybottom, jitter, tabValues, cap, xwidth, yheight, options);
			}
			
			if (flagtop) {
				ROI2DPolyLine roiTopTrack = new ROI2DPolyLine (limitTop);
				roiTopTrack.setName(cap.getLast2ofCapillaryName()+"_toplevel");
				roiTopTrack.setStroke(1);
				roiTopTrack.setT(t);
				seqkymo.seq.addROI(roiTopTrack);
				cap.ptsTop = roiTopTrack.getPolyline2D();
			}
			
			if (flagbottom) {
				ROI2DPolyLine roiBottomTrack = new ROI2DPolyLine (limitBottom);
				roiBottomTrack.setName(cap.getLast2ofCapillaryName()+"_bottomlevel");
				roiBottomTrack.setStroke(1);
				roiBottomTrack.setT(t);
				seqkymo.seq.addROI(roiBottomTrack);
				cap.ptsBottom = roiBottomTrack.getPolyline2D();
			}
		}
		seqkymo.seq.endUpdate();

		// send some info
		System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
		progressBar.close();
	}
	
	void detectTop(int ix, int oldiytop, int jitter, double[] tabValues, Capillary cap, int xwidth, int yheight, DetectLimits_Options options) {
		boolean found = false;
		double x = ix;
		double y = 0;
		oldiytop -= jitter;
		if (oldiytop < 0) 
			oldiytop = 0;

		// for each line, go from left to right - starting from the last position found minus "jitter" (set to 10)
		for (int iy = oldiytop; iy < yheight; iy++) {
			boolean flag = false;
			if (options.directionUp)
				flag = tabValues [ix + iy* xwidth] > options.detectLevelThreshold;
			else 
				flag = tabValues [ix + iy* xwidth] < options.detectLevelThreshold;

			if( flag) {
				y = iy;
				found = true;
				oldiytop = iy;
				break;
			}
		}
		if (!found) {
			oldiytop = 0;
		}
		limitTop.add(new Point2D.Double (x, y));
	}
	
	void detectBottom(int ix, int oldiybottom, int jitter, double[] tabValues, Capillary cap, int xwidth, int yheight, DetectLimits_Options options) {
		// set flags for internal loop (part of the row)
		boolean found = false;
		double x = ix;
		double y = 0;
		oldiybottom = yheight - 1;

		// for each line, go from left to right - starting from the last position found minus "jitter" (set to 10)
		for (int iy = oldiybottom; iy >= 0 ; iy--) {
			boolean flag = false;
			if (options.directionUp)
				flag = tabValues [ix + iy* xwidth] > options.detectLevelThreshold;
			else 
				flag = tabValues [ix + iy* xwidth] < options.detectLevelThreshold;
			if (flag) {
				y = iy;
				found = true;
				oldiybottom = iy;
				break;
			}
		}
		if (!found) {
			oldiybottom = yheight - 1;
		}
		limitBottom.add(new Point2D.Double (x, y));
	}
	
}
