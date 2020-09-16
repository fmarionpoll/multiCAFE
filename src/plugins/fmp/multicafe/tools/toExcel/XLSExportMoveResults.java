package plugins.fmp.multicafe.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.sequence.Cage;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.sequence.XYTaSeries;
import plugins.fmp.multicafe.sequence.XYTaValue;
import plugins.fmp.multicafe.tools.Comparators;


public class XLSExportMoveResults  extends XLSExport {
	ExperimentList expList = null;
	List <XYTaSeries> rowsForOneExp = new ArrayList <XYTaSeries> ();

	// -----------------------
	
	public void exportToFile(String filename, XLSExportOptions opt) {	
		System.out.println("XLS move measures output");
		options = opt;
		expList = options.expList;

		int column = 1;
		int iSeries = 0;
		boolean loadCapillaries = true;
		boolean loadDrosoTrack = true; 
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
				
				if (options.xyImage)		
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.XYIMAGE);
				if (options.xyTopCage) 		
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.XYTOPCAGE);
				if (options.xyTipCapillaries)  	
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.XYTIPCAPS);
				if (options.distance)  	
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.DISTANCE);
				if (options.alive)	
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.ISALIVE);
				if (options.sleep) 	
					getMoveDataAndExport(exp, column, charSeries, EnumXLSExportType.SLEEP);
				
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
	
	private int getMoveDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExportType datatype) {	
		getDataFromOneSeriesOfExperiments(exp, datatype);
		XSSFSheet sheet = xlsInitSheet(datatype.toString());
		int colmax = xlsExportResultsArrayToSheet(sheet, datatype, col0, charSeries);
		
		if (options.onlyalive) {
			trimDeadsFromRowMoveData(exp);
			sheet = xlsInitSheet(datatype.toString()+"_alive");
			xlsExportResultsArrayToSheet(sheet, datatype, col0, charSeries);
		}
		return colmax;
	}
	
	private void getDataFromOneSeriesOfExperiments(Experiment exp, EnumXLSExportType xlsoption) {	
		// loop to get all capillaries into expAll and init rows for this experiment
		expAll.cages.copy(exp.cages);
		expAll.capillaries.copy(exp.capillaries);
		expAll.fileTimeImageFirst 	= exp.fileTimeImageFirst;
		expAll.fileTimeImageLast 	= exp.fileTimeImageLast;
		expAll.setExperimentFileName( exp.getExperimentFileName());
		expAll.exp_boxID 				= exp.exp_boxID;
		expAll.experiment 			= exp.experiment;
		expAll.comment1 			= exp.comment1;
		expAll.comment2 			= exp.comment2;

		Experiment expi = exp.nextExperiment;
		while (expi != null ) {
			expAll.cages.mergeLists(expi.cages);
			expAll.fileTimeImageLast = expi.fileTimeImageLast;
			expi = expi.nextExperiment;
		}
		expAll.fileTimeImageFirstMinute = (long) (expAll.fileTimeImageFirst.toMillis()/60000d);
		expAll.fileTimeImageLastMinute = (long) (expAll.fileTimeImageLast.toMillis()/60000d);
		int nFrames = (int) ((expAll.fileTimeImageLastMinute - expAll.fileTimeImageFirstMinute)/expAll.getCagesFrameStep() +1) ;
		if (expAll.getCagesFrameEnd() < nFrames) {
			expAll.setCagesFrameEnd(nFrames-1);
			exp.setCagesFrameEnd(nFrames-1);
		}
		int ncages = expAll.cages.cageList.size();
		rowsForOneExp = new ArrayList <XYTaSeries> (ncages);
		for (int i=0; i< ncages; i++) {
			Cage cage = expAll.cages.cageList.get(i);
			XYTaSeries row = new XYTaSeries (cage.roi.getName(), xlsoption, nFrames, expAll.getCagesFrameStep());
			row.nflies = cage.cageNFlies;
			rowsForOneExp.add(row);
		}
		Collections.sort(rowsForOneExp, new Comparators.XYTaSeriesComparator());
				
		// load data for one experiment - assume that exp = first experiment in the chain and iterate through the chain
		expi = exp;
		while (expi != null) {
			List <XYTaSeries> resultsArrayList = new ArrayList <XYTaSeries> (expi.cages.cageList.size());
			for (Cage cage: expi.cages.cageList) {
				XYTaSeries results = new XYTaSeries();
				results.copy(cage.flyPositions);
				results.binsize = expi.getCagesFrameStep();
				results.name = cage.roi.getName();
				results.nflies = cage.cageNFlies;
				if (results.nflies > 0) {				
					switch (xlsoption) {
						case DISTANCE:
							results.computeDistanceBetweenPoints();
							break;
						case ISALIVE:
							results.computeIsAlive();
							break;
						case SLEEP:
							results.computeSleep();
							break;
						case XYTOPCAGE:
							results.computeNewPointsOrigin(cage.getCenterTopCage());
							break;
						case XYTIPCAPS:
							results.computeNewPointsOrigin(cage.getCenterTipCapillaries(exp.capillaries));
							break;
						case XYIMAGE:
						default:
							break;
					}
					double pixelsize = 32. / exp.capillaries.capillariesArrayList.get(0).capPixels;
					results.changePixelSize(pixelsize);
					resultsArrayList.add(results);
				}
				// here add resultsArrayList to expAll
				addMoveResultsTo_rowsForOneExp(expi, resultsArrayList);
			}
			expi = expi.nextExperiment;
		}
		for (XYTaSeries row: rowsForOneExp ) {
			row.checkIsAliveFromAliveArray();
		}
	}
	
	private XYTaSeries getResultsArrayWithThatName(String testname, List <XYTaSeries> resultsArrayList) {
		XYTaSeries resultsFound = null;
		for (XYTaSeries results: resultsArrayList) {
			if (!results.name.equals(testname))
				continue;
			resultsFound = results;
			break;
		}
		return resultsFound;
	}
	
	private void addMoveResultsTo_rowsForOneExp(Experiment expi, List <XYTaSeries> resultsArrayList) {
		final int transfer_first_index = (int) (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute) / expAll.getCagesFrameStep() ;
		final int transfer_nvalues = (int) ((expi.fileTimeImageLastMinute - expi.fileTimeImageFirstMinute)/expi.getCagesFrameStep())+1;
		for (XYTaSeries row: rowsForOneExp ) {
			XYTaSeries results = getResultsArrayWithThatName(row.name,  resultsArrayList);
			if (results != null) {
				if (options.collateSeries && options.padIntervals && expi.previousExperiment != null) 
					padWithLastPreviousValue(row, transfer_first_index);
				
				for (int fromTime = expi.getCagesFrameStart(); fromTime <= expi.getCagesFrameEnd(); fromTime += expi.getCagesFrameStep()) {
					int from_i = fromTime / expi.getCagesFrameStep();
					if (from_i >= results.pointsList.size())
						break;
					XYTaValue aVal = results.pointsList.get(from_i);
					int to_i = (int) (fromTime + expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute) / expAll.getCagesFrameStep() ;
					if (to_i >= row.pointsList.size())
						break;
					row.pointsList.get(to_i).copy(aVal);
				}
				
			} else {
				if (options.collateSeries && options.padIntervals && expi.previousExperiment != null) {
					XYTaValue posok = padWithLastPreviousValue(row, transfer_first_index);
					int nvalues = transfer_nvalues;
					if (posok != null) {
						if (nvalues > row.pointsList.size())
							nvalues = row.pointsList.size();
						int tofirst = transfer_first_index;
						int tolast = tofirst + nvalues;
						if (tolast > row.pointsList.size())
							tolast = row.pointsList.size();
						for (int toi = tofirst; toi < tolast; toi++) 
							row.pointsList.get(toi).copy(posok);
					}
				}
			}
		}
	}
	
	private XYTaValue padWithLastPreviousValue(XYTaSeries row, int transfer_first_index) {
		XYTaValue posok = null;
		int index = getIndexOfFirstNonEmptyValueBackwards(row, transfer_first_index);
		if (index >= 0) {
			posok = row.pointsList.get(index);
			for (int i=index+1; i< transfer_first_index; i++) {
				XYTaValue pos = row.pointsList.get(i);
				pos.copy(posok);
				pos.xytPadded = true;
			}
		}
		return posok;
	}
	
	private int getIndexOfFirstNonEmptyValueBackwards(XYTaSeries row, int fromindex) {
		int index = -1;
		for (int i= fromindex; i>= 0; i--) {
			XYTaValue pos = row.pointsList.get(i);
			if (!Double.isNaN(pos.xytPoint.getX())) {
				index = i;
				break;
			}
		}
		return index;
	}
	
	private void trimDeadsFromRowMoveData(Experiment exp) {
		for (Cage cage: exp.cages.cageList) {
			int cagenumber = Integer.parseInt(cage.roi.getName().substring(4));
			int ilastalive = 0;
			if (cage.cageNFlies > 0) {
				Experiment expi = exp;
				while (expi.nextExperiment != null && expi.nextExperiment.isFlyAlive(cagenumber)) {
					expi = expi.nextExperiment;
				}
				int lastIntervalFlyAlive = expi.getLastIntervalFlyAlive(cagenumber);
				int lastMinuteAlive = (int) (lastIntervalFlyAlive * expi.getCagesFrameStep() 
						+ (expi.fileTimeImageFirstMinute - expAll.fileTimeImageFirstMinute));		
				ilastalive = lastMinuteAlive / expAll.getCagesFrameStep();
			}
			for (XYTaSeries row : rowsForOneExp) {
				int rowCageNumber = Integer.parseInt(row.name.substring(4));
				if ( rowCageNumber == cagenumber) {
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
		writeRows(sheet, columndataarea, rowseries, pt);		
		pt_main.x = pt.x+1;
		return pt_main;
	}
	
	private void writeRows(XSSFSheet sheet, int column_dataArea, int rowSeries, Point pt) {
		boolean transpose = options.transpose;
		for (XYTaSeries row: rowsForOneExp) {
			pt.y = column_dataArea;
			int col = getColFromCageName(row.name)*2;
			pt.x = rowSeries + col; 
			if (row.nflies < 1)
				continue;
			
			for (int coltime=expAll.getCagesFrameStart(); coltime < expAll.getCagesFrameEnd(); coltime+=options.buildExcelBinStep, pt.y++) {
				int i_from = coltime / row.binsize;
				if (i_from >= row.pointsList.size())
					break;
				double valueL = Double.NaN;
				double valueR = Double.NaN;
				XYTaValue pos = row.pointsList.get(i_from);
				switch (row.exportType) {
					case DISTANCE:
						valueL = pos.xytDistance;
						valueR = valueL;
						break;
					case ISALIVE:
						valueL = pos.xytAlive ? 1: 0;
						valueR = valueL;
						break;
					case SLEEP:
						valueL = pos.xytSleep? 1: 0;
						valueR = valueL;
						break;
					case XYTOPCAGE:
					case XYTIPCAPS:
					case XYIMAGE:
						valueL = pos.xytPoint.getX();
						valueR = pos.xytPoint.getY();
					default:
						break;
				}
				
				if (!Double.isNaN(valueL)) {
					XLSUtils.setValue(sheet, pt, transpose, valueL);
					if (pos.xytPadded)
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
				}
				if (!Double.isNaN(valueR)) {
					pt.x++;
					XLSUtils.setValue(sheet, pt, transpose, valueR);
					if (pos.xytPadded)
						XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
					pt.x--;
				}
			}
			pt.x+=2;
		}
	}
	

}
