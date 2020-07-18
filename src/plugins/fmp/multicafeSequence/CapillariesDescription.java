package plugins.fmp.multicafeSequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.util.XMLUtil;

public class CapillariesDescription {
	public int version = 1;
	public double 	volume 			= 5.;
	public int 		pixels 			= 5;
	public String 	sourceName 		= null;
	public long 	analysisStart 	= 0;
	public long 	analysisEnd 	= 0;
	public int 		analysisStep 	= 1;
	
	public String 	old_boxID		= new String("..");
	public String	old_experiment	= new String("..");
	public String 	old_comment1	= new String("..");
	public String 	old_comment2	= new String("..");
	
	public int		grouping 		= 2;
	String 			stimulusR		= new String("..");
	String 			concentrationR	= new String("..");
	String 			stimulusL		= new String("..");
	String 			concentrationL	= new String("..");
	
	private final String ID_CAPILLARYTRACK 	= "capillaryTrack";
	private final String ID_PARAMETERS 		= "Parameters";	
	private final String ID_FILE 			= "file";
	private final String ID_ID 				= "ID";
	private final String ID_DESCGROUPING 	= "Grouping";
	private final String ID_DESCN 			= "n";
	private final String ID_DESCCAPVOLUME 	= "capillaryVolume";
	private final String ID_DESCVOLUMEUL 	= "volume_ul";
	private final String ID_DESCCAPILLARYPIX = "capillaryPixels";
	private final String ID_DESCNPIXELS 	= "npixels";

	private final String ID_LRSTIMULUS 		= "LRstimulus";
	private final String ID_STIMR 			= "stimR";
	private final String ID_CONCR 			= "concR";
	private final String ID_STIML 			= "stimL";
	private final String ID_CONCL 			= "concL";
	private final String ID_EXPERIMENT 		= "Experiment";
	private final String ID_BOXID 			= "boxID";
	private final String ID_EXPT 			= "expt";
	private final String ID_COMMENT1 		= "comment";
	private final String ID_COMMENT2 		= "comment2";
	private final String ID_NOPE 			= "..";

	

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
		flag |= (volume != desc.volume);
		flag |= (pixels != desc.pixels) ;
		flag |= (grouping != desc.grouping);
		flag |= (analysisStart != desc.analysisStart);
		flag |= (analysisEnd != desc.analysisEnd);
		flag |= (analysisStep != desc.analysisStep);
		flag |= (stimulusR != null && !stimulusR .equals(desc.stimulusR));
		flag |= (concentrationR != null && !concentrationR .equals(desc.concentrationR));
		flag |= (stimulusL != null && !stimulusL .equals(desc.stimulusL));
		flag |= (concentrationL != null && !concentrationL .equals(desc.concentrationL));
		return flag;
	}
	
	boolean xmlSaveCapillaryDescription (Document doc) {
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 2);
        
		Element xmlElement = XMLUtil.addElement(node, ID_PARAMETERS);
		
		XMLUtil.addElement(xmlElement, ID_FILE, sourceName);
		Element xmlVal = XMLUtil.addElement(xmlElement, "capillaries");
		XMLUtil.setElementIntValue(xmlVal, ID_DESCGROUPING, grouping);
		XMLUtil.setElementDoubleValue(xmlVal, ID_DESCVOLUMEUL, volume);
		XMLUtil.setElementIntValue(xmlVal, ID_DESCNPIXELS, pixels);

		xmlVal = XMLUtil.addElement(xmlElement, ID_EXPERIMENT);
		XMLUtil.setElementValue(xmlVal, ID_BOXID, old_boxID);
		XMLUtil.setElementValue(xmlVal, ID_EXPT, old_experiment);
		XMLUtil.setElementValue(xmlVal, ID_COMMENT1, old_comment1);
		XMLUtil.setElementValue(xmlVal, ID_COMMENT2, old_comment2);
			
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
		
		xmlVal = XMLUtil.getElement(xmlElement, ID_DESCGROUPING);
		grouping = XMLUtil.getAttributeIntValue(xmlVal, ID_DESCN, 2);
		
		xmlVal = XMLUtil.getElement(xmlElement, ID_DESCCAPVOLUME);
		volume = XMLUtil.getAttributeDoubleValue(xmlVal, ID_DESCVOLUMEUL, Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, ID_DESCCAPILLARYPIX);
		pixels = (int) XMLUtil.getAttributeDoubleValue(xmlVal, ID_DESCNPIXELS, Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, ID_LRSTIMULUS);
		if (xmlVal != null) {
			stimulusR 		= XMLUtil.getAttributeValue(xmlVal, ID_STIMR, ID_STIMR);
			concentrationR 	= XMLUtil.getAttributeValue(xmlVal, ID_CONCR, ID_CONCR);
			stimulusL 		= XMLUtil.getAttributeValue(xmlVal, ID_STIML, ID_STIML);
			concentrationL 	= XMLUtil.getAttributeValue(xmlVal, ID_CONCL, ID_CONCL);
		}
		
		xmlVal = XMLUtil.getElement(xmlElement, ID_EXPERIMENT);
		if (xmlVal != null) {
			old_boxID 		= XMLUtil.getAttributeValue(xmlVal, ID_BOXID, ID_NOPE);
			old_experiment 	= XMLUtil.getAttributeValue(xmlVal, ID_EXPT, ID_NOPE);
			old_comment1 	= XMLUtil.getAttributeValue(xmlVal, ID_COMMENT1, ID_NOPE);
			old_comment2 	= XMLUtil.getAttributeValue(xmlVal, ID_COMMENT2, ID_NOPE);
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
			grouping		= XMLUtil.getElementIntValue(xmlVal, ID_DESCGROUPING, 2);
			volume 			= XMLUtil.getElementDoubleValue(xmlVal, ID_DESCVOLUMEUL, Double.NaN);
			pixels 			= XMLUtil.getElementIntValue(xmlVal, ID_DESCNPIXELS, 5);
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
			old_boxID 		= XMLUtil.getElementValue(xmlVal, ID_BOXID, ID_NOPE);
			old_experiment 	= XMLUtil.getElementValue(xmlVal, ID_EXPT, ID_NOPE);
			old_comment1 	= XMLUtil.getElementValue(xmlVal, ID_COMMENT1, ID_NOPE);
			old_comment2 	= XMLUtil.getElementValue(xmlVal, ID_COMMENT2, ID_NOPE);
		}
		
		return true;
	}
	


}
