package plugins.fmp.multicafe2.experiment;

import java.awt.geom.Point2D;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;


public class XYTaValue implements XMLPersistent 
{
	public Point2D 	xyPoint 	= new Point2D.Double(Double.NaN, Double.NaN);
	public int 		indexT 		= 0;
	public boolean 	bAlive 		= false;
	public boolean 	bSleep 		= false;
	public boolean  bPadded		= false;
	public double	distance 	= 0.;
	
	
	public XYTaValue() 
	{
	}
	
	public XYTaValue(int indexT) 
	{
		this.indexT = indexT;
	}
	
	public XYTaValue(int indexT, Point2D point) 
	{
		this.xyPoint = point;
		this.indexT = indexT;
	}
	
	public XYTaValue(int indexT, Point2D point, boolean alive) 
	{
		this.xyPoint = point;
		this.indexT = indexT;
		this.bAlive = alive;
	}
	
	public void copy (XYTaValue aVal) 
	{
		xyPoint = (Point2D) aVal.xyPoint.clone();
		indexT = aVal.indexT;
		bAlive = aVal.bAlive;
		bSleep = aVal.bSleep;
		bPadded = aVal.bPadded;
		distance = aVal.distance;
	}
	
	@Override
	public boolean loadFromXML(Node node) 
	{
		if (node == null)
			return false;	
		Element node_XYTa = XMLUtil.getElement(node, "XYTa");	
		double x =  XMLUtil.getAttributeDoubleValue( node_XYTa, "x", Double.NaN);
		double y =  XMLUtil.getAttributeDoubleValue( node_XYTa, "y", Double.NaN);
		xyPoint.setLocation(x, y);
		indexT =  XMLUtil.getAttributeIntValue(node_XYTa, "t", 0);
		bAlive = XMLUtil.getAttributeBooleanValue(node_XYTa, "a", false);
		bSleep = XMLUtil.getAttributeBooleanValue(node_XYTa, "s", false);
		return false;
	}

	@Override
	public boolean saveToXML(Node node) 
	{
		if (node == null)
			return false;		
		Element node_XYTa = XMLUtil.addElement(node, "XYTa");
		XMLUtil.setAttributeDoubleValue(node_XYTa, "x", xyPoint.getX());
		XMLUtil.setAttributeDoubleValue(node_XYTa, "y", xyPoint.getY());
		XMLUtil.setAttributeIntValue(node_XYTa, "t", indexT);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "a", bAlive);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "s", bSleep);
		return false;
	}
}
