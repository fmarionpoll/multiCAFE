package plugins.fmp.multicafe.sequence;

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



public class CapillaryLimit  implements XMLPersistent  {

	public Level2D 	polylineLimit 	= null;
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
	
	CapillaryLimit(String typename) {
		this.typename = typename;
		name = typename;
	}
	
	public CapillaryLimit(String name, Polyline2D polyline) {
		this.name = name;
		polylineLimit = new Level2D(polyline);
	}
	
	public CapillaryLimit(String name, int indexImage, List<Point2D> limit) {
		this.name = name;
		polylineLimit = new Level2D(limit);
	}
	
	int getNpoints() {
		if (polylineLimit == null)
			return 0;
		return polylineLimit.npoints;
	}

	int restoreNpoints() {
		if (polyline_old != null) {
			polylineLimit = polyline_old.clone();
		}
		return polylineLimit.npoints;
	}
	
	void cropToNPoints(int npoints) {
		if (polyline_old == null) {
			polyline_old = polylineLimit.clone();
		}
		polylineLimit.npoints = npoints;
	}
	
	void copy(CapillaryLimit cap) {
		if (cap.polylineLimit != null)
			polylineLimit = cap.polylineLimit.clone(); 
	}
	
	boolean isThereAnyMeasuresDone() {
		boolean yes = (polylineLimit != null && polylineLimit.npoints > 0);
		return yes;
	}
	
	List<Integer> getMeasures() {
		List<Integer> datai = getIntegerArrayFromPolyline2D();
		return datai;
	}
	
	int getLastMeasure() {	
		if (polylineLimit == null)
			return 0;
		int lastitem = polylineLimit.ypoints.length - 1;
		int ivalue = (int) polylineLimit.ypoints[lastitem];
		return ivalue;
	}
	
	int getT0Measure() {	
		if (polylineLimit == null)
			return 0;
		int ivalue = (int) polylineLimit.ypoints[0];
		return ivalue;
	}
	
	int getLastDeltaMeasure() {	
		if (polylineLimit == null)
			return 0;
		int lastitem = polylineLimit.ypoints.length - 1;
		int ivalue = (int) (polylineLimit.ypoints[lastitem] - polylineLimit.ypoints[lastitem-1]);
		return ivalue;
	}
	
	List<ROI2D> addToROIs(List<ROI2D> listrois, int indexImage) {
		if (polylineLimit != null) 
			listrois.add(transferPolyline2DToROI(indexImage));
		return listrois;
	}
	
	List<ROI2D> addToROIs(List<ROI2D> listrois, Color color, double stroke, int indexImage) {
		if (polylineLimit != null) { 
			ROI2D roi = transferPolyline2DToROI(indexImage);
			roi.setColor(color);
			roi.setStroke(stroke);
			roi.setName(name);
			listrois.add(roi);
		}
		return listrois;
	}
	
	void transferROIsToMeasures(List<ROI> listRois) {	
		for (ROI roi: listRois) {		
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine ) {
				if  (roiname .contains (name)) {
					polylineLimit = new Level2D(((ROI2DPolyLine)roi).getPolyline2D());
					name = roiname;
				}
			}
		}
	}
	
	@Override
	public boolean loadFromXML(Node node) {
		loadCapillaryLimitFromXML(node, typename, header);
		return false;
	}

	@Override
	public boolean saveToXML(Node node) {
		saveCapillaryLimit2XML(node, typename);
		return false;
	}
	
	List<Integer> getIntegerArrayFromPolyline2D() {
		if (polylineLimit == null)
			return null;
		List<Integer> arrayInt = new ArrayList<Integer>(polylineLimit.ypoints.length);
		for (double i: polylineLimit.ypoints)
			arrayInt.add((int) i);
		return arrayInt;
	}
	
	public ROI2D transferPolyline2DToROI(int indexImage) {
		if (polylineLimit == null)
			return null;
		
		ROI2D roi = new ROI2DPolyLine(polylineLimit); 
		roi.setName(name);
		roi.setT(indexImage);
		return roi;
	}
	
	int loadCapillaryLimitFromXML(Node node, String nodename, String header) {
		final Node nodeMeta = XMLUtil.getElement(node, nodename);
		int npoints = 0;
		polylineLimit = null;
	    if (nodeMeta != null) {
	    	name =  XMLUtil.getElementValue(nodeMeta, ID_NAME, nodename);
	    	if (!name.contains("_")) {
	    		this.header = header;
	    		name = header + name;
	    	} 
	    	polylineLimit = loadPolyline2DFromXML(nodeMeta);
		    if (polylineLimit != null)
		    	npoints = polylineLimit.npoints;
	    }
		final Node nodeMeta_old = XMLUtil.getElement(node, nodename+"old");
		if (nodeMeta_old != null) {
			polyline_old = loadPolyline2DFromXML(nodeMeta_old);
	    }
	    return npoints;
	}

	Level2D loadPolyline2DFromXML(Node nodeMeta) {
		Level2D line = null;
    	int npoints1 = XMLUtil.getElementIntValue(nodeMeta, ID_NPOINTS, 0);
    	if (npoints1 > 0) {
	    	double[] xpoints = new double [npoints1];
	    	double[] ypoints = new double [npoints1];
	    	for (int i=0; i< npoints1; i++) {
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
	
	void saveCapillaryLimit2XML(Node node, String nodename) {
		if (polylineLimit == null)
			return;
		final Node nodeMeta = XMLUtil.setElement(node, nodename);
	    if (nodeMeta != null) {
	    	XMLUtil.setElementValue(nodeMeta, ID_NAME, name);
	    	saveLevel2XML(nodeMeta, polylineLimit);
	    	final Node nodeMeta_old = XMLUtil.setElement(node, nodename+"old");
		    if (polyline_old != null && polyline_old.npoints != polylineLimit.npoints) 
		    	saveLevel2XML(nodeMeta_old,  polyline_old);
	    }
	}
	
	void saveLevel2XML(Node nodeMeta, Polyline2D line) {
		XMLUtil.setElementIntValue(nodeMeta, ID_NPOINTS, line.npoints);
    	for (int i=0; i< line.npoints; i++) {
    		Element elmt = XMLUtil.setElement(nodeMeta, ID_N+i);
    		if (i==0)
    			XMLUtil.setAttributeDoubleValue(elmt, ID_X, line.xpoints[i]);
    		XMLUtil.setAttributeDoubleValue(elmt, ID_Y, line.ypoints[i]);
    	}
	}
	
	public void adjustToImageWidth(int imageSize) {
		if (polylineLimit == null)
			return;

		int npoints = polylineLimit.npoints;
		int npoints_old = 0;
		if (polyline_old != null && polyline_old.npoints > npoints) {
			npoints_old = polyline_old.npoints;
		}
		if (npoints == imageSize || npoints_old == imageSize)
			return;
		
		// reduce polyline npoints to imageSize
		if (npoints > imageSize) {
			int newSize = imageSize;
			if (npoints < npoints_old)
				newSize = imageSize *npoints / npoints_old;
			polylineLimit = polylineLimit.reducePolylineToNewSize(newSize);
			if (npoints_old != 0)
				polyline_old = polyline_old.reducePolylineToNewSize(imageSize);
		}
		// expand polyline npoints to imageSize
		else { 
			int newSize = imageSize;
			if (npoints < npoints_old)
				newSize = imageSize *npoints / npoints_old;
			polylineLimit = polylineLimit.expandPolylineToNewSize(newSize);
			if (npoints_old != 0)
				polyline_old = polyline_old.expandPolylineToNewSize(imageSize);
		}
	}
	
	
}
