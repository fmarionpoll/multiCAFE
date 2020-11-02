package plugins.fmp.multicafe.series;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe.sequence.Capillary;
import plugins.fmp.multicafe.sequence.CapillaryLimits;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.SequenceKymos;
import plugins.fmp.multicafe.tools.Polyline2DUtil;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;



public class DetectLevels_series extends BuildSeries  {

	void analyzeExperiment(Experiment exp) {
		String resultsDirectory = exp.getResultsDirectory(); 
		exp.loadExperimentCapillariesData_ForSeries();
		if (exp.loadKymographs()) {	
			exp.kymosBuildFiltered( 0, 1, options.transformForLevels, options.spanDiffTop);
			detectCapillaryLevels(exp);
			exp.saveExperimentMeasures(resultsDirectory);
		}
		exp.seqKymos.closeSequence();
	}
	
	private void detectCapillaryLevels(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		int jitter = 10;
		int firstkymo = 0;
		int lastkymo = seqKymos.seq.getSizeT() -1;
		if (! options.detectAllKymos) {
			firstkymo = options.firstKymo;
			lastkymo = firstkymo;
		}
		seqKymos.seq.beginUpdate();
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		
		int nframes = lastkymo - firstkymo +1;
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detect_levels");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
				
		for (int frame = firstkymo; frame <= lastkymo; frame++) {
			seqKymos.removeROIsAtT(frame);
			final int t_from = frame;
			final int t_kymofirst = firstkymo;
			futures.add(processor.submit(new Runnable () {
			@Override
			public void run() {
				List<Point2D> limitTop = new ArrayList<Point2D>();
				List<Point2D> limitBottom = new ArrayList<Point2D>();
	 
				int c = 0;
				IcyBufferedImage image = seqKymos.seq.getImage(t_from, 1);
				Object dataArray = image.getDataXY(c);
				double[] tabValues = Array1DUtil.arrayToDoubleArray(dataArray, image.isSignedDataType());
				
				int startPixel = 0;
				int endPixel = image.getSizeX()-1;
				int xwidth = image.getSizeX();
				int yheight = image.getSizeY();
				Capillary cap = exp.capillaries.capillariesArrayList.get(t_from);
				if (!options.detectR && cap.getCapillaryName().endsWith("2"))
					return;
				if (!options.detectL && cap.getCapillaryName().endsWith("1"))
					return;
				
				cap.ptsDerivative = null;
				cap.gulpsRois = null;
				options.copy(cap.limitsOptions);
				if (options.analyzePartOnly) {
					startPixel = options.startPixel;
					endPixel = options.endPixel;
					if (endPixel > xwidth-1)
						endPixel = xwidth -1;
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
					roiTopTrack.setT(t_from);
					seqKymos.seq.addROI(roiTopTrack);
					cap.ptsTop = new CapillaryLimits(roiTopTrack.getName(), t_from-t_kymofirst, roiTopTrack.getPolyline2D());
	
					ROI2DPolyLine roiBottomTrack = new ROI2DPolyLine (limitBottom);
					roiBottomTrack.setName(cap.getLast2ofCapillaryName()+"_bottomlevel");
					roiBottomTrack.setStroke(1);
					roiBottomTrack.setT(t_from);
					seqKymos.seq.addROI(roiBottomTrack);
					cap.ptsBottom = new CapillaryLimits(roiBottomTrack.getName(), t_from-t_kymofirst, roiBottomTrack.getPolyline2D());
				}
			}
			}));
		}
		waitAnalyzeExperimentCompletion(processor, futures, progressBar);
		seqKymos.seq.endUpdate();
		progressBar.close();
		processor.shutdown();
	}
	
	private int detectTop(int ix, int oldiytop, int jitter, double[] tabValues, int xwidth, int yheight, BuildSeries_Options options) {
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
	
	private int detectBottom(int ix, int oldiybottom, int jitter, double[] tabValues, int xwidth, int yheight, BuildSeries_Options options) {
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
			oldiybottom = 0;
		}
		return y;
	}

	
}
