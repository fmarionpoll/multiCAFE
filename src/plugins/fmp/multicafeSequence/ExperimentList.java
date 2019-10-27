package plugins.fmp.multicafeSequence;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;

public class ExperimentList {
	
	public List<Experiment> experimentList = new ArrayList<Experiment> ();
	
	public ExperimentList () {
	}
	
	public Experiment getStartAndEndFromAllExperiments() {
		ProgressFrame progress = new ProgressFrame("Get time start and time end across all experiments");
		progress.setLength(experimentList.size());

		Experiment expglobal = new Experiment();
		Experiment exp0 = experimentList.get(0);
		expglobal.fileTimeImageFirst = exp0.fileTimeImageFirst;
		expglobal.fileTimeImageLast = exp0.fileTimeImageLast;
		
		for (Experiment exp: experimentList) {
			if (expglobal.fileTimeImageFirst.compareTo(exp.fileTimeImageFirst) > 0) 
				expglobal.fileTimeImageFirst = exp.fileTimeImageFirst;
			if (expglobal.fileTimeImageLast .compareTo(exp.fileTimeImageLast) <0)
				expglobal.fileTimeImageLast = exp.fileTimeImageLast;
			if (expglobal.number_of_frames < exp.seqCamData.seq.getSizeT())
				expglobal.number_of_frames = exp.seqCamData.seq.getSizeT();
			if (exp.seqCamData.analysisEnd > exp.seqCamData.seq.getSizeT()-1)
				exp.seqCamData.analysisEnd = exp.seqCamData.seq.getSizeT()-1;
			progress.incPosition();
		}
		
		expglobal.fileTimeImageFirstMinute = expglobal.fileTimeImageFirst.toMillis()/60000;
		expglobal.fileTimeImageLastMinute = expglobal.fileTimeImageLast.toMillis()/60000;
		progress.close();
		return expglobal;
	}
	
	public boolean readInfosFromAllExperiments() {
		ProgressFrame progress = new ProgressFrame("Load experiment(s) parameters");
		progress.setLength(experimentList.size());
		boolean flag = true;
		for (Experiment exp: experimentList) {
			flag &= exp.openSequenceAndMeasures();
			progress.incPosition();
		}
		progress.close();
		return flag;
	}
	
	public boolean readInfosFromAllExperiments(boolean loadCapillaryTrack, boolean loadDrosoTrack) {
		ProgressFrame progress = new ProgressFrame("Load experiment(s) parameters");
		int nexpts = experimentList.size();
		int index = 1;
		progress.setLength(nexpts);
		boolean flag = true;
		for (Experiment exp: experimentList) {
			progress.setMessage("Load experiment "+ index +" of "+ nexpts);
			flag &= exp.openSequenceAndMeasures(loadCapillaryTrack, loadDrosoTrack);
			progress.incPosition();
			index++;
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
					if (expi.fileTimeImageLastMinute < exp.fileTimeImageFirstMinute) {
						if (exp.previousExperiment == null)
							exp.previousExperiment = expi;
						else if (expi.fileTimeImageLastMinute > exp.previousExperiment.fileTimeImageLastMinute ) {
							(exp.previousExperiment).nextExperiment = expi;
							expi.previousExperiment = exp.previousExperiment;
							expi.nextExperiment = exp;
							exp.previousExperiment = expi;
						}
						continue;
					}
					// if after, insert eventually
					if (expi.fileTimeImageFirstMinute > exp.fileTimeImageLastMinute) {
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
					System.out.println("error in chaining "+ exp.experimentFileName +" with ->" + expi.experimentFileName);
					flagOK = false;
				}
			}
		}
		return flagOK;
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

	public int getPositionOfCamFileName(String filename) {
		int position = -1;
		if (filename != null) {
			for (int i=0; i< experimentList.size(); i++) {
				Experiment exp =  experimentList.get(i);
				if (exp.seqCamData == null)
					continue;
				if (filename .contains(exp.seqCamData.getFileName())) {
					position = i;
					break;
				}
			}
		}
		return position;
	}
	
	public Experiment getExperiment(int index) {
		if (index < 0)
			return null;
		return experimentList.get(index);
	}
	
	public int addNewExperiment () {
		Experiment exp = new Experiment();
		experimentList.add(exp);
		return experimentList.size()-1;
	}
	
	public int addExperiment (Experiment exp) {
		experimentList.add(exp);
		return experimentList.size()-1;
	}

	public int addNewExperiment (String filename) {
		boolean exists = false;
		int index = -1;
		String parent0 = getDirectoryName(filename);
				
		for (int i=0; i < experimentList.size(); i++) {
			Experiment exp = experimentList.get(i);
			if (exp.experimentFileName == null && exp.seqCamData != null) {
				exp.experimentFileName = exp.seqCamData.getFileName();
			}
			
			if (exp.experimentFileName != null) {	
				String parent = getDirectoryName(exp.experimentFileName);
				exp.experimentFileName = parent;		
				if (parent.contains(parent0)) {
					exists = true;
					index = i;
					break;
				}
			}
		}
		if (!exists) {
			Experiment exp = new Experiment(filename);
			experimentList.add(exp);
			index = experimentList.size()-1;
		}
		return index;
	}
	
	String getDirectoryName(String filename) {
		File f0 = new File(filename);
		String parent0 = f0.getAbsolutePath();
		if (!f0.isDirectory()) {
			Path path = Paths.get(parent0);
			parent0 = path.getParent().toString();
		}
		return parent0;
	}
	
}
