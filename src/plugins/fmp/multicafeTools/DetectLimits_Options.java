package plugins.fmp.multicafeTools;

import java.awt.Rectangle;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class DetectLimits_Options implements XMLPersistent {
	
			boolean 	detectTop 				= true;
			boolean 	detectBottom 			= true;
			
	public	boolean		detectL					= true;
	public	boolean		detectR					= true;
	public 	boolean 	detectAllKymos 			= true;
	public 	int			firstKymo				= 0;
	public 	boolean		directionUp				= true;
	public 	int			detectLevelThreshold 	= 35;
	public 	TransformOp	transformForLevels 		= TransformOp.R2MINUS_GB;
	public 	boolean 	analyzePartOnly			= false;
	public 	int 		startPixel 				= -1;
	public 	int 		endPixel 				= -1;
	public  int			spanDiffTop				= 3;
	public Rectangle 	parent0Rect 			= null;
	public ExperimentList expList				= null;
	
	// -----------------------
	
	void copy(DetectLimits_Options destination) {
		destination.detectTop 				= detectTop; 
		destination.detectBottom 			= detectBottom; 
		destination.transformForLevels 		= transformForLevels;
		destination.directionUp 			= directionUp;
		destination.detectLevelThreshold 	= detectLevelThreshold;
		destination.detectAllKymos 			= detectAllKymos;
	}

	@Override
	public boolean loadFromXML(Node node) {
		final Node nodeMeta = XMLUtil.setElement(node, "LimitsOptions");
	    if (nodeMeta != null) {
	    	detectTop = XMLUtil.getElementBooleanValue(nodeMeta, "detectTop", detectTop);
	    	detectBottom = XMLUtil.getElementBooleanValue(nodeMeta, "detectBottom", detectBottom);
	    	detectAllKymos = XMLUtil.getElementBooleanValue(nodeMeta, "detectAllImages", detectAllKymos);
	    	directionUp = XMLUtil.getElementBooleanValue(nodeMeta, "directionUp", directionUp);
	    	firstKymo = XMLUtil.getElementIntValue(nodeMeta, "firstImage", firstKymo);
	    	detectLevelThreshold = XMLUtil.getElementIntValue(nodeMeta, "detectLevelThreshold", detectLevelThreshold);
	    	transformForLevels = TransformOp.findByText(XMLUtil.getElementValue(nodeMeta, "Transform", transformForLevels.toString()));       
	    }
        return true;
	}

	@Override
	public boolean saveToXML(Node node) {
		final Node nodeMeta = XMLUtil.setElement(node, "LimitsOptions");
	    if (nodeMeta != null) {
	    	XMLUtil.setElementBooleanValue(nodeMeta, "detectTop", detectTop);
	    	XMLUtil.setElementBooleanValue(nodeMeta, "detectBottom", detectBottom);
	    	XMLUtil.setElementBooleanValue(nodeMeta, "detectAllImages", detectAllKymos);
	    	XMLUtil.setElementBooleanValue(nodeMeta, "directionUp", directionUp);
	    	XMLUtil.setElementIntValue(nodeMeta, "firstImage", firstKymo);
	    	XMLUtil.setElementIntValue(nodeMeta, "detectLevelThreshold", detectLevelThreshold);
	        XMLUtil.setElementValue(nodeMeta, "Transform", transformForLevels.toString());       
	    }
        return true;
	}

}
