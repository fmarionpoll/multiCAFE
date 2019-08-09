package plugins.fmp.multicafeSequence;


import java.util.ArrayList;
import icy.gui.frame.progress.ProgressFrame;

public class ExperimentList {
	
	public ArrayList<Experiment> experimentList = null;
	
	public ExperimentList () {
		experimentList = new ArrayList<Experiment> ();
	}
	
	public Experiment getStartAndEndFromAllExperiments() {
		ProgressFrame progress = new ProgressFrame("Get time start and time end across all experiments");
		progress.setLength(experimentList.size());

		Experiment expglobal = new Experiment();
		Experiment exp0 = experimentList.get(0);
		expglobal.fileTimeImageFirst = exp0.fileTimeImageFirst;
		expglobal.fileTimeImageLast = exp0.fileTimeImageLast;
		
		for (Experiment exp: experimentList) 
		{
			if (expglobal.fileTimeImageFirst.compareTo(exp.fileTimeImageFirst) > 0) 
				expglobal.fileTimeImageFirst = exp.fileTimeImageFirst;
			if (expglobal.fileTimeImageLast .compareTo(exp.fileTimeImageLast) <0)
				expglobal.fileTimeImageLast = exp.fileTimeImageLast;
			if (expglobal.number_of_frames < exp.vSequence.seq.getSizeT())
				expglobal.number_of_frames = exp.vSequence.seq.getSizeT();
			if (exp.vSequence.analysisEnd > exp.vSequence.seq.getSizeT()-1)
				exp.vSequence.analysisEnd = exp.vSequence.seq.getSizeT()-1;
			progress.incPosition();
		}
		
		expglobal.fileTimeImageFirstMinute = expglobal.fileTimeImageFirst.toMillis()/60000;
		expglobal.fileTimeImageLastMinutes = expglobal.fileTimeImageLast.toMillis()/60000;
		progress.close();
		return expglobal;
	}
	
	public boolean readInfosFromAllExperiments() {
		ProgressFrame progress = new ProgressFrame("Load experiment(s) parameters");
		progress.setLength(experimentList.size());
		
		boolean flag = true;
		for (Experiment exp: experimentList) 
		{
			boolean ok = exp.openSequenceAndMeasures();
			flag &= ok;
			progress.incPosition();
		}
		progress.close();
		return flag;
	}
	
	public boolean chainExperiments() {
		boolean flagOK = true;
		for (Experiment exp: experimentList) {
			for (Experiment expi: experimentList) {
				if (expi == exp)
					continue;
				
				if (expi.boxID .equals(exp.boxID)) {
					// if before, insert eventually
					if (expi.fileTimeImageLastMinutes < exp.fileTimeImageFirstMinute) {
						if (exp.previousExperiment == null)
							exp.previousExperiment = expi;
						else if (expi.fileTimeImageLastMinutes > exp.previousExperiment.fileTimeImageLastMinutes ) {
							(exp.previousExperiment).nextExperiment = expi;
							expi.previousExperiment = exp.previousExperiment;
							expi.nextExperiment = exp;
							exp.previousExperiment = expi;
						}
						continue;
					}
					// if after, insert eventually
					if (expi.fileTimeImageFirstMinute > exp.fileTimeImageLastMinutes) {
						if (exp.nextExperiment == null)
							exp.nextExperiment = expi;
						else if (expi.fileTimeImageFirstMinute < exp.nextExperiment.fileTimeImageFirstMinute ) {
							(exp.nextExperiment).previousExperiment = expi;
							expi.nextExperiment = (exp.nextExperiment);
							expi.previousExperiment = exp;
							exp.nextExperiment = expi;
						}
						continue;
					}
					// it should never arrive here
					System.out.println("error in chainExperiments");
					flagOK = false;
				}
			}
		}
		return flagOK;
	}
	
	public long getFirstMinute(Experiment exp) {
		long firstMinute = exp.fileTimeImageFirstMinute;
		for (Experiment expi: experimentList) {
			if (!expi.boxID .equals(exp.boxID))
				continue;
			// if before, change
			if (expi.fileTimeImageFirstMinute < firstMinute) 
				firstMinute = expi.fileTimeImageFirstMinute;
		}				
		return firstMinute;
	}
	
	public long getLastMinute(Experiment exp) {	
		long lastMinute = exp.fileTimeImageLastMinutes;
		for (Experiment expi: experimentList) {
			if (!expi.boxID .equals(exp.boxID))
				continue;
			// if before, change
			if (expi.fileTimeImageLastMinutes > lastMinute) 
				lastMinute = expi.fileTimeImageLastMinutes;
		}			
		return lastMinute;
	}
	
	public int getStackColumnPosition (Experiment exp, int col0) {
		boolean found = false;
		for (Experiment expi: experimentList) {
			if (!expi.boxID .equals(exp.boxID) || expi == exp)
				continue;
			if (expi.col < 0)
				continue;
			
			found = true;
			exp.col = expi.col;
			col0 = exp.col;
				break;
		}
		
		if (!found) {
			exp.col = col0;
		}
		return col0;
	}
}
