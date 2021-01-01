package plugins.fmp.multicafe.sequence;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import icy.roi.ROI;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.series.Options_BuildSeries;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class Capillaries {
	
	public CapillariesDescription 	desc				= new CapillariesDescription();
	public CapillariesDescription 	desc_old			= new CapillariesDescription();
	public List <Capillary> 		capillariesArrayList= new ArrayList <Capillary>();
	public Options_BuildSeries 		limitsOptions		= new Options_BuildSeries();
	
	private final static String ID_CAPILLARYTRACK 		= "capillaryTrack";
	private final static String ID_NCAPILLARIES 		= "N_capillaries";
	private final static String ID_LISTOFCAPILLARIES 	= "List_of_capillaries";
	private final static String ID_CAPILLARY_ 			= "capillary_";

	// ---------------------------------
		
	String getXMLNameToAppend() {
		return "MCcapillaries.xml";
	}

	public boolean xmlSaveCapillaries_Descriptors(String csFileName) {
		if (csFileName != null) {
			final Document doc = XMLUtil.createDocument(true);
			if (doc != null) {
				Collections.sort(capillariesArrayList);
				desc.xmlSaveCapillaryDescription (doc);
				xmlSaveListOfCapillaries(doc);
				return XMLUtil.saveDocument(doc, csFileName);
			}
		}
		return false;
	}
	
	public boolean xmlSaveCapillaries_Measures(String csFileName) {
		if (csFileName == null)
			return false;
		Collections.sort(capillariesArrayList);
		for (Capillary cap: capillariesArrayList) {
			String tempname = csFileName + File.separator + cap.getCapillaryName()+ ".xml";
			final Document capdoc = XMLUtil.createDocument(true);
			cap.saveToXML(XMLUtil.getRootElement(capdoc, true));
			XMLUtil.saveDocument(capdoc, tempname);
		}
		return true;
	}
	
	public boolean xmlLoadCapillaries_Descriptors(String csFileName) {	
		if (csFileName == null)
			return false;
				
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) {
			desc.xmlLoadCapillaryDescription(doc);
			return xmlLoadCapillariesv1(doc);		
		}
		return false;
	}
	
	public boolean xmlLoadOldCapillaries_Only(String csFileName) {
		if (csFileName == null)
			return false;
				
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) {
			desc.xmlLoadCapillaryDescription(doc);
			xmlLoadCapillariesv1(doc);
			switch (desc.version) {
			case 1: // old xml storage structure
				xmlLoadCapillariesv1(doc);
				break;
			case 0: // old-old xml storage structure
				xmlLoadCapillariesv0(doc, csFileName);
				break;
			default:
				xmlLoadCapillariesv2(doc, csFileName);
				return false;
			}		
		return true;
		}
		return false;
	}
	
	public boolean xmlLoadCapillaries_Measures(String csFileName) { // TODO
		if (csFileName == null)
			return false;
		
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) {
			switch (desc.version) {
			case 2:	// current xml storage structure
				xmlLoadCapillariesv2(doc, csFileName);
				break;
			case 1: // old xml storage structure
				xmlLoadCapillariesv1(doc);
				break;
			case 0: // old-old xml storage structure
				xmlLoadCapillariesv0(doc, csFileName);
				break;
			default:
				return false;
			}		
			Collections.sort(capillariesArrayList);
			transferDescToCapillariesVersionInfos0();
			return true;
		}
		return false;
	}
	
	public boolean xmlLoadCapillaries_Measures2(String directory) {
		boolean flag = true;
		for (Capillary cap: capillariesArrayList) {
			String csFile = directory + File.separator + cap.getCapillaryName() + ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			Node node = XMLUtil.getRootElement(capdoc, true);
			flag &= cap.loadFromXML(node);
		}
		return flag;
	}
		
	private void transferDescToCapillariesVersionInfos0() {
		for (Capillary cap: capillariesArrayList) {
			if (cap.versionInfos == 0) {
				transferDescriptionToCapillary(cap);
				transferCapGroupToCapillary (cap);
			}
		}
	}
	
	private boolean xmlSaveListOfCapillaries(Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 2);
		Node nodecaps = XMLUtil.setElement(node, ID_LISTOFCAPILLARIES);
		XMLUtil.setElementIntValue(nodecaps, ID_NCAPILLARIES, capillariesArrayList.size());
		int i= 0;
		Collections.sort(capillariesArrayList);
		for (Capillary cap: capillariesArrayList) {
			Node nodecapillary = XMLUtil.setElement(node, ID_CAPILLARY_+i);
			cap.saveToXML_CapillaryOnly(nodecapillary);
			i++;
		}
		return true;
	}
	
	private void xmlLoadCapillariesv0(Document doc, String csFileName) {
		// load xml files stored in "results"
		int t = 0;
		List<ROI> listOfCapillaryROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
		capillariesArrayList.clear();
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		// then load measures stored into individual files
		for (ROI roiCapillary: listOfCapillaryROIs) {
			Capillary cap = new Capillary((ROI2DShape) roiCapillary);
			capillariesArrayList.add(cap);
			String csFile = directory + roiCapillary.getName() + ".xml";
			cap.indexImage = t;
			t++;
			final Document dockymo = XMLUtil.loadDocument(csFile);
			if (dockymo != null) {
				NodeList nodeROISingle = dockymo.getElementsByTagName("roi");					
				if (nodeROISingle.getLength() > 0) {	
					List<ROI> rois = new ArrayList<ROI>();
	                for (int i=0; i< nodeROISingle.getLength(); i++) {
	                	Node element = nodeROISingle.item(i);
	                    ROI roi_i = ROI.createFromXML(element);
	                    if (roi_i != null)
	                        rois.add(roi_i);
	                }
					cap.transferROIsToMeasures(rois);
				}
			}
		}
	}

	private boolean xmlLoadCapillariesv1(Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		Node nodecaps = XMLUtil.getElement(node, ID_LISTOFCAPILLARIES);
		int nitems = XMLUtil.getElementIntValue(nodecaps, ID_NCAPILLARIES, 0);
		capillariesArrayList = new ArrayList<Capillary> (nitems);
		for (int i= 0; i< nitems; i++) {
			Node nodecapillary = XMLUtil.getElement(node, ID_CAPILLARY_+i);
			Capillary cap = new Capillary();
			cap.loadFromXML(nodecapillary);
			if (desc.grouping == 2 && (cap.capStimulus != null && cap.capStimulus.equals(".."))) {
				if (cap.getCapillarySide().equals("R")) {
					cap.capStimulus = desc.stimulusR;
					cap.capConcentration = desc.concentrationR;
				} else {
					cap.capStimulus = desc.stimulusL;
					cap.capConcentration = desc.concentrationL;
				}
			}
			capillariesArrayList.add(cap);
		}
		return true;
	}
	
	private void xmlLoadCapillariesv2(Document doc, String csFileName) {
		xmlLoadCapillariesv1(doc);
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		for (Capillary cap: capillariesArrayList) {
			String csFile = directory + cap.getCapillaryName() + ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			Node node = XMLUtil.getRootElement(capdoc, true);
			cap.loadFromXML(node);
		}
	}	

	public void copy (Capillaries cap) {
		desc.copy(cap.desc);
		capillariesArrayList.clear();
		for (Capillary ccap: cap.capillariesArrayList) {
			Capillary capi = new Capillary();
			capi.copy(ccap);
			capillariesArrayList.add(capi);
		}
	}
	
	public boolean isPresent(Capillary capnew) {
		boolean flag = false;
		for (Capillary cap: capillariesArrayList) {
			if (cap.capillaryName.contentEquals(capnew.capillaryName)) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	public void mergeLists(Capillaries caplist) {
		for (Capillary capm : caplist.capillariesArrayList ) {
			if (!isPresent(capm))
				capillariesArrayList.add(capm);
		}
	}
	
	public void adjustToImageWidth (int imageWidth) {
		for (Capillary cap: capillariesArrayList) {
			cap.ptsTop.adjustToImageWidth(imageWidth);
			cap.ptsBottom.adjustToImageWidth(imageWidth);
			cap.ptsDerivative.adjustToImageWidth(imageWidth);
			cap.gulpsRois = null; // TODO: deal with gulps.. (simply remove?)
		}
	}

	public void transferDescriptionToCapillaries() {
		for (Capillary cap: capillariesArrayList) {
			transferCapGroupToCapillary(cap);
			transferDescriptionToCapillary (cap);
		}
	}
	
	private void transferDescriptionToCapillary (Capillary cap) {
		cap.capVolume = desc.volume;
		cap.capPixels = desc.pixels;
		cap.descriptionOK = true;
	}
	
	private void transferCapGroupToCapillary (Capillary cap) {
		if (desc.grouping != 2)
			return;
		String	name = cap.roi.getName();
		String letter = name.substring(name.length() - 1);
		cap.capSide = letter;
		if (letter .equals("R")) {	
			String nameL = name.substring(0, name.length() - 1) + "L";
			Capillary cap0 = getCapillaryFromName(nameL);
			if (cap0 != null) {
				cap.capNFlies = cap0.capNFlies;
				cap.capCageID = cap0.capCageID;
			}
		}
	}
	
	private Capillary getCapillaryFromName(String name) {
		Capillary capFound = null;
		for (Capillary cap: capillariesArrayList) {
			if (cap.roi.getName().equals(name)) {
				capFound = cap;
				break;
			}
		}
		return capFound;
	}
}
