package plugins.fmp.multicafeTools;


import java.util.ArrayList;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;

import plugins.fmp.multicafeSequence.Capillary;

import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.nchenouard.kymographtracker.Util;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;

 
public class BuildKymographs implements Runnable 
{
	public BuildKymographs_Options 	options 			= new BuildKymographs_Options();
	public boolean 					stopFlag 			= false;
	public boolean 					threadRunning 		= false;
	
	private IcyBufferedImage 		workImage 			= null; 
	private Sequence 				seqForRegistration	= new Sequence();
	private DataType 				dataType 			= DataType.INT;
	private int 					imagewidth =1;
	private ArrayList<double []> 	sourceValuesList 	= null;
	private List<ROI> 				roiList 			= null;
	
	
	@Override
	public void run() {

		if (options.seqCamData == null || options.seqKymos == null)
			return;
		System.out.println("start buildkymographsThreads");
		
		if (options.startFrame < 0) 
			options.startFrame = 0;
		if ((options.endFrame >= (int) options.seqCamData.nTotalFrames) || (options.endFrame < 0)) 
			options.endFrame = (int) options.seqCamData.nTotalFrames-1;
		if (options.seqCamData.bufferThread == null) {	
			options.seqCamData.prefetchForwardThread_START(200); 
		}
		
		int nbframes = options.endFrame - options.startFrame +1;
		ProgressChrono progressBar = new ProgressChrono("Processing started");
		progressBar.initStuff(nbframes);
		threadRunning = true;
		stopFlag = false;
		  
		initArraysToBuildKymographImages();
		
		int vinputSizeX = options.seqCamData.seq.getSizeX();		
		int ipixelcolumn = 0;
		workImage = options.seqCamData.seq.getImage(options.startFrame, 0); 
		Thread thread = null;
		BuildVisuUpdater visuUpdater = null;
		if (options.updateViewerDuringComputation) {
			roiList = options.seqCamData.seq.getROIs();
			options.seqCamData.seq.removeAllROI();
			visuUpdater = new BuildVisuUpdater(options.seqCamData);
			thread = new Thread(null, visuUpdater, "visuUpdater Thread");
			thread.start();
		}
		
		seqForRegistration.addImage(0, workImage);
		seqForRegistration.addImage(1, workImage);
		int nbcapillaries = options.seqKymos.capillaries.capillariesArrayList.size();
		if (nbcapillaries == 0)
			return;
		
		options.seqCamData.seq.beginUpdate();
		for (int t = options.startFrame ; t <= options.endFrame && !stopFlag; t += options.analyzeStep, ipixelcolumn++ ) {
			progressBar.updatePositionAndTimeLeft(t);
			if (!getImageAndUpdateViewer (t))
				continue;
			if (options.doRegistration ) 
				adjustImage();
			transferWorkImageToDoubleArrayList ();
			
			for (int iroi=0; iroi < nbcapillaries; iroi++) {
				Capillary cap = options.seqKymos.capillaries.capillariesArrayList.get(iroi);
				final int t_out = ipixelcolumn;
				for (int chan = 0; chan < options.seqCamData.seq.getSizeC(); chan++) { 
					double [] tabValues = cap.tabValuesList.get(chan); 
					double [] sourceValues = sourceValuesList.get(chan);
					int cnt = 0;
					for (ArrayList<int[]> mask:cap.masksList) {
						double sum = 0;
						for (int[] m:mask)
							sum += sourceValues[m[0] + m[1]*vinputSizeX];
						if (mask.size() > 1)
							sum = sum/mask.size();
						tabValues[cnt*imagewidth + t_out] = sum; 
						cnt ++;
					}
				}
			}
		}
		options.seqCamData.seq.endUpdate();
		options.seqKymos.seq.removeAllImages();
		options.seqKymos.seq.setVirtual(false); 
		if (options.updateViewerDuringComputation) {
			if (thread != null && thread.isAlive()) {
				visuUpdater.isInterrupted = true;
				try {
					thread.join();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			options.seqCamData.seq.addROIs(roiList, false);
		}

		for (int t=0; t < nbcapillaries; t++) {
			Capillary cap = options.seqKymos.capillaries.capillariesArrayList.get(t);
			for (int chan = 0; chan < options.seqCamData.seq.getSizeC(); chan++) {
				double [] tabValues = cap.tabValuesList.get(chan); 
				Object destArray = cap.bufImage.getDataXY(chan);
				Array1DUtil.doubleArrayToSafeArray(tabValues, destArray, cap.bufImage.isSignedDataType());
				cap.bufImage.setDataXY(chan, destArray);
			}
			options.seqKymos.seq.setImage(t, 0, cap.bufImage);
			
			cap.masksList.clear();
			cap.tabValuesList.clear();
			cap.bufImage = null;
		}
		options.seqKymos.seq.setName(options.seqKymos.getDecoratedImageNameFromCapillary(0));
		
		System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
		progressBar.close();
		
		options.seqCamData.prefetchForwardThread_STOP(); //RESTART(); 
		threadRunning = false;
	}
	
	// -------------------------------------------
	
	private boolean getImageAndUpdateViewer(int t) {	
		workImage = IcyBufferedImageUtil.getCopy(options.seqCamData.getImageFromForwardBuffer(t));
		if (workImage == null) {
			System.out.println("workImage null");
			return false;
		}
		return true;
	}
	
	private boolean transferWorkImageToDoubleArrayList() {	
		sourceValuesList = new ArrayList<double []>();
		for (int chan = 0; chan < options.seqCamData.seq.getSizeC(); chan++)  {
			double [] sourceValues = Array1DUtil.arrayToDoubleArray(workImage.getDataXY(chan), workImage.isSignedDataType()); 
			sourceValuesList.add(sourceValues);
		}
		return true;
	}
	
	private void initArraysToBuildKymographImages() {

		int sizex = options.seqCamData.seq.getSizeX();
		int sizey = options.seqCamData.seq.getSizeY();	
		int numC = options.seqCamData.seq.getSizeC();
		if (numC <= 0)
			numC = 3;
		double fimagewidth =  1 + (options.endFrame - options.startFrame )/options.analyzeStep;
		imagewidth = (int) fimagewidth;
		dataType = options.seqCamData.seq.getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int nbcapillaries = options.seqKymos.capillaries.capillariesArrayList.size();
		int masksizeMax = 0;
		for (int t=0; t < nbcapillaries; t++) {
			Capillary cap = options.seqKymos.capillaries.capillariesArrayList.get(t);
			cap.masksList = new ArrayList<ArrayList<int[]>>();
			initExtractionParametersfromROI(cap.capillaryRoi, cap.masksList, options.diskRadius, sizex, sizey);
			if (cap.masksList.size() > masksizeMax)
				masksizeMax = cap.masksList.size();
		}
		
		for (int t=0; t < nbcapillaries; t++) {
			Capillary cap = options.seqKymos.capillaries.capillariesArrayList.get(t);
			cap.bufImage = new IcyBufferedImage(imagewidth, masksizeMax, numC, dataType);
			cap.tabValuesList = new ArrayList <double []>();
			for (int chan = 0; chan < numC; chan++) {
				Object dataArray = cap.bufImage.getDataXY(chan);
				double[] tabValues =  Array1DUtil.arrayToDoubleArray(dataArray, false);
				cap.tabValuesList.add(tabValues);
			}
		} 
	}
	
	private double initExtractionParametersfromROI( ROI2DShape roi, List<ArrayList<int[]>> masks,  double diskRadius, int sizex, int sizey)
	{
		CubicSmoothingSpline xSpline 	= Util.getXsplineFromROI((ROI2DShape) roi);
		CubicSmoothingSpline ySpline 	= Util.getYsplineFromROI((ROI2DShape) roi);
		double length 					= Util.getSplineLength((ROI2DShape) roi);
		double len = 0;
		while (len < length) {
			ArrayList<int[]> mask = new ArrayList<int[]>();
			double x = xSpline.evaluate(len);
			double y = ySpline.evaluate(len);
			double dx = xSpline.derivative(len);
			double dy = ySpline.derivative(len);
			double ux = dy/Math.sqrt(dx*dx + dy*dy);
			double uy = -dx/Math.sqrt(dx*dx + dy*dy);
			double tt = -diskRadius;
			while (tt <= diskRadius) {
				int xx = (int) Math.round(x + tt*ux);
				int yy = (int) Math.round(y + tt*uy);
				if (xx >= 0 && xx < sizex && yy >= 0 && yy < sizey)
					mask.add(new int[]{xx, yy});
				tt += 1d;
			}
			masks.add(mask);			
			len ++;
		}
		return length;
	}
		
	private void adjustImage() {
		seqForRegistration.setImage(1, 0, workImage);
		int referenceChannel = 1;
		int referenceSlice = 0;
		DufourRigidRegistration.correctTemporalTranslation2D(seqForRegistration, referenceChannel, referenceSlice);
        boolean rotate = DufourRigidRegistration.correctTemporalRotation2D(seqForRegistration, referenceChannel, referenceSlice);
        if (rotate) 
        	DufourRigidRegistration.correctTemporalTranslation2D(seqForRegistration, referenceChannel, referenceSlice);
        workImage = seqForRegistration.getLastImage(1);
	}

}