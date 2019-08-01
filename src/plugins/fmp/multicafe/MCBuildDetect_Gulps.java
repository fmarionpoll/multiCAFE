package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeTools.ProgressChrono;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class MCBuildDetect_Gulps {
	
	public void detectGulps(MCBuildDetect_GulpsOptions options, SequencePlus seqkymo) {	
		
		ProgressChrono progressBar = new ProgressChrono("Detection of gulps started");
		progressBar.initStuff(seqkymo.getSizeT() );
		
		int jitter = 5;
		int firstkymo = 0;
		int lastkymo = seqkymo.getSizeT() -1;
		if (! options.detectAllGulps) {
			firstkymo = options.firstkymo;
			lastkymo = firstkymo;
		}
		
		seqkymo.beginUpdate();
		
		for (int kymo=firstkymo; kymo <= lastkymo; kymo++) 
		{
			progressBar.updatePositionAndTimeLeft(kymo);

			Capillary cap = seqkymo.capillaries.capillariesArrayList.get(kymo);
			
			options.copy(cap.gulpsOptions);
			removeSpecificRoisFromSequence(seqkymo, kymo, "gulp");
			removeSpecificRoisFromSequence(seqkymo, kymo, "derivative");
			cap.derivedValuesArrayList.clear();

			cap.derivedValuesArrayList.add(0);
			ArrayList <Integer> topLevelArray = cap.getYFromPtArray(cap.ptsTop);
			getDerivativeProfile(seqkymo, kymo, cap, topLevelArray, jitter);				;
			if (options.computeDiffnAndDetect) 
				getGulps(seqkymo, cap, topLevelArray);
		}
		seqkymo.endUpdate();

		// send some info
		System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
		progressBar.close();
	}	

	private void removeSpecificRoisFromSequence(SequencePlus kymographSeq, int t, String gulp) {
		
		for (ROI roi:kymographSeq.getROIs()) {
			if (roi instanceof ROI2D 
			&& ((ROI2D) roi).getT() == t 
			&& roi.getName().contains(gulp))
				kymographSeq.removeROI(roi);
		}
	}
	
	private void getDerivativeProfile(SequencePlus kymographSeq, int t, Capillary cap, ArrayList <Integer> topLevelArray, int jitter) {
		
		int z = kymographSeq.getSizeZ() -1;
		IcyBufferedImage image = kymographSeq.getImage(t, z, 0);
		List<Point2D> listOfMaxPoints = new ArrayList<>();
		int[] kymoImageValues = Array1DUtil.arrayToIntArray(image.getDataXY(0), image.isSignedDataType());	// channel 0 - RED
		int xwidth = image.getSizeX();
		int yheight = image.getSizeY();
		int ix = 0;
		int iy = 0;
		for (ix = 1; ix < topLevelArray.size(); ix++) 
		{
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
			listOfMaxPoints.add(new Point2D.Double((double) ix, (double) ( yheight/2 - max)));
			cap.derivedValuesArrayList.add(max);
		}
		ROI2DPolyLine roiMaxTrack = new ROI2DPolyLine ();
		roiMaxTrack.setName("derivative");
		roiMaxTrack.setColor(Color.yellow);
		roiMaxTrack.setStroke(1);
		roiMaxTrack.setPoints(listOfMaxPoints);
		kymographSeq.addROI(roiMaxTrack, false);
	}

	private void getGulps(SequencePlus kymographSeq, Capillary cap, ArrayList <Integer> topLevelArray) {
		int ix = 0;
		ROI2DPolyLine roiTrack = new ROI2DPolyLine ();

		List<Point2D> listOfGulpPoints = new ArrayList<>();
		Collection<ROI> gulpsRois = new ArrayList <> ();
		Point2D.Double singlePoint = null;
		for (ix = 1; ix < topLevelArray.size(); ix++) 
		{
			int max = cap.derivedValuesArrayList.get(ix-1);
			if (max < kymographSeq.detectGulpsThreshold)
				continue;
			
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

		if (listOfGulpPoints.size() > 0) {
			roiTrack.setPoints(listOfGulpPoints);
			roiTrack.setColor(Color.red);
			roiTrack.setStroke(1);
			roiTrack.setName("gulp"+String.format("%07d", ix));
			gulpsRois.add(roiTrack);
		}
		kymographSeq.addROIs(gulpsRois, false);
		
	}
}
