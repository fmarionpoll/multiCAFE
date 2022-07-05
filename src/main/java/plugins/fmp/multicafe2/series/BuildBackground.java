package plugins.fmp.multicafe2.series;


import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageCursor;
import icy.image.IcyBufferedImageUtil;
import icy.sequence.Sequence;
import plugins.fmp.multicafe2.experiment.Experiment;

import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;





public class BuildBackground extends BuildSeries 
{
	public Sequence seqData = new Sequence();
	public Sequence seqReference = null;
	
	private Viewer vData = null;
	private Viewer vReference = null;

	private FlyDetectTools flyDetectTools = new FlyDetectTools();	

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runBuildBackground(exp);
		
    }

	private void closeSequences () 
	{
		closeSequence(seqReference); 
		closeSequence(seqData);
	}
	
	private void closeViewers() 
	{
		closeViewer(vData);
		closeViewer(vReference);
		closeSequences();
	}
	
	public void openViewers(Experiment exp) 
	{
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() 
				{			
					seqData = newSequence("data recorded", exp.seqCamData.getSeqImage(0, 0));
					vData = new Viewer(seqData, true);
					
					seqReference = newSequence("referenceImage", exp.seqCamData.refImage);
					exp.seqReference = seqReference;
					vReference = new Viewer(seqReference, true);
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void runBuildBackground(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		flyDetectTools.initParametersForDetection(exp, options);
		flyDetectTools.initTempRectROIs(exp, exp.seqCamData.seq, options.detectCage);
		options.threshold = options.thresholdDiff;
		
		openViewers(exp);
		try {
			ImageTransformOptions transformOptions = new ImageTransformOptions();
			transformOptions.transformOption = EnumImageTransformations.SUBTRACT; 
			transformOptions.setSingleThreshold(options.threshold, stopFlag);
			buildBackgroundImage(exp, transformOptions);
			exp.saveReferenceImage(seqReference.getFirstImage());
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		closeViewers();
	}
	
	private void buildBackgroundImage(Experiment exp, ImageTransformOptions transformOptions) throws InterruptedException 
	{
		ProgressFrame progress = new ProgressFrame("Build background image...");
		flyDetectTools.initParametersForDetection(exp, options);

		int t_from = (int) ((exp.cages.detectFirst_Ms - exp.camFirstImage_ms)/exp.camBinImage_ms);
		long limit = 50 ;
		if (limit > exp.seqCamData.nTotalFrames)
			limit = exp.seqCamData.nTotalFrames;
		limit = limit * exp.cages.detectBin_Ms +exp.cages.detectFirst_Ms ;
		
		exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.getSeqImage(t_from, 0));
		transformOptions.referenceImage = exp.seqCamData.refImage;

		long first_ms = exp.cages.detectFirst_Ms + exp.cages.detectBin_Ms;
		int t0 = (int) ((first_ms - exp.cages.detectFirst_Ms)/exp.camBinImage_ms);

		for (long indexms = first_ms ; indexms <= limit && !stopFlag; indexms += exp.cages.detectBin_Ms) 
		{
			int t = (int) ((indexms - exp.cages.detectFirst_Ms)/exp.camBinImage_ms);
			if (t == t0)
				continue;
			
			IcyBufferedImage currentImage = imageIORead(exp.seqCamData.getFileName(t));
			seqData.setImage(0, 0, currentImage);
			
			transformBackground(currentImage, transformOptions);
			seqReference.setImage(0, 0, transformOptions.referenceImage);
			
//			System.out.println("t= "+t+ " n pixels changed=" + transformOptions.npixels_changed);
			if (transformOptions.npixels_changed < 10 && t > 0 ) 
				break;
		}
		exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(seqReference.getFirstImage());
		progress.close();
	}
	
	void transformBackground(IcyBufferedImage sourceImage, ImageTransformOptions transformOptions) 
	{
		if (transformOptions.referenceImage == null)
			return;
		
		int width = sourceImage.getSizeX();
		int height = sourceImage.getSizeY();
		int planes = sourceImage.getSizeC();
		transformOptions.npixels_changed = 0;
		int changed = 0;
		
		IcyBufferedImageCursor sourceCursor = new IcyBufferedImageCursor(sourceImage);
		IcyBufferedImageCursor referenceCursor = new IcyBufferedImageCursor(transformOptions.referenceImage);
		
		try 
		{
			for (int y = 0; y < height; y++ ) 
			{
				for (int x = 0; x < width; x++) 
				{
					for (int c = 0; c < planes; c++) 
					{
						double val = sourceCursor.get(x, y, c) - referenceCursor.get(x, y, c);
						if (val >= transformOptions.simplethreshold) 
						{
							changed ++;
							int delta = 10;
							for (int yy = y-delta; yy < y+delta; yy++ ) 
							{
								if (yy < 0 || yy >= height)
									continue;
								for (int xx = x-delta; xx < x+delta; xx++) 
								{
									if (xx < 0 || xx >= width)
										continue;
									for (int cc = 0; cc < planes; cc++) 
									{
										referenceCursor.set(xx, yy, cc, sourceCursor.get(xx, yy, cc));
									}
								}
							}

						}
					}
				}
			}
		} 
		finally 
		{
			referenceCursor.commitChanges();
			transformOptions.npixels_changed = changed;
		}
	}


}