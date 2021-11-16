package plugins.fmp.multicafe2.series;


import java.awt.geom.Point2D;
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

import plugins.fmp.multicafe2.experiment.CapillariesWithTime;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.tools.Bresenham;




public class BuildKymographs_series extends BuildSeries  
{
	ArrayList<IcyBufferedImage>	cap_bufKymoImage 	= null;
	int 						kymoImageWidth 		= 0;
	
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
		exp.loadFileIntervalsFromSeqCamData();
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
					Capillary cap = exp.capillaries.capillariesList.get(t_index);
					String filename = directory + File.separator + cap.getKymographName() + ".tiff";
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
		if (exp.capillaries.capillariesList.size() < 1) 
		{
			System.out.println("Abort (1): nbcapillaries = 0");
			return false;
		}
		
		int startFrame = options.referenceFrame;
		final int sizeC = seqCamData.seq.getSizeC();
		
		seqCamData.seq.removeAllROI();
		exp.seqKymos.seq = new Sequence();
		
		int nbcapillaries = exp.capillaries.capillariesList.size();
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
		
		for (int iframe = 0 ; iframe < nframes; iframe++) {
			final int indexTo =  iframe;	
			long iindexms = iframe *  exp.kymoBinCol_Ms + exp.offsetFirstCol_Ms;
			final int indexFrom = (int) Math.round(((double)iindexms) / ((double) exp.camBinImage_Ms));
			if (indexFrom >= seqCamData.nTotalFrames)
				continue;
			
			final List<Capillary> capillaries = exp.capillaries.getCapillariesListAt(iframe);
			
			futuresArray.add(processor.submit(new Runnable () 
			{
				@Override
				public void run() 
				{						
					IcyBufferedImage sourceImage = imageIORead(seqCamData.getFileName(indexFrom));
					int sourceImageWidth = sourceImage.getWidth();				
					if (options.doRegistration ) {
						String referenceImageName = seqCamData.getFileName(startFrame);			
						IcyBufferedImage referenceImage = imageIORead(referenceImageName);
						adjustImage(sourceImage, referenceImage);
					}

					int len = sourceImage.getSizeX() *  sourceImage.getSizeY();
					
					for (int chan = 0; chan < sizeC; chan++) { 
						int [] sourceImageChannel = new int[len];
						sourceImageChannel = Array1DUtil.arrayToIntArray(
								sourceImage.getDataXY(chan), 
								sourceImageChannel, sourceImage.isSignedDataType()); 
//						System.out.println("sourceImageChannel chan="+chan);
						
						for (int icap = 0; icap < nbcapillaries; icap++) {
							Capillary cap = capillaries.get(icap);
//							Capillary cap0 = exp.capillaries.capillariesList.get(icap); //getCapillaryFromName(cap.getRoiName());
//							System.out.println("icap="+icap);
							int [] kymoImageChannel = cap.cap_Integer.get(chan); 
							
							int cnt = 0;
							for (ArrayList<int[]> mask : cap.masksList) {
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
		buildKymographImages(exp, seqKymos.seq, sizeC, nbcapillaries);
        progressBar.close();
        
		return true;
	}
	
	private void buildKymographImages(Experiment exp, Sequence seqKymo, int sizeC, int nbcapillaries)
	{
		seqKymo.beginUpdate();
		for (int icap=0; icap < nbcapillaries; icap++) {
			Capillary cap = exp.capillaries.capillariesList.get(icap);
			ArrayList<int[]> cap_Integer = cap.cap_Integer;
			IcyBufferedImage cap_Image = cap_bufKymoImage.get(icap);
			boolean isSignedDataType = cap_Image.isSignedDataType();
			for (int chan = 0; chan < sizeC; chan++) {
				int [] tabValues = cap_Integer.get(chan); ; 
				Object destArray = cap_Image.getDataXY(chan);
				Array1DUtil.intArrayToSafeArray(tabValues, 0, destArray, 0, -1, isSignedDataType, isSignedDataType);
				cap_Image.setDataXY(chan, destArray);
			}
			seqKymo.setImage(icap, 0, cap_Image);
		}
		seqKymo.endUpdate();
	}
		
	private void initArraysToBuildKymographImages(Experiment exp) 
	{
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData.seq == null) 
			seqCamData.seq = exp.seqCamData.initV2SequenceFromFirstImage(exp.seqCamData.getImagesList(true));
		int sizex = seqCamData.seq.getSizeX();
		int sizey = seqCamData.seq.getSizeY();	

		kymoImageWidth = (int) ((exp.offsetLastCol_Ms - exp.offsetFirstCol_Ms) / exp.kymoBinCol_Ms +1);
		
		int imageHeight = buildMasks(exp, exp.capillaries.capillariesList,sizex, sizey);
		exp.capillaries.CreateCapillariesWithTimeIfNull();
		for (CapillariesWithTime capillaries : exp.capillaries.capillariesWithTime) {
			int imageHeight_i = buildMasks(exp, capillaries.capillariesList, sizex, sizey);
			if (imageHeight_i > imageHeight) imageHeight = imageHeight_i;
		}
		buildCapInteger(exp, imageHeight);
		transferCapIntegerToListsWithTime(exp);
	}
	
	private int buildMasks (Experiment exp, List <Capillary> capillaries, int sizex, int sizey) {
		int imageHeight = 0;
		int nbcapillaries = capillaries.size();
		for (int i = 0; i < nbcapillaries; i++) {
			Capillary cap = capillaries.get(i);
			cap.masksList = new ArrayList<ArrayList<int[]>>();
			getPointsfromROIPolyLineUsingBresenham(cap.getCapillaryPoints(), cap.masksList, options.diskRadius, sizex, sizey);
			if (cap.masksList.size() > imageHeight)
				imageHeight = cap.masksList.size();
		}	
		return imageHeight;
	}
	
	private void buildCapInteger (Experiment exp, int imageHeight) 
	{
		SequenceCamData seqCamData = exp.seqCamData;
		int numC = seqCamData.seq.getSizeC();
		if (numC <= 0)
			numC = 3;
		
		DataType dataType = seqCamData.seq.getDataType_();
		if (dataType.toString().equals("undefined"))
			dataType = DataType.UBYTE;

		int len = kymoImageWidth * imageHeight;
		int nbcapillaries = exp.capillaries.capillariesList.size();
		cap_bufKymoImage = new ArrayList<IcyBufferedImage>(nbcapillaries);
		
		for (int i=0; i < nbcapillaries; i++) {
			IcyBufferedImage cap_Image = new IcyBufferedImage(kymoImageWidth, imageHeight, numC, dataType);
			cap_bufKymoImage.add(cap_Image);
			
			Capillary cap = exp.capillaries.capillariesList.get(i);
			cap.cap_Integer = new ArrayList <int []>(numC);

			for (int chan = 0; chan < numC; chan++) {
				//Object dataArray = cap_Image.getDataXY(chan);
				int[] tabValues = new int[len];
				//tabValues = Array1DUtil.arrayToIntArray(dataArray, tabValues, false);
				cap.cap_Integer.add(tabValues);
			}
		}
	}
	
	private int transferCapIntegerToListsWithTime (Experiment exp) 
	{
		int imageHeight = 0;
		
		for (CapillariesWithTime capillariesWithTime : exp.capillaries.capillariesWithTime) {
			int nbcapillaries = capillariesWithTime.capillariesList.size();
			for (int i = 0; i < nbcapillaries; i++) {
				Capillary cap = capillariesWithTime.capillariesList.get(i);
				Capillary cap0 = exp.capillaries.getCapillaryFromName(cap.getRoiName(), exp.capillaries.capillariesList);
				cap.cap_Integer = cap0.cap_Integer;
			}	
		}
		return imageHeight;
	}
	
	private void getPointsfromROIPolyLineUsingBresenham (ArrayList<Point2D> pointsList, List<ArrayList<int[]>> masks, double diskRadius, int sizex, int sizey) 
	{
		ArrayList<int[]> pixels = Bresenham.getPixelsAlongLineFromROI2D (pointsList);
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
		int minY = pixel[1]; // getValueWithinLimits(pixel[1] - diskRadius, 0, sizey-1);
		int maxY = pixel[1]; // getValueWithinLimits(pixel[1] + diskRadius, minY, sizey-1);

		for (int x = minX; x <= maxX; x++) {
		    for (int y = minY; y <= maxY; y++) {
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

	private void adjustImage(IcyBufferedImage workImage, IcyBufferedImage referenceImage) 
	{
		int referenceChannel = 0;
		GaspardRigidRegistration.correctTranslation2D(workImage, referenceImage, referenceChannel);
        boolean rotate = GaspardRigidRegistration.correctRotation2D(workImage, referenceImage, referenceChannel);
        if (rotate) 
        	GaspardRigidRegistration.correctTranslation2D(workImage, referenceImage, referenceChannel);
	}

}
