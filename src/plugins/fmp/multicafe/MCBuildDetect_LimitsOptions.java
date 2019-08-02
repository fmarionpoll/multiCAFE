package plugins.fmp.multicafe;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class MCBuildDetect_LimitsOptions implements XMLPersistent {
	
	boolean 		detectTop 				= true;
	boolean 		detectBottom 			= true;
	boolean 		detectAllImages 		= true;
	int				firstImage				= 0;
	boolean			directionUp				= true;
	int				detectLevelThreshold 	= 35;
	TransformOp		transformForLevels 		= TransformOp.R2MINUS_GB;

	
	void copy(MCBuildDetect_LimitsOptions destination) {

		destination.detectTop 				= detectTop; 
		destination.detectBottom 			= detectBottom; 
		destination.transformForLevels 		= transformForLevels;
		destination.directionUp 			= directionUp;
		destination.detectLevelThreshold 	= detectLevelThreshold;
		destination.detectAllImages 		= detectAllImages;
	}


	@Override
	public boolean loadFromXML(Node node) {
		final Node nodeMeta = XMLUtil.setElement(node, "LimitsOptions");
	    if (nodeMeta != null)
	    {
	    	detectTop = XMLUtil.getElementBooleanValue(nodeMeta, "detectTop", detectTop);
	    	detectBottom = XMLUtil.getElementBooleanValue(nodeMeta, "detectBottom", detectBottom);
	    	detectAllImages = XMLUtil.getElementBooleanValue(nodeMeta, "detectAllImages", detectAllImages);
	    	directionUp = XMLUtil.getElementBooleanValue(nodeMeta, "directionUp", directionUp);
	    	firstImage = XMLUtil.getElementIntValue(nodeMeta, "firstImage", firstImage);
	    	detectLevelThreshold = XMLUtil.getElementIntValue(nodeMeta, "detectLevelThreshold", detectLevelThreshold);
	    	transformForLevels = TransformOp.findByText(XMLUtil.getElementValue(nodeMeta, "Transform", transformForLevels.toString()));       
	    }
        return true;
	}


	@Override
	public boolean saveToXML(Node node) {
		final Node nodeMeta = XMLUtil.setElement(node, "LimitsOptions");
	    if (nodeMeta != null)
	    {
	    	XMLUtil.setElementBooleanValue(nodeMeta, "detectTop", detectTop);
	    	XMLUtil.setElementBooleanValue(nodeMeta, "detectBottom", detectBottom);
	    	XMLUtil.setElementBooleanValue(nodeMeta, "detectAllImages", detectAllImages);
	    	XMLUtil.setElementBooleanValue(nodeMeta, "directionUp", directionUp);
	    	XMLUtil.setElementIntValue(nodeMeta, "firstImage", firstImage);
	    	XMLUtil.setElementIntValue(nodeMeta, "detectLevelThreshold", detectLevelThreshold);
	        XMLUtil.setElementValue(nodeMeta, "Transform", transformForLevels.toString());       
	    }
        return true;
	}

}
