package plugins.fmp.multicafe.series;


import java.awt.Rectangle;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.tools.ImageTransformTools.TransformOp;



public class DetectFlies_Options implements XMLPersistent {
	
	public int 		threshold 				= -1;
	public int		thresholdBckgnd			= 40;
	public int		thresholdDiff			= 100;
	public boolean 	btrackWhite 			= false;
	public boolean  blimitLow 				= false;
	public boolean  blimitUp 				= false;
	public int  	limitLow				= 0;
	public int  	limitUp					= 1;
	public int		limitRatio				= 4;
	public int 		jitter 					= 10;
	public boolean	forceBuildBackground	= false;
	public boolean	detectFlies				= true;
	
	public TransformOp transformop; 
	public int		videoChannel 			= 0;
	public boolean 	backgroundSubstraction 	= false;

	public int		df_stepFrame 			= 1;
	public int 		df_startFrame			= 0;
	public int 		df_endFrame				= 1;
	public boolean	isFrameFixed			= false;
	public int 		nbframes				= 1;
	
	public Rectangle 	parent0Rect 		= null;
	public String		resultsSubPath		= null;
	public ExperimentList expList 			= null;
	
	
	// -----------------------------------------------------
	
	public void copyParameters (DetectFlies_Options det) {
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
		df_stepFrame 			= det.df_stepFrame;
		df_startFrame			= det.df_startFrame;
		df_endFrame				= det.df_endFrame;
		isFrameFixed			= det.isFrameFixed;
		nbframes				= det.nbframes;
	}
	
	@Override
	public boolean loadFromXML(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "DetectFliesParameters");
		if (xmlVal == null) 
			return false;
		threshold =  XMLUtil.getElementIntValue(xmlVal, "threshold", -1);
		btrackWhite = XMLUtil.getElementBooleanValue(xmlVal, "btrackWhite", false);
		blimitLow = XMLUtil.getElementBooleanValue(xmlVal, "blimitLow",false);
		blimitUp = XMLUtil.getElementBooleanValue(xmlVal, "blimitUp", false);
		limitLow =  XMLUtil.getElementIntValue(xmlVal, "limitLow", -1);
		limitUp =  XMLUtil.getElementIntValue(xmlVal, "limitUp", -1);
		jitter =  XMLUtil.getElementIntValue(xmlVal, "jitter", 10); 
		String op1 = XMLUtil.getElementValue(xmlVal, "transformOp", null);
		transformop = TransformOp.findByText(op1);
		df_startFrame =  XMLUtil.getAttributeIntValue(xmlVal, "start", 0);
		df_endFrame = XMLUtil.getAttributeIntValue(xmlVal, "end", 0);
		df_stepFrame = XMLUtil.getAttributeIntValue(xmlVal, "step", 1);
		videoChannel = XMLUtil.getAttributeIntValue(xmlVal, "videoChannel", 0);
		return true;
	}
	
	@Override
	public boolean saveToXML(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "DetectFliesParameters");
		XMLUtil.setElementIntValue(xmlVal, "threshold", threshold);
		XMLUtil.setElementBooleanValue(xmlVal, "btrackWhite", btrackWhite);
		XMLUtil.setElementBooleanValue(xmlVal, "blimitLow", blimitLow);
		XMLUtil.setElementBooleanValue(xmlVal, "blimitUp", blimitUp);
		XMLUtil.setElementIntValue(xmlVal, "limitLow", limitLow);
		XMLUtil.setElementIntValue(xmlVal, "limitUp", limitUp);
		XMLUtil.setElementIntValue(xmlVal, "jitter", jitter); 
		if (transformop != null) {
			String transform1 = transformop.toString();
			XMLUtil.setElementValue(xmlVal, "transformOp", transform1);
		}
		XMLUtil.setAttributeIntValue(xmlVal, "start", df_startFrame);
		XMLUtil.setAttributeIntValue(xmlVal, "end", df_endFrame); 
		XMLUtil.setAttributeIntValue(xmlVal, "step", df_stepFrame); 
		XMLUtil.setAttributeIntValue(xmlVal, "videoChannel", videoChannel);
		return true;
	}
	
	
}
