package plugins.fmp.multicafe2.series;

import java.awt.geom.Point2D;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;

import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;




public class FlyDetect2 extends BuildSeries 
{
	private FlyDetectTools find_flies = new FlyDetectTools();	
	public boolean viewInternalImages = true;

	// -----------------------------------------

	void analyzeExperiment(Experiment exp) 
	{
		if (!loadDrosoTrack(exp))
			return;
		if (!checkBoundsForCages(exp))
			return;

		runFlyDetect2(exp);
		exp.cages.orderFlyPositions();
		if (!stopFlag)
			exp.cages.xmlWriteCagesToFileNoQuestion(exp.getMCDrosoTrackFullName());
		exp.seqCamData.closeSequence();
    }
	
	private void runFlyDetect2(Experiment exp) 
	{
		exp.cleanPreviousDetectedFliesROIs();
		find_flies.initParametersForDetection(exp, options);
		find_flies.initCagesPositions(exp, options.detectCage);
		options.threshold = options.thresholdDiff;

		if (exp.loadReferenceImage()) 
		{
			openFlyDetectViewers(exp);
			findFliesInAllFrames(exp);
		}
	}

	private void findFliesInAllFrames(Experiment exp) 
	{
		ProgressFrame progressBar = new ProgressFrame("Detecting flies...");
		find_flies.initCagesPositions(exp, options.detectCage);
		seqNegative.removeAllROI();
		
		ImageTransformOptions transformOptions = new ImageTransformOptions();
		transformOptions.transformOption = EnumImageTransformations.SUBTRACT_REF;
		transformOptions.referenceImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.refImage);
		ImageTransformInterface transformFunction = transformOptions.transformOption.getFunction();
		
		long last_ms = exp.cages.detectLast_Ms + exp.cages.detectBin_Ms ;
		for (long index_ms = exp.cages.detectFirst_Ms ; index_ms <= last_ms; index_ms += exp.cages.detectBin_Ms ) 
		{
			final int t_from = (int) ((index_ms - exp.camFirstImage_ms)/exp.camBinImage_ms);
			if (t_from >= exp.seqCamData.nTotalFrames)
				continue;
			String title = "Frame #"+ t_from + "/" + exp.seqCamData.nTotalFrames;
			progressBar.setMessage(title);

			IcyBufferedImage workImage = imageIORead(exp.seqCamData.getFileName(t_from));
			IcyBufferedImage negativeImage = transformFunction.transformImage(workImage, transformOptions);
			try {
				seqNegative.beginUpdate();
				seqNegative.setImage(0, 0, negativeImage);
				vNegative.setTitle(title);
				List<Point2D> listPoints = find_flies.findFlies2(seqNegative, negativeImage, t_from);
				addGreenROI2DPoints(seqNegative, listPoints, true);
				seqNegative.endUpdate();
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		progressBar.close();
	}

}