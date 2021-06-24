package plugins.fmp.multicafe2.series;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.CapillaryLimit;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.tools.ImageTransformTools;



public class DetectLevels_series extends BuildSeries  
{
	ImageTransformTools tImg = new ImageTransformTools();
	
	void analyzeExperiment(Experiment exp) 
	{
		if (loadExperimentDataToDetectLevels(exp)) 
		{ 
			exp.seqKymos.displayViewerAtRectangle(options.parent0Rect);
			detectCapillaryLevels(exp);
		}
		exp.closeSequences();
	}
	
	private boolean loadExperimentDataToDetectLevels(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCCapillaries();
		return exp.loadKymographs();
	}
	
	private boolean detectCapillaryLevels(Experiment exp) 
	{
		SequenceKymos seqKymos = exp.seqKymos;
		seqKymos.seq.removeAllROI();
		
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		int firstKymo = options.firstKymo;
		if (firstKymo > seqKymos.seq.getSizeT() || firstKymo < 0)
			firstKymo = 0;
		int lastKymo = options.lastKymo;
		if (lastKymo >= seqKymos.seq.getSizeT())
			lastKymo = seqKymos.seq.getSizeT() -1;
		seqKymos.seq.beginUpdate();
		
		// create an array of tasks for multi-thread processing
        // => rationale: one task per kymograph image
		int nframes = lastKymo - firstKymo +1;
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detectlevel");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		tImg.setSpanDiff(options.spanDiffTop);
		tImg.setSequence(seqKymos);
		
		for (int indexKymo = firstKymo; indexKymo <= lastKymo; indexKymo++) 
		{
			final int t_index = indexKymo;
			final Capillary cap = exp.capillaries.capillariesArrayList.get(t_index);
			if (!options.detectR && cap.getCapillaryName().endsWith("2"))
				return false;
			if (!options.detectL && cap.getCapillaryName().endsWith("1"))
				return false;
			final String name = seqKymos.getFileName(t_index);
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{	
					final Capillary capi = cap;
					IcyBufferedImage rawImage = imageIORead(name);
					IcyBufferedImage sourceImage = tImg.transformImage (rawImage, options.transformForLevels);
					int c = 0;
					Object dataArray = sourceImage.getDataXY(c);
					int[] sourceValues = Array1DUtil.arrayToIntArray(dataArray, sourceImage.isSignedDataType());

					capi.indexImage= t_index;
					capi.ptsDerivative = null;
					capi.gulpsRois = null;
					capi.limitsOptions.copyFrom(options);
					
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
						capi.ptsTop = null;
						capi.ptsBottom = null;
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
						capi.ptsTop.polylineLimit.insertSeriesofYPoints(limitTop, firstColumn, lastColumn);
						capi.ptsBottom.polylineLimit.insertSeriesofYPoints(limitBottom, firstColumn, lastColumn);
					} 
					else 
					{
						capi.ptsTop    = new CapillaryLimit(capi.getLast2ofCapillaryName()+"_toplevel", t_index, limitTop);
						capi.ptsBottom = new CapillaryLimit(capi.getLast2ofCapillaryName()+"_bottomlevel", t_index, limitBottom);
					}

					exp.capillaries.xmlSaveCapillary_Measures(exp.getKymosBinFullDirectory(), capi);
//					System.out.println("save capillary "+ capi.roi.getName() + " at directory "+exp.getKymosBinFullDirectory());
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

