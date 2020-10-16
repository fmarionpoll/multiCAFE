package plugins.fmp.multicafe.tools.buildKymos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import loci.formats.FormatException;
import plugins.fmp.multicafe.sequence.Capillary;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.sequence.SequenceCamData;
import plugins.fmp.multicafe.sequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.nchenouard.kymographtracker.Util;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;

// see use of list of tasks in selectionfilter.java in plugin.adufour.filtering

public class BuildKymographs2_series extends SwingWorker<Integer, Integer>  {
	public BuildKymographs_Options 	options 			= new BuildKymographs_Options();
	public boolean 					stopFlag 			= false;
	public boolean 					threadRunning 		= false;
	public boolean					buildBackground		= true;

	//private IcyBufferedImage 		workImage 			= null; 
	private Sequence 				seqForRegistration	= null;
	private DataType 				dataType 			= DataType.INT;
	private int 					imagewidth 			= 1;
	private ArrayList<double []> 	sourceValuesList 	= null;
	private List<ROI> 				roiList 			= null;
	
	// ------------------------------
	// From A. Dufour Filter.java
    public static final Processor service = new Processor(SystemUtil.getNumberOfCPUs());
    
    static
    {
        service.setThreadName("buildkymo");
    }
    
//    public final VarBoolean stopFlag = new VarBoolean("stop", false);
//    
//    public final VarDouble  progress = new VarDouble("progression", 0.0);
    
	// ------------------------------
	@Override
	protected Integer doInBackground() throws Exception {
		System.out.println("start buildkymographsThread");
        threadRunning = true;
		int nbiterations = 0;
		ExperimentList expList = options.expList;
		ProgressFrame progress = new ProgressFrame("Build kymographs");
		long startTimeInNs = System.nanoTime();
			
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag)
				break;
			Experiment exp = expList.getExperiment(index);
			progress.setMessage("Processing file: " + (index +1) + "//" + (expList.index1+1));
			System.out.println((index+1)+": " +exp.getExperimentFileName());
			
			loadExperimentDataToBuildKymos(exp);
			exp.displaySequenceData(options.parent0Rect, exp.seqCamData.seq);
			exp.setKymoFrameStep (options.stepFrame);
			exp.resultsSubPath = options.resultsSubPath;
			exp.getResultsDirectory();
			
			if (options.isFrameFixed) {
				exp.setKymoFrameStart( options.startFrame);
				exp.setKymoFrameEnd (options.endFrame);
				if (exp.getKymoFrameEnd() > (exp.getSeqCamSizeT() - 1))
					exp.setKymoFrameEnd (exp.getSeqCamSizeT() - 1);
			} else {
				exp.setKymoFrameStart (0);
				exp.setKymoFrameEnd (exp.seqCamData.seq.getSizeT() - 1);
			}
			if (computeKymo(exp)) 
				if (expList.index0 != expList.index1)
					System.out.println(index+ " - "+ exp.getExperimentFileName() + " " + exp.resultsSubPath);			
				saveComputation(exp);
			exp.seqCamData.closeSequence();
		}
		long endTimeInNs = System.nanoTime();
		System.out.println("process ended - duration: "+((endTimeInNs-startTimeInNs)/ 1000000000f) + " s");
		progress.close();
		threadRunning = false;
		return nbiterations;
	}
	
	@Override
	protected void done() {
		int statusMsg = 0;
		try {
			statusMsg = get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} 
		if (!threadRunning || stopFlag) {
			firePropertyChange("thread_ended", null, statusMsg);
		} else {
			firePropertyChange("thread_done", null, statusMsg);
		}
    }
	
	private void loadExperimentDataToBuildKymos(Experiment exp) {
		exp.xmlLoadExperiment();
		exp.seqCamData.loadSequence(exp.getExperimentFileName()) ;
		exp.xmlLoadMCcapillaries_Only();
	}
			
	private void saveComputation(Experiment exp) {	
		if (options.doCreateResults_bin) {
			exp.resultsSubPath = exp.getResultsDirectoryNameFromKymoFrameStep();
		}
		String directory = exp.getResultsDirectory();
		if (directory == null)
			return;
		
		for (int t = 0; t < exp.seqKymos.seq.getSizeT(); t++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(t);
			String filename = directory + File.separator + cap.getCapillaryName() + ".tiff";
			File file = new File (filename);
			IcyBufferedImage image = exp.seqKymos.seq.getImage(t, 0);
			try {
				Saver.saveImage(image, file, true);
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		exp.xmlSaveExperiment();
	}
	
	private boolean computeKymo (Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqCamData == null || seqKymos == null)
			return false;
	
		threadRunning = true;
		stopFlag = false;

		initArraysToBuildKymographImages(exp);
		if (exp.capillaries.capillariesArrayList.size() < 1) {
			System.out.println("Abort (1): nbcapillaries = 0");
			return false;
		}
		
		int vinputSizeX = seqCamData.seq.getSizeX();		
		IcyBufferedImage  workImage0 = seqCamData.seq.getImage(options.startFrame, 0); 
		roiList = seqCamData.seq.getROIs();
		seqCamData.seq.removeAllROI();
		
		seqForRegistration	= new Sequence();
		seqForRegistration.addImage(0, workImage0);
		seqForRegistration.addImage(1, workImage0);
		int nbcapillaries = exp.capillaries.capillariesArrayList.size();
		if (nbcapillaries == 0) {
			System.out.println("Abort(2): nbcapillaries = 0");
			return false;
		}
		
		seqCamData.seq.beginUpdate();
		int nframes = (exp.getKymoFrameEnd() - exp.getKymoFrameStart()) / exp.getKymoFrameStep() +1;
		ArrayList<Future<?>> tasks = new ArrayList<Future<?>>(nframes);
		
		// clear the task array
		tasks.clear();
		
		int ipixelcolumn = 0;
		for (int t = exp.getKymoFrameStart() ; t <= exp.getKymoFrameEnd(); t += exp.getKymoFrameStep(), ipixelcolumn++ ) {
			if (stopFlag)
				break;
			
			final int t_from = t;
			final int t_out = ipixelcolumn;
//			final IcyBufferedImage  workImage = seqCamData.getImageCopy(t_from);
//			if (workImage != null) {
				tasks.add(service.submit(new Runnable () {
					@Override
					public void run() {		
						IcyBufferedImage  workImage = seqCamData.getImageCopy(t_from);
						if (workImage != null) {
							if (options.doRegistration ) 
								adjustImage(workImage);
							transferWorkImageToDoubleArrayList (workImage);
							for (int iroi=0; iroi < nbcapillaries; iroi++) {
								Capillary cap = exp.capillaries.capillariesArrayList.get(iroi);
								for (int chan = 0; chan < seqCamData.seq.getSizeC(); chan++) { 
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
					}
				}));
//			}
		}
		
        // wait until kymo is built
        waitCompletion(tasks);
	
		seqCamData.seq.endUpdate();
		seqKymos.seq.removeAllImages();
		seqKymos.seq.setVirtual(false); 
		seqCamData.seq.addROIs(roiList, false);

		for (int icap=0; icap < nbcapillaries; icap++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(icap);
			for (int chan = 0; chan < seqCamData.seq.getSizeC(); chan++) {
				double [] tabValues = cap.tabValuesList.get(chan); 
				Object destArray = cap.bufImage.getDataXY(chan);
				Array1DUtil.doubleArrayToSafeArray(tabValues, destArray, cap.bufImage.isSignedDataType());
				cap.bufImage.setDataXY(chan, destArray);
			}
			seqKymos.seq.setImage(icap, 0, cap.bufImage);
			cap.masksList.clear();
			cap.tabValuesList.clear();
			cap.bufImage = null;
		}
		seqKymos.seq.setName(exp.getDecoratedImageNameFromCapillary(0));

		return true;
	}
	
    private void waitCompletion(List<Future<?>> futures)
    {
        while (!futures.isEmpty())
        {
            // get last in queue
            final Future<?> f = futures.get(futures.size() - 1);

            try
            {
                // wait for it
                f.get();
            }
            catch (ExecutionException e)
            {
                // warning
                System.out.println("KymoBuilder - Warning: " + e);
            }
            catch (InterruptedException e)
            {
                // ignore
            }

            // remove it
            futures.remove(f);
        }
    }
	
	// -------------------------------------------
	
	private boolean transferWorkImageToDoubleArrayList(IcyBufferedImage  workImage) {	
		sourceValuesList = new ArrayList<double []>();
		for (int chan = 0; chan < workImage.getSizeC(); chan++)  {
			double [] sourceValues = Array1DUtil.arrayToDoubleArray(workImage.getDataXY(chan), workImage.isSignedDataType()); 
			sourceValuesList.add(sourceValues);
		}
		return true;
	}
	
	private void initArraysToBuildKymographImages(Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		int sizex = seqCamData.seq.getSizeX();
		int sizey = seqCamData.seq.getSizeY();	
		int numC = seqCamData.seq.getSizeC();
		if (numC <= 0)
			numC = 3;
		double fimagewidth =  1 + (exp.getKymoFrameEnd() - exp.getKymoFrameStart() )/options.stepFrame;
		if (fimagewidth < 0) {
			options.stepFrame = 1;
			exp.setKymoFrameStep (options.stepFrame);
			fimagewidth =  1 + (exp.getKymoFrameEnd() - exp.getKymoFrameStart() )/options.stepFrame;
		}
			
		imagewidth = (int) fimagewidth;
		dataType = seqCamData.seq.getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int nbcapillaries = exp.capillaries.capillariesArrayList.size();
		int masksizeMax = 0;
		for (int t=0; t < nbcapillaries; t++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(t);
			cap.masksList = new ArrayList<ArrayList<int[]>>();
			initExtractionParametersfromROI(cap.roi, cap.masksList, options.diskRadius, sizex, sizey);
			if (cap.masksList.size() > masksizeMax)
				masksizeMax = cap.masksList.size();
		}
		
		for (int t=0; t < nbcapillaries; t++) {
			Capillary cap = exp.capillaries.capillariesArrayList.get(t);
			cap.bufImage = new IcyBufferedImage(imagewidth, masksizeMax, numC, dataType);
			cap.tabValuesList = new ArrayList <double []>();
			for (int chan = 0; chan < numC; chan++) {
				Object dataArray = cap.bufImage.getDataXY(chan);
				double[] tabValues =  Array1DUtil.arrayToDoubleArray(dataArray, false);
				cap.tabValuesList.add(tabValues);
			}
		} 
	}
	
	private double initExtractionParametersfromROI( ROI2DShape roi, List<ArrayList<int[]>> masks,  double diskRadius, int sizex, int sizey) {
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
		
	private void adjustImage(IcyBufferedImage  workImage) {
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
