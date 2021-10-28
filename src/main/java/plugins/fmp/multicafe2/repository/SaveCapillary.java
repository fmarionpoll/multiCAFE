package plugins.fmp.multicafe2.repository;

import java.nio.file.Paths;

import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.experiment.Capillary;


public class SaveCapillary 
{
	private final String 				ID_META 		= "metaMC";
	private final String 				ID_ROI 			= "roiMC";
	private final String				ID_NFLIES		= "nflies";
	private final String				ID_CAGENB		= "cage_number";
	private final String 				ID_CAPVOLUME 	= "capillaryVolume";
	private final String 				ID_CAPPIXELS 	= "capillaryPixels";
	private final String 				ID_STIML 		= "stimulus";
	private final String 				ID_CONCL 		= "concentration";
	private final String 				ID_SIDE 		= "side";
	private final String 				ID_DESCOK 		= "descriptionOK";
	private final String				ID_VERSIONINFOS	= "versionInfos";
	
	private final String 				ID_INDEXIMAGE 	= "indexImageMC";
	private final String 				ID_NAME 		= "nameMC";
	private final String 				ID_NAMETIFF 	= "filenameTIFF";
	private final String 				ID_VERSION		= "version"; 
	private final String 				ID_VERSIONNUM	= "1.0.0"; 

	
	public boolean saveToXML(Node node, Capillary cap) 
	{
		saveToXML_CapillaryOnly(node, cap);
		saveToXML_MeasuresOnly(node, cap); 
        return true;
	}
	
	public void saveToXML_CapillaryOnly(Node node, Capillary cap) 
	{
	    final Node nodeMeta = XMLUtil.setElement(node, ID_META);
	    if (nodeMeta != null) 
	    {
	    	if (cap.version == null)
	    		cap.version = ID_VERSIONNUM;
	    	XMLUtil.setElementValue(nodeMeta, ID_VERSION, cap.version);
	        XMLUtil.setElementIntValue(nodeMeta, ID_INDEXIMAGE, cap.indexKymograph);
	        XMLUtil.setElementValue(nodeMeta, ID_NAME, cap.getKymographName());
	        if (cap.filenameTIFF != null ) {
	        	String filename = Paths.get(cap.filenameTIFF).getFileName().toString();
	        	XMLUtil.setElementValue(nodeMeta, ID_NAMETIFF, filename);
	        }
	        XMLUtil.setElementBooleanValue(nodeMeta, ID_DESCOK, cap.descriptionOK);
	        XMLUtil.setElementIntValue(nodeMeta, ID_VERSIONINFOS, cap.versionInfos);
	        XMLUtil.setElementIntValue(nodeMeta, ID_NFLIES, cap.capNFlies);
	        XMLUtil.setElementIntValue(nodeMeta, ID_CAGENB, cap.capCageID);
			XMLUtil.setElementDoubleValue(nodeMeta, ID_CAPVOLUME, cap.capVolume);
			XMLUtil.setElementIntValue(nodeMeta, ID_CAPPIXELS, cap.capPixels);
			XMLUtil.setElementValue(nodeMeta, ID_STIML, cap.capStimulus);
			XMLUtil.setElementValue(nodeMeta, ID_SIDE, cap.capSide);
			XMLUtil.setElementValue(nodeMeta, ID_CONCL, cap.capConcentration);

	        saveToXML_ROI(nodeMeta, cap.roi); 
	    }
	}

	public void saveToXML_MeasuresOnly(Node node, Capillary cap) 
	{
		if (cap.ptsTop != null)
		cap.ptsTop.saveCapillaryLimit2XML(node, cap.ID_TOPLEVEL);
		if (cap.ptsBottom != null)
			cap.ptsBottom.saveCapillaryLimit2XML(node, cap.ID_BOTTOMLEVEL);
		if (cap.ptsDerivative != null)
			cap.ptsDerivative.saveCapillaryLimit2XML(node, cap.ID_DERIVATIVE);
		if (cap.gulpsRois != null)
			cap.gulpsRois.saveToXML(node);
	}
	
	private void saveToXML_ROI(Node node, ROI roi) 
	{
		final Node nodeROI = XMLUtil.setElement(node, ID_ROI);
        if (!roi.saveToXML(nodeROI)) 
        {
            XMLUtil.removeNode(node, nodeROI);
            System.err.println("Error: the roi " + roi.getName() + " was not correctly saved to XML !");
        }
	}
 
}
