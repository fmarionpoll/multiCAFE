package plugins.fmp.multicafe2.series;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import loci.formats.FormatException;

import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.KymoROI2D;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.tools.Bresenham;
import plugins.fmp.multicafe2.tools.GaspardRigidRegistration;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;


public class BuildKymographs extends BuildSeries  
{
	public Sequence seqData = new Sequence();
	private Viewer vData = null;
	ArrayList<IcyBufferedImage>	cap_bufKymoImage 	= null;
	int 						kymoImageWidth 		= 0;
	
	
	void analyzeExperiment(Experiment exp) 
	{
		loadExperimentDataToBuildKymos(exp);
		
		openKymoViewers(exp);
		getTimeLimitsOfSequence(exp);
		if (buildKymo(exp)) 
			saveComputation(exp);
		
		closeKymoViewers();
		exp.seqKymos.closeSequence();
	}
	
	private boolean loadExperimentDataToBuildKymos(Experiment exp) 
	{
		boolean flag = exp.xmlLoadMCCapillaries_Only();
		exp.seqCamData.seq = exp.seqCamData.initSequenceFromFirstImage(exp.seqCamData.getImagesList(true));
		return flag;
	}
	
	private void getTimeLimitsOfSequence(Experiment exp)
	{
		exp.getFileIntervalsFromSeqCamData();
		exp.kymoBin_ms = options.t_Ms_BinDuration;
		if (options.isFrameFixed) {
			exp.kymoFirst_ms = options.t_Ms_First;
			exp.kymoLast_ms = options.t_Ms_Last;
			if (exp.kymoLast_ms + exp.camImageFirst_ms > exp.camImageLast_ms)
				exp.kymoLast_ms = exp.camImageLast_ms - exp.camImageFirst_ms;
		} 
		else {
			exp.kymoFirst_ms = 0;
			exp.kymoLast_ms = exp.camImageLast_ms - exp.camImageFirst_ms;
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
		int nCPUs = SystemUtil.getNumberOfCPUs();
	    final Processor processor = new Processor(nCPUs);
	    processor.setThreadName("buildkymo2");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();
		
		for (int t = 0; t < exp.seqKymos.seq.getSizeT(); t++) {
			final int t_index = t;
			futuresArray.add(processor.submit(new Runnable () {
				@Override
				public void run() {	
					Capillary cap = exp.capillaries.capillariesList.get(t_index);
					String filename = directory + File.separator + cap.getKymographName() + ".tiff";
					File file = new File (filename);
					IcyBufferedImage image = exp.seqKymos.getSeqImage(t_index, 0);
					try {
						Saver.saveImage(image, file, true);
					} 
					catch (FormatException e) {
						e.printStackTrace();
					} 
					catch (IOException e) {
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
		SequenceKymos seqKymos = exp.seqKymos;
		seqKymos.seq = new Sequence();
	
		initArraysToBuildKymographImages(exp);
		if (exp.capillaries.capillariesList.size() < 1) {
			System.out.println("Abort (1): nbcapillaries = 0");
			return false;
		}
		
		final int sizeC = seqData.getSizeC();
		
		int nbcapillaries = exp.capillaries.capillariesList.size();
		if (nbcapillaries == 0) {
			System.out.println("Abort(2): nbcapillaries = 0");
			return false;
		}
		
		threadRunning = true;
		stopFlag = false;
		ProgressFrame progressBar = new ProgressFrame("Processing with subthreads started");
		int nframes = (int) ((exp.kymoLast_ms - exp.kymoFirst_ms) / exp.kymoBin_ms +1);
	    	
		for (int iframe = 0 ; iframe < nframes; iframe++) {
			
			final int indexToFrame =  iframe;	
			long iindexms = iframe *  exp.kymoBin_ms + exp.kymoFirst_ms;
			final int indexFromFrame = (int) Math.round(((double)iindexms) / ((double) exp.camImageBin_ms));
			if (indexFromFrame >= exp.seqCamData.nTotalFrames)
				continue;
			
			vData.setTitle("Frame: " + (iframe +1)+ " / " + nframes);
			
			IcyBufferedImage sourceImage = loadImageFromIndex(exp, indexFromFrame);
			seqData.setImage(0, 0, sourceImage);
			int sourceImageWidth = sourceImage.getWidth();
			final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
		    processor.setThreadName("buildkymo2");
		    processor.setPriority(Processor.NORM_PRIORITY);
		    int nFutures = exp.capillaries.capillariesList.size() * sizeC;
	        ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nFutures );
			futuresArray.clear();
			
			for (int chan = 0; chan < sizeC; chan++) { 
				final int [] sourceImageChannel =  Array1DUtil.arrayToIntArray(
										sourceImage.getDataXY(chan), 
										sourceImage.isSignedDataType()); 
				
				for (Capillary cap: exp.capillaries.capillariesList) 
				{
					KymoROI2D capT = cap.getROI2DKymoAtIntervalT(indexFromFrame);
					int [] kymoImageChannel = cap.cap_Integer.get(chan); 
					
					futuresArray.add(processor.submit(new Runnable () {
					@Override
					public void run() 
					{	
						int cnt = 0;
						for (ArrayList<int[]> mask : capT.getMasksList()) 
						{
							int sum = 0;
							for (int[] m: mask)
								sum += sourceImageChannel[m[0] + m[1]*sourceImageWidth];
							if (mask.size() > 0)
								kymoImageChannel[cnt*kymoImageWidth + indexToFrame] = (int) (sum/mask.size()); 
							cnt ++;
						}
					}}));
				}
			}
			waitFuturesCompletion(processor, futuresArray, null); //progressBar);
		}
		buildKymographImages(exp, seqKymos.seq, sizeC, nbcapillaries);
        progressBar.close();
        
		return true;
	}
	
	private IcyBufferedImage loadImageFromIndex(Experiment exp, int indexFromFrame) 
	{
		IcyBufferedImage sourceImage = imageIORead(exp.seqCamData.getFileName(indexFromFrame));				
		if (options.doRegistration ) 
		{
			String referenceImageName = exp.seqCamData.getFileName(options.referenceFrame);			
			IcyBufferedImage referenceImage = imageIORead(referenceImageName);
			adjustImage(sourceImage, referenceImage);
		}
		return sourceImage;
	}
	
	private void buildKymographImages(Experiment exp, Sequence seqKymo, int sizeC, int nbcapillaries)
	{
		seqKymo.beginUpdate();
		for (int icap = 0; icap < nbcapillaries; icap++) {
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
			seqCamData.seq = exp.seqCamData.initSequenceFromFirstImage(exp.seqCamData.getImagesList(true));
		int sizex = seqCamData.seq.getSizeX();
		int sizey = seqCamData.seq.getSizeY();	

		kymoImageWidth = (int) ((exp.kymoLast_ms - exp.kymoFirst_ms) / exp.kymoBin_ms +1);
		
		int imageHeight = 0;
		for (Capillary cap: exp.capillaries.capillariesList) {
			for (KymoROI2D capT : cap.getROIsForKymo()) {
				int imageHeight_i = buildMasks(capT, sizex, sizey);
				if (imageHeight_i > imageHeight) imageHeight = imageHeight_i;
			}
		}
		buildCapInteger(exp, imageHeight);
	}
	
	private int buildMasks (KymoROI2D capT, int sizex, int sizey) {
		ArrayList<ArrayList<int[]>> masks = new ArrayList<ArrayList<int[]>>();
		getPointsfromROIPolyLineUsingBresenham(
				ROI2DUtilities.getCapillaryPoints(capT.getRoi()), 
				masks, 
				options.diskRadius, 
				sizex, 
				sizey);
		capT.setMasksList(masks);	
		return masks.size();
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
				int[] tabValues = new int[len];
				cap.cap_Integer.add(tabValues);
			}
		}
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
		int minX = clipValueToLimits(pixel[0] - diskRadius, 0, sizex-1);
		int maxX = clipValueToLimits(pixel[0] + diskRadius, minX, sizex-1);
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
	
	private int clipValueToLimits(int x, int min, int max)
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
	
	private void closeKymoViewers() 
	{
		closeViewer(vData);
		closeSequence(seqData);
	}
	
	private void openKymoViewers(Experiment exp) 
	{
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() 
				{			
					seqData = newSequence("data frame", exp.seqCamData.getSeqImage(0, 0));
					vData = new Viewer(seqData, true);
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}
	}

}
