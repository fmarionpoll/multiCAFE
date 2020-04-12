package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;





public class XLSExportCapillariesResults2  extends XLSExport {
	ExperimentList expList = null;
	List <XLSCapillaryResults> rowsForOneExp = new ArrayList <XLSCapillaryResults> ();

	// ----------------------
	
	public void exportToFile(String filename, XLSExportOptions opt) {	
		System.out.println("XLS capillary measures output");
		options = opt;
		expList = options.expList;

		boolean loadCapillaries = true;
		boolean loadDrosoTrack = options.onlyalive;
		// get time first and last of each experiment, plus get 
		expList.loadAllExperiments(loadCapillaries, loadDrosoTrack);
		// chain/unchain experiments
		expList.chainExperiments(options.collateSeries);
		// store parameters common to all experiments into expAll (intervals, step)
		expAll = expList.getStartAndEndFromAllExperiments(options);
		
		expAll.stepFrame = expList.getExperiment(0).stepFrame * options.buildExcelBinStep;
		expAll.startFrame = (int) expAll.fileTimeImageFirstMinute;
		expAll.endFrame = (int) expAll.fileTimeImageLastMinute;
		expAll.number_of_frames = (int) (expAll.endFrame - expAll.startFrame)/expAll.stepFrame +1;

		try { 
			XSSFWorkbook workbook = xlsInitWorkbook();
		    
			int column = 1;
			int iSeries = 0;
			
			ProgressFrame progress = new ProgressFrame("Export data to Excel");
			int nbexpts = expList.getSize();
			progress.setLength(nbexpts);
			
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = expList.getExperiment(index);
				if (exp.previousExperiment != null)
					continue;
				
				progress.setMessage("Export experiment "+ (index+1) +" of "+ nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);
				if (options.topLevel) 		
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportType.TOPLEVEL);
				if (options.sum && options.topLevel) 		
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportType.TOPLEVEL_LR);
				if (options.topLevelDelta) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportType.TOPLEVELDELTA);
				if (options.sum && options.topLevelDelta) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportType.TOPLEVELDELTA_LR);
				if (options.consumption) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportType.SUMGULPS);
				if (options.sum && options.consumption) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportType.SUMGULPS_LR);

				if (options.bottomLevel) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportType.BOTTOMLEVEL);		
				if (options.derivative) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportType.DERIVEDVALUES);	
				
				if (!options.collateSeries || exp.previousExperiment == null)
					column += expList.maxSizeOfCapillaryArrays +2;
				iSeries++;
				progress.incPosition();
			}
			
			progress.setMessage( "Save Excel file to disk... ");
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
	        progress.close();
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}
	
	private int getDataAndExport(Experiment exp, XSSFWorkbook workbook, int col0, String charSeries, EnumXLSExportType datatype) {	
		getDataFromOneSeriesOfExperiments(exp, datatype);
		XSSFSheet sheet = xlsInitSheet(workbook, datatype.toString());
		int colmax = xlsExportResultsArrayToSheet(sheet, datatype, col0, charSeries);
		if (options.onlyalive) {
			trimDeadsFromArrayList(exp);
			sheet = xlsInitSheet(workbook, datatype.toString()+"_alive");
			xlsExportResultsArrayToSheet(sheet, datatype, col0, charSeries);
		}
		return colmax;
	}
	
	private void getDataFromOneSeriesOfExperiments(Experiment exp, EnumXLSExportType xlsoption) {	
		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.capillaries.copy(exp.capillaries);
		expAll.fileTimeImageFirst = exp.fileTimeImageFirst;
		expAll.fileTimeImageLast = exp.fileTimeImageLast;
		expAll.experimentFileName = exp.experimentFileName;
		expAll.boxID 		= exp.boxID;
		expAll.experiment 	= exp.experiment;
		expAll.comment1 	= exp.comment1;
		expAll.comment2 	= exp.comment2;

		Experiment expi = exp.nextExperiment;
		while (expi != null ) {
			expAll.capillaries.mergeLists(expi.capillaries);
			expAll.fileTimeImageLast = expi.fileTimeImageLast;
			expi= expi.nextExperiment;
		}
		expAll.fileTimeImageFirstMinute = expAll.fileTimeImageFirst.toMillis()/60000;
		expAll.fileTimeImageLastMinute = expAll.fileTimeImageLast.toMillis()/60000;
		
		int ncapillaries = expAll.capillaries.capillariesArrayList.size();
		rowsForOneExp = new ArrayList <XLSCapillaryResults> (ncapillaries);
		for (int i=0; i< ncapillaries; i++) {
			Capillary cap = expAll.capillaries.capillariesArrayList.get(i);
			XLSCapillaryResults row = new XLSCapillaryResults (cap.capillaryRoi.getName(), xlsoption);
			row.initValuesArray(expAll.number_of_frames);
			rowsForOneExp.add(row);
		}
		Collections.sort(rowsForOneExp, new Comparators.XLSCapillaryResultsComparator());
				
		// load data for one experiment - assume that exp = first experiment in the chain and iterate through the chain
		expi = exp;
		while (expi != null) {
			List <XLSCapillaryResults> resultsArrayList = new ArrayList <XLSCapillaryResults> (expi.capillaries.capillariesArrayList.size());
			for (Capillary cap: expi.capillaries.capillariesArrayList) {
				XLSCapillaryResults results = new XLSCapillaryResults(cap.capillaryRoi.getName(), xlsoption);
				switch (xlsoption) {
					case TOPLEVEL:
					case TOPLEVEL_LR:
						if (options.t0) 
							results.data = exp.seqKymos.subtractT0(cap.getMeasures(EnumListType.topLevel));
						else
							results.data = cap.getMeasures(EnumListType.topLevel);
						break;
					case TOPLEVELDELTA:
					case TOPLEVELDELTA_LR:
						results.data = exp.seqKymos.subtractTdelta(cap.getMeasures(EnumListType.topLevel), options.buildExcelBinStep);
						break;
					case DERIVEDVALUES:
						results.data = cap.getMeasures(EnumListType.derivedValues);
						break;
					case SUMGULPS:
					case SUMGULPS_LR:
						results.data = cap.getMeasures(EnumListType.cumSum);
						break;
					case BOTTOMLEVEL:
						results.data = cap.getMeasures(EnumListType.bottomLevel);
						break;
					default:
						break;
				}
				resultsArrayList.add(results);
			}
			// here add resultsArrayList to expAll
			addResultsTo_rowsForOneExp(expi, resultsArrayList);
			expi = expi.nextExperiment;
		}
	}
	
	private void addResultsTo_rowsForOneExp(Experiment expi, List <XLSCapillaryResults> resultsArrayList) {
		EnumXLSExportType xlsoption = resultsArrayList.get(0).exportType;
		double scalingFactorToPhysicalUnits = expi.capillaries.desc.volume / expi.capillaries.desc.pixels;
		int transfer_first_index = (int) (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute) / expAll.stepFrame ;
		int transfer_nvalues = (int) ((expi.fileTimeImageLastMinute - expi.fileTimeImageFirstMinute)/expAll.stepFrame)+1;
		
		for (XLSCapillaryResults row: rowsForOneExp ) {
			boolean found = false;
			for (XLSCapillaryResults results: resultsArrayList) {
				if (!results.name.equals(row.name))
					continue;
				found = true;
				double dvalue = 0;
				switch (xlsoption) {
					case TOPLEVEL:
					case TOPLEVEL_LR:
					case SUMGULPS:
					case SUMGULPS_LR:
						if (options.collateSeries && options.padIntervals && expi.previousExperiment != null) 
							dvalue = padWithLastPreviousValue(row, transfer_first_index);
						break;
					case DERIVEDVALUES:
					case TOPLEVELDELTA:
					case TOPLEVELDELTA_LR:
					case BOTTOMLEVEL:
					default:
						break;
				}
				int tofirst = transfer_first_index;
				int tolast = tofirst + transfer_nvalues;
				int fromi = 0;
				for (int toi = tofirst; toi < tolast; toi++) {
					if (results.data == null || fromi >= results.data.size())
						break;
					row.values_out[toi]= results.data.get(fromi) * scalingFactorToPhysicalUnits + dvalue;
					fromi += options.buildExcelBinStep;
				}
				break;
			}
			if (!found) {
				if (options.collateSeries && options.padIntervals && expi.previousExperiment != null) {
					double dvalue = padWithLastPreviousValue(row, transfer_first_index);
					int tofirst = transfer_first_index;
					int tolast = tofirst + transfer_nvalues;
					for (int toi = tofirst; toi < tolast; toi++) {
						row.values_out[toi]= dvalue;
					}
				}
			}
		}
	}
	
	private double padWithLastPreviousValue(XLSCapillaryResults row, int transfer_first_index) {
		double dvalue = 0;
		int index = getIndexOfFirstNonEmptyValueBackwards(row, transfer_first_index);
		if (index >= 0) {
			dvalue = row.values_out[index];
			for (int i=index+1; i< transfer_first_index; i++) {
				row.values_out[i] = dvalue;
				row.padded_out[i] = true;
			}
		}
		return dvalue;
	}
	
	private int getIndexOfFirstNonEmptyValueBackwards(XLSCapillaryResults row, int fromindex) {
		int index = -1;
		for (int i= fromindex; i>= 0; i--) {
			if (!Double.isNaN(row.values_out[i])) {
				index = i;
				break;
			}
		}
		return index;
	}
	
	private void trimDeadsFromArrayList(Experiment exp) {
	for (Cage cage: exp.cages.cageList) {
			String cagenumberString = cage.cageLimitROI.getName().substring(4);
			int cagenumber = Integer.parseInt(cagenumberString);
			if (cagenumber == 0 || cagenumber == 9)
				continue;
			// find the last time it is alive in the whole series --------------------
			Experiment expi = exp;
			while (expi.nextExperiment != null && expi.nextExperiment.isFlyAlive(cagenumber)) {
				expi = expi.nextExperiment;
			}
			// remove data up to the end ----------------------------------------------
			int lastIntervalFlyAlive = expi.getLastIntervalFlyAlive(cagenumber);
			int lastMinuteAlive = (int) (lastIntervalFlyAlive * expi.stepFrame + (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute));		
			int ilastalive = lastMinuteAlive / expAll.stepFrame;
			for (XLSCapillaryResults row : rowsForOneExp) {
				if (getCageFromCapillaryName (row.name) == cagenumber) {
					row.clearValues(ilastalive+1);
				}
			}
		}	
	}
	
	private XSSFWorkbook xlsInitWorkbook() {
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
	
	private XSSFSheet xlsInitSheet(XSSFWorkbook workBook, String title) {
		XSSFSheet sheet = workBook.getSheet(title);
		if (sheet == null) {
			sheet = workBook.createSheet(title);
			int row = outputFieldDescriptors(sheet);
			outputDataTimeIntervals(sheet, row);
		}
		return sheet;
	}
	
	private int xlsExportResultsArrayToSheet(XSSFSheet sheet, EnumXLSExportType xlsExportOption, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		writeExperimentDescriptors(expAll, charSeries, sheet, pt);
		pt = writeData2(sheet, xlsExportOption, pt);
		return pt.x;
	}
	
	private void outputDataTimeIntervals(XSSFSheet sheet, int row) {
		boolean transpose = options.transpose;
		Point pt = new Point(0, row);
		for (int i = expAll.startFrame; i <= expAll.endFrame; i += expAll.stepFrame, pt.y++) {
			XLSUtils.setValue(sheet, pt, transpose, "t"+i);
		}
	}
		
	private Point writeData2 (XSSFSheet sheet, EnumXLSExportType option, Point pt_main) {
		int rowseries = pt_main.x +2;
		int columndataarea = pt_main.y;
		Point pt = new Point(pt_main);
		switch (option) {
			case TOPLEVEL_LR:
			case TOPLEVELDELTA_LR:
			case SUMGULPS_LR:
				writeLRRows(sheet, columndataarea, rowseries, pt);
				break;
			default:
				writeSimpleRow(sheet, columndataarea, rowseries, pt);
				break;
		}			
		pt_main.x = pt.x+1;
		return pt_main;
	}
	
	private void writeSimpleRow(XSSFSheet sheet, int columndataarea, int rowseries, Point pt) {
		boolean transpose = options.transpose;
		for (XLSCapillaryResults row: rowsForOneExp) {
			pt.y = columndataarea;
			int col = getColFromKymoFileName(row.name);
			pt.x = rowseries + col; 
			for (int i=0; i < row.values_out.length; i++, pt.y++) {
				double value = row.values_out[i];
				if (!Double.isNaN(value)) {
					XLSUtils.setValue(sheet, pt, transpose, value);
					if (row.padded_out[i])
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
			}
			pt.x++;
		}
	}
	
	private void writeLRRows(XSSFSheet sheet, int columndataarea, int rowseries, Point pt) {
		boolean transpose = options.transpose;
		for (int irow = 0; irow < rowsForOneExp.size(); irow ++) {
			XLSCapillaryResults rowL = rowsForOneExp.get(irow);
			pt.y = columndataarea;
			int colL = getColFromKymoFileName(rowL.name);
			pt.x = rowseries + colL; 
			int cageL = getCageFromKymoFileName(rowL.name);
			XLSCapillaryResults rowR = null;
			if (irow+1 < rowsForOneExp.size()) {
				rowR = rowsForOneExp.get(irow+1);
				int cageR = getCageFromKymoFileName(rowR.name);
				if (cageR == cageL)
					irow++;
				else
					rowR = null;
			}
			// output values from the row
			int lenL = rowL.values_out.length;
			if (rowR != null && lenL != rowR.values_out.length)
				System.out.println("length of data - rowL="+lenL+" rowR="+rowR.values_out.length);
			int row0 = pt.x;
			for (int i=0; i < lenL; i++, pt.y++) {
				pt.x = row0;
				double dataL = rowL.values_out[i];
				double dataR = Double.NaN;
				if (rowR != null) 
					dataR = rowR.values_out[i];
				
				if (Double.isNaN(dataR) && !Double.isNaN(dataL)) 
					dataR=0;
				else if (!Double.isNaN(dataR) && Double.isNaN(dataL)) 
					dataL=0;
					
				double valueL = dataL+dataR;
				if (!Double.isNaN(valueL)) {
					XLSUtils.setValue(sheet, pt, transpose, valueL);
					if (rowL.padded_out[i])
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
				pt.x ++;
				if (valueL != 0 && !Double.isNaN(valueL)) {
					double valueR = (dataL-dataR)/valueL;
					if (!Double.isNaN(valueR)) {
						XLSUtils.setValue(sheet, pt, transpose, valueR);
						if (rowL.padded_out[i])
							XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
					}
				}
			}
		}
	}
	

}
