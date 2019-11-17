package plugins.fmp.multicafeTools;

import icy.gui.viewer.Viewer;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.SequenceCamData;

public class ViewerUpdater implements Runnable {

	Viewer 			viewer 			= null;
	SequenceCamData seqCamData 		= null;
	boolean 		isInterrupted 	= false;
	int				milliSleep		= 200;
	
	
	ViewerUpdater (SequenceCamData seq) {
		this.seqCamData = seq;
		viewer = seqCamData.seq.getFirstViewer();
		isInterrupted = false;
		milliSleep = 200;
	}
	
	ViewerUpdater (SequenceCamData seq, int milliSleep) {
		this.seqCamData = seq;
		viewer = seqCamData.seq.getFirstViewer();
		isInterrupted = false;
		this.milliSleep = milliSleep;
	}
	
	@Override
	public void run() {
		while (!isInterrupted) {
			int posT = seqCamData.currentFrame;
			if (viewer != null)
				viewer.setPositionT(posT);
			ThreadUtil.sleep(milliSleep);
		}
	}
}
