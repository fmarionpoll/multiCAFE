package plugins.fmp.multicafe2.experiment;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;


public class CapillaryGulps implements XMLPersistent  
{	
	private final String ID_GULPS = "gulpsMC";
	public ArrayList<ROI2D> rois = null; 
	public ArrayList<Polyline2D> gulps = null;

	// -------------------------------
	
	public void copy(CapillaryGulps capG) 
	{
		rois = new ArrayList <ROI2D> ();
		rois.addAll(capG.rois);		
	}
	
	@Override
	public boolean loadFromXML(Node node) 
	{
		boolean flag = false;
		rois = new ArrayList <ROI2D> ();
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
        return flag;
	}

	@Override
	public boolean saveToXML(Node node) 
	{
		boolean flag = false;
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
	
	boolean isThereAnyMeasuresDone() 
	{
		return (rois != null && rois.size() > 0);
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
			data_in = getCumSumFromRoisArray(npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		case NBGULPS:
			data_in = getIsGulpsFromRoisArray(npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		case AMPLITUDEGULPS:
			data_in = getAmplitudeGulpsFromRoisArray(npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		case TTOGULP:
		case TTOGULP_LR:
			List<Integer> datag = getIsGulpsFromRoisArray(npoints);
			data_in = getTToNextGulp(datag, npoints);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
			
		case AUTOCORREL:
		case AUTOCORREL_LR:
		case CROSSCORREL:
		case CROSSCORREL_LR:
			data_in = getAmplitudeGulpsFromRoisArray(npoints);
			convertPositiveAmplitudesIntoEvent(data_in);
			data_in = stretchArrayToOutputBins(data_in, seriesBinMs, outputBinMs);
			break;
		default:
			break;
		}
		return data_in;
	}
	
	ArrayList<Integer> getCumSumFromRoisArray(int npoints) 
	{
		if (rois == null)
			return null;
		ArrayList<Integer> arrayInt = new ArrayList<Integer> (Collections.nCopies(npoints, 0));
		for (ROI roi: rois) 
			ROI2DUtilities.addROItoCumulatedSumArray((ROI2DPolyLine) roi, arrayInt);
		return arrayInt;
	}
	
	private ArrayList<Integer> getIsGulpsFromRoisArray(int npoints) 
	{
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
	
	private ArrayList<Integer> getAmplitudeGulpsFromRoisArray(int npoints) 
	{
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

	public void transferROIsToMeasures(List<ROI> listRois) 
	{	
		rois = new ArrayList<ROI2D>();
		for (ROI roi: listRois) 
		{		
			String roiname = roi.getName();
			if (roi instanceof ROI2DPolyLine ) 
			{
				if (roiname .contains("gulp"))	
					rois.add( (ROI2D) roi);
			}
		}
	}

	public void addGulpRoi(ROI2DPolyLine roi, int indexkymo, String name) 
	{
		roi.setColor(Color.red);
		roi.setStroke(1);
		roi.setName(name);
		roi.setT(indexkymo);
		if (rois == null)
			rois = new ArrayList <ROI2D> ();
		rois.add(roi);
	}
	
	public List<ROI2D> addToROIs(List<ROI2D> listrois, int indexImage) 
	{
		if (rois != null) 
			listrois.addAll(rois);
		return listrois;
	}
	
	public void removeROIsWithinInterval(int startPixel, int endPixel) 
	{
		Iterator <ROI2D> iterator = rois.iterator();
		while (iterator.hasNext()) 
		{
			ROI2D roi = iterator.next();
			// if roi.first >= startpixel && roi.first <= endpixel	
			Rectangle rect = ((ROI2D) roi).getBounds();
			if (rect.x >= startPixel && rect.x <= endPixel) 
				iterator.remove();
		}
	}
	
	// -------------------------------
		
	public String csvExportData(String kymographName) {

		StringBuffer sbfN = csvExportFirstColumns(kymographName, "N");
		StringBuffer sbfX = csvExportFirstColumns(kymographName, "X");
		StringBuffer sbfY = csvExportFirstColumns(kymographName, "Y");

//		System.out.println(" ---kymograph= "+kymographName);
        for (int i =0; i< rois.size(); i++)
        {
        	Polyline2D polyline = ((ROI2DPolyLine) rois.get(i)).getPolyline2D();
        	if (polyline.npoints < 2)
        		System.out.println(kymographName + " roi i="+i + " npoints= "+polyline.npoints);
        	for (int j=0; j< polyline.npoints; j++)
        	{
        		sbfN.append(Integer.toString(i)); 
                sbfN.append("\t");
                sbfX.append(Integer.toString((int) polyline.xpoints[j]));
                sbfX.append("\t");
                sbfY.append(Integer.toString((int) polyline.ypoints[j]));
                sbfY.append("\t");
        	}
        }

		StringBuffer sbf = new StringBuffer();
		sbf.append(sbfN);
		sbf.append("\n");
		sbf.append(sbfX);
		sbf.append("\n");
		sbf.append(sbfY);
		sbf.append("\n");

		return sbf.toString();
	}
	
	private StringBuffer csvExportFirstColumns(String kymographName, String XorYorN) {
		StringBuffer sbf = new StringBuffer();
		sbf.append(kymographName + "\t"
				+ XorYorN +"\t"
				+ Integer.toString(rois.size())+ "\t");
		return sbf;
	}
	
	public void csvImportGulpsFrom3Rows(int[] dataN, int[] dataX, int [] dataY, String roiNamePrefix, int indexkymo) {
		int icurrent = -1;
		ArrayList<Integer> xpoints = new ArrayList<Integer>();
		ArrayList<Integer> ypoints =  new ArrayList<Integer>();
		for (int i = 0; i < dataN.length; i++) {
			if (dataN[i] != icurrent || i == dataN.length -1) {
				if (icurrent >= 0) {
					addNewGulp(roiNamePrefix, indexkymo, icurrent, xpoints, ypoints);
					xpoints = new ArrayList<Integer>();
					ypoints =  new ArrayList<Integer>();
					icurrent = dataN[i];
				}
			}
			xpoints.add(dataX[i]);
			ypoints.add(dataY[i]);
			icurrent = dataN[i];
		}
	}
	
	void addNewGulp(String roiNamePrefix, int indexkymo, int icurrent, ArrayList<Integer> xpoints, ArrayList<Integer> ypoints ) {
		int[] xInt = xpoints.stream().mapToInt(Integer::intValue).toArray();
		int[] yInt = ypoints.stream().mapToInt(Integer::intValue).toArray();
		ROI2DPolyLine roi = new ROI2DPolyLine(new Polyline2D (xInt, yInt, xInt.length));
		addGulpRoi(roi, indexkymo, getRoiGulpName(roiNamePrefix, icurrent));
	}
	
	static String getRoiGulpName(String roiNamePrefix, int icurrent) {
		return roiNamePrefix + "_gulp" + String.format("%07d", icurrent);
	}
	

		
}