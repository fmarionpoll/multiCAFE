package plugins.fmp.multicafe2.series;


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
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.tools.Bresenham;
import plugins.kernel.roi.roi2d.ROI2DShape;



public class BuildKymographs_series extends BuildSeries  
{
	ArrayList<ArrayList <int []>> 			cap_kymoImageInteger = null;
	ArrayList<IcyBufferedImage>				cap_bufKymoImage 	 = null;
	ArrayList<ArrayList<ArrayList<int[]>>>	cap_masksList 		= null;
	int 									kymoImageWidth = 0;
	
	void analyzeExperiment(Experiment exp) 
	{
		loadExperimentDataToBuildKymos(exp);
		exp.seqCamData.displayViewerAtRectangle(options.parent0Rect);
		getTimeLimitsOfSequence(exp);
		if (buildKymo(exp)) 
			saveComputation(exp);
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
	
	private boolean loadExperimentDataToBuildKymos(Experiment exp) 
	{
		boolean flag = exp.xmlLoadMCCapillaries_Only();
		exp.seqCamData.seq = exp.seqCamData.initV2SequenceFromFirstImage(exp.seqCamData.getImagesList(true));
		return flag;
	}
	
	private void getTimeLimitsOfSequence(Experiment exp)
	{
		exp.kymoBinCol_Ms = options.t_binMs;
		if (options.isFrameFixed) 
		{
			exp.offsetFirstCol_Ms = options.t_firstMs;
			exp.offsetLastCol_Ms = options.t_lastMs;
			if (exp.offsetLastCol_Ms + exp.camFirstImage_Ms > exp.camLastImage_Ms)
				exp.offsetLastCol_Ms = exp.camLastImage_Ms - exp.camFirstImage_Ms;
		} 
		else 
		{
			exp.offsetFirstCol_Ms = 0;
			exp.offsetLastCol_Ms = exp.camLastImage_Ms - exp.camFirstImage_Ms;
		}
	}
			
	private void saveComputation(Experiment exp) 
	{	
		if (options.doCreateBinDir) 
			exp.setBinSubDirectory (exp.getBinNameFromKymoFrameStep());
		String directory = exp.getDirectoryToSaveResults(); 
		if (directory == null)
			return;
		
		ProgressFrame progressBar = new ProgressFrame("Save kymographs");
		int nframes = exp.seqKymos.seq.getSizeT();
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("buildkymo2");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();
		
		for (int t = 0; t < exp.seqKymos.seq.getSizeT(); t++) 
		{
			final int t_index = t;
			futuresArray.add(processor.submit(new Runnable () 
			{
				
				@Override
				public void run() 
				{	
					Capillary cap = exp.capillaries.capillariesArrayList.get(t_index);
					String filename = directory + File.separator + cap.getCapillaryName() + ".tiff";
					File file = new File (filename);
					IcyBufferedImage image = exp.seqKymos.getSeqImage(t_index, 0);
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
				}}));
		}
		waitFuturesCompletion(processor, futuresArray, progressBar);
		progressBar.close();
		exp.xmlSaveMCExperiment();
	}
	
	private boolean buildKymo (Experiment exp) 
	{
		SequenceCamData seqCamData = exp.seqCamData;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqCamData == null || seqKymos == null)
			return false;
	
		initArraysToBuildKymographImages(exp);
		if (exp.capillaries.capillariesArrayList.size() < 1) 
		{
			System.out.println("Abort (1): nbcapillaries = 0");
			return false;
		}
		
		int startFrame = (int) (exp.offsetFirstCol_Ms  /exp.camBinImage_Ms);
		final IcyBufferedImage referenceImage = seqCamData.getSeqImage(startFrame, 0); 
		seqCamData.seq.removeAllROI();
		
		exp.seqKymos.seq = new Sequence();
		
		int nbcapillaries = exp.capillaries.capillariesArrayList.size();
		if (nbcapillaries == 0) 
		{
			System.out.println("Abort(2): nbcapillaries = 0");
			return false;
		}
		
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		int nframes = (int) ((exp.offsetLastCol_Ms - exp.offsetFirstCol_Ms) / exp.kymoBinCol_Ms +1);
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("buildkymo2");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();
		
		for (int iframe = 0 ; iframe < nframes; iframe++) 
		{
			final int indexTo =  iframe;	
			long iindexms = iframe *  exp.kymoBinCol_Ms + exp.offsetFirstCol_Ms;
			final int indexFrom = (int) Math.round(((double)iindexms) / ((double) exp.camBinImage_Ms));
			if (indexFrom >= seqCamData.nTotalFrames)
				continue;

			futuresArray.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{				
					String sourceImageName = seqCamData.getFileName(indexFrom);			
					IcyBufferedImage sourceImage = imageIORead(sourceImageName);
					int sourceImageWidth = sourceImage.getWidth();				
					if (options.doRegistration ) 
						sourceImage = adjustImage(sourceImage, referenceImage);

					int len =  sourceImage.getSizeX() *  sourceImage.getSizeY();
					for (int chan = 0; chan < seqCamData.seq.getSizeC(); chan++) 
					{ 
						int [] sourceImageChannel = new int[len];
						sourceImageChannel = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(chan), sourceImageChannel, sourceImage.isSignedDataType()); 

						for (int icap=0; icap < nbcapillaries; icap++) 
						{
							ArrayList<int[]> cap_Integer = cap_kymoImageInteger.get(icap);
							ArrayList<ArrayList<int[]>> masksList =	cap_masksList.get(icap);
								
							int [] kymoImageChannel = cap_Integer.get(chan); 
							int cnt = 0;
							for (ArrayList<int[]> mask : masksList) 
							{
								int sum = 0;
								for (int[] m: mask)
									sum += sourceImageChannel[m[0] + m[1]*sourceImageWidth];
								if (mask.size() > 0)
									kymoImageChannel[cnt*kymoImageWidth + indexTo] = (int) (sum/mask.size()); 
								cnt ++;
							}
						}
					}
				}}));
		}
		
		waitFuturesCompletion(processor, futuresArray, progressBar);
		
        progressBar.close();
        
        /// ---------------------------------------------------------
        
        seqKymos.seq.beginUpdate();
		for (int icap=0; icap < nbcapillaries; icap++) 
		{
			ArrayList<int[]> cap_Integer = cap_kymoImageInteger.get(icap);
			IcyBufferedImage cap_Image = cap_bufKymoImage.get(icap);
			boolean isSignedDataType = cap_Image.isSignedDataType();
			for (int chan = 0; chan < seqCamData.seq.getSizeC(); chan++) 
			{
				int [] tabValues = cap_Integer.get(chan); ; 
				Object destArray = cap_Image.getDataXY(chan);
				Array1DUtil.intArrayToSafeArray(tabValues, 0, destArray, 0, -1, isSignedDataType, isSignedDataType);
				cap_Image.setDataXY(chan, destArray);
			}
			seqKymos.seq.setImage(icap, 0, cap_Image);
		}
		seqKymos.seq.endUpdate();
		return true;
	}
		
	private void initArraysToBuildKymographImages(Experiment exp) 
	{
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData.seq == null) 
			seqCamData.seq = exp.seqCamData.initV2SequenceFromFirstImage(exp.seqCamData.getImagesList(true));
		int sizex = seqCamData.seq.getSizeX();
		int sizey = seqCamData.seq.getSizeY();	
		int numC = seqCamData.seq.getSizeC();
		if (numC <= 0)
			numC = 3;

		kymoImageWidth = (int) ((exp.offsetLastCol_Ms - exp.offsetFirstCol_Ms) / exp.kymoBinCol_Ms +1);
		DataType dataType = seqCamData.seq.getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int nbcapillaries = exp.capillaries.capillariesArrayList.size();
		int imageHeight = 0;
		cap_masksList = new ArrayList<ArrayList<ArrayList<int[]>>>(nbcapillaries);
		for (int i=0; i < nbcapillaries; i++) 
		{
			Capillary cap = exp.capillaries.capillariesArrayList.get(i);
			ArrayList<ArrayList<int[]>> masksList = new ArrayList<ArrayList<int[]>>();
			getPointsfromROIPolyLineUsingBresenham(cap.roi, masksList, options.diskRadius, sizex, sizey);
			if (masksList.size() > imageHeight)
				imageHeight = masksList.size();
			cap_masksList.add(masksList);
		}
		
		int len = kymoImageWidth * imageHeight;
		cap_bufKymoImage = new ArrayList<IcyBufferedImage>(nbcapillaries);
		cap_kymoImageInteger = new ArrayList<ArrayList<int[]>>(nbcapillaries);
		
		for (int i=0; i < nbcapillaries; i++) 
		{
			IcyBufferedImage cap_Image = new IcyBufferedImage(kymoImageWidth, imageHeight, numC, dataType);
			cap_bufKymoImage.add(cap_Image);
			ArrayList<int[]> cap_Integer = new ArrayList <int []>(len * numC);
			cap_kymoImageInteger.add(cap_Integer);
			for (int chan = 0; chan < numC; chan++) 
			{
				Object dataArray = cap_Image.getDataXY(chan);
				int[] tabValues = new int[len];
				tabValues = Array1DUtil.arrayToIntArray(dataArray, tabValues, false);
				cap_Integer.add(tabValues);
			}
		} 
	}
	
	private void getPointsfromROIPolyLineUsingBresenham ( ROI2DShape roi, List<ArrayList<int[]>> masks, double diskRadius, int sizex, int sizey) 
	{
		ArrayList<int[]> pixels = Bresenham.getPixelsAlongLineFromROI2D (roi);
		int idiskRadius = (int) diskRadius;
		for (int[] pixel: pixels) 
			masks.add(getAllPixelsAroundPixel(pixel, idiskRadius, sizex, sizey));
	}
	
	private ArrayList<int[]> getAllPixelsAroundPixel(int[] pixel, int diskRadius, int sizex, int sizey) 
	{
		ArrayList<int[]> maskAroundPixel = new ArrayList<int[]>();
		double m1 = pixel[0];
		double m2 = pixel[1];
		double radiusSquared = diskRadius * diskRadius;
		int minX = getValueWithinLimits(pixel[0] - diskRadius, 0, sizex-1);
		int maxX = getValueWithinLimits(pixel[0] + diskRadius, minX, sizex-1);
		int minY = getValueWithinLimits(pixel[1] - diskRadius, 0, sizey-1);
		int maxY = getValueWithinLimits(pixel[1] + diskRadius, minX, sizey-1);
		for (int x = minX; x <= maxX; x++)
		{
		    for (int y = minY; y <= maxY; y++)
		    {
		        double dx = x - m1;
		        double dy = y - m2;
		        double distanceSquared = dx * dx + dy * dy;
		        if (distanceSquared <= radiusSquared)
		        {
		        	maskAroundPixel.add(new int[]{x, y});
		        }
		    }
		}
		return maskAroundPixel;
	}
	
	private int getValueWithinLimits(int x, int min, int max)
	{
		if (x < min)
			x = min;
		if (x > max)
			x = max;
		return x;
	}

	private IcyBufferedImage adjustImage(IcyBufferedImage  workImage, IcyBufferedImage referenceImage) 
	{
		int referenceChannel = 1;
		GaspardRigidRegistration.correctTranslation2D(workImage, referenceImage, referenceChannel);
        boolean rotate = GaspardRigidRegistration.correctRotation2D(workImage, referenceImage, referenceChannel);
        if (rotate) 
        	GaspardRigidRegistration.correctTranslation2D(workImage, referenceImage, referenceChannel);
        return workImage;
	}

}
