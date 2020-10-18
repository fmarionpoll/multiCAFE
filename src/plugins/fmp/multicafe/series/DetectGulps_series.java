package plugins.fmp.multicafe.series;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafe.sequence.Capillary;
import plugins.fmp.multicafe.sequence.CapillaryLimits;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.sequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;



public class DetectGulps_series extends SwingWorker<Integer, Integer> {
	private SequenceKymos 		seqkymo 		= null;
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public DetectGulps_Options 	options 		= new DetectGulps_Options();
	
	@Override
	protected Integer doInBackground() throws Exception {
		System.out.println("start detect gulps thread");
		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(false);
		
		threadRunning = true;
        int nbiterations = 0;
		ExperimentList expList = options.expList;
		ProgressFrame progress = new ProgressFrame("Detect limits");
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag)
				break;
			Experiment exp = expList.getExperiment(index);
			exp.resultsSubPath = options.resultsSubPath;
			exp.getResultsDirectory(); 
			progress.setMessage("Processing file: " + (index +1) + "//" + (expList.index1 +1));

			exp.loadExperimentCapillariesData_ForSeries();
			if ( exp.loadKymographs()) {
				System.out.println((index+1) + " - "+ exp.getExperimentFileName() + " " + exp.resultsSubPath);
				buildFilteredImage(exp);
				detectGulps(exp);
				exp.xmlSaveMCcapillaries();
			}
			exp.seqKymos.closeSequence();
		}
		progress.close();
		threadRunning = false;
		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(true);
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
	
	private void buildFilteredImage(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		int zChannelDestination = 2;
		exp.kymosBuildFiltered(0, zChannelDestination, options.transformForGulps, options.spanDiff);
	}
	
	public void detectGulps(Experiment exp) {			
		this.seqkymo = exp.seqKymos;

		int jitter = 5;
		int firstkymo = 0;
		int lastkymo = seqkymo.seq.getSizeT() -1;
		if (!options.detectAllGulps) {
			firstkymo = options.firstkymo;
			lastkymo = firstkymo;
		}
		seqkymo.seq.beginUpdate();
		for (int indexkymo=firstkymo; indexkymo <= lastkymo; indexkymo++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(indexkymo);
			cap.setGulpsOptions(options);
			if (options.buildDerivative) {
				seqkymo.removeRoisContainingString(indexkymo, "derivative");
				getDerivativeProfile(indexkymo, cap, jitter);	
			}
			if (options.buildGulps) {
				cap.cleanGulps();
				seqkymo.removeRoisContainingString(indexkymo, "gulp");
				cap.getGulps(indexkymo);
				if (cap.gulpsRois.rois.size() > 0)
					seqkymo.seq.addROIs(cap.gulpsRois.rois, false);
			}
		}
		seqkymo.seq.endUpdate();
		seqkymo.closeSequence();
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
		if (polyline == null)
			return;
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


