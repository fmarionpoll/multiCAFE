package plugins.fmp.multicafe.sequence;

import java.io.File;
import java.nio.file.Files;
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
		
		// TODO suppress use of kymoFrameStep
		//expAll.setKymoFrameStep(exp0.getKymoFrameStep());
		expAll.setKymoFrameStep( -1);

		// make sure they have all the same step
		for (Experiment exp: experimentList) {
			if (exp.getKymoFrameStep() != expAll.getKymoFrameStep())
				exp.setKymoFrameStep(expAll.getKymoFrameStep());
		}
		
		if (options.absoluteTime) {
			expAll.setFileTimeImageFirst(exp0.getFileTimeImageFirst(true));
			expAll.setFileTimeImageLast(exp0.getFileTimeImageLast(true));
			for (Experiment exp: experimentList) {
				if (expAll.getFileTimeImageFirst(false).compareTo(exp.getFileTimeImageFirst(true)) > 0) 
					expAll.setFileTimeImageFirst(exp.getFileTimeImageFirst(true));
				if (expAll.getFileTimeImageLast(false) .compareTo(exp.getFileTimeImageLast(true)) <0)
					expAll.setFileTimeImageLast(exp.getFileTimeImageLast(true));
			}
			expAll.firstCamImage_Ms = expAll.getFileTimeImageFirst(false).toMillis();
			expAll.lastCamImage_Ms = expAll.getFileTimeImageLast(false).toMillis();	
		} 
		else {
			expAll.firstCamImage_Ms = 0;
			expAll.lastCamImage_Ms = 0;
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
				if (expAll.lastCamImage_Ms < diff) 
					expAll.lastCamImage_Ms = (long) diff;
			}
		}
		
		expAll.firstKymoCol_Ms = expAll.firstCamImage_Ms;
		expAll.lastKymoCol_Ms = expAll.lastCamImage_Ms;
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
			exp.resultsSubPath = expListResultsSubPath;
			flag &= exp.openSequenceAndMeasures(loadCapillaries, loadDrosoTrack);
			int image_size = exp.seqKymos.seq.getSizeX();
			if (image_size != 0) {
				exp.setKymoFrameStep(exp.seqCamData.nTotalFrames / image_size);
				if (exp.getKymoFrameStep() < 1) {
					System.out.println("Error: experiment with stepFrame set to 1");
					exp.setKymoFrameStep (1);
				}
			}
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
				if (expi.lastCamImage_Ms < exp.firstCamImage_Ms) {
					if (exp.previousExperiment == null)
						exp.previousExperiment = expi;
					else if (expi.lastCamImage_Ms > exp.previousExperiment.lastCamImage_Ms ) {
						(exp.previousExperiment).nextExperiment = expi;
						expi.previousExperiment = exp.previousExperiment;
						expi.nextExperiment = exp;
						exp.previousExperiment = expi;
					}
					continue;
				}
				// same exp series: if after, insert eventually
				if (expi.firstCamImage_Ms > exp.lastCamImage_Ms) {
					if (exp.nextExperiment == null)
						exp.nextExperiment = expi;
					else if (expi.firstCamImage_Ms < exp.nextExperiment.firstCamImage_Ms ) {
						(exp.nextExperiment).previousExperiment = expi;
						expi.nextExperiment = (exp.nextExperiment);
						expi.previousExperiment = exp;
						exp.nextExperiment = expi;
					}
					continue;
				}
				// it should never arrive here
				System.out.println("error in chaining "+ exp.getExperimentFileName() +" with ->" + expi.getExperimentFileName());
			}
		}
	}

	public int getPositionOfCamFileName(String filename) {
		int position = -1;
		if (filename != null) {
			Path filepath = stripFilenameFromPath(filename);
			for (int i=0; i< experimentList.size(); i++) {
				Experiment exp =  experimentList.get(i);
				if (exp.seqCamData == null)
					continue;
				String filename2 = exp.seqCamData.getSequenceFileName();
				if (filename2 == null) {
					filename2 = exp.getExperimentFileName(); 
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
	
	public Experiment getExperimentFromFileName(String filename) {
		Experiment exp = null;
		currentExperimentIndex = getPositionOfCamFileName(filename);
		if (currentExperimentIndex < 0) {
			exp = new Experiment();
			currentExperimentIndex = addExperiment(exp);
		} else {
			if (currentExperimentIndex > getSize()-1)
				currentExperimentIndex = getSize()-1;
			exp = getExperiment(currentExperimentIndex);
		}
		return exp;
	}
	
	private Path stripFilenameFromPath(String fileNameWithFullPath) {
		Path path = Paths.get(fileNameWithFullPath);		
		boolean isDirectory = Files.isDirectory(path);   // Check if it's a directory
		if (isDirectory)
			return path;
		return path.getParent();
	}
	
	// ---------------------
	
	public int getSize() {
		return experimentList.size();
	}
	
	public void clear() {
		experimentList.clear();
	}
	
	public Experiment getExperiment(int index) {
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
		return getExperiment(currentExperimentIndex);
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
			if (exp.getExperimentFileName() == null	&& exp.seqCamData != null) {
				exp.setExperimentFileName(exp.seqCamData.getSequenceFileName());
			}
			
			if (exp.getExperimentFileName() != null) {	
				String parent = getDirectoryName(exp.getExperimentFileName());
				exp.setExperimentFileName(parent);		
				if (parent.contains(parent0)) {
					exists = true;
					break;
				}
			}
		}
		if (!exists) {
			exp = new Experiment(filename);
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
