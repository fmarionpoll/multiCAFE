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
	
	public void detectCapillaryLevels(DetectLimits_Options options, SequenceKymos seqkymo) {
		ProgressChrono progressBar = new ProgressChrono("Detection of upper/lower capillary limits started");
		progressBar.initStuff(seqkymo.seq.getSizeT() );
		
		int jitter = 10;
		int tfirst = 0;
		int tlast = seqkymo.seq.getSizeT() -1;
		if (! options.detectAllImages) {
			tfirst = options.firstImage;
			tlast = tfirst;
		}
		seqkymo.seq.beginUpdate();
				
		for (int t=tfirst; t <= tlast; t++) {
			progressBar.updatePositionAndTimeLeft(t);
			seqkymo.removeROIsAtT(t);
			limitTop = new ArrayList<Point2D>();
			limitBottom = new ArrayList<Point2D>();
 
			IcyBufferedImage image = null;
			int c = 0;
			image = seqkymo.seq.getImage(t, 1);
			Object dataArray = image.getDataXY(c);
			double[] tabValues = Array1DUtil.arrayToDoubleArray(dataArray, image.isSignedDataType());
			
			int startPixel = 0;
			int endPixel = image.getSizeX()-1;
			int xwidth = image.getSizeX();
			int yheight = image.getSizeY();
			Capillary cap = seqkymo.capillaries.capillariesArrayList.get(t);
			cap.ptsDerivative = null;
			cap.gulpsRois = null;
			options.copy(cap.limitsOptions);
			if (options.analyzePartOnly) {
				startPixel = options.startPixel;
				endPixel = options.endPixel;
			} else {
				cap.ptsTop = null;
				cap.ptsBottom = null;
			}
			int oldiytop = 0;		// assume that curve goes from left to right with jitter 
			int oldiybottom = yheight-1;

			// scan each image column
			for (int ix = startPixel; ix < endPixel; ix++) {
				int ytop = detectTop(ix, oldiytop, jitter, tabValues, xwidth, yheight, options);
				int ybottom = detectBottom(ix, oldiybottom, jitter, tabValues, xwidth, yheight, options);
				
				limitTop.add(new Point2D.Double(ix, ytop));
				limitBottom.add(new Point2D.Double(ix, ybottom));
				
				oldiytop = ytop;		// assume that curve goes from left to right with jitter 
				oldiybottom = ybottom;
			}
			
			if (options.analyzePartOnly) {
				Polyline2DUtil.insertSeriesofYPoints(limitTop, cap.ptsTop, startPixel, endPixel);
				seqkymo.seq.addROI(cap.transferPolyline2DToROI(cap.ID_TOPLEVEL, cap.ptsTop));
				
				Polyline2DUtil.insertSeriesofYPoints(limitBottom, cap.ptsBottom, startPixel, endPixel);
				seqkymo.seq.addROI(cap.transferPolyline2DToROI(cap.ID_BOTTOMLEVEL, cap.ptsBottom));
			} else {
				ROI2DPolyLine roiTopTrack = new ROI2DPolyLine (limitTop);
				roiTopTrack.setName(cap.getLast2ofCapillaryName()+"_toplevel");
				roiTopTrack.setStroke(1);
				roiTopTrack.setT(t);
				seqkymo.seq.addROI(roiTopTrack);
				cap.ptsTop = roiTopTrack.getPolyline2D();

				ROI2DPolyLine roiBottomTrack = new ROI2DPolyLine (limitBottom);
				roiBottomTrack.setName(cap.getLast2ofCapillaryName()+"_bottomlevel");
				roiBottomTrack.setStroke(1);
				roiBottomTrack.setT(t);
				seqkymo.seq.addROI(roiBottomTrack);
				cap.ptsBottom = roiBottomTrack.getPolyline2D();
			}
		
		}
		seqkymo.seq.endUpdate();
		progressBar.close();
	}
	
	int detectTop(int ix, int oldiytop, int jitter, double[] tabValues, int xwidth, int yheight, DetectLimits_Options options) {
		boolean found = false;
		int y = 0;
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

			if (flag) {
				y = iy;
				found = true;
				oldiytop = iy;
				break;
			}
		}
		if (!found) {
			oldiytop = 0;
		}
		return y;
	}
	
	int detectBottom(int ix, int oldiybottom, int jitter, double[] tabValues, int xwidth, int yheight, DetectLimits_Options options) {
		// set flags for internal loop (part of the row)
		boolean found = false;
		int y = 0;
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
		return y;
	}
	
}
