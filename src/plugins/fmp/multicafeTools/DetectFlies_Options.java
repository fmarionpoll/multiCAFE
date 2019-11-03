package plugins.fmp.multicafeTools;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class DetectFlies_Options implements XMLPersistent {
	
	public int 		threshold = -1;
	public boolean 	btrackWhite = false;
	public int		ichanselected = 0;
	public boolean  blimitLow = false;
	public boolean  blimitUp = false;
	public int  	limitLow;
	public int  	limitUp;
	public int 		jitter = 10;
	public TransformOp transformop2; 
	
	public long 	analysisStart = 0;
	public long 	analysisEnd = 0;
	public int 		analysisStep = 1;
	
	public TransformOp transformop1; 
	public int		videoChannel = 0;
	public boolean 	backgroundSubstraction = false;

	
	@Override
	public boolean loadFromXML(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "DetectFliesParameters");
		if (xmlVal == null) 
			return false;
		threshold =  XMLUtil.getElementIntValue(xmlVal, "threshold", -1);
		btrackWhite = XMLUtil.getElementBooleanValue(xmlVal, "btrackWhite", false);
		ichanselected = XMLUtil.getElementIntValue(xmlVal, "ichanselected", 0);
		blimitLow = XMLUtil.getElementBooleanValue(xmlVal, "blimitLow",false);
		blimitUp = XMLUtil.getElementBooleanValue(xmlVal, "blimitUp", false);
		limitLow =  XMLUtil.getElementIntValue(xmlVal, "limitLow", -1);
		limitUp =  XMLUtil.getElementIntValue(xmlVal, "limitUp", -1);
		jitter =  XMLUtil.getElementIntValue(xmlVal, "jitter", 10); 
		String op2 = XMLUtil.getElementValue(xmlVal, "transformOp", null);
		transformop2 = TransformOp.findByText(op2);
		String op1 = XMLUtil.getElementValue(xmlVal, "transformOp1", null);
		transformop1 = TransformOp.findByText(op1);
		analysisStart =  XMLUtil.getAttributeLongValue(xmlVal, "start", 0);
		analysisEnd = XMLUtil.getAttributeLongValue(xmlVal, "end", 0);
		analysisStep = XMLUtil.getAttributeIntValue(xmlVal, "step", 1);
		
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
		XMLUtil.setElementIntValue(xmlVal, "ichanselected", ichanselected);
		XMLUtil.setElementBooleanValue(xmlVal, "blimitLow", blimitLow);
		XMLUtil.setElementBooleanValue(xmlVal, "blimitUp", blimitUp);
		XMLUtil.setElementIntValue(xmlVal, "limitLow", limitLow);
		XMLUtil.setElementIntValue(xmlVal, "limitUp", limitUp);
		XMLUtil.setElementIntValue(xmlVal, "jitter", jitter); 
		if (transformop2 != null) {
			String transform2 = transformop2.toString();
			XMLUtil.setElementValue(xmlVal, "transformOp", transform2);
		}
		if (transformop1 != null) {
			String transform1 = transformop1.toString();
			XMLUtil.setElementValue(xmlVal, "transformOp1", transform1);
		}
		XMLUtil.setAttributeLongValue(xmlVal, "start", analysisStart);
		XMLUtil.setAttributeLongValue(xmlVal, "end", analysisEnd); 
		XMLUtil.setAttributeIntValue(xmlVal, "step", analysisStep); 
		XMLUtil.setAttributeIntValue(xmlVal, "videoChannel", videoChannel);

		return true;
	}
	
}
