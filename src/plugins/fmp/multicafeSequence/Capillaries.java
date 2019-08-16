package plugins.fmp.multicafeSequence;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import icy.roi.ROI;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.DetectLimits_Options;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class Capillaries {
	
	public double 	volume 				= 5.;
	public int 		pixels 				= 5;
	public int		grouping 			= 2;
	public String 	sourceName 			= null;
	public long 	analysisStart 		= 0;
	public long 	analysisEnd 		= 0;
	public int 		analysisStep 		= 1;
	
	public String 	boxID				= new String("boxID");
	public String	experiment			= new String("experiment");
	public String 	comment				= new String("...");
	public String 	stimulusR			= new String("stimulusR");
	public String 	concentrationR		= new String("xmMR");
	public String 	stimulusL			= new String("stimulusL");
	public String 	concentrationL		= new String("xmML");
	
	public DetectLimits_Options limitsOptions		= new DetectLimits_Options();
	public DetectGulps_Options 	gulpsOptions		= new DetectGulps_Options();
	public List <Capillary> capillariesArrayList 	= new ArrayList <Capillary>();
	
	private final static String ID_CAPILLARYTRACK = "capillaryTrack";
	private final static String ID_PARAMETERS = "Parameters";	
	private final static String ID_FILE = "file";
	private final static String ID_ID = "ID";
	private final static String ID_GROUPING = "Grouping";
	private final static String ID_N = "n";
	private final static String ID_CAPVOLUME = "capillaryVolume";
	private final static String ID_VOLUMEUL = "volume_ul";
	private final static String ID_CAPILLARYPIX = "capillaryPixels";
	private final static String ID_NPIXELS = "npixels";
	private final static String ID_ANALYSIS = "analysis";
	private final static String ID_START = "start";
	private final static String ID_END = "end";
	private final static String ID_STEP = "step";
	private final static String ID_LRSTIMULUS = "LRstimulus";
	private final static String ID_STIMR = "stimR";
	private final static String ID_CONCR = "concR";
	private final static String ID_STIML = "stimL";
	private final static String ID_CONCL = "concL";
	private final static String ID_EXPERIMENT = "Experiment";
	private final static String ID_BOXID = "boxID";
	private final static String ID_EXPT = "expt";
	private final static String ID_COMMENT = "comment";
	private final static String ID_NCAPILLARIES = "N_capillaries";
	private final static String ID_LISTOFCAPILLARIES = "List_of_capillaries";
	private final static String ID_CAPILLARY_ = "capillary_";

	
	// ------------------------------------------------------------
	
	public int xmlLoadCapillaryParametersv1 (Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return -1;
		int version = XMLUtil.getElementIntValue(node, "version", 0);
		if (version != 1)
			return version;
		
		Element xmlElement = XMLUtil.getElement(node, ID_PARAMETERS);
		if (xmlElement == null) 
			return -1;

		sourceName = XMLUtil.getElementValue(xmlElement, ID_FILE, null);
		Element xmlVal = XMLUtil.getElement(xmlElement, "capillaries");
		if (xmlVal != null) {
			grouping = XMLUtil.getElementIntValue(xmlVal, ID_GROUPING, 2);
			volume = XMLUtil.getElementDoubleValue(xmlVal, ID_VOLUMEUL, Double.NaN);
			pixels = XMLUtil.getElementIntValue(xmlVal, ID_NPIXELS, 5);
		}

		xmlVal = XMLUtil.getElement(xmlElement, ID_ANALYSIS);
		if (xmlVal != null) {
			analysisStart 	= XMLUtil.getElementLongValue(xmlVal, ID_START, 0);
			analysisEnd 	= XMLUtil.getElementLongValue(xmlVal, ID_END, 0);
			analysisStep 	= XMLUtil.getElementIntValue(xmlVal, ID_STEP, 1);
		}

		xmlVal = XMLUtil.getElement(xmlElement, ID_LRSTIMULUS);
		if (xmlVal != null) {
			stimulusR 		= XMLUtil.getElementValue(xmlVal, ID_STIMR, ID_STIMR);
			concentrationR 	= XMLUtil.getElementValue(xmlVal, ID_CONCR, ID_CONCR);
			stimulusL 		= XMLUtil.getElementValue(xmlVal, ID_STIML, ID_STIML);
			concentrationL 	= XMLUtil.getElementValue(xmlVal, ID_CONCL, ID_CONCL);
		}
		
		xmlVal = XMLUtil.getElement(xmlElement, ID_EXPERIMENT);
		if (xmlVal != null) {
			boxID 		= XMLUtil.getElementValue(xmlVal, ID_BOXID, ID_BOXID);
			experiment 	= XMLUtil.getElementValue(xmlVal, ID_EXPT, "experiment");
			comment 	= XMLUtil.getElementValue(xmlVal, ID_COMMENT, ".");
		}
		return version;
	}

	private boolean xmlSaveCapillaryParametersv1 (Document doc, SequenceKymos seq) {
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 1);
        
		Element xmlElement = XMLUtil.addElement(node, ID_PARAMETERS);
		
		XMLUtil.addElement(xmlElement, ID_FILE, sourceName);
		Element xmlVal = XMLUtil.addElement(xmlElement, "capillaries");
		XMLUtil.setElementIntValue(xmlVal, ID_GROUPING, grouping);
		XMLUtil.setElementDoubleValue(xmlVal, ID_VOLUMEUL, volume);
		XMLUtil.setElementIntValue(xmlVal, ID_NPIXELS, pixels);

		xmlVal = XMLUtil.addElement(xmlElement, ID_ANALYSIS);
		XMLUtil.setElementLongValue(xmlVal, ID_START, seq.analysisStart);
		XMLUtil.setElementLongValue(xmlVal, ID_END, seq.analysisEnd); 
		XMLUtil.setElementIntValue(xmlVal, ID_STEP, seq.analysisStep); 
		
		xmlVal = XMLUtil.addElement(xmlElement, ID_LRSTIMULUS);
		XMLUtil.setElementValue(xmlVal, ID_STIMR, stimulusR);
		XMLUtil.setElementValue(xmlVal, ID_CONCR, concentrationR);
		XMLUtil.setElementValue(xmlVal, ID_STIML, stimulusL);
		XMLUtil.setElementValue(xmlVal, ID_CONCL, concentrationL);

		xmlVal = XMLUtil.addElement(xmlElement, ID_EXPERIMENT);
		XMLUtil.setElementValue(xmlVal, ID_BOXID, boxID);
		XMLUtil.setElementValue(xmlVal, ID_EXPT, experiment);
		XMLUtil.setElementValue(xmlVal, ID_COMMENT, comment);

		return true;
	}
	
	public boolean xmlSaveCapillariesv1(Document doc, SequenceKymos seq) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 1);
		Node nodecaps = XMLUtil.setElement(node, ID_LISTOFCAPILLARIES);
		XMLUtil.setElementIntValue(nodecaps, ID_NCAPILLARIES, seq.capillaries.capillariesArrayList.size());
		int i= 0;
		for (Capillary cap: seq.capillaries.capillariesArrayList) {
			Node nodecapillary = XMLUtil.setElement(node, ID_CAPILLARY_+i);
			cap.saveToXML(nodecapillary);
			i++;
		}
		return true;
	}
	
	public boolean xmlLoadCapillariesv1(Document doc, SequenceKymos seq) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		Node nodecaps = XMLUtil.getElement(node, ID_LISTOFCAPILLARIES);
		int nitems = XMLUtil.getElementIntValue(nodecaps, ID_NCAPILLARIES, 0);
		seq.capillaries.capillariesArrayList = new ArrayList<Capillary> (nitems);
		for (int i= 0; i< nitems; i++) {
			Node nodecapillary = XMLUtil.getElement(node, ID_CAPILLARY_+i);
			Capillary cap = new Capillary();
			cap.loadFromXML(nodecapillary);
			seq.capillaries.capillariesArrayList.add(cap);
		}
		return true;
	}

	// ---------------------------------
	
	public int xmlReadCapillaryParametersv0 (Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return -1;
		int version = 0;

		Element xmlElement = XMLUtil.getElement(node, ID_PARAMETERS);
		if (xmlElement == null) 
			return -1;

		Element xmlVal = XMLUtil.getElement(xmlElement, ID_FILE);
		sourceName = XMLUtil.getAttributeValue(xmlVal, ID_ID, null);
		
		xmlVal = XMLUtil.getElement(xmlElement, ID_GROUPING);
		grouping = XMLUtil.getAttributeIntValue(xmlVal, ID_N, 2);
		
		xmlVal = XMLUtil.getElement(xmlElement, ID_CAPVOLUME);
		volume = XMLUtil.getAttributeDoubleValue(xmlVal, ID_VOLUMEUL, Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, ID_CAPILLARYPIX);
		double dpixels = XMLUtil.getAttributeDoubleValue(xmlVal, ID_NPIXELS, Double.NaN);
		pixels = (int) dpixels;

		xmlVal = XMLUtil.getElement(xmlElement, ID_ANALYSIS);
		if (xmlVal != null) {
			analysisStart 	= XMLUtil.getAttributeLongValue(xmlVal, ID_START, 0);
			analysisEnd 	= XMLUtil.getAttributeLongValue(xmlVal, ID_END, 0);
			analysisStep 	= XMLUtil.getAttributeIntValue(xmlVal, ID_STEP, 1);
		}

		xmlVal = XMLUtil.getElement(xmlElement, ID_LRSTIMULUS);
		if (xmlVal != null) {
			stimulusR 		= XMLUtil.getAttributeValue(xmlVal, ID_STIMR, ID_STIMR);
			concentrationR 	= XMLUtil.getAttributeValue(xmlVal, ID_CONCR, ID_CONCR);
			stimulusL 		= XMLUtil.getAttributeValue(xmlVal, ID_STIML, ID_STIML);
			concentrationL 	= XMLUtil.getAttributeValue(xmlVal, ID_CONCL, ID_CONCL);
		}
		
		xmlVal = XMLUtil.getElement(xmlElement, ID_EXPERIMENT);
		if (xmlVal != null) {
			boxID 		= XMLUtil.getAttributeValue(xmlVal, ID_BOXID, ID_BOXID);
			experiment 	= XMLUtil.getAttributeValue(xmlVal, ID_EXPT, "experiment");
			comment 	= XMLUtil.getAttributeValue(xmlVal, ID_COMMENT, ".");
		}
		return version;
	}
	
	public boolean xmlWriteROIsAndData(String name, SequenceKymos seq) {
		String csFile = MulticafeTools.saveFileAs(name, seq.getDirectory(), "xml");
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		return xmlSaveCapillaries(csFile, seq);
	}
	
	public boolean xmlSaveCapillaries(String csFile, SequenceKymos seq) {
		if (csFile != null) {
			final Document doc = XMLUtil.createDocument(true);
			if (doc != null) {
				xmlSaveCapillaryParametersv1 (doc, seq);
				xmlSaveCapillariesv1(doc, seq);
				XMLUtil.saveDocument(doc, csFile);
				return true;
			}
		}
		return false;
	}
	
	public boolean xmlLoadCapillaries(String csFileName, SequenceKymos seq) {
		if (csFileName != null)  {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc != null) {
				int version = xmlLoadCapillaryParametersv1(doc);
				switch (version) {
				case 1: // current xml storage structure
					xmlLoadCapillariesv1(doc, seq);
					break;
				case 0: // old xml storage structure
					xmlReadCapillaryParametersv0(doc);
					xmlLoadCapillariesv0(doc, csFileName);
					break;
				default:
					return false;
				}
 				return true;
			}
		}
		return false;
	}
	
	private void xmlLoadCapillariesv0(Document doc, String csFileName) {
		// load xml files stored in "results"
		int t = 0;
		List<ROI> listOfCapillaryROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
		capillariesArrayList.clear();
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator + "results"+File.separator;
		// then load measures stored into individual files
		for (ROI roiCapillary: listOfCapillaryROIs) {
			Capillary cap = new Capillary((ROI2DShape) roiCapillary);
			capillariesArrayList.add(cap);
			String csFile = directory + roiCapillary.getName() + ".xml";
			//cap.filenameTIFF = directory + roiCapillary.getName() + ".tiff";
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
	
	public void copy (Capillaries cap) {
		volume = cap.volume;
		pixels = cap.pixels;
		grouping = cap.grouping;
		analysisStart = cap.analysisStart;
		analysisEnd = cap.analysisEnd;
		analysisStep = cap.analysisStep;
		stimulusR = cap.stimulusR;
		stimulusL = cap.stimulusL;
		concentrationR = cap.concentrationR;
		concentrationL = cap.concentrationL;
		boxID = cap.boxID;
		experiment = cap.experiment;
		comment = cap.comment;
		
		capillariesArrayList.clear();
		for (Capillary ccap: cap.capillariesArrayList)
			capillariesArrayList.add(ccap);
	}
	
	public boolean isChanged (Capillaries cap) {
		boolean flag = false; 
		flag = (cap.volume != volume) || flag;
		flag = (cap.pixels != pixels) || flag;
		flag = (cap.analysisStart != analysisStart) || flag;
		flag = (cap.analysisEnd != analysisEnd) || flag;
		flag = (cap.analysisStep != analysisStep) || flag;
		flag = (stimulusR != null && !cap.stimulusR .equals(stimulusR)) || flag;
		flag = (concentrationR != null && !cap.concentrationR .equals(concentrationR)) || flag;
		flag = (stimulusL != null && !cap.stimulusL .equals(stimulusL)) || flag;
		flag = (concentrationL != null && !cap.concentrationL .equals(concentrationL)) || flag;
		flag = (cap.boxID != null && !cap.boxID .equals(boxID)) || flag;
		flag = (cap.experiment != null && !cap.experiment .equals(experiment)) || flag;
		flag = (cap.comment != null && !cap.comment .equals(comment)) || flag;
		return flag;
	}

}
