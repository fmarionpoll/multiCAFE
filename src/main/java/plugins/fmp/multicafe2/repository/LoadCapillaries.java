package plugins.fmp.multicafe2.repository;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import icy.roi.ROI;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.experiment.CapillariesDescription;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class LoadCapillaries {
	
	private final static String ID_CAPILLARYTRACK 		= "capillaryTrack";
	private final static String ID_NCAPILLARIES 		= "N_capillaries";
	private final static String ID_LISTOFCAPILLARIES 	= "List_of_capillaries";
	private final static String ID_CAPILLARY_ 			= "capillary_";

	
	
	public boolean xmlLoadCapillaries_Descriptors(String csFileName, CapillariesDescription desc, List <Capillary> capillariesArrayList) 
	{	
		if (csFileName == null)
			return false;
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) 
		{
			desc.xmlLoadCapillaryDescription(doc);
			return xmlLoadCapillaries_Only_v1(doc, desc, capillariesArrayList);		
		}
		return false;
	}
	
	public boolean xmlLoadOldCapillaries_Only(String csFileName, CapillariesDescription desc, List <Capillary> capillariesArrayList) 
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
				xmlLoadCapillaries_Only_v1(doc, desc, capillariesArrayList);
				break;
			case 0: // old-old xml storage structure
				xmlLoadCapillaries_v0(doc, csFileName, capillariesArrayList);
				break;
			default:
				xmlLoadCapillaries_Only_v2(doc, csFileName, desc, capillariesArrayList);
				return false;
			}		
			return true;
		}
		return false;
	}
	
	public boolean xmlLoadCapillaries_Measures(String directory, List <Capillary> capillariesArrayList) 
	{
		boolean flag = true;
		int ncapillaries = capillariesArrayList.size();

		for (int i=0; i< ncapillaries; i++) 
		{
			String csFile = directory + File.separator + capillariesArrayList.get(i).getKymographName() + ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			Node node = XMLUtil.getRootElement(capdoc, true);
			Capillary cap = capillariesArrayList.get(i);
			cap.indexImage = i;
			flag |= cap.loadFromXML_MeasuresOnly(node);
		}
		return flag;
	}
	
	private void xmlLoadCapillaries_v0(Document doc, String csFileName, List <Capillary> capillariesArrayList) 
	{
		List<ROI> listOfCapillaryROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
		capillariesArrayList.clear();
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		int t = 0;
		for (ROI roiCapillary: listOfCapillaryROIs) 
		{
			xmlLoadIndividualCapillary_v0((ROI2DShape) roiCapillary, directory, t, capillariesArrayList);
			t++;
		}
	}
	
	private void xmlLoadIndividualCapillary_v0(ROI2DShape roiCapillary, String directory, int t, List <Capillary> capillariesArrayList) 
	{
		Capillary cap = new Capillary((ROI2DShape) roiCapillary);
		if (!isCapillaryPresent(cap, capillariesArrayList))
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
	
	private boolean xmlLoadCapillaries_Only_v1(Document doc, CapillariesDescription desc, List <Capillary> capillariesArrayList) 
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
			if (!isCapillaryPresent(cap, capillariesArrayList))
				capillariesArrayList.add(cap);
		}
		return true;
	}

	private void xmlLoadCapillaries_Only_v2(Document doc, String csFileName, CapillariesDescription desc, List <Capillary> capillariesArrayList) 
	{
		xmlLoadCapillaries_Only_v1(doc, desc, capillariesArrayList);
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		for (Capillary cap: capillariesArrayList) 
		{
			String csFile = directory + cap.getKymographName() + ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			Node node = XMLUtil.getRootElement(capdoc, true);
			cap.loadFromXML_CapillaryOnly(node);
		}
	}
	
	private boolean isCapillaryPresent(Capillary capNew, List <Capillary> capillariesArrayList) 
	{
		boolean flag = false;
		for (Capillary cap: capillariesArrayList) 
		{
			if (cap.getKymographName().contentEquals(capNew.getKymographName())) 
			{
				flag = true;
				break;
			}
		}
		return flag;
	}


}
