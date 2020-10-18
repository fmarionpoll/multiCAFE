package plugins.fmp.multicafe.series;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.tools.ImageTransformTools.TransformOp;

public class BuildSeries_Options implements XMLPersistent {
	public int 				stepFrame 			= 1;
	public int 				startFrame 			= 0;
	public int				endFrame 			= 99999999;
	public boolean			isFrameFixed		= false;
	
	public int 				diskRadius 			= 5;
	public boolean 			doRegistration 		= false;
	public boolean			doCreateResults_bin	= false;
	public ArrayList<ROI2D> listROIStoBuildKymos= new ArrayList<ROI2D> ();
	public ExperimentList	expList;
	public Rectangle 		parent0Rect 		= null;
	public String 			resultsSubPath 		= null;
	
	public boolean 			loopRunning 		= false;	
	
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
	
	public int 			detectGulpsThreshold	= 90;
	public TransformOp 	transformForGulps 		= TransformOp.XDIFFN;
	public int			spanDiff				= 3;
	public boolean 		detectAllGulps 			= true;
	public boolean		buildGulps				= true;
	public boolean		buildDerivative			= true;
	public int			firstkymo 				= 0;



// -----------------------

void copy(BuildSeries_Options destination) {
	destination.detectTop 				= detectTop; 
	destination.detectBottom 			= detectBottom; 
	destination.transformForLevels 		= transformForLevels;
	destination.directionUp 			= directionUp;
	destination.detectLevelThreshold 	= detectLevelThreshold;
	destination.detectAllKymos 			= detectAllKymos;
	
	destination.detectGulpsThreshold 	= detectGulpsThreshold;
	destination.transformForGulps 		= transformForGulps;
	destination.detectAllGulps 			= detectAllGulps;
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
	if (nodeMeta != null) {
		XMLUtil.setElementBooleanValue(nodeMeta, "detectTop", detectTop);
		XMLUtil.setElementBooleanValue(nodeMeta, "detectBottom", detectBottom);
		XMLUtil.setElementBooleanValue(nodeMeta, "detectAllImages", detectAllKymos);
		XMLUtil.setElementBooleanValue(nodeMeta, "directionUp", directionUp);
		XMLUtil.setElementIntValue(nodeMeta, "firstImage", firstKymo);
		XMLUtil.setElementIntValue(nodeMeta, "detectLevelThreshold", detectLevelThreshold);
	    XMLUtil.setElementValue(nodeMeta, "Transform", transformForLevels.toString()); 
	    
	    XMLUtil.setElementBooleanValue(nodeMeta, "detectAllGulps", detectAllGulps);
    	XMLUtil.setElementBooleanValue(nodeMeta, "buildGulps", buildGulps);
    	XMLUtil.setElementBooleanValue(nodeMeta, "buildDerivative", buildDerivative);
    	XMLUtil.setElementValue(nodeMeta, "Transform", transformForGulps.toString());       
    }
	return true;
}

}
