package plugins.fmp.multicafe.series;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import loci.formats.FormatException;
import plugins.fmp.multicafe.experiment.Capillary;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.SequenceKymos;
import plugins.fmp.multicafe.tools.Bresenham;
import plugins.kernel.roi.roi2d.ROI2DShape;



public class BuildKymographs_series  extends BuildSeries  
{
	void analyzeExperiment(Experiment exp) {
		loadExperimentDataToBuildKymos(exp);
		exp.seqCamData.displayViewerAtRectangle(options.parent0Rect);
		exp.kymoBinCol_Ms = options.t_binMs;
		if (options.isFrameFixed) 
		{
			exp.kymoFirstCol_Ms = options.t_firstMs;
			exp.kymoLastCol_Ms = options.t_lastMs;
			if (exp.kymoLastCol_Ms > exp.camLastImage_Ms)
				exp.kymoLastCol_Ms = exp.camLastImage_Ms;
		} 
		else 
		{
			exp.kymoFirstCol_Ms = exp.camFirstImage_Ms;
			exp.kymoLastCol_Ms = exp.camLastImage_Ms;
		}
		if (buildKymo(exp)) 
			saveComputation(exp);
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
	
	private void loadExperimentDataToBuildKymos(Experiment exp) 
	{
//		exp.openSequenceCamData();
		//exp.loadCamDataImages();
		exp.xmlLoadMCCapillaries_Only();
		
	}
			
	private void saveComputation(Experiment exp) 
	{	
		if (options.doCreateBinDir) 
			exp.setBinSubDirectory (exp.getBinNameFromKymoFrameStep());
		String directory = exp.getDirectoryToSaveResults(); 
		if (directory == null)
			return;
		for (int t = 0; t < exp.seqKymos.seq.getSizeT(); t++) 
		{
			Capillary cap = exp.capillaries.capillariesArrayList.get(t);
			String filename = directory + File.separator + cap.getCapillaryName() + ".tiff";
			File file = new File (filename);
			IcyBufferedImage image = exp.seqKymos.seq.getImage(t, 0);
			try 
			{
				Saver.saveImage(image, file, true);
			} 
			catch (FormatException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		exp.xmlSaveMCExperiment();
	}
	
	private boolean buildKymo (Experiment exp) 
	{
		SequenceCamData seqCamData = exp.seqCamData;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqCamData == null || seqKymos == null)
			return false;
	
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		
		initArraysToBuildKymographImages(exp);
		if (exp.capillaries.capillariesArrayList.size() < 1) 
		{
			System.out.println("Abort (1): nbcapillaries = 0");
			return false;
		}
		
		int startFrame = (int) ((exp.kymoFirstCol_Ms - exp.camFirstImage_Ms)/exp.camBinImage_Ms);
		IcyBufferedImage sourceImage0 = seqCamData.seq.getImage(startFrame, 0); 
		seqCamData.seq.removeAllROI();
		
		final Sequence seqForRegistration = new Sequence();
		if (options.doRegistration) 
		{
			seqForRegistration.addImage(0, sourceImage0);
			seqForRegistration.addImage(1, sourceImage0);
		}
		exp.seqKymos.seq = new Sequence();
		
		int nbcapillaries = exp.capillaries.capillariesArrayList.size();
		if (nbcapillaries == 0) 
		{
			System.out.println("Abort(2): nbcapillaries = 0");
			return false;
		}
		
		int nframes = (int) ((exp.kymoLastCol_Ms - exp.kymoFirstCol_Ms) / exp.kymoBinCol_Ms +1);
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("buildkymo2");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(nframes);
		futures.clear();

		seqCamData.seq.beginUpdate();
		int ipixelcolumn = 0;
		for (long indexms = exp.kymoFirstCol_Ms ; indexms <= exp.kymoLastCol_Ms; indexms += exp.kymoBinCol_Ms, ipixelcolumn++ ) 
		{
			final int t_from = (int) ((indexms - exp.camFirstImage_Ms)/exp.camBinImage_Ms);
			final int t_out = ipixelcolumn;
			futures.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{	
					final IcyBufferedImage  sourceImage = seqCamData.imageIORead(t_from);
					if (options.doRegistration ) 
						adjustImage(seqForRegistration, sourceImage);
					int widthSourceImage = sourceImage.getWidth();				
					ArrayList<int []> sourceImageInteger = transferImageToIntegerArrayList (sourceImage);
					for (int icap=0; icap < nbcapillaries; icap++) 
					{
						Capillary cap = exp.capillaries.capillariesArrayList.get(icap);
						int widthKymoImage = cap.bufKymoImage.getWidth();
						for (int channel = 0; channel < seqCamData.seq.getSizeC(); channel++) 
						{ 
							int [] sourceImageOneChannel = sourceImageInteger.get(channel);
							int [] kymoImageOneChannel = cap.kymoImageInteger.get(channel); 
							int cnt = 0;
							for (ArrayList<int[]> mask : cap.masksList) 
							{
								int sum = 0;
								for (int[] m: mask)
									sum += sourceImageOneChannel[m[0] + m[1]*widthSourceImage];
								kymoImageOneChannel[cnt*widthKymoImage + t_out] = (int) (sum/mask.size()); 
								cnt ++;
							}
						}
					}
				}}));
		}
		waitAnalyzeExperimentCompletion(processor, futures, progressBar);
		seqCamData.seq.endUpdate();
        progressBar.close();
        /// ---------------------------------------------------------
		for (int icap=0; icap < nbcapillaries; icap++) 
		{
			Capillary cap = exp.capillaries.capillariesArrayList.get(icap);
			for (int chan = 0; chan < seqCamData.seq.getSizeC(); chan++) 
			{
				int [] tabValues = cap.kymoImageInteger.get(chan); 
				Object destArray = cap.bufKymoImage.getDataXY(chan);
				Array1DUtil.intArrayToSafeArray(tabValues, 0, destArray, 0, -1, cap.bufKymoImage.isSignedDataType(), cap.bufKymoImage.isSignedDataType());
				cap.bufKymoImage.setDataXY(chan, destArray);
			}
			cap.bufKymoImage.setVolatile(true);
			seqKymos.seq.setImage(icap, 0, cap.bufKymoImage);
		}
		return true;
	}
	
	private ArrayList<int []> transferImageToIntegerArrayList(IcyBufferedImage  workImage) 
	{	
		ArrayList<int []> sourceValuesArray = new ArrayList<int[]>(workImage.getSizeC());
		int len =  workImage.getSizeX() *  workImage.getSizeY();
		for (int chan = 0; chan < workImage.getSizeC(); chan++)  
		{
			int [] sourceValues = new int[len];
			sourceValues = Array1DUtil.arrayToIntArray(workImage.getDataXY(chan), sourceValues, workImage.isSignedDataType()); 
			sourceValuesArray.add(sourceValues);
		}
		return sourceValuesArray;
	}
	
	private void initArraysToBuildKymographImages(Experiment exp) 
	{
		SequenceCamData seqCamData = exp.seqCamData;
		int sizex = seqCamData.seq.getSizeX();
		int sizey = seqCamData.seq.getSizeY();	
		int numC = seqCamData.seq.getSizeC();
		if (numC <= 0)
			numC = 3;
		double fimagewidth =  1 + (exp.camLastImage_Ms - exp.camFirstImage_Ms ) / options.t_binMs;	
		int imageWidth = (int) fimagewidth;
		DataType dataType = seqCamData.seq.getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int nbcapillaries = exp.capillaries.capillariesArrayList.size();
		int imageHeight = 0;
		for (int i=0; i < nbcapillaries; i++) 
		{
			Capillary cap = exp.capillaries.capillariesArrayList.get(i);
			cap.masksList = new ArrayList<ArrayList<int[]>>();
			getPointsfromROIPolyLineUsingBresenham(cap.roi, cap.masksList, options.diskRadius, sizex, sizey);
			if (cap.masksList.size() > imageHeight)
				imageHeight = cap.masksList.size();
		}
		
		int len = imageWidth * imageHeight;
		for (int i=0; i < nbcapillaries; i++) 
		{
			Capillary cap = exp.capillaries.capillariesArrayList.get(i);
			cap.bufKymoImage = new IcyBufferedImage(imageWidth, imageHeight, numC, dataType);
			cap.kymoImageInteger = new ArrayList <int []>(len * numC);
			for (int chan = 0; chan < numC; chan++) 
			{
				Object dataArray = cap.bufKymoImage.getDataXY(chan);
				int[] tabValues = new int[len];
				tabValues = Array1DUtil.arrayToIntArray(dataArray, tabValues, false);
				cap.kymoImageInteger.add(tabValues);
			}
		} 
	}
	
	private void getPointsfromROIPolyLineUsingBresenham ( ROI2DShape roi, List<ArrayList<int[]>> masks, double diskRadius, int sizex, int sizey) 
	{
		ArrayList<int[]> pixels = Bresenham.getPixelsAlongLineFromROI2D (roi);
		double previousX = pixels.get(0)[0] - (pixels.get(1)[0] - pixels.get(0)[0]);
		double previousY = pixels.get(0)[1] - (pixels.get(1)[1] - pixels.get(0)[1]);
		for (int[] pixel: pixels) 
		{
			ArrayList<int[]> mask = new ArrayList<int[]>();
			double x = pixel[0];
			double y = pixel[1];
			double dx = x - previousX;
			double dy = y - previousY;
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
			previousX = x;
			previousY = y;
		}
	}

	private void adjustImage(Sequence seqForRegistration, IcyBufferedImage  workImage) 
	{
		seqForRegistration.setImage(1, 0, workImage);
		int referenceChannel = 1;
		int referenceSlice = 0;
		GaspardRigidRegistration.correctTemporalTranslation2D(seqForRegistration, referenceChannel, referenceSlice);
        boolean rotate = GaspardRigidRegistration.correctTemporalRotation2D(seqForRegistration, referenceChannel, referenceSlice);
        if (rotate) 
        	GaspardRigidRegistration.correctTemporalTranslation2D(seqForRegistration, referenceChannel, referenceSlice);
        workImage = seqForRegistration.getLastImage(1);
	}

}
