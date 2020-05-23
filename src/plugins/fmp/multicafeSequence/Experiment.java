package plugins.fmp.multicafeSequence;


import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.Comparators;
import plugins.fmp.multicafeTools.ImageTransformTools;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Experiment {
	
	public String			experimentFileName			= null;
	public SequenceCamData 	seqCamData 					= null;
	public SequenceKymos 	seqKymos					= null;
	public Sequence 		seqBackgroundImage			= null;
	public Capillaries 		capillaries 				= new Capillaries();
	public Cages			cages 						= new Cages();

	public FileTime			fileTimeImageFirst;
	public FileTime			fileTimeImageLast;
	public long				fileTimeImageFirstMinute 	= 0;
	public long				fileTimeImageLastMinute 	= 0;
	public int				number_of_frames 			= 0;
	
	private int 			kymoFrameStart 				= 0;
	private int 			kymoFrameEnd 				= 0;
	private int 			kymoFrameStep 				= 1;									
	
	public String			boxID 						= new String("..");
	public String			experiment					= new String("..");
	public String 			comment1					= new String("..");
	public String 			comment2					= new String("..");
	
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
	private final String ID_STEP = "stepFrame";
	private final String ID_BOXID = "boxID";
	private final String ID_EXPERIMENT = "experiment";
	private final String ID_EXPTFILENAME = "exptFileName";
	private final String ID_COMMENT1 = "comment";
	private final String ID_COMMENT2 = "comment2";
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
	
	public void closeSequences() {
		if (seqKymos != null) {
			seqKymos.closeSequence();
		}
		if (seqCamData != null) {
			seqCamData.closeSequence();
		}
		if (seqBackgroundImage != null) {
			seqBackgroundImage.close();
		}
	}
	
	public void displayCamData(Rectangle parent0Rect) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				Viewer viewerCamData = seqCamData.seq.getFirstViewer();
				if (viewerCamData == null)
					viewerCamData = new Viewer(seqCamData.seq, true);
				Rectangle rectv = viewerCamData.getBoundsInternal();
				rectv.setLocation(parent0Rect.x+ parent0Rect.width, parent0Rect.y);
				viewerCamData.setBounds(rectv);				
			}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		} 
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

		xmlReadDrosoTrackDefault();
		return true;
	}
	
	public boolean openSequenceAndMeasures(boolean loadCapillaries, boolean loadDrosoPositions) {
		if (seqCamData == null) {
			seqCamData = new SequenceCamData();
		}
		boolean flag = xmlLoadExperiment ();
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
			comment1 = capillaries.desc.old_comment1;
			comment2 = capillaries.desc.old_comment2;
		}
		
		if (loadDrosoPositions)
			xmlReadDrosoTrackDefault();
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
	
	public boolean xmlLoadExperiment () {
		if (experimentFileName == null) {
			String directory = seqCamData.getDirectory();
			experimentFileName = directory;
		}
		String csFileName = experimentFileName + File.separator + "results" + File.separator + "MCexperiment.xml";
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc == null)
			return false;
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_MCEXPERIMENT);
		if (node == null)
			return false;

		String version = XMLUtil.getElementValue(node, ID_VERSION, ID_VERSIONNUM);
		if (!version .equals(ID_VERSIONNUM))
			return false;
		fileTimeImageFirstMinute= XMLUtil.getElementLongValue(node, ID_TIMEFIRSTIMAGE, fileTimeImageFirstMinute);
		fileTimeImageLastMinute = XMLUtil.getElementLongValue(node, ID_TIMELASTIMAGE, fileTimeImageLastMinute);
		number_of_frames 		= XMLUtil.getElementIntValue(node, ID_NFRAMES, number_of_frames);
		kymoFrameStart 			= XMLUtil.getElementIntValue(node, ID_STARTFRAME, kymoFrameStart);
		kymoFrameEnd 			= XMLUtil.getElementIntValue(node, ID_ENDFRAME, kymoFrameEnd);
		kymoFrameStep 			= XMLUtil.getElementIntValue(node, ID_STEP, kymoFrameStep);
		boxID 					= XMLUtil.getElementValue(node, ID_BOXID, "..");
        experiment 				= XMLUtil.getElementValue(node, ID_EXPERIMENT, "..");
        comment1 				= XMLUtil.getElementValue(node, ID_COMMENT1, "..");
        comment2 				= XMLUtil.getElementValue(node, ID_COMMENT2, "..");
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
			XMLUtil.setElementIntValue(node, ID_STARTFRAME, kymoFrameStart);
			XMLUtil.setElementIntValue(node, ID_ENDFRAME, kymoFrameEnd);
			XMLUtil.setElementIntValue(node, ID_STEP, kymoFrameStep);
			XMLUtil.setElementValue(node, ID_BOXID, boxID);
	        XMLUtil.setElementValue(node, ID_EXPERIMENT, experiment);
	        XMLUtil.setElementValue(node, ID_COMMENT1, comment1);
	        XMLUtil.setElementValue(node, ID_COMMENT2, comment2);
	        
	        if (experimentFileName == null ) 
	        	experimentFileName = seqCamData.getDirectory();
	        XMLUtil.setElementValue(node, ID_EXPTFILENAME, experimentFileName);

	        String directory = seqCamData.getDirectory();
	        String tempname = directory + File.separator + "results" + File.separator + "MCexperiment.xml";
	        return XMLUtil.saveDocument(doc, tempname);
		}
		return false;
	}
	
 	public boolean loadKymographs() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!xmlLoadKymos_Measures(seqCamData.getDirectory())) 
			return false;
		List<String> myList = seqKymos.loadListOfKymographsFromCapillaries(seqCamData.getDirectory(), capillaries);
		boolean flag = seqKymos.loadImagesFromList(myList, true);
		seqKymos.transferCapillariesToKymosRois(capillaries);
		return flag;
	}
	
	public boolean loadDrosotrack() {
		return xmlReadDrosoTrackDefault();
	}
	
	public boolean loadKymos_Measures() {
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		if (!xmlLoadKymos_Measures(seqCamData.getDirectory())) 
			return false;
		return true;
	}
	
	// ----------------------------------
	
	public String getSubName(Path path, int subnameIndex) {
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
	
	// -----------------------
	
	public boolean isFlyAlive(int cagenumber) {
		boolean isalive = false;
		for (Cage cage: cages.cageList) {
			String cagenumberString = cage.roi.getName().substring(4);
			if (Integer.parseInt(cagenumberString) == cagenumber) {
				isalive = (cage.flyPositions.getLastIntervalAlive() > 0);
				break;
			}
		}
		return isalive;
	}
	
	public int getLastIntervalFlyAlive(int cagenumber) {
		int flypos = -1;
		for (Cage cage: cages.cageList) {
			String cagenumberString = cage.roi.getName().substring(4);
			if (Integer.parseInt(cagenumberString) == cagenumber) {
				flypos = cage.flyPositions.getLastIntervalAlive();
				break;
			}
		}
		return flypos;
	}
	
	public boolean isDataAvailable(int cagenumber) {
		boolean isavailable = false;
		for (Cage cage: cages.cageList) {
			String cagenumberString = cage.roi.getName().substring(4);
			if (Integer.parseInt(cagenumberString) == cagenumber) {
				isavailable = true;
				break;
			}
		}
		return isavailable;
	}
	
	// --------------------------------------------
	
	public boolean checkStepConsistency() {
		int imageWidth = seqKymos.imageWidthMax; 
		int len = (kymoFrameEnd - kymoFrameStart + 1) / kymoFrameStep;
		boolean isOK = true;
		if (len > imageWidth) {
			isOK = false;
			kymoFrameStep = (kymoFrameEnd - kymoFrameStart + 1)/imageWidth;
		}
		return isOK;
	}
	
	public int setKymoFrameStart(int start) {
		kymoFrameStart = start;
		return kymoFrameStart;
	}
	
	public int setKymoFrameEnd(int end) {
		kymoFrameEnd = end;
		return kymoFrameEnd;
	}
	
	public int setKymoFrameStep(int step) {
		kymoFrameStep = step;
		return kymoFrameStep;
	}
	
	public int getKymoFrameStart() {
		return kymoFrameStart;
	}
	
	public int getKymoFrameEnd() {
		return kymoFrameEnd;
	}
	
	public int getSeqCamSizeT() {
		int lastFrame = kymoFrameEnd;
		if (seqCamData != null && seqCamData.seq != null)
			lastFrame = seqCamData.seq.getSizeT() -1;
		return lastFrame;
	}
	
	public int getKymoFrameStep() {
		return kymoFrameStep;
	}
	
	public int checkKymoFrameStep() {
		int step = -1;
		if (seqKymos == null || seqKymos.seq == null)
			return step;
		if (seqKymos.imageWidthMax < 1) {
			seqKymos.imageWidthMax = seqKymos.seq.getSizeX();
			if (seqKymos.imageWidthMax < 1)
				return step;
		}
		if (kymoFrameEnd == 0) {
			if (seqCamData != null && seqCamData.seq != null)
				kymoFrameEnd = seqCamData.seq.getSizeT() -1;
			else
				return step;
		}
		if (kymoFrameStep == 0)
			kymoFrameStep = 1;
		int len2 = (kymoFrameEnd +1)/ kymoFrameStep;
		if (len2 != seqKymos.imageWidthMax) 
			kymoFrameStep = (kymoFrameEnd +1)/(seqKymos.imageWidthMax-1);
		return kymoFrameStep;
	}
	
	public int setCagesFrameStart(int start) {
		cages.frameStart = start;
		return cages.frameStart;
	}
	
	public int setCagesFrameEnd(int end) {
		cages.frameEnd = end;
		return cages.frameEnd;
	}
	
	public int setCagesFrameStep(int step) {
		cages.frameStep = step;
		return cages.frameStep;
	}
	
	public int getCagesFrameStart() {
		return cages.frameStart;
	}
	
	public int getCagesFrameEnd() {
		return cages.frameEnd;
	}
	
	public int getCagesFrameStep() {
		return cages.frameStep;
	}
	
	// --------------------------------------------
	
	public boolean adjustCapillaryMeasuresDimensions() {
		if (seqKymos.imageWidthMax < 1) {
			seqKymos.imageWidthMax = seqKymos.seq.getSizeX();
			if (seqKymos.imageWidthMax < 1)
				return false;
		}
		int imageWidth = seqKymos.imageWidthMax;
		capillaries.adjustToImageWidth(imageWidth);
		seqKymos.seq.removeAllROI();
		seqKymos.transferCapillariesToKymosRois(capillaries);
		return true;
	}
	
	public void loadExperimentData() {
		xmlLoadExperiment();
		seqCamData.loadSequence(experimentFileName) ;
		seqCamData.getCamDataROIS (capillaries);
		capillaries.desc.analysisStart = kymoFrameStart; 
		capillaries.desc.analysisEnd  = kymoFrameEnd;
		capillaries.desc.analysisStep = kymoFrameStep;
		xmlLoadMCcapillaries(experimentFileName);
	}
	
	public void loadExperimentCamData() {
		xmlLoadExperiment();
		seqCamData.loadSequence(experimentFileName) ;
	}
	
	public void loadExperimentDataToBuildKymos() {
		xmlLoadExperiment();
		seqCamData.loadSequence(experimentFileName) ;
		xmlLoadMCcapillariesOnly(experimentFileName);
	}
	
	public void saveExperimentMeasures() {
		if (seqKymos != null) {
			seqKymos.validateRois();
			seqKymos.transferKymosRoisToCapillaries(capillaries);
			xmlSaveMCcapillaries();
			xmlSaveKymos_Measures();
		}
	}
		
	public void kymosBuildFiltered(int zChannelSource, int zChannelDestination, TransformOp transformop, int spanDiff) {
		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(spanDiff);
		
		int nimages = seqKymos.seq.getSizeT();
		seqKymos.seq.beginUpdate();
		tImg.setSequence(seqKymos);
		
		if (capillaries.capillariesArrayList.size() != nimages) {
			SequenceKymosUtils.transferCamDataROIStoKymo(this);
		}
		
		for (int t= 0; t < nimages; t++) {
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
			capillaries.gulpsOptions.transformForGulps = transformop;
		seqKymos.seq.dataChanged();
		seqKymos.seq.endUpdate();
	}
	
	public void setReferenceImageWithConstant (double [] pixel) {
		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(0);
		Sequence seq = seqKymos.seq;
		tImg.referenceImage = new IcyBufferedImage(seq.getSizeX(), seq.getSizeY(), seq.getSizeC(), seq.getDataType_());
		IcyBufferedImage result = tImg.referenceImage;
		for (int c=0; c < seq.getSizeC(); c++) {
			double [] doubleArray = Array1DUtil.arrayToDoubleArray(result.getDataXY(c), result.isSignedDataType());
			Array1DUtil.fill(doubleArray, 0, doubleArray.length, pixel[c]);
			Array1DUtil.doubleArrayToArray(doubleArray, result.getDataXY(c));
		}
		result.dataChanged();
	}
	
	public boolean capillaryRoisOpen(String csFileName) {
		boolean flag = false;
		if (capillaries != null) {
			if (csFileName == null) {
				csFileName = seqKymos.getDirectory() + File.separator+ "MCcapillaries.xml";
				flag = capillaries.xmlLoadCapillaries(csFileName);
				if (!flag) {
					csFileName = seqCamData.getDirectory() + File.separator + "capillaryTrack.xml";
					flag = capillaries.xmlLoadCapillaries(csFileName);
				}
			}
			kymoFrameStart = (int) capillaries.desc.analysisStart;
			kymoFrameEnd = (int) capillaries.desc.analysisEnd;
			kymoFrameStep = capillaries.desc.analysisStep;
		}
		return flag;
	}
	
	public boolean xmlLoadKymos_Measures(String pathname) {
		pathname = capillaries.getCorrectPath(pathname);
		if (pathname == null)
			return false;
		boolean flag = capillaries.xmlLoadCapillaries(pathname);
		if (flag) {
			Path pathfilename = Paths.get(pathname);
			seqKymos.directory = pathfilename.getParent().toString();
			seqKymos.loadListOfKymographsFromCapillaries(seqKymos.getDirectory(), capillaries);
		}
		return flag;
	}
	
	public boolean xmlLoadMCcapillariesOnly(String pathname) {
		pathname = capillaries.getCorrectPath(pathname);
		if (pathname == null)
			return false;
		return capillaries.xmlLoadCapillaries(pathname);
	}
	
	public boolean xmlLoadMCcapillaries(String pathname) {
		pathname = capillaries.getCorrectPath(pathname);
		if (pathname == null)
			return false;
		boolean flag = capillaries.xmlLoadCapillaries(pathname);
		if (flag) {
			Path pathfilename = Paths.get(pathname);
			seqKymos.directory = pathfilename.getParent().toString();
			seqKymos.loadListOfKymographsFromCapillaries(seqCamData.getResultsDirectory(), capillaries);
		}
		return flag;
	}
	
	public boolean xmlSaveMCcapillaries() {
		String saveMCcapillariesFullPath = capillaries.getMCCapillaryNameFromExperimentPath(experimentFileName);
		capillaries.xmlSaveCapillaries_Only(saveMCcapillariesFullPath);
		boolean flag = capillaries.xmlSaveCapillaries_Measures(seqCamData.getResultsDirectory());
		return flag;
	}
	
	public boolean xmlSaveKymos_Measures() {
		return capillaries.xmlSaveCapillaries_Measures(seqCamData.getResultsDirectory());
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
		List<ROI2D> listROISCap = seqCamData.getROIs2DContainingString ("line");
		Collections.sort(listROISCap, new Comparators.ROI2DNameComparator());
		for (Capillary cap: capillaries.capillariesArrayList) {
			cap.valid = false;
			String capName = cap.replace_LR_with_12(cap.roi.getName());
			Iterator <ROI2D> iterator = listROISCap.iterator();
			while(iterator.hasNext()) { 
				ROI2D roi = iterator.next();
				String roiName = cap.replace_LR_with_12(roi.getName());
				if (roiName.equals (capName)) {
					cap.roi = (ROI2DShape) roi;
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
		Collections.sort(capillaries.capillariesArrayList);
		return;
	}
	
	public String getDecoratedImageNameFromCapillary(int t) {
		if (capillaries != null & capillaries.capillariesArrayList.size() > 0)
			return capillaries.capillariesArrayList.get(t).roi.getName() + " ["+(t+1)+ "/" + seqKymos.seq.getSizeT() + "]";
		return seqKymos.csFileName + " ["+(t+1)+ "/" + seqKymos.seq.getSizeT() + "]";
	}
	
	public boolean loadReferenceImage() {
		BufferedImage image = null;
		String path = seqCamData.getResultsDirectory()+File.separator+"referenceImage.jpg";
		File inputfile = new File(path);
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
	
	public boolean saveReferenceImage() {
		String path = seqCamData.getResultsDirectory()+File.separator+"referenceImage.jpg";
		File outputfile = new File(path);
		RenderedImage image = ImageUtil.toRGBImage(seqCamData.refImage);
		return ImageUtil.save(image, "jpg", outputfile);
	}

	public 	void cleanPreviousDetections() {
		for (Cage cage: cages.cageList) {
			cage.flyPositions = new XYTaSeries();
			cage.detectedFliesList.clear();
		}
		ArrayList<ROI2D> list = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (roi.getName().contains("det")) {
				seqCamData.seq.removeROI(roi);
			}
		}
	}
	
	public void storeAnalysisParametersToCages() {
		cages.detect.startFrame = (int) kymoFrameStart;
		cages.detect.endFrame = (int) kymoFrameEnd;
		cages.detect.stepFrame = kymoFrameStep;
	}
	
	public void xmlSaveFlyPositionsForAllCages() {			
//		cages.getCagesFromROIs(seqCamData);
		cages.xmlWriteCagesToFileNoQuestion(getMCdrosotrackName());
	}
	
	// --------------------------
	
	private String getMCdrosotrackName() {
		return seqCamData.getResultsDirectory() + File.separator + "MCdrosotrack.xml";
	}
	
	public boolean xmlReadDrosoTrackDefault() {
		boolean flag = cages.xmlReadCagesFromFileNoQuestion(getMCdrosotrackName(), seqCamData);
		if (!flag)
			flag = cages.xmlReadCagesFromFileNoQuestion(seqCamData.getDirectory() + File.separator + "drosotrack.xml", seqCamData);
		return flag;
	}
	
	public boolean xmlReadDrosoTrack(String filename) {
		return cages.xmlReadCagesFromFileNoQuestion(filename, seqCamData);
	}
	
	public boolean xmlWriteDrosoTrackDefault() {
		return cages.xmlWriteCagesToFileNoQuestion(getMCdrosotrackName());
	}
	
	

}
