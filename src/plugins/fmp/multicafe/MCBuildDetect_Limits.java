package plugins.fmp.multicafe;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeTools.EnumArrayListType;
import plugins.fmp.multicafeTools.ProgressChrono;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class MCBuildDetect_Limits {
	
	public void detectCapillaryLevels(MCBuildDetect_LimitsOptions options,  ArrayList <SequencePlus> kymographArrayList) {

		// send some info
		ProgressChrono progressBar = new ProgressChrono("Detection of gulps started");
		progressBar.initStuff(kymographArrayList.size() );
		

		int jitter = 10;
		int firstkymo = 0;
		int lastkymo = kymographArrayList.size() -1;
		if (! options.detectAllLevel) {
			firstkymo = options.firstkymo;
			lastkymo = firstkymo;
		}

		// scan each kymograph in the list
		for (int kymo=firstkymo; kymo <= lastkymo; kymo++) 
		{
			// update progression bar
			progressBar.updatePositionAndTimeLeft(kymo);

			SequencePlus kymographSeq = kymographArrayList.get(kymo);
			kymographSeq.removeAllROI();
			options.copyDetectionParametersToSequenceHeader(kymographSeq); 
			
			List<Point2D> ptsTop = new ArrayList<>();			
			List<Point2D> ptsBottom = new ArrayList<>();

			kymographSeq.beginUpdate();
			IcyBufferedImage image = null;
			int c = 0;
			image = kymographSeq.getImage(0, 1);
			Object dataArray = image.getDataXY(c);
			double[] tabValues = Array1DUtil.arrayToDoubleArray(dataArray, image.isSignedDataType());
			
			int xwidth = image.getSizeX();
			int yheight = image.getSizeY();
	
			int ix = 0;
			int iy = 0;
			int oldiytop = 0;		// assume that curve goes from left to right with jitter 
			int oldiybottom = yheight-1;
			
			boolean flagtop = true;
			boolean flagbottom = true; 

			// scan each image column
			for (ix = 0; ix < xwidth; ix++) 
			{
				// ---------------------------------------------------- detect top level
				if (flagtop) {
					// set flags for internal loop (part of the row)
					boolean found = false;
					double x = ix;
					double y = 0;
					oldiytop -= jitter;
					if (oldiytop < 0) 
						oldiytop = 0;

					// for each line, go from left to right - starting from the last position found minus "jitter" (set to 10)
					for (iy = oldiytop; iy < yheight; iy++) 
					{
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
					// add new point to display as roi
					ptsTop.add(new Point2D.Double (x, y));
				}
				
				// --------------------------------------------------- detect bottom level
				if (flagbottom) {
					// set flags for internal loop (part of the row)
					boolean found = false;
					double x = ix;
					double y = 0;
					oldiybottom = yheight - 1;

					// for each line, go from left to right - starting from the last position found minus "jitter" (set to 10)
					for (iy = oldiybottom; iy >= 0 ; iy--) 
					{
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
					// add new point to display as roi
					ptsBottom.add(new Point2D.Double (x, y));
				}
			}
			
			if (flagtop) {
				ROI2DPolyLine roiTopTrack = new ROI2DPolyLine ();
				roiTopTrack.setName("toplevel");
				roiTopTrack.setStroke(1);
				kymographSeq.addROI(roiTopTrack);
				roiTopTrack.setPoints(ptsTop);
			}
			
			if (flagbottom) {
				ROI2DPolyLine roiBottomTrack = new ROI2DPolyLine ();
				roiBottomTrack.setName("bottomlevel");
				roiBottomTrack.setStroke(1);
				kymographSeq.addROI(roiBottomTrack);
				roiBottomTrack.setPoints(ptsBottom);
			}
			
			kymographSeq.getArrayListFromRois(EnumArrayListType.cumSum);
			kymographSeq.endUpdate();
		}

		// send some info
		System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
		progressBar.close();
	}
	
}
