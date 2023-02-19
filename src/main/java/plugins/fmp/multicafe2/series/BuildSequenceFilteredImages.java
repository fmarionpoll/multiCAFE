package plugins.fmp.multicafe2.series;

import java.util.ArrayList;
import java.util.concurrent.Future;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import plugins.fmp.multicafe2.tools.TransformImage.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.TransformImage.TransformImageInterface;

public class BuildSequenceFilteredImages extends BuildSequence {

	@Override
	void runFilter(Sequence seq) {

		int zChannelDestination = 1;
		buildFiltered(seq, 0, zChannelDestination, options.transform01, options.spanDiff);
		
	}
	
	void buildFiltered(Sequence seq, 
			int zChannelSource, 
			int zChannelDestination, 
			EnumImageTransformations transformop1, 
			int spanDiff) 
	{
		seq.beginUpdate();
		int nimages = seq.getSizeT();
		int zDimensions = seq.getSizeZ();
		if (zDimensions <= 1) 
			SequenceUtil.addZ(seq, 1);
		openSequenceViewer(seq);
		temporaryViewer.setPositionZ(zChannelDestination);
		
		TransformImageInterface transform = transformop1.getFunction();
		if (transform == null)
			return;
		
		ProgressFrame progressBar = new ProgressFrame("Build filtered images");
		int nframes = seq.getSizeT();
		int nCPUs = SystemUtil.getNumberOfCPUs();
	    final Processor processor = new Processor(nCPUs);
	    processor.setThreadName("buildFilteredImages");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nframes);
		futuresArray.clear();
		
		for (int t = 0; t < nimages; t++) 
		{
			final int t_index = t;
			futuresArray.add(processor.submit(new Runnable () {
				@Override
				public void run() {	
					IcyBufferedImage img = seq.getImage(t_index, zChannelSource);
					IcyBufferedImage img2 = transform.getTransformedImage (img, null);
					seq.setImage(t_index, zChannelDestination, img2);
					temporaryViewer.setPositionT(t_index);
				}}));
		}
		
		waitFuturesCompletion(processor, futuresArray, progressBar);
		closeSequenceViewer();
		progressBar.close();
		
		seq.dataChanged();
		seq.endUpdate();
	}
	

}
