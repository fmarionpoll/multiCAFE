package plugins.fmp.multicafeSequence;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeTools.XLSExportOptions;

public class ExperimentList {
	
	public List<Experiment> experimentList = new ArrayList<Experiment> ();
	public int index0 = 0;
	public int index1 = 0;
	
	

	public ExperimentList () {
	}
	
	public Experiment getStartAndEndFromAllExperiments(XLSExportOptions options) {
		ProgressFrame progress = new ProgressFrame("Get time start and time end across all experiments");
		progress.setLength(experimentList.size());

		Experiment expglobal = new Experiment();
		if (options.absoluteTime) {
			Experiment exp0 = experimentList.get(0);
			expglobal.setFileTimeImageFirst(exp0.getFileTimeImageFirst(true));
			expglobal.setFileTimeImageLast(exp0.getFileTimeImageLast(true));
			for (Experiment exp: experimentList) {
				if (expglobal.getFileTimeImageFirst(false).compareTo(exp.getFileTimeImageFirst(true)) > 0) 
					expglobal.setFileTimeImageFirst(exp.getFileTimeImageFirst(true));
				if (expglobal.getFileTimeImageLast(false) .compareTo(exp.getFileTimeImageLast(true)) <0)
						expglobal.setFileTimeImageLast(exp.getFileTimeImageLast(true));
				progress.incPosition();
			}
			expglobal.fileTimeImageFirstMinute = expglobal.getFileTimeImageFirst(false).toMillis()/60000;
			expglobal.fileTimeImageLastMinute = expglobal.getFileTimeImageLast(false).toMillis()/60000;
		} 
		else {
			expglobal.fileTimeImageFirstMinute = 0;
			Experiment exp0 = experimentList.get(0);
			long last = exp0.getFileTimeImageLast(options.collateSeries).toMillis();
			long first = exp0.getFileTimeImageFirst(options.collateSeries).toMillis();
			if (options.t0) {
				last = last - first;
				first = 0;
			}
			expglobal.fileTimeImageLastMinute = last;
			for (Experiment exp: experimentList) {
				last = exp.getFileTimeImageLast(options.collateSeries).toMillis();
				first = exp.getFileTimeImageFirst(options.collateSeries).toMillis();
				if (options.t0) {
					last = last - first;
					first = 0;
				}
				long diff = ( last - first) /60000;
				if (expglobal.fileTimeImageLastMinute < diff) 
					expglobal.fileTimeImageLastMinute = diff;
				progress.incPosition();
			}
		}
		
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
	
	public boolean readInfosFromAllExperiments(boolean loadCapillaries, boolean loadDrosoTrack) {
		ProgressFrame progress = new ProgressFrame("Load experiment(s) parameters");
		int nexpts = experimentList.size();
		int index = 1;
		progress.setLength(nexpts);
		boolean flag = true;
		for (Experiment exp: experimentList) {
			progress.setMessage("Load experiment "+ index +" of "+ nexpts);
			
			flag &= exp.openSequenceAndMeasures(loadCapillaries, loadDrosoTrack);
			exp.xmlLoadExperiment();
			int image_size = exp.seqKymos.seq.getSizeX();
			if (image_size != 0)
				exp.step = exp.seqCamData.nTotalFrames / image_size;
			progress.incPosition();
			index++;
		}
		progress.close();
		return flag;
	}
	
	public void chainExperiments() {
		for (Experiment exp: experimentList) {
			for (Experiment expi: experimentList) {
				if (expi == exp)
					continue;
				if (!expi.boxID .equals(exp.boxID))
					continue;
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
			}
		}
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
			Path filepath = stripFilenameFromPath(filename);
			for (int i=0; i< experimentList.size(); i++) {
				Experiment exp =  experimentList.get(i);
				if (exp.seqCamData == null)
					continue;
				String filename2 = exp.seqCamData.getFileName();
				if (filename2 == null) {
					filename2 = exp.experimentFileName; 
					if (filename2 == null) {
						filename2 = exp.seqCamData.getDirectory();
						if (filename2 == null)
							continue;
					}
				}
				Path filepath2 = stripFilenameFromPath(filename2);
				if (filepath.compareTo(filepath2) == 0) {
					position = i;
					break;
				}
			}
		}
		return position;
	}
	
	private Path stripFilenameFromPath(String fileNameWithFullPath) {
		Path path = Paths.get(fileNameWithFullPath);
		return path.getParent();
	}
	
	public Experiment getExperiment(int index) {
		if (index < 0)
			return null;
		if (index > experimentList.size() -1)
			index = experimentList.size() -1;
		return experimentList.get(index);
	}
	
	public Experiment addNewExperiment () {
		Experiment exp = new Experiment();
		experimentList.add(exp);
		return exp;
	}
	
	public int addExperiment (Experiment exp) {
		experimentList.add(exp);
		return experimentList.size()-1;
	}

	public Experiment addNewExperiment (String filename) {
		boolean exists = false;
		String parent0 = getDirectoryName(filename);
		Experiment exp = null;
				
		for (int i=0; i < experimentList.size(); i++) {
			exp = experimentList.get(i);
			if (exp.experimentFileName == null	&& exp.seqCamData != null) {
				exp.experimentFileName = exp.seqCamData.getFileName();
			}
			
			if (exp.experimentFileName != null) {	
				String parent = getDirectoryName(exp.experimentFileName);
				exp.experimentFileName = parent;		
				if (parent.contains(parent0)) {
					exists = true;
					break;
				}
			}
		}
		if (!exists) {
			exp = new Experiment(filename);
			experimentList.add(exp);
		}
		return exp;
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
