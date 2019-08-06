package plugins.fmp.multicafeTools;


import java.util.ArrayList;

import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.SequencePlus;

import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.nchenouard.kymographtracker.Util;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;

 
public class BuildKymographsThread implements Runnable 
{
	public BuildKymographsOptions 					options 			= new BuildKymographsOptions();
	
	public  SequencePlus 							vkymos 				= null;
	private ArrayList<double []> 					sourceValuesList 	= null;
	public boolean 									stopFlag 			= false;
	public boolean 									threadRunning 		= false;
	
	private Viewer 									sequenceViewer 		= null;
	private IcyBufferedImage 						workImage 			= null; 
	private Sequence 								seqForRegistration	= new Sequence();
	private DataType 								dataType 			= DataType.INT;
	private int 									imagewidth =1;
	
	@Override
	public void run() {

		if (options.vSequence == null || vkymos == null)
			return;
		System.out.println("start buildkymographsThreads");
		threadRunning = true;
		
		if (options.startFrame < 0) 
			options.startFrame = 0;
		if ((options.endFrame >= (int) options.vSequence.nTotalFrames) || (options.endFrame < 0)) 
			options.endFrame = (int) options.vSequence.nTotalFrames-1;
		int nbframes = options.endFrame - options.startFrame +1;
		ProgressChrono progressBar = new ProgressChrono("Processing started");
		progressBar.initStuff(nbframes);
		stopFlag = false;
		  
		initArraysToBuildKymographImages();
		
		int vinputSizeX = options.vSequence.seq.getSizeX();
		options.vSequence.seq.beginUpdate();
		sequenceViewer = Icy.getMainInterface().getFirstViewer(options.vSequence.seq);
		int ipixelcolumn = 0;
		getImageAndUpdateViewer (options.startFrame);
		
		seqForRegistration.addImage(0, workImage);
		seqForRegistration.addImage(1, workImage);
		int nbcapillaries = vkymos.capillaries.capillariesArrayList.size();
		
		for (int t = options.startFrame ; t <= options.endFrame && !stopFlag; t += options.analyzeStep, ipixelcolumn++ )
		{
			progressBar.updatePositionAndTimeLeft(t);
			if (!getImageAndUpdateViewer (t))
				continue;
			if (options.doRegistration ) {
				adjustImage();
			}
			transferWorkImageToDoubleArrayList ();
			
			for (int iroi=0; iroi < nbcapillaries; iroi++)
			{
				Capillary cap = vkymos.capillaries.capillariesArrayList.get(iroi);
				final int t_out = ipixelcolumn;

				for (int chan = 0; chan < options.vSequence.seq.getSizeC(); chan++) 
				{ 
					double [] tabValues = cap.tabValuesList.get(chan); 
					double [] sourceValues = sourceValuesList.get(chan);
					int cnt = 0;
					for (ArrayList<int[]> mask:cap.masksList)
					{
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
		options.vSequence.seq.endUpdate();
		vkymos.seq.removeAllImages();

		for (int iroi=0; iroi < nbcapillaries; iroi++)
		{
			Capillary cap = vkymos.capillaries.capillariesArrayList.get(iroi);
			for (int chan = 0; chan < vkymos.seq.getSizeC(); chan++) 
			{
				double [] tabValues = cap.tabValuesList.get(chan); 
				Array1DUtil.doubleArrayToSafeArray(tabValues, cap.bufImage.getDataXY(chan), cap.bufImage.isSignedDataType());
			}
			vkymos.seq.setImage(iroi, 0, cap.bufImage);
			
//			cap.masksList.clear();
//			cap.tabValuesList.clear();
			cap.bufImage.dataChanged();
		}
		
		System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
		progressBar.close();
		threadRunning = false;
	}
	
	// -------------------------------------------
	
	private boolean getImageAndUpdateViewer(int t) {
		
		workImage = options.vSequence.seq.getImage(t, 0, true); 
		sequenceViewer.setPositionT(t);
		sequenceViewer.setTitle(options.vSequence.getDecoratedImageName(t));		
		options.vSequence.currentFrame = t;

		if (workImage == null) {
			System.out.println("workImage null");
			return false;
		}
		return true;
	}
	
	private boolean transferWorkImageToDoubleArrayList() {
		
		sourceValuesList = new ArrayList<double []>();
		for (int chan = 0; chan < options.vSequence.seq.getSizeC(); chan++) 
		{
			double [] sourceValues = Array1DUtil.arrayToDoubleArray(workImage.getDataXY(chan), workImage.isSignedDataType()); 
			sourceValuesList.add(sourceValues);
		}
		return true;
	}
	
	private void initArraysToBuildKymographImages() {

		int sizex = options.vSequence.seq.getSizeX();
		int sizey = options.vSequence.seq.getSizeY();
		
		int numC = options.vSequence.seq.getSizeC();
		if (numC <= 0)
			numC = 3;
		double fimagewidth =  1 + (options.endFrame - options.startFrame )/options.analyzeStep;
		imagewidth = (int) fimagewidth;
		dataType = options.vSequence.seq.getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		options.vSequence.capillaries.extractLinesFromSequence(options.vSequence);
		vkymos.capillaries.getCopy(options.vSequence.capillaries);
		
		int nbcapillaries = vkymos.capillaries.capillariesArrayList.size();
		int masksizeMax = 0;
		for (int iroi=0; iroi < nbcapillaries; iroi++)
		{
			Capillary cap = vkymos.capillaries.capillariesArrayList.get(iroi);
			cap.indexImage = iroi;
			cap.masksList = new ArrayList<ArrayList<int[]>>();
			initExtractionParametersfromROI(cap.roi, cap.masksList, options.diskRadius, sizex, sizey);
			if (cap.masksList.size() > masksizeMax)
				masksizeMax = cap.masksList.size();
		}
		
		for (int iroi=0; iroi < nbcapillaries; iroi++)
		{
			Capillary cap = vkymos.capillaries.capillariesArrayList.get(iroi);
			cap.bufImage = new IcyBufferedImage(imagewidth, masksizeMax, numC, dataType);
			cap.tabValuesList = new ArrayList <double []>();
			for (int chan = 0; chan < numC; chan++) 
			{
				double[] tabValues =  Array1DUtil.arrayToDoubleArray(cap.bufImage.getDataXY(chan), false);
				cap.tabValuesList.add(tabValues);
			}
		} 
	}
	
	private double initExtractionParametersfromROI( ROI2DShape roi, ArrayList<ArrayList<int[]>> masks,  double diskRadius, int sizex, int sizey)
	{
		CubicSmoothingSpline xSpline 	= Util.getXsplineFromROI((ROI2DShape) roi);
		CubicSmoothingSpline ySpline 	= Util.getYsplineFromROI((ROI2DShape) roi);
		double length 					= Util.getSplineLength((ROI2DShape) roi);
		double len = 0;
		while (len < length)
		{
			ArrayList<int[]> mask = new ArrayList<int[]>();
			double x = xSpline.evaluate(len);
			double y = ySpline.evaluate(len);
			double dx = xSpline.derivative(len);
			double dy = ySpline.derivative(len);
			double ux = dy/Math.sqrt(dx*dx + dy*dy);
			double uy = -dx/Math.sqrt(dx*dx + dy*dy);
			double tt = -diskRadius;
			while (tt <= diskRadius)
			{
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