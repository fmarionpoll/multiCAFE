package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeTools.EnumArrayListType;
import plugins.fmp.multicafeTools.ProgressChrono;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class MCBuildDetect_Gulps {
	
	public void detectGulps(MCBuildDetect_GulpsOptions options, ArrayList <SequencePlus> kymographArrayList) {	
		
		ProgressChrono progressBar = new ProgressChrono("Detection of gulps started");
		progressBar.initStuff(kymographArrayList.size() );
		
		int jitter = 5;
		int firstkymo = 0;
		int lastkymo = kymographArrayList.size() -1;
		if (! options.detectAllGulps) {
			firstkymo = options.firstkymo;
			lastkymo = firstkymo;
		}
		
		for (int kymo=firstkymo; kymo <= lastkymo; kymo++) 
		{
			progressBar.updatePositionAndTimeLeft(kymo);

			SequencePlus kymographSeq = kymographArrayList.get(kymo);
			kymographSeq.beginUpdate();
			
			options.copyDetectionParametersToSequenceHeader(kymographSeq);
			clearPreviousGulps(kymographSeq, "gulp");
			clearPreviousGulps(kymographSeq, "derivative");
			
			IcyBufferedImage image = kymographSeq.getImage(0, 2, 0);	// time=0; z=2; c=0
			double[] kymoImageValues = Array1DUtil.arrayToDoubleArray(image.getDataXY(0), image.isSignedDataType());	// channel 0 - RED

			int xwidth = image.getSizeX();
			int yheight = image.getSizeY();
			int ix = 0;
			int iy = 0;
			List<Point2D> listOfMaxPoints = new ArrayList<>();
			List<Point2D> listOfGulpPoints = new ArrayList<>();
			Collection<ROI> gulpsRois = new ArrayList <> ();
			Point2D.Double singlePoint = null;

			// scan each image row
			kymographSeq.derivedValuesArrayList.add(0);
			
			// once an event is detected, we will cut and paste the corresponding part of topLevelArray
			ArrayList <Integer> topLevelArray = kymographSeq.getArrayListFromRois(EnumArrayListType.topLevel);
			ROI2DPolyLine roiTrack = new ROI2DPolyLine ();

			for (ix = 1; ix < topLevelArray.size(); ix++) 
			{
				// for each point of topLevelArray, define a bracket of rows to look at ("jitter" = 10)
				int low = topLevelArray.get(ix)- jitter;
				int high = low + 2*jitter;
				if (low < 0) 
					low = 0;
				if (high >= yheight) 
					high = yheight-1;

				int max = (int) kymoImageValues [ix + low*xwidth];
				for (iy = low+1; iy < high; iy++) 
				{
					int val = (int) kymoImageValues [ix  + iy*xwidth];
					if (max < val) 
						max = val;
				}

				// add new point to display as roi
				if (max > kymographSeq.detectGulpsThreshold) {
					if (listOfGulpPoints.size() > 0) {
						Point2D prevPt = listOfGulpPoints.get(listOfGulpPoints.size() -1);
						if (prevPt.getX() != (double) (ix-1)) {
							roiTrack.setColor(Color.red);
							roiTrack.setName("gulp"+String.format("%07d", ix));
							roiTrack.setPoints(listOfGulpPoints);
							gulpsRois.add(roiTrack);
							roiTrack = new ROI2DPolyLine ();
							listOfGulpPoints = new ArrayList<>();
							singlePoint = new Point2D.Double (ix-1, topLevelArray.get(ix-1));
							listOfGulpPoints.add(singlePoint);
						}
					} 
					singlePoint = new Point2D.Double (ix, topLevelArray.get(ix));
					listOfGulpPoints.add(singlePoint);
				}
				
				listOfMaxPoints.add(new Point2D.Double((double) ix, (double) (max + yheight/2)));
				kymographSeq.derivedValuesArrayList.add(max);
			}

			if (listOfGulpPoints.size() > 0) {
				roiTrack.setPoints(listOfGulpPoints);
				roiTrack.setColor(Color.red);
				roiTrack.setStroke(1);
				roiTrack.setName("gulp"+String.format("%07d", ix));
				gulpsRois.add(roiTrack);
			}
			kymographSeq.addROIs(gulpsRois, false);
			
			ROI2DPolyLine roiMaxTrack = new ROI2DPolyLine ();
			roiMaxTrack.setName("derivative");
			roiMaxTrack.setColor(Color.cyan);
			roiMaxTrack.setStroke(1);
			roiMaxTrack.setPoints(listOfMaxPoints);
			kymographSeq.addROI(roiMaxTrack, false);
			
			
			kymographSeq.endUpdate(); 
		}

		// send some info
		System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
		progressBar.close();
	}	

	private void clearPreviousGulps(SequencePlus kymographSeq, String gulp) {
		
		for (ROI roi:kymographSeq.getROIs()) {
			if (roi.getName().contains(gulp))
				kymographSeq.removeROI(roi);
		}
		kymographSeq.derivedValuesArrayList.clear();
	}
	
}
