package plugins.fmp.multicafeSequence;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;

public class Experiment {
	
	public String			experimentFileName			= null;
	public SequenceCamData 	seqCamData 					= null;
	public SequenceKymos 	seqKymos					= null;
	
	public FileTime			fileTimeImageFirst;
	public FileTime			fileTimeImageLast;
	public long				fileTimeImageFirstMinute 	= 0;
	public long				fileTimeImageLastMinute 	= 0;
	public int				number_of_frames 			= 0;
	
	public int 				startFrame 					= 0;
	public int 				step 						= 1;
	public int 				endFrame 					= 0;
	
	public String			boxID 						= null;
	public int				col							= -1;
	public Experiment 		previousExperiment			= null;		// pointer to chain this experiment to another one before
	public Experiment 		nextExperiment 				= null;		// pointer to chain this experiment to another one after
	
	// ----------------------------------
	
	public Experiment() {
		seqCamData = new SequenceCamData();
		seqKymos   = new SequenceKymos();
	}
	
	public Experiment(String filename) {
		seqCamData = new SequenceCamData();
		seqKymos   = new SequenceKymos();
		
		File f = new File(filename);
		String parent = f.getAbsolutePath();
		if (!f.isDirectory()) {
			Path path = Paths.get(parent);
			parent = path.getParent().toString();
		}
		this.experimentFileName = parent;
	}
	
	public Experiment(SequenceCamData seq) {
		seqCamData = seq;
		seqKymos   = new SequenceKymos();
		
		seqCamData.setParentDirectoryAsFileName() ;
		fileTimeImageFirst = seqCamData.getImageModifiedTime(0);
		fileTimeImageLast = seqCamData.getImageModifiedTime(seqCamData.seq.getSizeT()-1);
		fileTimeImageFirstMinute = fileTimeImageFirst.toMillis()/60000;
		fileTimeImageLastMinute = fileTimeImageLast.toMillis()/60000;
	}
	
	public boolean openSequenceAndMeasures() {
		seqCamData = new SequenceCamData();
		if (null == seqCamData.loadSequence(experimentFileName))
			return false;
		fileTimeImageFirst = seqCamData.getImageModifiedTime(0);
		fileTimeImageLast = seqCamData.getImageModifiedTime(seqCamData.seq.getSizeT()-1);
		fileTimeImageFirstMinute = fileTimeImageFirst.toMillis()/60000;
		fileTimeImageLastMinute = fileTimeImageLast.toMillis()/60000;
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!seqKymos.xmlLoadKymos_Measures(seqCamData.getDirectory())) 
			return false;
		boxID = seqKymos.capillaries.desc.boxID;

		seqCamData.xmlReadDrosoTrackDefault();
		return true;
	}
	
	public boolean openSequenceAndMeasures(boolean loadCapillaryTrack, boolean loadDrosoTrack) {
		if (seqCamData == null) {
			seqCamData = new SequenceCamData();
		}
		if (null == seqCamData.loadSequence(experimentFileName))
			return false;
		
		fileTimeImageFirst = seqCamData.getImageModifiedTime(0);
		fileTimeImageLast = seqCamData.getImageModifiedTime(seqCamData.seq.getSizeT()-1);
		fileTimeImageFirstMinute = fileTimeImageFirst.toMillis()/60000;
		fileTimeImageLastMinute = fileTimeImageLast.toMillis()/60000;
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (loadCapillaryTrack) {
			if (!seqKymos.xmlLoadKymos_Measures(seqCamData.getDirectory())) 
				return false;
			boxID = seqKymos.capillaries.desc.boxID;
		}
		
		if (loadDrosoTrack)
			seqCamData.xmlReadDrosoTrackDefault();
		return true;
	}
	
	public SequenceCamData openSequenceCamData(String filename) {
		this.experimentFileName = filename;
		seqCamData = new SequenceCamData();
		if (null == seqCamData.loadSequence(filename))
			return null;
		seqCamData.setParentDirectoryAsFileName() ;
		fileTimeImageFirst = seqCamData.getImageModifiedTime(0);
		fileTimeImageLast = seqCamData.getImageModifiedTime(seqCamData.seq.getSizeT()-1);
		fileTimeImageFirstMinute = fileTimeImageFirst.toMillis()/60000;
		fileTimeImageLastMinute = fileTimeImageLast.toMillis()/60000;
		return seqCamData;
	}
	
	public boolean loadKymographs() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (seqKymos.capillaries.capillariesArrayList.size() == 0) {
			if (!seqKymos.xmlLoadKymos_Measures(seqCamData.getDirectory())) 
				return false;
			boxID = seqKymos.capillaries.desc.boxID;
		}
		List<String> myList = seqKymos.loadListOfKymographsFromCapillaries(seqCamData.getDirectory());
		boolean flag = seqKymos.loadImagesFromList(myList, true);
		return flag;
	}
	
	public boolean loadDrosotrack() {
		return seqCamData.xmlReadDrosoTrackDefault();
	}
	
	public boolean loadCapillaryTrack() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!seqKymos.xmlLoadKymos_Measures(seqCamData.getDirectory())) 
			return false;
		boxID = seqKymos.capillaries.desc.boxID;
		// TODO
		return true;
	}
	
	protected String getBoxIdentificatorFromFilePath () {
		Path path = Paths.get(seqCamData.getFileName());
		String name = getSubName(path, 2); 
		return name;
	}
	
	protected String getSubName(Path path, int subnameIndex) {
		String name = "-";
		if (path.getNameCount() >= subnameIndex)
			name = path.getName(path.getNameCount() -subnameIndex).toString();
		return name;
	}
}
