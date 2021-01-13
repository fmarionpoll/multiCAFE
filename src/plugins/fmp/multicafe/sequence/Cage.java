package plugins.fmp.multicafe.sequence;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.ROI2DPoint;



public class Cage {
	public ROI2D 		cageRoi					= null;

	public XYTaSeriesArrayList 	flyPositions 			= new XYTaSeriesArrayList();
	public int 			cageNFlies  			= 1;
	public int 			cageAge 				= 5;
	public String 		strCageComment 			= "..";
	public String 		strCageSex 				= "..";
	public String 		strCageStrain 			= "..";
	private String 		strCageNumber 			= null;
	public	boolean		valid					= false;
	public	boolean		bDetect					= true;
	
	private final String ID_CAGELIMITS 			= "CageLimits";
	private final String ID_FLYPOSITIONS		= "FlyPositions";
	private final String ID_NFLIES 				= "nflies"; 
	private final String ID_AGE 				= "age"; 
	private final String ID_COMMENT				= "comment";
	private final String ID_SEX					= "sex";
	private final String ID_STRAIN				= "strain";
	
	
	
	public boolean xmlSaveCage (Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "Cage"+index);		
		xmlSaveCageLimits(xmlVal);
		xmlSaveCageParameters(xmlVal);
		if (cageNFlies > 0)
			xmlSaveFlyPositions(xmlVal);
		return true;
	}
	
	public boolean xmlSaveCageParameters(Element xmlVal) {
		XMLUtil.setElementIntValue(xmlVal, ID_NFLIES, cageNFlies);
		XMLUtil.setElementIntValue(xmlVal, ID_AGE, cageAge);
		XMLUtil.setElementValue(xmlVal, ID_COMMENT, strCageComment);
		XMLUtil.setElementValue(xmlVal, ID_SEX, strCageSex);
		XMLUtil.setElementValue(xmlVal, ID_STRAIN, strCageStrain);
		
		return true;
	}
	
	public boolean xmlSaveCageLimits(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_CAGELIMITS);
		if (cageRoi != null) {
			cageRoi.setSelected(false);
			cageRoi.saveToXML(xmlVal2);
		}
		return true;
	}
	
	public boolean xmlSaveFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.addElement(xmlVal, ID_FLYPOSITIONS);
		flyPositions.saveToXML(xmlVal2);
		return true;
	}
	
	public boolean xmlLoadCage (Node node, int index) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "Cage"+index);
		if (xmlVal == null)
			return false;
		xmlLoadCageLimits(xmlVal);
		xmlLoadCageParameters(xmlVal);
		xmlLoadFlyPositions(xmlVal); 
		return true;
	}
	
	public boolean xmlLoadCageLimits (Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_CAGELIMITS);
		if (xmlVal2 != null) {
			cageRoi = (ROI2D) ROI.createFromXML(xmlVal2 );
	        cageRoi.setSelected(false);
		}
		return true;
	}
	
	public boolean xmlLoadCageParameters (Element xmlVal) {
		cageNFlies 		= XMLUtil.getElementIntValue(xmlVal, ID_NFLIES, cageNFlies);
		cageAge 		= XMLUtil.getElementIntValue(xmlVal, ID_AGE, cageAge);
		strCageComment 	= XMLUtil.getElementValue(xmlVal, ID_COMMENT, strCageComment);
		strCageSex 		= XMLUtil.getElementValue(xmlVal, ID_SEX, strCageSex);
		strCageStrain 	= XMLUtil.getElementValue(xmlVal, ID_STRAIN, strCageStrain);
		return true;
	}
	
	public boolean xmlLoadFlyPositions(Element xmlVal) {
		Element xmlVal2 = XMLUtil.getElement(xmlVal, ID_FLYPOSITIONS);
		if (xmlVal2 != null) {
			flyPositions.loadFromXML(xmlVal2);
			return true;
		}
		return false;
	}

	public String getCageNumber() {
		if (strCageNumber == null) 
			strCageNumber = cageRoi.getName().substring(cageRoi.getName().length() - 3);
		return strCageNumber;
	}
	
	public int getCageNumberInteger() {
		int cagenb = -1;
		strCageNumber = getCageNumber();
		if (strCageNumber != null) {
			try {
			    return Integer.parseInt(strCageNumber);
			  } catch (NumberFormatException e) {
			    return cagenb;
			  }
		}
		return cagenb;
	}
	
	public void clearMeasures () {
		flyPositions.clear();
	}
	
	public Point2D getCenterTopCage() {
		Rectangle2D rect = cageRoi.getBounds2D();
		Point2D pt = new Point2D.Double(rect.getX() + rect.getWidth()/2, rect.getY());
		return pt;
	}
	
	public Point2D getCenterTipCapillaries(Capillaries capList) {
		List<Point2D> listpts = new ArrayList<Point2D>();
		for (Capillary cap: capList.capillariesArrayList) {
			Point2D pt = cap.getCapillaryTipWithinROI2D(cageRoi);
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
	
	public void copy (Cage cage) {
		cageRoi			= cage.cageRoi;
		cageNFlies  	= cage.cageNFlies;
		strCageComment 	= cage.strCageComment;
		strCageNumber 	= cage.strCageNumber;
		valid 			= false; 
		flyPositions.copy(cage.flyPositions);
	}
	
	public ROI2DPoint getRoiPointFromPositionAtT(int t) {
		int nitems = flyPositions.xytList.size();
		if (nitems == 0 || t >= nitems)
			return null;
		XYTaValue aValue = flyPositions.xytList.get(t);
		ROI2DPoint flyRoi = new ROI2DPoint(aValue.xyPoint.getX(), aValue.xyPoint.getY());
		flyRoi.setName("det"+getCageNumber() +"_" + t );
		flyRoi.setT( t );
		return flyRoi;
	}
	
	public void transferRoisToPositions(List<ROI2D> detectedROIsList) {
		String filter = "det"+getCageNumber();
		for (ROI2D roi: detectedROIsList) {
			String name = roi.getName();
			if (!name .contains(filter))
				continue;
			Point2D point = ((ROI2DPoint) roi).getPoint();
			int t = roi.getT();	
//			for (XYTaValue aValue: flyPositions.xytList) {
//				if (aValue.indexT == t) {
//					aValue.xyPoint = point;
//					break;
//				}
//			}
			flyPositions.xytList.get(t).xyPoint = point;
		}
	}
	
	
}
