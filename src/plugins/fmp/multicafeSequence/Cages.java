package plugins.fmp.multicafeSequence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.fmp.multicafeTools.DetectFlies_Options;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.kernel.roi.roi2d.ROI2DPolygon;


public class Cages {
	
	public DetectFlies_Options 	detect 					= new DetectFlies_Options();
	public List<ROI2D> 			cageLimitROIList		= new ArrayList<ROI2D>();
	public List<ROI2D> 			detectedFliesList		= new ArrayList<ROI2D>();
	public List<XYTaSeries> 	flyPositionsList 		= new ArrayList<XYTaSeries>();
	

	public void clear() {
		flyPositionsList.clear();
	}
	
	public boolean xmlWriteCagesToFile(String name, String directory) {

		String csFile = MulticafeTools.saveFileAs(name, directory, "xml");
		if (csFile == null)
			return false;
		
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		return xmlWriteCagesToFileNoQuestion(csFile);
	}
		
	public boolean xmlWriteCagesToFileNoQuestion(String csFile) {
		if (csFile == null) 
			return false;
		final Document doc = XMLUtil.createDocument(true);
		if (doc == null)
			return false;
		
		xmlSaveCages (doc);
		XMLUtil.saveDocument(doc, csFile);
		return true;
	}
	
	public boolean xmlReadCagesFromFile(SequenceCamData seq) {

		String [] filedummy = null;
		String filename = seq.getFileName();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		filedummy = MulticafeTools.selectFiles(directory, "xml");
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i= 0; i< filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadCagesFromFileNoQuestion(csFile, seq);
			}
		}
		return wasOk;

	}
	
	public boolean xmlReadCagesFromFileNoQuestion(String csFileName, SequenceCamData seq) {

		if (csFileName != null)  {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc != null) {
				xmlLoadCages(doc);
				fromCagesToROIs(seq);
				return true;
			}
		}
		return false;
	}
	
	private boolean xmlLoadCages (Document doc) {
		String nodeName = "drosoTrack";
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;
		detect.loadFromXML(node);
		xmlLoadCagesLimits(node);
		xmlLoadFlyPositions(node);
		xmlLoadDetectedFlies(node);
		return true;
	}
	
	private boolean xmlSaveCages (Document doc) {
		String nodeName = "drosoTrack";
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;

		detect.saveToXML(node);
		xmlSaveCagesLimits(node);
		xmlSaveFlyPositions(node);
		xmlSaveDetectedFlies(node);
		return true;
	}
	
	private boolean xmlSaveCagesLimits(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "Cage_Limits");
		XMLUtil.setAttributeIntValue(xmlVal, "nb_items", cageLimitROIList.size());
		int i=0;
		for (ROI roi: cageLimitROIList) {
			Element subnode = XMLUtil.addElement(xmlVal, "cage"+i);
			roi.saveToXML(subnode);
			i++;
		}
		return true;
	}
	
	private boolean xmlLoadCagesLimits(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "Cage_Limits");
		if (xmlVal == null) 
			return false;
		
		cageLimitROIList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			ROI2DPolygon roi = (ROI2DPolygon) ROI.create("plugins.kernel.roi.roi2d.ROI2DPolygon");
			Element subnode = XMLUtil.getElement(xmlVal, "cage"+i);
			roi.loadFromXML(subnode);
			cageLimitROIList.add((ROI2D) roi);
		}
		return true;
	}
		
	private boolean xmlSaveFlyPositions(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "Fly_Detected");
		XMLUtil.setAttributeIntValue(xmlVal, "nb_items", flyPositionsList.size());
		int i=0;
		for (XYTaSeries pos: flyPositionsList) {
			Element subnode = XMLUtil.addElement(xmlVal, "cage"+i);
			pos.saveToXML(subnode);
			i++;
		}
		return true;
	}
	
	private boolean xmlLoadFlyPositions(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "Fly_Detected");
		if (xmlVal == null) 
			return false;
		
		flyPositionsList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, "nb_items", 0);
		int ielement = 0;
		for (int i=0; i< nb_items; i++) {
			Element subnode = XMLUtil.getElement(xmlVal, "cage"+ielement);
			XYTaSeries pos = new XYTaSeries();
			pos.loadFromXML(subnode);
			flyPositionsList.add(pos);
			ielement++;
		}
		return true;
	}
	
	private boolean xmlSaveDetectedFlies(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "Flies_detected");
		XMLUtil.setAttributeIntValue(xmlVal, "nb_items", detectedFliesList.size());
		int i=0;
		for (ROI roi: detectedFliesList) {
			Element subnode = XMLUtil.addElement(xmlVal, "det"+i);
			roi.saveToXML(subnode);
			i++;
		}
		return true;
	}
	
	private boolean xmlLoadDetectedFlies(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "Flies_detected");
		if (xmlVal == null) 
			return false;
		
		detectedFliesList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			ROI2DPolygon roi = (ROI2DPolygon) ROI.create("plugins.kernel.roi.roi2d.ROI2DPolygon");
			Element subnode = XMLUtil.getElement(xmlVal, "det"+i);
			roi.loadFromXML(subnode);
			detectedFliesList.add((ROI2D) roi);
		}
		return true;
	}
	
	public void fromCagesToROIs(SequenceCamData seq) {
		ArrayList<ROI2D> list = seq.seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("cage"))
				continue;
			seq.seq.removeROI(roi);
		}
		seq.seq.addROIs(cageLimitROIList, true);
	}
	
	public void fromROISToCages(SequenceCamData seq) {
		cageLimitROIList.clear();
		ArrayList<ROI2D> list = seq.seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("cage"))
				continue;
			cageLimitROIList.add(roi);
		}
		Collections.sort(cageLimitROIList, new MulticafeTools.ROI2DNameComparator());
	}

	public void fromDetectedFliesToROIs(SequenceCamData seq) {
		ArrayList<ROI2D> list = seq.seq.getROI2Ds();
		detectedFliesList.clear();
		for (ROI2D roi: list) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("det"))
				continue;
			seq.seq.removeROI(roi);
		}
		seq.seq.addROIs(detectedFliesList, true);
	}
	
	public void fromROIstoDetectedFlies(SequenceCamData seq) {
		detectedFliesList.clear();
		ArrayList<ROI2D> list = seq.seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("det"))
				continue;
			detectedFliesList.add(roi);
		}
		Collections.sort(detectedFliesList, new MulticafeTools.ROI2DNameComparator());
	}

}
