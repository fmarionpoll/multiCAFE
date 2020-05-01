package plugins.fmp.multicafeSequence;


import java.awt.Color;
import java.awt.geom.Point2D;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;

import plugins.fmp.multicafeTools.EnumListType;
import plugins.fmp.multicafeTools.EnumXLSExportType;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.DetectLevels_Options;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;



public class Capillary implements XMLPersistent, Comparable <Capillary>  {

	public ROI2DShape 					roi 			= null;	// the capillary (source)
	public int							indexImage 		= -1;
	String								capillaryName 	= null;
	String 								version 		= null;
	public String						filenameTIFF	= null;
	public String 						stimulus		= new String("stimulus");
	public String 						concentration	= new String("xmM");
	public String						side			= ".";
	public int							nflies			= 1;
	public int							cagenb			= 0;
	public double 						volume 			= 5.;
	public int 							pixels 			= 5;
	public boolean						descriptionOK	= false;
	
	public DetectLevels_Options 		limitsOptions	= new DetectLevels_Options();
	public DetectGulps_Options 			gulpsOptions	= new DetectGulps_Options();
	
	public  final String 				ID_TOPLEVEL 	= "toplevel";	
	public  final String 				ID_BOTTOMLEVEL 	= "bottomlevel";	
	public  final String 				ID_DERIVATIVE 	= "derivative";	
	public CapillaryLimits				ptsTop  		= new CapillaryLimits(ID_TOPLEVEL, 0); 
	public CapillaryLimits				ptsBottom 		= new CapillaryLimits(ID_BOTTOMLEVEL, 0); 
	public CapillaryLimits				ptsDerivative 	= new CapillaryLimits(ID_DERIVATIVE, 0); 
	public CapillaryGulps 				gulpsRois 		= new CapillaryGulps(); 
	
	public List<ArrayList<int[]>> 		masksList 		= null;
	public List <double []> 			tabValuesList 	= null;
	public IcyBufferedImage 			bufImage 		= null;
	public boolean						valid			= true;


	private final String 				ID_META 		= "metaMC";
	private final String 				ID_ROI 			= "roiMC";
	private final String				ID_NFLIES		= "nflies";
	private final String				ID_CAGENB		= "cage_number";
	private final String 				ID_CAPVOLUME 	= "capillaryVolume";
	private final String 				ID_CAPPIXELS 	= "capillaryPixels";
	private final String 				ID_STIML 		= "stimulus";
	private final String 				ID_CONCL 		= "concentration";
	private final String 				ID_SIDE 		= "side";
	private final String 				ID_DESCOK 		= "descriptionOK";
	
	private final String 				ID_INDEXIMAGE 	= "indexImageMC";
	private final String 				ID_NAME 		= "nameMC";
	private final String 				ID_NAMETIFF 	= "filenameTIFF";
	private final String 				ID_VERSION		= "version"; 
	private final String 				ID_VERSIONNUM	= "1.0.0"; 
	
	// ----------------------------------------------------
	
	Capillary(ROI2DShape roi) {
		this.roi = roi;
		this.capillaryName = replace_LR_with_12(roi.getName());
	}
	
	Capillary(String name) {
		this.capillaryName = replace_LR_with_12(name);
	}
	
	public Capillary() {
	}

	public void copy(Capillary cap) {
		indexImage 		= cap.indexImage;
		capillaryName 	= cap.capillaryName;
		version 		= cap.version;
		roi 	= cap.roi;
		filenameTIFF	= cap.filenameTIFF;
		limitsOptions	= cap.limitsOptions;
		gulpsOptions	= cap.gulpsOptions;
		gulpsRois.rois	= new ArrayList <ROI2D> ();
		gulpsRois.rois.addAll(cap.gulpsRois.rois);
		ptsTop.copy(cap.ptsTop); 
		ptsBottom.copy(cap.ptsBottom); 
		ptsDerivative.copy(cap.ptsDerivative); 
	}
	
	public String getCapillaryName() {
		return capillaryName;
	}
	
	public void setCapillaryName(String name) {
		this.capillaryName = name;
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
	
	public int getCageFromRoiName() {
		String name = roi.getName();
		if (!name .contains("line"))
			return -1;
		return Integer.parseInt(name.substring(4, 5));
	}
	
	public String getSideDescriptor(EnumXLSExportType xlsExportOption) {
		String value = null;
		switch (xlsExportOption) {
		case DISTANCE:
		case ISALIVE:
			value = side + "(L=R)";
			break;
		case SUMGULPS_LR:
		case TOPLEVELDELTA_LR:
		case TOPLEVEL_LR:
			if (side.equals("L"))
				value = "sum";
			else
				value = "ratio";
			break;
		case XYIMAGE:
		case XYTOPCAGE:
			if (side .equals ("L"))
				value = "x";
			else
				value = "y";
			break;
		default:
			value = side;
			break;
		}
		return value;
	}
	
	public boolean isThereAnyMeasuresDone(EnumListType option) {
		boolean yes = false;
		switch (option) {
		case derivedValues:
			yes= (ptsDerivative != null && ptsDerivative.isThereAnyMeasuresDone());
			break;
		case cumSum:
			yes= (gulpsRois!= null && gulpsRois.isThereAnyMeasuresDone());
			break;
		case bottomLevel:
			yes= ptsBottom.isThereAnyMeasuresDone();
			break;
		case topLevel:
		default:
			yes= ptsTop.isThereAnyMeasuresDone();
			break;
		}
		return yes;
	}
	
	public List<Integer> getMeasures(EnumListType option) {
		List<Integer> datai = null;
		switch (option) {
		case derivedValues:
			if (ptsDerivative != null)
				datai = ptsDerivative.getMeasures();
			break;
		case cumSum:
			if (gulpsRois != null)
				datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNpoints());
			break;
		case bottomLevel:
			datai = ptsBottom.getMeasures();
			break;
		case topLevel:
		default:
			datai = ptsTop.getMeasures();
			break;
		}
		return datai;
	}
	
	public void cropMeasuresToNPoints (int npoints) {
		if (ptsTop.polylineLimit != null)
			ptsTop.cropToNPoints(npoints);
		if (ptsBottom.polylineLimit != null)
			ptsBottom.cropToNPoints(npoints);
		if (ptsDerivative.polylineLimit != null)
			ptsDerivative.cropToNPoints(npoints);
	}
	
	public void restoreCroppedMeasures () {
		if (ptsTop.polylineLimit != null)
			ptsTop.restoreNpoints();
		if (ptsBottom.polylineLimit != null)
			ptsBottom.restoreNpoints();
		if (ptsDerivative.polylineLimit != null)
			ptsDerivative.restoreNpoints();
	}
	
	public void cleanGulps(DetectGulps_Options options) {
		if (gulpsRois == null) {
			gulpsRois = new CapillaryGulps();
			gulpsRois.rois = new ArrayList <> ();
			return;
		}
		if (options.analyzePartOnly) 
			gulpsRois.removeROIsWithinInterval(options.startPixel, options.endPixel);
		else 
			gulpsRois.rois.clear();
	}
	
	public void getGulps(int indexkymo, DetectGulps_Options options) {
		int indexpixel = 0;
		int start = 1;
		int end = ptsTop.polylineLimit.npoints;
		if (options.analyzePartOnly) {
			start = options.startPixel;
			end = options.endPixel;
		} 
		
		ROI2DPolyLine roiTrack = new ROI2DPolyLine ();
		List<Point2D> gulpPoints = new ArrayList<>();
		for (indexpixel = start; indexpixel < end; indexpixel++) {
			int derivativevalue = (int) ptsDerivative.polylineLimit.ypoints[indexpixel-1];
			if (derivativevalue < options.detectGulpsThreshold)
				continue;
			
			if (gulpPoints.size() > 0) {
				Point2D prevPt = gulpPoints.get(gulpPoints.size() -1);
				if ((int) prevPt.getX() <  (indexpixel-1)) {
					roiTrack.setPoints(gulpPoints);
					gulpsRois.addGulp(roiTrack, indexkymo, getLast2ofCapillaryName()+"_gulp"+String.format("%07d", indexpixel));
					roiTrack = new ROI2DPolyLine ();
					gulpPoints = new ArrayList<>();
				}
			}
			if (gulpPoints.size() == 0)
				gulpPoints.add(new Point2D.Double (indexpixel-1, ptsTop.polylineLimit.ypoints[indexpixel-1]));
			Point2D.Double detectedPoint = new Point2D.Double (indexpixel, ptsTop.polylineLimit.ypoints[indexpixel]);
			gulpPoints.add(detectedPoint);
		}
		
		if (gulpPoints.size() > 1) {
			roiTrack.setPoints(gulpPoints);
			gulpsRois.addGulp(roiTrack, indexkymo, getLast2ofCapillaryName()+"_gulp"+String.format("%07d", indexpixel));
		}
		if (gulpPoints.size() == 1)
			System.out.print("only_1_point_detected");
	}
	
	public int getLastMeasure(EnumListType option) {
		int lastMeasure = 0;
		switch (option) {
		case derivedValues:
			if (ptsDerivative != null)
				lastMeasure = ptsDerivative.getLastMeasure();
			break;
		case cumSum:
			if (gulpsRois != null) {
				List<Integer> datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNpoints());
				lastMeasure = datai.get(datai.size()-1);
			}
			break;
		case bottomLevel:
			lastMeasure = ptsBottom.getLastMeasure();
			break;
		case topLevel:
		default:
			lastMeasure = ptsTop.getLastMeasure();
			break;
		}
		return lastMeasure;
	}
	
	public int getLastDeltaMeasure(EnumListType option) {
		int lastMeasure = 0;
		switch (option) {
		case derivedValues:
			if (ptsDerivative != null)
				lastMeasure = ptsDerivative.getLastDeltaMeasure();
			break;
		case cumSum:
			if (gulpsRois != null) {
				List<Integer> datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNpoints());
				lastMeasure = datai.get(datai.size()-1) - datai.get(datai.size()-2);
			}
			break;
		case bottomLevel:
			lastMeasure = ptsBottom.getLastDeltaMeasure();
			break;
		case topLevel:
		default:
			lastMeasure = ptsTop.getLastDeltaMeasure();
			break;
		}
		return lastMeasure;
	}
	
	public int getT0Measure(EnumListType option) {
		int t0Measure = 0;
		switch (option) {
		case derivedValues:
			if (ptsDerivative != null)
				t0Measure = ptsDerivative.getT0Measure();
			break;
		case cumSum:
			if (gulpsRois != null) {
				List<Integer> datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNpoints());
				t0Measure = datai.get(0);
			}
			break;
		case bottomLevel:
			t0Measure = ptsBottom.getT0Measure();
			break;
		case topLevel:
		default:
			t0Measure = ptsTop.getT0Measure();
			break;
		}
		return t0Measure;
	}
	
	public List<ROI2D> transferMeasuresToROIs() {
		List<ROI2D> listrois = new ArrayList<ROI2D> ();
		if (ptsTop != null)
			ptsTop.addToROIs(listrois, indexImage);
		if (ptsBottom != null)
			ptsBottom.addToROIs(listrois, indexImage);
		if (gulpsRois != null)
			gulpsRois.addToROIs(listrois, indexImage);
		if (ptsDerivative != null)
			ptsDerivative.addToROIs(listrois, Color.yellow, 1., indexImage);
		return listrois;
	}
	
	public void transferROIsToMeasures(List<ROI> listRois) {
		if (ptsTop != null)
			ptsTop.transferROIsToMeasures(listRois);
		if (ptsBottom != null)
			ptsBottom.transferROIsToMeasures(listRois);
		if (gulpsRois != null)
			gulpsRois.transferROIsToMeasures(listRois);
		if (ptsDerivative != null)
			ptsDerivative.transferROIsToMeasures(listRois);
	}

	// -------------------------------------------
	
	@Override
	public boolean loadFromXML(Node node) {
		boolean result = loadFromXML_CapillaryOnly(node);	
		String header = getLast2ofCapillaryName()+"_";
		result |= ptsDerivative.loadPolyline2DFromXML(node, ID_DERIVATIVE, header) > 0;
		result |= ptsTop.loadPolyline2DFromXML(node, ID_TOPLEVEL, header) > 0;
		result |= ptsBottom.loadPolyline2DFromXML(node, ID_BOTTOMLEVEL, header) > 0;
		result |= gulpsRois.loadFromXML(node);
		return result;
	}
	
	@Override
	public boolean saveToXML(Node node) {
		saveToXML_CapillaryOnly(node);
		if (ptsTop != null)
			ptsTop.savePolyline2DToXML(node, ID_TOPLEVEL);
		if (ptsBottom != null)
			ptsBottom.savePolyline2DToXML(node, ID_BOTTOMLEVEL);
		if (ptsDerivative != null)
			ptsDerivative.savePolyline2DToXML(node, ID_DERIVATIVE);
		if (gulpsRois != null)
			gulpsRois.saveToXML(node);
        return true;
	}
		
	boolean loadFromXML_CapillaryOnly(Node node) {
	    final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    boolean flag = (nodeMeta != null); 
	    if (flag) {
	    	version = XMLUtil.getElementValue(nodeMeta, ID_VERSION, ID_VERSIONNUM);
	    	indexImage = XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        capillaryName = XMLUtil.getElementValue(nodeMeta, ID_NAME, capillaryName);
	        filenameTIFF = XMLUtil.getElementValue(nodeMeta, ID_NAMETIFF, filenameTIFF);
	        
	        descriptionOK = XMLUtil.getElementBooleanValue(nodeMeta, ID_DESCOK, false);
	        nflies = XMLUtil.getElementIntValue(nodeMeta, ID_NFLIES, nflies);
	        cagenb = XMLUtil.getElementIntValue(nodeMeta, ID_CAGENB, cagenb);
	        volume 	= XMLUtil.getElementDoubleValue(nodeMeta, ID_CAPVOLUME, Double.NaN);
			pixels 	= XMLUtil.getElementIntValue(nodeMeta, ID_CAPPIXELS, 5);
			stimulus = XMLUtil.getElementValue(nodeMeta, ID_STIML, ID_STIML);
			concentration = XMLUtil.getElementValue(nodeMeta, ID_CONCL, ID_CONCL);
			side = XMLUtil.getElementValue(nodeMeta, ID_SIDE, ".");
			
	        roi = (ROI2DShape) loadFromXML_ROI(nodeMeta);
	        limitsOptions.loadFromXML(nodeMeta);
	        gulpsOptions.loadFromXML(nodeMeta);
	    }
	    return flag;
	}
	
	void saveToXML_CapillaryOnly(Node node) {
	    final Node nodeMeta = XMLUtil.setElement(node, ID_META);
	    if (nodeMeta != null) {
	    	if (version == null)
	    		version = ID_VERSIONNUM;
	    	XMLUtil.setElementValue(nodeMeta, ID_VERSION, version);
	        XMLUtil.setElementIntValue(nodeMeta, ID_INDEXIMAGE, indexImage);
	        XMLUtil.setElementValue(nodeMeta, ID_NAME, capillaryName);
	        if (filenameTIFF != null ) {
	        	String filename = Paths.get(filenameTIFF).getFileName().toString();
	        	XMLUtil.setElementValue(nodeMeta, ID_NAMETIFF, filename);
	        }
	        
	        XMLUtil.getElementBooleanValue(nodeMeta, ID_DESCOK, descriptionOK);
	        XMLUtil.setElementIntValue(nodeMeta, ID_NFLIES, nflies);
	        XMLUtil.setElementIntValue(nodeMeta, ID_CAGENB, cagenb);
			XMLUtil.setElementDoubleValue(nodeMeta, ID_CAPVOLUME, volume);
			XMLUtil.setElementIntValue(nodeMeta, ID_CAPPIXELS, pixels);
			XMLUtil.setElementValue(nodeMeta, ID_STIML, stimulus);
			XMLUtil.setElementValue(nodeMeta, ID_SIDE, side);
			XMLUtil.setElementValue(nodeMeta, ID_CONCL, concentration);

	        saveToXML_ROI(nodeMeta, roi); 
	    }
	}

	private void saveToXML_ROI(Node node, ROI roi) {
		final Node nodeROI = XMLUtil.setElement(node, ID_ROI);
        if (!roi.saveToXML(nodeROI)) {
            XMLUtil.removeNode(node, nodeROI);
            System.err.println("Error: the roi " + roi.getName() + " was not correctly saved to XML !");
        }
	}
 
	private ROI loadFromXML_ROI(Node node) {
		final Node nodeROI = XMLUtil.getElement(node, ID_ROI);
        if (nodeROI != null) {
			ROI roi = ROI.createFromXML(nodeROI);
	        return roi;
        }
        return null;
	}

	@Override
	public int compareTo(Capillary o) {
		int compareValue = this.capillaryName.compareTo(o.capillaryName);
		return compareValue;
	}

}
