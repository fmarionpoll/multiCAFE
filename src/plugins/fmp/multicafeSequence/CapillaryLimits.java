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

	public Polyline2D 	polyline = null;
	public String		name = null;
	public int			indexImage = 0;
	
	private final String ID_NPOINTS		= "npoints";
	private final String ID_N			= "n";
	private final String ID_X			= "x";
	private final String ID_Y			= "y";
	
	// -------------------------
	
	CapillaryLimits(String name, int indexImage) {
		this.name = name;
		this.indexImage = indexImage;
	}
	
	public int getNpoints() {
		return polyline.npoints;
	}
	
	public void copy(CapillaryLimits cap) {
		polyline = (Polyline2D) cap.polyline.clone(); 
	}
	
	public boolean isThereAnyMeasuresDone() {
		boolean yes = (polyline != null && polyline.npoints > 0);
		return yes;
	}
	
	public List<Integer> getMeasures() {
		List<Integer> datai = getIntegerArrayFromPolyline2D();
		return datai;
	}
	
	public List<ROI> transferMeasuresToROIs(List<ROI> listrois) {
		if (polyline != null) 
			listrois.add(transferPolyline2DToROI());
		return listrois;
	}
	
	public List<ROI> transferMeasuresToROIs(List<ROI> listrois, Color color, double stroke) {
		if (polyline != null) { 
			ROI2D derivativeRoi = transferPolyline2DToROI();
			derivativeRoi.setColor(color);
			derivativeRoi.setStroke(stroke);
			listrois.add(derivativeRoi);
		}
		return listrois;
	}
	
	public void transferROIsToMeasures(List<ROI> listRois) {	
		for (ROI roi: listRois) {		
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine ) {
				if  (roiname .contains (name)) {
					polyline = ((ROI2DPolyLine)roi).getPolyline2D();
					((ROI2DPolyLine) roi).setT(indexImage);
				}
			}
		}
	}
	
	@Override
	public boolean loadFromXML(Node node) {
		loadPolyline2DFromXML(node, name);
		return false;
	}

	@Override
	public boolean saveToXML(Node node) {
		savePolyline2DToXML(node);
		return false;
	}
	
	public List<Integer> getIntegerArrayFromPolyline2D() {
		if (polyline == null)
			return null;
		double [] array = polyline.ypoints;
		List<Integer> arrayInt = new ArrayList<Integer>(array.length);
		for (int i=0; i< array.length; i++)
			arrayInt.add((int) array[i]);
		return arrayInt;
	}
	
	public ROI2D transferPolyline2DToROI() {
		if (polyline == null)
			return null;
		
		ROI2D roi = new ROI2DPolyLine(polyline); 
		roi.setName(name);
		return roi;
	}
	
	int loadPolyline2DFromXML(Node node, String name) {
		final Node nodeMeta = XMLUtil.getElement(node, name);
		int npoints = 0;
		polyline = null;
	    if (nodeMeta != null) {
	    	int npoints1 = XMLUtil.getElementIntValue(nodeMeta, ID_NPOINTS, 0);
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
	    	polyline = new Polyline2D(xpoints, ypoints, npoints1);
	    }
	    if (polyline != null)
	    	npoints = polyline.npoints;
	    return npoints;
	}
	
	void savePolyline2DToXML(Node node) {
		if (polyline == null)
			return;
		
		final Node nodeMeta = XMLUtil.setElement(node, name);
	    if (nodeMeta != null) {
	    	XMLUtil.setElementIntValue(nodeMeta, ID_NPOINTS, polyline.npoints);
	    	for (int i=0; i< polyline.npoints; i++) {
	    		Element elmt = XMLUtil.setElement(nodeMeta, ID_N+i);
	    		if (i==0)
	    			XMLUtil.setAttributeDoubleValue(elmt, ID_X, polyline.xpoints[i]);
	    		XMLUtil.setAttributeDoubleValue(elmt, ID_Y, polyline.ypoints[i]);
	    	}
	    }
	}
	
	

}
