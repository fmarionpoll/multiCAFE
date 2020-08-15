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
	List <XLSResults> rowsForOneExp = new ArrayList <XLSResults> ();

	// -----------------------
	
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
		expAll.resultsSubPath = expList.expListResultsSubPath;
	
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
				
				if (options.topLevel) {	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPRAW);
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL);
				}
				if (options.sum_ratio_LR && options.topLevel) 		
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL_LR);
				if (options.topLevelDelta) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA);
				if (options.sum_ratio_LR && options.topLevelDelta) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA_LR);
				if (options.consumption) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.SUMGULPS);
				if (options.sum_ratio_LR && options.consumption) 	
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
		
		if (options.cage) {
			combineData(exp);
			sheet = xlsInitSheet(datatype.toString()+"_cage");
			xlsExportResultsArrayToSheet(sheet, datatype, col0, charSeries);
		}
		
		return colmax;
	}
	
	private void getDataFromOneSeriesOfExperiments(Experiment exp, EnumXLSExportType xlsoption) {	
		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.capillaries.copy(exp.capillaries);
		expAll.fileTimeImageFirst 	= exp.fileTimeImageFirst;
		expAll.fileTimeImageLast 	= exp.fileTimeImageLast;
		expAll.setExperimentFileName(exp.getExperimentFileName());
		expAll.exp_boxID 				= exp.exp_boxID;
		expAll.experiment 			= exp.experiment;
		expAll.comment1 			= exp.comment1;
		expAll.comment2 			= exp.comment2;	

		Experiment expi = exp.nextExperiment;
		while (expi != null ) {
			expAll.capillaries.mergeLists(expi.capillaries);
			expAll.fileTimeImageLast = expi.fileTimeImageLast;
			expi = expi.nextExperiment;
		}
		expAll.fileTimeImageFirstMinute = (long) (expAll.fileTimeImageFirst.toMillis()/60000d);
		expAll.fileTimeImageLastMinute = (long) (expAll.fileTimeImageLast.toMillis()/60000d);
		int nFrames = (expAll.getKymoFrameEnd() - expAll.getKymoFrameStart())/expAll.getKymoFrameStep() +1 ;
		
		int ncapillaries = expAll.capillaries.capillariesArrayList.size();
		rowsForOneExp = new ArrayList <XLSResults> (ncapillaries);
		for (int i=0; i< ncapillaries; i++) {
			Capillary cap = expAll.capillaries.capillariesArrayList.get(i);
			XLSResults row = new XLSResults (cap.roi.getName(), cap.capNFlies, xlsoption, nFrames, expAll.getKymoFrameStep());
			row.stimulus = cap.capStimulus;
			row.concentration = cap.capConcentration;
			row.cageID = cap.capCageID;
			rowsForOneExp.add(row);
		}
		Collections.sort(rowsForOneExp, new Comparators.XLSResultsComparator());
				
		// load data for one experiment - assume that exp = first experiment in the chain and iterate through the chain
		expi = exp;
		while (expi != null) {
			expi.resultsSubPath = expAll.resultsSubPath;
			XLSResultsArray resultsArrayList = new XLSResultsArray (expi.capillaries.capillariesArrayList.size());
			
			switch (xlsoption) {
				case TOPRAW:
					for (Capillary cap: expi.capillaries.capillariesArrayList) {
						resultsArrayList.checkIfSameStimulusAndConcentration(cap);
						XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsoption, expi.getKymoFrameStep());
						results.data = cap.getMeasures(EnumListType.topLevel);
						resultsArrayList.add(results);
					}
					break;
				case TOPLEVEL:
				case TOPLEVEL_LR:
				case TOPLEVELDELTA:
				case TOPLEVELDELTA_LR:
					for (Capillary cap: expi.capillaries.capillariesArrayList) {
						resultsArrayList.checkIfSameStimulusAndConcentration(cap);
						XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsoption, expi.getKymoFrameStep());
						if (options.t0) 
							results.data = exp.seqKymos.subtractT0(cap.getMeasures(EnumListType.topLevel));
						else
							results.data = cap.getMeasures(EnumListType.topLevel);
						resultsArrayList.add(results);
					}
					if (options.subtractEvaporation)
						resultsArrayList.subtractEvaporation();
					break;
				case DERIVEDVALUES:
					for (Capillary cap: expi.capillaries.capillariesArrayList) {
						resultsArrayList.checkIfSameStimulusAndConcentration(cap);
						XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsoption, expi.getKymoFrameStep());
						results.data = cap.getMeasures(EnumListType.derivedValues);
						resultsArrayList.add(results);
					}
					if (options.subtractEvaporation)
						resultsArrayList.subtractEvaporation();
					break;
				case SUMGULPS:
				case SUMGULPS_LR:
					for (Capillary cap: expi.capillaries.capillariesArrayList) {
						resultsArrayList.checkIfSameStimulusAndConcentration(cap);
						XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsoption, expi.getKymoFrameStep());
						results.data = cap.getMeasures(EnumListType.cumSum);
						resultsArrayList.add(results);
					}
					if (options.subtractEvaporation)
						resultsArrayList.subtractEvaporation();
					break;
				case BOTTOMLEVEL:
					for (Capillary cap: expi.capillaries.capillariesArrayList) {
						XLSResults results = new XLSResults(cap.roi.getName(), cap.capNFlies, xlsoption, expi.getKymoFrameStep());
						results.data = cap.getMeasures(EnumListType.bottomLevel);
						resultsArrayList.add(results);
					}
					break;
				default:
					break;
			}
				
			addResultsTo_rowsForOneExp(expi, resultsArrayList);
			expi = expi.nextExperiment;
		}
		
		switch (xlsoption) {
			case TOPLEVELDELTA:
			case TOPLEVELDELTA_LR:
				for (XLSResults row: rowsForOneExp ) 
					row.subtractDeltaT(expAll.getKymoFrameStep(), options.buildExcelBinStep);
				break;
			default:
				break;
		}
	}
	
	private XLSResults getResultsArrayWithThatName(String testname, XLSResultsArray resultsArrayList) {
		XLSResults resultsFound = null;
		for (XLSResults results: resultsArrayList.resultsArrayList) {
			if (results.name.equals(testname)) {
				resultsFound = results;
				break;
			}
		}
		return resultsFound;
	}
	
	private void addResultsTo_rowsForOneExp(Experiment expi, XLSResultsArray resultsArrayList) {
		if (resultsArrayList.resultsArrayList.size() <1)
			return;
		EnumXLSExportType xlsoption = resultsArrayList.get(0).exportType;
		double scalingFactorToPhysicalUnits = expi.capillaries.desc.volume / expi.capillaries.desc.pixels;
		
		long to_first_index = (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute) / expAll.getKymoFrameStep() ;
		long to_nvalues = ((expi.fileTimeImageLastMinute - expi.fileTimeImageFirstMinute)/expi.getKymoFrameStep())+1;
		for (XLSResults row: rowsForOneExp ) {
			XLSResults results = getResultsArrayWithThatName(row.name,  resultsArrayList);
			if (results != null && results.data != null) {
				double dvalue = 0;
				switch (xlsoption) {
					case TOPLEVEL:
					case TOPLEVEL_LR:
					case SUMGULPS:
					case SUMGULPS_LR:
					case TOPLEVELDELTA:
					case TOPLEVELDELTA_LR:
						if (options.collateSeries && options.padIntervals && expi.previousExperiment != null) 
							dvalue = padWithLastPreviousValue(row, to_first_index);
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
					if (to_i >= row.values_out.length)
						break;
					if (to_i < 0)
						break;
					row.values_out[to_i]= value;
				}
				
			} else {
				if (options.collateSeries && options.padIntervals && expi.previousExperiment != null) {
					double dvalue = padWithLastPreviousValue(row, to_first_index);
					int tofirst = (int) to_first_index;
					int tolast = (int) (tofirst + to_nvalues);
					if (tolast > row.values_out.length)
						tolast = row.values_out.length;
					for (int toi = tofirst; toi < tolast; toi++) {
						row.values_out[toi]= dvalue;
					}
				}
			}
		}
	}
	
	private double padWithLastPreviousValue(XLSResults row, long to_first_index) {
		double dvalue = 0;
		int index = getIndexOfFirstNonEmptyValueBackwards(row, to_first_index);
		if (index >= 0) {
			dvalue = row.values_out[index];
			for (int i=index+1; i< to_first_index; i++) {
				row.values_out[i] = dvalue;
				row.padded_out[i] = true;
			}
		}
		return dvalue;
	}
	
	private int getIndexOfFirstNonEmptyValueBackwards(XLSResults row, long fromindex) {
		int index = -1;
		int ifrom = (int) fromindex;
		for (int i= ifrom; i>= 0; i--) {
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
			int ilastalive = 0;
			if (cage.cageNFlies > 0) {
				Experiment expi = exp;
				while (expi.nextExperiment != null && expi.nextExperiment.isFlyAlive(cagenumber)) {
					expi = expi.nextExperiment;
				}
				int lastIntervalFlyAlive = expi.getLastIntervalFlyAlive(cagenumber);
				int lastMinuteAlive = (int) (lastIntervalFlyAlive * expi.getKymoFrameStep() 
						+ (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute));		
				ilastalive = lastMinuteAlive / expAll.getKymoFrameStep();
			}
			for (XLSResults row : rowsForOneExp) {
				if (getCageFromCapillaryName (row.name) == cagenumber) {
					row.clearValues(ilastalive+1);
				}
			}
		}	
	}
	
	private void combineData(Experiment exp) {
		for (XLSResults row_master : rowsForOneExp) {
			if (row_master.nflies == 0 || row_master.values_out == null)
				continue;
			for (XLSResults row : rowsForOneExp) {
				if (row.nflies == 0 || row.values_out == null)
					continue;
				if (row.cageID != row_master.cageID)
					continue;
				if (row.name .equals(row_master.name))
					continue;
				if (row.stimulus .equals(row_master.stimulus) && row.concentration .equals(row_master.concentration)) {
					row_master.addValues_out(row);
					row.clearAll();
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
				writeSimpleRows(sheet, columndataarea, rowseries, pt);
				break;
		}			
		pt_main.x = pt.x+1;
		return pt_main;
	}
	
	private void writeSimpleRows(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt) {
		boolean transpose = options.transpose;
		for (XLSResults row: rowsForOneExp) {
			pt.y = column_dataArea;
			int col = getColFromKymoFileName(row.name);
			pt.x = rowSeries + col; 
			if (row.values_out == null)
				continue;
			//System.out.println("writeSimpleRows row.binsize =" +row.binsize);
			for (int coltime=expAll.getKymoFrameStart(); coltime < expAll.getKymoFrameEnd(); coltime+=options.buildExcelBinStep, pt.y++) {
				int i_from = coltime / row.rowbinsize;
				if (i_from >= row.values_out.length)
					break;
				double value = row.values_out[i_from];
				if (!Double.isNaN(value)) {
					XLSUtils.setValue(sheet, pt, transpose, value);
					if (i_from < row.padded_out.length && row.padded_out[i_from])
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
			}
			pt.x++;
		}
	}
	
	private void writeLRRows(XSSFSheet sheet, int columndataarea, int rowseries, Point pt) {
		boolean transpose = options.transpose;
		for (int irow = 0; irow < rowsForOneExp.size(); irow ++) {
			XLSResults rowL = rowsForOneExp.get(irow);
			pt.y = columndataarea;
			int colL = getColFromKymoFileName(rowL.name);
			pt.x = rowseries + colL; 
			int cageL = getCageFromKymoFileName(rowL.name);
			XLSResults rowR = null;
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
			if (rowR != null && rowR.values_out != null && lenL != rowR.values_out.length)
				System.out.println("length of data - rowL="+lenL+" rowR="+rowR.values_out.length);
			int row0 = pt.x;
			
			for (int coltime=expAll.getKymoFrameStart(); coltime < expAll.getKymoFrameEnd(); coltime+=options.buildExcelBinStep, pt.y++) {
				pt.x = row0;
				int i_from = coltime / rowL.rowbinsize;
				if (i_from >= rowL.values_out.length)
					break;
				double dataL = rowL.values_out[i_from];
				double dataR = Double.NaN;
				if (rowR != null && rowR.values_out != null) 
					dataR = rowR.values_out[i_from];
				
				if (Double.isNaN(dataR) && !Double.isNaN(dataL)) 
					dataR=0;
				else if (!Double.isNaN(dataR) && Double.isNaN(dataL)) 
					dataL=0;
					
				double sum = dataL+dataR;
				if (!Double.isNaN(sum)) {
					XLSUtils.setValue(sheet, pt, transpose, sum);
					if (i_from < rowL.padded_out.length && rowL.padded_out[i_from])
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
				pt.x ++;
				if (sum != 0 && !Double.isNaN(sum)) {
					double ratio = (dataL-dataR)/sum;
					if (!Double.isNaN(ratio)) {
						XLSUtils.setValue(sheet, pt, transpose, ratio);
						if (i_from < rowL.padded_out.length && rowL.padded_out[i_from])
							XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
					}
				}
			}
		}
	}
	

}
