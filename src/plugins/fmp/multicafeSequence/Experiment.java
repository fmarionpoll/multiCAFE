package plugins.fmp.multicafeSequence;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

public class Experiment {
	
	public String			filename 					= null;
	public SequenceCamData 	seqCamData 					= null;
	public SequenceKymos 	seqKymos					= null;
	
	public FileTime			fileTimeImageFirst;
	public FileTime			fileTimeImageLast;
	public long				fileTimeImageFirstMinute 	= 0;
	public long				fileTimeImageLastMinutes 	= 0;
	public int				number_of_frames 			= 0;
	
	public int 				startFrame 					= 0;
	public int 				step 						= 1;
	public int 				endFrame 					= 0;
	
	public String			boxID 						= null;
	public int				col							= -1;
	public Experiment 		previousExperiment			= null;		// pointer to chain this experiment to another one before
	public Experiment 		nextExperiment 				= null;		// pointer to chain this experiment to another one after
	
	
	public boolean openSequenceAndMeasures() {
		seqCamData = new SequenceCamData();
		if (null == seqCamData.loadSequence(filename))
			return false;
		fileTimeImageFirst = seqCamData.getImageModifiedTime(0);
		fileTimeImageLast = seqCamData.getImageModifiedTime(seqCamData.seq.getSizeT()-1);
		fileTimeImageFirstMinute = fileTimeImageFirst.toMillis()/60000;
		fileTimeImageLastMinutes = fileTimeImageLast.toMillis()/60000;

		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!seqKymos.xmlLoadCapillaryTrack(seqCamData.getDirectory())) 
			return false;
		boxID = seqKymos.capillaries.boxID;

		seqCamData.xmlReadDrosoTrackDefault();
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
