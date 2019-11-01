package plugins.fmp.multicafeSequence;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class Cage {
	public ROI2D 			cageLimitROI		= null;
	public XYTaSeries 		flyPositions 		= new XYTaSeries();
	public List<ROI2D> 		detectedFliesList	= new ArrayList<ROI2D>();
	
	public boolean xmlSaveCage (Node node, int index) {
		if (node == null)
			return false;
		
		Element xmlVal = XMLUtil.addElement(node, "Cage"+index);
		
		Element xmlVal2 = XMLUtil.addElement(xmlVal, "CageLimits");
		if (cageLimitROI != null)
			cageLimitROI.saveToXML(xmlVal2);
		
		xmlVal2 = XMLUtil.addElement(xmlVal, "FlyPositions");
		flyPositions.saveToXML(xmlVal2);
		
		xmlVal2 = XMLUtil.addElement(xmlVal, "RoisDetected");
		XMLUtil.setAttributeIntValue(xmlVal2, "nb_items", detectedFliesList.size());
		int i=0;
		for (ROI roi: detectedFliesList) {
			Element subnode = XMLUtil.addElement(xmlVal2, "det"+i);
			roi.saveToXML(subnode);
			i++;
		}
		return true;
	}
	
	public boolean xmlLoadCage (Node node, int index) {
		if (node == null)
			return false;
		
		Element xmlVal = XMLUtil.getElement(node, "Cage"+index);
		if (xmlVal == null)
			return false;
		
		Element xmlVal2 = XMLUtil.getElement(xmlVal, "CageLimits");
		if (xmlVal2 != null) {
			cageLimitROI = (ROI2DPolygon) ROI.create("plugins.kernel.roi.roi2d.ROI2DPolygon");
			cageLimitROI.loadFromXML(xmlVal2);
		}
		
		xmlVal2 = XMLUtil.getElement(xmlVal, "FlyPositions");
		if (xmlVal2 != null) {
			flyPositions.loadFromXML(xmlVal2);
		}
		
		xmlVal2 = XMLUtil.getElement(xmlVal, "RoisDetected");
		if (xmlVal2 != null) {
			int nb_items =  XMLUtil.getAttributeIntValue(xmlVal2, "nb_items", detectedFliesList.size());
			for (int i=0; i< nb_items; i++) {
				ROI2DPolygon roi = (ROI2DPolygon) ROI.create("plugins.kernel.roi.roi2d.ROI2DPolygon");
				Element subnode = XMLUtil.getElement(xmlVal2, "det"+i);
				roi.loadFromXML(subnode);
				detectedFliesList.add((ROI2D) roi);
			}
		}
		return true;
	}

}
