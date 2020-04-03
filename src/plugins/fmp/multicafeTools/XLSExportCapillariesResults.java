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



public class XLSExportCapillariesResults extends XLSExport {
	ExperimentList expList = null;
	
	public void exportToFile(String filename, XLSExportOptions opt) {	
		System.out.println("XLS capillary measures output");
		options = opt;
		expList = options.expList;

		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col_max = 1;
			int col_end = 1;
			int iSeries = 0;
			expList.readInfosFromAllExperiments(true, options.onlyalive);
			if (options.collateSeries)
				expList.chainExperiments();
			expAll 		= expList.getStartAndEndFromAllExperiments(options);
			expAll.stepFrame = expList.experimentList.get(0).stepFrame;
			int nbexpts = expList.experimentList.size();
			ProgressFrame progress = new ProgressFrame("Export data to Excel");
			progress.setLength(nbexpts);
			
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = expList.experimentList.get(index);
				progress.setMessage("Export experiment "+ (index+1) +" of "+ nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);
				if (options.topLevel) 		col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVEL);
				if (options.topLevelDelta) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVELDELTA);
				if (options.bottomLevel) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.BOTTOMLEVEL);		
				if (options.derivative) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.DERIVEDVALUES);	
				if (options.consumption) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.SUMGULPS);
				if (options.sum) {		
					if (options.topLevel) 		col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVEL_LR);
					if (options.topLevelDelta) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVELDELTA_LR);
					if (options.consumption) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.SUMGULPS_LR);
				}
				if (col_end > col_max)
					col_max = col_end;
				iSeries++;
				progress.incPosition();
			}
			
			if (options.transpose && options.pivot) {
				progress.setMessage( "Build pivot tables... ");
				String sourceSheetName = null;
				if (options.topLevel) 			sourceSheetName = EnumXLSExportItems.TOPLEVEL.toString();
				else if (options.topLevelDelta) sourceSheetName = EnumXLSExportItems.TOPLEVELDELTA.toString();
				else if (options.bottomLevel)  	sourceSheetName = EnumXLSExportItems.BOTTOMLEVEL.toString();
				else if (options.derivative) 	sourceSheetName = EnumXLSExportItems.DERIVEDVALUES.toString();	
				else if (options.consumption) 	sourceSheetName = EnumXLSExportItems.SUMGULPS.toString();
				else if (options.sum) 			sourceSheetName = EnumXLSExportItems.TOPLEVEL_LR.toString();
				if (sourceSheetName != null)
					xlsCreatePivotTables(workbook, sourceSheetName);
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
		List <XLSCapillaryResults> resultsArrayList = getDataFromCapillaryMeasures(exp, datatype, options.t0);		
		int colmax = xlsExportToWorkbook(exp, workbook, datatype.toString(), datatype, col0, charSeries, resultsArrayList);
		if (options.onlyalive) {
			trimDeadsFromArrayList(exp, resultsArrayList);
			xlsExportToWorkbook(exp, workbook, datatype.toString()+"_alive", datatype, col0, charSeries, resultsArrayList);
		}
		return colmax;
	}
	
	private List <XLSCapillaryResults> getDataFromCapillaryMeasures(Experiment exp, EnumXLSExportItems xlsoption, boolean optiont0) {	
		List <XLSCapillaryResults> resultsArrayList = new ArrayList <XLSCapillaryResults> ();	
		Capillaries capillaries = exp.capillaries;
		double scalingFactorToPhysicalUnits = capillaries.desc.volume / exp.capillaries.desc.pixels;
		for (Capillary cap: capillaries.capillariesArrayList) {
			XLSCapillaryResults results = new XLSCapillaryResults();
			results.name = cap.capillaryRoi.getName(); 
			switch (xlsoption) {
			case TOPLEVEL:
			case TOPLEVEL_LR:
				if (optiont0) {
					if (options.collateSeries && exp.previousExperiment != null) {
						double dvalue = getLastPhysicalValue_Of_PreviousChain_Of_Exp(cap.getName(), exp.previousExperiment, xlsoption, optiont0);
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
				results.data = exp.seqKymos.subtractTi(cap.getMeasures(EnumListType.topLevel));
				break;
			case DERIVEDVALUES:
				results.data = cap.getMeasures(EnumListType.derivedValues);
				break;
			case SUMGULPS:
			case SUMGULPS_LR:
				if (options.collateSeries && exp.previousExperiment != null) {
					double dvalue = getLastPhysicalValue_Of_PreviousChain_Of_Exp(cap.getName(), exp.previousExperiment, xlsoption, optiont0);
					int addedValue = (int) (dvalue / scalingFactorToPhysicalUnits);
					results.data = exp.seqKymos.addConstant(cap.getMeasures(EnumListType.cumSum), addedValue);
				}
				else {
					results.data = cap.getMeasures(EnumListType.cumSum);
				}
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
	
	private double getLastPhysicalValue_Of_PreviousChain_Of_Exp(String capName, Experiment exp, EnumXLSExportItems xlsoption, boolean optiont0) {
		double valuePreviousSeries = 0.;
		if (exp.previousExperiment != null)
			valuePreviousSeries = getLastPhysicalValue_Of_PreviousChain_Of_Exp(capName,  exp.previousExperiment, xlsoption, optiont0);
		
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
						if (optiont0)
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
			
			if (options.collateSeries && exp.nextExperiment != null) {
				if(exp.nextExperiment.isFlyAlive(cagenumber))
					continue; 
			}
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
		for (int i=array.size()-1; i> ilastalive; i--)
			array.remove(i);
	}
	
	private int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, String title, EnumXLSExportItems xlsExportOption, int col0, String charSeries, List <XLSCapillaryResults> arrayList) {
		XSSFSheet sheet = workBook.getSheet(title);
		if (sheet == null) {
			sheet = workBook.createSheet(title);
			outputFieldHeaders(sheet, options.transpose);
		}
		Point pt = new Point(col0, 0);
		if (options.collateSeries) 
			pt.x = expList.getStackColumnPosition(exp, col0);
		
		xssfCellStyle_red = workBook.createCellStyle();
	    font_red = workBook.createFont();
	    font_red.setColor(HSSFColor.HSSFColorPredefined.RED.getIndex());
	    xssfCellStyle_red.setFont(font_red);
	    
		xssfCellStyle_blue = workBook.createCellStyle();
	    font_blue = workBook.createFont();
	    font_blue.setColor(HSSFColor.HSSFColorPredefined.BLUE.getIndex());
	    xssfCellStyle_blue.setFont(font_blue);
		
		pt = writeSeriesInfos(exp, sheet, xlsExportOption, pt, options.transpose, charSeries);
		pt = writeData(exp, sheet, xlsExportOption, pt, options.transpose, charSeries, arrayList);
		return pt.x;
	}
	
	private Point writeSeriesInfos (Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt, boolean transpose, String charSeries) {	
		if (exp.previousExperiment == null)
			writeExperimentDescriptors(exp, charSeries, sheet, pt, transpose);
		else
			pt.y += 17;  // n descriptor columns = 17
		return pt;
	}
		
	private Point writeData (Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt_main, boolean transpose, String charSeries, List <XLSCapillaryResults> dataArrayList) {
		double scalingFactorToPhysicalUnits = exp.capillaries.desc.volume / exp.capillaries.desc.pixels;
		int col0 = pt_main.x;
		int row0 = pt_main.y;
		if (charSeries == null)
			charSeries = "t";
		int startFrame 	= (int) exp.startFrame;
		int endFrame 	= (int) exp.endFrame;
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
						int colL = getColFromKymoFileName(xlsDataL.name);
						if (indexData < xlsDataL.data.size()) {
							double dataL = xlsDataL.data.get(indexData) ;
							int colR = colL;
							XLSCapillaryResults dataListR = null;
							double dataR = 0.0;
							if (indexDataArrayList < dataArrayList.size()-1) {
								dataListR = dataArrayList.get(indexDataArrayList+1);
								colR = getColFromKymoFileName(dataListR.name);
								if (colR == colL +1)
									if (indexData < dataListR.data.size())
										dataR = dataListR.data.get(indexData);
								else
									indexDataArrayList--;
							}
							if (colL >= 0)
								pt_main.x = colseries + colL;
							Point pt0 = new Point(pt_main);
							double valueL = (dataL+dataR)*scalingFactorToPhysicalUnits;
							if (!Double.isNaN(valueL ))
								XLSUtils.setValue(sheet, pt0, transpose, valueL);
							pt0.x ++;
							double valueR = (dataL-dataR)*scalingFactorToPhysicalUnits/valueL;
							if (!Double.isNaN(valueR))
								XLSUtils.setValue(sheet, pt0, transpose, valueR);
						}
					}
					pt_main.x += 2;
				break;
				default:
					for (XLSCapillaryResults xlsData: dataArrayList) {
						double value = xlsData.getAt(indexData, scalingFactorToPhysicalUnits);
						if (!Double.isNaN(value )) 
							XLSUtils.setValue(sheet, getCellXCoordinateFromDataName(xlsData, pt_main, colseries), transpose, value);
//						else if (options.collateSeries && options.padIntervals && exp.nextExperiment != null) 
//							outputMissingData(sheet, getCellXCoordinateFromDataName(xlsData, pt_main, colseries), transpose, exp, xlsData, scalingFactorToPhysicalUnits);
					}
					pt_main.x ++;
				break;
			}
			lastFrame = currentFrame;
		}		
		
		// pad remaining cells with the last value
		// TODO suppress this?
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
							XLSCapillaryResults dataListL = dataArrayList.get(idataArray);
							int colL2 = getColFromKymoFileName(dataListL.name);
							padpt.x = colseries + colL2;
							boolean flag = true;
							if (colL2 >1 && colL2 < 18) {
								flag = exp.nextExperiment.isFlyAlive(colL2/2); 
							} else {
								flag = exp.nextExperiment.isDataAvailable(colL2/2);
							}
							if (flag) {
								if (colL2 >= 0)
									padpt.x = colseries + colL2;			
								int lastL = dataListL.data.size()-1;
								double dataL = dataListL.data.get(lastL);
								double dataR = 0;
								if (idataArray < dataArrayList.size()-1) {
									XLSCapillaryResults dataListR = dataArrayList.get(idataArray+1);
									int colR = getColFromKymoFileName(dataListR.name);
									if (colR == colL2 +1) {
										int lastR = dataListR.data.size()-1;
										dataR = dataListR.data.get(lastR)*scalingFactorToPhysicalUnits;
									}
									else
										idataArray--;
								}
								double valueL = (dataL+dataR)*scalingFactorToPhysicalUnits;
								if (!Double.isNaN(valueL ))
									XLSUtils.setValue(sheet, padpt, transpose, valueL);
								XLSUtils.getCell(sheet, padpt, transpose).setCellStyle(xssfCellStyle_red);
								padpt.x ++;
								double valueR = (dataL-dataR)*scalingFactorToPhysicalUnits/valueL;
								if (!Double.isNaN(valueR))
									XLSUtils.setValue(sheet, padpt, transpose, valueR); 
								XLSUtils.getCell(sheet, padpt, transpose).setCellStyle(xssfCellStyle_red);
							}
						}
						break;
					default:
						for (XLSCapillaryResults xlsData: dataArrayList) {
							outputMissingData(sheet, getCellXCoordinateFromDataName(xlsData, padpt, colseries), transpose, exp, xlsData, scalingFactorToPhysicalUnits);
						}
						break;
				}
			}
		}
		return pt_main;
	}
	
	private void outputMissingData(XSSFSheet sheet, Point ptadp, boolean transpose, Experiment exp, XLSCapillaryResults xlsData, double scalingFactorToPhysicalUnits) {
		int cage = getCageIndexFromKymoFileName(xlsData.name);
//		int col = getColFromKymoFileName(xlsData.name);
//		System.out.println(xlsData.name + " -> col="+col + " cage="+cage+" pt.x="+ptadp.x);
		boolean flag = false;
		if (cage >0 && cage < 9)
			flag = exp.nextExperiment.isFlyAlive(cage); 
		else 
			flag = exp.nextExperiment.isDataAvailable(cage);
		if (flag) {
			double value = xlsData.getLast(scalingFactorToPhysicalUnits);
			if (!Double.isNaN(value )) {
				XLSUtils.setValue(sheet, ptadp, transpose, value);
				XLSUtils.getCell(sheet, ptadp, transpose).setCellStyle(xssfCellStyle_red);
			}
		}
	}

}
