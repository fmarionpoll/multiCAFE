package plugins.fmp.multicafeSequence;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.fmp.multicafeTools.OverlayThreshold;
import plugins.fmp.multicafeTools.OverlayTrapMouse;
import plugins.fmp.multicafeTools.ROI2DUtilities;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class SequenceKymos extends SequenceCamData  {
	
	public Capillaries 		capillaries 			= new Capillaries();
	public 	boolean 		hasChanged 				= false;
	public 	boolean 		bStatusChanged 			= false;

	public 	LocalDateTime	startDate				= null;
	public 	LocalDateTime	endDate					= null;
	public 	long			minutesBetweenImages	= 1;
	public 	OverlayThreshold thresholdOverlay 		= null;
	public 	OverlayTrapMouse trapOverlay 			= null;
	

	private static String KYMOGRAPH_RESULTS = "KymographAnalysis";
	private static String ID_ANALYSISSTART = "analysisStart";
	private static String ID_ANALYSISEND = "analysisEnd";
	private static String ID_ANALYSISSTEP = "analysisStep";
	private static String ID_KYMONAME = "KymoName";
	
	// -----------------------------------------------------
	
	public SequenceKymos() {
		super ();
		status = EnumStatus.KYMOGRAPH;
	}
	
	public SequenceKymos(String name, IcyBufferedImage image) {
		super (name, true, image);
		status = EnumStatus.KYMOGRAPH;
	}
	
	public SequenceKymos (String [] list, String directory) {
		super(list, true, directory);
		status = EnumStatus.KYMOGRAPH;
	}
	
	public SequenceKymos (List<String> listFullPaths) {
		super(listFullPaths, true);
		status = EnumStatus.KYMOGRAPH;
	}
	
	public String getDecoratedImageNameFromCapillary(int t) {
		if (capillaries != null & capillaries.capillariesArrayList.size() > 0)
			return capillaries.capillariesArrayList.get(t).roi.getName() + " ["+(t+1)+ "/" + seq.getSizeT() + "]";
		return csFileName + " ["+(t+1)+ "/" + seq.getSizeT() + "]";
	}

	// ----------------------------
	
	public void roisSaveEdits() {
		if (hasChanged) {
			validateRois();
			transferRoisToCapillaries();
			hasChanged = false;
		}
	}
	
	public void validateRois() {
		List<ROI2D> listRois = seq.getROI2Ds();
		int width = seq.getWidth();
		for (ROI2D roi: listRois) {
			if (!(roi instanceof ROI2DPolyLine))
				continue;
			// interpolate missing points if necessary
			if (roi.getName().contains("level") || roi.getName().contains("gulp")) {
				ROI2DUtilities.interpolateMissingPointsAlongXAxis ((ROI2DPolyLine) roi, width);
				continue;
			}
			if (roi.getName().contains("derivative"))
				continue;
				
			// if gulp not found - add an index to it	
			ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
			Polyline2D line = roiLine.getPolyline2D();
			roi.setName("gulp"+String.format("%07d", (int) line.xpoints[0]));
			roi.setColor(Color.red);
		}
		Collections.sort(listRois, new MulticafeTools.ROI2DNameComparator());
	}

	private void transferRoisToCapillaries() {
		List<ROI> allRois = seq.getROIs();
		for (int t=0; t< seq.getSizeT(); t++) {
			List<ROI> roisAtT = new ArrayList<ROI> ();
			for (ROI roi: allRois) {
				if (roi instanceof ROI2D) {
					if (((ROI2D)roi).getT() == t)
						roisAtT.add(roi);
				}
			}
			if (capillaries.capillariesArrayList.size() <= t) {
				capillaries.capillariesArrayList.add(new Capillary());
			}
			Capillary cap = capillaries.capillariesArrayList.get(t);
			cap.fileName = getFileName(t);
			cap.transferROIsToMeasures(roisAtT);	
		}
	}
	
	// ----------------------------
	
	public void updateCapillaries(SequenceCamData seqCam) {
		SequenceKymosUtils.transferROIStoCapillaries(seqCam, this);
		transferAnalysisParametersToCapillaries();
		return;
	}
	
	public void transferAnalysisParametersToCapillaries () {
		capillaries.analysisStart = analysisStart;
		capillaries.analysisEnd = analysisEnd;
		capillaries.analysisStep = analysisStep;
	}
	
	private void transferCapillariesToAnalysisParameters () {
		analysisStart = capillaries.analysisStart;
		analysisEnd = capillaries.analysisEnd;
		analysisStep = capillaries.analysisStep;
	}
	
	private void loadFileNamesFromCapillaries() {
		String directory = getDirectory() +File.separator +"results" +File.separator;
		List<String> myListOfFilesNames = new ArrayList<String>(capillaries.capillariesArrayList.size());
		Collections.sort(capillaries.capillariesArrayList, new MulticafeTools.CapillaryIndexImageComparator());
		for (Capillary cap: capillaries.capillariesArrayList) {
			if (!cap.fileName .contains(directory)) {
				Path oldpath = Paths.get(cap.fileName);
				cap.fileName = directory + oldpath.getFileName();
			}
			myListOfFilesNames.add(cap.fileName);
		}
		loadSequenceFromList(convertLinexLRFileNames(myListOfFilesNames));
		status = EnumStatus.KYMOGRAPH;
	}
	
	public boolean xmlReadCapillaryTrackDefault() {
		return xmlReadCapillaryTrack(getDirectory()+ File.separator + "capillarytrack.xml");
	}
	
	public boolean xmlReadCapillaryTrack(String filename) {
		boolean flag = capillaries.xmlReadROIsAndData(filename, this);
		if (flag) {
			transferCapillariesToAnalysisParameters ();
			loadFileNamesFromCapillaries();	
		}
		return flag;
	}
	
	public boolean xmlWriteCapillaryTrackDefault() {
		boolean flag = false;
		String name = getDirectory() + File.separator + "capillarytrack.xml";
		flag = capillaries.xmlWriteROIsAndDataNoQuestion(name, this);
		return flag;
	}
	
	// ----------------------------

	public List<Integer> subtractTi(List<Integer > array) {
		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
			item0 = value;
		}
		return array;
	}
	
	public List<Integer> subtractT0 (List<Integer> array) {
		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	public List<Integer> subtractT0AndAddConstant (List<Integer> array, int constant) {
		if (array == null)
			return null;
		int item0 = array.get(0) - constant;
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	public List<Integer> addConstant (List<Integer> array, int constant) {
		if (array == null)
			return null;
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value + constant);
		}
		return array;
	}

	public boolean loadXMLKymographAnalysis (Capillary cap, String directory) {
		if (directory == null)
			return false;	
		if (!directory .contains("results")) {
			directory = directory + File.separator + "results";
			Path resultsDirectoryPath = Paths.get(directory);
			if (Files.notExists(resultsDirectoryPath)) 
				return false; 
		}
		String filename = directory + File.separator + cap.getName()+".xml";
		Path filenamePath = Paths.get(filename);
		if (Files.notExists(filenamePath)) 
			return false; 
		
		File file = new File(filename);
		Document document = XMLUtil.loadDocument(file);
        if (document == null) 
        	return false;
        Element root = XMLUtil.getRootElement( document );
		if ( root == null )
			return false;
		Element resultsElement = XMLUtil.getElement( root, KYMOGRAPH_RESULTS );
		if ( resultsElement == null )
			return false;
		
		readCapillaryMeasure(resultsElement, cap); 
		return true;
	}
	
	private boolean readCapillaryMeasure(Node rootNode, Capillary cap) {
		final Node myNode = rootNode; //XMLUtil.getElement(rootNode, cap.name + "_parameters");
		if (myNode == null)
			return false;
		
		analysisStart = XMLUtil.getElementIntValue(myNode, ID_ANALYSISSTART, 0);
		analysisEnd = XMLUtil.getElementIntValue(myNode, ID_ANALYSISEND, -1);
		analysisStep = XMLUtil.getElementIntValue(myNode, ID_ANALYSISSTEP, 1);
		
		cap.loadFromXML(myNode); 
		return true;
	}
	
 	public boolean saveXMLKymographAnalysis(Capillary cap, String directory) {
		// check if directory is present. If not, create it
		String resultsDirectory = directory;
		String subDirectory = File.separator + "results";
		if (!resultsDirectory.contains (subDirectory))
			resultsDirectory += subDirectory;
		resultsDirectory += File.separator;
		Path resultsPath = Paths.get(resultsDirectory);
		if (Files.notExists(resultsPath)) {
			try {
				resultsPath = Files.createDirectories(resultsPath);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		Document document = XMLUtil.createDocument(true);
		Element myNode = document.createElement(KYMOGRAPH_RESULTS);
		XMLUtil.getRootElement( document ).appendChild(myNode);
		
		XMLUtil.setAttributeValue(myNode, ID_KYMONAME, cap.getName()+"_parameters");

		XMLUtil.setElementIntValue(myNode, ID_ANALYSISSTART, (int) analysisStart);
		XMLUtil.setElementIntValue(myNode, ID_ANALYSISEND, (int) analysisEnd);
		XMLUtil.setElementIntValue(myNode, ID_ANALYSISSTEP, analysisStep);
		
		cap.saveToXML(myNode);
		
		File file = new File(resultsDirectory+cap.getName()+".xml");
		boolean result = XMLUtil.saveDocument(document, file);
		return result;
	}

	// ----------------------------
	
	public void setThresholdOverlay(boolean bActive) {
		if (bActive) {
			if (thresholdOverlay == null) 
				thresholdOverlay = new OverlayThreshold(this);
			if (!seq.contains(thresholdOverlay)) 
				seq.addOverlay(thresholdOverlay);
			thresholdOverlay.setSequence (this);
		}
		else {
			if (thresholdOverlay != null && seq.contains(thresholdOverlay) )
				seq.removeOverlay(thresholdOverlay);
			thresholdOverlay = null;
		}
	}
	
	public void setThresholdOverlayParametersSingle(TransformOp transf, int threshold) {
		thresholdOverlay.setTransform(transf);
		thresholdOverlay.setThresholdSingle(threshold);
		thresholdOverlay.painterChanged();
	}
	
	public void setThresholdOverlayParametersColors(TransformOp transf, ArrayList <Color> colorarray, int colordistancetype, int colorthreshold) {
		thresholdOverlay.setTransform(transf);
		thresholdOverlay.setThresholdColor(colorarray, colordistancetype, colorthreshold);
		thresholdOverlay.painterChanged();
	}

	public void setMouseTrapOverlay (boolean bActive, JButton pickColorButton, JComboBox<Color> colorPickCombo) {
		if (bActive) {
			if (trapOverlay == null)
				trapOverlay = new OverlayTrapMouse (pickColorButton, colorPickCombo);
			if (!seq.contains(trapOverlay))
				seq.addOverlay(trapOverlay);
		}
		else {
			if (trapOverlay != null && seq.contains(trapOverlay))
				seq.removeOverlay(trapOverlay);
			trapOverlay = null;
		}
	}
	
}
