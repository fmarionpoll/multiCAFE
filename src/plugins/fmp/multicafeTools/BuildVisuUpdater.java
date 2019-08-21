package plugins.fmp.multicafeTools;

import icy.gui.viewer.Viewer;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.SequenceCamData;

public class BuildVisuUpdater implements Runnable {

	Viewer viewer = null;
	SequenceCamData seqCamData = null;
	boolean isInterrupted = false;
	
	BuildVisuUpdater (SequenceCamData seq) {
		this.seqCamData = seq;
		viewer = seqCamData.seq.getFirstViewer();
		isInterrupted = false;
	}
	
	@Override
	public void run() {
		while (!isInterrupted) {
			int posT = seqCamData.currentFrame;
			viewer.setPositionT(posT);
			//viewer.setTitle(seqCamData.getDecoratedImageName(posT));
			ThreadUtil.sleep(200);
		}
	}
}
