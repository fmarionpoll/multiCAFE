package plugins.fmp.multicafe.series;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe.experiment.Capillary;
import plugins.fmp.multicafe.experiment.CapillaryLimit;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceKymos;
import plugins.fmp.multicafe.tools.ImageTransformTools;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;



public class DetectLevels_series2 extends BuildSeries  
{
	ImageTransformTools tImg = new ImageTransformTools();	
	void analyzeExperiment(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCcapillaries();
		if (exp.loadKymographs(false)) 
		{	
			detectCapillaryLevels(exp);
			String resultsDirectory = exp.getKymosBinFullDirectory(); 
			exp.capillaries.xmlSaveCapillaries_Measures(resultsDirectory);
		}
		exp.seqKymos.closeSequence();
	}
	
	private void detectCapillaryLevels(Experiment exp) 
	{
		SequenceKymos seqKymos = exp.seqKymos;
		int firstkymo = 0;
		int lastkymo = seqKymos.seq.getSizeT() -1;
		if (! options.detectAllKymos) 
		{
			firstkymo = options.firstKymo;
			lastkymo = firstkymo;
		}
		
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with sub-threads started");
		
		tImg.setSpanDiff(options.spanDiffTop);
		tImg.setSequence(seqKymos);
		
		for (int frame = firstkymo; frame <= lastkymo; frame++) 
		{
			seqKymos.removeROIsPolylineAtT(frame);
			final int t_from = frame;
			
			IcyBufferedImage sourceImage = tImg.transformImage (seqKymos.imageIORead(t_from), options.transformForLevels);
			int c = 0;
			Object dataArray = sourceImage.getDataXY(c);
			int[] sourceValues = Array1DUtil.arrayToIntArray(dataArray, sourceImage.isSignedDataType());
			
			int firstColumn = 0;
			int lastColumn = sourceImage.getSizeX()-1;
			int xwidth = sourceImage.getSizeX();
			int yheight = sourceImage.getSizeY();
			Capillary cap = exp.capillaries.capillariesArrayList.get(t_from);
			if (!options.detectR && cap.getCapillaryName().endsWith("2"))
				return;
			if (!options.detectL && cap.getCapillaryName().endsWith("1"))
				return;
			cap.ptsDerivative = null;
			cap.gulpsRois = null;
			options.copyTo(cap.limitsOptions);
			if (options.analyzePartOnly) 
			{
				firstColumn = options.startPixel;
				lastColumn = options.endPixel;
				if (lastColumn > xwidth-1)
					lastColumn = xwidth -1;
			} 
			else 
			{
				cap.ptsTop = null;
				cap.ptsBottom = null;
			}
			int oldiytop = 0;		// assume that curve goes from left to right with jitter 
			int oldiybottom = yheight-1;
			int nColumns = lastColumn - firstColumn +1;
			final int jitter = 10;
			List<Point2D> limitTop = new ArrayList<Point2D>(nColumns);
			List<Point2D> limitBottom = new ArrayList<Point2D>(nColumns);

			// scan each image column
			for (int iColumn = firstColumn; iColumn <= lastColumn; iColumn++) 
			{
				int ytop = detectThresholdFromTop(iColumn, oldiytop, jitter, sourceValues, xwidth, yheight, options);
				int ybottom = detectThresholdFromBottom(iColumn, oldiybottom, jitter, sourceValues, xwidth, yheight, options);
				if (ybottom <= ytop) 
				{
					ybottom = oldiybottom;
					ytop = oldiytop;
				}
				limitTop.add(new Point2D.Double(iColumn, ytop));
				limitBottom.add(new Point2D.Double(iColumn, ybottom));
				oldiytop = ytop;
				oldiybottom = ybottom;
			}
			
			if (options.analyzePartOnly) 
			{
				cap.ptsTop.polylineLimit.insertSeriesofYPoints(limitTop, firstColumn, lastColumn);
				cap.ptsBottom.polylineLimit.insertSeriesofYPoints(limitBottom, firstColumn, lastColumn);
			} 
			else 
			{
				cap.ptsTop    = getLimits(limitTop, cap.getLast2ofCapillaryName()+"_toplevel", t_from, seqKymos);
				cap.ptsBottom = getLimits(limitBottom, cap.getLast2ofCapillaryName()+"_bottomlevel", t_from, seqKymos);
			}
		}
		progressBar.close();

	}
	
	private CapillaryLimit getLimits (List<Point2D> limit, String name, int t_from, SequenceKymos seqKymos ) 
	{
		ROI2DPolyLine roiTrack = new ROI2DPolyLine (limit);
		roiTrack.setName(name);
		roiTrack.setStroke(1);
		roiTrack.setT(t_from);
		seqKymos.seq.addROI(roiTrack);
		return new CapillaryLimit(roiTrack.getName(), roiTrack.getPolyline2D());
	}

	private int checkLimits (int rowIndex, int maximumRowIndex) 
	{
		if (rowIndex < 0)
			rowIndex = 0;
		if (rowIndex > maximumRowIndex)
			rowIndex = maximumRowIndex;
		return rowIndex;
	}

	private int detectThresholdFromTop(int ix, int oldiytop, int jitter, int [] tabValues, int xwidth, int yheight, Options_BuildSeries options) 
	{
		int y = yheight-1;
		oldiytop = checkLimits(oldiytop - jitter, yheight-1);
		for (int iy = oldiytop; iy < yheight; iy++) 
		{
			boolean flag = false;
			if (options.directionUp)
				flag = tabValues [ix + iy* xwidth] > options.detectLevelThreshold;
			else 
				flag = tabValues [ix + iy* xwidth] < options.detectLevelThreshold;
			if (flag) {
				y = iy;
				break;
			}
		}
		return y;
	}
	
	private int detectThresholdFromBottom(int ix, int oldiybottom, int jitter, int[] tabValues, int xwidth, int yheight, Options_BuildSeries options) 
	{
		int y = 0;
		oldiybottom = yheight - 1; // no memory needed  - the bottom is quite stable
		for (int iy = oldiybottom; iy >= 0 ; iy--) 
		{
			boolean flag = false;
			if (options.directionUp)
				flag = tabValues [ix + iy* xwidth] > options.detectLevelThreshold;
			else 
				flag = tabValues [ix + iy* xwidth] < options.detectLevelThreshold;
			if (flag) {
				y = iy;
				break;
			}
		}
		return y;
	}
	
}

