package plugins.fmp.multicafe2.series;


import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;

import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;





public class FlyDetect1 extends BuildSeries 
{
	public boolean buildBackground	= true;
	public boolean	detectFlies = true;
	private Viewer vNegative = null;
	private Sequence seqNegative = null;
	public FlyDetectTools find_flies = new FlyDetectTools();
	
	// -----------------------------------------------------
	
	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;
		
		runFlyDetect1(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
		exp.seqCamData.closeSequence();
    }
	
	private void openDetectionViewer(Experiment exp) 
	{
		try 
		{
			SwingUtilities.invokeAndWait(new Runnable() 
			{ 
				public void run() 
				{
					seqNegative = newSequence("detectionImage", exp.seqCamData.refImage);
					vNegative = new Viewer (seqNegative, false);
					vNegative.setVisible(true);
				}});
		} 
		catch (InvocationTargetException | InterruptedException e) 
		{
			e.printStackTrace();
		} 
	}
	
	private void runFlyDetect1(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initCagesPositions(exp, options.detectCage);
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		openDetectionViewer(exp);
		
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = options.transformop;
		getReferenceImage (exp, 0, transformOptions);
		ImageTransformInterface transformFunction = options.transformop.getFunction();
		
		int t_current = 0;
	
		long last_ms = exp.cages.detectLast_Ms + exp.cages.detectBin_Ms ;
		for (long index_ms = exp.cages.detectFirst_Ms; index_ms <= last_ms; index_ms += exp.cages.detectBin_Ms ) 
		{
			final int t_previous = t_current;
			final int t_from = (int) ((index_ms - exp.camFirstImage_ms)/exp.camBinImage_ms);
			if (t_from >= exp.seqCamData.nTotalFrames)
				continue;
			
			t_current = t_from;
			progressBar.setMessage("Processing image: " + (t_from +1));
	
			IcyBufferedImage sourceImage = imageIORead(exp.seqCamData.getFileName(t_from));
			getReferenceImage (exp, t_previous, transformOptions);
			IcyBufferedImage workImage = transformFunction.transformImage(sourceImage, transformOptions); 
			if (workImage == null)
				return;

			try 
			{
				seqNegative.beginUpdate();
				seqNegative.setImage(0, 0, workImage);
				vNegative.setTitle("Frame #"+ t_from + "/" + exp.seqCamData.nTotalFrames);
				List<Point2D> listPoints = find_flies.findFlies1 (workImage, t_from);
				addGreenROI2DPoints(seqNegative, listPoints, true);
				seqNegative.endUpdate();
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}					
		}

		progressBar.close();
	}
	
	private void getReferenceImage (Experiment exp, int t, ImageTransformOptions options) 
	{
		switch (options.transformOption) 
		{
			case SUBTRACT_TM1: 
				options.referenceImage = imageIORead(exp.seqCamData.getFileName(t));
				break;
				
			case SUBTRACT_T0:
			case SUBTRACT_REF:
				if (options.referenceImage == null)
					options.referenceImage = imageIORead(exp.seqCamData.getFileName(0));
				break;
				
			case NONE:
			default:
				break;
		}
	}
	
	
}