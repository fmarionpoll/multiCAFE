package plugins.fmp.multicafe2.experiment;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.series.Options_BuildSeries;
import plugins.fmp.multicafe2.tools.Comparators;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class Capillaries 
{	
	public CapillariesDescription 	desc				= new CapillariesDescription();
	public CapillariesDescription 	desc_old			= new CapillariesDescription();
	public List <Capillary> 		capillariesArrayList= new ArrayList <Capillary>();
	public Options_BuildSeries 		limitsOptions		= new Options_BuildSeries();
	
	private final static String ID_CAPILLARYTRACK 		= "capillaryTrack";
	private final static String ID_NCAPILLARIES 		= "N_capillaries";
	private final static String ID_LISTOFCAPILLARIES 	= "List_of_capillaries";
	private final static String ID_CAPILLARY_ 			= "capillary_";

	// ---------------------------------
		
	String getXMLNameToAppend() 
	{
		return "MCcapillaries.xml";
	}

	public boolean xmlSaveCapillaries_Descriptors(String csFileName) 
	{
		if (csFileName != null) 
		{
			final Document doc = XMLUtil.createDocument(true);
			if (doc != null) 
			{
				desc.xmlSaveCapillaryDescription (doc);
				xmlSaveListOfCapillaries(doc);
				return XMLUtil.saveDocument(doc, csFileName);
			}
		}
		return false;
	}
	
	public boolean xmlSaveCapillaries_Measures(String directory) 
	{
		if (directory == null)
			return false;
		
		for (Capillary cap: capillariesArrayList) 
		{
			if (cap.roi == null)
				continue;
			String tempname = directory + File.separator + cap.getCapillaryName()+ ".xml";

			final Document capdoc = XMLUtil.createDocument(true);
			cap.saveToXML(XMLUtil.getRootElement(capdoc, true));
			XMLUtil.saveDocument(capdoc, tempname);
		}
		return true;
	}
	
	public boolean xmlLoadCapillaries_Descriptors(String csFileName) 
	{	
		if (csFileName == null)
			return false;
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) 
		{
			desc.xmlLoadCapillaryDescription(doc);
			return xmlLoadCapillaries_Only_v1(doc);		
		}
		return false;
	}
	
	public boolean xmlLoadOldCapillaries_Only(String csFileName) 
	{
		if (csFileName == null)
			return false;			
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) 
		{
			desc.xmlLoadCapillaryDescription(doc);
			switch (desc.version) 
			{
			case 1: // old xml storage structure
				xmlLoadCapillaries_Only_v1(doc);
				break;
			case 0: // old-old xml storage structure
				xmlLoadCapillaries_v0(doc, csFileName);
				break;
			default:
				xmlLoadCapillaries_Only_v2(doc, csFileName);
				return false;
			}		
			return true;
		}
		return false;
	}
	
	public boolean xmlLoadCapillaries_Measures(String directory) 
	{
		boolean flag = true;
		int ncapillaries = capillariesArrayList.size();

		for (int i=0; i< ncapillaries; i++) 
		{
			String csFile = directory + File.separator + capillariesArrayList.get(i).getCapillaryName() + ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			Node node = XMLUtil.getRootElement(capdoc, true);
			Capillary cap = capillariesArrayList.get(i);
			cap.indexImage = i;
			flag &= cap.loadFromXML_MeasuresOnly(node);
		}
		return flag;
	}
	
	private boolean xmlSaveListOfCapillaries(Document doc) 
	{
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 2);
		Node nodecaps = XMLUtil.setElement(node, ID_LISTOFCAPILLARIES);
		XMLUtil.setElementIntValue(nodecaps, ID_NCAPILLARIES, capillariesArrayList.size());
		int i= 0;
		Collections.sort(capillariesArrayList);
		for (Capillary cap: capillariesArrayList) 
		{
			Node nodecapillary = XMLUtil.setElement(node, ID_CAPILLARY_+i);
			cap.saveToXML_CapillaryOnly(nodecapillary);
			i++;
		}
		return true;
	}
	
	private void xmlLoadCapillaries_v0(Document doc, String csFileName) 
	{
		List<ROI> listOfCapillaryROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
		capillariesArrayList.clear();
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		int t = 0;
		for (ROI roiCapillary: listOfCapillaryROIs) 
		{
			xmlLoadIndividualCapillary_v0((ROI2DShape) roiCapillary, directory, t);
			t++;
		}
	}
	
	private void xmlLoadIndividualCapillary_v0(ROI2DShape roiCapillary, String directory, int t) 
	{
		Capillary cap = new Capillary((ROI2DShape) roiCapillary);
		if (!isPresent(cap))
			capillariesArrayList.add(cap);
		String csFile = directory + roiCapillary.getName() + ".xml";
		cap.indexImage = t;
		final Document dockymo = XMLUtil.loadDocument(csFile);
		if (dockymo != null) 
		{
			NodeList nodeROISingle = dockymo.getElementsByTagName("roi");					
			if (nodeROISingle.getLength() > 0) 
			{	
				List<ROI> rois = new ArrayList<ROI>();
                for (int i=0; i< nodeROISingle.getLength(); i++) 
                {
                	Node element = nodeROISingle.item(i);
                    ROI roi_i = ROI.createFromXML(element);
                    if (roi_i != null)
                        rois.add(roi_i);
                }
				cap.transferROIsToMeasures(rois);
			}
		}
	}

//	private boolean xmlLoadCapillaries_v1(Document doc) 
//	{
//		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
//		if (node == null)
//			return false;
//		Node nodecaps = XMLUtil.getElement(node, ID_LISTOFCAPILLARIES);
//		int nitems = XMLUtil.getElementIntValue(nodecaps, ID_NCAPILLARIES, 0);
//		capillariesArrayList = new ArrayList<Capillary> (nitems);
//		for (int i= 0; i< nitems; i++) 
//		{
//			Node nodecapillary = XMLUtil.getElement(node, ID_CAPILLARY_+i);
//			Capillary capNew = new Capillary();
//			capNew.loadFromXML(nodecapillary);
//			if (desc.grouping == 2 && (capNew.capStimulus != null && capNew.capStimulus.equals(".."))) 
//			{
//				if (capNew.getCapillarySide().equals("R")) 
//				{
//					capNew.capStimulus = desc.stimulusR;
//					capNew.capConcentration = desc.concentrationR;
//				} 
//				else 
//				{
//					capNew.capStimulus = desc.stimulusL;
//					capNew.capConcentration = desc.concentrationL;
//				}
//			}
//			if (!isPresent(capNew))
//				capillariesArrayList.add(capNew);
//		}
//		return true;
//	}
	
	private boolean xmlLoadCapillaries_Only_v1(Document doc) 
	{
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		Node nodecaps = XMLUtil.getElement(node, ID_LISTOFCAPILLARIES);
		int nitems = XMLUtil.getElementIntValue(nodecaps, ID_NCAPILLARIES, 0);
		capillariesArrayList = new ArrayList<Capillary> (nitems);
		for (int i= 0; i< nitems; i++) 
		{
			Node nodecapillary = XMLUtil.getElement(node, ID_CAPILLARY_+i);
			Capillary cap = new Capillary();
			cap.loadFromXML_CapillaryOnly(nodecapillary);
			if (desc.grouping == 2 && (cap.capStimulus != null && cap.capStimulus.equals(".."))) 
			{
				if (cap.getCapillarySide().equals("R")) 
				{
					cap.capStimulus = desc.stimulusR;
					cap.capConcentration = desc.concentrationR;
				} 
				else 
				{
					cap.capStimulus = desc.stimulusL;
					cap.capConcentration = desc.concentrationL;
				}
			}
			if (!isPresent(cap))
				capillariesArrayList.add(cap);
		}
		return true;
	}

	private void xmlLoadCapillaries_Only_v2(Document doc, String csFileName) 
	{
		xmlLoadCapillaries_Only_v1(doc);
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		for (Capillary cap: capillariesArrayList) 
		{
			String csFile = directory + cap.getCapillaryName() + ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			Node node = XMLUtil.getRootElement(capdoc, true);
			cap.loadFromXML_CapillaryOnly(node);
		}
	}	

	// -----------------------
	
	public void copy (Capillaries cap) 
	{
		desc.copy(cap.desc);
		capillariesArrayList.clear();
		for (Capillary ccap: cap.capillariesArrayList) 
		{
			Capillary capi = new Capillary();
			capi.copy(ccap);
			capillariesArrayList.add(capi);
		}
	}
	
	public boolean isPresent(Capillary capNew) 
	{
		boolean flag = false;
		for (Capillary cap: capillariesArrayList) 
		{
			if (cap.capillaryName.contentEquals(capNew.capillaryName)) 
			{
				flag = true;
				break;
			}
		}
		return flag;
	}

	public void mergeLists(Capillaries caplist) 
	{
		for (Capillary capm : caplist.capillariesArrayList ) 
		{
			if (!isPresent(capm))
				capillariesArrayList.add(capm);
		}
	}
	
	public void adjustToImageWidth (int imageWidth) 
	{
		for (Capillary cap: capillariesArrayList) 
		{
			cap.ptsTop.adjustToImageWidth(imageWidth);
			cap.ptsBottom.adjustToImageWidth(imageWidth);
			cap.ptsDerivative.adjustToImageWidth(imageWidth);
			cap.gulpsRois = null; // TODO: deal with gulps.. (simply remove?)
		}
	}

	public void transferDescriptionToCapillaries() 
	{
		for (Capillary cap: capillariesArrayList) 
		{
			transferCapGroupToCapillary(cap);
			transferDescriptionToCapillary (cap);
		}
	}
	
	private void transferDescriptionToCapillary (Capillary cap) 
	{
		cap.capVolume = desc.volume;
		cap.capPixels = desc.pixels;
		cap.descriptionOK = true;
	}
	
	private void transferCapGroupToCapillary (Capillary cap) 
	{
		if (desc.grouping != 2)
			return;
		String	name = cap.roi.getName();
		String letter = name.substring(name.length() - 1);
		cap.capSide = letter;
		if (letter .equals("R")) 
		{	
			String nameL = name.substring(0, name.length() - 1) + "L";
			Capillary cap0 = getCapillaryFromName(nameL);
			if (cap0 != null) 
			{
				cap.capNFlies = cap0.capNFlies;
				cap.capCageID = cap0.capCageID;
			}
		}
	}
	
	private Capillary getCapillaryFromName(String name) 
	{
		Capillary capFound = null;
		for (Capillary cap: capillariesArrayList) 
		{
			if (cap.roi.getName().equals(name)) 
			{
				capFound = cap;
				break;
			}
		}
		return capFound;
	}

	public void updateCapillariesFromSequence(Sequence seq) 
	{
		List<ROI2D> listROISCap = ROI2DUtilities.getROIs2DContainingString ("line", seq);
		Collections.sort(listROISCap, new Comparators.ROI2D_Name_Comparator());
		for (Capillary cap: capillariesArrayList) 
		{
			cap.valid = false;
			String capName = Capillary.replace_LR_with_12(cap.roi.getName());
			Iterator <ROI2D> iterator = listROISCap.iterator();
			while(iterator.hasNext()) 
			{ 
				ROI2D roi = iterator.next();
				String roiName = Capillary.replace_LR_with_12(roi.getName());
				if (roiName.equals (capName)) 
				{
					cap.roi = (ROI2DShape) roi;
					cap.valid = true;
				}
				if (cap.valid) 
				{
					iterator.remove();
					break;
				}
			}
		}
		Iterator <Capillary> iterator = capillariesArrayList.iterator();
		while (iterator.hasNext()) 
		{
			Capillary cap = iterator.next();
			if (!cap.valid )
				iterator.remove();
		}
		if (listROISCap.size() > 0) 
		{
			for (ROI2D roi: listROISCap) 
			{
				Capillary cap = new Capillary((ROI2DShape) roi);
				if (!isPresent(cap))
					capillariesArrayList.add(cap);
			}
		}
		Collections.sort(capillariesArrayList);
		return;
	}

	public void transferCapillaryRoiToSequence(Sequence seq) 
	{
		for (Capillary cap: capillariesArrayList) 
		{
			seq.addROI(cap.roi);
		}
	}
}
