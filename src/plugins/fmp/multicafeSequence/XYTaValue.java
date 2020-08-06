package plugins.fmp.multicafeSequence;

import java.awt.geom.Point2D;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;

public class XYTaValue implements XMLPersistent {
	public Point2D 	xytPoint 	= new Point2D.Double(Double.NaN, Double.NaN);
	public int 		xytTime 	= 0;
	public boolean 	xytAlive 	= false;
	public boolean 	xytSleep 	= false;
	public boolean  xytPadded	= false;
	public double	xytDistance = 0.;
	
	
	
	public XYTaValue() {
	}
	
	public XYTaValue(int time) {
		this.xytTime = time;
	}
	
	public XYTaValue(Point2D point, int time) {
		this.xytPoint = point;
		this.xytTime = time;
	}
	
	public XYTaValue(Point2D point, int time, boolean alive) {
		this.xytPoint = point;
		this.xytTime = time;
		this.xytAlive = alive;
	}
	
	public void copy (XYTaValue aVal) {
		xytPoint = (Point2D) aVal.xytPoint.clone();
		xytTime = aVal.xytTime;
		xytAlive = aVal.xytAlive;
		xytSleep = aVal.xytSleep;
		xytPadded = aVal.xytPadded;
		xytDistance = aVal.xytDistance;
	}
	
	@Override
	public boolean loadFromXML(Node node) {
		if (node == null)
			return false;
		
		Element node_XYTa = XMLUtil.getElement(node, "XYTa");
		
		double x =  XMLUtil.getAttributeDoubleValue( node_XYTa, "x", 0);
		double y =  XMLUtil.getAttributeDoubleValue( node_XYTa, "y", 0);
		xytPoint.setLocation(x, y);
		xytTime =  XMLUtil.getAttributeIntValue(node_XYTa, "t", 0);
		xytAlive = XMLUtil.getAttributeBooleanValue(node_XYTa, "a", false);
		xytSleep = XMLUtil.getAttributeBooleanValue(node_XYTa, "s", false);
		return false;
	}

	@Override
	public boolean saveToXML(Node node) {
		if (node == null)
			return false;
		
		Element node_XYTa = XMLUtil.addElement(node, "XYTa");
		XMLUtil.setAttributeDoubleValue(node_XYTa, "x", xytPoint.getX());
		XMLUtil.setAttributeDoubleValue(node_XYTa, "y", xytPoint.getY());
		XMLUtil.setAttributeIntValue(node_XYTa, "t", xytTime);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "a", xytAlive);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "s", xytSleep);
		return false;
	}
}
