package plugins.fmp.multicafeTools;


import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class DetectGulps_Options implements XMLPersistent {
	
	public int 			detectGulpsThreshold	= 90;
	public TransformOp 	transformForGulps 		= TransformOp.XDIFFN;
	public boolean 		detectAllGulps 			= true;
	public boolean		buildGulps				= true;
	public boolean		buildDerivative			= true;
	public int			firstkymo 				= 0;
	public 	boolean 	analyzePartOnly			= false;
	public 	int 		startPixel 				= -1;
	public 	int 		endPixel 				= -1;
	
	
	public void copy(DetectGulps_Options destination) {
		destination.detectGulpsThreshold 	= detectGulpsThreshold;
		destination.transformForGulps 		= transformForGulps;
		destination.detectAllGulps 			= detectAllGulps;
	}

	@Override
	public boolean loadFromXML(Node node) {
		final Node nodeMeta = XMLUtil.setElement(node, "LimitsOptions");
	    if (nodeMeta != null)
	    {
	    	detectAllGulps = XMLUtil.getElementBooleanValue(nodeMeta, "detectAllGulps", detectAllGulps);
	    	buildGulps = XMLUtil.getElementBooleanValue(nodeMeta, "buildGulps", buildGulps);
	    	buildDerivative = XMLUtil.getElementBooleanValue(nodeMeta, "buildDerivative", buildDerivative);
	    	
	    	transformForGulps = TransformOp.findByText(XMLUtil.getElementValue(nodeMeta, "Transform", transformForGulps.toString()));       
	    }
        return true;
	}


	@Override
	public boolean saveToXML(Node node) {
		final Node nodeMeta = XMLUtil.setElement(node, "LimitsOptions");
	    if (nodeMeta != null)
	    {
	    	XMLUtil.setElementBooleanValue(nodeMeta, "detectAllGulps", detectAllGulps);
	    	XMLUtil.setElementBooleanValue(nodeMeta, "buildGulps", buildGulps);
	    	XMLUtil.setElementBooleanValue(nodeMeta, "buildDerivative", buildDerivative);
	    	
	    	XMLUtil.setElementValue(nodeMeta, "Transform", transformForGulps.toString());       
	    }
        return true;
	}

}
