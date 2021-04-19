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
import plugins.fmp.multicafe.experiment.Capillary;
import plugins.fmp.multicafe.experiment.CapillaryLimit;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceKymos;
import plugins.fmp.multicafe.tools.ImageTransformTools;



public class DetectLevels_series extends BuildSeries  
{
	ImageTransformTools tImg = new ImageTransformTools();
	
	void analyzeExperiment(Experiment exp) 
	{
		if (loadExperimentDataToDetectLevels(exp)) 
		{ 
			exp.seqKymos.displayViewerAtRectangle(options.parent0Rect);
			if (detectCapillaryLevels(exp)) 
				exp.capillaries.xmlSaveCapillaries_Measures(exp.getKymosBinFullDirectory());
		}
		exp.closeSequences();
	}
	
	private boolean loadExperimentDataToDetectLevels(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCcapillaries();
		boolean flag = exp.loadKymographs(false);
		return flag;
	}
	
	private boolean detectCapillaryLevels(Experiment exp) 
	{
		SequenceKymos seqKymos = exp.seqKymos;
		seqKymos.seq.removeAllROI();
		
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		seqKymos.seq.beginUpdate();
		
		int nframes = options.lastKymo - options.firstKymo +1;
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("buildkymo2");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		tImg.setSpanDiff(options.spanDiffTop);
		tImg.setSequence(seqKymos);
		
		for (int indexKymo = options.firstKymo; indexKymo <= options.lastKymo; indexKymo++) 
		{
			final int t_index = indexKymo;
			Capillary capi = exp.capillaries.capillariesArrayList.get(t_index);
			if (!options.detectR && capi.getCapillaryName().endsWith("2"))
				return false;
			if (!options.detectL && capi.getCapillaryName().endsWith("1"))
				return false;
			final Capillary cap = capi;
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{
					final IcyBufferedImage sourceImage = tImg.transformImage (seqKymos.imageIORead(t_index), options.transformForLevels);
					int c = 0;
					Object dataArray = sourceImage.getDataXY(c);
					int[] sourceValues = Array1DUtil.arrayToIntArray(dataArray, sourceImage.isSignedDataType());

					cap.indexImage= t_index;
					cap.ptsDerivative = null;
					cap.gulpsRois = null;
					cap.limitsOptions.copyFrom(options);
					
					int firstColumn = 0;
					int lastColumn = sourceImage.getSizeX()-1;
					int xwidth = sourceImage.getSizeX();
					int yheight = sourceImage.getSizeY();
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
						cap.ptsTop    = new CapillaryLimit(cap.getLast2ofCapillaryName()+"_toplevel", t_index, limitTop);
						cap.ptsBottom = new CapillaryLimit(cap.getLast2ofCapillaryName()+"_bottomlevel", t_index, limitBottom);
					}
				}}));
		}
		waitAnalyzeExperimentCompletion(processor, futures, progressBar);
		seqKymos.seq.endUpdate();
		progressBar.close();
		
		return true;
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

