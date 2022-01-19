package plugins.fmp.multicafe2.series;

import java.util.ArrayList;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;



public class DetectLevels02  extends BuildSeries  
{
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

		final int jitter = 10;
		final String directory = exp.getKymosBinFullDirectory();
		final ImageTransformInterface transformPass1 = options.transform01.getFunction();
		final ImageTransformInterface transformPass2 = options.transform02.getFunction();
		
		for (int index = firstKymo; index <= lastKymo; index++) 
		{
			final int indexCapillary = index;
			final Capillary capi = exp.capillaries.capillariesList.get(indexCapillary);
			if (!options.detectR && capi.getKymographName().endsWith("2"))
				continue;
			if (!options.detectL && capi.getKymographName().endsWith("1"))
				continue;
				
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{	
					final IcyBufferedImage rawImage = imageIORead(seqKymos.getFileName(indexCapillary));
					
					capi.indexKymograph = indexCapillary;
					capi.ptsDerivative = null;
					capi.gulpsRois = null;
					capi.limitsOptions.copyFrom(options);
					
					int firstColumn = 0;
					int lastColumn = rawImage.getSizeX()-1;
					int imageWidth = rawImage.getSizeX();
					int imageHeight = rawImage.getSizeY();
					if (options.analyzePartOnly) 
					{
						firstColumn = options.firstPixel;
						lastColumn = options.lastPixel;
						if (lastColumn > imageWidth-1)
							lastColumn = imageWidth -1;
					} 
					
					if (options.pass1) 
					{		
						int c = 0;
						IcyBufferedImage transformedImage1 = transformPass1.run (rawImage, null);
						Object transformedArray1 = transformedImage1.getDataXY(c);
						int[] transformed1DArray1 = Array1DUtil.arrayToIntArray(transformedArray1, transformedImage1.isSignedDataType());
						
						int topSearchFrom = 0;
						capi.ptsTop.limit = new int [lastColumn - firstColumn +1];
						capi.ptsBottom.limit = new int [lastColumn - firstColumn +1];
						
						for (int ix = firstColumn; ix <= lastColumn; ix++) 
						{
							int iyTop = detectThresholdFromTop(ix, topSearchFrom, jitter, transformed1DArray1, imageWidth, imageHeight, options);
							int iyBottom = detectThresholdFromBottom(ix, jitter, transformed1DArray1, imageWidth, imageHeight, options);
							if (iyBottom <= iyTop) 
								iyTop = topSearchFrom;
							capi.ptsTop.limit[ix] = iyTop;
							capi.ptsBottom.limit[ix] = iyBottom;
							topSearchFrom = iyTop;
						}
					}
					
					if (options.pass2) 
					{
						if (capi.ptsTop.limit == null)
							capi.ptsTop.setTempDataFromPolylineLevel();

						int c = 0;
						IcyBufferedImage transformedImage2 = transformPass2.run (rawImage, null);		
						Object transformedArray2 = transformedImage2.getDataXY(c);
						int[] transformed1DArray2 = Array1DUtil.arrayToIntArray(transformedArray2, transformedImage2.isSignedDataType());
						switch (options.transform02)
						{
							case COLORDISTANCE_L1_Y:
							case COLORDISTANCE_L2_Y:
								findBestPosition(capi.ptsTop.limit, firstColumn, lastColumn, transformed1DArray2, imageWidth, imageHeight, 5);
								break;
								
							case SUBTRACT_1RSTCOL:
							case L1DIST_TO_1RSTCOL:
								detectThresholdUp(capi.ptsTop.limit, firstColumn, lastColumn, transformed1DArray2, imageWidth, imageHeight, 20, options.detectLevel2Threshold);
								break;
								
							case DERICHE:
								findBestPosition(capi.ptsTop.limit, firstColumn, lastColumn, transformed1DArray2, imageWidth, imageHeight, 5);
								break;
								
							default:
								break;
						}
					}

					if (options.analyzePartOnly) 
					{
						capi.ptsTop.polylineLevel.insertYPoints(capi.ptsTop.limit, firstColumn, lastColumn);
						if (capi.ptsBottom.limit != null)
							capi.ptsBottom.polylineLevel.insertYPoints(capi.ptsBottom.limit, firstColumn, lastColumn);
					} else {
						capi.ptsTop.setPolylineLevelFromTempData(capi.getLast2ofCapillaryName()+"_toplevel", firstColumn, lastColumn);
						if (capi.ptsBottom.limit != null)
							capi.ptsBottom.setPolylineLevelFromTempData(capi.getLast2ofCapillaryName()+"_bottomlevel", firstColumn, lastColumn);
					}
					capi.ptsTop.limit = null;
					capi.ptsBottom.limit = null;
					
					capi.xmlSaveCapillary_Measures(directory);
				}}));
		}
		waitFuturesCompletion(processor, futures, progressBar);
		seqKymos.seq.endUpdate();
		
		progressBar.close();
		
		return true;
	}
	
	private void findBestPosition(int [] limits, int firstColumn, int lastColumn, int[] transformed1DArray2, int imageWidth, int imageHeight, int delta) 
	{
		for (int ix = firstColumn; ix <= lastColumn; ix++) 
		{
			int iy = limits[ix];
			int maxVal = transformed1DArray2[ix + iy * imageWidth];
			int iyVal = iy;
			for (int irow = iy + delta; irow > iy - delta; irow--) 
			{
				if (irow < 0 || irow >= imageHeight)
					continue;
				
				int val = transformed1DArray2[ix + irow * imageWidth];
				if (val > maxVal) 
				{
					maxVal = val;
					iyVal = irow;
				}
			}
			limits[ix] = iyVal;
		}
	}
	
	private void detectThresholdUp(int [] limits, int firstColumn, int lastColumn, int[] transformed1DArray2, int imageWidth, int imageHeight, int delta, int threshold) 
	{
		for (int ix = firstColumn; ix <= lastColumn; ix++) 
		{
			int iy = limits[ix];
			int iyVal = iy;
			for (int irow = iy + delta; irow > iy - delta; irow--) 
			{
				if (irow < 0 || irow >= imageHeight)
					continue;
				
				int val = transformed1DArray2[ix + irow * imageWidth];
				if (val > threshold) 
				{
					iyVal = irow;
					break;
				}
			}
			limits[ix] = iyVal;
		}
	}
	
	private int checkIndexLimits (int rowIndex, int maximumRowIndex) 
	{
		if (rowIndex < 0)
			rowIndex = 0;
		if (rowIndex > maximumRowIndex)
			rowIndex = maximumRowIndex;
		return rowIndex;
	}

	private int detectThresholdFromTop(int ix, int searchFrom, int jitter, int [] tabValues, int imageWidth, int imageHeight, BuildSeriesOptions options) 
	{
		int y = imageHeight-1;
		searchFrom = checkIndexLimits(searchFrom - jitter, imageHeight-1);
		for (int iy = searchFrom; iy < imageHeight; iy++) 
		{
			boolean flag = false;
			if (options.directionUp1)
				flag = tabValues [ix + iy* imageWidth] > options.detectLevel1Threshold;
			else 
				flag = tabValues [ix + iy* imageWidth] < options.detectLevel1Threshold;
			if (flag) 
			{
				y = iy;
				break;
			}
		}
		return y;
	}
	
	private int detectThresholdFromBottom(int ix, int jitter, int[] tabValues, int imageWidth, int imageHeight, BuildSeriesOptions options) 
	{
		int y = 0;
		for (int iy = imageHeight - 1; iy >= 0 ; iy--) 
		{
			boolean flag = false;
			if (options.directionUp1)
				flag = tabValues [ix + iy* imageWidth] > options.detectLevel1Threshold;
			else 
				flag = tabValues [ix + iy* imageWidth] < options.detectLevel1Threshold;
			if (flag) 
			{
				y = iy;
				break;
			}
		}
		return y;
	}
	
}
