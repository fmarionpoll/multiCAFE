package plugins.fmp.multicafeTools;


import icy.gui.viewer.Viewer;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Experiment;

public class Build_series {

	Viewer	viewer1 = null;
	Viewer	viewer2 = null;


	void closeViewer (Experiment exp) {
		exp.seqCamData.seq.close();
		exp.seqKymos.seq.close();
	}
	
	void initViewerCamData (Experiment exp) {
		ThreadUtil.invoke (new Runnable() {
			@Override
			public void run() {
				viewer1 = new Viewer(exp.seqCamData.seq, true);
			}
		}, true);
		
		if (viewer1 == null) {
			viewer1 = exp.seqCamData.seq.getFirstViewer(); 
			if (!viewer1.isInitialized()) {
				try {
					Thread.sleep(1000);
					if (!viewer1.isInitialized())
						System.out.println("Viewer still not initialized after 1 s waiting");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	void initViewerKymosData (Experiment exp) {
		ThreadUtil.invoke (new Runnable() {
//		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				viewer2 = new Viewer(exp.seqKymos.seq, true);
			}
//		});
		}, true);
		
		if (viewer2 == null) {
			viewer2 = exp.seqKymos.seq.getFirstViewer(); 
			if (!viewer2.isInitialized()) {
				try {
					Thread.sleep(1000);
					if (!viewer2.isInitialized())
						System.out.println("Viewer still not initialized after 1 s waiting");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
