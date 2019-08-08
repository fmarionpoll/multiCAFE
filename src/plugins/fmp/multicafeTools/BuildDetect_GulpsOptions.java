package plugins.fmp.multicafeTools;


import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class BuildDetect_GulpsOptions implements XMLPersistent {
	
	public int 			detectGulpsThreshold	= 90;
	public TransformOp 	transformForGulps 		= TransformOp.XDIFFN;
	public boolean 		detectAllGulps 			= true;
	public boolean		computeDiffnAndDetect	= true;
	public int			firstkymo 				= 0;
	
	public void copy(BuildDetect_GulpsOptions destination) {
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
	    	computeDiffnAndDetect = XMLUtil.getElementBooleanValue(nodeMeta, "computeDiffnAndDetect", computeDiffnAndDetect);
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
	    	XMLUtil.setElementBooleanValue(nodeMeta, "computeDiffnAndDetect", computeDiffnAndDetect);
	    	XMLUtil.setElementValue(nodeMeta, "Transform", transformForGulps.toString());       
	    }
        return true;
	}

}
