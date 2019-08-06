package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
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
		progressBar.initStuff(seqkymo.seq.getSizeT() );
		
		int jitter = 5;
		int tfirst = 0;
		int tlast = seqkymo.seq.getSizeT() -1;
		if (! options.detectAllGulps) {
			tfirst = options.firstkymo;
			tlast = tfirst;
		}
		
		seqkymo.seq.beginUpdate();
		
		for (int t=tfirst; t <= tlast; t++) 
		{
			progressBar.updatePositionAndTimeLeft(t);
			Capillary cap = seqkymo.capillaries.capillariesArrayList.get(t);
			options.copy(cap.gulpsOptions);
			
			removeSpecificRoisFromSequence(seqkymo, t, "derivative");
			if (cap.derivedValuesArrayList != null)
				cap.derivedValuesArrayList.clear();
			cap.derivedValuesArrayList = new ArrayList<Integer>();
			cap.derivedValuesArrayList.add(0);
			ArrayList <Integer> topLevelArray = cap.getYFromPtArray(cap.ptsTop);
			
			getDerivativeProfile(seqkymo, t, cap, topLevelArray, jitter);	
			if (options.computeDiffnAndDetect) {
				removeSpecificRoisFromSequence(seqkymo, t, "gulp");
				getGulps(seqkymo, t, cap, topLevelArray);
			}
		}
		seqkymo.seq.endUpdate();

		System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
		progressBar.close();
	}	

	private void removeSpecificRoisFromSequence(SequencePlus kymographSeq, int t, String gulp) {
		
		for (ROI roi:kymographSeq.seq.getROIs()) {
			if (roi instanceof ROI2D 
			&& ((ROI2D) roi).getT() == t 
			&& roi.getName().contains(gulp))
				kymographSeq.seq.removeROI(roi);
		}
	}
	
	private void getDerivativeProfile(SequencePlus kymographSeq, int t, Capillary cap, ArrayList <Integer> topLevelArray, int jitter) {
		
		int z = kymographSeq.seq.getSizeZ() -1;
		IcyBufferedImage image = kymographSeq.seq.getImage(t, z, 0);
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
		kymographSeq.seq.addROI(roiMaxTrack, false);
	}

	private void getGulps(SequencePlus kymographSeq, int t, Capillary cap, ArrayList <Integer> topLevelArray) {
		int ix = 0;
		if (cap.gulpsRois != null)
			cap.gulpsRois.clear();
		cap.gulpsRois = new ArrayList <> ();
		ROI2DPolyLine roiTrack = new ROI2DPolyLine ();
		List<Point2D> gulpPoints = new ArrayList<>();
		Point2D.Double singlePoint = null;
		for (ix = 1; ix < topLevelArray.size(); ix++) 
		{
			int max = cap.derivedValuesArrayList.get(ix-1);
			if (max < kymographSeq.detectGulpsThreshold)
				continue;
			
			if (gulpPoints.size() > 0) {
				Point2D prevPt = gulpPoints.get(gulpPoints.size() -1);
				if ((int) prevPt.getX() !=  (ix-1)) {
					roiTrack.setColor(Color.red);
					roiTrack.setStroke(1);
					roiTrack.setName("gulp"+String.format("%07d", ix));
					roiTrack.setPoints(gulpPoints);
					roiTrack.setT(t);
					cap.gulpsRois.add(roiTrack);
					
					roiTrack = new ROI2DPolyLine ();
					gulpPoints = new ArrayList<>();
					singlePoint = new Point2D.Double (ix-1, topLevelArray.get(ix-1));
					gulpPoints.add(singlePoint);
				}
			} 
			singlePoint = new Point2D.Double (ix, topLevelArray.get(ix));
			gulpPoints.add(singlePoint);
		}

		if (gulpPoints.size() > 0) {
			roiTrack.setPoints(gulpPoints);
			roiTrack.setColor(Color.red);
			roiTrack.setStroke(1);
			roiTrack.setT(t);
			roiTrack.setName("gulp"+String.format("%07d", ix));
			cap.gulpsRois.add(roiTrack);
		}
		kymographSeq.seq.addROIs(cap.gulpsRois, false);
	}
}
