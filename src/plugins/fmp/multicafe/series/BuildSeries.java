package plugins.fmp.multicafe.series;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import icy.gui.frame.progress.ProgressFrame;
import icy.main.Icy;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;

public abstract class BuildSeries extends SwingWorker<Integer, Integer> {

	public BuildSeries_Options 	options 		= new BuildSeries_Options();
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	
	@Override
	protected Integer doInBackground() throws Exception {
		System.out.println("start detect levels thread");
		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(false);
		
		threadRunning = true;
        int nbiterations = 0;
		ExperimentList expList = options.expList;
		ProgressFrame progress = new ProgressFrame("Starting thread");
		long startTimeInNs = System.nanoTime();
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag)
				break;
			progress.setMessage("Processing file: " + (index +1) + "//" + (expList.index1+1));
			Experiment exp = expList.getExperiment(index);	
			System.out.println((index+1)+": " +exp.getExperimentFileName());
			
			exp.resultsSubPath = options.resultsSubPath;
			String resultsDirectory = exp.getResultsDirectory(); 
			exp.loadExperimentCapillariesData_ForSeries();
			
			if (exp.loadKymographs()) {	
				System.out.println((index+1) + " - "+ exp.getExperimentFileName() + " " + exp.resultsSubPath);
				exp.kymosBuildFiltered( 0, 1, options.transformForLevels, options.spanDiffTop);
				runMeasurement(exp);
				exp.saveExperimentMeasures(resultsDirectory);
				long endTime2InNs = System.nanoTime();
				System.out.println("process ended - duration: "+((endTime2InNs-endTimeInNs)/ 1000000000f) + " s");
			
			}
			exp.seqKymos.closeSequence();
		}
		progress.close();
		threadRunning = false;
		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(true);
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
		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(true);
	  
    }
	

	private void runMeasurement(Experiment exp) {
	}
}
