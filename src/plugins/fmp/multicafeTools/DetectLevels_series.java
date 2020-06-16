package plugins.fmp.multicafeTools;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.CapillaryLimits;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;



public class DetectLevels_series  extends SwingWorker<Integer, Integer> {
	List<Point2D> 				limitTop 		= null;
	List<Point2D> 				limitBottom 	= null;
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public DetectLevels_Options options 		= new DetectLevels_Options();
	
	
	@Override
	protected Integer doInBackground() throws Exception {
		System.out.println("start detectLimits thread");
        threadRunning = true;
        int nbiterations = 0;
		ExperimentList expList = options.expList;
		expList.resultsSubPath = options.resultsSubPath; 
		int nbexp = expList.index1 - expList.index0 +1;
		ProgressFrame progress = new ProgressFrame("Detect limits");
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag)
				break;
			Experiment exp = expList.getExperiment(index);
			System.out.println(index+ " - "+ exp.getExperimentFileName());
			progress.setMessage("Processing file: " + (index-expList.index0 +1) + "//" + nbexp);

			exp.loadExperimentData_ForSeries();
//			exp.displayCamData(options.parent0Rect);
			if (exp.loadKymographs()) {
				exp.displaySequenceData(options.parent0Rect, exp.seqKymos.seq);
				exp.kymosBuildFiltered( 0, 1, options.transformForLevels, options.spanDiffTop);
				detectCapillaryLevels(exp);
				saveComputation(exp);
			}
//			exp.seqCamData.closeSequence();
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
		ProgressFrame progress = new ProgressFrame("Save kymograph measures");		
		exp.saveExperimentMeasures();
		progress.close();
	}
	
	private void detectCapillaryLevels(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		int jitter = 10;
		int kymofirst = 0;
		int kymolast = seqKymos.seq.getSizeT() -1;
		if (! options.detectAllKymos) {
			kymofirst = options.firstKymo;
			kymolast = kymofirst;
		}
		seqKymos.seq.beginUpdate();
				
		for (int kymo = kymofirst; kymo <= kymolast; kymo++) {
			seqKymos.removeROIsAtT(kymo);
			limitTop = new ArrayList<Point2D>();
			limitBottom = new ArrayList<Point2D>();
 
			IcyBufferedImage image = null;
			int c = 0;
			image = seqKymos.seq.getImage(kymo, 1);
			Object dataArray = image.getDataXY(c);
			double[] tabValues = Array1DUtil.arrayToDoubleArray(dataArray, image.isSignedDataType());
			
			int startPixel = 0;
			int endPixel = image.getSizeX()-1;
			int xwidth = image.getSizeX();
			int yheight = image.getSizeY();
			Capillary cap = exp.capillaries.capillariesArrayList.get(kymo);
			if (!options.detectR && cap.getCapillaryName().endsWith("2"))
				continue;
			if (!options.detectL && cap.getCapillaryName().endsWith("1"))
				continue;
			
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
			for (int ix = startPixel; ix <= endPixel; ix++) {
				int ytop = detectTop(ix, oldiytop, jitter, tabValues, xwidth, yheight, options);
				int ybottom = detectBottom(ix, oldiybottom, jitter, tabValues, xwidth, yheight, options);
				if (ybottom <= ytop) {
					ybottom = oldiybottom;
					ytop = oldiytop;
				}
				limitTop.add(new Point2D.Double(ix, ytop));
				limitBottom.add(new Point2D.Double(ix, ybottom));
				
				oldiytop = ytop;		// assume that curve goes from left to right with jitter 
				oldiybottom = ybottom;
			}
			if (options.analyzePartOnly) {
				Polyline2DUtil.insertSeriesofYPoints(limitTop, cap.ptsTop.polylineLimit, startPixel, endPixel);
				seqKymos.seq.addROI(cap.ptsTop.transferPolyline2DToROI());
				
				Polyline2DUtil.insertSeriesofYPoints(limitBottom, cap.ptsBottom.polylineLimit, startPixel, endPixel);
				seqKymos.seq.addROI(cap.ptsBottom.transferPolyline2DToROI());
			} else {
				ROI2DPolyLine roiTopTrack = new ROI2DPolyLine (limitTop);
				roiTopTrack.setName(cap.getLast2ofCapillaryName()+"_toplevel");
				roiTopTrack.setStroke(1);
				roiTopTrack.setT(kymo);
				seqKymos.seq.addROI(roiTopTrack);
				cap.ptsTop = new CapillaryLimits(roiTopTrack.getName(), kymo-kymofirst, roiTopTrack.getPolyline2D());

				ROI2DPolyLine roiBottomTrack = new ROI2DPolyLine (limitBottom);
				roiBottomTrack.setName(cap.getLast2ofCapillaryName()+"_bottomlevel");
				roiBottomTrack.setStroke(1);
				roiBottomTrack.setT(kymo);
				seqKymos.seq.addROI(roiBottomTrack);
				cap.ptsBottom = new CapillaryLimits(roiBottomTrack.getName(), kymo-kymofirst, roiBottomTrack.getPolyline2D());
			}
		}
		seqKymos.seq.endUpdate();
	}
	
	private int detectTop(int ix, int oldiytop, int jitter, double[] tabValues, int xwidth, int yheight, DetectLevels_Options options) {
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
			oldiytop = yheight-1; // 0;
		}
		return y;
	}
	
	private int detectBottom(int ix, int oldiybottom, int jitter, double[] tabValues, int xwidth, int yheight, DetectLevels_Options options) {
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
			oldiybottom = 0; //yheight - 1;
		}
		return y;
	}

	
}
