package plugins.fmp.multicafe.tools;


import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.series.DetectLevels_Options;



public class AdjustMeasuresDimensions_series  extends SwingWorker<Integer, Integer> {
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public DetectLevels_Options options 		= new DetectLevels_Options();
	
	
	@Override
	protected Integer doInBackground() throws Exception {
		System.out.println("start detectLimits thread");
        threadRunning = true;
        int nbiterations = 0;
		ExperimentList expList = options.expList;
		int nbexp = expList.index1 - expList.index0 +1;
		ProgressFrame progress = new ProgressFrame("Detect limits");
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag)
				break;
			Experiment exp = expList.getExperiment(index);
			System.out.println(exp.getExperimentFileName());
			progress.setMessage("Processing file: " + (index-expList.index0 +1) + "//" + nbexp);
			
			exp.loadExperimentCapillariesData_ForSeries();
			exp.displaySequenceData(options.parent0Rect, exp.seqCamData.seq);
			if (exp.loadKymographs()) {
				exp.adjustCapillaryMeasuresDimensions();
				exp.saveExperimentMeasures(exp.getResultsDirectory());
			}
			exp.seqCamData.closeSequence();
			exp.seqKymos.closeSequence();
		}
		progress.close();
		threadRunning = false;
		return nbiterations;
	}

	@Override
	protected void done() {
		int statusMsg = 0;
		try {
			statusMsg = get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} 
		if (!threadRunning || stopFlag) {
			firePropertyChange("thread_ended", null, statusMsg);
		} else {
			firePropertyChange("thread_done", null, statusMsg);
		}
    }

}
