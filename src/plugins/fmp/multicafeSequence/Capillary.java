package plugins.fmp.multicafeSequence;


import java.awt.Color;
import java.awt.geom.Point2D;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;

import plugins.fmp.multicafeTools.EnumListType;
import plugins.fmp.multicafeTools.ROI2DUtilities;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.DetectLimits_Options;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Capillary implements XMLPersistent  {

	public int							indexImage 				= -1;
	private String						name 					= null;
	public String 						version 				= null;
	public ROI2DShape 					roi 					= null;	// the capillary (source)
	public String						filenameTIFF			= null;
	
	public DetectLimits_Options 		limitsOptions			= new DetectLimits_Options();
	public DetectGulps_Options 			gulpsOptions			= new DetectGulps_Options();
	
	public Polyline2D 					ptsTop  				= null; 
	public Polyline2D 					ptsBottom 				= null; 
	public Polyline2D 					ptsDerivative 			= null; 
	public Collection<ROI> 				gulpsRois 				= null; 
	
	public List<ArrayList<int[]>> 		masksList 				= null;
	public List <double []> 			tabValuesList 			= null;
	public IcyBufferedImage 			bufImage 				= null;
	
	private final static String ID_META 		= "metaMC";
	private final static String ID_ROI 			= "roiMC";
	private final static String ID_GULPS 		= "gulpsMC";
	private final static String ID_INDEXIMAGE 	= "indexImageMC";
	private final static String ID_NAME 		= "nameMC";
	private final static String ID_NAMETIFF 	= "filenameTIFF";
	private final static String ID_TOPLEVEL 	= "toplevel";	
	private final static String ID_BOTTOMLEVEL 	= "bottomlevel";	
	private final static String ID_DERIVATIVE 	= "derivedvalues";	
	private final static String ID_VERSION		= "version"; 
	private final static String ID_VERSIONNUM	= "1.0.0"; 
	private final static String ID_NPOINTS		= "npoints";
	private final static String ID_X			= "x";
	private final static String ID_Y			= "y";
	    
	// ----------------------------------------------------
	
	Capillary(ROI2DShape roi) {
		this.roi = roi;
		this.name = replace_LR_with_12(roi.getName());
	}
	
	Capillary(String name) {
		this.name = replace_LR_with_12(name);
	}
	
	public Capillary() {
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getLast2ofCapillaryName() {
		return roi.getName().substring(roi.getName().length() -2);
	}
	
	public String replace_LR_with_12(String name) {
		String newname = null;
		if (name .endsWith("R"))
			newname = name.replace("R",  "2");
		else if (name.endsWith("L"))
			newname = name.replace("L", "1");
		else 
			newname = name;
		return newname;
	}
	
	public List<Integer> getIntegerArrayFromPointArray(List<Point2D> ptsList) {
		if (ptsList == null)
			return null;
		List<Integer> arrayInt = new ArrayList<Integer> ();
		for (Point2D pt: ptsList) {
			int value = (int) pt.getY();
			arrayInt.add(value);
		}
		return arrayInt;
	}
	
	public List<Integer> getIntegerArrayFromPolyline2D(Polyline2D ptsList) {
		if (ptsList == null)
			return null;
		double [] array = ptsList.ypoints;
		List<Integer> arrayInt = new ArrayList<Integer>(array.length);
		for (int i=0; i< array.length; i++)
			arrayInt.add((int) array[i]);
		return arrayInt;
	}
	
	public List<Point2D> getPointArrayFromIntegerArray(List<Integer> data) {
		if (data == null)
			return null;
		List<Point2D> ptsList = null;
		if (data.size() > 0) {
			ptsList = new ArrayList<Point2D>(data.size());
			for (int i=0; i < data.size(); i++) {
				Point2D pt = new Point2D.Double((double) i, (double) data.get(i));
				ptsList.add(pt);
			}
		}
		return ptsList;
	}
	
	public boolean isThereAnyMeasuresDone(EnumListType option) {
		boolean yes = false;
		switch (option) {
		case derivedValues:
			yes= (ptsDerivative != null && ptsDerivative.npoints > 0);
			break;
		case cumSum:
			yes= (gulpsRois != null && gulpsRois.size() > 0);
			break;
		case bottomLevel:
			yes= (ptsBottom != null && ptsBottom.npoints > 0);
			break;
		case topLevel:
		default:
			yes= (ptsTop != null && ptsTop.npoints > 0);
			break;
		}
		return yes;
	}
	
	public List<Integer> getMeasures(EnumListType option) {
		List<Integer> datai = null;
		switch (option) {
		case derivedValues:
			datai = getIntegerArrayFromPolyline2D(ptsDerivative);
			break;
		case cumSum:
			datai = getCumSumFromRoisArray(gulpsRois);
			break;
		case bottomLevel:
			datai = getIntegerArrayFromPolyline2D(ptsBottom);
			break;
		case topLevel:
		default:
			datai = getIntegerArrayFromPolyline2D(ptsTop);
			break;
		}
		return datai;
	}
	
	public List<ROI> transferMeasuresToROIs() {
		List<ROI> listrois = new ArrayList<ROI> ();
		if (ptsTop != null) 
			listrois.add(transferPolyline2DToROI(ID_TOPLEVEL, ptsTop));
		if (ptsBottom != null) 
			listrois.add(transferPolyline2DToROI(ID_BOTTOMLEVEL, ptsBottom));
		if (gulpsRois != null)	
			listrois.addAll(gulpsRois);
		if (ptsDerivative != null) {
			ROI2D derivativeRoi = transferPolyline2DToROI(ID_DERIVATIVE, ptsDerivative);
			derivativeRoi.setColor(Color.yellow);
			derivativeRoi.setStroke(1.);
			listrois.add(derivativeRoi);
		}
		return listrois;
	}
	
	public void transferROIsToMeasures(List<ROI> listRois) {	
		gulpsRois.clear();
		for (ROI roi: listRois) {		
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine ) {
				if (roiname .contains("gulp"))	
					gulpsRois.add(roi);
				else if  (roiname .contains ("toplevel"))
					ptsTop = ((ROI2DPolyLine)roi).getPolyline2D();
				else if (roiname .contains("bottomlevel"))
					ptsBottom = ((ROI2DPolyLine)roi).getPolyline2D();
				else if (roiname .contains("derivative") )
					ptsDerivative = ((ROI2DPolyLine)roi).getPolyline2D();
			}
		}
	}
	
	// ---------------------
	
	private ROI2D transferPolyline2DToROI(String name, Polyline2D polyline) {
		ROI2D roi = new ROI2DPolyLine(polyline); 
		if (indexImage >= 0) {
			roi.setT(indexImage);
			roi.setName(getLast2ofCapillaryName()+"_"+name);
		}
		else
			roi.setName(name);
		return roi;
	}
	
	private List<Integer> getCumSumFromRoisArray(Collection<ROI> gulpsRois) {
	
		if (gulpsRois == null)
			return null;
		List<Integer> arrayInt = new ArrayList<Integer> (Collections.nCopies(ptsTop.npoints, 0));
		for (ROI roi: gulpsRois) {
			ROI2DUtilities.addRoitoCumulatedSumArray((ROI2DPolyLine) roi, arrayInt);
		}
		return arrayInt;
	}
	
	// ---------------------
	
	@Override
	public boolean loadFromXML(Node node) {
		boolean result = true;
		result |= loadMetaDataFromXML(node);		
		result |= (ptsDerivative = loadPolyline2DFromXML(node, ID_DERIVATIVE)) != null;
		result |= (ptsTop = loadPolyline2DFromXML(node, ID_TOPLEVEL)) != null;
		result |= (ptsBottom = loadPolyline2DFromXML(node, ID_BOTTOMLEVEL))!= null;
		result |= (gulpsRois = loadROIsFromXML(node)) != null;
		return result;
	}

	@Override
	public boolean saveToXML(Node node) {
		saveMetaDataToXML(node);
		if (ptsDerivative != null)
			savePolyline2DToXML(node, ID_DERIVATIVE, ptsDerivative);
		if (ptsTop != null)
			savePolyline2DToXML(node, ID_TOPLEVEL, ptsTop);
		if (ptsBottom != null)
			savePolyline2DToXML(node, ID_BOTTOMLEVEL, ptsBottom);
		if (gulpsRois != null)
			saveROIsToXML(node, gulpsRois);
        return true;
	}
	
	private boolean loadMetaDataFromXML(Node node) {
	    final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    if (nodeMeta == null)	// nothing to load
            return true;
	    if (nodeMeta != null) {
	    	version = XMLUtil.getElementValue(nodeMeta, ID_VERSION, ID_VERSIONNUM);
	    	indexImage = XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        name = XMLUtil.getElementValue(nodeMeta, ID_NAME, name);
	        filenameTIFF = XMLUtil.getElementValue(nodeMeta, ID_NAMETIFF, filenameTIFF);
	        roi = (ROI2DShape) loadROIFromXML(nodeMeta);
	        limitsOptions.loadFromXML(nodeMeta);
	        gulpsOptions.loadFromXML(nodeMeta);
	    }
	    return true;
	}
	
	private void saveMetaDataToXML(Node node) {
	    final Node nodeMeta = XMLUtil.setElement(node, ID_META);
	    if (nodeMeta != null) {
	    	if (version == null)
	    		version = ID_VERSIONNUM;
	    	XMLUtil.setElementValue(nodeMeta, ID_VERSION, version);
	        XMLUtil.setElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        XMLUtil.setElementValue(nodeMeta, ID_NAME, name);
	        XMLUtil.setElementValue(nodeMeta, ID_NAMETIFF, Paths.get(filenameTIFF).getFileName().toString());
	        saveROIToXML(nodeMeta, roi); 
	        limitsOptions.saveToXML(nodeMeta);
	        gulpsOptions.saveToXML(nodeMeta);
	    }
	}

	private void saveROIToXML(Node node, ROI roi) {
		final Node nodeROI = XMLUtil.addElement(node, ID_ROI);
        if (!roi.saveToXML(nodeROI)) {
            XMLUtil.removeNode(node, nodeROI);
            System.err.println("Error: the roi " + roi.getName() + " was not correctly saved to XML !");
        }
	}
 
	private ROI loadROIFromXML(Node node) {
		final Node nodeROI = XMLUtil.getElement(node, ID_ROI);
        if (nodeROI != null) {
			ROI roi = ROI.createFromXML(nodeROI);
	        return roi;
        }
        return null;
	}
	
	private void saveROIsToXML(Node node, Collection<ROI> rois) {
        final Node nodeROIs = XMLUtil.setElement(node, ID_GULPS);
        if (nodeROIs != null){
	        ROI.saveROIsToXML(nodeROIs, (List<ROI>) rois);
	    }
	}
	
	private List <ROI> loadROIsFromXML(Node node) {
        final Node nodeROIs = XMLUtil.getElement(node, ID_GULPS);
        List <ROI> rois = new ArrayList <ROI> ();
        if (nodeROIs != null) {
        	rois = ROI.loadROIsFromXML(nodeROIs);
	    }
        return rois;
	}
	
	private void savePolyline2DToXML(Node node, String name, Polyline2D data) {
		final Node nodeMeta = XMLUtil.setElement(node, name);
	    if (nodeMeta != null) {
	    	XMLUtil.setElementIntValue(nodeMeta, ID_NPOINTS, data.npoints);
	    	for (int i=0; i< data.npoints; i++) {
	    		XMLUtil.setElementDoubleValue(nodeMeta, ID_X+i, data.xpoints[i]);
	    		XMLUtil.setElementDoubleValue(nodeMeta, ID_Y+i, data.ypoints[i]);
	    	}
	    }
	}
	
	private Polyline2D loadPolyline2DFromXML(Node node, String name) {
		final Node nodeMeta = XMLUtil.getElement(node, name);
		Polyline2D data = null;
	    if (nodeMeta != null) {
	    	int npoints = XMLUtil.getElementIntValue(nodeMeta, ID_NPOINTS, 0);
	    	double[] xpoints = new double [npoints];
	    	double[] ypoints = new double [npoints];
	    	for (int i=0; i< npoints; i++) {
	    		xpoints[i] = XMLUtil.getElementDoubleValue(nodeMeta, ID_X+i, 0);
	    		ypoints[i] = XMLUtil.getElementDoubleValue(nodeMeta, ID_Y+i, 0);
    		}
	    	data = new Polyline2D(xpoints, ypoints, npoints);
	    }
	    return data;
	}
}
