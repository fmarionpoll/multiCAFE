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
import plugins.fmp.multicafe2.experiment.CapillaryLevel;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.tools.ImageToolsTransform;



public class DetectLevels_series extends BuildSeries  
{
	ImageToolsTransform tImg = new ImageToolsTransform();
	
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
		
		int nframes = lastKymo - firstKymo +1;
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detectlevel");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		tImg.setSpanDiff(options.spanDiffTop);
		tImg.setSequence(seqKymos);
		final int jitter = 10;
		
		for (int index = firstKymo; index <= lastKymo; index++) 
		{
			final int t_index = index;
			final Capillary cap_index = exp.capillaries.capillariesList.get(t_index);
			if (!options.detectR && cap_index.getKymographName().endsWith("2"))
				continue;
			if (!options.detectL && cap_index.getKymographName().endsWith("1"))
				continue;
				
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{	
					IcyBufferedImage rawImage = imageIORead(seqKymos.getFileName(t_index));
					IcyBufferedImage transformedImage1 = tImg.transformImage (rawImage, options.transform1);
//					IcyBufferedImage transformedImage2 = tImg.transformImage (rawImage, options.transform2);		
					int c = 0;
					Object transformedArray1 = transformedImage1.getDataXY(c);
					int[] transformed1DArray1 = Array1DUtil.arrayToIntArray(transformedArray1, transformedImage1.isSignedDataType());

					cap_index.indexKymograph = t_index;
					cap_index.ptsDerivative = null;
					cap_index.gulpsRois = null;
					cap_index.limitsOptions.copyFrom(options);
					
					int firstColumn = 0;
					int lastColumn = transformedImage1.getSizeX()-1;
					int imageWidth = transformedImage1.getSizeX();
					int imageHeight = transformedImage1.getSizeY();
					if (options.analyzePartOnly) 
					{
						firstColumn = options.firstPixel;
						lastColumn = options.lastPixel;
						if (lastColumn > imageWidth-1)
							lastColumn = imageWidth -1;
					} 
					else 
					{
						cap_index.ptsTop = null;
						cap_index.ptsBottom = null;
					}
					
					int topSearchFrom = 0;
					int nColumns = lastColumn - firstColumn +1;

					List<Point2D> limitTop = new ArrayList<Point2D>(nColumns);
					List<Point2D> limitBottom = new ArrayList<Point2D>(nColumns);
		
					// scan each image column
					for (int iColumn = firstColumn; iColumn <= lastColumn; iColumn++) 
					{
						int ytop = detectThresholdFromTop(iColumn, topSearchFrom, jitter, transformed1DArray1, imageWidth, imageHeight, options);
						int ybottom = detectThresholdFromBottom(iColumn, jitter, transformed1DArray1, imageWidth, imageHeight, options);
						if (ybottom <= ytop) 
						{
							ytop = topSearchFrom;
						}
						
						limitTop.add(new Point2D.Double(iColumn, ytop));
						limitBottom.add(new Point2D.Double(iColumn, ybottom));
						topSearchFrom = ytop;
					}	
					
					if (options.analyzePartOnly) 
					{
						cap_index.ptsTop.polylineLevel.insertSeriesofYPoints(limitTop, firstColumn, lastColumn);
						cap_index.ptsBottom.polylineLevel.insertSeriesofYPoints(limitBottom, firstColumn, lastColumn);
					} 
					else 
					{
						cap_index.ptsTop    = new CapillaryLevel(cap_index.getLast2ofCapillaryName()+"_toplevel", t_index, limitTop);
						cap_index.ptsBottom = new CapillaryLevel(cap_index.getLast2ofCapillaryName()+"_bottomlevel", t_index, limitBottom);
					}
					exp.capillaries.xmlSaveCapillary_Measures(exp.getKymosBinFullDirectory(), cap_index);
				}}));
		}
		waitFuturesCompletion(processor, futures, progressBar);
		seqKymos.seq.endUpdate();
		
		progressBar.close();
		
		return true;
	}

	private int checkIndexLimits (int rowIndex, int maximumRowIndex) 
	{
		if (rowIndex < 0)
			rowIndex = 0;
		if (rowIndex > maximumRowIndex)
			rowIndex = maximumRowIndex;
		return rowIndex;
	}

	private int detectThresholdFromTop(int ix, int searchFrom, int jitter, int [] tabValues, int imageWidth, int imageHeight, Options_BuildSeries options) 
	{
		int y = imageHeight-1;
		searchFrom = checkIndexLimits(searchFrom - jitter, imageHeight-1);
		for (int iy = searchFrom; iy < imageHeight; iy++) 
		{
			boolean flag = false;
			if (options.directionUp)
				flag = tabValues [ix + iy* imageWidth] > options.detectLevelThreshold;
			else 
				flag = tabValues [ix + iy* imageWidth] < options.detectLevelThreshold;
			if (flag) 
			{
				y = iy;
				break;
			}
		}
		return y;
	}
	
	private int detectThresholdFromBottom(int ix, int jitter, int[] tabValues, int imageWidth, int imageHeight, Options_BuildSeries options) 
	{
		int y = 0;
		for (int iy = imageHeight - 1; iy >= 0 ; iy--) 
		{
			boolean flag = false;
			if (options.directionUp)
				flag = tabValues [ix + iy* imageWidth] > options.detectLevelThreshold;
			else 
				flag = tabValues [ix + iy* imageWidth] < options.detectLevelThreshold;
			if (flag) 
			{
				y = iy;
				break;
			}
		}
		return y;
	}
	
}

