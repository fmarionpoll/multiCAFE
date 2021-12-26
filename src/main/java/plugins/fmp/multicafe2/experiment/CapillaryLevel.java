package plugins.fmp.multicafe2.experiment;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import icy.file.xml.XMLPersistent;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;



public class CapillaryLevel  implements XMLPersistent  
{
	public Level2D 	polylineLevel 	= null;
	public Level2D 	polyline_old 	= null;
	
	public String 	typename 		= "notype";
	public String	name 			= "noname";
	public String 	header 			= null;
	
	private final String ID_NPOINTS	= "npoints";
	private final String ID_NAME	= "name";
	private final String ID_N		= "n";
	private final String ID_X		= "x";
	private final String ID_Y		= "y";
	
	// -------------------------
	
	CapillaryLevel(String typename) 
	{
		this.typename = typename;
		name = typename;
	}
	
	public CapillaryLevel(String name, Polyline2D polyline) 
	{
		this.name = name;
		polylineLevel = new Level2D(polyline);
	}
	
	public CapillaryLevel(String name, int indexImage, List<Point2D> limit) 
	{
		this.name = name;
		polylineLevel = new Level2D(limit);
	}
	
	int getNPoints() 
	{
		if (polylineLevel == null)
			return 0;
		return polylineLevel.npoints;
	}

	int restoreNPoints()  
	{
		if (polyline_old != null) 
			polylineLevel = polyline_old.clone();
		return polylineLevel.npoints;
	}
	
	void cropToNPoints(int npoints) 
	{
		if (polyline_old == null) 
			polyline_old = polylineLevel.clone();
		polylineLevel.npoints = npoints;
	}
	
	void copy(CapillaryLevel cap) 
	{
		if (cap.polylineLevel != null)
			polylineLevel = cap.polylineLevel.clone(); 
	}
	
	boolean isThereAnyMeasuresDone() 
	{
		return (polylineLevel != null && polylineLevel.npoints > 0);
	}
	
	ArrayList<Integer> getMeasures(long seriesBinMs, long outputBinMs) 
	{
		if (polylineLevel == null)
			return null;
		long maxMs = (polylineLevel.ypoints.length -1) * seriesBinMs;
		long npoints = (maxMs / outputBinMs)+1;
		ArrayList<Integer> arrayInt = new ArrayList<Integer>((int) npoints);
		for (double iMs = 0; iMs <= maxMs; iMs += outputBinMs) 
		{
			int index = (int) (iMs  / seriesBinMs);
			arrayInt.add((int) polylineLevel.ypoints[index]);
		}
		return arrayInt;
	}
	
	List<Integer> getMeasures() 
	{
		return getIntegerArrayFromPolyline2D();
	}
	
	int getLastMeasure() 
	{	
		if (polylineLevel == null)
			return 0;
		int lastitem = polylineLevel.ypoints.length - 1;
		int ivalue = (int) polylineLevel.ypoints[lastitem];
		return ivalue;
	}
	
	int getT0Measure() 
	{	
		if (polylineLevel == null)
			return 0;
		return (int) polylineLevel.ypoints[0];
	}
	
	int getLastDeltaMeasure() 
	{	
		if (polylineLevel == null)
			return 0;
		int lastitem = polylineLevel.ypoints.length - 1;
		return (int) (polylineLevel.ypoints[lastitem] - polylineLevel.ypoints[lastitem-1]);
	}
	
	List<ROI2D> addToROIs(List<ROI2D> listrois, int indexImage) 
	{
		if (polylineLevel != null) 
			listrois.add(transferPolyline2DToROI(indexImage));
		return listrois;
	}
	
	List<ROI2D> addToROIs(List<ROI2D> listrois, Color color, double stroke, int indexImage) 
	{
		if (polylineLevel != null) 
		{ 
			ROI2D roi = transferPolyline2DToROI(indexImage);
			roi.setColor(color);
			roi.setStroke(stroke);
			roi.setName(name);
			listrois.add(roi);
		}
		return listrois;
	}
	
	void transferROIsToMeasures(List<ROI> listRois) 
	{	
		for (ROI roi: listRois) 
		{		
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine && roiname .contains (name)) 
			{
				polylineLevel = new Level2D(((ROI2DPolyLine)roi).getPolyline2D());
				name = roiname;	
			}
		}
	}
	
	@Override
	public boolean loadFromXML(Node node) 
	{
		loadCapillaryLimitFromXML(node, typename, header);
		return false;
	}

	@Override
	public boolean saveToXML(Node node) 
	{
		saveCapillaryLimit2XML(node, typename);
		return false;
	}
	
	List<Integer> getIntegerArrayFromPolyline2D() 
	{
		if (polylineLevel == null)
			return null;
		List<Integer> arrayInt = new ArrayList<Integer>(polylineLevel.ypoints.length);
		for (double i: polylineLevel.ypoints)
			arrayInt.add((int) i);
		return arrayInt;
	}
	
	public ROI2D transferPolyline2DToROI(int indexImage) 
	{
		if (polylineLevel == null)
			return null;	
		ROI2D roi = new ROI2DPolyLine(polylineLevel); 
		roi.setName(name);
		roi.setT(indexImage);
		return roi;
	}
	
	public int loadCapillaryLimitFromXML(Node node, String nodename, String header) 
	{
		final Node nodeMeta = XMLUtil.getElement(node, nodename);
		int npoints = 0;
		polylineLevel = null;
	    if (nodeMeta != null)  
	    {
	    	name =  XMLUtil.getElementValue(nodeMeta, ID_NAME, nodename);
	    	if (!name.contains("_")) 
	    	{
	    		this.header = header;
	    		name = header + name;
	    	} 
	    	polylineLevel = loadPolyline2DFromXML(nodeMeta);
		    if (polylineLevel != null)
		    	npoints = polylineLevel.npoints;
	    }
		final Node nodeMeta_old = XMLUtil.getElement(node, nodename+"old");
		if (nodeMeta_old != null) 
			polyline_old = loadPolyline2DFromXML(nodeMeta_old);
	    return npoints;
	}

	Level2D loadPolyline2DFromXML(Node nodeMeta) 
	{
		Level2D line = null;
    	int npoints1 = XMLUtil.getElementIntValue(nodeMeta, ID_NPOINTS, 0);
    	if (npoints1 > 0) 
    	{
	    	double[] xpoints = new double [npoints1];
	    	double[] ypoints = new double [npoints1];
	    	for (int i=0; i< npoints1; i++) 
	    	{
	    		Element elmt = XMLUtil.getElement(nodeMeta, ID_N+i);
	    		if (i ==0)
	    			xpoints[i] = XMLUtil.getAttributeDoubleValue(elmt, ID_X, 0);
	    		else
	    			xpoints[i] = i+xpoints[0];
	    		ypoints[i] = XMLUtil.getAttributeDoubleValue(elmt, ID_Y, 0);
			}
	    	line = new Level2D(xpoints, ypoints, npoints1);
    	}
    	return line;
    }
	
	public void saveCapillaryLimit2XML(Node node, String nodename) 
	{
		if (polylineLevel == null)
			return;
		final Node nodeMeta = XMLUtil.setElement(node, nodename);
	    if (nodeMeta != null) 
	    {
	    	XMLUtil.setElementValue(nodeMeta, ID_NAME, name);
	    	saveLevel2XML(nodeMeta, polylineLevel);
	    	final Node nodeMeta_old = XMLUtil.setElement(node, nodename+"old");
		    if (polyline_old != null && polyline_old.npoints != polylineLevel.npoints) 
		    	saveLevel2XML(nodeMeta_old,  polyline_old);
	    }
	}
	
	void saveLevel2XML(Node nodeMeta, Polyline2D line)  
	{
		XMLUtil.setElementIntValue(nodeMeta, ID_NPOINTS, line.npoints);
    	for (int i=0; i< line.npoints; i++) 
    	{
    		Element elmt = XMLUtil.setElement(nodeMeta, ID_N+i);
    		if (i==0)
    			XMLUtil.setAttributeDoubleValue(elmt, ID_X, line.xpoints[i]);
    		XMLUtil.setAttributeDoubleValue(elmt, ID_Y, line.ypoints[i]);
    	}
	}
	
	public void adjustToImageWidth(int imageSize) 
	{
		if (polylineLevel == null)
			return;
		int npoints = polylineLevel.npoints;
		int npoints_old = 0;
		if (polyline_old != null && polyline_old.npoints > npoints) 
			npoints_old = polyline_old.npoints;
		if (npoints == imageSize || npoints_old == imageSize)
			return;
		
		// reduce polyline npoints to imageSize
		if (npoints > imageSize) 
		{
			int newSize = imageSize;
			if (npoints < npoints_old)
				newSize = 1 + imageSize *npoints / npoints_old;
			polylineLevel = polylineLevel.contractPolylineToNewSize(newSize);
			if (npoints_old != 0)
				polyline_old = polyline_old.contractPolylineToNewSize(imageSize);
		}
		// expand polyline npoints to imageSize
		else 
		{ 
			int newSize = imageSize;
			if (npoints < npoints_old)
				newSize = imageSize *npoints / npoints_old;
			polylineLevel = polylineLevel.expandPolylineToNewSize(newSize);
			if (npoints_old != 0)
				polyline_old = polyline_old.expandPolylineToNewSize(imageSize);
		}
	}

	public void cropToImageWidth(int imageSize) 
	{
		if (polylineLevel == null)
			return;
		int npoints = polylineLevel.npoints;
		if (npoints == imageSize)
			return;
		
		int npoints_old = 0;
		if (polyline_old != null && polyline_old.npoints > npoints) 
			npoints_old = polyline_old.npoints;
		if (npoints == imageSize || npoints_old == imageSize)
			return;
		
		// reduce polyline npoints to imageSize
		int newSize = imageSize;
		polylineLevel = polylineLevel.cropPolylineToNewSize(newSize);		
	}
	
}
