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
import icy.roi.ROI2D;
import icy.util.XMLUtil;

import plugins.fmp.multicafeTools.EnumArrayListType;
import plugins.fmp.multicafeTools.ROI2DUtilities;
import plugins.fmp.multicafeTools.BuildDetect_GulpsOptions;
import plugins.fmp.multicafeTools.BuildDetect_LimitsOptions;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Capillary implements XMLPersistent  {

	public int							indexImage 				= -1;
	public String						name 					= null;
	public String 						version 				= null;
	public ROI2DShape 					roi 					= null;	// the capillary (source)
	public BuildDetect_LimitsOptions 	limitsOptions			= new BuildDetect_LimitsOptions();
	public BuildDetect_GulpsOptions 	gulpsOptions			= new BuildDetect_GulpsOptions();
	
	public List<Point2D> 				ptsTop  				= null; 
	public List<Point2D> 				ptsBottom 				= null; 
	public Collection<ROI> 				gulpsRois 				= null; 
	public ArrayList<Integer> 			derivedValuesArrayList 	= null; 
	
	public ArrayList<ArrayList<int[]>> 	masksList 				= null;
	public ArrayList <double []> 		tabValuesList 			= null;
	public IcyBufferedImage 			bufImage 				= null;
	
	private final static String ID_META 		= "metaMC";
	private final static String ID_ROI 			= "roiMC";
	private final static String ID_GULPS 		= "gulpsMC";
	private final static String ID_INDEXIMAGE 	= "indexImageMC";
	private final static String ID_NAME 		= "nameMC";
	    
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
	
	public int getCapillaryIndexFromCapillaryName(String name) {
		if (!name .contains("line"))
			return -1;
		String num = name.substring(4, 5);
		int numFromName = Integer.parseInt(num);
		String side = name.substring(5, 6);
		if (side != null) {
			if (side .equals("R") || side .equals("2")) {
				numFromName = numFromName* 2;
				numFromName += 1;
			}
			else if (side .equals("L") || side .equals("1"))
				numFromName = numFromName* 2;
		}
		return numFromName;
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
	
	public ArrayList<Integer> getIntegerArrayFromPointArray(List<Point2D> ptsList) {
		if (ptsList == null)
			return null;
		ArrayList<Integer> arrayInt = new ArrayList<Integer> ();
		for (Point2D pt: ptsList) {
			int value = (int) pt.getY();
			arrayInt.add(value);
		}
		return arrayInt;
	}
	
	public List<Point2D> getPointArrayFromIntegerArray(ArrayList<Integer> data) {
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
	
	public boolean isThereAnyMeasuresDone(EnumArrayListType option) {
		boolean yes = false;
		switch (option) {
		case derivedValues:
			yes= (derivedValuesArrayList != null && derivedValuesArrayList.size() > 0);
			break;
		case cumSum:
			yes= (gulpsRois != null && gulpsRois.size() > 0);
			break;
		case bottomLevel:
			yes= (ptsBottom != null && ptsBottom.size() > 0);
			break;
		case topLevel:
		default:
			yes= (ptsTop != null && ptsTop.size() > 0);
			break;
		}
		return yes;
	}
	
	public ArrayList<Integer> getMeasures(EnumArrayListType option) {
		ArrayList<Integer> datai = null;
		switch (option) {
		case derivedValues:
			datai = derivedValuesArrayList;
			break;
		case cumSum:
			datai = getCumSumFromRoisArray(gulpsRois);
			break;
		case bottomLevel:
			datai = getIntegerArrayFromPointArray(ptsBottom);
			break;
		case topLevel:
		default:
			datai = getIntegerArrayFromPointArray(ptsTop);
			break;
		}
		return datai;
	}
	
	public List<ROI> getROIsFromMeasures() {
		List<ROI> listrois = new ArrayList<ROI> ();
		listrois.add(getPtListToROI(ptsTop));
		listrois.add(getPtListToROI(ptsBottom));
		listrois.addAll(gulpsRois);
		return listrois;
	}
	
	// ---------------------
	
	private ROI2D getPtListToROI(List<Point2D> ptslist) {
		ROI2D topline = ROI2DUtilities.transferPointArrayToRoi(ptslist);
		topline.setT(indexImage);
		return topline;
	}

	private ArrayList<Integer> getCumSumFromRoisArray(Collection<ROI> gulpsRois) {
	
		if (gulpsRois == null)
			return null;
		ArrayList<Integer> arrayInt = new ArrayList<Integer> (Collections.nCopies(ptsTop.size(), 0));
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
		derivedValuesArrayList = loadIntegerArrayFromXML(node, "derivedvalues");
		result |= (derivedValuesArrayList != null);
		ArrayList<Integer> data = loadIntegerArrayFromXML(node, "topLevel");
		if (data != null)
			ptsTop = getPointArrayFromIntegerArray(data);
		else 
			result = false;
		data = loadIntegerArrayFromXML(node, "bottomLevel");
		if (data != null)
			ptsBottom = getPointArrayFromIntegerArray(data);
		else
			result = false;
		result |= loadROIsFromXML(node, gulpsRois);
		return result;
	}

	@Override
	public boolean saveToXML(Node node) {
		saveMetaDataToXML(node);
		if (derivedValuesArrayList != null)
			saveIntArraytoXML(node, "derivedvalues", derivedValuesArrayList);
		if (ptsTop != null)
			saveIntArraytoXML(node, "topLevel", getIntegerArrayFromPointArray(ptsTop));
		if (ptsBottom != null)
			saveIntArraytoXML(node, "bottomLevel", getIntegerArrayFromPointArray(ptsBottom));
		if (gulpsRois != null)
			saveROIsToXML(node, gulpsRois);
        return true;
	}
	
	// ---------------------
	
	private boolean loadMetaDataFromXML(Node node) {
	    final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    if (nodeMeta == null)	// nothing to load
            return true;
	    if (nodeMeta != null) {
	    	version = XMLUtil.getElementValue(nodeMeta, "capillary_", "version 1.0.0");
	        
	    	indexImage = XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        name = XMLUtil.getElementValue(nodeMeta, ID_NAME, name);
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
	    		version = "version 1.0.0";
	    	XMLUtil.setElementValue(nodeMeta, "capillary_", version);
	        XMLUtil.setElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        XMLUtil.setElementValue(nodeMeta, ID_NAME, name);
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
	
	private boolean loadROIsFromXML(Node node, Collection<ROI> rois) {
        final Node nodeROIs = XMLUtil.getElement(node, ID_GULPS);
        if (nodeROIs != null) {
        	rois = ROI.loadROIsFromXML(nodeROIs);
	    }
        return true;
	}
	
	private void saveIntArraytoXML(Node node, String name, ArrayList <Integer> data) {
		final Node nodeMeta = XMLUtil.setElement(node, name);
	    if (nodeMeta != null) {
	    	int i= 0;
	    	XMLUtil.setElementIntValue(nodeMeta, "nitems", data.size());
	    	for (int value: data) {
	    		XMLUtil.setElementIntValue(nodeMeta, "i"+i, value);
	    		i++;
	    	}
	    }
	}
	
	private ArrayList <Integer> loadIntegerArrayFromXML(Node node, String name) {
		final Node nodeMeta = XMLUtil.getElement(node, name);
		ArrayList <Integer> data = null;
	    if (nodeMeta != null) {
	    	int nitems = XMLUtil.getElementIntValue(nodeMeta, "nitems", 0);
	    	data = new ArrayList<Integer>(nitems);
	    	for (int i=0; i< nitems; i++) {
    			int value = XMLUtil.getElementIntValue(nodeMeta, "i"+i, 0);
	    		data.add(i, value);
    		}
	    }
	    return data;
	}

	
}
