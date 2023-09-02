package plugins.fmp.multicafe2.dlg.cages;

import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import icy.util.XMLUtil;
import plugins.fmp.multicafe2.experiment.CapillariesDescription;
import plugins.fmp.multicafe2.experiment.Capillary;

public class SaveCapillaries {

	private final static String ID_CAPILLARYTRACK 		= "capillaryTrack";
	private final static String ID_NCAPILLARIES 		= "N_capillaries";
	private final static String ID_LISTOFCAPILLARIES 	= "List_of_capillaries";
	private final static String ID_CAPILLARY_ 			= "capillary_";
	
	public boolean xmlSaveCapillaries_Descriptors(String csFileName, CapillariesDescription desc, List <Capillary> capillariesArrayList) 
	{
		if (csFileName != null) 
		{
			final Document doc = XMLUtil.createDocument(true);
			if (doc != null) 
			{
				desc.xmlSaveCapillaryDescription (doc);
				xmlSaveListOfCapillaries(doc, capillariesArrayList);
				return XMLUtil.saveDocument(doc, csFileName);
			}
		}
		return false;
	}
	
	private boolean xmlSaveListOfCapillaries(Document doc, List <Capillary> capillariesArrayList) 
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
	

}
