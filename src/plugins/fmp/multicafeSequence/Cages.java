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
import plugins.fmp.multicafeTools.Comparators;
import plugins.fmp.multicafeTools.DetectFlies_Options;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.kernel.roi.roi2d.ROI2DPolygon;


public class Cages {
	
	public DetectFlies_Options 	detect 					= new DetectFlies_Options();
	public List<Cage>			cageList				= new ArrayList<Cage>();
	
	private final String ID_CAGES 		= "Cages";
	private final String ID_NCAGES 		= "n_cages";
	private final String ID_DROSOTRACK 	= "drosoTrack";
	private final String ID_NBITEMS 	= "nb_items";
	private final String ID_CAGELIMITS 	= "Cage_Limits";
	private final String ID_FLYDETECTED = "Fly_Detected";
	
	

	public void clear() {
		for (Cage cage: cageList) {
			cage.clearMeasures();
		}
	}
	
	public boolean xmlWriteCagesToFile(String name, String directory) {
		String csFile = MulticafeTools.saveFileAs(name, directory, "xml");
		if (csFile == null)
			return false;
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) 
			csFile += ".xml";
		return xmlWriteCagesToFileNoQuestion(csFile);
	}
		
	public boolean xmlWriteCagesToFileNoQuestion(String tempname) {
		if (tempname == null) 
			return false;
		final Document doc = XMLUtil.createDocument(true);
		if (doc == null)
			return false;
		boolean flag = xmlSaveCages (doc);
		if (!flag)
			System.out.println("failed to write cages to file");
		return XMLUtil.saveDocument(doc, tempname);
	}
		
	public boolean xmlReadCagesFromFile(SequenceCamData seq) {
		String [] filedummy = null;
		String filename = seq.getSequenceFileName();
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
	
	public boolean xmlReadCagesFromFileNoQuestion(String tempname, SequenceCamData seq) {
		if (tempname == null) 
			return false;
		final Document doc = XMLUtil.loadDocument(tempname);
		if (doc == null)
			return false;
		boolean flag = xmlLoadCages(doc); 
		if (flag) {
			fromCagesToROIs(seq);
			fromDetectedFliesToROIs(seq);
		}
		else {
			System.out.println("failed to write cages to file");
			return false;
		}
		return true;
	}
	
	private boolean xmlLoadCages (Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_DROSOTRACK);
		if (node == null)
			return false;
		cageList.clear();
		detect.loadFromXML(node);
		Element xmlVal = XMLUtil.getElement(node, ID_CAGES);
		if (xmlVal != null) {
			int ncages = XMLUtil.getAttributeIntValue(xmlVal, ID_NCAGES, 0);
			if (ncages > 0)
				cageList.clear();
			for (int index = 0; index < ncages; index++) {
				Cage cage = new Cage();
				cage.xmlLoadCage(xmlVal, index);
				cageList.add(cage);
			}
		} else {
			List<ROI2D> cageLimitROIList = new ArrayList<ROI2D>();
			if (v0XmlLoadCagesLimits(node, cageLimitROIList)) {
				List<XYTaSeries> flyPositionsList = new ArrayList<XYTaSeries>();
				v0XmlLoadFlyPositions(node, flyPositionsList);
				v0TransferDataToCages(cageLimitROIList, flyPositionsList);
			}
			else
				return false;
		}
		return true;
	}
	
	private void v0TransferDataToCages(List<ROI2D> cageLimitROIList, List<XYTaSeries> flyPositionsList) {
		cageList.clear();
		int ncages = cageLimitROIList.size();
		for (int index=0; index< ncages; index++) {
			Cage cage = new Cage();
			cage.roi = cageLimitROIList.get(index);
			cage.flyPositions = flyPositionsList.get(index);
			cageList.add(cage);
		}
	}
	
	private boolean xmlSaveCages (Document doc) {
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), ID_DROSOTRACK);
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
		Element xmlVal = XMLUtil.addElement(node, ID_CAGES);
		int ncages = cageList.size();
		XMLUtil.setAttributeIntValue(xmlVal, ID_NCAGES, ncages);
		for (Cage cage: cageList) {
			cage.xmlSaveCage(xmlVal, index);
			index++;
		}
		return true;
	}
	
	private boolean v0XmlLoadCagesLimits(Node node, List<ROI2D> cageLimitROIList) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, ID_CAGELIMITS);
		if (xmlVal == null) 
			return false;
		
		cageLimitROIList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, ID_NBITEMS, 0);
		for (int i=0; i< nb_items; i++) {
			ROI2DPolygon roi = (ROI2DPolygon) ROI.create("plugins.kernel.roi.roi2d.ROI2DPolygon");
			Element subnode = XMLUtil.getElement(xmlVal, "cage"+i);
			roi.loadFromXML(subnode);
			cageLimitROIList.add((ROI2D) roi);
		}
		return true;
	}
	
	private boolean v0XmlLoadFlyPositions(Node node, List<XYTaSeries> flyPositionsList) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, ID_FLYDETECTED);
		if (xmlVal == null) 
			return false;
		
		flyPositionsList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, ID_NBITEMS, 0);
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
	
	public void fromCagesToROIs(SequenceCamData seqCamData) {
		List <ROI2D> cageLimitROIList = getRoisWithCageName(seqCamData);
		seqCamData.seq.removeROIs(cageLimitROIList, false);
		for (Cage cage: cageList) {
			cageLimitROIList.add(cage.roi);
		}
		seqCamData.seq.addROIs(cageLimitROIList, true);
	}
	
	public void fromROIsToCages(SequenceCamData seqCamData) {
		List <ROI2D> cageLimitROIList = getRoisWithCageName(seqCamData);
		Collections.sort(cageLimitROIList, new Comparators.ROI2DNameComparator());
		addMissingCages(cageLimitROIList);
		removeOrphanCages(cageLimitROIList);
	}
	
	private void addMissingCages(List<ROI2D> roiList) {
		for (ROI2D roi:roiList) {
			boolean found = false;
			for (Cage cage: cageList) {
				if (roi.getName().equals(cage.roi.getName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				Cage cage = new Cage();
				cage.roi = roi;
				cageList.add(cage);
			}
		}
	}
	
	
	private void removeOrphanCages(List<ROI2D> roiList) {
		// remove cages which names are not in the list
		Iterator<Cage> iterator = cageList.iterator();
		while (iterator.hasNext()) {
			Cage cage = iterator.next();
			boolean found = false;
			String cageRoiName = cage.roi.getName();
			for (ROI2D roi: roiList) {
				if (roi.getName().equals(cageRoiName)) {
					found = true;
					break;
				}
			}
			if (!found ) {
				iterator.remove();
			}
		}
	}
	
	private List <ROI2D> getRoisWithCageName(SequenceCamData seqCamData) {
		List<ROI2D> roiList = seqCamData.seq.getROI2Ds();
		Collections.sort(roiList, new Comparators.ROI2DNameComparator());
		
		List<ROI2D> cageList = new ArrayList<ROI2D>();
		for ( ROI2D roi : roiList ) {
			String csName = roi.getName();
			if (!(roi instanceof ROI2DPolygon))
				continue;
			if (( csName.contains( "cage") 
				|| csName.contains("Polygon2D")) ) {
				cageList.add(roi);
			}
		}
		return cageList;
	}
	
	public void fromDetectedFliesToROIs(SequenceCamData seqCamData) {
		ArrayList<ROI2D> seqlist = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: seqlist) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("det"))
				continue;
			seqCamData.seq.removeROI(roi);
		}
		
		List<ROI2D> detectedFliesList = new ArrayList<ROI2D>();
		for (Cage cage: cageList) 
			detectedFliesList.addAll(cage.detectedFliesList);
		seqCamData.seq.addROIs(detectedFliesList, true);
	}
	
//	public void fromROIstoDetectedFlies(SequenceCamData seq) {
//		detectedFliesList.clear();
//		ArrayList<ROI2D> list = seq.seq.getROI2Ds();
//		for (ROI2D roi: list) {
//			if (!(roi instanceof ROI2DShape))
//				continue;
//			if (!roi.getName().contains("det"))
//				continue;
//			detectedFliesList.add(roi);
//		}
//		Collections.sort(detectedFliesList, new Comparators.ROI2DNameComparator());
//	}
	
}
