package plugins.fmp.multicafeSequence;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.ROI2DRectangle;



public class Cage {
	public ROI2D 		roi						= null;
	public 	int			frameStep				= 1;
	public	int			frameStart				= 0;
	public	int			frameEnd				= 1;
	public XYTaSeries 	flyPositions 			= new XYTaSeries();
	public List<ROI2D> 	detectedFliesList		= new ArrayList<ROI2D>();
	public int 			cageNFlies  			= 1;
	public String 		strCageComment 			= "..";
	private String 		strCageNumber 			= null;
	public	boolean		valid					= false;
	public	boolean		saveDetectedROIs		= false;
	
	private final String ID_CAGELIMITS 			= "CageLimits";
	private final String ID_FLYPOSITIONS		= "FlyPositions";
	private final String ID_ROISDETECTED		= "RoisDetected";
	private final String ID_NBITEMS				= "nb_items";
	private final String ID_NFLIES 				= "nflies"; 
	private final String ID_COMMENT				= "comment";
	private final String ID_STARTFRAME 			= "startFrame";
	private final String ID_ENDFRAME 			= "endFrame";
	private final String ID_STEP 				= "stepFrame";
	
	
	public boolean xmlSaveCage (Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "Cage"+index);		
		xmlSaveCageLimitsAndParameters(xmlVal);
		xmlSaveFlyPositions(xmlVal);
		if (saveDetectedROIs)
			xmlSaveDetecteRois(xmlVal);
		return true;
	}
	
	public boolean xmlSaveCageLimitsAndParameters(Element xmlVal) {
		XMLUtil.setElementIntValue(xmlVal, ID_NFLIES, cageNFlies);
		XMLUtil.setElementValue(xmlVal, ID_COMMENT, strCageComment);
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_CAGELIMITS);
		if (roi != null) {
			roi.setSelected(false);
			roi.saveToXML(xmlVal2);
		}
		XMLUtil.setElementIntValue(xmlVal, ID_STARTFRAME, frameStart);
		XMLUtil.setElementIntValue(xmlVal, ID_ENDFRAME, frameEnd);
		XMLUtil.setElementIntValue(xmlVal, ID_STEP, frameStep);
		return true;
	}
	
	public boolean xmlSaveFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_FLYPOSITIONS);
		flyPositions.saveToXML(xmlVal2);
		return true;
	}
	
	public boolean xmlSaveDetecteRois(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_ROISDETECTED);
		XMLUtil.setAttributeIntValue(xmlVal2, ID_NBITEMS, detectedFliesList.size());
		int i=0;
		for (ROI roi: detectedFliesList) {
			if (roi != null) {
				Element subnode = XMLUtil.addElement(xmlVal2, "det"+i);
				roi.saveToXML(subnode);
			} 
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
		getCageLimits(xmlVal);
		getFlyPositions(xmlVal);
		getRoisDetected(xmlVal);
		return true;
	}
	
	public boolean getCageLimits (Element xmlVal) {
		cageNFlies 		= XMLUtil.getElementIntValue(xmlVal, ID_NFLIES, cageNFlies);
		strCageComment 	= XMLUtil.getElementValue(xmlVal, ID_COMMENT, strCageComment);
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_CAGELIMITS);
		if (xmlVal2 != null) {
			roi = (ROI2D) ROI.createFromXML(xmlVal2 );
	        roi.setSelected(false);
		}
		frameStart 	= XMLUtil.getElementIntValue(xmlVal, ID_STARTFRAME, frameStart);
		frameEnd 	= XMLUtil.getElementIntValue(xmlVal, ID_ENDFRAME, frameEnd);
		frameStep 	= XMLUtil.getElementIntValue(xmlVal, ID_STEP, frameStep);
		return true;
	}
	
	public boolean getFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_FLYPOSITIONS);
		if (xmlVal2 != null) {
			flyPositions.loadFromXML(xmlVal2);
			return true;
		}
		return false;
	}
	
	public boolean getRoisDetected(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_ROISDETECTED);
		if (xmlVal2 != null) {
			int nb_items =  XMLUtil.getAttributeIntValue(xmlVal2, ID_NBITEMS, detectedFliesList.size());
			for (int i=0; i< nb_items; i++) {
				Element subnode = XMLUtil.getElement(xmlVal2, "det"+i);
				ROI roi = ROI.createFromXML(subnode);
				detectedFliesList.add((ROI2D) roi);
			}
		}
		return true;
	}

	public String getCageNumber() {
		if (strCageNumber == null) 
			strCageNumber = roi.getName().substring(roi.getName().length() - 3);
		return strCageNumber;
	}
	
	public int getCageNumberInteger() {
		int cagenb = -1;
		strCageNumber = getCageNumber();
		if (strCageNumber != null) {
			cagenb = Integer.valueOf(strCageNumber);
		}
		return cagenb;
	}
	
	public void clearMeasures () {
		detectedFliesList.clear();
		flyPositions.clear();
	}
	
	public Point2D getCenterTopCage() {
		Rectangle2D rect = roi.getBounds2D();
		Point2D pt = new Point2D.Double(rect.getX() + rect.getWidth()/2, rect.getY());
		return pt;
	}
	
	public Point2D getCenterTipCapillaries(Capillaries capList) {
		List<Point2D> listpts = new ArrayList<Point2D>();
		for (Capillary cap: capList.capillariesArrayList) {
			Point2D pt = cap.getCapillaryTipWithinROI2D(roi);
			if (pt != null)
				listpts.add(pt);
		}
		double x = 0;
		double y = 0;
		int n = listpts.size();
		for (Point2D pt: listpts) {
			x  += pt.getX();
			y += pt.getY();
		}
		Point2D pt = new Point2D.Double(x/n, y/n);
		return pt;
	}
	
	public void copy (Cage cag) {
		roi	= cag.roi;
		frameStep = cag.frameStep;
		frameStart = cag.frameStart;
		frameEnd = cag.frameEnd;
		cageNFlies  = cag.cageNFlies;
		strCageComment = cag.strCageComment;
		strCageNumber = cag.strCageNumber;
		valid = false; 
		
		flyPositions.copy(cag.flyPositions);
		
		detectedFliesList	= new ArrayList<ROI2D>();
		detectedFliesList.addAll(cag.detectedFliesList);
	}
	
	public void transferPositionsToRois() {
		int width = 10;
		int height = 10;
		detectedFliesList.clear();
		for (XYTaValue aValue: flyPositions.pointsList) {
			ROI2DRectangle flyRect = new ROI2DRectangle(
					aValue.xytPoint.getX()-width/2, 
					aValue.xytPoint.getY()-height/2, 
					aValue.xytPoint.getX()+width, 
					aValue.xytPoint.getY()+height);
			int t = aValue.xytTime;
			flyRect.setName("det"+getCageNumber() +"_" + t );
			flyRect.setT( t );
			// TODO flyRect.setColor(value);
			detectedFliesList.add(flyRect);
		}
	}
	
	
}
