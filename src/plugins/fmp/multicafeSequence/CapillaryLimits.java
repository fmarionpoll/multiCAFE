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

	public Polyline2D 	ppolyline 		= null;
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
		this.ppolyline = polyline;
	}
	
	int getNpoints() {
		if (ppolyline == null)
			return 0;
		return ppolyline.npoints;
	}

	int restoreNpoints() {
		if (polyline_old != null) {
			ppolyline = (Polyline2D) polyline_old.clone();
		}
		return ppolyline.npoints;
	}
	
	void cropToNPoints(int npoints) {
		if (polyline_old == null) {
			polyline_old = (Polyline2D) ppolyline.clone();
		}
		ppolyline.npoints = npoints;
	}
	
	void copy(CapillaryLimits cap) {
		ppolyline = (Polyline2D) cap.ppolyline.clone(); 
	}
	
	boolean isThereAnyMeasuresDone() {
		boolean yes = (ppolyline != null && ppolyline.npoints > 0);
		return yes;
	}
	
	List<Integer> getMeasures() {
		List<Integer> datai = getIntegerArrayFromPolyline2D();
		return datai;
	}
	
	int getLastMeasure() {	
		if (ppolyline == null)
			return 0;
		int lastitem = ppolyline.ypoints.length - 1;
		int ivalue = (int) ppolyline.ypoints[lastitem];
		return ivalue;
	}
	
	int getT0Measure() {	
		if (ppolyline == null)
			return 0;
		int ivalue = (int) ppolyline.ypoints[0];
		return ivalue;
	}
	
	int getLastDeltaMeasure() {	
		if (ppolyline == null)
			return 0;
		int lastitem = ppolyline.ypoints.length - 1;
		int ivalue = (int) (ppolyline.ypoints[lastitem] - ppolyline.ypoints[lastitem-1]);
		return ivalue;
	}
	
	List<ROI2D> addToROIs(List<ROI2D> listrois, int indexImage) {
		this.indexImage = indexImage;
		if (ppolyline != null) 
			listrois.add(transferPolyline2DToROI());
		return listrois;
	}
	
	List<ROI2D> addToROIs(List<ROI2D> listrois, Color color, double stroke, int indexImage) {
		if (ppolyline != null) { 
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
					ppolyline = ((ROI2DPolyLine)roi).getPolyline2D();
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
		if (ppolyline == null)
			return null;
		List<Integer> arrayInt = new ArrayList<Integer>(ppolyline.ypoints.length);
		for (double i: ppolyline.ypoints)
			arrayInt.add((int) i);
		return arrayInt;
	}
	
	public ROI2D transferPolyline2DToROI() {
		if (ppolyline == null)
			return null;
		
		ROI2D roi = new ROI2DPolyLine(ppolyline); 
		roi.setName(name);
		roi.setT(indexImage);
		return roi;
	}
	
	int loadPolyline2DFromXML(Node node, String nodename, String header) {
		final Node nodeMeta = XMLUtil.getElement(node, nodename);
		int npoints = 0;
		ppolyline = null;
	    if (nodeMeta != null) {
	    	name =  XMLUtil.getElementValue(nodeMeta, "name", nodename);
	    	if (!name.contains("_")) {
	    		this.header = header;
	    		name = header + name;
	    	} 
	    	ppolyline = loadPolyline2DFromXML(nodeMeta);
		    if (ppolyline != null)
		    	npoints = ppolyline.npoints;
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
		if (ppolyline == null)
			return;
		final Node nodeMeta = XMLUtil.setElement(node, nodename);
	    if (nodeMeta != null) {
	    	XMLUtil.setElementValue(nodeMeta, "name", name);
	    	savePolyline2DtoXML(nodeMeta, ppolyline);
	    }
	    final Node nodeMeta_old = XMLUtil.setElement(node, nodename+"old");
	    if (polyline_old != null && polyline_old.npoints != ppolyline.npoints) 
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
		if (ppolyline == null || ppolyline.npoints == imageSize)
			return;
		else if (ppolyline.npoints > imageSize) {
			double [] xpoints = new double[imageSize];
			double [] ypoints = new double [imageSize];
			for (int i=0; i< imageSize; i++) {
				int j = i * ppolyline.npoints / imageSize;
				xpoints[i] = i;
				ypoints[i] = ppolyline.ypoints[j];
			}
			ppolyline = new Polyline2D (xpoints, ypoints, imageSize);
		}
		else { // imageSize > polyline.npoints
			double [] xpoints = new double[imageSize];
			double [] ypoints = new double [imageSize];
			for (int j=0; j< ppolyline.npoints; j++) {
				int i0 = j * imageSize / ppolyline.npoints;
				int i1 = (j +1) * imageSize / ppolyline.npoints;
				double y0 = ppolyline.ypoints[j];
				double y1 = y0;
				if ((j+1) < ppolyline.npoints)
					y1 = ppolyline.ypoints[j+1]; 
				for (int i = i0; i< i1; i++) {
					xpoints[i] = i;
					ypoints[i] = y0 + (y1-y0) * (i-i0)/(i1-i0);
				}
			}
			ppolyline = new Polyline2D (xpoints, ypoints, imageSize);
		}
	}


}
