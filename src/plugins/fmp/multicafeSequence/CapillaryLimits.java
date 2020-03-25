package plugins.fmp.multicafeSequence;

import java.awt.Color;
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



public class CapillaryLimits  implements XMLPersistent  {

	public Polyline2D 	polyline 		= null;
	public Polyline2D 	polyline_old 	= null;
	
	public String 		typename 		= "notype";
	public String		name 			= "noname";
	public String 		header 			= null;
	public int			indexImage 		= 0;
	
	private final String ID_NPOINTS		= "npoints";
	private final String ID_N			= "n";
	private final String ID_X			= "x";
	private final String ID_Y			= "y";
	
	// -------------------------
	
	CapillaryLimits(String typename, int indexImage) {
		this.typename = typename;
		name = typename;
		this.indexImage = indexImage;
	}
	
	public CapillaryLimits(String name, int indexImage, Polyline2D polyline) {
		this.name = name;
		this.indexImage = indexImage;
		this.polyline = polyline;
	}
	
	int getNpoints() {
		if (polyline == null)
			return 0;
		return polyline.npoints;
	}

	int restoreNpoints() {
		if (polyline_old != null) {
			polyline = (Polyline2D) polyline_old.clone();
		}
		return polyline.npoints;
	}
	
	void cropToNPoints(int npoints) {
		if (polyline_old == null) {
			polyline_old = (Polyline2D) polyline.clone();
		}
		polyline.npoints = npoints;
	}
	
	void copy(CapillaryLimits cap) {
		polyline = (Polyline2D) cap.polyline.clone(); 
	}
	
	boolean isThereAnyMeasuresDone() {
		boolean yes = (polyline != null && polyline.npoints > 0);
		return yes;
	}
	
	List<Integer> getMeasures() {
		List<Integer> datai = getIntegerArrayFromPolyline2D();
		return datai;
	}
	
	int getLastMeasure() {	
		if (polyline == null)
			return 0;
		int lastitem = polyline.ypoints.length - 1;
		int ivalue = (int) polyline.ypoints[lastitem];
		return ivalue;
	}
	
	int getT0Measure() {	
		if (polyline == null)
			return 0;
		int ivalue = (int) polyline.ypoints[0];
		return ivalue;
	}
	
	int getLastDeltaMeasure() {	
		if (polyline == null)
			return 0;
		int lastitem = polyline.ypoints.length - 1;
		int ivalue = (int) (polyline.ypoints[lastitem] - polyline.ypoints[lastitem-1]);
		return ivalue;
	}
	
	List<ROI2D> addToROIs(List<ROI2D> listrois, int indexImage) {
		this.indexImage = indexImage;
		if (polyline != null) 
			listrois.add(transferPolyline2DToROI());
		return listrois;
	}
	
	List<ROI2D> addToROIs(List<ROI2D> listrois, Color color, double stroke, int indexImage) {
		if (polyline != null) { 
			this.indexImage = indexImage;
			ROI2D roi = transferPolyline2DToROI();
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
					polyline = ((ROI2DPolyLine)roi).getPolyline2D();
					name = roiname;
					((ROI2DPolyLine) roi).setT(indexImage);
				}
			}
		}
	}
	
	@Override
	public boolean loadFromXML(Node node) {
		loadPolyline2DFromXML(node, typename, header);
		return false;
	}

	@Override
	public boolean saveToXML(Node node) {
		savePolyline2DToXML(node, typename);
		return false;
	}
	
	List<Integer> getIntegerArrayFromPolyline2D() {
		if (polyline == null)
			return null;
		List<Integer> arrayInt = new ArrayList<Integer>(polyline.ypoints.length);
		for (double i: polyline.ypoints)
			arrayInt.add((int) i);
		return arrayInt;
	}
	
	public ROI2D transferPolyline2DToROI() {
		if (polyline == null)
			return null;
		
		ROI2D roi = new ROI2DPolyLine(polyline); 
		roi.setName(name);
		roi.setT(indexImage);
		return roi;
	}
	
	int loadPolyline2DFromXML(Node node, String nodename, String header) {
		final Node nodeMeta = XMLUtil.getElement(node, nodename);
		int npoints = 0;
		polyline = null;
	    if (nodeMeta != null) {
	    	name =  XMLUtil.getElementValue(nodeMeta, "name", nodename);
	    	if (!name.contains("_")) {
	    		this.header = header;
	    		name = header + name;
	    	} 
	    	polyline = loadPolyline2DFromXML(nodeMeta);
		    if (polyline != null)
		    	npoints = polyline.npoints;
	    }
		final Node nodeMeta_old = XMLUtil.getElement(node, nodename+"old");
		if (nodeMeta_old != null) {
			polyline_old = loadPolyline2DFromXML(nodeMeta_old);
	    }
	    return npoints;
	}

	Polyline2D loadPolyline2DFromXML(Node nodeMeta) {
		Polyline2D line = null;
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
	    	line = new Polyline2D(xpoints, ypoints, npoints1);
    	}
    	return line;
    }
	
	void savePolyline2DToXML(Node node, String nodename) {
		if (polyline == null)
			return;
		final Node nodeMeta = XMLUtil.setElement(node, nodename);
	    if (nodeMeta != null) {
	    	XMLUtil.setElementValue(nodeMeta, "name", name);
	    	savePolyline2DtoXML(nodeMeta, polyline);
	    }
	    final Node nodeMeta_old = XMLUtil.setElement(node, nodename+"old");
	    if (polyline_old != null && polyline_old.npoints != polyline.npoints) 
	    	savePolyline2DtoXML(nodeMeta_old,  polyline_old);
	}
	
	void savePolyline2DtoXML(Node nodeMeta, Polyline2D line) {
		XMLUtil.setElementIntValue(nodeMeta, ID_NPOINTS, line.npoints);
    	for (int i=0; i< line.npoints; i++) {
    		Element elmt = XMLUtil.setElement(nodeMeta, ID_N+i);
    		if (i==0)
    			XMLUtil.setAttributeDoubleValue(elmt, ID_X, line.xpoints[i]);
    		XMLUtil.setAttributeDoubleValue(elmt, ID_Y, line.ypoints[i]);
    	}
	}
	
	public void adjustToImageWidth(int imageSize) {
		if (polyline == null || polyline.npoints == imageSize)
			return;
		else if (polyline.npoints > imageSize) {
			double [] xpoints = new double[imageSize];
			double [] ypoints = new double [imageSize];
			for (int i=0; i< imageSize; i++) {
				int j = i * polyline.npoints / imageSize;
				xpoints[i] = i;
				ypoints[i] = polyline.ypoints[j];
			}
			polyline = new Polyline2D (xpoints, ypoints, imageSize);
		}
		else { // imageSize > polyline.npoints
			double [] xpoints = new double[imageSize];
			double [] ypoints = new double [imageSize];
			for (int j=0; j< polyline.npoints; j++) {
				int i0 = j * imageSize / polyline.npoints;
				int i1 = (j +1) * imageSize / polyline.npoints;
				double y0 = polyline.ypoints[j];
				double y1 = y0;
				if ((j+1) < polyline.npoints)
					y1 = polyline.ypoints[j+1]; 
				for (int i = i0; i< i1; i++) {
					xpoints[i] = i;
					ypoints[i] = y0 + (y1-y0) * (i-i0)/(i1-i0);
				}
			}
			polyline = new Polyline2D (xpoints, ypoints, imageSize);
		}
	}


}
