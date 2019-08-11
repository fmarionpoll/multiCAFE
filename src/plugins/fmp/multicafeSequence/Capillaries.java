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
import icy.sequence.edit.ROIAddsSequenceEdit;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.DetectLimits_Options;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
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
	
	public DetectLimits_Options limitsOptions			= new DetectLimits_Options();
	public DetectGulps_Options 	gulpsOptions			= new DetectGulps_Options();
	public ArrayList <Capillary> capillariesArrayList 	= new ArrayList <Capillary>();
	
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
	
	public boolean xmlReadCapillaryParameters (Document doc) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;

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
		return true;
	}
	
	private boolean xmlWriteCapillaryParameters (Document doc, SequenceCapillaries seq) {
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		
		Element xmlElement = XMLUtil.addElement(node, ID_PARAMETERS);
		Element xmlVal = XMLUtil.addElement(xmlElement, ID_FILE);
		sourceName = seq.getFileName();
		XMLUtil.setAttributeValue(xmlVal, ID_ID, sourceName);
		
		xmlVal = XMLUtil.addElement(xmlElement, ID_GROUPING);
		XMLUtil.setAttributeIntValue(xmlVal, ID_N, grouping);
		
		xmlVal = XMLUtil.addElement(xmlElement, ID_CAPVOLUME);
		XMLUtil.setAttributeDoubleValue(xmlVal, ID_VOLUMEUL, volume);

		xmlVal = XMLUtil.addElement(xmlElement, ID_CAPILLARYPIX);
		XMLUtil.setAttributeDoubleValue(xmlVal, ID_NPIXELS, (double) pixels);

		xmlVal = XMLUtil.addElement(xmlElement, ID_ANALYSIS);
		XMLUtil.setAttributeLongValue(xmlVal, ID_START, seq.analysisStart);
		XMLUtil.setAttributeLongValue(xmlVal, ID_END, seq.analysisEnd); 
		XMLUtil.setAttributeIntValue(xmlVal, ID_STEP, seq.analysisStep); 
		
		xmlVal = XMLUtil.addElement(xmlElement, ID_LRSTIMULUS);
		XMLUtil.setAttributeValue(xmlVal, ID_STIMR, stimulusR);
		XMLUtil.setAttributeValue(xmlVal, ID_CONCR, concentrationR);
		XMLUtil.setAttributeValue(xmlVal, ID_STIML, stimulusL);
		XMLUtil.setAttributeValue(xmlVal, ID_CONCL, concentrationL);

		xmlVal = XMLUtil.addElement(xmlElement, ID_EXPERIMENT);
		XMLUtil.setAttributeValue(xmlVal, ID_BOXID, boxID);
		XMLUtil.setAttributeValue(xmlVal, ID_EXPT, experiment);
		XMLUtil.setAttributeValue(xmlVal, ID_COMMENT, comment);

		return true;
	}
	
	public boolean xmlWriteCapillaries(Document doc, SequenceCapillaries seq) {
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
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

	public void transferROIStoCapillaries (SequenceCapillaries seq) {
		ArrayList<ROI2D> list = seq.seq.getROI2Ds();
		ArrayList<ROI2D> listROISCap = new ArrayList<ROI2D> ();
		for (ROI2D roi:list) {
			if (!(roi instanceof ROI2DShape) || !roi.getName().contains("line")) 
				continue;
			if (roi instanceof ROI2DLine || roi instanceof ROI2DPolyLine)
				listROISCap.add(roi);
		}
		Collections.sort(listROISCap, new MulticafeTools.ROI2DNameComparator());
		
		if (seq.capillaries == null)
			seq.capillaries = new Capillaries();
		
		// rois not in cap?
		for (ROI2D roi:listROISCap) {
			boolean found = false;
			for (Capillary cap: seq.capillaries.capillariesArrayList) {
				if (roi.getName().equals(cap.roi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				capillariesArrayList.add(new Capillary((ROI2DShape)roi));
		}
		
		// cap with no corresponding roi?
		for (Capillary cap: seq.capillaries.capillariesArrayList) {
			boolean found = false;
			for (ROI2D roi:listROISCap) {
				if (roi.getName().equals(cap.roi.getName())) {
					found = true;
					break;
				}
			}
			if (!found)
				seq.capillaries.capillariesArrayList.remove(cap);
		}
	}
	
	public boolean xmlWriteROIsAndData(String name, SequenceCapillaries seq) {
		String csFile = MulticafeTools.saveFileAs(name, seq.getDirectory(), "xml");
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		return xmlWriteROIsAndDataNoQuestion(csFile, seq);
	}
	
	public boolean xmlWriteROIsAndDataNoQuestion(String csFile, SequenceCapillaries seq) {
		if (csFile != null) {
			final Document doc = XMLUtil.createDocument(true);
			if (doc != null) {
				List<ROI> roisList = new ArrayList<ROI>();
				ROI.saveROIsToXML(XMLUtil.getRootElement(doc), roisList);
				xmlWriteCapillaryParameters (doc, seq);
				xmlWriteCapillaries(doc, seq);
				XMLUtil.saveDocument(doc, csFile);
				return true;
			}
		}
		return false;
	}
	
	public boolean xmlWriteROIsAndDataNoFilter(String name, SequenceCapillaries seq) {
		String csFile = MulticafeTools.saveFileAs(name, seq.getDirectory(), "xml");
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		
		final Document doc = XMLUtil.createDocument(true);
		if (doc != null) {
			List<ROI> roisList = seq.seq.getROIs();
			ROI.saveROIsToXML(XMLUtil.getRootElement(doc), roisList);
			xmlWriteCapillaryParameters (doc, seq);
			XMLUtil.saveDocument(doc, csFile);
			return true;
		}
		return false;
	}
	
	public boolean xmlReadROIsAndData(SequenceCapillaries seq) {
		String [] filedummy = null;
		String filename = seq.getFileName();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		filedummy = MulticafeTools.selectFiles(directory, "xml");
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i= 0; i< filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadROIsAndData(csFile, seq);
			}
		}
		return wasOk;
	}
	
	public boolean xmlReadROIsAndData(String csFileName, SequenceCapillaries seq) {
		if (csFileName != null)  {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc != null) {
				xmlReadCapillaryParameters(doc);
				List<ROI> listOfROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
				for (ROI roi: listOfROIs) {
					if (!isAlreadyStoredAsCapillary(roi.getName()))
						capillariesArrayList.add(new Capillary((ROI2DShape) roi));
				}
				try  {  
					for (Capillary cap : capillariesArrayList)  {
						seq.seq.addROI(cap.roi);
					}
				}
				finally {
				}
				// add to undo manager
				seq.seq.addUndoableEdit(new ROIAddsSequenceEdit(seq.seq, listOfROIs) {
					@Override
					public String getPresentationName() {
						if (getROIs().size() > 1)
							return "ROIs loaded from XML file";
						return "ROI loaded from XML file"; };
				});
				return true;
			}
		}
		return false;
	}
	
	private boolean isAlreadyStoredAsCapillary(String name) {
		boolean flag = false;
		for (Capillary cap: capillariesArrayList) {
			if (name .equals (cap.getName())) {
				flag = true;
				break;
			}
		}
		return flag;
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
