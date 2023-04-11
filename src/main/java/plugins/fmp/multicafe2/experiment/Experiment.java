package plugins.fmp.multicafe2.experiment;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.util.XMLUtil;

import plugins.fmp.multicafe2.tools.Directories;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.fmp.multicafe2.tools.Image.ImageTransformEnums;
import plugins.fmp.multicafe2.tools.Image.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSColumnHeader;




public class Experiment 
{
	public final static String 	RESULTS				= "results";
	public final static String 	BIN					= "bin_";
	
	private String			strImagesDirectory		= null;
	private String			strExperimentDirectory	= null;
	private String			strBinSubDirectory		= null;
		
	public SequenceCamData 	seqCamData 				= null;
	public SequenceKymos 	seqKymos				= null;
	public Sequence 		seqReference			= null;
	public Capillaries 		capillaries 			= new Capillaries();
	public Cages			cages 					= new Cages();
	
	public FileTime			firstImage_FileTime;
	public FileTime			lastImage_FileTime;
	
	// __________________________________________________
	
	public 	long			camImageFirst_ms		= -1;
	public 	long			camImageLast_ms			= -1;
	public 	long			camImageBin_ms			= -1;
	
	public 	long			kymoFirst_ms			= 0;
	public 	long			kymoLast_ms				= 0;
	public 	long			kymoBin_ms				= 60000;
	
	// _________________________________________________
	
	private String			field_boxID 			= new String("..");
	private String			field_experiment		= new String("..");
	private String 			field_comment1			= new String("..");
	private String 			field_comment2			= new String("..");
	private String 			field_strain			= new String("..");
	private String			field_sex				= new String("..");
	
	public int				col						= -1;
	public Experiment 		chainToPreviousExperiment = null;	
	public Experiment 		chainToNextExperiment	= null;	
	public long				chainImageFirst_ms 		= 0;
	public int				experimentID 			= 0;
	
	private final static String ID_VERSION			= "version"; 
	private final static String ID_VERSIONNUM		= "1.0.0"; 
	private final static String ID_TIMEFIRSTIMAGE	= "fileTimeImageFirstMinute"; 
	private final static String ID_TIMELASTIMAGE 	= "fileTimeImageLastMinute";
	
	private final static String ID_TIMEFIRSTIMAGEMS	= "fileTimeImageFirstMs"; 
	private final static String ID_TIMELASTIMAGEMS 	= "fileTimeImageLastMs";
	private final static String ID_FIRSTKYMOCOLMS	= "firstKymoColMs"; 
	private final static String ID_LASTKYMOCOLMS 	= "lastKymoColMs";
	private final static String ID_BINKYMOCOLMS 	= "binKymoColMs";	

	private final static String ID_IMAGESDIRECTORY 	= "imagesDirectory";
	private final static String ID_MCEXPERIMENT 	= "MCexperiment";
	private final static String ID_MCEXPERIMENT_XML = "MCexperiment.xml";
	private final static String ID_MCDROSOTRACK_XML = "MCdrosotrack.xml";
	
	private final static String ID_BOXID 			= "boxID";
	private final static String ID_EXPERIMENT 		= "experiment";
	private final static String ID_COMMENT1 		= "comment";
	private final static String ID_COMMENT2 		= "comment2";
	private final static String ID_STRAIN			= "strain";
	private final static String ID_SEX				= "sex";
	
	private final static int EXPT_DIRECTORY = 1;
	private final static int IMG_DIRECTORY = 2;
	private final static int BIN_DIRECTORY = 3;
	// ----------------------------------
	
	public Experiment() 
	{
		seqCamData = new SequenceCamData();
		seqKymos   = new SequenceKymos();
	}
	
	public Experiment(String expDirectory) 
	{
		seqCamData = new SequenceCamData();
		seqKymos   = new SequenceKymos();
		this.strExperimentDirectory = expDirectory;
	}
	
	public Experiment(SequenceCamData seqCamData) 
	{
		this.seqCamData = seqCamData;
		this.seqKymos   = new SequenceKymos();
		strExperimentDirectory = this.seqCamData.getImagesDirectory() + File.separator + RESULTS;
		getFileIntervalsFromSeqCamData();
		
		xmlLoadExperiment(concatenateExptDirectoryWithSubpathAndName(null, ID_MCEXPERIMENT_XML));
	}
	
	public Experiment(ExperimentDirectories eADF) 
	{
		String imgDir = null;
		if (eADF.cameraImagesList.size() > 0)
			imgDir = eADF.cameraImagesList.get(0);
		strImagesDirectory = Directories.getDirectoryFromName(imgDir);
		strExperimentDirectory = eADF.resultsDirectory;
		String binDirectory = strExperimentDirectory + File.separator + eADF.binSubDirectory;
		Path binDirectoryPath = Paths.get(binDirectory);
		Path lastSubPath = binDirectoryPath.getName(binDirectoryPath.getNameCount()-1);
		strBinSubDirectory = lastSubPath.toString();
		
		seqCamData = new SequenceCamData(eADF.cameraImagesList);
		getFileIntervalsFromSeqCamData();
		seqKymos = new SequenceKymos(eADF.kymosImagesList);
		
		xmlLoadExperiment(concatenateExptDirectoryWithSubpathAndName(null, ID_MCEXPERIMENT_XML));
	}
	
	// ----------------------------------
	
	public String getExperimentDirectory() 
	{
		return strExperimentDirectory;
	}
	
	public String toString () 
	{
		return strExperimentDirectory;
	}
	
	public void setExperimentDirectory(String fileName) 
	{
		strExperimentDirectory = ExperimentDirectories.getParentIf(fileName, BIN);
	}
	
	public String getKymosBinFullDirectory() 
	{
		String filename = strExperimentDirectory;
		if (strBinSubDirectory != null)
			filename += File.separator + strBinSubDirectory;
		return filename;
	}
	
	public void setBinSubDirectory (String bin) 
	{
		strBinSubDirectory = bin;
	}
	
	public String getBinSubDirectory () 
	{
		return strBinSubDirectory;
	}
	
	public boolean createDirectoryIfDoesNotExist(String directory) 
    {	
		Path pathDir = Paths.get(directory);
		if (Files.notExists(pathDir))  
		{
			try 
			{
				Files.createDirectory(pathDir);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				System.out.println("Creating directory failed: "+ directory);
				return false;
			}
		}
		return true;
    }
	  
	public void checkKymosDirectory(String kymosSubDirectory) 
	{
		if (kymosSubDirectory == null) 
		{
			List<String> listTIFFlocations = Directories.getSortedListOfSubDirectoriesWithTIFF(getExperimentDirectory());
			if (listTIFFlocations.size() < 1)
				return;
			boolean found = false;
			for (String subDir : listTIFFlocations) 
			{
				String test = subDir.toLowerCase();
				if (test.contains(Experiment.BIN)) 
				{
					kymosSubDirectory = subDir;
					found = true;
					break;
				}
				if (test.contains(Experiment.RESULTS)) 
				{
					found = true;
					break;
				}
			}
			if (!found) 
			{
				int lowest = getBinStepFromDirectoryName( listTIFFlocations.get(0)) + 1;
				for (String subDir: listTIFFlocations) 
				{
					int val = getBinStepFromDirectoryName( subDir);
					if (val < lowest) 
					{
						lowest = val;
						kymosSubDirectory = subDir;
					}
				}
			}
		}
		setBinSubDirectory(kymosSubDirectory);
	}
		
	public void setImagesDirectory(String name) 
	{
		strImagesDirectory = name;
	}
	
	public String getImagesDirectory() 
	{
		return strImagesDirectory;
	}
	
	public void closeSequences() 
	{
		if (seqKymos != null) 
			seqKymos.closeSequence();
		if (seqCamData != null) 
			seqCamData.closeSequence();
		if (seqReference != null) 
			seqReference.close();
	}
	
	public boolean openMeasures(boolean loadCapillaries, boolean loadDrosoPositions) 
	{
		if (seqCamData == null) 
			seqCamData = new SequenceCamData();
		xmlLoadMCExperiment ();
		
		getFileIntervalsFromSeqCamData();
		
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (loadCapillaries) 
		{
			xmlLoadMCCapillaries_Only();
			if (!capillaries.xmlLoadCapillaries_Measures(getKymosBinFullDirectory())) 
				return false;
		}

		if (loadDrosoPositions)
			xmlReadDrosoTrack(null);
		return true;
	}
	
	private String getRootWithNoResultNorBinString(String directoryName) 
	{
		String name = directoryName.toLowerCase();
		while (name .contains(RESULTS) || name .contains(BIN)) 
			name = Paths.get(strExperimentDirectory).getParent().toString();
		return name;
	}
	
	private SequenceCamData loadImagesForSequenceCamData(String filename) 
	{
		strImagesDirectory = ExperimentDirectories.getImagesDirectoryAsParentFromFileName(filename);			
		List<String> imagesList = ExperimentDirectories.getV2ImagesListFromPath(strImagesDirectory);
		imagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(imagesList, "jpg");
		if (imagesList.size() < 1) 
		{
			seqCamData = null;
		} 
		else 
		{
			seqCamData = new SequenceCamData();
			seqCamData.setImagesList(imagesList);
			seqCamData.attachSequence(seqCamData.loadSequenceFromImagesList(imagesList));
		}
		return seqCamData;
	}
	
	public boolean loadCamDataImagesAndCapillaries()
	{	
		if (seqCamData != null) 
			seqCamData.loadImages();

		xmlLoadMCCapillaries_Only();
		capillaries.transferCapillaryRoiToSequence(seqCamData.seq);
		
		return (seqCamData != null && seqCamData.seq != null);
	}
	
	public boolean loadKymosImages() 
	{
		if (seqKymos != null)
			seqKymos.loadImages();
		return (seqKymos != null && seqKymos.seq != null);
	}
		
	public SequenceCamData openSequenceCamData() 
	{
		loadImagesForSequenceCamData(strImagesDirectory);
		if (seqCamData != null) 
		{
			xmlLoadMCExperiment();
			getFileIntervalsFromSeqCamData();
		}
		return seqCamData;
	}
	
	public void getFileIntervalsFromSeqCamData() 
	{
		if (seqCamData != null && (camImageFirst_ms < 0 || camImageLast_ms < 0 || camImageBin_ms < 0))
		{
			loadFileIntervalsFromSeqCamData();
		}
	}
	
	public void loadFileIntervalsFromSeqCamData() 
	{
		if (seqCamData != null)
		{	
			seqCamData.setImagesDirectory(strImagesDirectory);
			firstImage_FileTime = seqCamData.getFileTimeFromStructuredName(0);
			lastImage_FileTime = seqCamData.getFileTimeFromStructuredName(seqCamData.nTotalFrames-1);
			if (firstImage_FileTime != null && lastImage_FileTime != null)
			{
				camImageFirst_ms = firstImage_FileTime.toMillis();
				camImageLast_ms = lastImage_FileTime.toMillis();
				camImageBin_ms = (camImageLast_ms - camImageFirst_ms)/(seqCamData.nTotalFrames-1);
				if (camImageBin_ms == 0)
					System.out.println("error / file interval size");
			}
			else
			{
				System.out.println("error / file intervals of " + seqCamData.getImagesDirectory());
			}
		}
	}

	public String getBinNameFromKymoFrameStep() 
	{
		return BIN + kymoBin_ms/1000;
	}
	
	public String getDirectoryToSaveResults() 
	{
		Path dir = Paths.get(strExperimentDirectory);
		if (strBinSubDirectory != null) 
			dir = dir.resolve(strBinSubDirectory);
		String directory = dir.toAbsolutePath().toString();
		if (!createDirectoryIfDoesNotExist(directory))
			directory = null;
		return directory;
	}
	
	public int getBinStepFromDirectoryName(String resultsPath) 
	{
		int step = -1;
		if (resultsPath.contains(BIN)) 
		{
			if (resultsPath.length() < (BIN.length() +1)) 
			{
				step = (int) kymoBin_ms;
			} 
			else 
			{
				step = Integer.valueOf(resultsPath.substring(BIN.length()))*1000;
			}
		}
		return step;
	}

	public String getSubName(Path path, int subnameIndex) 
	{
		String name = "-";
		if (path.getNameCount() >= subnameIndex)
			name = path.getName(path.getNameCount() -subnameIndex).toString();
		return name;
	}
	
	private String findFile_3Locations(String xmlFileName, int first, int second, int third) 
	{
		// current directory
		String xmlFullFileName = findFile_1Location(xmlFileName, first);
		if (xmlFullFileName == null) 
			xmlFullFileName = findFile_1Location(xmlFileName, second);
		if (xmlFullFileName == null) 
			xmlFullFileName = findFile_1Location(xmlFileName, third);
		return xmlFullFileName;
	}
	
	private String findFile_1Location(String xmlFileName, int item) 
	{
		String xmlFullFileName = File.separator + xmlFileName;
		switch (item) 
		{
		case IMG_DIRECTORY:
			strImagesDirectory = getRootWithNoResultNorBinString(strExperimentDirectory);
			xmlFullFileName = strImagesDirectory + File.separator + xmlFileName;
			break;
			
		case BIN_DIRECTORY:
			// any directory (below)
			Path dirPath = Paths.get(strExperimentDirectory);
			List<Path> subFolders = Directories.getAllSubPathsOfDirectory(strExperimentDirectory, 1);
			if (subFolders == null)
				return null;
			List<String> resultsDirList = Directories.getPathsContainingString(subFolders, RESULTS);
			List<String> binDirList = Directories.getPathsContainingString(subFolders, BIN);
			resultsDirList.addAll(binDirList);
			for (String resultsSub : resultsDirList) 
			{
				Path dir = dirPath.resolve(resultsSub+ File.separator + xmlFileName);
				if (Files.notExists(dir))
					continue;
				xmlFullFileName = dir.toAbsolutePath().toString();	
				break;
			}
			break;
			
		case EXPT_DIRECTORY:
		default:
			xmlFullFileName = strExperimentDirectory + xmlFullFileName;
			break;	
		}
		
		// current directory
		if(xmlFullFileName != null && fileExists (xmlFullFileName)) 
		{
			if (item == IMG_DIRECTORY) {
				strImagesDirectory = getRootWithNoResultNorBinString(strExperimentDirectory);
				ExperimentDirectories.moveAndRename(xmlFileName, strImagesDirectory, xmlFileName,strExperimentDirectory);
				xmlFullFileName = strExperimentDirectory + xmlFullFileName;
			}
			return xmlFullFileName;
		}
		return null;
	}
	
	private boolean fileExists (String fileName) 
	{
		File f = new File(fileName);
		return (f.exists() && !f.isDirectory()); 
	}

	// -------------------------------
	
	public boolean xmlLoadMCExperiment () 
	{
		if (strExperimentDirectory == null && seqCamData != null) 
		{
			strImagesDirectory = seqCamData.getImagesDirectory() ;
			strExperimentDirectory = strImagesDirectory + File.separator + RESULTS;
		}
		boolean found = xmlLoadExperiment(concatenateExptDirectoryWithSubpathAndName(null, ID_MCEXPERIMENT_XML));
		return found;
	}
	
	private String concatenateExptDirectoryWithSubpathAndName(String subpath, String name) 
	{
		if (subpath != null)
			return strExperimentDirectory + File.separator + subpath + File.separator + name;
		else
			return strExperimentDirectory + File.separator + name;
	}
	
	private boolean xmlLoadExperiment (String csFileName) 
	{	
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc == null)
			return false;
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_MCEXPERIMENT);
		if (node == null)
			return false;

		String version = XMLUtil.getElementValue(node, ID_VERSION, ID_VERSIONNUM);
		if (!version .equals(ID_VERSIONNUM))
			return false;
		camImageFirst_ms = XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGEMS, 0);
		camImageLast_ms = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGEMS, 0);
		if (camImageLast_ms <= 0) 
		{
			camImageFirst_ms = XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGE, 0)*60000;
			camImageLast_ms = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGE, 0)*60000;
		}

		kymoFirst_ms = XMLUtil.getElementLongValue(node, ID_FIRSTKYMOCOLMS, -1); 
		kymoLast_ms = XMLUtil.getElementLongValue(node, ID_LASTKYMOCOLMS, -1);
		kymoBin_ms = XMLUtil.getElementLongValue(node, ID_BINKYMOCOLMS, -1); 	
		
		ugly_checkOffsetValues();
		
		if (field_boxID != null && field_boxID .contentEquals("..")) 
		{
			field_boxID		= XMLUtil.getElementValue(node, ID_BOXID, "..");
	        field_experiment= XMLUtil.getElementValue(node, ID_EXPERIMENT, "..");
	        field_comment1 	= XMLUtil.getElementValue(node, ID_COMMENT1, "..");
	        field_comment2 	= XMLUtil.getElementValue(node, ID_COMMENT2, "..");
	        field_strain 	= XMLUtil.getElementValue(node, ID_STRAIN, "..");
	        field_sex 		= XMLUtil.getElementValue(node, ID_SEX, "..");
		}
		return true;
	}
	
	private void ugly_checkOffsetValues()
	{
		if (camImageFirst_ms < 0)
			camImageFirst_ms = 0;
		if (camImageLast_ms < 0)
			camImageLast_ms = 0;
		if (kymoFirst_ms < 0)
			kymoFirst_ms = 0; 
		if (kymoLast_ms < 0)
			kymoLast_ms = 0;
		
		if (kymoBin_ms < 0)
			kymoBin_ms = 60000;
	}
	
	public boolean xmlSaveMCExperiment () 
	{
		final Document doc = XMLUtil.createDocument(true);
		if (doc != null) 
		{
			Node xmlRoot = XMLUtil.getRootElement(doc, true);
			Node node = XMLUtil.setElement(xmlRoot, ID_MCEXPERIMENT);
			if (node == null)
				return false;
			
			XMLUtil.setElementValue(node, ID_VERSION, ID_VERSIONNUM);
			XMLUtil.setElementLongValue(node, ID_TIMEFIRSTIMAGEMS, camImageFirst_ms);
			XMLUtil.setElementLongValue(node, ID_TIMELASTIMAGEMS, camImageLast_ms);
			
			XMLUtil.setElementLongValue(node, ID_FIRSTKYMOCOLMS, kymoFirst_ms); 
			XMLUtil.setElementLongValue(node, ID_LASTKYMOCOLMS, kymoLast_ms);
			XMLUtil.setElementLongValue(node, ID_BINKYMOCOLMS, kymoBin_ms); 	
			
			XMLUtil.setElementValue(node, ID_BOXID, field_boxID);
	        XMLUtil.setElementValue(node, ID_EXPERIMENT, field_experiment);
	        XMLUtil.setElementValue(node, ID_COMMENT1, field_comment1);
	        XMLUtil.setElementValue(node, ID_COMMENT2, field_comment2);
	        XMLUtil.setElementValue(node, ID_STRAIN, field_strain);
	        XMLUtil.setElementValue(node, ID_SEX, field_sex);
	        
	        if (strImagesDirectory == null ) 
	        	strImagesDirectory = seqCamData.getImagesDirectory();
	        XMLUtil.setElementValue(node, ID_IMAGESDIRECTORY, strImagesDirectory);

	        String tempname = concatenateExptDirectoryWithSubpathAndName(null, ID_MCEXPERIMENT_XML) ;
	        return XMLUtil.saveDocument(doc, tempname);
		}
		return false;
	}
	
 	public boolean loadKymographs() 
 	{
		if (seqKymos == null) 
			seqKymos = new SequenceKymos();
		List<ImageFileDescriptor> myList = seqKymos.loadListOfPotentialKymographsFromCapillaries(getKymosBinFullDirectory(), capillaries);
		ImageFileDescriptor.getExistingFileNames(myList);
		return seqKymos.loadImagesFromList(myList, true);
	}
		
	public boolean loadDrosotrack() 
	{
		return xmlReadDrosoTrack(null);
	}
	
	// ----------------------------------
	
	public Experiment getFirstChainedExperiment(boolean globalValue) 
	{
		Experiment exp = this;
		if (globalValue && chainToPreviousExperiment != null)
			exp = chainToPreviousExperiment.getFirstChainedExperiment(globalValue);
		return exp;
	}
		
	public Experiment getLastChainedExperiment(boolean globalValue) 
	{
		Experiment exp = this;
		if (globalValue && chainToNextExperiment != null)
			exp = chainToNextExperiment.getLastChainedExperiment(globalValue);
		return exp;
	}
	
	public void setFileTimeImageFirst(FileTime fileTimeImageFirst) 
	{
		this.firstImage_FileTime = fileTimeImageFirst;
	}
		
	public void setFileTimeImageLast(FileTime fileTimeImageLast) 
	{
		this.lastImage_FileTime = fileTimeImageLast;
	}
		
	public int getSeqCamSizeT() 
	{
		int lastFrame = 0;
		if (seqCamData != null )
			lastFrame = seqCamData.nTotalFrames -1;
		return lastFrame;
	}
	
	public String getExperimentField(EnumXLSColumnHeader fieldEnumCode)
	{
		String strField = null;
		switch (fieldEnumCode)
		{
		case EXP_STIM:
			strField = field_comment1;
			break;
		case EXP_CONC:
			strField = field_comment2;
			break;
		case EXP_EXPT:
			strField = field_experiment;
			break;
		case EXP_BOXID:
			strField = field_boxID;
			break;
		case EXP_STRAIN:
			strField = field_strain;
			break;
		case EXP_SEX:
			strField = field_sex;
			break;
		default:
			break;
		}
		return strField;
	}
	
	public void getFieldValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList)
	{
		switch (fieldEnumCode)
		{
		case EXP_STIM:
		case EXP_CONC:
		case EXP_EXPT:
		case EXP_BOXID:
		case EXP_STRAIN:
		case EXP_SEX:
			addValue(getExperimentField(fieldEnumCode), textList);
			break;
		case CAP_STIM:
		case CAP_CONC:
			addCapillariesValues(fieldEnumCode, textList);
			break;
		default:
			break;
		}
	}
	
	private void addCapillariesValues(EnumXLSColumnHeader fieldEnumCode, List<String> textList)
	{
		if (capillaries.capillariesList.size() == 0)
			xmlLoadMCCapillaries_Only();
		for (Capillary cap:  capillaries.capillariesList) 
			addValue(cap.getCapillaryField(fieldEnumCode), textList);
	}
	
	private void addValue(String text, List<String> textList) {
		if (!isFound(text, textList))
			textList.add(text);
	}
	
	private boolean isFound (String pattern, List<String> names) 
	{
		boolean found = false;
		if (names.size() > 0) 
		{
			for (String name: names) 
			{
				found = name.equals(pattern);
				if (found)
					break;
			}
		}
		return found;
	}
	
	public boolean replaceExperimentFieldIfEqualOld (EnumXLSColumnHeader fieldEnumCode, String oldValue, String newValue)
	{
		boolean flag = getExperimentField(fieldEnumCode).equals (oldValue) ;
		if (flag) {
			setExperimentFieldNoTest(fieldEnumCode, newValue);
		}
		return flag;
	}
	
	public void copyExperimentFields(Experiment expSource) {
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_BOXID, expSource.getExperimentField(EnumXLSColumnHeader.EXP_BOXID));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_EXPT, expSource.getExperimentField(EnumXLSColumnHeader.EXP_EXPT));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STIM, expSource.getExperimentField(EnumXLSColumnHeader.EXP_STIM));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_CONC, expSource.getExperimentField(EnumXLSColumnHeader.EXP_CONC));	
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_STRAIN, expSource.getExperimentField(EnumXLSColumnHeader.EXP_STRAIN));
		setExperimentFieldNoTest(EnumXLSColumnHeader.EXP_SEX, expSource.getExperimentField(EnumXLSColumnHeader.EXP_SEX));
	}
	
	public void setExperimentFieldNoTest (EnumXLSColumnHeader fieldEnumCode, String newValue)
	{
		switch (fieldEnumCode)
		{
		case EXP_STIM:
			field_comment1 = newValue;
			break;
		case EXP_CONC:
			field_comment2  = newValue;
			break;
		case EXP_EXPT:
			field_experiment = newValue;
			break;
		case EXP_BOXID:
			field_boxID  = newValue; 
			break;
		case EXP_STRAIN:
			field_strain  = newValue; 
			break;
		case EXP_SEX:
			field_sex  = newValue; 
			break;
		default:
			break;
		}
	}
	
	public void replaceFieldValue(EnumXLSColumnHeader fieldEnumCode, String oldValue, String newValue) 
	{
		switch (fieldEnumCode)
		{
		case EXP_STIM:
		case EXP_CONC:
		case EXP_EXPT:
		case EXP_BOXID:
		case EXP_STRAIN:
		case EXP_SEX:
			replaceExperimentFieldIfEqualOld(fieldEnumCode, oldValue, newValue);
			break;
		case CAP_STIM:
		case CAP_CONC:
			if(replaceCapillariesValuesIfEqualOld(fieldEnumCode, oldValue, newValue));
				xmlSaveMCCapillaries_Only();
			break;
		default:
			break;
		}
		xmlSaveMCExperiment();
	}
	
	private boolean replaceCapillariesValuesIfEqualOld(EnumXLSColumnHeader fieldEnumCode, String oldValue, String newValue)
	{
		if (capillaries.capillariesList.size() == 0)
			xmlLoadMCCapillaries_Only();
		boolean flag = false;
		for (Capillary cap:  capillaries.capillariesList) 
		{
			if (cap.getCapillaryField(fieldEnumCode) .equals(oldValue))
			{
				cap.setCapillaryField(fieldEnumCode, newValue);
				flag = true;
			}
		}
		return flag;
	}
	
	// --------------------------------------------
	
	public boolean adjustCapillaryMeasuresDimensions() 
	{
		if (seqKymos.imageWidthMax < 1) 
		{
			seqKymos.imageWidthMax = seqKymos.seq.getSizeX();
			if (seqKymos.imageWidthMax < 1)
				return false;
		}
		int imageWidth = seqKymos.imageWidthMax;
		capillaries.adjustToImageWidth(imageWidth);
		seqKymos.seq.removeAllROI();
		seqKymos.transferCapillariesMeasuresToKymos(capillaries);
		return true;
	}
	
	public boolean cropCapillaryMeasuresDimensions() 
	{
		if (seqKymos.imageWidthMax < 1) 
		{
			seqKymos.imageWidthMax = seqKymos.seq.getSizeX();
			if (seqKymos.imageWidthMax < 1)
				return false;
		}
		int imageWidth = seqKymos.imageWidthMax;
		capillaries.cropToImageWidth(imageWidth);
		seqKymos.seq.removeAllROI();
		seqKymos.transferCapillariesMeasuresToKymos(capillaries);
		return true;
	}
	
	public boolean xmlLoadMCCapillaries() 
	{
		String xmlCapillaryFileName = findFile_3Locations(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY, BIN_DIRECTORY, IMG_DIRECTORY);
		boolean flag1 = capillaries.xmlLoadCapillaries_Descriptors(xmlCapillaryFileName);
		String kymosImagesDirectory = getKymosBinFullDirectory();
		boolean flag2 = capillaries.xmlLoadCapillaries_Measures(kymosImagesDirectory);
		if (flag1 & flag2) 
			seqKymos.loadListOfPotentialKymographsFromCapillaries(kymosImagesDirectory, capillaries);
		return flag1 & flag2;
	}
	
	public boolean saveExperimentMeasures(String directory) 
	{
		boolean flag = false;
		if (seqKymos != null && seqKymos.seq != null) 
		{
			seqKymos.validateRois();
			seqKymos.transferKymosRoisToCapillaries_Measures(capillaries);
			flag = capillaries.xmlSaveCapillaries_Measures(directory);
		}
		return flag;
	}
	
	public void kymosBuildFiltered01(int zChannelSource, int zChannelDestination, ImageTransformEnums transformop1, int spanDiff) 
	{
		int nimages = seqKymos.seq.getSizeT();
		seqKymos.seq.beginUpdate();

		ImageTransformInterface transform = transformop1.getFunction();
		if (transform == null)
			return;
		
		if (capillaries.capillariesList.size() != nimages) 
			SequenceKymosUtils.transferCamDataROIStoKymo(this);
		
		for (int t= 0; t < nimages; t++) 
		{
			Capillary cap = capillaries.capillariesList.get(t);
			cap.indexKymograph = t;
			IcyBufferedImage img = seqKymos.getSeqImage(t, zChannelSource);
			IcyBufferedImage img2 = transform.getTransformedImage (img, null);
			if (seqKymos.seq.getSizeZ(0) < (zChannelDestination+1)) 
				seqKymos.seq.addImage(t, img2);
			else
				seqKymos.seq.setImage(t, zChannelDestination, img2);
		}
		
		seqKymos.seq.dataChanged();
		seqKymos.seq.endUpdate();
	}
	
	public boolean xmlLoadMCCapillaries_Only() {
		String xmlCapillaryFileName = findFile_3Locations(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY, BIN_DIRECTORY, IMG_DIRECTORY);
		if (xmlCapillaryFileName == null && seqCamData != null) 
		{
			return xmlLoadOldCapillaries();
		}
		boolean flag = capillaries.xmlLoadCapillaries_Descriptors(xmlCapillaryFileName);
		if (capillaries.capillariesList.size() < 1)
			flag = xmlLoadOldCapillaries();
		
		// load MCcapillaries description of experiment
		if (field_boxID .contentEquals("..")
				&& field_experiment.contentEquals("..") 
				&& field_comment1.contentEquals("..")
				&& field_comment2.contentEquals("..")
				&& field_sex.contentEquals("..")
				&& field_strain.contentEquals("..")) 
		{
			field_boxID = capillaries.desc.old_boxID;
			field_experiment = capillaries.desc.old_experiment;
			field_comment1 = capillaries.desc.old_comment1;
			field_comment2 = capillaries.desc.old_comment2;
			field_sex = capillaries.desc.old_sex;
			field_strain = capillaries.desc.old_strain;
		}
		return flag;
	}
	
	private boolean xmlLoadOldCapillaries() 
	{
		String filename = findFile_3Locations("capillarytrack.xml", IMG_DIRECTORY, EXPT_DIRECTORY, BIN_DIRECTORY);
		if (capillaries.xmlLoadOldCapillaries_Only(filename)) 
		{
			xmlSaveMCCapillaries_Only();
			xmlSaveMCCapillaries_Measures();
			try {
		        Files.delete(Paths.get(filename));
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
			return true;
		}
		filename = findFile_3Locations("roislines.xml", IMG_DIRECTORY, EXPT_DIRECTORY, BIN_DIRECTORY);
		if (xmlReadCamDataROIs(filename)) 
		{
			xmlReadRoiLineParameters(filename);
			try {
		        Files.delete(Paths.get(filename));
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
			return true;
		}
		return false;
	}
	
	private boolean xmlReadCamDataROIs(String fileName) 
	{
		Sequence seq = seqCamData.seq;
		if (fileName != null)  
		{
			final Document doc = XMLUtil.loadDocument(fileName);
			if (doc != null) 
			{
				List<ROI2D> seqRoisList = seq.getROI2Ds(false);
				List<ROI2D> newRoisList = ROI2DUtilities.loadROIsFromXML(doc);
				ROI2DUtilities.mergeROIsListNoDuplicate(seqRoisList, newRoisList, seq);
				seq.removeAllROI();
				seq.addROIs(seqRoisList, false);
				return true;
			}
		}
		return false;
	}
	
	private boolean xmlReadRoiLineParameters(String filename) 
	{
		if (filename != null)  
		{
			final Document doc = XMLUtil.loadDocument(filename);
			if (doc != null) 
				return capillaries.desc.xmlLoadCapillaryDescription(doc); 
		}
		return false;
	}
	
	public boolean xmlSaveMCCapillaries_Only() 
	{
		String xmlCapillaryFileName = strExperimentDirectory + File.separator + capillaries.getXMLNameToAppend();
		transferExpDescriptorsToCapillariesDescriptors();
		return capillaries.xmlSaveCapillaries_Descriptors(xmlCapillaryFileName);
	}
	
	public boolean xmlSaveMCCapillaries_Measures() 
	{
		return capillaries.xmlSaveCapillaries_Measures(getKymosBinFullDirectory());
	}
	
	// ----------------------------------
	
	private void transferExpDescriptorsToCapillariesDescriptors() 
	{
		capillaries.desc.old_boxID = field_boxID;
		capillaries.desc.old_experiment = field_experiment;
		capillaries.desc.old_comment1 = field_comment1;
		capillaries.desc.old_comment2 = field_comment2;	
		capillaries.desc.old_strain = field_strain;
		capillaries.desc.old_sex = field_sex;
	}

	public boolean loadReferenceImage() 
	{
		BufferedImage image = null;
		File inputfile = new File(getReferenceImageFullName());
		boolean exists = inputfile.exists();
		if (!exists) 
			return false;	
		image = ImageUtil.load(inputfile, true);
		if (image == null) {
			System.out.println("image not loaded / not found");
			return false;
		}			
		seqCamData.refImage =  IcyBufferedImage.createFrom(image);
		seqReference = new Sequence(seqCamData.refImage);
		seqReference.setName("referenceImage");
		return true;
	}
	
	public boolean saveReferenceImage(IcyBufferedImage referenceImage) 
	{
		File outputfile = new File(getReferenceImageFullName());
		RenderedImage image = ImageUtil.toRGBImage(referenceImage);
		return ImageUtil.save(image, "jpg", outputfile);
	}
	
	private String getReferenceImageFullName() 
	{
		return strExperimentDirectory+File.separator+"referenceImage.jpg";
	}
	
	public void cleanPreviousDetectedFliesROIs() 
	{
		ArrayList<ROI2D> list = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: list) 
		{
			if (roi.getName().contains("det")) 
				seqCamData.seq.removeROI(roi);
		}
	}
	
	// --------------------------
	public String getMCDrosoTrackFullName() 
	{
		return strExperimentDirectory+File.separator+ID_MCDROSOTRACK_XML;
	}
	
	private String getXMLDrosoTrackLocation() 
	{
		String fileName = findFile_3Locations(ID_MCDROSOTRACK_XML, EXPT_DIRECTORY, BIN_DIRECTORY, IMG_DIRECTORY);
		if (fileName == null)  
			fileName = findFile_3Locations("drosotrack.xml", IMG_DIRECTORY, EXPT_DIRECTORY, BIN_DIRECTORY);
		return fileName;
	}
	
	public boolean xmlReadDrosoTrack(String filename) 
	{
		if (filename == null) 
		{
			filename = getXMLDrosoTrackLocation();
			if (filename == null)
				return false;
		}
		return cages.xmlReadCagesFromFileNoQuestion(filename, this);
	}
	
	// --------------------------
		
	public void updateROIsAt(int t) 
	{
		seqCamData.seq.beginUpdate();
		List<ROI2D> rois = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: rois) 
		{
		    if (roi.getName().contains("det") ) 
		    	seqCamData.seq.removeROI(roi);
		}
		seqCamData.seq.addROIs(cages.getPositionsAsListOfROI2DRectanglesAtT(t), false);
		seqCamData.seq.endUpdate();
	}
		
	public void saveDetRoisToPositions() 
	{
		List<ROI2D> detectedROIsList= seqCamData.seq.getROI2Ds();
		for (Cage cage : cages.cagesList) 
		{
			cage.transferRoisToPositions(detectedROIsList);
		}
	}
	
	
	
}
