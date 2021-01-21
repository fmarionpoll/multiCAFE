package plugins.fmp.multicafe.sequence;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.tools.toExcel.XLSExportOptions;

public class ExperimentList {
	
	protected List<Experiment> experimentList 	= new ArrayList<Experiment> ();
	public 	int 	index0 						= 0;
	public 	int 	index1 						= 0;
	public	int		maxSizeOfCapillaryArrays 	= 0;
	public 	String 	expListResultsSubPath 		= null; 
	public int		currentExperimentIndex		= -1;
	


	public ExperimentList () {
	}
	
	public Experiment getMsColStartAndEndFromAllExperiments(XLSExportOptions options) {
		Experiment expAll = new Experiment();
		Experiment exp0 = experimentList.get(0);
	
		if (options.absoluteTime) {
			expAll.setFileTimeImageFirst(exp0.getFileTimeImageFirst(true));
			expAll.setFileTimeImageLast(exp0.getFileTimeImageLast(true));
			for (Experiment exp: experimentList) {
				if (expAll.getFileTimeImageFirst(false).compareTo(exp.getFileTimeImageFirst(true)) > 0) 
					expAll.setFileTimeImageFirst(exp.getFileTimeImageFirst(true));
				if (expAll.getFileTimeImageLast(false) .compareTo(exp.getFileTimeImageLast(true)) <0)
					expAll.setFileTimeImageLast(exp.getFileTimeImageLast(true));
			}
			expAll.camFirstImage_Ms = expAll.getFileTimeImageFirst(false).toMillis();
			expAll.camLastImage_Ms = expAll.getFileTimeImageLast(false).toMillis();	
		} 
		else {
			expAll.camFirstImage_Ms = 0;
			expAll.camLastImage_Ms = 0;
			for (Experiment exp: experimentList) {
				if (options.collateSeries && exp.previousExperiment != null)
					continue;
				double last = exp.getFileTimeImageLast(options.collateSeries).toMillis();
				double first = exp.getFileTimeImageFirst(options.collateSeries).toMillis();
				double diff = last - first;
				if (diff < 1) {
					System.out.println("error when computing FileTime difference between last and first image; set dt between images = 1 ms");
					diff = exp.seqCamData.seq.getSizeT();
				}
				if (expAll.camLastImage_Ms < diff) 
					expAll.camLastImage_Ms = (long) diff;
			}
		}
		
		expAll.kymoFirstCol_Ms = expAll.camFirstImage_Ms;
		expAll.kymoLastCol_Ms = expAll.camLastImage_Ms;
		return expAll;
	}
		
	public boolean loadAllExperiments(boolean loadCapillaries, boolean loadDrosoTrack) {
		ProgressFrame progress = new ProgressFrame("Load experiment(s) parameters");
		int nexpts = experimentList.size();
		int index = 1;
		maxSizeOfCapillaryArrays = 0;
		progress.setLength(nexpts);
		boolean flag = true;
		for (Experiment exp: experimentList) {
			progress.setMessage("Load experiment "+ index +" of "+ nexpts);
			//exp.resultsSubPath = expListResultsSubPath;
			flag &= exp.openSequenceAndMeasures(loadCapillaries, loadDrosoTrack);
			if (maxSizeOfCapillaryArrays < exp.capillaries.capillariesArrayList.size())
				maxSizeOfCapillaryArrays = exp.capillaries.capillariesArrayList.size();
			progress.incPosition();
			index++;
		}
		progress.close();
		return flag;
	}
	
	public void chainExperiments(boolean collate) {
		for (Experiment exp: experimentList) {
			if (!collate) {
				exp.previousExperiment = null;
				exp.nextExperiment = null;
				continue;
			}
			for (Experiment expi: experimentList) {
				if (expi.experimentID == exp.experimentID 
					|| !expi.experiment .equals(exp.experiment) 
					|| !expi.exp_boxID .equals(exp.exp_boxID)
					|| !expi.comment1 .equals(exp.comment1)
					|| !expi.comment2 .equals(exp.comment2)
					)
					continue;
				// same exp series: if before, insert eventually
				if (expi.camLastImage_Ms < exp.camFirstImage_Ms) {
					if (exp.previousExperiment == null)
						exp.previousExperiment = expi;
					else if (expi.camLastImage_Ms > exp.previousExperiment.camLastImage_Ms ) {
						(exp.previousExperiment).nextExperiment = expi;
						expi.previousExperiment = exp.previousExperiment;
						expi.nextExperiment = exp;
						exp.previousExperiment = expi;
					}
					continue;
				}
				// same exp series: if after, insert eventually
				if (expi.camFirstImage_Ms > exp.camLastImage_Ms) {
					if (exp.nextExperiment == null)
						exp.nextExperiment = expi;
					else if (expi.camFirstImage_Ms < exp.nextExperiment.camFirstImage_Ms ) {
						(exp.nextExperiment).previousExperiment = expi;
						expi.nextExperiment = (exp.nextExperiment);
						expi.previousExperiment = exp;
						exp.nextExperiment = expi;
					}
					continue;
				}
				// it should never arrive here
				System.out.println("error in chaining "+ exp.getExperimentDirectoryName() +" with ->" + expi.getExperimentDirectoryName());
			}
		}
	}

	public int getPositionOfExperiment(String filename) {
		int position = -1;
		if (filename != null) {
			for (int i=0; i< experimentList.size(); i++) {
				Experiment exp =  experimentList.get(i);
				String filename2 = exp.getExperimentDirectoryName();
				if (filename.compareTo(filename2) == 0) {
					position = i;
					break;
				}
			}
		}
		return position;
	}
	
	public Experiment getExperimentFromFileName(String filename) {
		Experiment exp = null;
		currentExperimentIndex = getPositionOfExperiment(filename);
		if (currentExperimentIndex >= 0) {
			if (currentExperimentIndex > getExperimentListSize()-1)
				currentExperimentIndex = getExperimentListSize()-1;
			exp = getExperimentFromList(currentExperimentIndex);
		}
		return exp;
	}
	
	// ---------------------
	
	public int getExperimentListSize() {
		return experimentList.size();
	}
	
	public void clear() {
		experimentList.clear();
	}
	
	public Experiment getExperimentFromList(int index) {
		if (index < 0)
			return null;
		if (index > experimentList.size() -1)
			index = experimentList.size() -1;
		Experiment exp = experimentList.get(index);
		if (expListResultsSubPath != null)
			exp.resultsSubPath = expListResultsSubPath;
		return exp;
	}
	
	public Experiment getCurrentExperiment() {
		return getExperimentFromList(currentExperimentIndex);
	}
	
	public Experiment addNewExperimentToList () {
		Experiment exp = new Experiment();
		experimentList.add(exp);
		return exp;
	}
	
	public int addExperiment (Experiment exp) {
		experimentList.add(exp);
		currentExperimentIndex = getExperimentListSize()-1;
		return currentExperimentIndex;
	}

	public Experiment addNewExperimentToList (String expDirectory) {
		boolean exists = false;
		String expDirectory0 = getDirectoryName(expDirectory);
		Experiment exp = null;
				
		for (int i=0; i < experimentList.size(); i++) {
			exp = experimentList.get(i);
			if (exp.getExperimentDirectoryName() .equals (expDirectory0)) {	
				exists = true;
				break;
			}
		}
		
		if (!exists) {
			exp = new Experiment(expDirectory0);
			int experimentNewID  = 0;
			for (Experiment expi: experimentList) {
				if (expi.experimentID > experimentNewID)
					experimentNewID = expi.experimentID;
			}
			exp.experimentID = experimentNewID + 1;
			experimentList.add(exp);
		}
		return exp;
	}
	
	private String getDirectoryName(String filename) {
		File f0 = new File(filename);
		String parent0 = f0.getAbsolutePath();
		if (!f0.isDirectory()) {
			Path path = Paths.get(parent0);
			parent0 = path.getParent().toString();
		}
		return parent0;
	}
	
}
