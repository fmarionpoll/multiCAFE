package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.XYTaSeries;

public class XLSExportCapillaryResults2  extends XLSExport {
	ExperimentList expList = null;
	
	public void exportToFile(String filename, XLSExportOptions opt) {	
		System.out.println("XLS capillary measures output");
		options = opt;
		expList = options.expList;

		try { 
			XSSFWorkbook workbook = xlsInitWorkbook();
		    
			int column = 1;
			int iSeries = 0;
			expList.readInfosFromAllExperiments(true, options.onlyalive);
			if (options.collateSeries)
				expList.chainExperiments();
			expAll = expList.getStartAndEndFromAllExperiments(options);
			expAll.stepFrame = expList.getExperiment(0).stepFrame;
			int nbexpts = expList.getSize();
			ProgressFrame progress = new ProgressFrame("Export data to Excel");
			progress.setLength(nbexpts);
			
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = expList.getExperiment(index);
				progress.setMessage("Export experiment "+ (index+1) +" of "+ nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);
				if (options.topLevel) 		
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportItems.TOPLEVEL);
				if (options.sum && options.topLevel) 		
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportItems.TOPLEVEL_LR);
				if (options.topLevelDelta) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportItems.TOPLEVELDELTA);
				if (options.sum && options.topLevelDelta) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportItems.TOPLEVELDELTA_LR);
				if (options.consumption) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportItems.SUMGULPS);
				if (options.sum && options.consumption) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportItems.SUMGULPS_LR);

				if (options.bottomLevel) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportItems.BOTTOMLEVEL);		
				if (options.derivative) 	
					getDataAndExport(exp, workbook, column, charSeries, EnumXLSExportItems.DERIVEDVALUES);	
				
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
	
	private int getDataAndExport(Experiment exp, XSSFWorkbook workbook, int col0, String charSeries, EnumXLSExportItems datatype) {	
		List <XLSCapillaryResults> resultsArrayList = getDataFromCapillaryMeasures(exp, datatype);
		options.trim_alive = false;
		XSSFSheet sheet = xlsInitSheet(workbook, datatype.toString());
		int colmax = xlsExportResultsArrayToSheet(exp, sheet, datatype, col0, charSeries, resultsArrayList);
		if (options.onlyalive) {
			options.trim_alive = true;
			trimDeadsFromArrayList(exp, resultsArrayList);
			sheet = xlsInitSheet(workbook, datatype.toString()+"_alive");
			xlsExportResultsArrayToSheet(exp, sheet, datatype, col0, charSeries, resultsArrayList);
		}
		return colmax;
	}
	
	private List <XLSCapillaryResults> getDataFromCapillaryMeasures(Experiment exp, EnumXLSExportItems xlsoption) {	
		List <XLSCapillaryResults> resultsArrayList = new ArrayList <XLSCapillaryResults> ();	
		Capillaries capillaries = exp.capillaries;
		double scalingFactorToPhysicalUnits = capillaries.desc.volume / exp.capillaries.desc.pixels;
		for (Capillary cap: capillaries.capillariesArrayList) {
			XLSCapillaryResults results = new XLSCapillaryResults();
			results.name = cap.capillaryRoi.getName(); 
			switch (xlsoption) {
				case TOPLEVEL:
				case TOPLEVEL_LR:
					if (options.t0) {
						if (options.collateSeries && exp.previousExperiment != null) {
							double dvalue = getLastValue_Of_Previous_Experiment(exp.previousExperiment, xlsoption, cap.getName());
							int addedValue = (int) (dvalue / scalingFactorToPhysicalUnits);
							results.data = exp.seqKymos.subtractT0AndAddConstant(cap.getMeasures(EnumListType.topLevel), addedValue);
						}
						else
							results.data = exp.seqKymos.subtractT0(cap.getMeasures(EnumListType.topLevel));
					}
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
					if (options.collateSeries && exp.previousExperiment != null) {
						double dvalue = getLastValue_Of_Previous_Experiment(exp.previousExperiment, xlsoption, cap.getName());
						int addedValue = (int) (dvalue / scalingFactorToPhysicalUnits);
						results.data = exp.seqKymos.addConstant(cap.getMeasures(EnumListType.cumSum), addedValue);
					}
					else 
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
		return resultsArrayList;
	}
	
	private double getLastValue_Of_Previous_Experiment(Experiment exp, EnumXLSExportItems xlsoption, String capName) {
		double valuePreviousSeries = 0.;
		if (exp.previousExperiment != null)
			valuePreviousSeries = getLastValue_Of_Previous_Experiment(exp.previousExperiment, xlsoption, capName);
		
		Capillaries capillaries = exp.capillaries;
		for (Capillary cap : capillaries.capillariesArrayList) {
			if (cap.getName().equals(capName)) {
				int lastValue = 0;
				switch (xlsoption) {
					case SUMGULPS:
					case SUMGULPS_LR:
						lastValue = cap.getLastMeasure(EnumListType.cumSum);
						break;
					case TOPLEVELDELTA:
					case TOPLEVELDELTA_LR: 
						lastValue = cap.getLastDeltaMeasure(EnumListType.topLevel);
						break;
					case DERIVEDVALUES:
						lastValue = cap.getLastMeasure(EnumListType.derivedValues);
						break;
					default:
					case TOPLEVEL:
					case TOPLEVEL_LR:
						if (options.t0)
							lastValue = cap.getLastMeasure(EnumListType.topLevel) -cap.getT0Measure(EnumListType.topLevel);
						else
							lastValue = cap.getLastMeasure(EnumListType.topLevel);
						break;
				}
				double scalingFactorToPhysicalUnits = exp.capillaries.desc.volume / exp.capillaries.desc.pixels;
				valuePreviousSeries += (lastValue * scalingFactorToPhysicalUnits);
				break;
			}
		}
		return valuePreviousSeries;
	}
	
	private void trimDeadsFromArrayList(Experiment exp, List <XLSCapillaryResults> resultsArrayList) {
 		for (Cage cage: exp.cages.cageList) {
			XYTaSeries flypos = cage.flyPositions;		
			String cagenumberString = cage.cageLimitROI.getName().substring(4);
			int cagenumber = Integer.parseInt(cagenumberString);
			if (cagenumber == 0 || cagenumber == 9)
				continue;
			if (options.collateSeries && exp.nextExperiment != null && exp.nextExperiment.isFlyAlive(cagenumber))
				continue; 
			for (XLSCapillaryResults capillaryResult : resultsArrayList) {
				if (getCageFromCapillaryName (capillaryResult.name) == cagenumber) {
					flypos.getLastIntervalAlive();
					int ilastalive = flypos.lastTimeAlive/ exp.stepFrame;
					trimArrayLength(capillaryResult.data, ilastalive);
				}
			}
		}		
	}
	
	private void trimArrayLength (List<Integer> array, int ilastalive) {
		if (array == null)
			return;
		int arraysize = array.size();
		if (ilastalive < 0)
			ilastalive = 0;
		if (ilastalive > (arraysize-1))
			ilastalive = arraysize-1;
		for (int i=array.size()-1; i>= ilastalive; i--)
			array.remove(i);
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
			outputFieldHeaders(sheet, options.transpose);
		}
		return sheet;
	}
	
	private int xlsExportResultsArrayToSheet(Experiment exp, XSSFSheet sheet, EnumXLSExportItems xlsExportOption, int col0, String charSeries, List <XLSCapillaryResults> arrayList) {
		Point pt = new Point(col0, 0);
		if (options.collateSeries) 
			pt.x = expList.getStackColumnPosition(exp, col0);
		
		pt = writeSeriesInfos(exp, sheet, xlsExportOption, pt, charSeries);
		pt = writeData(exp, sheet, xlsExportOption, pt, charSeries, arrayList);
		return pt.x;
	}
	
	private Point writeSeriesInfos (Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt, String charSeries) {	
		if (exp.previousExperiment == null)
			writeExperimentDescriptors(exp, charSeries, sheet, pt, options);
		else
			pt.y += 17;  // n descriptor columns = 17
		return pt;
	}
		
	private Point writeData (Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt_main, String charSeries, List <XLSCapillaryResults> dataArrayList) {
		boolean transpose = options.transpose;
		double scalingFactorToPhysicalUnits = exp.capillaries.desc.volume / exp.capillaries.desc.pixels;
		int col0 = pt_main.x;
		int row0 = pt_main.y;
		if (charSeries == null)
			charSeries = "t";
		int startFrame 	= (int) exp.startFrame;
		int endFrame 	= (int) exp.endFrame;
		if (endFrame > exp.seqCamData.seq.getSizeT()-1)
			endFrame = exp.seqCamData.seq.getSizeT()-1;
		int fullstep 	= exp.stepFrame * options.buildExcelBinStep;
		long imageTimeMinutes = exp.seqCamData.getImageFileTime(startFrame).toMillis()/ 60000;
		long referenceFileTimeImageFirstMinutes = exp.getFileTimeImageFirst(true).toMillis()/60000;
		long referenceFileTimeImageLastMinutes = exp.getFileTimeImageLast(true).toMillis()/60000;
		if (options.absoluteTime) {
			referenceFileTimeImageFirstMinutes = expAll.fileTimeImageFirstMinute;
			referenceFileTimeImageLastMinutes = expAll.fileTimeImageLastMinute;
		}
		
		pt_main.x =0;
		long tspanMinutes = referenceFileTimeImageLastMinutes-referenceFileTimeImageFirstMinutes;
		long lastinterval = getnearest(tspanMinutes, fullstep)/ fullstep;
		
		long firstImageTimeMinutes = exp.getFileTimeImageFirst(false).toMillis()/60000;
		long diff2 = getnearest(firstImageTimeMinutes-referenceFileTimeImageFirstMinutes, fullstep);
		pt_main.y = (int) (diff2/fullstep + row0); 
		int row_y0 = pt_main.y;
		for (int i = 0; i<= lastinterval; i++) {
			long diff3 = getnearest(imageTimeMinutes-referenceFileTimeImageFirstMinutes, fullstep);
			XLSUtils.setValue(sheet, pt_main, transpose, "t"+diff3);
			imageTimeMinutes += fullstep ;
			pt_main.y++;
		}
		
		pt_main.y = row_y0 -1;
		int lastFrame = 0;
		if (options.collateSeries && options.padIntervals && exp.nextExperiment != null)
			exp.nextExperiment.loadKymos_Measures();
		
		for (int currentFrame=startFrame; currentFrame < endFrame; currentFrame+= fullstep) {	
			pt_main.x = col0;
			pt_main.y++;
			imageTimeMinutes = exp.seqCamData.getImageFileTime(currentFrame).toMillis()/ 60000;
			XLSUtils.setValue(sheet, pt_main, transpose, imageTimeMinutes);
			pt_main.x++;
			pt_main.x++;
			int colseries = pt_main.x;
			int indexData = (currentFrame - startFrame)/exp.stepFrame;
			switch (option) {
				case TOPLEVEL_LR:
				case TOPLEVELDELTA_LR:
				case SUMGULPS_LR:
					for (int indexDataArrayList=0; indexDataArrayList< dataArrayList.size()-1; indexDataArrayList+=2) {
						XLSCapillaryResults xlsDataL = dataArrayList.get(indexDataArrayList);
						int cage = getCageFromKymoFileName(xlsDataL.name);
						pt_main.x = colseries + cage*2;
						double dataL = getDataL( exp,  dataArrayList, indexDataArrayList, indexData, scalingFactorToPhysicalUnits);
						double dataR = getDataR( exp,  dataArrayList, indexDataArrayList, indexData, scalingFactorToPhysicalUnits);
						if (!Double.isNaN(dataL) || !Double.isNaN(dataR)) {
							if (Double.isNaN(dataL)) dataL = 0;
							if (Double.isNaN(dataR)) dataR = 0;
						}
						double valueL = dataL+dataR;
						if (!Double.isNaN(valueL)) 
							XLSUtils.setValue(sheet, pt_main, transpose, valueL);
						pt_main.x ++;
						double valueR = (dataL-dataR)/valueL;
						if (!Double.isNaN(valueR)) 
							XLSUtils.setValue(sheet, pt_main, transpose, valueR);
						if (!isThisAndNextCapillarySameCage(dataArrayList, indexDataArrayList)) 
							indexDataArrayList--;
					}
					break;
				default:
					for (XLSCapillaryResults xlsData: dataArrayList) {
						double value = xlsData.getAt(indexData, scalingFactorToPhysicalUnits);
						Point pt = getCellXCoordinateFromDataName(xlsData, pt_main, colseries);
						if (!Double.isNaN(value)) 
							XLSUtils.setValue(sheet, pt, transpose, value);
						else  {
							value = getDataForTable (exp, xlsData, indexData, scalingFactorToPhysicalUnits);
							if (!Double.isNaN(value)) {
								XLSUtils.setValue(sheet, pt, transpose, value);
								XLSUtils.getCell(sheet, pt, transpose).setCellStyle(xssfCellStyle_red);
							}
						}
					}
					pt_main.x ++;
				break;
			}
			lastFrame = currentFrame;
		}		
		
		// pad remaining cells with the last value
		if (options.collateSeries && options.padIntervals && exp.nextExperiment != null) {
			Point padpt = new Point(pt_main);
			padpt.x = col0;
			int startNextExpt = (int) (((exp.nextExperiment.fileTimeImageFirstMinute- exp.fileTimeImageFirstMinute)/fullstep)*fullstep);
			if (startNextExpt < (exp.nextExperiment.fileTimeImageFirstMinute- exp.fileTimeImageFirstMinute))
				startNextExpt += fullstep;
			exp.nextExperiment.loadKymos_Measures();
				
			for (int nextFrame= lastFrame; nextFrame <= startNextExpt; nextFrame+= fullstep, padpt.y++) {	
				padpt.x = col0;
				XLSUtils.setValue(sheet, padpt, transpose, "xxxT");
				XLSUtils.getCell(sheet, padpt, transpose).setCellStyle(xssfCellStyle_red);
				padpt.x++;
				if (exp.seqCamData.isFileStack()) {
					XLSUtils.setValue(sheet, padpt, transpose, "xxxF" );
					XLSUtils.getCell(sheet, padpt, transpose).setCellStyle(xssfCellStyle_red);	
				}
				padpt.x++;
				int colseries = padpt.x;
				switch (option) {
					case TOPLEVEL_LR:
					case TOPLEVELDELTA_LR:
					case SUMGULPS_LR:
						for (int idataArray=0; idataArray< dataArrayList.size()-1; idataArray+=2) {
							// get current data array and compute column position
							XLSCapillaryResults dataList = dataArrayList.get(idataArray);
							// see if this set of data needs to be padded up to next experiment
							int cage = getCageFromKymoFileName(dataList.name);
							boolean flag = options.trim_alive;
							if (cage > 0 && cage < 9  && options.trim_alive) {
								flag = exp.nextExperiment.isFlyAlive(cage); 
							} else {
								flag = exp.nextExperiment.isDataAvailable(cage);
							}
							// get position of this set of data
							int colL2 = getColFromKymoFileName(dataList.name);
							padpt.x = colseries + colL2;
							double dataL = 0;
							double dataR = 0;
							// get data from first capillary
							int lastL = dataList.data.size()-1;
							if (lastL >=0)
								dataL = dataList.data.get(lastL)*scalingFactorToPhysicalUnits;
							// get second capillary - capillary nb = cage nb * 2 +1
							if (idataArray < dataArrayList.size()-1) {
								XLSCapillaryResults dataListR = dataArrayList.get(idataArray+1);
								int cageR = getCageFromKymoFileName(dataListR.name);
								if (cage == cageR) {
									int lastR = dataListR.data.size()-1;
									if (lastR >= 0)
										dataR = dataListR.data.get(lastR)*scalingFactorToPhysicalUnits;
								}
								else
									idataArray--;
							}
							// patch data if necessary
							if (flag) {			
								double valueL = (dataL+dataR);
								if (!Double.isNaN(valueL ))
									XLSUtils.setValue(sheet, padpt, transpose, valueL);
								XLSUtils.getCell(sheet, padpt, transpose).setCellStyle(xssfCellStyle_red);
								padpt.x ++;
								double valueR = (dataL-dataR)/valueL;
								if (!Double.isNaN(valueR))
									XLSUtils.setValue(sheet, padpt, transpose, valueR); 
								XLSUtils.getCell(sheet, padpt, transpose).setCellStyle(xssfCellStyle_red);
							}
						}
						break;
					default:
						for (XLSCapillaryResults xlsData: dataArrayList) 
							outputMissingData(sheet, getCellXCoordinateFromDataName(xlsData, padpt, colseries), exp, xlsData, scalingFactorToPhysicalUnits);
						break;
				}
			}
		}
		pt_main.x++;
		return pt_main;
	}
	
	private void outputMissingData(XSSFSheet sheet, Point ptadp, Experiment exp, XLSCapillaryResults xlsData, double scalingFactorToPhysicalUnits) {
		int cage = getCageFromKymoFileName(xlsData.name);
		boolean flag = options.trim_alive;
		if (exp.nextExperiment != null) {
			if (cage >0 && cage < 9 && options.trim_alive) {
				flag = exp.nextExperiment.isFlyAlive(cage);
			} else { 
				flag = exp.nextExperiment.isDataAvailable(cage);
			}
		}
		if (flag) {
			double value = xlsData.getLast(scalingFactorToPhysicalUnits);
			if (!Double.isNaN(value )) {
				XLSUtils.setValue(sheet, ptadp, options.transpose, value);
				XLSUtils.getCell(sheet, ptadp, options.transpose).setCellStyle(xssfCellStyle_red);
			}
		}
	}
	
	private double getDataForTable (Experiment exp, XLSCapillaryResults xlsData, int indexData, double scalingFactorToPhysicalUnits) {
		double value = xlsData.getAt(indexData, scalingFactorToPhysicalUnits);
		if (Double.isNaN(value) && options.collateSeries && options.padIntervals && exp.nextExperiment != null) {
			int cage = getCageFromKymoFileName(xlsData.name);
			boolean flag = options.trim_alive;
			if (options.trim_alive) {
				flag = exp.nextExperiment.isFlyAlive(cage);
			} else { 
				flag = exp.nextExperiment.isDataAvailable(cage);
			}
			if (flag) {
				value = xlsData.getLast(scalingFactorToPhysicalUnits);
			}
		}
		return value;
	}
	
	private double getDataL(Experiment exp, List <XLSCapillaryResults> dataArrayList, int indexDataArrayList, int indexData, double scalingFactorToPhysicalUnits) {
		double value = Double.NaN;
		XLSCapillaryResults xlsData = dataArrayList.get(indexDataArrayList);
		if (indexData < xlsData.data.size()) {
			value =  getDataForTable (exp, xlsData, indexData, scalingFactorToPhysicalUnits);
		}
		return value;
	}
	
	private double getDataR(Experiment exp, List <XLSCapillaryResults> dataArrayList, int indexDataArrayList, int indexData, double scalingFactorToPhysicalUnits) {
		double value = Double.NaN;
		if (isThisAndNextCapillarySameCage(dataArrayList, indexDataArrayList)) {
			XLSCapillaryResults xlsDataR = dataArrayList.get(indexDataArrayList+1);
			if (indexData < xlsDataR.data.size()) {
				value =  getDataForTable (exp, xlsDataR, indexData, scalingFactorToPhysicalUnits);
			}
		}
		return value;
	}
	
	private boolean isThisAndNextCapillarySameCage(List <XLSCapillaryResults> dataArrayList, int indexDataArrayList) {
		XLSCapillaryResults xlsDataL = dataArrayList.get(indexDataArrayList);
		int cageL = getCageFromKymoFileName(xlsDataL.name);
		XLSCapillaryResults xlsDataR = dataArrayList.get(indexDataArrayList+1);
		int cageR = getCageFromKymoFileName(xlsDataR.name);
		return (cageL == cageR);
	}

}
