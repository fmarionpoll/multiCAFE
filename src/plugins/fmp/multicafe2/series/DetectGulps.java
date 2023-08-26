package plugins.fmp.multicafe2.series;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.CapillaryLevel;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;



public class DetectGulps extends BuildSeries  
{
	
	void analyzeExperiment(Experiment exp) 
	{
		if (loadExperimentDataToDetectGulps(exp)) 
		{
			buildFilteredImage(exp);
			detectGulpsFromExperiment(exp);
		}
		exp.seqKymos.closeSequence();
	}
	
	private boolean loadExperimentDataToDetectGulps(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		boolean flag = exp.xmlLoadMCCapillaries_Only();
		flag &= exp.loadKymographs();
		flag &= exp.capillaries.xmlLoadCapillaries_Measures(exp.getKymosBinFullDirectory());
		return flag;
	}
	
	private void buildFilteredImage(Experiment exp) 
	{
		if (exp.seqKymos == null)
			return;
		int zChannelDestination = 2;
		exp.kymosBuildFiltered01(0, zChannelDestination, options.transformForGulps, options.spanDiff);
	}
	
	public void detectGulpsFromExperiment(Experiment exp) 
	{			
		SequenceKymos seqCapillariesKymographs = exp.seqKymos;	
		int jitter = 5;
		int firstCapillary = 0;
		int lastCapillary = seqCapillariesKymographs.seq.getSizeT() -1;
		if (!options.detectAllGulps) {
			firstCapillary = options.kymoFirst;
			lastCapillary = firstCapillary;
		}
		seqCapillariesKymographs.seq.beginUpdate();
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		
		int nframes = lastCapillary - firstCapillary +1;
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("detect_levels");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();
		
		final String directory = exp.getKymosBinFullDirectory();
		
		for (int indexCapillary = firstCapillary; indexCapillary <= lastCapillary; indexCapillary++) 
		{
			final Capillary capi = exp.capillaries.capillariesList.get(indexCapillary);
			capi.setGulpsOptions(options);
			// TODO save date in poly2D instead in rois
			final int icap = indexCapillary;
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{
					if (options.buildDerivative) 
					{
						ROI2DUtilities.removeRoisContainingString(icap, "derivative", seqCapillariesKymographs.seq);
						transformPointsIntoProfile( getDerivativeProfile(seqCapillariesKymographs.seq, icap, capi, jitter), icap, capi);	
					}
					if (options.buildGulps) 
					{
						capi.cleanGulps();
						ROI2DUtilities.removeRoisContainingString(icap, "gulp", seqCapillariesKymographs.seq);
						capi.detectGulps(icap);
						if (capi.gulpsRois.rois.size() > 0) 
						{
							seqCapillariesKymographs.seq.addROIs(capi.gulpsRois.rois, false);
//							System.out.println(capi.getRoiName() + "- save n rois:" + capi.gulpsRois.rois.size());
						}
					}
					capi.xmlSaveCapillary_Measures(directory);
				}}));
		}
		
		waitFuturesCompletion(processor, futures, progressBar);
		seqCapillariesKymographs.seq.endUpdate();
		progressBar.close();
		processor.shutdown();
	}	

	private List<Point2D> getDerivativeProfile(Sequence seq, int indexkymo, Capillary cap, int jitter) 
	{	
		Polyline2D 	polyline = cap.ptsTop.polylineLevel;
		if (polyline == null)
			return null;
		
		int z = seq.getSizeZ() -1;
		int c = 0;
		IcyBufferedImage image = seq.getImage(indexkymo, z, c);
		List<Point2D> listOfMaxPoints = new ArrayList<>();
		int[] kymoImageValues = Array1DUtil.arrayToIntArray(image.getDataXY(c), image.isSignedDataType());	
		int xwidth = image.getSizeX();
		int yheight = image.getSizeY();

		for (int ix = 1; ix < polyline.npoints; ix++) 
		{
			// for each point of topLevelArray, define a bracket of rows to look at ("jitter" = 10)
			int low = (int) polyline.ypoints[ix]- jitter;
			int high = low + 2*jitter;
			if (low < 0) 
				low = 0;
			if (high >= yheight) 
				high = yheight-1;
			int max = kymoImageValues [ix + low*xwidth];
			for (int iy = low + 1; iy < high; iy++) 
			{
				int val = kymoImageValues [ix  + iy*xwidth];
				if (max < val) 
					max = val;
			}
			listOfMaxPoints.add(new Point2D.Double((double) ix, (double) max));
		}
		return listOfMaxPoints;
	}
	
	private void transformPointsIntoProfile(List<Point2D> listOfMaxPoints, int indexkymo, Capillary cap) 
	{
		ROI2DPolyLine roiDerivative = new ROI2DPolyLine ();
		roiDerivative.setName(cap.getLast2ofCapillaryName()+"_derivative");
		roiDerivative.setColor(Color.yellow);
		roiDerivative.setStroke(1);
		roiDerivative.setPoints(listOfMaxPoints);
		roiDerivative.setT(indexkymo);
//		seq.addROI(roiDerivative, false);
		cap.ptsDerivative = new CapillaryLevel(roiDerivative.getName(), roiDerivative.getPolyline2D());
	}
	
}


