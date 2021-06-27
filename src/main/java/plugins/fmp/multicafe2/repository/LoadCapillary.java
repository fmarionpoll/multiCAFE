package plugins.fmp.multicafe2.repository;


import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class LoadCapillary {
	
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

	
	public boolean loadFromXML(Node node, Capillary cap) 
	{
		boolean result = loadFromXML_CapillaryOnly(node, cap);	
		result |= loadFromXML_MeasuresOnly( node, cap);
		return result;
	}
		
	public boolean loadFromXML_CapillaryOnly(Node node, Capillary cap) 
	{
	    final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    boolean flag = (nodeMeta != null); 
	    if (flag) 
	    {
	    	cap.version 		= XMLUtil.getElementValue(nodeMeta, ID_VERSION, "0.0.0");
	    	cap.indexImage 		= XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, cap.indexImage);
	    	cap.capillaryName 	= XMLUtil.getElementValue(nodeMeta, ID_NAME, cap.capillaryName);
	    	cap.filenameTIFF 	= XMLUtil.getElementValue(nodeMeta, ID_NAMETIFF, cap.filenameTIFF);	        
	    	cap. descriptionOK 	= XMLUtil.getElementBooleanValue(nodeMeta, ID_DESCOK, false);
	    	cap.versionInfos 	= XMLUtil.getElementIntValue(nodeMeta, ID_VERSIONINFOS, 0);
	    	cap.capNFlies 		= XMLUtil.getElementIntValue(nodeMeta, ID_NFLIES, cap.capNFlies);
	    	cap.capCageID 		= XMLUtil.getElementIntValue(nodeMeta, ID_CAGENB, cap.capCageID);
	    	cap.capVolume 		= XMLUtil.getElementDoubleValue(nodeMeta, ID_CAPVOLUME, Double.NaN);
	    	cap.capPixels 		= XMLUtil.getElementIntValue(nodeMeta, ID_CAPPIXELS, 5);
	    	cap.capStimulus 	= XMLUtil.getElementValue(nodeMeta, ID_STIML, ID_STIML);
	    	cap.capConcentration= XMLUtil.getElementValue(nodeMeta, ID_CONCL, ID_CONCL);
	    	cap.capSide 		= XMLUtil.getElementValue(nodeMeta, ID_SIDE, ".");
			
	    	cap.roi = (ROI2DShape) loadFromXML_ROI(nodeMeta);
	    	cap.limitsOptions.loadFromXML(nodeMeta);
	    }
	    return flag;
	}
	
	public boolean loadFromXML_MeasuresOnly(Node node, Capillary cap) 
	{
		String header = cap.getLast2ofCapillaryName()+"_";
		boolean result = cap.ptsDerivative.loadCapillaryLimitFromXML(node, cap.ID_DERIVATIVE, header) > 0;
		result |= cap.ptsTop.loadCapillaryLimitFromXML(node, cap.ID_TOPLEVEL, header) > 0;
		result |= cap.ptsBottom.loadCapillaryLimitFromXML(node, cap.ID_BOTTOMLEVEL, header) > 0;
		result |= cap.gulpsRois.loadFromXML(node);
		return result;
	}
 
	private ROI loadFromXML_ROI(Node node) 
	{
		final Node nodeROI = XMLUtil.getElement(node, ID_ROI);
        if (nodeROI != null) 
        {
			ROI roi = ROI.createFromXML(nodeROI);
	        return roi;
        }
        return null;
	}

}
