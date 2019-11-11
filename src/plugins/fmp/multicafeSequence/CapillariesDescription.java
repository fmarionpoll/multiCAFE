package plugins.fmp.multicafeSequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;

public class CapillariesDescription {
	public int version = 1;
	public double 	volume 				= 5.;
	public int 		pixels 				= 5;
	public String 	sourceName 			= null;
	public long 	analysisStart 		= 0;
	public long 	analysisEnd 		= 0;
	public int 		analysisStep 		= 1;
	
	public String 	old_boxID			= new String("boxID");
	public String	old_experiment		= new String("experiment");
	public String 	old_comment			= new String("...");
	
	public int		grouping 			= 2;
	public String 	stimulusR			= new String("stimulusR");
	public String 	concentrationR		= new String("xmMR");
	public String 	stimulusL			= new String("stimulusL");
	public String 	concentrationL		= new String("xmML");
	
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

	

	public void copy (CapillariesDescription desc) {
		volume 			= desc.volume;
		pixels 			= desc.pixels;
		grouping 		= desc.grouping;
		analysisStart 	= desc.analysisStart;
		analysisEnd 	= desc.analysisEnd;
		analysisStep 	= desc.analysisStep;
		stimulusR 		= desc.stimulusR;
		stimulusL 		= desc.stimulusL;
		concentrationR 	= desc.concentrationR;
		concentrationL 	= desc.concentrationL;
	}
	
	public boolean isChanged (CapillariesDescription desc) {
		boolean flag = false; 
		flag = (volume != desc.volume) || flag;
		flag = (pixels != desc.pixels) || flag;
		flag = (analysisStart != desc.analysisStart) || flag;
		flag = (analysisEnd != desc.analysisEnd) || flag;
		flag = (analysisStep != desc.analysisStep) || flag;
		flag = (stimulusR != null && !stimulusR .equals(desc.stimulusR)) || flag;
		flag = (concentrationR != null && !concentrationR .equals(desc.concentrationR)) || flag;
		flag = (stimulusL != null && !stimulusL .equals(desc.stimulusL)) || flag;
		flag = (concentrationL != null && !concentrationL .equals(desc.concentrationL)) || flag;
		return flag;
	}
	
	boolean xmlSaveCapillaryDescription (Document doc, SequenceKymos seq) {
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 2);
        
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

		return true;
	}
	
	boolean xmlLoadCapillaryDescription (Document doc) {
		boolean flag = false;
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return flag;
		version = XMLUtil.getElementIntValue(node, "version", 0);
		switch (version) {
		case 0:
			flag = xmlLoadCapillaryDescriptionv0(node);
			break;
		case 1:
		default:
			flag = xmlLoadCapillaryDescriptionv1(node);
			break;
		}
		return flag;
	}
	
	private boolean xmlLoadCapillaryDescriptionv0 (Node node) {
		Element xmlElement = XMLUtil.getElement(node, ID_PARAMETERS);
		if (xmlElement == null) 
			return false;

		Element xmlVal = XMLUtil.getElement(xmlElement, ID_FILE);
		sourceName = XMLUtil.getAttributeValue(xmlVal, ID_ID, null);
		
		xmlVal = XMLUtil.getElement(xmlElement, ID_GROUPING);
		grouping = XMLUtil.getAttributeIntValue(xmlVal, ID_N, 2);
		
		xmlVal = XMLUtil.getElement(xmlElement, ID_CAPVOLUME);
		volume = XMLUtil.getAttributeDoubleValue(xmlVal, ID_VOLUMEUL, Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, ID_CAPILLARYPIX);
		pixels = (int) XMLUtil.getAttributeDoubleValue(xmlVal, ID_NPIXELS, Double.NaN);

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
			old_boxID 		= XMLUtil.getAttributeValue(xmlVal, ID_BOXID, ID_BOXID);
			old_experiment 	= XMLUtil.getAttributeValue(xmlVal, ID_EXPT, "experiment");
			old_comment 	= XMLUtil.getAttributeValue(xmlVal, ID_COMMENT, ".");
		}
		return true;
	}
	
	private boolean xmlLoadCapillaryDescriptionv1 (Node node) {
		Element xmlElement = XMLUtil.getElement(node, ID_PARAMETERS);
		if (xmlElement == null) 
			return false;

		sourceName = XMLUtil.getElementValue(xmlElement, ID_FILE, null);
		Element xmlVal 		= XMLUtil.getElement(xmlElement, "capillaries");
		if (xmlVal != null) {
			grouping		= XMLUtil.getElementIntValue(xmlVal, ID_GROUPING, 2);
			volume 			= XMLUtil.getElementDoubleValue(xmlVal, ID_VOLUMEUL, Double.NaN);
			pixels 			= XMLUtil.getElementIntValue(xmlVal, ID_NPIXELS, 5);
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
			old_boxID 		= XMLUtil.getAttributeValue(xmlVal, ID_BOXID, ID_BOXID);
			old_experiment 	= XMLUtil.getAttributeValue(xmlVal, ID_EXPT, "experiment");
			old_comment 	= XMLUtil.getAttributeValue(xmlVal, ID_COMMENT, ".");
		}
		
		return true;
	}
	


}
