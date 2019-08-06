package plugins.fmp.multicafeSequence;


import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafeTools.EnumArrayListType;
import plugins.fmp.multicafe.MCBuildDetect_GulpsOptions;
import plugins.fmp.multicafe.MCBuildDetect_LimitsOptions;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Capillary implements XMLPersistent  {

	public int							indexImage 				= -1;
	public String						name 					= null;
	public String 						version 				= null;
	public ROI2DShape 					roi 					= null;	// the capillary (source)
	public MCBuildDetect_LimitsOptions 	limitsOptions			= new MCBuildDetect_LimitsOptions();
	public MCBuildDetect_GulpsOptions 	gulpsOptions			= new MCBuildDetect_GulpsOptions();
	
	public List<Point2D> 				ptsTop  				= null; 
	public List<Point2D> 				ptsBottom 				= null; 
	public Collection<ROI> 				gulpsRois 				= null; 
	public ArrayList<Integer> 			derivedValuesArrayList 	= null; 
	
	public ArrayList<ArrayList<int[]>> masksList = null;
	public ArrayList <double []> tabValuesList = null;
	public IcyBufferedImage bufImage = null;
	
	private final static String ID_META = "metaMC";
	private final static String ID_ROI = "roiMC";
	private final static String ID_GULPS = "gulpsMC";
	private final static String ID_INDEXIMAGE = "indexImageMC";
	private final static String ID_NAME = "nameMC";
	    
	// ----------------------------------------------------
	
	Capillary(ROI2DShape roi) {
		this.roi = roi;
		this.name = roi.getName();
	}
	
	Capillary(String name) {
		this.name = name;
	}
	
	public Capillary() {
	}

	public String getName() {
		return name;
	}
	
	public ArrayList<Integer> getYFromPtArray(List<Point2D> ptsList) {
		if (ptsList == null)
			return null;
		
		ArrayList<Integer> arrayInt = new ArrayList<Integer> ();
		for (Point2D pt: ptsList) {
			int value = (int) pt.getY();
			arrayInt.add(value);
		}
		return arrayInt;
	}
	
	public ArrayList<Integer> getArrayListFromRois(EnumArrayListType option) {
		ArrayList<Integer> datai = null;
		
		switch (option) {
		case derivedValues:
			datai = derivedValuesArrayList;
			break;
		case cumSum:
			datai = getCumSumFromRoisArray(gulpsRois);
			break;
		case bottomLevel:
			datai = getYFromPtArray(ptsBottom);
			break;
		case topLevel:
		default:
			datai = getYFromPtArray(ptsTop);
			break;
		}
		return datai;
	}
	
	public ArrayList<Integer> getCumSumFromRoisArray(Collection<ROI> gulpsRois) {
		if (gulpsRois == null)
			return null;
		ArrayList<Integer> arrayInt = new ArrayList<Integer> (Collections.nCopies(ptsTop.size(), 0));
		for (ROI roi: gulpsRois) {
			addRoitoCumulatedSumArray((ROI2DPolyLine) roi, arrayInt);
		}
		return arrayInt;
	}
	
	private void addRoitoCumulatedSumArray(ROI2DPolyLine roi, ArrayList<Integer> sumArrayList) {
		
		interpolateMissingPointsAlongXAxis (roi);
		ArrayList<Integer> intArray = transfertRoiYValuesToDataArray(roi);
		Polyline2D line = roi.getPolyline2D();
		int jstart = (int) line.xpoints[0];

		int previousY = intArray.get(0);
		for (int i=0; i< intArray.size(); i++) {
			int val = intArray.get(i);
			int deltaY = val - previousY;
			previousY = val;
			for (int j = jstart+i; j< sumArrayList.size(); j++) {
				sumArrayList.set(j, sumArrayList.get(j) +deltaY);
			}
		}
	}
	
	private boolean interpolateMissingPointsAlongXAxis (ROI2DPolyLine roiLine) {
		// interpolate points so that each x step has a value	
		// assume that points are ordered along x
	
		Polyline2D line = roiLine.getPolyline2D();
		int roiLine_npoints = line.npoints;
		// exit if the length of the segment is the same
		int roiLine_nintervals =(int) line.xpoints[roiLine_npoints-1] - (int) line.xpoints[0] +1;  
		
		if (roiLine_npoints == roiLine_nintervals)
			return true;
		else if (roiLine_npoints > roiLine_nintervals)
			return false;
		
		List<Point2D> pts = new ArrayList <Point2D>(roiLine_npoints);
		double ylast = line.ypoints[roiLine_npoints-1];
		for (int i=1; i< roiLine_npoints; i++) {
			
			int xfirst = (int) line.xpoints[i-1];
			int xlast = (int) line.xpoints[i];
			double yfirst = line.ypoints[i-1];
			ylast = line.ypoints[i];
			for (int j = xfirst; j< xlast; j++) {
				
				int val = (int) (yfirst + (ylast-yfirst)*(j-xfirst)/(xlast-xfirst));
				Point2D pt = new Point2D.Double(j, val);
				pts.add(pt);
			}
		}
		Point2D pt = new Point2D.Double(line.xpoints[roiLine_npoints-1], ylast);
		pts.add(pt);
		
		roiLine.setPoints(pts);
		return true;
	}
	
	private ArrayList<Integer> transfertRoiYValuesToDataArray(ROI2DPolyLine roiLine) {

		Polyline2D line = roiLine.getPolyline2D();
		ArrayList<Integer> intArray = new ArrayList<Integer> (line.npoints);
		for (int i=0; i< line.npoints; i++) 
			intArray.add((int) line.ypoints[i]);

		return intArray;
	}

	@Override
	public boolean loadFromXML(Node node) {
		boolean result = true;
		result |= loadMetaDataFromXML(node);
		result |= loadROIsFromXML(node, gulpsRois);
		result |= loadIntegerArrayFromXML(node, "derivedvalues", derivedValuesArrayList);
		ArrayList<Integer> data = new ArrayList<Integer>();
		boolean flag = loadIntegerArrayFromXML(node, "topLevel", data);
		result |= flag;
		if (flag)
			convertIntegerArrayToPointArray(data, ptsTop);
		flag = loadIntegerArrayFromXML(node, "bottomLevel", data);
		if (flag)
			convertIntegerArrayToPointArray(data, ptsBottom);
		result |= flag;
		return result;
	}

	@Override
	public boolean saveToXML(Node node) {

		saveMetaDataToXML(node);
		if (gulpsRois != null)
			saveROIsToXML(node, gulpsRois);
		if (derivedValuesArrayList != null)
			saveIntArraytoXML(node, derivedValuesArrayList, "derivedvalues");
		if (ptsTop != null)
			saveIntArraytoXML(node, getYFromPtArray(ptsTop), "topLevel");
		if (ptsBottom != null)
			saveIntArraytoXML(node, getYFromPtArray(ptsBottom), "bottomLevel");
        
        return true;
	}
	
	private boolean loadMetaDataFromXML(Node node)
	{
	    final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    if (nodeMeta == null)	// nothing to load
            return true;
	    
	    if (nodeMeta != null)
	    {
	    	version = XMLUtil.getElementValue(nodeMeta, "capillary__", "version 1.0.0");
	        
	    	indexImage = XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        name = XMLUtil.getElementValue(nodeMeta, ID_NAME, name);
	        roi = (ROI2DShape) loadSingleROIFromXML(nodeMeta);
	        limitsOptions.loadFromXML(nodeMeta);
	        gulpsOptions.loadFromXML(nodeMeta);
	    }
	    return true;
	}
	
	private void saveMetaDataToXML(Node node)
	{
	    final Node nodeMeta = XMLUtil.setElement(node, ID_META);
	    if (nodeMeta != null)
	    {
	    	if (version == null)
	    		version = "version 1.0.0";
	    	XMLUtil.setElementValue(nodeMeta, "capillary__", version);
	        XMLUtil.setElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        XMLUtil.setElementValue(nodeMeta, ID_NAME, name);
	        saveROIToXML(nodeMeta, roi); 
	        limitsOptions.saveToXML(nodeMeta);
	        gulpsOptions.saveToXML(nodeMeta);
	    }
	}

	private void saveROIToXML(Node node, ROI roi) {
		final Node nodeROI = XMLUtil.addElement(node, ID_ROI);
        if (!roi.saveToXML(nodeROI))
        {
            XMLUtil.removeNode(node, nodeROI);
            System.err.println("Error: the roi " + roi.getName() + " was not correctly saved to XML !");
        }
	}
 
	private ROI loadSingleROIFromXML(Node node) {
		final Node nodeROI = XMLUtil.getElement(node, ID_ROI);
        if (nodeROI != null) {
			ROI roi = ROI.createFromXML(nodeROI);
	        return roi;
        }
        return null;
	}
	
	private void saveROIsToXML(Node node, Collection<ROI> rois)
	{
        final Node nodeROIs = XMLUtil.setElement(node, ID_GULPS);
        if (nodeROIs != null)
        {
            XMLUtil.removeAllChildren(nodeROIs);
	        ROI.saveROIsToXML(nodeROIs, (List<ROI>) rois);
	    }
	}
	
	private boolean loadROIsFromXML(Node node, Collection<ROI> rois)
	{
        final Node nodeROIs = XMLUtil.getElement(node, ID_GULPS);
        if (nodeROIs != null)
        {
        	rois = ROI.loadROIsFromXML(nodeROIs);
	    }
        return true;
	}
	
	private void saveIntArraytoXML(Node node, ArrayList <Integer> data, String name) {
		final Node nodeMeta = XMLUtil.setElement(node, name);
	    if (nodeMeta != null) {
	    	int i= 0;
	    	XMLUtil.setElementIntValue(nodeMeta, "nitems", data.size());
	    	for (int value: data) {
	    		XMLUtil.setElementIntValue(nodeMeta, "point"+i, value);
	    		i++;
	    	}
	    }
	}
	
	private boolean loadIntegerArrayFromXML(Node node, String name, ArrayList <Integer> data) {
		final Node nodeMeta = XMLUtil.getElement(node, name);
	    if (nodeMeta != null) {
	    	int nitems = XMLUtil.getElementIntValue(nodeMeta, "nitems", data.size());
	    	data = new ArrayList<Integer>(nitems);
	    	for (int i=0; i< nitems; i++) {
	    		int value = XMLUtil.getElementIntValue(nodeMeta, "point"+i, 0);
	    		data.set(i, value);
	    	}
	    }
	    return true;
	}
	
	private void convertIntegerArrayToPointArray(ArrayList<Integer> data, List<Point2D> ptsList) {
		ptsList = new ArrayList<Point2D>();
		for (int i=0; i < data.size(); i++) {
			Point2D pt = new Point2D.Double((double) i, (double) data.get(i));
			ptsList.add(pt);
		}
	}
	
	public int getCapillaryIndexFromCapillaryName(String name) {
		if (!name .contains("line"))
			return -1;

		String num = name.substring(4, 5);
		int numFromName = Integer.parseInt(num);
		String side = name.substring(5, 6);
		if (side != null) {
			if (side .equals("R")) {
				numFromName = numFromName* 2;
				numFromName += 1;
			}
			else if (side .equals("L"))
				numFromName = numFromName* 2;
		}
		return numFromName;
	}

}
