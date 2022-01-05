package plugins.fmp.multicafe2.experiment;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;

import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

import plugins.fmp.multicafe2.series.Options_BuildSeries;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;




public class Capillary implements XMLPersistent, Comparable <Capillary>  
{

	private ROI2D 						roi 			= null;
	private ArrayList<KymoROI2D>		roisForKymo 	= new ArrayList<KymoROI2D>();
	
	private String						kymographName 	= null;
	
	public int							indexKymograph 	= -1;
	public String 						version 		= null;
	public String						filenameTIFF	= null;
	
	public ArrayList<int[]> 			cap_Integer		= null;
	
	public String 						capStimulus		= new String("..");
	public String 						capConcentration= new String("..");
	public String						capSide			= ".";
	public int							capNFlies		= 1;
	public int							capCageID		= 0;
	public double 						capVolume 		= 5.;
	public int 							capPixels 		= 5;
	public boolean						descriptionOK	= false;
	public int							versionInfos	= 0;
	
	public Options_BuildSeries 			limitsOptions	= new Options_BuildSeries();
	
	public  final String 				ID_TOPLEVEL 	= "toplevel";	
	public  final String 				ID_BOTTOMLEVEL 	= "bottomlevel";	
	public  final String 				ID_DERIVATIVE 	= "derivative";	
	public CapillaryLevel				ptsTop  		= new CapillaryLevel(ID_TOPLEVEL); 
	public CapillaryLevel				ptsBottom 		= new CapillaryLevel(ID_BOTTOMLEVEL); 
	public CapillaryLevel				ptsDerivative 	= new CapillaryLevel(ID_DERIVATIVE); 
	public CapillaryGulps 				gulpsRois 		= new CapillaryGulps(); 
	
	public boolean						valid			= true;

	private final String 				ID_META 		= "metaMC";
	private final String				ID_NFLIES		= "nflies";
	private final String				ID_CAGENB		= "cage_number";
	private final String 				ID_CAPVOLUME 	= "capillaryVolume";
	private final String 				ID_CAPPIXELS 	= "capillaryPixels";
	private final String 				ID_STIML 		= "stimulus";
	private final String 				ID_CONCL 		= "concentration";
	private final String 				ID_SIDE 		= "side";
	private final String 				ID_DESCOK 		= "descriptionOK";
	private final String				ID_VERSIONINFOS	= "versionInfos";
	
	private final String 				ID_INTERVALS 	= "INTERVALS";
	private final String				ID_NINTERVALS	= "nintervals";
	private final String 				ID_INTERVAL 	= "interval_";
	
	private final String 				ID_INDEXIMAGE 	= "indexImageMC";
	private final String 				ID_NAME 		= "nameMC";
	private final String 				ID_NAMETIFF 	= "filenameTIFF";
	private final String 				ID_VERSION		= "version"; 
	private final String 				ID_VERSIONNUM	= "1.0.0"; 
	
	// ----------------------------------------------------
	
	public Capillary(ROI2D roiCapillary) 
	{
		this.roi = roiCapillary;
		this.kymographName = replace_LR_with_12(roiCapillary.getName());
	}
	
	Capillary(String name) 
	{
		this.kymographName = replace_LR_with_12(name);
	}
	
	public Capillary() 
	{
	}

	@Override
	public int compareTo(Capillary o) 
	{
		if (o != null)
			return this.kymographName.compareTo(o.kymographName);
		return 1;
	}
	
	// ------------------------------------------
	
	public void copy(Capillary cap) 
	{
		indexKymograph 	= cap.indexKymograph;
		kymographName 	= cap.kymographName;
		version 		= cap.version;
		roi 			= (ROI2D) cap.roi.getCopy();
		filenameTIFF	= cap.filenameTIFF;
		
		capStimulus		= cap.capStimulus;
		capConcentration= cap.capConcentration;
		capSide			= cap.capSide;
		capNFlies		= cap.capNFlies;
		capCageID		= cap.capCageID;
		capVolume 		= cap.capVolume;
		capPixels 		= cap.capPixels;
		
		limitsOptions	= cap.limitsOptions;
		gulpsRois.rois	= new ArrayList <ROI2D> ();
		if (cap.gulpsRois.rois != null && cap.gulpsRois.rois.size() > 0)
			gulpsRois.rois.addAll(cap.gulpsRois.rois);
		
		ptsTop.copy(cap.ptsTop); 
		ptsBottom.copy(cap.ptsBottom); 
		ptsDerivative.copy(cap.ptsDerivative); 
	}
	
	public String getKymographName() 
	{
		return kymographName;
	}
	
	public void setKymographName(String name) 
	{
		this.kymographName = name;
	}
	
	public ROI2D getRoi() {
		return roi;
	}
	
	public void setRoi(ROI2D roi) {
		this.roi = roi;
	}
	
	public void setRoiName(String name) 
	{
		roi.setName(name);
	}
	
	public String getRoiName() {
		return roi.getName();
	}
	
	public String getLast2ofCapillaryName() 
	{
		return roi.getName().substring(roi.getName().length() -2);
	}
	
	public String getCapillarySide() 
	{
		return roi.getName().substring(roi.getName().length() -1);
	}
	
	public static String replace_LR_with_12(String name) 
	{
		String newname = name;
		if (name .contains("R"))
			newname = name.replace("R", "2");
		else if (name.contains("L"))
			newname = name.replace("L", "1");
		return newname;
	}
	
	public int getCageIndexFromRoiName() 
	{
		String name = roi.getName();
		if (!name .contains("line"))
			return -1;
		return Integer.valueOf(name.substring(4, 5));
	}
	
	public String getSideDescriptor(EnumXLSExportType xlsExportOption) 
	{
		String value = null;
		capSide = getCapillarySide();
		switch (xlsExportOption) 
		{
		case DISTANCE:
		case ISALIVE:
			value = capSide + "(L=R)";
			break;
		case SUMGULPS_LR:
		case TOPLEVELDELTA_LR:
		case TOPLEVEL_LR:
			if (capSide.equals("L"))
				value = "sum";
			else
				value = "PI";
			break;
		case XYIMAGE:
		case XYTOPCAGE:
		case XYTIPCAPS:
			if (capSide .equals ("L"))
				value = "x";
			else
				value = "y";
			break;
		default:
			value = capSide;
			break;
		}
		return value;
	}
	
	// -----------------------------------------
	
	public boolean isThereAnyMeasuresDone(EnumXLSExportType option) 
	{
		boolean yes = false;
		switch (option) 
		{
		case DERIVEDVALUES:
			yes= (ptsDerivative != null && ptsDerivative.isThereAnyMeasuresDone());
			break;
		case SUMGULPS:
			yes= (gulpsRois!= null && gulpsRois.isThereAnyMeasuresDone());
			break;
		case BOTTOMLEVEL:
			yes= ptsBottom.isThereAnyMeasuresDone();
			break;
		case TOPLEVEL:
		default:
			yes= ptsTop.isThereAnyMeasuresDone();
			break;
		}
		return yes;
	}
		
	public ArrayList<Integer> getCapillaryMeasuresForPass1(EnumXLSExportType option, long seriesBinMs, long outputBinMs) 
	{
		ArrayList<Integer> datai = null;
		switch (option) 
		{
		case DERIVEDVALUES:
			if (ptsDerivative != null) 
				datai = ptsDerivative.getMeasures(seriesBinMs, outputBinMs);
			break;
		case SUMGULPS:
		case SUMGULPS_LR:
		case NBGULPS:
		case AMPLITUDEGULPS:
		case TTOGULP:
		case TTOGULP_LR:
		case AUTOCORREL:
		case AUTOCORREL_LR:
		case CROSSCORREL:
		case CROSSCORREL_LR:
			if (gulpsRois != null)
				datai = gulpsRois.getMeasuresFromGulps(option, ptsTop.getNPoints(), seriesBinMs, outputBinMs);
			break;
		case BOTTOMLEVEL:
			datai = ptsBottom.getMeasures(seriesBinMs, outputBinMs);
			break;
		case TOPLEVEL:
		case TOPRAW:
		case TOPLEVEL_LR:
		case TOPLEVELDELTA:
		case TOPLEVELDELTA_LR:
			default:
			datai = ptsTop.getMeasures(seriesBinMs, outputBinMs);
			break;
		}
		return datai;
	}
		
	public void cropMeasuresToNPoints (int npoints) 
	{
		if (ptsTop.polylineLevel != null)
			ptsTop.cropToNPoints(npoints);
		if (ptsBottom.polylineLevel != null)
			ptsBottom.cropToNPoints(npoints);
		if (ptsDerivative.polylineLevel != null)
			ptsDerivative.cropToNPoints(npoints);
	}
	
	public void restoreClippedMeasures () 
	{
		if (ptsTop.polylineLevel != null)
			ptsTop.restoreNPoints();
		if (ptsBottom.polylineLevel != null)
			ptsBottom.restoreNPoints();
		if (ptsDerivative.polylineLevel != null)
			ptsDerivative.restoreNPoints();
	}
	
	public void setGulpsOptions (Options_BuildSeries options) 
	{
		limitsOptions = options;
	}
	
	public Options_BuildSeries getGulpsOptions () 
	{
		return limitsOptions;
	}
	
	public void cleanGulps() 
	{
		if (gulpsRois == null) 
		{
			gulpsRois = new CapillaryGulps();
			gulpsRois.rois = new ArrayList <> ();
		}
		else {
			if (limitsOptions.analyzePartOnly) 
				gulpsRois.removeROIsWithinInterval(limitsOptions.firstPixel, limitsOptions.lastPixel);
			else 
				gulpsRois.rois.clear();
		}
	}
	
	public void detectGulps(int indexkymo) 
	{
		int indexPixel = 0;
		int firstPixel = 1;
		if (ptsTop.polylineLevel == null)
			return;
		int lastPixel = ptsTop.polylineLevel.npoints;
		if (limitsOptions.analyzePartOnly) 
		{
			firstPixel = limitsOptions.firstPixel;
			lastPixel = limitsOptions.lastPixel;
		} 
		
		int threshold = (int) ((limitsOptions.detectGulpsThresholdUL / capVolume) * capPixels);
		
		for (indexPixel = firstPixel; indexPixel < lastPixel; indexPixel++) 
		{
			int derivativevalue = (int) ptsDerivative.polylineLevel.ypoints[indexPixel-1];
			if (derivativevalue < threshold) 
				continue;		
			saveGulpPointsToGulpRois(indexPixel, indexkymo);
		}
	}
	
	private void saveGulpPointsToGulpRois(int indexPixel, int indexkymo ) 
	{
		int delta = (int) Math.abs(ptsTop.polylineLevel.ypoints[indexPixel]-ptsTop.polylineLevel.ypoints[indexPixel-1]);
		if (delta <= 1)
			return;
		List<Point2D> gulpPoints = new ArrayList<>();
		Point2D.Double detectedPoint = new Point2D.Double (indexPixel, ptsTop.polylineLevel.ypoints[indexPixel-1]);
		gulpPoints.add(detectedPoint);
		Point2D.Double detectedPoint2 = new Point2D.Double (indexPixel, ptsTop.polylineLevel.ypoints[indexPixel]);
		gulpPoints.add(detectedPoint2);
		gulpsRois.addGulp(new ROI2DPolyLine (gulpPoints), indexkymo, getLast2ofCapillaryName()+"_gulp"+String.format("%07d", indexPixel));
	}
	
	public int getLastMeasure(EnumXLSExportType option) 
	{
		int lastMeasure = 0;
		switch (option) 
		{
		case DERIVEDVALUES:
			if (ptsDerivative != null)
				lastMeasure = ptsDerivative.getLastMeasure();
			break;
		case SUMGULPS:
			if (gulpsRois != null) 
			{
				List<Integer> datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNPoints());
				lastMeasure = datai.get(datai.size()-1);
			}
			break;
		case BOTTOMLEVEL:
			lastMeasure = ptsBottom.getLastMeasure();
			break;
		case TOPLEVEL:
		default:
			lastMeasure = ptsTop.getLastMeasure();
			break;
		}
		return lastMeasure;
	}
	
	public int getLastDeltaMeasure(EnumXLSExportType option) 
	{
		int lastMeasure = 0;
		switch (option) 
		{
		case DERIVEDVALUES:
			if (ptsDerivative != null)
				lastMeasure = ptsDerivative.getLastDeltaMeasure();
			break;
		case SUMGULPS:
			if (gulpsRois != null) {
				List<Integer> datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNPoints());
				lastMeasure = datai.get(datai.size()-1) - datai.get(datai.size()-2);
			}
			break;
		case BOTTOMLEVEL:
			lastMeasure = ptsBottom.getLastDeltaMeasure();
			break;
		case TOPLEVEL:
		default:
			lastMeasure = ptsTop.getLastDeltaMeasure();
			break;
		}
		return lastMeasure;
	}
	
	public int getT0Measure(EnumXLSExportType option) 
	{
		int t0Measure = 0;
		switch (option) 
		{
		case DERIVEDVALUES:
			if (ptsDerivative != null)
				t0Measure = ptsDerivative.getT0Measure();
			break;
		case SUMGULPS:
			if (gulpsRois != null) {
				List<Integer> datai = gulpsRois.getCumSumFromRoisArray(ptsTop.getNPoints());
				t0Measure = datai.get(0);
			}
			break;
		case BOTTOMLEVEL:
			t0Measure = ptsBottom.getT0Measure();
			break;
		case TOPLEVEL:
		default:
			t0Measure = ptsTop.getT0Measure();
			break;
		}
		return t0Measure;
	}
	
	public List<ROI2D> transferMeasuresToROIs() 
	{
		List<ROI2D> listrois = new ArrayList<ROI2D> ();
		if (ptsTop != null)
			ptsTop.addToROIs(listrois, indexKymograph);
		if (ptsBottom != null)
			ptsBottom.addToROIs(listrois, indexKymograph);
		if (gulpsRois != null)
			gulpsRois.addToROIs(listrois, indexKymograph);
		if (ptsDerivative != null)
			ptsDerivative.addToROIs(listrois, Color.yellow, 1., indexKymograph);
		return listrois;
	}
	
	public void transferROIsToMeasures(List<ROI> listRois) 
	{
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
	public boolean loadFromXML(Node node) 
	{
		boolean result = loadFromXML_CapillaryOnly(node);	
		result |= loadFromXML_MeasuresOnly( node);
		return result;
	}
	
	@Override
	public boolean saveToXML(Node node) 
	{
		saveToXML_CapillaryOnly(node);
		saveToXML_MeasuresOnly(node); 
        return true;
	}
		
	public boolean loadFromXML_CapillaryOnly(Node node) 
	{
	    final Node nodeMeta = XMLUtil.getElement(node, ID_META);
	    boolean flag = (nodeMeta != null); 
	    if (flag) 
	    {
	    	version 		= XMLUtil.getElementValue(nodeMeta, ID_VERSION, "0.0.0");
	    	indexKymograph 	= XMLUtil.getElementIntValue(nodeMeta, ID_INDEXIMAGE, indexKymograph);
	        kymographName 	= XMLUtil.getElementValue(nodeMeta, ID_NAME, kymographName);
	        filenameTIFF 	= XMLUtil.getElementValue(nodeMeta, ID_NAMETIFF, filenameTIFF);	        
	        descriptionOK 	= XMLUtil.getElementBooleanValue(nodeMeta, ID_DESCOK, false);
	        versionInfos 	= XMLUtil.getElementIntValue(nodeMeta, ID_VERSIONINFOS, 0);
	        capNFlies 		= XMLUtil.getElementIntValue(nodeMeta, ID_NFLIES, capNFlies);
	        capCageID 		= XMLUtil.getElementIntValue(nodeMeta, ID_CAGENB, capCageID);
	        capVolume 		= XMLUtil.getElementDoubleValue(nodeMeta, ID_CAPVOLUME, Double.NaN);
			capPixels 		= XMLUtil.getElementIntValue(nodeMeta, ID_CAPPIXELS, 5);
			capStimulus 	= XMLUtil.getElementValue(nodeMeta, ID_STIML, ID_STIML);
			capConcentration= XMLUtil.getElementValue(nodeMeta, ID_CONCL, ID_CONCL);
			capSide 		= XMLUtil.getElementValue(nodeMeta, ID_SIDE, ".");
			
	        roi = ROI2DUtilities.loadFromXML_ROI(nodeMeta);
	        limitsOptions.loadFromXML(nodeMeta);
	        
	        loadFromXML_intervals(node);
	    }
	    return flag;
	}
	
	private boolean loadFromXML_intervals(Node node) 
	{
		roisForKymo.clear();
		final Node nodeMeta2 = XMLUtil.getElement(node, ID_INTERVALS);
	    if (nodeMeta2 == null)
	    	return false;
	    int nitems = XMLUtil.getElementIntValue(nodeMeta2, ID_NINTERVALS, 0);
		if (nitems > 0) {
        	for (int i=0; i < nitems; i++) {
        		Node node_i = XMLUtil.setElement(nodeMeta2, ID_INTERVAL+i);
        		KymoROI2D roiInterval = new KymoROI2D();
        		roiInterval.loadFromXML(node_i);
        		roisForKymo.add(roiInterval);
        		
        		if (i == 0) {
        			roi = roisForKymo.get(0).getRoi();
        		}
        	}
        }
        return true;
	}
	
	public boolean loadFromXML_MeasuresOnly(Node node) 
	{
		String header = getLast2ofCapillaryName()+"_";
		boolean result = ptsDerivative.loadCapillaryLimitFromXML(node, ID_DERIVATIVE, header) > 0;
		result |= ptsTop.loadCapillaryLimitFromXML(node, ID_TOPLEVEL, header) > 0;
		result |= ptsBottom.loadCapillaryLimitFromXML(node, ID_BOTTOMLEVEL, header) > 0;
		result |= gulpsRois.loadFromXML(node);
		return result;
	}
	
	public boolean saveToXML_CapillaryOnly(Node node) 
	{
	    final Node nodeMeta = XMLUtil.setElement(node, ID_META);
	    if (nodeMeta == null)
	    	return false;
    	if (version == null)
    		version = ID_VERSIONNUM;
    	XMLUtil.setElementValue(nodeMeta, ID_VERSION, version);
        XMLUtil.setElementIntValue(nodeMeta, ID_INDEXIMAGE, indexKymograph);
        XMLUtil.setElementValue(nodeMeta, ID_NAME, kymographName);
        if (filenameTIFF != null ) {
        	String filename = Paths.get(filenameTIFF).getFileName().toString();
        	XMLUtil.setElementValue(nodeMeta, ID_NAMETIFF, filename);
        }
        XMLUtil.setElementBooleanValue(nodeMeta, ID_DESCOK, descriptionOK);
        XMLUtil.setElementIntValue(nodeMeta, ID_VERSIONINFOS, versionInfos);
        XMLUtil.setElementIntValue(nodeMeta, ID_NFLIES, capNFlies);
        XMLUtil.setElementIntValue(nodeMeta, ID_CAGENB, capCageID);
		XMLUtil.setElementDoubleValue(nodeMeta, ID_CAPVOLUME, capVolume);
		XMLUtil.setElementIntValue(nodeMeta, ID_CAPPIXELS, capPixels);
		XMLUtil.setElementValue(nodeMeta, ID_STIML, capStimulus);
		XMLUtil.setElementValue(nodeMeta, ID_SIDE, capSide);
		XMLUtil.setElementValue(nodeMeta, ID_CONCL, capConcentration);

		ROI2DUtilities.saveToXML_ROI(nodeMeta, roi); 
		
		boolean flag = saveToXML_intervals(node);
	    return flag;
	}
	
	private boolean saveToXML_intervals(Node node) 
	{
		final Node nodeMeta2 = XMLUtil.setElement(node, ID_INTERVALS);
	    if (nodeMeta2 == null)
	    	return false;
		int nitems = roisForKymo.size();
		XMLUtil.setElementIntValue(nodeMeta2, ID_NINTERVALS, nitems);
        if (nitems > 0) {
        	for (int i=0; i < nitems; i++) {
        		Node node_i = XMLUtil.setElement(nodeMeta2, ID_INTERVAL+i);
        		roisForKymo.get(i).saveToXML(node_i);
        	}
        }
        return true;
	}
	
	public void saveToXML_MeasuresOnly(Node node) 
	{
		if (ptsTop != null)
			ptsTop.saveCapillaryLimit2XML(node, ID_TOPLEVEL);
		if (ptsBottom != null)
			ptsBottom.saveCapillaryLimit2XML(node, ID_BOTTOMLEVEL);
		if (ptsDerivative != null)
			ptsDerivative.saveCapillaryLimit2XML(node, ID_DERIVATIVE);
		if (gulpsRois != null)
			gulpsRois.saveToXML(node);
	}
	 
	// -------------------------------------------
	
	public Point2D getCapillaryTipWithinROI2D (ROI2D roi2D) 
	{
		Point2D pt = null;		
		if (roi instanceof ROI2DPolyLine) 
		{
			Polyline2D line = (( ROI2DPolyLine) roi).getPolyline2D();
			int last = line.npoints - 1;
			if (roi2D.contains(line.xpoints[0],  line.ypoints[0]))
				pt = new Point2D.Double(line.xpoints[0],  line.ypoints[0]);
			else if (roi2D.contains(line.xpoints[last],  line.ypoints[last])) 
				pt = new Point2D.Double(line.xpoints[last],  line.ypoints[last]);
		} 
		else if (roi instanceof ROI2DLine) 
		{
			Line2D line = (( ROI2DLine) roi).getLine();
			if (roi2D.contains(line.getP1()))
				pt = line.getP1();
			else if (roi2D.contains(line.getP2())) 
				pt = line.getP2();
		}
		return pt;
	}
	
	public Point2D getCapillaryLowestPoint () 
	{
		Point2D pt = null;		
		if (roi instanceof ROI2DPolyLine) 
		{
			Polyline2D line = ((ROI2DPolyLine) roi).getPolyline2D();
			int last = line.npoints - 1;
			if (line.ypoints[0] > line.ypoints[last])
				pt = new Point2D.Double(line.xpoints[0],  line.ypoints[0]);
			else  
				pt = new Point2D.Double(line.xpoints[last],  line.ypoints[last]);
		} 
		else if (roi instanceof ROI2DLine) 
		{
			Line2D line = ((ROI2DLine) roi).getLine();
			if (line.getP1().getY() > line.getP2().getY())
				pt = line.getP1();
			else
				pt = line.getP2();
		}
		return pt;
	}
	
	public Point2D getCapillaryFirstPoint () 
	{
		Point2D pt = null;		
		if (roi instanceof ROI2DPolyLine) 
		{
			Polyline2D line = ((ROI2DPolyLine) roi).getPolyline2D();
			pt = new Point2D.Double(line.xpoints[0],  line.ypoints[0]);
		} 
		else if (roi instanceof ROI2DLine) 
		{
			Line2D line = ((ROI2DLine) roi).getLine();
			pt = line.getP1();
		}
		return pt;
	}
	
	public Point2D getCapillaryLastPoint () 
	{
		Point2D pt = null;		
		if (roi instanceof ROI2DPolyLine) 
		{
			Polyline2D line = ((ROI2DPolyLine) roi).getPolyline2D();
			int last = line.npoints - 1;
			pt = new Point2D.Double(line.xpoints[last],  line.ypoints[last]);
		} 
		else if (roi instanceof ROI2DLine) 
		{
			Line2D line = ((ROI2DLine) roi).getLine();
			pt = line.getP2();
		}
		return pt;
	}
	

	// --------------------------------------------
	
	public List<KymoROI2D> getRoisForKymo() {
		if (roisForKymo.size() < 1) 
			initROI2DForKymoList();
		return roisForKymo;
	}
	
 	public KymoROI2D getROI2DKymoAt(int i) {
		if (roisForKymo.size() < 1) 
			initROI2DForKymoList();
		return roisForKymo.get(i);
	}
 	
 	public KymoROI2D getROI2DKymoAtIntervalT(long t) {
		if (roisForKymo.size() < 1) 
			initROI2DForKymoList();
		
		KymoROI2D capRoi = null;
		for (KymoROI2D item : roisForKymo) {
			if (t < item.getStart())
				break;
			capRoi = item;
		}
		return capRoi;
	}
 	
 	public void removeROI2DIntervalStartingAt(long start) {
 		KymoROI2D itemFound = null;
 		for (KymoROI2D item : roisForKymo) {
			if (start != item.getStart())
				continue;
			itemFound = item;
		}
 		if (itemFound != null)
 			roisForKymo.remove(itemFound);
	}
	
	private void initROI2DForKymoList() { 
		roisForKymo.add(new KymoROI2D(0, roi));		
	}
	
	public void setVolumeAndPixels(double volume, int pixels) 
	{
		capVolume = volume;
		capPixels = pixels;
		descriptionOK = true;
	}
	
}
