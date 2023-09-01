package plugins.fmp.multicafe2.experiment;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.StringUtil;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;


public class CapillaryGulps implements XMLPersistent  
{	
	private final String ID_GULPS = "gulpsMC";
	public ArrayList<Polyline2D> gulps = new ArrayList<Polyline2D> ();
	public String gulpNamePrefix = "haha"; 
	public int	gulpIndexKymo = -1; 

	// -------------------------------
	

	public void copy(CapillaryGulps capG) 
	{
		gulps = new ArrayList <Polyline2D> (capG.gulps.size());
		gulps.addAll(capG.gulps);
		gulpNamePrefix = new String(capG.gulpNamePrefix);
	}
	
	@Override
	public boolean loadFromXML(Node node) 
	{
		boolean flag = false;
		ArrayList <ROI2D>rois = new ArrayList <ROI2D> ();
		final Node nodeROIs = XMLUtil.getElement(node, ID_GULPS);
		if (nodeROIs != null) 
		{
			flag = true;
			List<ROI> roislocal = ROI.loadROIsFromXML(nodeROIs);
			for (ROI roislocal_i : roislocal) 
			{
        	   ROI2D roi = (ROI2D) roislocal_i;
        	   rois.add(roi);
           }
		}
		
		buildGulpsFromROIs(rois);
        return flag;
	}

	@Override
	public boolean saveToXML(Node node) 
	{
		boolean flag = false;
		ArrayList<ROI2DPolyLine> rois = getGulpsAsROIs();
		
		final Node nodeROIs = XMLUtil.setElement(node, ID_GULPS);
        if (nodeROIs != null)
        {
        	flag = true;
        	if (rois != null && rois.size() > 0) 
        	{
        		List<ROI> roislocal = new ArrayList<ROI> (rois.size());
        		for (ROI2D roi: rois)
        			roislocal.add((ROI) roi);
        		ROI.saveROIsToXML(nodeROIs, roislocal);
        	}
	    }
        return flag;
	}
	
	// -------------------------------
		
	public void addNewGulpFromPoints( ArrayList<Point2D> gulpPoints ) 
	{
		int npoints = gulpPoints.size();
		if (npoints < 1)
			return;
		
		double[] xpoints = new double[npoints] ;
		double[] ypoints = new double[npoints] ;
		for (int i = 0; i < npoints; i++) {
			xpoints[i] = gulpPoints.get(i).getX();
			ypoints[i] = gulpPoints.get(i).getY();
		}
		Polyline2D gulpLine = new Polyline2D (xpoints, ypoints, npoints);
		gulps.add(gulpLine);
	}
	
	boolean isThereAnyMeasuresDone() 
	{
		
		return (gulps != null && gulps.size() > 0);
	}
	
	private void convertPositiveAmplitudesIntoEvent(ArrayList<Integer> data_in)
	{
		if (data_in == null) 
			return;
		
		int npoints = data_in.size();
		for (int i = 0; i < npoints; i++) 
			data_in.set(i, data_in.get(i) != 0? 1: 0);
	}
	
	private ArrayList<Integer> stretchArrayToOutputBins(ArrayList<Integer> data_in, long seriesBinMs, long outputBinMs) 
	{
		if (data_in == null) 
			return null;
		
		long npoints_out = data_in.size() * seriesBinMs / outputBinMs + 1;
		double time_last = data_in.size() * seriesBinMs;
		ArrayList<Integer> data_out = new ArrayList<Integer> ((int) npoints_out);
		for (double time_out = 0; time_out <= time_last; time_out += outputBinMs) 
		{
			int index_in = (int) (time_out / seriesBinMs);
			if (index_in >= data_in.size())
				index_in = data_in.size() -1;
			data_out.add( data_in.get(index_in));
		}
		return data_out;
	}
	
	public ArrayList<Integer> getMeasuresFromGulps(EnumXLSExportType option, int npoints, long seriesBinMs, long outputBinMs) 
	{	
		ArrayList<Integer> data_in = null;
		switch (option) 
		{
		case SUMGULPS:
		case SUMGULPS_LR:
			data_in = getCumSumFromROIsArray(npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		case NBGULPS:
			data_in = getIsGulpsFromROIsArray(npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		case AMPLITUDEGULPS:
			data_in = getAmplitudeGulpsFromROIsArray(npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		case TTOGULP:
		case TTOGULP_LR:
			List<Integer> datag = getIsGulpsFromROIsArray(npoints);
			data_in = getTToNextGulp(datag, npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
			
		case AUTOCORREL:
		case AUTOCORREL_LR:
		case CROSSCORREL:
		case CROSSCORREL_LR:
			data_in = getAmplitudeGulpsFromROIsArray(npoints);
			convertPositiveAmplitudesIntoEvent(data_in);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		default:
			break;
		}
		return data_in;
	}
		
	ArrayList<Integer> getTToNextGulp(List<Integer> datai, int npoints) 
	{
		int nintervals = -1;
		ArrayList<Integer> data_out = null;
		for (int index = datai.size()-1; index >= 0; index--) 
		{
			if (datai.get(index) == 1) 
			{
				if (nintervals < 0) 
				{
					int nitems = index+1;
					data_out = new ArrayList<Integer> (Collections.nCopies(nitems, 0));
				}
				nintervals = 0;
				data_out.set(index, nintervals);
			}
			else if (nintervals >= 0) 
			{
				nintervals++;
				data_out.set(index, nintervals);
			}
		}
		return data_out;
	}

	public void removeGulpsWithinInterval(int startPixel, int endPixel) 
	{
		Iterator <Polyline2D> iterator = gulps.iterator();
		while (iterator.hasNext()) 
		{
			Polyline2D gulp = iterator.next();
			// if roi.first >= startpixel && roi.first <= endpixel	
			Rectangle rect = ((Polyline2D) gulp).getBounds();
			if (rect.x >= startPixel && rect.x <= endPixel) 
				iterator.remove();
		}
	}
	
	// -------------------------------
		
	public boolean csvExportDataToRow(StringBuffer sbf) 
	{
		int ngulps = 0;
		if (gulps != null)
			ngulps = gulps.size();
		sbf.append(Integer.toString(ngulps) + ",");
		if (ngulps > 0) {
		    for (int indexgulp = 0; indexgulp < gulps.size(); indexgulp++) 
		    	csvExportOneGulp(sbf, indexgulp);
		}
		return true;
	}
	
	private void csvExportOneGulp(StringBuffer sbf, int indexgulp)
	{
		sbf.append("g"+indexgulp+",");
		Polyline2D gulp = gulps.get(indexgulp);
    	sbf.append(StringUtil.toString((int) gulp.npoints));
        sbf.append(",");
        for (int i = 0; i< gulp.npoints; i++) {
	    	sbf.append(StringUtil.toString((int) gulp.xpoints[i]));
            sbf.append(",");
            sbf.append(StringUtil.toString((int) gulp.ypoints[i]));
            sbf.append(",");
        }
	}
	
	public void csvImportDataFromRow(String [] data, int startAt, String roiNamePrefix, int indexkymo) 
	{
		gulpNamePrefix = roiNamePrefix;
		gulpIndexKymo = indexkymo;
	
		if (data.length < startAt) 
			return;
			
		int ngulps = Integer.valueOf(data[startAt]);
		if (ngulps > 0) {
			int offset = startAt+1;
			for (int i = 0; i < ngulps; i++) {
				offset = csvImportOneGulp(data, offset);
			}
		}
	}
	
	private int csvImportOneGulp(String[] data, int offset) 
	{
		offset++;
		int npoints = Integer.valueOf(data[offset]);
		offset++;
		
		int[] x = new int[npoints];
		int[] y = new int[npoints];
		for (int i = 0; i < npoints; i++) { 
			x[i] = Integer.valueOf(data[offset]);
			offset++;
			y[i] = Integer.valueOf(data[offset]);
			offset++;
		}
		Polyline2D gulpLine = new Polyline2D (x, y, npoints);
		gulps.add(gulpLine);
		
		return offset;
	}
		
	// -------------------------------
		
	static String buildROIGulpName(String rootName, int tIndex) {
		return rootName + "_gulp" + String.format("%07d", tIndex);
	}
	
	public ArrayList <ROI2DPolyLine> getGulpsAsROIs() 
	{
		ArrayList<ROI2DPolyLine> rois = new ArrayList<ROI2DPolyLine> (gulps.size());
		for (Polyline2D gulpLine: gulps) 
			rois.add( buildROIfromGulp(gulpLine));
		return rois;
	}
	
	private ROI2DPolyLine buildROIfromGulp(Polyline2D gulpLine) 
	{
		ROI2DPolyLine roi = new ROI2DPolyLine (gulpLine);
		roi.setName(buildROIGulpName(gulpNamePrefix, (int) gulpLine.xpoints[0]));
		roi.setColor(Color.red);
		roi.setStroke(1);
		roi.setT(gulpIndexKymo);
		return roi;
	}
	
	public void buildGulpsFromROIs(ArrayList<ROI2D> rois ) 
	{
		gulps = new ArrayList<Polyline2D> (rois.size());
		for (ROI2D roi : rois) {
			Polyline2D gulpLine = ((ROI2DPolyLine) roi).getPolyline2D();
			gulps.add(gulpLine);
		}
	}
	
	public void transferROIsToMeasures(List<ROI> listRois) 
	{	
		ArrayList<ROI2D> rois = new ArrayList<ROI2D>();
		for (ROI roi: listRois) 
		{		
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine ) 
			{
				if (roiname .contains("gulp"))	
					rois.add( (ROI2DPolyLine) roi);
			}
		}
		buildGulpsFromROIs(rois);
	}

	public List<ROI2D> addGulpsToROIs(List<ROI2D> listrois, int indexImage) 
	{
		ArrayList<ROI2DPolyLine> rois = getGulpsAsROIs();
		listrois.addAll(rois);
		return listrois;
	}
	
	ArrayList<Integer> getCumSumFromROIsArray(int npoints) 
	{
		ArrayList<ROI2DPolyLine> rois = getGulpsAsROIs();
		if (rois == null)
			return null;
		
		ArrayList<Integer> arrayInt = new ArrayList<Integer> (Collections.nCopies(npoints, 0));
		for (ROI roi: rois) 
			ROI2DUtilities.addROItoCumulatedSumArray((ROI2DPolyLine) roi, arrayInt);
		return arrayInt;
	}
	
	private ArrayList<Integer> getIsGulpsFromROIsArray(int npoints) 
	{
		ArrayList<ROI2DPolyLine> rois = getGulpsAsROIs();
		if (rois == null)
			return null;
		
		ArrayList<Integer> arrayInt = new ArrayList<Integer> (Collections.nCopies(npoints, 0));
		for (ROI roi: rois) 
			addROItoIsGulpsArray((ROI2DPolyLine) roi, arrayInt);
		return arrayInt;
	}
	
	private void addROItoIsGulpsArray (ROI2DPolyLine roi, ArrayList<Integer> isGulpsArrayList) 
	{
		Polyline2D roiline = roi.getPolyline2D();
		double yvalue = roiline.ypoints[0];
		int npoints = roiline.npoints;
		for (int j = 0; j < npoints; j++) 
		{
			if (roiline.ypoints[j] != yvalue) 
			{
				int timeIndex =  (int) roiline.xpoints[j];
				isGulpsArrayList.set(timeIndex, 1);
			}
			yvalue = roiline.ypoints[j];
		}
	}
	
	private ArrayList<Integer> getAmplitudeGulpsFromROIsArray(int npoints) 
	{
		ArrayList<ROI2DPolyLine> rois = getGulpsAsROIs();
		if (rois == null)
			return null;
		ArrayList<Integer> amplitudeGulpsArray = new ArrayList<Integer> (Collections.nCopies(npoints, 0));
		for (ROI roi: rois) 
			addROItoAmplitudeGulpsArray((ROI2DPolyLine) roi, amplitudeGulpsArray);
		return amplitudeGulpsArray;
	}
	
	private void addROItoAmplitudeGulpsArray (ROI2DPolyLine roi, ArrayList<Integer> amplitudeGulpsArray) 
	{
		Polyline2D polyline2D = roi.getPolyline2D();
		double yvalue = polyline2D.ypoints[0];
		int npoints = polyline2D.npoints;
		for (int j = 0; j < npoints; j++) 
		{
			int timeIndex =  (int) polyline2D.xpoints[j];
			int delta = (int) (polyline2D.ypoints[j] - yvalue);
			amplitudeGulpsArray.set(timeIndex, delta);		
			yvalue = polyline2D.ypoints[j];
		}
	}

}