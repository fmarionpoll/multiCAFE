package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.XYTaSeries;



public class XLSExportCapillariesResults extends XLSExport {

	public void exportToFile(String filename, XLSExportOptions opt) {	
		System.out.println("XLS capillary measures output");
		options = opt;

		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col_max = 1;
			int col_end = 1;
			int iSeries = 0;
			options.expList.readInfosFromAllExperiments(true, options.onlyalive);
			options.expList.chainExperiments(options);
			expAll 		= options.expList.getStartAndEndFromAllExperiments(options);
			expAll.step = options.expList.experimentList.get(0).seqCamData.analysisStep;
			int nbexpts = options.expList.experimentList.size();
			ProgressFrame progress = new ProgressFrame("Export data to Excel");
			progress.setLength(nbexpts);
			
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = options.expList.experimentList.get(index);
				
				progress.setMessage("Export experiment "+ (index+1) +" of "+ nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);
				if (options.topLevel) 		col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVEL);
				if (options.bottomLevel) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.BOTTOMLEVEL);		
				if (options.derivative) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.DERIVEDVALUES);	
				if (options.consumption) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.SUMGULPS);
				if (options.sum) {		
					if (options.topLevel) 		col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVEL_LR);
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
		List <XLSCapillaryResults> arrayList = getDataFromCapillaryMeasures(exp, datatype, options.t0);	
		int colmax = xlsExportToWorkbook(exp, workbook, datatype.toString(), datatype, col0, charSeries, arrayList);
		if (options.onlyalive) {
			trimDeadsFromArrayList(exp, arrayList);
			xlsExportToWorkbook(exp, workbook, datatype.toString()+"_alive", datatype, col0, charSeries, arrayList);
		}
		return colmax;
	}
	
	private List <XLSCapillaryResults> getDataFromCapillaryMeasures(Experiment exp, EnumXLSExportItems xlsoption, boolean optiont0) {	
		List <XLSCapillaryResults> resultsList = new ArrayList <XLSCapillaryResults> ();	
		Capillaries capillaries = exp.seqKymos.capillaries;
		double scalingFactorToPhysicalUnits = capillaries.desc.volume / exp.seqKymos.capillaries.desc.pixels;
		
		for (int t=0; t < capillaries.capillariesArrayList.size(); t++) {
			Capillary cap = capillaries.capillariesArrayList.get(t);
			XLSCapillaryResults results = new XLSCapillaryResults();
			results.name = cap.capillaryRoi.getName(); 
			switch (xlsoption) {
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
					double dvalue = getLastValueOfPreviousExp(cap.getName(), exp.previousExperiment, xlsoption, optiont0);
					int addedValue = (int) (dvalue / scalingFactorToPhysicalUnits);
					results.data = exp.seqKymos.addConstant(cap.getMeasures(EnumListType.cumSum), addedValue);
				}
				else
					results.data = cap.getMeasures(EnumListType.cumSum);
				break;
			case BOTTOMLEVEL:
				results.data = cap.getMeasures(EnumListType.bottomLevel);
				break;
			case TOPLEVEL:
			case TOPLEVEL_LR:
				if (optiont0) {
					if (options.collateSeries && exp.previousExperiment != null) {
						double dvalue = getLastValueOfPreviousExp(cap.getName(), exp.previousExperiment, xlsoption, optiont0);
						int addedValue = (int) (dvalue / scalingFactorToPhysicalUnits);
						results.data = exp.seqKymos.subtractT0AndAddConstant(cap.getMeasures(EnumListType.topLevel), addedValue);
					}
					else
						results.data = exp.seqKymos.subtractT0(cap.getMeasures(EnumListType.topLevel));
				}
				else
					results.data = cap.getMeasures(EnumListType.topLevel);
				break;
			default:
				break;
			}
			resultsList.add(results);
		}
		return resultsList;
	}
	
	private double getLastValueOfPreviousExp(String capName, Experiment exp, EnumXLSExportItems xlsoption, boolean optiont0) {
		double lastValue = 0;
		if (exp.previousExperiment != null)
			lastValue =  getLastValueOfPreviousExp(capName,  exp.previousExperiment, xlsoption, optiont0);
		
		Capillaries capillaries = exp.seqKymos.capillaries;
		int ncapillaries = capillaries.capillariesArrayList.size();
		for (int t=0; t< ncapillaries; t++) {
			Capillary cap = capillaries.capillariesArrayList.get(t);			
			if (!cap.getName().equals(capName))
				continue;
			
			XLSCapillaryResults results = new XLSCapillaryResults();
			results.name = cap.getName();
			switch (xlsoption) {
			case SUMGULPS:
			case SUMGULPS_LR:
				results.data = cap.getMeasures(EnumListType.cumSum);
				break;
			case TOPLEVEL:
			case TOPLEVEL_LR:
				if (optiont0) 
					results.data = exp.seqKymos.subtractT0(cap.getMeasures(EnumListType.topLevel));
				else
					results.data = cap.getMeasures(EnumListType.topLevel);
				break;
			default:
				return lastValue;
			}
			double scalingFactorToPhysicalUnits = exp.seqKymos.capillaries.desc.volume / exp.seqKymos.capillaries.desc.pixels;
			int previousValue = 0; 
			if (results.data == null) {
				System.out.println("previous data series of capillary "+t+ "is not defined - file: "+exp.seqCamData.getDirectory());
			} else {
				int indexprevious = results.data.size()-1;
				if (indexprevious < 0)
					System.out.println("size of data series of capillary "+t+ "is less than 1");
				else
					previousValue = results.data.get(indexprevious);
			}
			double valuePreviousSeries = previousValue * scalingFactorToPhysicalUnits;
			lastValue = lastValue + valuePreviousSeries;
			break;
		}
		return lastValue;
	}
	
	private void trimDeadsFromArrayList(Experiment exp, List <XLSCapillaryResults> resultsArrayList) {
 		for (Cage cage: exp.seqCamData.cages.cageList) {
			XYTaSeries flypos = cage.flyPositions;		
			String cagenumberString = flypos.roi.getName().substring(4);
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
					trimArrayLength(capillaryResult.data, flypos.lastTimeAlive);
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
	
	private int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, String title, EnumXLSExportItems xlsExportOption, 
									int col0, String charSeries, List <XLSCapillaryResults> arrayList) {
		XSSFSheet sheet = workBook.getSheet(title);
		if (sheet == null) {
			sheet = workBook.createSheet(title);
			outputFieldHeaders(sheet, options.transpose);
		}
		Point pt = new Point(col0, 0);
		if (options.collateSeries) {
			pt.x = options.expList.getStackColumnPosition(exp, col0);
		}
		pt = writeSeriesInfos(exp, sheet, xlsExportOption, pt, options.transpose, charSeries);
		pt = writeData(exp, sheet, xlsExportOption, pt, options.transpose, charSeries, arrayList);
		return pt.x;
	}
	
	private Point writeSeriesInfos (Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt, boolean transpose, String charSeries) {	
		if (exp.previousExperiment == null)
			writeExperimentDescriptors(exp, charSeries, sheet, pt, transpose);
		else
			pt.y += 17;
		return pt;
	}
		
	private Point writeData (Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt_main, boolean transpose, 
								String charSeries, List <XLSCapillaryResults> dataArrayList) {
		double scalingFactorToPhysicalUnits = exp.seqKymos.capillaries.desc.volume / exp.seqKymos.capillaries.desc.pixels;
		int col0 = pt_main.x;
		int row0 = pt_main.y;
		if (charSeries == null)
			charSeries = "t";
		int startFrame 	= (int) exp.seqCamData.analysisStart;
		int endFrame 	= (int) exp.seqCamData.analysisEnd;
		int step 		= expAll.step * options.pivotBinStep;
		long imageTimeMinutes = exp.seqCamData.getImageFileTime(startFrame).toMillis()/ 60000;
		long referenceFileTimeImageFirstMinutes = exp.getFileTimeImageFirst(true).toMillis()/60000;
		long referenceFileTimeImageLastMinutes = exp.getFileTimeImageLast(true).toMillis()/60000;
		if (options.absoluteTime) {
			referenceFileTimeImageFirstMinutes = expAll.fileTimeImageFirstMinute;
			referenceFileTimeImageLastMinutes = expAll.fileTimeImageLastMinute;
		}
			
		pt_main.x =0;
		long tspanMinutes = referenceFileTimeImageLastMinutes-referenceFileTimeImageFirstMinutes;
		long diff = getnearest(tspanMinutes, step)/ step;
		
		long firstImageTimeMinutes = exp.getFileTimeImageFirst(false).toMillis()/60000;;
		long diff2 = getnearest(firstImageTimeMinutes-referenceFileTimeImageFirstMinutes, step);
		pt_main.y = (int) (diff2/step + row0); 
		int row_y0 = pt_main.y;
		for (int i = 0; i<= diff; i++) {
			diff2 = getnearest(imageTimeMinutes-referenceFileTimeImageFirstMinutes, step);
			XLSUtils.setValue(sheet, pt_main, transpose, "t"+diff2);
			imageTimeMinutes += step ;
			pt_main.y++;
		}
		
		pt_main.y = row_y0 -1;
		double valueL = 0.;
		double valueR = 0.;
		int currentFrame = 0;
		
//		System.out.println("output "+exp.experimentFileName +" startFrame=" + startFrame +" endFrame="+endFrame );
		for (currentFrame=startFrame; currentFrame < endFrame; currentFrame+=  options.pivotBinStep) {	
			pt_main.x = col0;
			pt_main.y++;
			imageTimeMinutes = exp.seqCamData.getImageFileTime(currentFrame).toMillis()/ 60000;
			XLSUtils.setValue(sheet, pt_main, transpose, imageTimeMinutes);
			pt_main.x++;
			if (exp.seqCamData.isFileStack())
				XLSUtils.setValue(sheet, pt_main, transpose, getShortenedName(exp.seqCamData, currentFrame) );
			pt_main.x++;
			
			int colseries = pt_main.x;
			switch (option) {
			case TOPLEVEL_LR:
			case TOPLEVELDELTA_LR:
			case SUMGULPS_LR:
				for (int idataArray=0; idataArray< dataArrayList.size()-1; idataArray+=2) {
					int colL = getColFromKymoFileName(dataArrayList.get(idataArray).name);
					if (colL >= 0)
						pt_main.x = colseries + colL;			
					List<Integer> dataL = dataArrayList.get(idataArray).data ;
					List<Integer> dataR = dataArrayList.get(idataArray+1).data;
					if (dataL != null && dataR != null) {
						int j = currentFrame - startFrame;
						if (j < dataL.size() && j < dataR.size()) {
							Point pt0 = new Point(pt_main);
							valueL = (dataL.get(j)+dataR.get(j))*scalingFactorToPhysicalUnits;
							XLSUtils.setValue(sheet, pt0, transpose, valueL);
							pt0.x ++;
							valueR = (dataL.get(j)-dataR.get(j))*scalingFactorToPhysicalUnits/valueL;
							XLSUtils.setValue(sheet, pt0, transpose, valueR);
						}
					}
					pt_main.x++;
					pt_main.x++;
				}
				break;
			default:
				for (int idataArray=0; idataArray< dataArrayList.size(); idataArray++) {
					int col = getColFromKymoFileName(dataArrayList.get(idataArray).name);
					if (col >= 0)
						pt_main.x = colseries + col;			
					List<Integer> data = dataArrayList.get(idataArray).data;
					if (data != null) {
						int j = currentFrame - startFrame;
						if (j < data.size()) {
							valueL = data.get(j)*scalingFactorToPhysicalUnits;
							XLSUtils.setValue(sheet, pt_main, transpose, valueL);
						}
					}
					pt_main.x++;
				}
				break;
			}
		}
		
		// pad remaining cells with the last value
		if (options.collateSeries && options.padIntervals && exp.nextExperiment != null) {
			Point padpt = new Point(pt_main);
			padpt.x = col0;
			int startNextExpt = (int) (exp.nextExperiment.fileTimeImageFirstMinute- exp.fileTimeImageFirstMinute);
//			System.out.println( "pad to " + exp.nextExperiment.experimentFileName +" from currentFrame=" + currentFrame +" startNextExpt="+startNextExpt );
			for (int nextFrame=currentFrame; nextFrame <= startNextExpt; nextFrame+=  options.pivotBinStep) {	
				padpt.x = col0;
				padpt.y++;
				XLSUtils.setValue(sheet, padpt, transpose, "xxxT");
				padpt.x++;
				if (exp.seqCamData.isFileStack())
					XLSUtils.setValue(sheet, padpt, transpose, "xxxF" );
				padpt.x++;
				
				int colseries = padpt.x;
				switch (option) {
				case TOPLEVEL_LR:
				case TOPLEVELDELTA_LR:
				case SUMGULPS_LR:
					for (int idataArray=0; idataArray< dataArrayList.size()-1; idataArray+=2) {
						int colL = getColFromKymoFileName(dataArrayList.get(idataArray).name);
						padpt.x = colseries + colL;
						boolean flag = true;
						if (colL >1 && colL < 18) {
							flag = exp.nextExperiment.isFlyAlive(colL/2); 
						} else {
							flag = exp.nextExperiment.isDataAvailable(colL/2);
						}
						if (flag) {
							if (colL >= 0)
								padpt.x = colseries + colL;			
							List<Integer> dataL = dataArrayList.get(idataArray).data ;
							List<Integer> dataR = dataArrayList.get(idataArray+1).data;
							if (dataL != null && dataR != null) {
								int j = dataL.size()-1;
								int k = dataR.size()-1;
								if (j != k)
									System.out.println("idataArray=" + idataArray+" j="+j+ " k="+k+ " difference="+ (j-k));
								valueL = (dataL.get(j)+dataR.get(k))*scalingFactorToPhysicalUnits;
								XLSUtils.setValue(sheet, padpt, transpose, valueL); // "xxxL");
								padpt.x ++;
								valueR = (dataL.get(j)-dataR.get(k))*scalingFactorToPhysicalUnits/valueL;
								XLSUtils.setValue(sheet, padpt, transpose, valueR); // "xxxR");
							}
						}
					}
					break;
				default:
					for (int idataArray=0; idataArray< dataArrayList.size(); idataArray++) {
						int col = getColFromKymoFileName(dataArrayList.get(idataArray).name);						
						padpt.x = colseries + col;		
						boolean flag = true;
						if (col >1 && col < 18) {
							flag = exp.nextExperiment.isFlyAlive(col/2); 
						} else {
							flag = exp.nextExperiment.isDataAvailable(col/2);
						}
						if (flag) {
							XLSUtils.setValue(sheet, padpt, transpose, "xxx-");
							List<Integer> data = dataArrayList.get(idataArray).data;
							if (data != null) {
								int j = data.size()-1; 
								valueL = data.get(j)*scalingFactorToPhysicalUnits;
								XLSUtils.setValue(sheet, padpt, transpose, valueL); // "xxx-");
							} else {
								System.out.println("skip because data is null");
							}
						}
					}
					break;
				}
			}
		}
		return pt_main;
	}
		
}
