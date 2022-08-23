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
	
	private void openBackgroundViewers(Experiment exp) 
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
		flyDetectTools.initCagesPositions(exp, options.detectCage);
		options.threshold = options.thresholdDiff;
		
		openBackgroundViewers(exp);
		try {
			ImageTransformOptions transformOptions = new ImageTransformOptions();
			transformOptions.transformOption = EnumImageTransformations.SUBTRACT; 
			transformOptions.setSingleThreshold(options.backgroundThreshold, stopFlag);
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

		transformOptions.referenceImage  = imageIORead(exp.seqCamData.getFileName(options.backgroundFirst));

		long first_ms = exp.cages.detectFirst_Ms + (options.backgroundFirst * exp.camBinImage_ms);
		final int t_first = (int) ((first_ms - exp.cages.detectFirst_Ms)/exp.camBinImage_ms);
		
		int t_last = options.backgroundFirst + options.backgroundNFrames;
		if (t_last > exp.seqCamData.nTotalFrames)
			t_last = exp.seqCamData.nTotalFrames;

		for (int t = t_first + 1; t <= t_last && !stopFlag; t ++) 
		{
			IcyBufferedImage currentImage = imageIORead(exp.seqCamData.getFileName(t));
			seqData.setImage(0, 0, currentImage);
			
			transformBackground(currentImage, transformOptions);
			seqReference.setImage(0, 0, transformOptions.referenceImage);
			
//			System.out.println("t= "+t+ " n pixels changed=" + transformOptions.npixels_changed);
			if (transformOptions.npixels_changed < 10 ) 
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
						double valDiff = referenceCursor.get(x, y, c) - sourceCursor.get(x, y, c);
						if (valDiff >= transformOptions.simplethreshold) 
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