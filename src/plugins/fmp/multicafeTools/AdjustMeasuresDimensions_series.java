package plugins.fmp.multicafeTools;


import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;



public class AdjustMeasuresDimensions_series  extends SwingWorker<Integer, Integer> {
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	public DetectLimits_Options options 		= new DetectLimits_Options();
	
	
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
			System.out.println(exp.experimentFileName);
			progress.setMessage("Processing file: " + (index-expList.index0 +1) + "//" + nbexp);
			
			exp.loadExperimentData();
			exp.displayCamData(options.parent0Rect);
			if (exp.loadKymographs()) {
				exp.adjustCapillaryMeasuresDimensions();
				saveComputation(exp);
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
	
	private void saveComputation(Experiment exp) {			
		ProgressFrame progress = new ProgressFrame("Save kymograph measures");		
		exp.saveExperimentMeasures();
		progress.close();
	}

}
