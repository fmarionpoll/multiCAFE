package plugins.fmp.multicafe.sequence;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import icy.util.XMLUtil;

import plugins.fmp.multicafe.tools.Directories;
import plugins.fmp.multicafe.tools.ImageTransformTools;
import plugins.fmp.multicafe.tools.ImageTransformTools.TransformOp;




public class Experiment 
{
	public final static String 	RESULTS				= "results";
	public final static String 	BIN					= "bin_";
	
	private String			imagesDirectory			= null;
	private String			experimentDirectory		= null;
	private String			binSubDirectory			= null;
		
	public SequenceCamData 	seqCamData 				= null;
	public SequenceKymos 	seqKymos				= null;
	public Sequence 		seqBackgroundImage		= null;
	public Capillaries 		capillaries 			= new Capillaries();
	public Cages			cages 					= new Cages();
	
	public FileTime			firstImage_FileTime;
	public FileTime			lastImage_FileTime;
	
	// __________________________________________________
	
	
	public 	long			camFirstImage_Ms		= 0;
	public 	long			camLastImage_Ms			= 0;
	public 	long			camBinImage_Ms			= 0;
	
	public 	long			kymoFirstCol_Ms			= 0;
	public 	long			kymoLastCol_Ms			= 0;
	public 	long			kymoBinCol_Ms			= 60000;
	
	// _________________________________________________
	
	public String			exp_boxID 				= new String("..");
	public String			experiment				= new String("..");
	public String 			comment1				= new String("..");
	public String 			comment2				= new String("..");
	
	public int				col						= -1;
	public Experiment 		previousExperiment		= null;		// pointer to chain this experiment to another one before
	public Experiment 		nextExperiment 			= null;		// pointer to chain this experiment to another one after
	public int				experimentID 			= 0;
	
	ImageTransformTools 	tImg 					= null;
	
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
	private final static String ID_MCDROSOTRACK     = "MCdrosotrack.xml";
	
	private final static String ID_BOXID 			= "boxID";
	private final static String ID_EXPERIMENT 		= "experiment";
	private final static String ID_COMMENT1 		= "comment";
	private final static String ID_COMMENT2 		= "comment2";
	
	private final static int EXPT_DIRECTORY = 1;
	private final static int IMG_DIRECTORY = 2;
	private final static int SUB_DIRECTORY = 3;
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
		this.experimentDirectory = expDirectory;
	}
	
	public Experiment(SequenceCamData seqCamData) 
	{
		this.seqCamData = seqCamData;
		this.seqKymos   = new SequenceKymos();
		experimentDirectory = this.seqCamData.getSeqDataDirectory() + File.separator + RESULTS;
		loadFileIntervalsFromSeqCamData();
	}
	
	// ----------------------------------
	
	public String getExperimentDirectory() 
	{
		return experimentDirectory;
	}
	
	public void setExperimentDirectory(String fileName) 
	{
		experimentDirectory = getParentIf(fileName, BIN);
	}
	
	public String getKymosDirectory() 
	{
		String filename = experimentDirectory;
		if (binSubDirectory != null)
			filename += File.separator + binSubDirectory;
		return filename;
	}
	
	public void setBinSubDirectory (String bin) 
	{
		binSubDirectory = bin;
	}
	
	public String getBinSubDirectory () 
	{
		return binSubDirectory;
	}
	
	public void checkKymosDirectory() 
	{
		String kymosSubDirectory = getBinSubDirectory();
		if (kymosSubDirectory == null) {
			List<String> listTIFFlocations = getSortedListOfSubDirectoriesWithTIFF();
			if (listTIFFlocations.size() < 1)
				return;
			boolean found = false;
			for (String subDir : listTIFFlocations) 
			{
				if (subDir .contains(Experiment.BIN)) 
				{
					kymosSubDirectory = subDir;
					found = true;
					break;
				}
				if (subDir .contains(Experiment.RESULTS)) 
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
		imagesDirectory = name;
	}
	
	public String getImagesDirectory() 
	{
		return imagesDirectory;
	}
	
	public void closeExperiment() 
	{
		if (seqKymos != null) 
			seqKymos.closeSequence();
		if (seqCamData != null) 
			seqCamData.closeSequence();
		if (seqBackgroundImage != null) 
			seqBackgroundImage.close();
	}
	
	public boolean openSequenceAndMeasures(boolean loadCapillaries, boolean loadDrosoPositions) 
	{
		if (seqCamData == null) 
			seqCamData = new SequenceCamData();
		xmlLoadMCExperiment ();
		if (null == seqCamData.loadSequenceOfImages(imagesDirectory))
			return false;
		loadFileIntervalsFromSeqCamData();
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (loadCapillaries) 
		{
			xmlLoadMCCapillaries_Only();
			if (!xmlLoadMCCapillaries_Measures()) 
				return false;
		}

		if (loadDrosoPositions)
			xmlReadDrosoTrack(null);
		return true;
	}
	
	static public String getImagesDirectoryAsParentFromFileName(String filename) 
	{
		filename = getParentIf(filename, BIN);
		filename = getParentIf(filename, RESULTS);
		return filename;
	}
	
	private String getRootWithNoResultNorBinString(String directoryName) 
	{
		String name = directoryName.toLowerCase();
		while (name .contains(RESULTS) || name .contains(BIN)) 
			name = Paths.get(experimentDirectory).getParent().toString();
		return name;
	}
	
	private static String getParentIf(String filename, String filter) 
	{
		if (filename .contains(filter)) 
			filename = Paths.get(filename).getParent().toString();
		return filename;
	}
	
	private SequenceCamData loadImagesForSequenceCamData(String filename) 
	{
		imagesDirectory = getImagesDirectoryAsParentFromFileName(filename);
		if (seqCamData == null)
			seqCamData = new SequenceCamData();
		if (null == seqCamData.loadSequenceOfImages(imagesDirectory) || seqCamData.seq.getSizeT() == 0)
			seqCamData = null;
		return seqCamData;
	}
		
	public SequenceCamData openSequenceCamData() 
	{
		loadImagesForSequenceCamData(imagesDirectory);
		if (seqCamData != null) 
		{
			xmlLoadMCExperiment();
			loadFileIntervalsFromSeqCamData();
		}
		return seqCamData;
	}
	
	public void loadFileIntervalsFromSeqCamData() 
	{
		if (seqCamData != null) 
		{
			firstImage_FileTime = seqCamData.getFileTimeFromStructuredName(0);
			lastImage_FileTime = seqCamData.getFileTimeFromStructuredName(seqCamData.seq.getSizeT()-1);
			camFirstImage_Ms = firstImage_FileTime.toMillis();
			camLastImage_Ms = lastImage_FileTime.toMillis();
			camBinImage_Ms = (camLastImage_Ms - camFirstImage_Ms)/(seqCamData.seq.getSizeT()-1);
		}
	}

	public String getBinNameFromKymoFrameStep() 
	{
		return BIN + kymoBinCol_Ms/1000;
	}
	
	public List<String> getSortedListOfSubDirectoriesWithTIFF() 
	{
		HashSet <String> hSet = Directories.getDirectoriesWithFilesType (getExperimentDirectory(), ".tiff");
		List<String> list = Directories.reduceFullNameToLastDirectory(new ArrayList<String>(hSet));
		List<String> sortedNames = list.stream().sorted().collect(Collectors.toList());
		return sortedNames;
	}
	
	public String getDirectoryToSaveResults() 
	{
		Path dir = Paths.get(experimentDirectory);
		if (binSubDirectory != null) 
			dir = dir.resolve(binSubDirectory);
		String directory = dir.toAbsolutePath().toString();
		if (Files.notExists(dir))  
		{
			try 
			{
				Files.createDirectory(dir);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				System.out.println("Creating directory failed: "+ directory);
				return null;
			}
		}
		return directory;
	}
	
	public int getBinStepFromDirectoryName(String resultsPath) 
	{
		int step = -1;
		if (resultsPath.contains(BIN)) 
		{
			if (resultsPath.length() < (BIN.length() +1)) 
			{
				step = (int) kymoBinCol_Ms;
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
	
	private String findFileLocation(String xmlFileName, int first, int second, int third) 
	{
		// current directory
		String xmlFullFileName = findFileLocation1(xmlFileName, first);
		if (xmlFullFileName == null) 
			xmlFullFileName = findFileLocation1(xmlFileName, second);
		if (xmlFullFileName == null) 
			xmlFullFileName = findFileLocation1(xmlFileName, third);
		return xmlFullFileName;
	}
	
	private String findFileLocation1(String xmlFileName, int item) 
	{
		String xmlFullFileName = File.separator + xmlFileName;
		switch (item) 
		{
		case IMG_DIRECTORY:
			imagesDirectory = getRootWithNoResultNorBinString(experimentDirectory);
			xmlFullFileName = imagesDirectory + File.separator + xmlFileName;
			break;
			
		case SUB_DIRECTORY:
			// any directory (below)
			Path dirPath = Paths.get(experimentDirectory);
			List<Path> subFolders = Directories.getAllSubPaths(experimentDirectory, 1);
			if (subFolders == null)
				return null;
			List<String> resultsDirList = Directories.getSubListContainingString(subFolders, RESULTS);
			List<String> binDirList = Directories.getSubListContainingString(subFolders, BIN);
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
			xmlFullFileName = experimentDirectory + xmlFullFileName;
			break;	
		}
		
		// current directory
		
		if(xmlFullFileName != null && fileExists (xmlFullFileName))
			return xmlFullFileName;
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
		if (experimentDirectory == null && seqCamData != null) 
		{
			imagesDirectory = seqCamData.getSeqDataDirectory() ;
			experimentDirectory = imagesDirectory + File.separator + RESULTS;
		}
		return xmlLoadExperiment(getMCExperimentFileName(null));
	}
	
	private String getMCExperimentFileName(String subpath) 
	{
		if (subpath != null)
			return experimentDirectory + File.separator + subpath + File.separator + "MCexperiment.xml";
		else
			return experimentDirectory + File.separator + "MCexperiment.xml";
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
		camFirstImage_Ms= XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGEMS, -1);
		camLastImage_Ms = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGEMS, -1);
		if (camFirstImage_Ms < 0) 
			camFirstImage_Ms = XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGE, -1)*60000;
		if (camLastImage_Ms < 0)
			camLastImage_Ms = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGE, -1)*60000;

		kymoFirstCol_Ms = XMLUtil.getElementLongValue(node, ID_FIRSTKYMOCOLMS, -1); 
		kymoLastCol_Ms = XMLUtil.getElementLongValue(node, ID_LASTKYMOCOLMS, -1);
		kymoBinCol_Ms = XMLUtil.getElementLongValue(node, ID_BINKYMOCOLMS, -1); 	
		
		if (exp_boxID .contentEquals("..")) 
		{
			exp_boxID	= XMLUtil.getElementValue(node, ID_BOXID, "..");
	        experiment 	= XMLUtil.getElementValue(node, ID_EXPERIMENT, "..");
	        comment1 	= XMLUtil.getElementValue(node, ID_COMMENT1, "..");
	        comment2 	= XMLUtil.getElementValue(node, ID_COMMENT2, "..");
		}
		
		//imagesDirectory = XMLUtil.getElementValue(node, ID_IMAGESDIRECTORY, imagesDirectory);
		return true;
	}
	// TODO
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
			XMLUtil.setElementLongValue(node, ID_TIMEFIRSTIMAGEMS, camFirstImage_Ms);
			XMLUtil.setElementLongValue(node, ID_TIMELASTIMAGEMS, camLastImage_Ms);
			
			XMLUtil.setElementLongValue(node, ID_FIRSTKYMOCOLMS, kymoFirstCol_Ms); 
			XMLUtil.setElementLongValue(node, ID_LASTKYMOCOLMS, kymoLastCol_Ms);
			XMLUtil.setElementLongValue(node, ID_BINKYMOCOLMS, kymoBinCol_Ms); 	
			
			XMLUtil.setElementValue(node, ID_BOXID, exp_boxID);
	        XMLUtil.setElementValue(node, ID_EXPERIMENT, experiment);
	        XMLUtil.setElementValue(node, ID_COMMENT1, comment1);
	        XMLUtil.setElementValue(node, ID_COMMENT2, comment2);
	        
	        if (imagesDirectory == null ) 
	        	imagesDirectory = seqCamData.getSeqDataDirectory();
	        XMLUtil.setElementValue(node, ID_IMAGESDIRECTORY, imagesDirectory);

	        String tempname = getMCExperimentFileName(null) ;
	        return XMLUtil.saveDocument(doc, tempname);
		}
		return false;
	}
	
 	public boolean loadKymographs() 
 	{
		if (seqKymos == null) 
			seqKymos = new SequenceKymos();
		List<FileProperties> myList = seqKymos.loadListOfPotentialKymographsFromCapillaries(getKymosDirectory(), capillaries);
		Directories.getFilesAndTestExist(myList);
		return seqKymos.loadImagesFromList(myList, true);
	}
	
	public boolean loadDrosotrack() 
	{
		return xmlReadDrosoTrack(null);
	}
	
	public boolean loadKymos_Measures() 
	{
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!xmlLoadMCCapillaries_Measures()) 
			return false;
		return true;
	}
	
	// ----------------------------------
	
	public FileTime getFileTimeImageFirst(boolean globalValue) 
	{
		FileTime filetime = firstImage_FileTime;
		if (globalValue && previousExperiment != null)
			filetime = previousExperiment.getFileTimeImageFirst(globalValue);
		return filetime;
	}
		
	public void setFileTimeImageFirst(FileTime fileTimeImageFirst) 
	{
		this.firstImage_FileTime = fileTimeImageFirst;
	}
	
	public FileTime getFileTimeImageLast(boolean globalValue) 
	{
		FileTime filetime = lastImage_FileTime;
		if (globalValue && nextExperiment != null)
			filetime = nextExperiment.getFileTimeImageLast(globalValue);
		return filetime;
	}
	
	public void setFileTimeImageLast(FileTime fileTimeImageLast) 
	{
		this.lastImage_FileTime = fileTimeImageLast;
	}
		
	public int getSeqCamSizeT() 
	{
		int lastFrame = 0;
		if (seqCamData != null && seqCamData.seq != null)
			lastFrame = seqCamData.seq.getSizeT() -1;
		return lastFrame;
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
	
	public boolean xmlLoadMCcapillaries() 
	{
		String xmlCapillaryFileName = findFileLocation(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY, SUB_DIRECTORY, IMG_DIRECTORY);
		boolean flag1 = capillaries.xmlLoadCapillaries_Descriptors(xmlCapillaryFileName);
		boolean flag2 = capillaries.xmlLoadCapillaries_Measures2(getKymosDirectory());
		if (flag1 & flag2) 
		{
			seqKymos.seqCamDataDirectory = getKymosDirectory();
			seqKymos.loadListOfPotentialKymographsFromCapillaries(seqKymos.seqCamDataDirectory, capillaries);
		}
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
		
	public void kymosBuildFiltered(int zChannelSource, int zChannelDestination, TransformOp transformop, int spanDiff) 
	{
		int nimages = seqKymos.seq.getSizeT();
		seqKymos.seq.beginUpdate();
		
		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(spanDiff);
		tImg.setSequence(seqKymos);
		
		if (capillaries.capillariesArrayList.size() != nimages) 
			SequenceKymosUtils.transferCamDataROIStoKymo(this);
		
		for (int t= 0; t < nimages; t++) 
		{
			Capillary cap = capillaries.capillariesArrayList.get(t);
			cap.indexImage = t;
			IcyBufferedImage img = seqKymos.seq.getImage(t, zChannelSource);
			IcyBufferedImage img2 = tImg.transformImage (img, transformop);
			if (seqKymos.seq.getSizeZ(0) < (zChannelDestination+1)) 
				seqKymos.seq.addImage(t, img2);
			else
				seqKymos.seq.setImage(t, zChannelDestination, img2);
		}
		
		if (zChannelDestination == 1)
			capillaries.limitsOptions.transformForLevels = transformop;
		else
			capillaries.limitsOptions.transformForGulps = transformop;
		seqKymos.seq.dataChanged();
		seqKymos.seq.endUpdate();
	}
	
	public void setReferenceImageWithConstant (double [] pixel) 
	{
		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(0);
		Sequence seq = seqKymos.seq;
		tImg.referenceImage = new IcyBufferedImage(seq.getSizeX(), seq.getSizeY(), seq.getSizeC(), seq.getDataType_());
		IcyBufferedImage result = tImg.referenceImage;
		for (int c=0; c < seq.getSizeC(); c++) 
		{
			double [] doubleArray = Array1DUtil.arrayToDoubleArray(result.getDataXY(c), result.isSignedDataType());
			Array1DUtil.fill(doubleArray, 0, doubleArray.length, pixel[c]);
			Array1DUtil.doubleArrayToArray(doubleArray, result.getDataXY(c));
		}
		result.dataChanged();
	}
	
	public boolean xmlLoadMCCapillaries_Measures() 
	{
		return capillaries.xmlLoadCapillaries_Measures2(getKymosDirectory());
	}
	
	public boolean xmlLoadMCCapillaries_Only() {
		String xmlCapillaryFileName = findFileLocation(capillaries.getXMLNameToAppend(), EXPT_DIRECTORY, SUB_DIRECTORY, IMG_DIRECTORY);
		if (xmlCapillaryFileName == null && seqCamData != null) 
		{
			return xmlLoadOldCapillaries();
		}
		boolean flag = capillaries.xmlLoadCapillaries_Descriptors(xmlCapillaryFileName);
		if (capillaries.capillariesArrayList.size() < 1)
			flag = xmlLoadOldCapillaries();
		
		// load mccapillaries description of experiment
		if (exp_boxID .contentEquals("..")) 
		{
			exp_boxID = capillaries.desc.old_boxID;
			experiment = capillaries.desc.old_experiment;
			comment1 = capillaries.desc.old_comment1;
			comment2 = capillaries.desc.old_comment2;
		}
		return flag;
	}
	
	private boolean xmlLoadOldCapillaries() 
	{
		String filename = findFileLocation("capillarytrack.xml", IMG_DIRECTORY, EXPT_DIRECTORY, SUB_DIRECTORY);
		if (capillaries.xmlLoadOldCapillaries_Only(filename)) 
		{
			xmlSaveMCCapillaries_Only();
			try {
		        Files.delete(Paths.get(filename));
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
			return true;
		}
		filename = findFileLocation("roislines.xml", IMG_DIRECTORY, EXPT_DIRECTORY, SUB_DIRECTORY);
		if (seqCamData.xmlReadROIs(filename)) {
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
	
	// TODO
	public boolean xmlSaveMCCapillaries_Only() 
	{
		String xmlCapillaryFileName = experimentDirectory + File.separator + capillaries.getXMLNameToAppend();
		transferExpDescriptorsToCapillariesDescriptors();
		return capillaries.xmlSaveCapillaries_Descriptors(xmlCapillaryFileName);
	}
	
	public boolean xmlSaveMCCapillaries_Measures() 
	{
		return capillaries.xmlSaveCapillaries_Measures(getKymosDirectory());
	}
	
	private void transferExpDescriptorsToCapillariesDescriptors() 
	{
		if (!exp_boxID 	.equals("..")) capillaries.desc.old_boxID = exp_boxID;
		if (!experiment	.equals("..")) capillaries.desc.old_experiment = experiment;
		if (!comment1	.equals("..")) capillaries.desc.old_comment1 = comment1;
		if (!comment2	.equals("..")) capillaries.desc.old_comment2 = comment2;	
	}
	
	public boolean xmlReadRoiLineParameters(String pathname) 
	{
		if (pathname != null)  
		{
			final Document doc = XMLUtil.loadDocument(pathname);
			if (doc != null) 
				return capillaries.desc.xmlLoadCapillaryDescription(doc); 
		}
		return false;
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
		seqBackgroundImage = new Sequence(seqCamData.refImage);
		seqBackgroundImage.setName("referenceImage");
		return true;
	}
	
	public boolean saveReferenceImage() 
	{
		File outputfile = new File(getReferenceImageFullName());
		RenderedImage image = ImageUtil.toRGBImage(seqCamData.refImage);
		return ImageUtil.save(image, "jpg", outputfile);
	}
	
	private String getReferenceImageFullName() 
	{
		return experimentDirectory+File.separator+"referenceImage.jpg";
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
	
	public void orderFlyPositionsForAllCages() 
	{
		cages.orderFlyPositions();
	}

	public void xmlSaveFlyPositionsForAllCages() 
	{			
		cages.xmlWriteCagesToFileNoQuestion(getMCDrosoTrackFullName());
	}
	
	// --------------------------
	private String getMCDrosoTrackFullName() 
	{
		return experimentDirectory+File.separator+ID_MCDROSOTRACK;
	}
	
	private String getXMLDrosoTrackLocation() 
	{
		String fileName = findFileLocation(ID_MCDROSOTRACK, EXPT_DIRECTORY, SUB_DIRECTORY, IMG_DIRECTORY);
		if (fileName == null)  
			fileName = findFileLocation("drosotrack.xml", IMG_DIRECTORY, EXPT_DIRECTORY, SUB_DIRECTORY);
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
	
	public boolean xmlWriteDrosoTrackDefault() 
	{
		return cages.xmlWriteCagesToFileNoQuestion(getMCDrosoTrackFullName());
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
		seqCamData.seq.addROIs(cages.getPositionsAtT(t), false);
		seqCamData.seq.endUpdate();
	}
		
	public void saveDetRoisToPositions() 
	{
		List<ROI2D> detectedROIsList= seqCamData.seq.getROI2Ds();
		for (Cage cage : cages.cageList) 
		{
			cage.transferRoisToPositions(detectedROIsList);
		}
	}
	
}
