package plugins.fmp.multicafeSequence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
	public List<Cage>			cageList				= new ArrayList<Cage>();
	
	private List<ROI2D> 		cageLimitROIList		= new ArrayList<ROI2D>();
	private List<ROI2D> 		detectedFliesList		= new ArrayList<ROI2D>();
	private List<XYTaSeries> 	flyPositionsList 		= new ArrayList<XYTaSeries>();
	
	

	public void clear() {
		detectedFliesList.clear();
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
				fromDetectedFliesToROIs(seq);
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
		cageList.clear();
		detect.loadFromXML(node);
		if (xmlLoadCagesLimits(node)) {
			xmlLoadFlyPositions(node);
			xmlLoadDetectedFlies(node);
			transferV0DataToCages();
		}
		else {
			Element xmlVal = XMLUtil.getElement(node, "Cages");
			if (xmlVal != null) {
				int ncages = XMLUtil.getAttributeIntValue(xmlVal, "n_cages", 0);
				if (ncages > 0)
					cageList.clear();
				for (int index = 0; index < ncages; index++) {
					Cage cage = new Cage();
					cage.xmlLoadCage(xmlVal, index);
					cageList.add(cage);
				}
			}
		}
		return true;
	}
	
	private void transferV0DataToCages() {
		cageList.clear();
		int ncages = cageLimitROIList.size();
		for (int index=0; index< ncages; index++) {
			Cage cage = new Cage();
			cage.cageLimitROI = cageLimitROIList.get(index);
			cage.flyPositions = flyPositionsList.get(index);
			cageList.add(cage);
		}
	}
	
	private boolean xmlSaveCages (Document doc) {
		String nodeName = "drosoTrack";
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;

		detect.saveToXML(node);
		xmlSaveCageList(node);
		return true;
	}
	
	private boolean xmlSaveCageList(Node node) {
		if (node == null)
			return false;
		int index = 0;
		Element xmlVal = XMLUtil.addElement(node, "Cages");
		int ncages = cageList.size();
		XMLUtil.setAttributeIntValue(xmlVal, "n_cages", ncages);
		for (Cage cage: cageList) {
			cage.xmlSaveCage(xmlVal, index);
			index++;
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
		
		cageLimitROIList.clear();
		for (Cage cage: cageList) {
			cageLimitROIList.add(cage.cageLimitROI);
		}
		seq.seq.addROIs(cageLimitROIList, true);
	}
	
	public void fromROIsToCages(SequenceCamData seq) {
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
		// remove cages with no names like in the list
		Iterator<Cage> iterator = cageList.iterator();
		while (iterator.hasNext()) {
			Cage cage = iterator.next();
			boolean found = false;
			for (ROI2D roi: cageLimitROIList) {
				if (roi.getName().equals(cage.cageLimitROI.getName())) {
					cage.cageLimitROI.copyFrom(roi);
					roi = null;
					found = true;
					break;
				}
			}
			if (!found ) {
				iterator.remove();
			}
		}	
		// copy names that are equal and create new ones
		for (ROI2D roi: cageLimitROIList) {
			for (Cage cage: cageList) {
				if (roi.getName().equals(cage.cageLimitROI.getName())) {
					cage.cageLimitROI.copyFrom(roi);
					roi = null;
					break;
				}
			}
			if (roi != null) {
				Cage cage = new Cage();
				cage.cageLimitROI = roi;
				cageList.add(cage);
			}
		}
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
