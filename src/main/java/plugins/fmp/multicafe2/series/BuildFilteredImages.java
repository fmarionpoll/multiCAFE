package plugins.fmp.multicafe2.series;

import java.util.ArrayList;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;

public class BuildFilteredImages extends BuildSeries {

	@Override
	void analyzeExperiment(Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		int zChannelDestination = 1;
		buildFiltered(seqCamData, 0, zChannelDestination, options.transform01, options.spanDiff);
		
	}
	
	void buildFiltered(SequenceCamData seqCamData, 
			int zChannelSource, 
			int zChannelDestination, 
			EnumImageTransformations transformop1, 
			int spanDiff) 
	{
		int nimages = seqCamData.seq.getSizeT();
		seqCamData.seq.beginUpdate();

		ImageTransformInterface transform = transformop1.getFunction();
		if (transform == null)
			return;
		
		ProgressFrame progressBar = new ProgressFrame("Save kymographs");
		int nframes = seqCamData.seq.getSizeT();
		int nCPUs = SystemUtil.getNumberOfCPUs();
	    final Processor processor = new Processor(nCPUs);
	    processor.setThreadName("buildFilteredImages");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();
		
		for (int t= 0; t < nimages; t++) 
		{
			final int t_index = t;
			futuresArray.add(processor.submit(new Runnable () {
				@Override
				public void run() {	
					IcyBufferedImage img = seqCamData.getSeqImage(t_index, zChannelSource);
					IcyBufferedImage img2 = transform.transformImage (img, null);
					seqCamData.seq.setImage(t_index, zChannelDestination, img2);
				}}));
		}
		
		waitFuturesCompletion(processor, futuresArray, progressBar);
		progressBar.close();
		seqCamData.seq.dataChanged();
		seqCamData.seq.endUpdate();
	}

}
