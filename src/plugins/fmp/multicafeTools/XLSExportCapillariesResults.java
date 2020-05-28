package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;





public class XLSExportCapillariesResults  extends XLSExport {
	ExperimentList expList = null;
	List <XLSCapillaryResults> rowsForOneExp = new ArrayList <XLSCapillaryResults> ();

	// ----------------------
	
	public void exportToFile(String filename, XLSExportOptions opt) {	
		System.out.println("XLS capillary measures output");
		options = opt;
		expList = options.expList;

		int column = 1;
		int iSeries = 0;
		boolean loadCapillaries = true;
		boolean loadDrosoTrack = options.onlyalive;
		expList.loadAllExperiments(loadCapillaries, loadDrosoTrack);
		expList.chainExperiments(options.collateSeries);
		expAll = expList.getStartAndEndFromAllExperiments(options);
	
		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		int nbexpts = expList.getSize();
		progress.setLength(nbexpts);

		try { 
			workbook = xlsInitWorkbook();
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = expList.getExperiment(index);
				if (exp.previousExperiment != null)
					continue;
				
				progress.setMessage("Export experiment "+ (index+1) +" of "+ nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);
				
				if (options.topLevel) 		
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL);
				if (options.sum && options.topLevel) 		
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL_LR);
				if (options.topLevelDelta) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA);
				if (options.sum && options.topLevelDelta) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA_LR);
				if (options.consumption) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.SUMGULPS);
				if (options.sum && options.consumption) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.SUMGULPS_LR);

				if (options.bottomLevel) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.BOTTOMLEVEL);		
				if (options.derivative) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.DERIVEDVALUES);	
				
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
	
	private int getDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExportType datatype) {	
		getDataFromOneSeriesOfExperiments(exp, datatype);
		
		XSSFSheet sheet = xlsInitSheet(datatype.toString());
		int colmax = xlsExportResultsArrayToSheet(sheet, datatype, col0, charSeries);
		if (options.onlyalive) {
			trimDeadsFromArrayList(exp);
			sheet = xlsInitSheet(datatype.toString()+"_alive");
			xlsExportResultsArrayToSheet(sheet, datatype, col0, charSeries);
		}
		return colmax;
	}
	
	private void getDataFromOneSeriesOfExperiments(Experiment exp, EnumXLSExportType xlsoption) {	
		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.capillaries.copy(exp.capillaries);
		expAll.fileTimeImageFirst 	= exp.fileTimeImageFirst;
		expAll.fileTimeImageLast 	= exp.fileTimeImageLast;
		expAll.experimentFileName 	= exp.experimentFileName;
		expAll.boxID 				= exp.boxID;
		expAll.experiment 			= exp.experiment;
		expAll.comment1 			= exp.comment1;
		expAll.comment2 			= exp.comment2;

		Experiment expi = exp.nextExperiment;
		while (expi != null ) {
			expAll.capillaries.mergeLists(expi.capillaries);
			expAll.fileTimeImageLast = expi.fileTimeImageLast;
			expi = expi.nextExperiment;
		}
		expAll.fileTimeImageFirstMinute = expAll.fileTimeImageFirst.toMillis()/60000;
		expAll.fileTimeImageLastMinute = expAll.fileTimeImageLast.toMillis()/60000;
		
		int ncapillaries = expAll.capillaries.capillariesArrayList.size();
		int nFrames = (expAll.getKymoFrameEnd() - expAll.getKymoFrameStart())/expAll.getKymoFrameStep() +1 ;
		rowsForOneExp = new ArrayList <XLSCapillaryResults> (ncapillaries);
		for (int i=0; i< ncapillaries; i++) {
			Capillary cap = expAll.capillaries.capillariesArrayList.get(i);
			XLSCapillaryResults row = new XLSCapillaryResults (cap.roi.getName(), xlsoption);
			row.initValuesArray(nFrames);
			rowsForOneExp.add(row);
			row.binsize = expAll.getKymoFrameStep();
		}
		Collections.sort(rowsForOneExp, new Comparators.XLSCapillaryResultsComparator());
				
		// load data for one experiment - assume that exp = first experiment in the chain and iterate through the chain
		expi = exp;
		while (expi != null) {
			List <XLSCapillaryResults> resultsArrayList = new ArrayList <XLSCapillaryResults> (expi.capillaries.capillariesArrayList.size());
			for (Capillary cap: expi.capillaries.capillariesArrayList) {
				XLSCapillaryResults results = new XLSCapillaryResults(cap.roi.getName(), xlsoption);
				results.binsize = expi.getKymoFrameStep();
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
	
	private XLSCapillaryResults getResultsArrayWithThatName(String testname, List <XLSCapillaryResults> resultsArrayList) {
		XLSCapillaryResults resultsFound = null;
		for (XLSCapillaryResults results: resultsArrayList) {
			if (!results.name.equals(testname))
				continue;
			resultsFound = results;
			break;
		}
		return resultsFound;
	}
	
	private void addResultsTo_rowsForOneExp(Experiment expi, List <XLSCapillaryResults> resultsArrayList) {
		EnumXLSExportType xlsoption = resultsArrayList.get(0).exportType;
		double scalingFactorToPhysicalUnits = expi.capillaries.desc.volume / expi.capillaries.desc.pixels;
		
		int transfer_first_index = (int) (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute) / expAll.getKymoFrameStep() ;
		int transfer_nvalues = (int) ((expi.fileTimeImageLastMinute - expi.fileTimeImageFirstMinute)/expi.getKymoFrameStep())+1;
		
		for (XLSCapillaryResults row: rowsForOneExp ) {
			XLSCapillaryResults results = getResultsArrayWithThatName(row.name,  resultsArrayList);
			if (results != null) {
				double dvalue = 0;
				switch (xlsoption) {
					case TOPLEVEL:
					case TOPLEVEL_LR:
					case SUMGULPS:
					case SUMGULPS_LR:
						if (options.collateSeries && options.padIntervals && expi.previousExperiment != null) 
							dvalue = padWithLastPreviousValue(row, transfer_first_index);
						break;
					default:
						break;
				}
				
				for (int fromTime = expi.getKymoFrameStart(); fromTime <= expi.getKymoFrameEnd(); fromTime += expi.getKymoFrameStep()) {
					int from_i = fromTime / expi.getKymoFrameStep();
					if (from_i >= results.data.size())
						break;
					double value = results.data.get(from_i) * scalingFactorToPhysicalUnits + dvalue;
					int to_i = (int) (fromTime + expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute) / expAll.getKymoFrameStep() ;
					row.values_out[to_i]= value;
				}
				
			} else {
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
			String cagenumberString = cage.roi.getName().substring(4);
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
			int lastMinuteAlive = (int) (lastIntervalFlyAlive * expi.getKymoFrameStep() + (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute));		
			int ilastalive = lastMinuteAlive / expAll.getKymoFrameStep();
			for (XLSCapillaryResults row : rowsForOneExp) {
				if (getCageFromCapillaryName (row.name) == cagenumber) {
					row.clearValues(ilastalive+1);
				}
			}
		}	
	}
	
	private int xlsExportResultsArrayToSheet(XSSFSheet sheet, EnumXLSExportType xlsExportOption, int col0, String charSeries) {
		Point pt = new Point(col0, 0);
		writeExperimentDescriptors(expAll, charSeries, sheet, pt, xlsExportOption);
		pt = writeData2(sheet, xlsExportOption, pt);
		return pt.x;
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
	
	private void writeSimpleRow(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt) {
		boolean transpose = options.transpose;
		for (XLSCapillaryResults row: rowsForOneExp) {
			pt.y = column_dataArea;
			int col = getColFromKymoFileName(row.name);
			pt.x = rowSeries + col; 
			
			for (int i_to=0; i_to < row.values_out.length; i_to++, pt.y++) {
				int coltime = i_to * options.buildExcelBinStep;
				int i_from = coltime / row.binsize;
				if (i_from >= row.values_out.length)
					break;
				double value = row.values_out[i_from];
				if (!Double.isNaN(value)) {
					XLSUtils.setValue(sheet, pt, transpose, value);
					if (row.padded_out[i_to])
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
			
			for (int i_to=0; i_to < lenL; i_to++, pt.y++) {
				pt.x = row0;
				int coltime = i_to * options.buildExcelBinStep;
				int i_from = coltime / rowL.binsize;
				if (i_from >= rowL.values_out.length)
					break;
				double dataL = rowL.values_out[i_from];
				double dataR = Double.NaN;
				if (rowR != null) 
					dataR = rowR.values_out[i_from];
				
				if (Double.isNaN(dataR) && !Double.isNaN(dataL)) 
					dataR=0;
				else if (!Double.isNaN(dataR) && Double.isNaN(dataL)) 
					dataL=0;
					
				double sum = dataL+dataR;
				if (!Double.isNaN(sum)) {
					XLSUtils.setValue(sheet, pt, transpose, sum);
					if (rowL.padded_out[i_to])
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
				pt.x ++;
				if (sum != 0 && !Double.isNaN(sum)) {
					double ratio = (dataL-dataR)/sum;
					if (!Double.isNaN(ratio)) {
						XLSUtils.setValue(sheet, pt, transpose, ratio);
						if (rowL.padded_out[i_to])
							XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
					}
				}
			}
		}
	}
	

}
