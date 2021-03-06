package plugins.fmp.multicafe2.tools.toExcel;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.multicafe2.dlg.JComponents.ExperimentCombo;
import plugins.fmp.multicafe2.experiment.Cage;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.Comparators;



public class XLSExport 
{
	protected XLSExportOptions 	options 			= null;
	protected Experiment 		expAll 				= null;

	XSSFCellStyle 				xssfCellStyle_red 	= null;
	XSSFCellStyle 				xssfCellStyle_blue 	= null;
    XSSFFont 					font_red 			= null;
    XSSFFont 					font_blue 			= null;
    XSSFWorkbook 				workbook			= null;		
    
	ExperimentCombo 			expList 			= null;
	List <XLSResults> 			rowListForOneExp 	= new ArrayList <XLSResults> ();


	// ------------------------------------------------
    	
	protected Point writeExperiment_descriptors(Experiment exp, String charSeries, XSSFSheet sheet, Point pt, EnumXLSExportType xlsExportOption) 
	{
		boolean transpose = options.transpose;
		int row = pt.y;
		int col0 = pt.x;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "..");
		pt.x++;
		int colseries = pt.x;
		for (int i = 0; i < 18; i++) 
		{
			XLSUtils.setValue(sheet, pt, transpose, "--");
			pt.x++;
		}
		pt.x = colseries;
		
		String filename = exp.getExperimentDirectory();
		if (filename == null)
			filename = exp.seqCamData.getImagesDirectory();
		Path path = Paths.get(filename);

		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		String date = df.format(exp.chainFirstImage_Ms);

		String name0 = path.toString();
		int pos = name0.indexOf("cam");
		String cam = "-"; 
		if (pos > 0) 
		{
			int pos5 = pos+5;
			if (pos5 >= name0.length())
				pos5 = name0.length() -1;
			cam = name0.substring(pos, pos5);
		}
		
		String sheetName = sheet.getSheetName();
		
		int rowmax = -1;
		for (EnumXLSColumnHeader dumb: EnumXLSColumnHeader.values()) 
		{
			if (rowmax < dumb.getValue())
				rowmax = dumb.getValue();		
		}
		
		List<Capillary> capList = exp.capillaries.capillariesArrayList;
		for (int t=0; t< capList.size(); t++) 
		{ 
			Capillary cap = capList.get(t);
			String	name = cap.roi.getName();
			int col = getRowIndexFromKymoFileName(name);
			if (col >= 0) 
				pt.x = colseries + col;
			int x = pt.x;
			int y = row;
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.PATH.getValue(), transpose, 		name0);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DATE.getValue(), transpose, 		date);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.BOXID.getValue(), transpose, 		exp.getField(EnumXLSColumnHeader.BOXID));
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.EXPT.getValue(), transpose, 		exp.getField(EnumXLSColumnHeader.EXPT));
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.COMMENT1.getValue(), transpose, 	exp.getField(EnumXLSColumnHeader.COMMENT1));
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.STRAIN.getValue(), transpose, 	exp.getField(EnumXLSColumnHeader.STRAIN));
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.SEX.getValue(), transpose, 		exp.getField(EnumXLSColumnHeader.SEX));			

			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAP.getValue(), transpose, cap.getSideDescriptor(xlsExportOption));
			desc_setValueDataOption(sheet, xlsExportOption, cap, transpose, x, y);

			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAM.getValue(), transpose, 		cam);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAGEINDEX.getValue(), transpose, 	cap.capCageID);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAGEID.getValue(), transpose, 	charSeries+cap.capCageID);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPVOLUME.getValue(), transpose, 	exp.capillaries.desc.volume);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPPIXELS.getValue(), transpose, 	exp.capillaries.desc.pixels);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.COMMENT2.getValue(), transpose, 	exp.getField(EnumXLSColumnHeader.COMMENT2));
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.NFLIES.getValue(), transpose, 	cap.capNFlies); 
			if (exp.cages.cageList.size() > cap.capCageID) 
			{
				Cage cage = exp.cages.cageList.get(cap.capCageID);
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM4.getValue(), transpose, cage.strCageComment + "/"+ cage.strCageStrain + "/" + cage.strCageSex + "/" + cage.cageAge );
			} 
			else 
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.DUM4.getValue(), transpose, sheetName);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAGECOMMENT.getValue(), transpose, desc_getChoiceTestType(capList, t));
		}
		pt.x = col0;
		pt.y = rowmax +1;
		return pt;
	}
	
	private String desc_getChoiceTestType(List<Capillary> capList, int t)
	{
		Capillary cap = capList.get(t);
		String choiceText = "..";
		String side = cap.getCapillarySide();
		if (side.contains("L"))
			t = t+1;
		else
			t = t-1;
		if (t >= 0 && t < capList.size()) {
			Capillary othercap = capList.get(t);
			String otherSide = othercap.getCapillarySide();
			if (!otherSide .contains(side))
			{
				if (cap.capStimulus.equals(othercap.capStimulus)
					&& cap.capConcentration.equals(othercap.capConcentration))
					choiceText  = "no-choice";
				else
					choiceText = "choice";
			}
		}
		return choiceText;
	}
	
	private void desc_setValueDataOption(XSSFSheet sheet, EnumXLSExportType xlsExportOption, Capillary cap, boolean transpose, int x, int y)
	{
		switch (xlsExportOption) {
		case TOPLEVEL_LR:
		case TOPLEVELDELTA_LR:
		case SUMGULPS_LR:
			if (cap.getCapillarySide().equals("L")) 
			{
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPSTIM.getValue(), transpose, "L+R");
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPCONC.getValue(), transpose, cap.capStimulus + ": "+ cap.capConcentration);
			} 
			else 
			{
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPSTIM.getValue(), transpose, "(L-R)/(L+R)");
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPCONC.getValue(), transpose, cap.capStimulus + ": "+ cap.capConcentration);
			}
			break;
		case TTOGULP_LR:
			if (cap.getCapillarySide().equals("L")) 
			{
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPSTIM.getValue(), transpose, "min_t_to_gulp");
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPCONC.getValue(), transpose, cap.capStimulus + ": "+ cap.capConcentration);
			} 
			else 
			{
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPSTIM.getValue(), transpose, "max_t_to_gulp");
				XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPCONC.getValue(), transpose, cap.capStimulus + ": "+ cap.capConcentration);
			}
			break;
		default:
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPSTIM.getValue(), transpose, 	cap.capStimulus);
			XLSUtils.setValue(sheet, x, y+EnumXLSColumnHeader.CAPCONC.getValue(), transpose, 	cap.capConcentration);	
			break;
		}
	}
	
	int writeTopRow_descriptors(XSSFSheet sheet) 
	{		
		Point pt = new Point(0,0);
		int x = 0;
		boolean transpose = options.transpose;
		int nextcol = -1;
		for (EnumXLSColumnHeader dumb: EnumXLSColumnHeader.values()) 
		{
			XLSUtils.setValue(sheet, x, dumb.getValue(), transpose, dumb.getName());
			if (nextcol < dumb.getValue())
				nextcol = dumb.getValue();
		}
		pt.y = nextcol+1;
		return pt.y;
	}
	
	void writeTopRow_timeIntervals(XSSFSheet sheet, int row) 
	{
		boolean transpose = options.transpose;
		Point pt = new Point(0, row);
		long duration = expAll.camLastImage_Ms - expAll.camFirstImage_Ms;
		long interval = 0;
		while (interval < duration) 
		{
			int i = (int) (interval / options.buildExcelUnitMs);
			XLSUtils.setValue(sheet, pt, transpose, "t"+i);
			pt.y++;
			interval += options.buildExcelStepMs;
		}
	}
	
	protected int desc_getCageFromCapillaryName(String name) 
	{
		if (!name .contains("line"))
			return -1;
		String num = name.substring(4, 5);
		int numFromName = Integer.valueOf(num);
		return numFromName;
	}
	
	protected int getRowIndexFromKymoFileName(String name) 
	{
		if (!name .contains("line"))
			return -1;
		String num = name.substring(4, 5);
		int numFromName = Integer.valueOf(num);
		if( name.length() > 5) 
		{
			String side = name.substring(5, 6);
			if (side != null) 
			{
				if (side .equals("R")) 
				{
					numFromName = numFromName* 2;
					numFromName += 1;
				}
				else if (side .equals("L"))
					numFromName = numFromName* 2;
			}
		}
		return numFromName;
	}
		
	protected int getRowIndexFromCageName(String name) 
	{
		if (!name .contains("cage"))
			return -1;
		String num = name.substring(4, name.length());
		int numFromName = Integer.valueOf(num);
		return numFromName;
	}
	
	protected Point getCellXCoordinateFromDataName(XLSResults xlsResults, Point pt_main, int colseries) 
	{
		int col = getRowIndexFromKymoFileName(xlsResults.name);
		if (col >= 0)
			pt_main.x = colseries + col;
		return pt_main;
	}
	
	protected int getCageFromKymoFileName(String name) 
	{
		if (!name .contains("line"))
			return -1;
		return Integer.valueOf(name.substring(4, 5));
	}
	
	XSSFWorkbook xlsInitWorkbook() 
	{
		XSSFWorkbook workbook = new XSSFWorkbook(); 
		workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
		xssfCellStyle_red = workbook.createCellStyle();
	    font_red = workbook.createFont();
	    font_red.setColor(HSSFColor.HSSFColorPredefined.RED.getIndex());
	    xssfCellStyle_red.setFont(font_red);
	    
		xssfCellStyle_blue = workbook.createCellStyle();
	    font_blue = workbook.createFont();
	    font_blue.setColor(HSSFColor.HSSFColorPredefined.BLUE.getIndex());
	    xssfCellStyle_blue.setFont(font_blue);
	    return workbook;
	}
	
	XSSFSheet xlsInitSheet(String title) 
	{
		XSSFSheet sheet = workbook.getSheet(title);
		if (sheet == null) 
		{
			sheet = workbook.createSheet(title);
			int row = writeTopRow_descriptors(sheet);
			writeTopRow_timeIntervals(sheet, row);
		}
		return sheet;
	}
	
	protected int getDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExportType xlsOption) 
	{	
		getCapDataFromOneExperimentSeries(exp, xlsOption);
		XSSFSheet sheet = xlsInitSheet(xlsOption.toString());
		int colmax = xlsExportResultsArrayToSheet(sheet, xlsOption, col0, charSeries);
		
		if (options.onlyalive) 
		{
			trimDeadsFromArrayList(exp);
			sheet = xlsInitSheet(xlsOption.toString()+"_alive");
			xlsExportResultsArrayToSheet(sheet, xlsOption, col0, charSeries);
		}
		
		if (options.cage) 
		{
			combineData(exp);
			sheet = xlsInitSheet(xlsOption.toString()+"_cage");
			xlsExportResultsArrayToSheet(sheet, xlsOption, col0, charSeries);
		}
		
		return colmax;
	}
		
	private void getDescriptorsForOneExperiment( Experiment exp, EnumXLSExportType xlsOption) 
	{
		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.cages.copy(exp.cages);
		expAll.capillaries.copy(exp.capillaries);
		expAll.chainFirstImage_Ms = exp.chainFirstImage_Ms;
		expAll.setField(EnumXLSColumnHeader.BOXID, exp.getField(EnumXLSColumnHeader.BOXID));
		expAll.setField(EnumXLSColumnHeader.EXPT, exp.getField(EnumXLSColumnHeader.EXPT));
		expAll.setField(EnumXLSColumnHeader.COMMENT1, exp.getField(EnumXLSColumnHeader.COMMENT1));
		expAll.setField(EnumXLSColumnHeader.COMMENT2, exp.getField(EnumXLSColumnHeader.COMMENT2));	
		expAll.setField(EnumXLSColumnHeader.STRAIN, exp.getField(EnumXLSColumnHeader.STRAIN));
		expAll.setField(EnumXLSColumnHeader.SEX, exp.getField(EnumXLSColumnHeader.SEX));
		expAll.setExperimentDirectory(exp.getExperimentDirectory());
		
		Experiment expi = exp.chainToNext;
		while (expi != null ) 
		{
			expAll.capillaries.mergeLists(expi.capillaries);
			expi = expi.chainToNext;
		}

		int nFrames = (int) ((expAll.camLastImage_Ms - expAll.camFirstImage_Ms)/options.buildExcelStepMs  +1) ;
		int ncapillaries = expAll.capillaries.capillariesArrayList.size();
		rowListForOneExp = new ArrayList <XLSResults> (ncapillaries);
		for (int i=0; i< ncapillaries; i++) 
		{
			Capillary cap 		= expAll.capillaries.capillariesArrayList.get(i);
			XLSResults row 		= new XLSResults (cap.roi.getName(), cap.capNFlies, xlsOption, nFrames);
			row.stimulus 		= cap.capStimulus;
			row.concentration 	= cap.capConcentration;
			row.cageID 			= cap.capCageID;
			rowListForOneExp.add(row);
		}
		Collections.sort(rowListForOneExp, new Comparators.XLSResults_Name_Comparator());
	}
	
	private EnumXLSExportType getMeasureOption (EnumXLSExportType xlsOption) 
	{
		EnumXLSExportType measureOption = null;
		switch (xlsOption) 
		{
		case TOPRAW:
		case TOPLEVEL_LR:
		case TOPLEVELDELTA:
		case TOPLEVELDELTA_LR:
			measureOption = EnumXLSExportType.TOPLEVEL;
			break;
		case SUMGULPS_LR:
			measureOption = EnumXLSExportType.SUMGULPS;
			break;
		default:
			measureOption = xlsOption;
			break;
		}
		return measureOption;
	}
	
	private void getCapDataFromOneExperimentSeries(Experiment exp, EnumXLSExportType xlsOption) 
	{	
		getDescriptorsForOneExperiment (exp, xlsOption);
		Experiment expi = exp.getFirstChainedExperiment(true); 
		
		EnumXLSExportType measureOption = getMeasureOption (xlsOption);
		while (expi != null) 
		{
			int nOutputFrames = (int) ((expi.offsetLastCol_Ms - expi.offsetFirstCol_Ms) / options.buildExcelStepMs +1);
			if (nOutputFrames <= 1) 
			{
				if (expi.seqKymos.imageWidthMax == 0)
					expi.loadKymographs();
				expi.offsetLastCol_Ms = expi.offsetFirstCol_Ms + expi.seqKymos.imageWidthMax * expi.kymoBinCol_Ms;
				nOutputFrames = (int) ((expi.offsetLastCol_Ms - expi.offsetFirstCol_Ms) / options.buildExcelStepMs +1);
				
				if (nOutputFrames <= 1) 
				{
					nOutputFrames = expi.seqCamData.nTotalFrames;
					String error = "ERROR in "+ expi.getExperimentDirectory() 
							+ "\n nOutputFrames="+ nOutputFrames 
							+ " kymoFirstCol_Ms=" + expi.offsetFirstCol_Ms 
							+ " kymoLastCol_Ms=" + expi.offsetLastCol_Ms;
					System.out.println(error);
					continue;
				}
			}

			if (nOutputFrames > 1)
			{
				XLSResultsArray resultsArrayList = new XLSResultsArray (expi.capillaries.capillariesArrayList.size());
				switch (xlsOption) {
//					case TOPRAW:
					case BOTTOMLEVEL:
					case ISGULPS:
					case TTOGULP:
					case TTOGULP_LR:
						for (Capillary cap: expi.capillaries.capillariesArrayList) 
						{
							resultsArrayList.checkIfSameStimulusAndConcentration(cap);
							XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsOption, nOutputFrames);
							results.data = cap.getMeasures(measureOption, exp.kymoBinCol_Ms, options.buildExcelStepMs);
							resultsArrayList.add(results);
						}
						break;
						
					case TOPRAW:
						for (Capillary cap: expi.capillaries.capillariesArrayList) 
						{
							resultsArrayList.checkIfSameStimulusAndConcentration(cap);
							XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsOption, nOutputFrames);
							results.data = cap.getMeasures(measureOption, exp.kymoBinCol_Ms, options.buildExcelStepMs);
							if (options.t0) 
								results.data = exp.seqKymos.subtractT0(results.data);
							resultsArrayList.add(results);
						}
						break;
						
					case TOPLEVEL:
					case TOPLEVEL_LR:
					case TOPLEVELDELTA:
					case TOPLEVELDELTA_LR:
						for (Capillary cap: expi.capillaries.capillariesArrayList) 
						{
							resultsArrayList.checkIfSameStimulusAndConcentration(cap);
							XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsOption, nOutputFrames);
							results.data = cap.getMeasures(measureOption, exp.kymoBinCol_Ms, options.buildExcelStepMs);
							if (options.t0) 
								results.data = exp.seqKymos.subtractT0(results.data);
							resultsArrayList.add(results);
						}
						if (options.subtractEvaporation)
							resultsArrayList.subtractEvaporation();
						break;
						
					case DERIVEDVALUES:
					case SUMGULPS:
					case SUMGULPS_LR:
						for (Capillary cap: expi.capillaries.capillariesArrayList) 
						{
							resultsArrayList.checkIfSameStimulusAndConcentration(cap);
							XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsOption, nOutputFrames);
							results.data = cap.getMeasures(measureOption, exp.kymoBinCol_Ms, options.buildExcelStepMs);
							resultsArrayList.add(results);
						}
						if (options.subtractEvaporation)
							resultsArrayList.subtractEvaporation();
						break;
					default:
						break;
				}
				addResultsTo_rowsForOneExp(expi, resultsArrayList);
			}
			expi = expi.chainToNext;
		}
		
		switch (xlsOption) 
		{
			case TOPLEVELDELTA:
			case TOPLEVELDELTA_LR:
				for (XLSResults row: rowListForOneExp ) 
					row.subtractDeltaT(1, 1); //options.buildExcelStepMs);
//					row.subtractDeltaT(expAll.getKymoFrameStep(), options.buildExcelStepMs);
				break;
			default:
				break;
		}
	}
	
	private XLSResults getResultsArrayWithThatName(String testname, XLSResultsArray resultsArrayList) 
	{
		XLSResults resultsFound = null;
		for (XLSResults results: resultsArrayList.resultsArrayList) 
		{
			if (results.name.equals(testname)) 
			{
				resultsFound = results;
				break;
			}
		}
		return resultsFound;
	}
	
	private void addResultsTo_rowsForOneExp(Experiment expi, XLSResultsArray resultsArrayList) 
	{
		if (resultsArrayList.resultsArrayList.size() <1)
			return;
		EnumXLSExportType xlsoption = resultsArrayList.get(0).exportType;
		double scalingFactorToPhysicalUnits = expi.capillaries.desc.volume / expi.capillaries.desc.pixels;
		switch (xlsoption) 
		{
			case ISGULPS:
			case TTOGULP:
			case TTOGULP_LR:
				scalingFactorToPhysicalUnits = 1.;
				break;
			default:
				break;
		}
		
		long offsetChain = expi.camFirstImage_Ms - expi.chainFirstImage_Ms;
		long start_Ms = expi.offsetFirstCol_Ms + offsetChain; // TODO check when collate?
		long end_Ms = expi.offsetLastCol_Ms + offsetChain;
		if (options.fixedIntervals) 
		{
			if (start_Ms < options.startAll_Ms)
				start_Ms = options.startAll_Ms;
			if (start_Ms > expi.camLastImage_Ms)
				return;
			
			if (end_Ms > options.endAll_Ms)
				end_Ms = options.endAll_Ms;
			if (end_Ms > expi.camFirstImage_Ms)
				return;
		}
		
		// TODO check this 
		final long from_first_Ms = start_Ms - offsetChain;
		final long from_lastMs = end_Ms - offsetChain;
		final int to_first_index = (int) (start_Ms / options.buildExcelStepMs) ;
		final int to_nvalues = (int) ((end_Ms - start_Ms)/options.buildExcelStepMs)+1;
		
		for (XLSResults row: rowListForOneExp ) 
		{
			XLSResults results = getResultsArrayWithThatName(row.name,  resultsArrayList);
			if (results != null && results.data != null) 
			{
				double dvalue = 0.;
				switch (xlsoption) 
				{
					case TOPLEVEL:
					case TOPLEVEL_LR:
					case SUMGULPS:
					case SUMGULPS_LR:
					case TOPLEVELDELTA:
					case TOPLEVELDELTA_LR:
						if (options.collateSeries && options.padIntervals && expi.chainToPrevious != null) 
							dvalue = padWithLastPreviousValue(row, to_first_index);
						break;
					default:
						break;
				}

				int icolTo = 0;
				if (options.collateSeries || options.absoluteTime)
					icolTo = to_first_index;
				for (long fromTime = from_first_Ms; fromTime <= from_lastMs; fromTime += options.buildExcelStepMs, icolTo++) 
				{
					int from_i = (int) Math.round(((double)(fromTime - from_first_Ms)) / ((double) options.buildExcelStepMs));
					if (from_i >= results.data.size())
						break;
					// TODO check how this can happen
					if (from_i< 0)
						continue;
					double value = results.data.get(from_i) * scalingFactorToPhysicalUnits + dvalue;
					if (icolTo >= row.values_out.length)
						break;
					row.values_out[icolTo]= value;
				}

			} 
			else 
			{
				if (options.collateSeries && options.padIntervals && expi.chainToPrevious != null) 
				{
					double dvalue = padWithLastPreviousValue(row, to_first_index);
					int tofirst = (int) to_first_index;
					int tolast = (int) (tofirst + to_nvalues);
					if (tolast > row.values_out.length)
						tolast = row.values_out.length;
					for (int toi = tofirst; toi < tolast; toi++) 
						row.values_out[toi]= dvalue;
				}
			}
		}
	}
	
	private double padWithLastPreviousValue(XLSResults row, long to_first_index) 
	{
		double dvalue = 0;
		if (to_first_index >= row.values_out.length)
			return dvalue;
		
		int index = getIndexOfFirstNonEmptyValueBackwards(row, to_first_index);
		if (index >= 0) 
		{
			dvalue = row.values_out[index];
			for (int i=index+1; i< to_first_index; i++) 
			{
				row.values_out[i] = dvalue;
				row.padded_out[i] = true;
			}
		}
		return dvalue;
	}
	
	private int getIndexOfFirstNonEmptyValueBackwards(XLSResults row, long fromindex) 
	{
		int index = -1;
		int ifrom = (int) fromindex;
		for (int i= ifrom; i>= 0; i--) 
		{
			if (!Double.isNaN(row.values_out[i])) 
			{
				index = i;
				break;
			}
		}
		return index;
	}
	
	private void trimDeadsFromArrayList(Experiment exp) 
	{
		for (Cage cage: exp.cages.cageList) 
		{
			String roiname = cage.cageRoi.getName();
			if (roiname.length() < 4 || !roiname.substring( 0 , 4 ).contains("cage"))
				continue;
			
			String cagenumberString = roiname.substring(4);		
			int cagenumber = Integer.valueOf(cagenumberString);
			int ilastalive = 0;
			if (cage.cageNFlies > 0) 
			{
				Experiment expi = exp;
				while (expi.chainToNext != null && expi.chainToNext.cages.isFlyAlive(cagenumber)) 
				{
					expi = expi.chainToNext;
				}
				int lastIntervalFlyAlive = expi.cages.getLastIntervalFlyAlive(cagenumber);
				int lastMinuteAlive = (int) (lastIntervalFlyAlive * expi.camBinImage_Ms 
						+ (expi.camFirstImage_Ms - expAll.camFirstImage_Ms));		
				ilastalive = (int) (lastMinuteAlive / expAll.kymoBinCol_Ms);
			}
			if (ilastalive > 0)
				ilastalive += 1;
			
			for (XLSResults row : rowListForOneExp) 
			{
				if (desc_getCageFromCapillaryName (row.name) == cagenumber)
					row.clearValues(ilastalive);
			}
		}	
	}
	
	private void combineData(Experiment exp) 
	{
		for (XLSResults row_master : rowListForOneExp) 
		{
			if (row_master.nflies == 0 || row_master.values_out == null)
				continue;
			for (XLSResults row : rowListForOneExp) 
			{
				if (row.nflies == 0 || row.values_out == null)
					continue;
				if (row.cageID != row_master.cageID)
					continue;
				if (row.name .equals(row_master.name))
					continue;
				if (row.stimulus .equals(row_master.stimulus) && row.concentration .equals(row_master.concentration)) 
				{
					row_master.addValues_out(row);
					row.clearAll();
				}
			}
		}
	}
	
	private int xlsExportResultsArrayToSheet(XSSFSheet sheet, EnumXLSExportType xlsExportOption, int col0, String charSeries) 
	{
		Point pt = new Point(col0, 0);
		writeExperiment_descriptors(expAll, charSeries, sheet, pt, xlsExportOption);
		pt = writeExperiment_data(sheet, xlsExportOption, pt);
		return pt.x;
	}
			
	private Point writeExperiment_data (XSSFSheet sheet, EnumXLSExportType option, Point pt_main) 
	{
		int rowSeries = pt_main.x +2;
		int column_dataArea = pt_main.y;
		Point pt = new Point(pt_main);
		switch (option) 
		{
			case TOPLEVEL_LR:
			case TOPLEVELDELTA_LR:
			case SUMGULPS_LR:
				writeExperiment_data_LRRows(sheet, column_dataArea, rowSeries, pt);
				break;
			case TTOGULP_LR:
				writeExperiment_data_TOGulpLR(sheet, column_dataArea, rowSeries, pt);
				break;
			default:
				writeExperiment_data_simpleRows(sheet, column_dataArea, rowSeries, pt);
				break;
		}			
		pt_main.x = pt.x+1;
		return pt_main;
	}
		
	private XLSResults getNextRow(XLSResults rowL, int irow) 
	{
		int cageL = getCageFromKymoFileName(rowL.name);
		XLSResults rowR = null;
		if (irow+1 < rowListForOneExp.size()) 
		{
			rowR = rowListForOneExp.get(irow+1);
			int cageR = getCageFromKymoFileName(rowR.name);
			if (cageR != cageL) 
				rowR = null;
		}
		return rowR;
	}
	
	private void writeExperiment_data_simpleRows(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt) 
	{
		for (XLSResults row: rowListForOneExp) 
		{
			writeRow(sheet, column_dataArea, rowSeries, pt, row);
		}
		
	}
	
	private void writeExperiment_data_LRRows(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt) 
	{
		for (int irow = 0; irow < rowListForOneExp.size(); irow ++) 
		{
			XLSResults rowL = rowListForOneExp.get(irow); 			
			XLSResults rowR = getNextRow (rowL, irow);
			if (rowR != null) 
			{
				irow++;
				XLSResults sumResults = new XLSResults(rowL.name, rowL.nflies, rowL.exportType, rowL.dimension);
				sumResults.getSumLR(rowL, rowR);
				writeRow(sheet, column_dataArea, rowSeries, pt, sumResults);
				
				XLSResults ratioResults = new XLSResults(rowR.name, rowL.nflies, rowL.exportType, rowL.dimension);
				ratioResults.getRatioLR(rowL, rowR);
				writeRow(sheet, column_dataArea, rowSeries, pt, ratioResults);
			} 
			else 
			{
				writeRow(sheet, column_dataArea, rowSeries, pt, rowL);
			}
		}
	}
	
	private void writeExperiment_data_TOGulpLR(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt) 
	{
		for (int irow = 0; irow < rowListForOneExp.size(); irow ++) 
		{
			XLSResults rowL = rowListForOneExp.get(irow);
			XLSResults rowR = getNextRow (rowL, irow);
			if (rowR != null) 
			{
				irow++;
				int len = rowL.values_out.length;
				if (rowR.values_out.length > len)
					len = rowR.values_out.length;
			
				XLSResults maxResults = new XLSResults(rowL.name, rowL.nflies, rowL.exportType, len);	
				maxResults.getMaxTimeToGulpLR(rowL, rowR);
				writeRow(sheet, column_dataArea, rowSeries, pt, maxResults);
				
				XLSResults minResults = new XLSResults(rowR.name, rowL.nflies, rowL.exportType, len);	
				minResults.getMinTimeToGulpLR(rowL, rowR);
				writeRow(sheet, column_dataArea, rowSeries, pt, minResults);
			} 
			else 
			{
				writeRow(sheet, column_dataArea, rowSeries, pt, rowL);
			}
		}
	}
	
	private void writeRow(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt, XLSResults row) 
	{
		boolean transpose = options.transpose;
		pt.y = column_dataArea;
		int col = getRowIndexFromKymoFileName(row.name);
		pt.x = rowSeries + col; 
		if (row.values_out == null)
			return;
		for (long coltime=expAll.camFirstImage_Ms; coltime < expAll.camLastImage_Ms; coltime+=options.buildExcelStepMs, pt.y++) 
		{
			int i_from = (int) ((coltime-expAll.camFirstImage_Ms) / options.buildExcelStepMs);
			if (i_from >= row.values_out.length) 
				break;
			double value = row.values_out[i_from];
			if (!Double.isNaN(value)) 
			{
				XLSUtils.setValue(sheet, pt, transpose, value);
				if (i_from < row.padded_out.length && row.padded_out[i_from])
					XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
			}
		}
		pt.x++;
	}

}
