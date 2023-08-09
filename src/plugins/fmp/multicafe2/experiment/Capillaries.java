package plugins.fmp.multicafe2.experiment;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;
import icy.util.XMLUtil;

import plugins.fmp.multicafe2.tools.Comparators;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class Capillaries 
{	
	public CapillariesDescription 	capillariesDescription	= new CapillariesDescription();
	public CapillariesDescription 	desc_old			= new CapillariesDescription();
	public ArrayList <Capillary> 	capillariesList		= new ArrayList <Capillary>();
	private	KymoIntervals 			capillariesListTimeIntervals = null;
		
	private final static String ID_CAPILLARYTRACK 		= "capillaryTrack";
	private final static String ID_NCAPILLARIES 		= "N_capillaries";
	private final static String ID_LISTOFCAPILLARIES 	= "List_of_capillaries";
	private final static String ID_CAPILLARY_ 			= "capillary_";
	private final static String ID_MCCAPILLARIES_XML 	= "MCcapillaries.xml";

	// ---------------------------------
		
	public String getXMLNameToAppend() 
	{
		return ID_MCCAPILLARIES_XML;
	}

	public boolean xmlSaveCapillaries_Descriptors(String csFileName) 
	{
		if (csFileName != null) {
			final Document doc = XMLUtil.createDocument(true);
			if (doc != null) 
			{
				capillariesDescription.xmlSaveCapillaryDescription (doc);
				xmlSaveListOfCapillaries(doc);
				return XMLUtil.saveDocument(doc, csFileName);
			}
		}
		return false;
	}
	
	public boolean xmlSaveCapillaries_Measures(String directory) 
	{
		if (directory == null)
			return false;
		
		for (Capillary cap: capillariesList) 
			cap.xmlSaveCapillary_Measures(directory);
		
		csvSaveCapillariesData(directory);
		return true;
	}
	
	public boolean xmlLoadCapillaries_Descriptors(String csFileName) 
	{	
		if (csFileName == null)
			return false;
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) 
		{
			capillariesDescription.xmlLoadCapillaryDescription(doc);
			return xmlLoadCapillaries_Only_v1(doc);		
		}
		return false;
	}
	
	public boolean xmlLoadOldCapillaries_Only(String csFileName) 
	{
		if (csFileName == null)
			return false;			
		final Document doc = XMLUtil.loadDocument(csFileName);
		if (doc != null) 
		{
			capillariesDescription.xmlLoadCapillaryDescription(doc);
			switch (capillariesDescription.version) 
			{
			case 1: // old xml storage structure
				xmlLoadCapillaries_Only_v1(doc);
				break;
			case 0: // old-old xml storage structure
				xmlLoadCapillaries_v0(doc, csFileName);
				break;
			default:
				xmlLoadCapillaries_Only_v2(doc, csFileName);
				return false;
			}		
			return true;
		}
		return false;
	}
	
	public boolean xmlLoadCapillaries_Measures(String directory) 
	{
		boolean flag = false;
		int ncapillaries = capillariesList.size();
		
		try {
			flag = csvLoadCapillariesData(directory);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (!flag) {
			for (int i = 0; i < ncapillaries; i++) 
			{
				String csFile = directory + File.separator + capillariesList.get(i).getKymographName() + ".xml";
				final Document capdoc = XMLUtil.loadDocument(csFile);
				Node node = XMLUtil.getRootElement(capdoc, true);
				Capillary cap = capillariesList.get(i);
				cap.indexKymograph = i;
				flag |= cap.loadFromXML_MeasuresOnly(node);
			}
		}
		return flag;
	}
	
	private boolean xmlSaveListOfCapillaries(Document doc) 
	{
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		XMLUtil.setElementIntValue(node, "version", 2);
		Node nodecaps = XMLUtil.setElement(node, ID_LISTOFCAPILLARIES);
		XMLUtil.setElementIntValue(nodecaps, ID_NCAPILLARIES, capillariesList.size());
		int i= 0;
		Collections.sort(capillariesList);
		for (Capillary cap: capillariesList) 
		{
			Node nodecapillary = XMLUtil.setElement(node, ID_CAPILLARY_+i);
			cap.saveToXML_CapillaryOnly(nodecapillary);
			i++;
		}
		return true;
	}
	
	private void xmlLoadCapillaries_v0(Document doc, String csFileName) 
	{
		List<ROI> listOfCapillaryROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
		capillariesList.clear();
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		int t = 0;
		for (ROI roiCapillary: listOfCapillaryROIs) 
		{
			xmlLoadIndividualCapillary_v0((ROI2DShape) roiCapillary, directory, t);
			t++;
		}
	}
	
	private void xmlLoadIndividualCapillary_v0(ROI2D roiCapillary, String directory, int t) 
	{
		Capillary cap = new Capillary(roiCapillary);
		if (!isPresent(cap))
			capillariesList.add(cap);
		String csFile = directory + roiCapillary.getName() + ".xml";
		cap.indexKymograph = t;
		final Document dockymo = XMLUtil.loadDocument(csFile);
		if (dockymo != null) 
		{
			NodeList nodeROISingle = dockymo.getElementsByTagName("roi");					
			if (nodeROISingle.getLength() > 0) {	
				List<ROI> rois = new ArrayList<ROI>();
                for (int i=0; i< nodeROISingle.getLength(); i++) 
                {
                	Node element = nodeROISingle.item(i);
                    ROI roi_i = ROI.createFromXML(element);
                    if (roi_i != null)
                        rois.add(roi_i);
                }
				cap.transferROIsToMeasures(rois);
			}
		}
	}
	
	private boolean xmlLoadCapillaries_Only_v1(Document doc) 
	{
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), ID_CAPILLARYTRACK);
		if (node == null)
			return false;
		Node nodecaps = XMLUtil.getElement(node, ID_LISTOFCAPILLARIES);
		int nitems = XMLUtil.getElementIntValue(nodecaps, ID_NCAPILLARIES, 0);
		capillariesList = new ArrayList<Capillary> (nitems);
		for (int i = 0; i < nitems; i++) 
		{
			Node nodecapillary = XMLUtil.getElement(node, ID_CAPILLARY_+i);
			Capillary cap = new Capillary();
			cap.loadFromXML_CapillaryOnly(nodecapillary);
			if (capillariesDescription.grouping == 2 && (cap.capStimulus != null && cap.capStimulus.equals(".."))) 
			{
				if (cap.getCapillarySide().equals("R")) 
				{
					cap.capStimulus = capillariesDescription.stimulusR;
					cap.capConcentration = capillariesDescription.concentrationR;
				} 
				else 
				{
					cap.capStimulus = capillariesDescription.stimulusL;
					cap.capConcentration = capillariesDescription.concentrationL;
				}
			}
			if (!isPresent(cap))
				capillariesList.add(cap);
		}
		return true;
	}

	private void xmlLoadCapillaries_Only_v2(Document doc, String csFileName) 
	{
		xmlLoadCapillaries_Only_v1(doc);
		Path directorypath = Paths.get(csFileName).getParent();
		String directory = directorypath + File.separator;
		for (Capillary cap: capillariesList) 
		{
			String csFile = directory + cap.getKymographName() + ".xml";
			final Document capdoc = XMLUtil.loadDocument(csFile);
			Node node = XMLUtil.getRootElement(capdoc, true);
			cap.loadFromXML_CapillaryOnly(node);
		}
	}	

	// -----------------------
	
	public void copy (Capillaries cap) 
	{
		capillariesDescription.copy(cap.capillariesDescription);
		capillariesList.clear();
		for (Capillary ccap: cap.capillariesList) 
		{
			Capillary capi = new Capillary();
			capi.copy(ccap);
			capillariesList.add(capi);
		}
	}
	
	public boolean isPresent(Capillary capNew) 
	{
		boolean flag = false;
		for (Capillary cap: capillariesList) 
		{
			if (cap.getKymographName().contentEquals(capNew.getKymographName())) 
			{
				flag = true;
				break;
			}
		}
		return flag;
	}

	public void mergeLists(Capillaries caplist)  
	{
		for (Capillary capm : caplist.capillariesList ) 
		{
			if (!isPresent(capm))
				capillariesList.add(capm);
		}
	}
	
	public void adjustToImageWidth (int imageWidth) 
	{
		for (Capillary cap: capillariesList) 
		{
			cap.ptsTop.adjustToImageWidth(imageWidth);
			cap.ptsBottom.adjustToImageWidth(imageWidth);
			cap.ptsDerivative.adjustToImageWidth(imageWidth);
			cap.gulpsRois = null; // TODO: deal with gulps.. (simply remove?)
		}
	}
	
	public void cropToImageWidth (int imageWidth) 
	{
		for (Capillary cap: capillariesList) 
		{
			cap.ptsTop.cropToImageWidth(imageWidth);
			cap.ptsBottom.cropToImageWidth(imageWidth);
			cap.ptsDerivative.cropToImageWidth(imageWidth);
			cap.gulpsRois = null; // TODO: deal with gulps.. (simply remove?)
		}
	}

	public void transferDescriptionToCapillaries() 
	{
		for (Capillary cap: capillariesList) 
		{
			transferCapGroupCageIDToCapillary(cap);
			cap.setVolumeAndPixels (capillariesDescription.volume, capillariesDescription.pixels);
		}
	}
	
	private void transferCapGroupCageIDToCapillary (Capillary cap) 
	{
		if (capillariesDescription.grouping != 2)
			return;
		String	name = cap.getRoiName();
		String letter = name.substring(name.length() - 1);
		cap.capSide = letter;
		if (letter .equals("R")) 
		{	
			String nameL = name.substring(0, name.length() - 1) + "L";
			Capillary cap0 = getCapillaryFromRoiName(nameL);
			if (cap0 != null) 
			{
//				cap.capNFlies = cap0.capNFlies;
				cap.capCageID = cap0.capCageID;
			}
		}
	}
	
	public Capillary getCapillaryFromRoiName(String name) {
		Capillary capFound = null;
		for (Capillary cap: capillariesList) 
		{
			if (cap.getRoiName().equals(name)) 
			{
				capFound = cap;
				break;
			}
		}
		return capFound;
	}
	
	public Capillary getCapillaryFromKymographName(String name) {
		Capillary capFound = null;
		for (Capillary cap: capillariesList) 
		{
			if (cap.getKymographName().equals(name)) 
			{
				capFound = cap;
				break;
			}
		}
		return capFound;
	}

	public void updateCapillariesFromSequence(Sequence seq) 
	{
		List<ROI2D> listROISCap = ROI2DUtilities.getROIs2DContainingString ("line", seq);
		Collections.sort(listROISCap, new Comparators.ROI2D_Name_Comparator());
		for (Capillary cap: capillariesList) 
		{
			cap.valid = false;
			String capName = Capillary.replace_LR_with_12(cap.getRoiName());
			Iterator <ROI2D> iterator = listROISCap.iterator();
			while(iterator.hasNext()) 
			{ 
				ROI2D roi = iterator.next();
				String roiName = Capillary.replace_LR_with_12(roi.getName());
				if (roiName.equals (capName)) 
				{
					cap.setRoi((ROI2DShape) roi);
					cap.valid = true;
				}
				if (cap.valid) 
				{
					iterator.remove();
					break;
				}
			}
		}
		Iterator <Capillary> iterator = capillariesList.iterator();
		while (iterator.hasNext()) 
		{
			Capillary cap = iterator.next();
			if (!cap.valid )
				iterator.remove();
		}
		if (listROISCap.size() > 0) 
		{
			for (ROI2D roi: listROISCap) 
			{
				Capillary cap = new Capillary((ROI2DShape) roi);
				if (!isPresent(cap))
					capillariesList.add(cap);
			}
		}
		Collections.sort(capillariesList);
		return;
	}

	public void transferCapillaryRoiToSequence(Sequence seq) 
	{
		seq.removeAllROI();
		for (Capillary cap: capillariesList) 
		{
			seq.addROI(cap.getRoi());
		}
	}

	public void initCapillariesWith10Cages(int nflies)
	{
		int capArraySize = capillariesList.size();
		for (int i=0; i< capArraySize; i++)
		{
			Capillary cap = capillariesList.get(i);
			cap.capNFlies = nflies;
			if (i<= 1  || i>= capArraySize-2 )
				cap.capNFlies = 0;
			cap.capCageID = i/2;
		}
	}
	
	public void initCapillariesWith6Cages(int nflies) 
	{
		int capArraySize = capillariesList.size();
		for (int i=0; i< capArraySize; i++) 
		{
			Capillary cap = capillariesList.get(i);
			cap.capNFlies = 1;
			if (i <= 1 ) 
			{
				cap.capNFlies = 0;
				cap.capCageID = 0;
			}
			else if (i >= capArraySize-2 ) 
			{
				cap.capNFlies = 0;
				cap.capCageID = 5;
			}
			else 
			{
				cap.capNFlies = nflies;
				cap.capCageID = 1 + (i-2)/4;
			}
		}
	}
	
	// -------------------------------------------------
	
	public KymoIntervals getKymoIntervalsFromCapillaries() 
	{
		if (capillariesListTimeIntervals == null) 
		{
			capillariesListTimeIntervals = new KymoIntervals();
			
			for (Capillary cap: capillariesList) 
			{
				for (KymoROI2D roiFK: cap.getRoisForKymo()) 
				{
					Long[] interval = {roiFK.getStart(), (long) -1}; 
					capillariesListTimeIntervals.addIfNew(interval);
				}
			}
		}
		return capillariesListTimeIntervals;
	}
	
	public int addKymoROI2DInterval(long start) 
	{
		Long[] interval = {start, (long) -1};
		int item = capillariesListTimeIntervals.addIfNew(interval);
		
		for (Capillary cap: capillariesList) 
		{
			List<KymoROI2D> listROI2DForKymo = cap.getRoisForKymo();
			ROI2D roi = cap.getRoi();
			if (item>0 ) 
				roi = (ROI2D) listROI2DForKymo.get(item-1).getRoi().getCopy();
			listROI2DForKymo.add(item, new KymoROI2D(start, roi));
		}
		return item;
	}
	
	public void deleteKymoROI2DInterval(long start) 
	{
		capillariesListTimeIntervals.deleteIntervalStartingAt(start);
		for (Capillary cap: capillariesList) 
			cap.removeROI2DIntervalStartingAt(start);
	}
	
	public int findKymoROI2DIntervalStart(long intervalT) 
	{
		return capillariesListTimeIntervals.findStartItem(intervalT);
	}
	
	public long getKymoROI2DIntervalsStartAt(int selectedItem) 
	{
		return capillariesListTimeIntervals.get(selectedItem)[0];
	}
	
	public double getScalingFactorToPhysicalUnits(EnumXLSExportType xlsoption ) 
	{
		double scalingFactorToPhysicalUnits; 
		switch (xlsoption) 
		{
			case NBGULPS:
			case TTOGULP:
			case TTOGULP_LR:
			case AUTOCORREL:
			case CROSSCORREL:
			case CROSSCORREL_LR:
				scalingFactorToPhysicalUnits = 1.;
				break;
			default:
				scalingFactorToPhysicalUnits = capillariesDescription.volume / capillariesDescription.pixels;
				break;
		}
		return scalingFactorToPhysicalUnits;
	}
	
	public ROI2DPolygon get2DPolygonEnclosingCapillaries() {
		Rectangle  outerRectangle = null;
		for (Capillary cap : capillariesList) {
			Rectangle rect = cap.getRoi().getBounds();
			if (outerRectangle == null) {
				outerRectangle = rect;
			}
			else
				outerRectangle.add(rect);
			}
		Polygon2D polygon = new Polygon2D(outerRectangle);
		return new ROI2DPolygon(polygon);
	}
	
	public void deleteAllCapillaries() {
		capillariesList.clear();
	}
	
	// --------------------------------
	boolean csvLoadCapillariesData(String directory) throws Exception {
		String pathToCsv = directory + File.separator +"CapillariesMeasures.csv";
		File csvFile = new File(pathToCsv);
		if (!csvFile.isFile()) 
			return false;
		
		BufferedReader csvReader = new BufferedReader(new FileReader(pathToCsv));
		String row;
		while ((row = csvReader.readLine()) != null) {
		    String[] data = row.split("\t");
		    if (data[0] .equals( "#")) {
		    	switch(data[1]) {
		    	case "DESCRIPTION":
		    		csvLoadDescription (csvReader);
		    		break;
		    	case "CAPILLARIES":
		    		csvLoadCapillariesDescription (csvReader);
		    		break;
		    	case "TOPLEVEL":
		    		System.out.println("toplevel\n" );
		    		csvLoadCapillariesMeasures(csvReader, EnumCapillaryMeasureType.TOPLEVEL);
		    		break;
		    	case "BOTTOMLEVEL":
		    		System.out.println("bottomlevel\n" );
		    		csvLoadCapillariesMeasures(csvReader, EnumCapillaryMeasureType.BOTTOMLEVEL);
		    		break;
		    	case "TOPDERIVATIVE":
		    		System.out.println("derivative\n");
		    		csvLoadCapillariesMeasures(csvReader, EnumCapillaryMeasureType.TOPDERIVATIVE);
		    		break;
		    	case "GULPS":
		    		System.out.println("gulps\n");
		    		csvLoadCapillariesMeasures(csvReader, EnumCapillaryMeasureType.GULPS);
		    		break;
	    		default:
	    			break;
		    	}
		    }
		}
		csvReader.close();
		
		return true;
	}
	
	boolean csvSaveCapillariesData(String directory) {
		try {
			FileWriter csvWriter = new FileWriter(directory + File.separator +"CapillariesMeasures.csv");
			
			csvSaveCapillariesDescription(csvWriter);
			csvSaveCapillariesMeasures(csvWriter, EnumCapillaryMeasureType.TOPLEVEL);
			csvSaveCapillariesMeasures(csvWriter, EnumCapillaryMeasureType.BOTTOMLEVEL);
			csvSaveCapillariesMeasures(csvWriter, EnumCapillaryMeasureType.TOPDERIVATIVE);
			csvSaveCapillariesMeasures(csvWriter, EnumCapillaryMeasureType.GULPS);
			csvWriter.flush();
			csvWriter.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	boolean csvSaveCapillariesDescription(FileWriter csvWriter) {
		try {
			csvWriter.append(capillariesDescription.getCSVDescriptorHeader());
			csvWriter.append(capillariesDescription.getCSVDescriptorData());
			csvWriter.append("n caps=\t" + Integer.toString(capillariesList.size()) + "\n");
			csvWriter.append("#\t#\n");
			
			if (capillariesList.size() > 0) {
				csvWriter.append(capillariesList.get(0).getCSVDescriptorHeader());
				for (Capillary cap:capillariesList) 
					csvWriter.append(cap.getCSVDescriptorData());
				csvWriter.append("#\t#\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	String csvLoadCapillariesDescription (BufferedReader csvReader) {
		String row;
		try {
			row = csvReader.readLine();			
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split("\t");
				if (data[0] .equals( "#")) 
					return data[1];
				Capillary cap = getCapillaryFromKymographName(data[0]);
				if (cap == null)
					cap = new Capillary();
				cap.setCSVDescriptorData(data);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	String csvLoadDescription (BufferedReader csvReader) {
		String row;
		try {
			row = csvReader.readLine();
			row = csvReader.readLine();
			String[] data = row.split("\t");
			capillariesDescription.setCSVDescriptorData(data);
			row = csvReader.readLine();
			data = row.split("\t");
			if ( data[0].substring(0, Math.min( data[0].length(), 5)).equals("n cap")) {
				int ncapillaries = Integer.valueOf(data[1]);
				if (ncapillaries >= capillariesList.size())
					capillariesList.ensureCapacity(ncapillaries);
				else
					capillariesList.subList(ncapillaries, capillariesList.size()).clear();
				row = csvReader.readLine();
				data = row.split("\t");
			}
			if (data[0] .equals( "#")) {
			  	return data[1];
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	boolean csvSaveCapillariesMeasures(FileWriter csvWriter, EnumCapillaryMeasureType measureType) {
		try {
			if (capillariesList.size() <= 1)
				return false;
			
			csvWriter.append(capillariesList.get(0).getCSVCapillaryDataHeader(measureType));
			for (Capillary cap:capillariesList) {
				csvWriter.append(cap.getCSVCapillaryData(measureType, false, true));
			}
			csvWriter.append("#\t#\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	String csvLoadCapillariesMeasures(BufferedReader csvReader, EnumCapillaryMeasureType measureType) {
		String row;
		int [] dataIntN = null;
		int [] dataIntX = null;
		int [] dataIntY = null;
		try {
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split("\t");
				if (data[0] .equals( "#")) 
					return data[1];
				
				if (data[1].charAt(0) == 'N')
					dataIntN = csvStringArrayToInt(data);
				if (data[1].charAt(0) == 'X')
					dataIntX = csvStringArrayToInt(data);
				if (data[1].charAt(0) == 'Y') {
					dataIntY = csvStringArrayToInt(data);
					Capillary cap = getCapillaryFromKymographName(data[0]);
					if (cap == null)
						cap = new Capillary();
					System.out.println(cap.getRoiName() );
					cap.setCSVCapillaryData(measureType, dataIntN, dataIntX, dataIntY);
				}
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	int[] csvStringArrayToInt(String[] data) {
		int length = data.length - 2;
		int[] dataInt = new int[length];
		for (int i=2; i<length; i++)
			dataInt[i-2] = Integer.valueOf(data[i]);
		return dataInt;
	}


}