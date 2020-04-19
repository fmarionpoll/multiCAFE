package plugins.fmp.multicafeTools;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;

import icy.type.collection.array.Array1DUtil;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.CapillaryLimits;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;


public class DetectGulps_series extends SwingWorker<Integer, Integer> {
	private SequenceKymos 		seqkymo 		= null;
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public DetectGulps_Options 	options 		= new DetectGulps_Options();
	
	@Override
	protected Integer doInBackground() throws Exception {
		System.out.println("start detectLimits thread");
        threadRunning = true;
        int nbiterations = 0;
		ExperimentList expList = options.expList;
		int nbexp = expList.index1 - expList.index0 +1;
		ProgressFrame progress = new ProgressFrame("Detect limits");
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag)
				break;
			Experiment exp = expList.getExperiment(index);
			System.out.println(exp.experimentFileName);
			progress.setMessage("Processing file: " + (index-expList.index0 +1) + "//" + nbexp);
			
			exp.loadExperimentData();
			exp.displayCamData(options.parent0Rect);
			if ( exp.loadKymographs()) {
				displayGulps(exp);
				detectGulps(exp);
				saveComputation(exp);
			}
			exp.seqCamData.closeSequence();
			exp.seqKymos.closeSequence();
		}
		progress.close();
		threadRunning = false;
		return nbiterations;
	}

	@Override
	protected void done() {
		int statusMsg = 0;
		try {
			statusMsg = get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} 
		if (!threadRunning || stopFlag) {
			firePropertyChange("thread_ended", null, statusMsg);
		} else {
			firePropertyChange("thread_done", null, statusMsg);
		}
    }
	
	private void saveComputation(Experiment exp) {			
		Path dir = Paths.get(exp.seqCamData.getDirectory());
		dir = dir.resolve("results");
		String directory = dir.toAbsolutePath().toString();
		if (Files.notExists(dir))  {
			try {
				Files.createDirectory(dir);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Creating directory failed: "+ directory);
				return;
			}
		}
		ProgressFrame progress = new ProgressFrame("Save kymograph measures");		
		exp.saveExperimentMeasures();
		progress.close();
	}
	
	private void displayGulps(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;

		int zChannelDestination = 2;
		exp.kymosBuildFiltered(0, zChannelDestination, options.transformForGulps, options.spanDiff);
	}
	
	public void detectGulps(Experiment exp) {			
		this.seqkymo = exp.seqKymos;
		ProgressChrono progressBar = new ProgressChrono("Detection of gulps started");
		progressBar.initChrono(seqkymo.seq.getSizeT() );
		int jitter = 5;
		int firstkymo = 0;
		int lastkymo = seqkymo.seq.getSizeT() -1;
		if (!options.detectAllGulps) {
			firstkymo = options.firstkymo;
			lastkymo = firstkymo;
		}
		seqkymo.seq.beginUpdate();
		for (int indexkymo=firstkymo; indexkymo <= lastkymo; indexkymo++) {
			progressBar.updatePositionAndTimeLeft(indexkymo);
			Capillary cap = exp.capillaries.capillariesArrayList.get(indexkymo);
			cap.gulpsOptions = options;
			if (options.buildDerivative) {
				seqkymo.removeRoisContainingString(indexkymo, "derivative");
				getDerivativeProfile(indexkymo, cap, jitter);	
			}
			if (options.buildGulps) {
				cap.cleanGulps(options);
				seqkymo.removeRoisContainingString(indexkymo, "gulp");
				cap.getGulps(indexkymo, options);
				if (cap.gulpsRois.rois.size() > 0)
					seqkymo.seq.addROIs(cap.gulpsRois.rois, false);
			}
		}
		seqkymo.seq.endUpdate();
		seqkymo.closeSequence();
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
		Polyline2D 	polyline = cap.ptsTop.polylineLimit;
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


