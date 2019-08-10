package plugins.fmp.multicafeSequence;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.util.XMLUtil;

import plugins.fmp.multicafeTools.OverlayThreshold;
import plugins.fmp.multicafeTools.OverlayTrapMouse;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class SequenceKymos extends SequenceCapillaries  {
	
	public 	boolean 		hasChanged 				= false;
	public 	boolean 		bStatusChanged 			= false;

//	public	TransformOp		transformForLevels 		= TransformOp.R2MINUS_GB;
//	public	TransformOp 	transformForGulps 		= TransformOp.XDIFFN;
	
	public 	LocalDateTime	startDate				= null;
	public 	LocalDateTime	endDate					= null;
	public 	long			minutesBetweenImages	= 1;
	public 	OverlayThreshold thresholdOverlay 		= null;
	public 	OverlayTrapMouse trapOverlay 			= null;
	
	public static String KYMOGRAPH_RESULTS = "KymographAnalysis";
	
	// -----------------------------------------------------
	
	public SequenceKymos() {
		super ();
		status = EnumStatus.KYMOGRAPH;
	}
	
	public SequenceKymos(String name, IcyBufferedImage image) {
		super (name, image);
		status = EnumStatus.KYMOGRAPH;
	}
	
	public SequenceKymos (String [] list, String directory) {
		super(list, directory);
		status = EnumStatus.KYMOGRAPH;
	}
	
	public SequenceKymos (List<String> listFullPaths) {
		super(listFullPaths);
		status = EnumStatus.KYMOGRAPH;
	}
	
	public ArrayList<Integer> subtractTi(ArrayList<Integer > array) {
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
	
	public ArrayList<Integer> subtractT0 (ArrayList<Integer> array) {
		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	public ArrayList<Integer> subtractT0AndAddConstant (ArrayList<Integer> array, int constant) {
		if (array == null)
			return null;
		int item0 = array.get(0) - constant;
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	public ArrayList<Integer> addConstant (ArrayList<Integer> array, int constant) {
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
			directory = directory + "\\results";
			Path resultsDirectoryPath = Paths.get(directory);
			if (Files.notExists(resultsDirectoryPath)) 
				return false; 
		}
		String filename = directory+"\\"+ cap.getName()+".xml";
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
		
		analysisStart = XMLUtil.getElementIntValue(myNode, "analysisStart", 0);
		analysisEnd = XMLUtil.getElementIntValue(myNode, "analysisEnd", -1);
		analysisStep = XMLUtil.getElementIntValue(myNode, "analysisStep", 1);
		
		cap.loadFromXML(myNode); 
		return true;
	}
	
 	public boolean saveXMLKymographAnalysis(Capillary cap, String directory) {
		// check if directory is present. If not, create it
		String resultsDirectory = directory;
		String subDirectory = "\\results";
		if (!resultsDirectory.contains (subDirectory))
			resultsDirectory += subDirectory;
		resultsDirectory += "\\";
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
		
		XMLUtil.setAttributeValue(myNode, "SequenceName", cap.getName()+"_parameters");

		XMLUtil.setElementIntValue(myNode, "analysisStart", (int) analysisStart);
		XMLUtil.setElementIntValue(myNode, "analysisEnd", (int) analysisEnd);
		XMLUtil.setElementIntValue(myNode, "analysisStep", analysisStep);
		
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
