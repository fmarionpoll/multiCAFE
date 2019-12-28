package plugins.fmp.multicafeTools;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.CapillaryLimits;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class DetectLimits_series extends Build_series implements Runnable {
	List<Point2D> 				limitTop 		= null;
	List<Point2D> 				limitBottom 	= null;
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public DetectLimits_Options options 		= new DetectLimits_Options();
	

	
	@Override
	public void run() {
		stopFlag = false;
		threadRunning = true;
		ExperimentList expList = options.expList;
		int nbexp = expList.index1 - expList.index0 +1;
		ProgressChrono progressBar = new ProgressChrono("Detect limits");
		progressBar.initChrono(nbexp);
		progressBar.setMessageFirstPart("Analyze series ");
		for (int index = expList.index0; index <= expList.index1; index++) {
			if (stopFlag)
				break;
			Experiment exp = expList.experimentList.get(index);
//			System.out.println(exp.experimentFileName);
			progressBar.updatePosition(index-expList.index0+1);
			boolean flag = true;
			if (nbexp > 1) {
				exp.loadExperimentData();
				flag = exp.loadKymographs();
			}
			if (flag) {
				exp.kymosBuildFiltered( 0, 1, options.transformForLevels, options.spanDiffTop);
				detectCapillaryLevels(exp);
				saveComputation(exp);
			}
		}
		progressBar.close();
		threadRunning = false;
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
	
	private void detectCapillaryLevels(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		int jitter = 10;
		int kymofirst = 0;
		int kymolast = seqKymos.seq.getSizeT() -1;
		if (! options.detectAllImages) {
			kymofirst = options.firstImage;
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
				Polyline2DUtil.insertSeriesofYPoints(limitTop, cap.ptsTop.polyline, startPixel, endPixel);
				seqKymos.seq.addROI(cap.ptsTop.transferPolyline2DToROI());
				
				Polyline2DUtil.insertSeriesofYPoints(limitBottom, cap.ptsBottom.polyline, startPixel, endPixel);
				seqKymos.seq.addROI(cap.ptsBottom.transferPolyline2DToROI());
			} else {
				ROI2DPolyLine roiTopTrack = new ROI2DPolyLine (limitTop);
				roiTopTrack.setName(cap.getLast2ofCapillaryName()+"_toplevel");
				roiTopTrack.setStroke(1);
				roiTopTrack.setT(kymo);
				seqKymos.seq.addROI(roiTopTrack);
				cap.ptsTop = new CapillaryLimits( "toplevel", kymo-kymofirst, roiTopTrack.getPolyline2D());

				ROI2DPolyLine roiBottomTrack = new ROI2DPolyLine (limitBottom);
				roiBottomTrack.setName(cap.getLast2ofCapillaryName()+"_bottomlevel");
				roiBottomTrack.setStroke(1);
				roiBottomTrack.setT(kymo);
				seqKymos.seq.addROI(roiBottomTrack);
				cap.ptsBottom = new CapillaryLimits( "bottomlevel", kymo-kymofirst, roiBottomTrack.getPolyline2D());
			}
		}
		seqKymos.seq.endUpdate();
	}
	
	private int detectTop(int ix, int oldiytop, int jitter, double[] tabValues, int xwidth, int yheight, DetectLimits_Options options) {
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
	
	private int detectBottom(int ix, int oldiybottom, int jitter, double[] tabValues, int xwidth, int yheight, DetectLimits_Options options) {
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
