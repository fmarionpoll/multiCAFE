package plugins.fmp.multicafe.series;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.dlg.JComponents.ExperimentCombo;
import plugins.fmp.multicafe.tools.ImageTransformTools.TransformOp;

public class Options_BuildSeries implements XMLPersistent 
{
	public boolean			isFrameFixed		= false;
	public long				t_firstMs			= 0;
	public long				t_lastMs			= 0;
	public long				t_binMs				= 1;
	
	public int 				diskRadius 			= 5;
	public boolean 			doRegistration 		= false;
	public boolean			doCreateBinDir		= false;
	public ArrayList<ROI2D> listROIStoBuildKymos= new ArrayList<ROI2D> ();
	public ExperimentCombo	expList;
	public Rectangle 		parent0Rect 		= null;
	public String 			binSubDirectory 	= null;
	
	public boolean 		loopRunning 			= false;	
	
	boolean 			detectTop 				= true;
	boolean 			detectBottom 			= true;
	
	public int			detectCage				= -1;
	public	boolean		detectL					= true;
	public	boolean		detectR					= true;
	public 	boolean 	detectAllKymos 			= true;
	public 	int			firstKymo				= 0;
	public  int			lastKymo				= 0;
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

	public int 			threshold 				= -1;
	public int			thresholdBckgnd			= 40;
	public int			thresholdDiff			= 100;
	public boolean 		btrackWhite 			= false;
	public boolean  	blimitLow 				= false;
	public boolean  	blimitUp 				= false;
	public int  		limitLow				= 0;
	public int  		limitUp					= 1;
	public int			limitRatio				= 4;
	public int 			jitter 					= 10;
	public boolean		forceBuildBackground	= false;
	public boolean		detectFlies				= true;
	
	public TransformOp 	transformop; 
	public int			videoChannel 			= 0;
	public boolean 		backgroundSubstraction 	= false;

	// -----------------------
	
	void copyTo(Options_BuildSeries destination) 
	{
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
	
	void copyFrom(Options_BuildSeries destination) 
	{
		detectTop 				= destination.detectTop; 
		detectBottom 			= destination.detectBottom; 
		transformForLevels 		= destination.transformForLevels;
		directionUp 			= destination.directionUp;
		detectLevelThreshold 	= destination.detectLevelThreshold;
		detectAllKymos 			= destination.detectAllKymos;
		
		detectGulpsThreshold 	= destination.detectGulpsThreshold;
		transformForGulps 		= destination.transformForGulps;
		detectAllGulps 			= destination.detectAllGulps;
	}
	
	public void copyParameters (Options_BuildSeries det) 
	{
		threshold = det.threshold;
		thresholdBckgnd			= det.thresholdBckgnd;
		thresholdDiff			= det.thresholdDiff;
		btrackWhite 			= det.btrackWhite;
		blimitLow 				= det.blimitLow;
		blimitUp 				= det.blimitUp;
		limitLow				= det.limitLow;
		limitUp					= det.limitUp;
		limitRatio				= det.limitRatio;
		jitter 					= det.jitter;
		forceBuildBackground	= det.forceBuildBackground;
		detectFlies				= det.detectFlies;
		transformop				= det.transformop; 
		videoChannel 			= det.videoChannel;
		backgroundSubstraction 	= det.backgroundSubstraction;
		isFrameFixed			= det.isFrameFixed;
	}
	
	@Override
	public boolean loadFromXML(Node node) 
	{
		final Node nodeMeta = XMLUtil.getElement(node, "LimitsOptions");
		if (nodeMeta != null) 
		{
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
		
		Element xmlVal = XMLUtil.getElement(node, "DetectFliesParameters");
		if (xmlVal != null) 
		{
			threshold =  XMLUtil.getElementIntValue(xmlVal, "threshold", -1);
			btrackWhite = XMLUtil.getElementBooleanValue(xmlVal, "btrackWhite", false);
			blimitLow = XMLUtil.getElementBooleanValue(xmlVal, "blimitLow",false);
			blimitUp = XMLUtil.getElementBooleanValue(xmlVal, "blimitUp", false);
			limitLow =  XMLUtil.getElementIntValue(xmlVal, "limitLow", -1);
			limitUp =  XMLUtil.getElementIntValue(xmlVal, "limitUp", -1);
			jitter =  XMLUtil.getElementIntValue(xmlVal, "jitter", 10); 
			String op1 = XMLUtil.getElementValue(xmlVal, "transformOp", null);
			transformop = TransformOp.findByText(op1);
			videoChannel = XMLUtil.getAttributeIntValue(xmlVal, "videoChannel", 0);
		}
		return true;
	}
	
	@Override
	public boolean saveToXML(Node node) 
	{
		final Node nodeMeta = XMLUtil.setElement(node, "LimitsOptions");
		if (nodeMeta != null) 
		{
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
		
		Element xmlVal = XMLUtil.addElement(node, "DetectFliesParameters");
		if (xmlVal != null) 
		{
			XMLUtil.setElementIntValue(xmlVal, "threshold", threshold);
			XMLUtil.setElementBooleanValue(xmlVal, "btrackWhite", btrackWhite);
			XMLUtil.setElementBooleanValue(xmlVal, "blimitLow", blimitLow);
			XMLUtil.setElementBooleanValue(xmlVal, "blimitUp", blimitUp);
			XMLUtil.setElementIntValue(xmlVal, "limitLow", limitLow);
			XMLUtil.setElementIntValue(xmlVal, "limitUp", limitUp);
			XMLUtil.setElementIntValue(xmlVal, "jitter", jitter); 
			if (transformop != null) 
			{
				String transform1 = transformop.toString();
				XMLUtil.setElementValue(xmlVal, "transformOp", transform1);
			}
			XMLUtil.setAttributeIntValue(xmlVal, "videoChannel", videoChannel);
		}
		return true;
	}

}
