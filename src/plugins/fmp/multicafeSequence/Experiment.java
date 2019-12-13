package plugins.fmp.multicafeSequence;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.ImageTransformTools;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Experiment {
	
	public String			experimentFileName			= null;
	public SequenceCamData 	seqCamData 					= null;
	public SequenceKymos 	seqKymos					= null;
	public Capillaries 		capillaries 			= new Capillaries();
	
	private FileTime		fileTimeImageFirst;
	private FileTime		fileTimeImageLast;
	public long				fileTimeImageFirstMinute 	= 0;
	public long				fileTimeImageLastMinute 	= 0;
	public int				number_of_frames 			= 0;
	
	public int 				startFrame 					= 0;
	public int 				endFrame 					= 0;
	public int 				step 						= 1;
	public long						analysisStart 			= 0;
	public long 					analysisEnd				= 99999999;
	public int 						analysisStep 			= 1;
	
	public String			boxID 						= new String("..");
	public String			experiment					= new String("..");
	public String 			comment						= new String("..");
	
	public int				col							= -1;
	public Experiment 		previousExperiment			= null;		// pointer to chain this experiment to another one before
	public Experiment 		nextExperiment 				= null;		// pointer to chain this experiment to another one after
	
	ImageTransformTools 	tImg 						= null;

	private final String ID_VERSION	= "version"; 
	private final String ID_VERSIONNUM	= "1.0.0"; 
	private final String ID_TIMEFIRSTIMAGE	= "fileTimeImageFirstMinute"; 
	private final String ID_TIMELASTIMAGE = "fileTimeImageLastMinute";
	private final String ID_NFRAMES = "number_of_frames";
	private final String ID_STARTFRAME = "startFrame";
	private final String ID_ENDFRAME = "endFrame";
	private final String ID_STEP = "step";
	private final String ID_BOXID = "boxID";
	private final String ID_EXPERIMENT = "experiment";
	private final String ID_EXPTFILENAME = "exptFileName";
	private final String ID_COMMENT = "comment";
	private final String ID_MCEXPERIMENT = "MCexperiment";
	
	
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
		experimentFileName = seqCamData.getDirectory();
		loadFileIntervalsFromSeqCamData();
	}
	
	public boolean openSequenceAndMeasures() {
		seqCamData = new SequenceCamData();
		if (null == seqCamData.loadSequence(experimentFileName))
			return false;
		loadFileIntervalsFromSeqCamData();
		
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!xmlLoadKymos_Measures(seqCamData.getDirectory())) 
			return false;

		seqCamData.xmlReadDrosoTrackDefault();
		return true;
	}
	
	public boolean openSequenceAndMeasures(boolean loadCapillaries, boolean loadDrosoPositions) {
		boolean flag = xmlLoadExperiment ();
		if (seqCamData == null) {
			seqCamData = new SequenceCamData();
		}
		if (null == seqCamData.loadSequence(experimentFileName))
			return false;
		
		loadFileIntervalsFromSeqCamData();
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (loadCapillaries) {
			if (!xmlLoadKymos_Measures(seqCamData.getDirectory())) 
				return false;
		}
		if (!flag || boxID .equals ("..")) {
			boxID = capillaries.desc.old_boxID;
			experiment = capillaries.desc.old_experiment;
			comment = capillaries.desc.old_comment;
		}
		
		if (loadDrosoPositions)
			seqCamData.xmlReadDrosoTrackDefault();
		return true;
	}
	
	public SequenceCamData openSequenceCamData(String filename) {
		this.experimentFileName = filename;
		seqCamData = new SequenceCamData();
		if (null == seqCamData.loadSequence(filename))
			return null;
		experimentFileName = filename;
		xmlLoadExperiment();

		seqCamData.setParentDirectoryAsFileName() ;
		loadFileIntervalsFromSeqCamData();
		return seqCamData;
	}
	
	public void loadFileIntervalsFromSeqCamData() {
		fileTimeImageFirst = seqCamData.getImageFileTime(0);
		fileTimeImageLast = seqCamData.getImageFileTime(seqCamData.seq.getSizeT()-1);
		fileTimeImageFirstMinute = fileTimeImageFirst.toMillis()/60000;
		fileTimeImageLastMinute = fileTimeImageLast.toMillis()/60000;
	}
	
	// TODO call it loadKymographs_Images if possible 
	
	public boolean xmlLoadExperiment () {
		String directory = experimentFileName;
		if (directory == null)
			directory = seqCamData.getDirectory();
		String csFileName = directory+ File.separator + "results" + File.separator + "MCexperiment.xml";
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) {
			Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_MCEXPERIMENT);
			if (node == null)
				return false;
			String version = XMLUtil.getElementValue(node, ID_VERSION, ID_VERSIONNUM);
			if (!version .equals(ID_VERSIONNUM))
				return false;
			fileTimeImageFirstMinute = XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGE, fileTimeImageFirstMinute);
			fileTimeImageLastMinute = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGE, fileTimeImageLastMinute);
			number_of_frames 		= XMLUtil.getElementIntValue(node, ID_NFRAMES, number_of_frames);
			startFrame 	= XMLUtil.getElementIntValue(node, ID_STARTFRAME, startFrame);
			endFrame 	= XMLUtil.getElementIntValue(node, ID_ENDFRAME, endFrame);
			step 		= XMLUtil.getElementIntValue(node, ID_STEP, step);
			boxID 		= XMLUtil.getElementValue(node, ID_BOXID, "..");
	        experiment 	= XMLUtil.getElementValue(node, ID_EXPERIMENT, "..");
	        comment 	= XMLUtil.getElementValue(node, ID_COMMENT, "..");
	        String exptName = XMLUtil.getElementValue(node, ID_EXPTFILENAME, null);
	        if (exptName != null)
	        	experimentFileName = exptName;
		}
		return true;
	}
	
	public boolean xmlSaveExperiment () {
		final Document doc = XMLUtil.createDocument(true);
		if (doc != null) {
			Node xmlRoot = XMLUtil.getRootElement(doc, true);
			Node node = XMLUtil.setElement(xmlRoot, ID_MCEXPERIMENT);
			if (node == null)
				return false;
			
			XMLUtil.setElementValue(node, ID_VERSION, ID_VERSIONNUM);
			XMLUtil.setElementLongValue(node, ID_TIMEFIRSTIMAGE, fileTimeImageFirstMinute);
			XMLUtil.setElementLongValue(node, ID_TIMELASTIMAGE, fileTimeImageLastMinute);
			XMLUtil.setElementIntValue(node, ID_NFRAMES, number_of_frames);
			XMLUtil.setElementIntValue(node, ID_STARTFRAME, startFrame);
			XMLUtil.setElementIntValue(node, ID_ENDFRAME, endFrame);
			XMLUtil.setElementIntValue(node, ID_STEP, step);
			XMLUtil.setElementValue(node, ID_BOXID, boxID);
	        XMLUtil.setElementValue(node, ID_EXPERIMENT, experiment);
	        XMLUtil.setElementValue(node, ID_COMMENT, comment);
	        if (experimentFileName == null ) 
	        	experimentFileName = seqCamData.getDirectory();
	        XMLUtil.setElementValue(node, ID_EXPTFILENAME, experimentFileName);

	        String csFileName = experimentFileName + File.separator + "results" + File.separator + "MCexperiment.xml";
	        XMLUtil.saveDocument(doc, csFileName);
		}
		return true;
	}
	
 	public boolean loadKymographs() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (capillaries.capillariesArrayList.size() == 0) {
			// TODO check if it is ok to load only the list of capillaries here
			if (!xmlLoadKymos_Measures(seqCamData.getDirectory())) 
				return false;;
		}
		List<String> myList = seqKymos.loadListOfKymographsFromCapillaries(seqCamData.getDirectory(), capillaries);
		boolean flag = seqKymos.loadImagesFromList(myList, true);
		seqKymos.transferMeasuresToKymosRois(capillaries);
		
		return flag;
	}
	
	public boolean loadDrosotrack() {
		return seqCamData.xmlReadDrosoTrackDefault();
	}
	
	public boolean loadKymos_Measures() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!xmlLoadKymos_Measures(seqCamData.getDirectory())) 
			return false;
		return true;
	}
	
	// ----------------------------------
	
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

	public FileTime getFileTimeImageFirst(boolean globalValue) {
		FileTime filetime = fileTimeImageFirst;
		if (globalValue && previousExperiment != null)
			filetime = previousExperiment.getFileTimeImageFirst(globalValue);
		return filetime;
	}
		
	public void setFileTimeImageFirst(FileTime fileTimeImageFirst) {
		this.fileTimeImageFirst = fileTimeImageFirst;
	}
	
	public FileTime getFileTimeImageLast(boolean globalValue) {
		FileTime filetime = fileTimeImageLast;
		if (globalValue && nextExperiment != null)
			filetime = nextExperiment.getFileTimeImageLast(globalValue);
		return filetime;
	}
	
	public void setFileTimeImageLast(FileTime fileTimeImageLast) {
		this.fileTimeImageLast = fileTimeImageLast;
	}
	
//	public long getLastImageTimeMinute(boolean globalValue) {
//		long fileTimeLast = fileTimeImageLastMinute;
//		if (globalValue && nextExperiment != null)
//			fileTimeLast = nextExperiment.getLastImageTimeMinute(globalValue);
//		return fileTimeLast;
//	}
//	
//	public long getFirstImageTimeMinute(boolean globalValue) {
//		long fileTimeFirst = fileTimeImageFirstMinute;
//		if (globalValue && previousExperiment != null)
//			fileTimeFirst = previousExperiment.getFirstImageTimeMinute(globalValue);
//		return fileTimeFirst;
//	}
//	
//	public long getImageTimeMinute(long t, boolean globalValue) {
//		long fileTimeFirst = fileTimeImageFirstMinute;
//		if (globalValue && previousExperiment != null)
//			fileTimeFirst = previousExperiment.getFirstImageTimeMinute(globalValue);
//		return fileTimeFirst + (t-startFrame);
//	}
	
	// -----------------------
	
	public boolean isFlyAlive(int cagenumber) {
		boolean isalive = false;
		for (Cage cage: seqCamData.cages.cageList) {
			String cagenumberString = cage.cageLimitROI.getName().substring(4);
			if (Integer.parseInt(cagenumberString) == cagenumber) {
				isalive = (cage.flyPositions.getLastIntervalAlive() > 0);
				break;
			}
		}
		return isalive;
	}
	
	public boolean isDataAvailable(int cagenumber) {
		boolean isavailable = false;
		for (Cage cage: seqCamData.cages.cageList) {
			String cagenumberString = cage.cageLimitROI.getName().substring(4);
			if (Integer.parseInt(cagenumberString) == cagenumber) {
				isavailable = true;
				break;
			}
		}
		return isavailable;
	}
	
	public boolean checkStepConsistency() {
		int imageWidth = seqKymos.imageWidthMax; 
		int len = (endFrame - startFrame + 1) / step;
		boolean isOK = true;
		if (len != imageWidth) {
			isOK = false;
			step = (endFrame - startFrame + 1)/imageWidth;
		}
		return isOK;
	}
	
	public void loadExperimentData() {
		xmlLoadExperiment();
		seqCamData.loadSequence(experimentFileName) ;
		seqCamData.setCapillariesFromCamData(capillaries);
		xmlLoadMCcapillaries(experimentFileName);
	}
	
	public void loadExperimentCamData() {
		xmlLoadExperiment();
		seqCamData.loadSequence(experimentFileName) ;
	}
	
	public void loadExperimentDataForBuildKymos() {
		xmlLoadExperiment();
		seqCamData.loadSequence(experimentFileName) ;
		xmlLoadMCcapillariesOnly(experimentFileName);
	}
	
	public void saveExperimentMeasures() {
		if (seqKymos != null) {
			seqKymos.roisSaveEdits(capillaries);
			xmlSaveMCcapillaries(seqCamData.getDirectory());
			xmlSaveKymos_Measures(seqCamData.getDirectory());
		}
	}
	
	public boolean saveFlyPositions() {
		return seqCamData.saveFlyPositions();
	}
	
	public void kymosBuildFiltered(int zChannelSource, int zChannelDestination, TransformOp transformop, int spanDiff) {
		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(spanDiff);
		
		int nimages = seqKymos.seq.getSizeT();
		seqKymos.seq.beginUpdate();
		tImg.setSequence(seqKymos);
		seqKymos.setStartStopStepToCapillaries(capillaries);
		if (capillaries.capillariesArrayList.size() != nimages) {
			SequenceKymosUtils.transferCamDataROIStoKymo(this);
		}
		
		for (int t= 0; t < nimages; t++) {
			Capillary cap = capillaries.capillariesArrayList.get(t);
			cap.indexImage = t;
			IcyBufferedImage img = seqKymos.seq.getImage(t, zChannelSource);
			IcyBufferedImage img2 = tImg.transformImage (img, transformop);
			img2 = tImg.transformImage(img2, TransformOp.RTOGB);
			
			if (seqKymos.seq.getSizeZ(0) < (zChannelDestination+1)) 
				seqKymos.seq.addImage(t, img2);
			else
				seqKymos.seq.setImage(t, zChannelDestination, img2);
		}
		
		if (zChannelDestination == 1)
			capillaries.limitsOptions.transformForLevels = transformop;
		else
			capillaries.gulpsOptions.transformForGulps = transformop;
		seqKymos.seq.dataChanged();
		seqKymos.seq.endUpdate();
	}
	
	public boolean capillaryRoisOpen(String csFileName) {
		boolean flag = false;
		if (capillaries != null) {
			if (csFileName == null) {
				csFileName = seqKymos.getDirectory() +File.separator+"results"+ File.separator+ "MCcapillaries.xml";
				flag = capillaries.xmlLoadCapillaries(csFileName);
				if (!flag) {
					csFileName = seqKymos.getDirectory() + File.separator + "capillaryTrack.xml";
					flag = capillaries.xmlLoadCapillaries(csFileName);
				}
			}
			analysisStart = capillaries.desc.analysisStart;
			analysisEnd = capillaries.desc.analysisEnd;
			analysisStep = capillaries.desc.analysisStep;
		}
		return flag;
	}
	
	public boolean xmlLoadKymos_Measures(String pathname) {
		pathname = seqKymos.getCorrectPath(pathname);
		if (pathname == null)
			return false;
		boolean flag = capillaries.xmlLoadCapillaries(pathname);
		if (flag) {
			Path pathfilename = Paths.get(pathname);
			seqKymos.directory = pathfilename.getParent().toString();
			seqKymos.getStartStopStepFromCapillaries (capillaries);
			seqKymos.loadListOfKymographsFromCapillaries(seqKymos.getDirectory(), capillaries);
		}
		return flag;
	}
	
	public boolean xmlLoadMCcapillariesOnly(String pathname) {
		pathname = seqKymos.getCorrectPath(pathname);
		if (pathname == null)
			return false;
		boolean flag = capillaries.xmlLoadCapillaries(pathname);
		return flag;
	}
	
	public boolean xmlLoadMCcapillaries(String pathname) {
		pathname = seqKymos.getCorrectPath(pathname);
		if (pathname == null)
			return false;
		boolean flag = capillaries.xmlLoadCapillaries(pathname);
		if (flag) {
			Path pathfilename = Paths.get(pathname);
			seqKymos.directory = pathfilename.getParent().toString();
			seqKymos.getStartStopStepFromCapillaries (capillaries);
			seqKymos.loadListOfKymographsFromCapillaries(seqKymos.getDirectory(), capillaries);
		}
		return flag;
	}
	
	public boolean xmlSaveMCcapillaries(String pathname) {
		String pathnameMC = seqKymos.buildCorrectPath(pathname);
		capillaries.xmlSaveCapillaries_Only(pathnameMC);
		boolean flag = capillaries.xmlSaveCapillaries_Measures(pathname);
		return flag;
	}
	
	public boolean xmlSaveKymos_Measures(String pathname) {
		File f = new File(pathname);
		if (!f.isDirectory())
			pathname = Paths.get(pathname).getParent().toString();
		return capillaries.xmlSaveCapillaries_Measures(pathname);
	}
	
	public boolean xmlReadRoiLineParameters(String pathname) {
		if (pathname != null)  {
			final Document doc = XMLUtil.loadDocument(pathname);
			if (doc != null) 
				return capillaries.desc.xmlLoadCapillaryDescription(doc); 
		}
		return false;
	}
	
	
	public void updateCapillariesFromCamData() {
		List<ROI2D> listROISCap = seqCamData.getCapillaries();
		for (Capillary cap: capillaries.capillariesArrayList) {
			cap.valid = false;
			String capName = cap.replace_LR_with_12(cap.capillaryRoi.getName());
			Iterator <ROI2D> iterator = listROISCap.iterator();
			while(iterator.hasNext()) { 
				ROI2D roi = iterator.next();
				String roiName = cap.replace_LR_with_12(roi.getName());
				if (roiName.equals (capName)) {
					cap.capillaryRoi = (ROI2DShape) roi;
					cap.valid = true;
				}
				if (cap.valid) {
					iterator.remove();
					break;
				}
			}
		}
		Iterator <Capillary> iterator = capillaries.capillariesArrayList.iterator();
		while (iterator.hasNext()) {
			Capillary cap = iterator.next();
			if (!cap.valid )
				iterator.remove();
		}
		
		if (listROISCap.size() > 0) {
			for (ROI2D roi: listROISCap) {
				Capillary cap = new Capillary((ROI2DShape) roi);
				capillaries.capillariesArrayList.add(cap);
			}
		}
		seqKymos.setStartStopStepToCapillaries(capillaries);
		return;
	}
	
	public String getDecoratedImageNameFromCapillary(int t) {
		if (capillaries != null & capillaries.capillariesArrayList.size() > 0)
			return capillaries.capillariesArrayList.get(t).capillaryRoi.getName() + " ["+(t+1)+ "/" + seqKymos.seq.getSizeT() + "]";
		return seqKymos.csFileName + " ["+(t+1)+ "/" + seqKymos.seq.getSizeT() + "]";
	}
	
}
