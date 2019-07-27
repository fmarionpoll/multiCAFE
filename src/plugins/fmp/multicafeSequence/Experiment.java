package plugins.fmp.multicafeSequence;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

public class Experiment {
	
	public String						filename 					= null;
	public SequenceVirtual 				vSequence 					= null;
	public ArrayList <SequencePlus> 	kymographArrayList			= null;
	
	public FileTime						fileTimeImageFirst;
	public FileTime						fileTimeImageLast;
	public long							fileTimeImageFirstMinute 	= 0;
	public long							fileTimeImageLastMinutes 	= 0;
	public int							number_of_frames 			= 0;
	
	public int 							startFrame 					= 0;
	public int 							step 						= 1;
	public int 							endFrame 					= 0;
	
	public String						boxID 						= null;
	public int							col							= -1;
	public Experiment 					previousExperiment			= null;		// pointer to chain this experiment to another one before
	public Experiment 					nextExperiment 				= null;		// pointer to chain this experiment to another one after
	
	
	public boolean openSequenceAndMeasures() {

		vSequence = new SequenceVirtual();
		if (null == vSequence.loadVirtualStackAt(filename))
			return false;
		fileTimeImageFirst = vSequence.getImageModifiedTime(0);
		fileTimeImageLast = vSequence.getImageModifiedTime(vSequence.getSizeT()-1);
		//System.out.println("read expt: "+ filename+" .....size "+ vSequence.getSizeT());
		
		fileTimeImageFirstMinute = fileTimeImageFirst.toMillis()/60000;
		fileTimeImageLastMinutes = fileTimeImageLast.toMillis()/60000;
		
		if (!vSequence.xmlReadCapillaryTrackDefault()) 
			return false;
		
		boxID = vSequence.capillaries.boxID;
		
		String directory = vSequence.getDirectory() +"\\results";
		kymographArrayList = SequencePlusUtils.openFiles(directory, vSequence.capillaries);
		vSequence.xmlReadDrosoTrackDefault();
		return true;
	}
	
	protected String getBoxIdentificatorFromFilePath () {
		Path path = Paths.get(vSequence.getFileName());
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
