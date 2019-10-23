package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.XYTaSeries;



public class XLSExportCapillariesResults extends XLSExport {

	public void exportToFile(String filename, XLSExportOptions opt) {	
		System.out.println("XLS capillary measures output");
		options = opt;
		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		
		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col_max = 1;
			int col_end = 1;
			int iSeries = 0;
			options.expList.readInfosFromAllExperiments(true, false);
			if (options.collateSeries)
				options.expList.chainExperiments();
			expAll 		= options.expList.getStartAndEndFromAllExperiments();
			expAll.step = options.expList.experimentList.get(0).seqCamData.analysisStep;
			
			progress.setMessage("Load measures...");
			progress.setLength(options.expList.experimentList.size());
			
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = options.expList.experimentList.get(index);
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
				if (options.topLevel) sourceSheetName = EnumXLSExportItems.TOPLEVEL.toString();
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
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		progress.close();
		System.out.println("XLS output finished");
	}
	
	private int getDataAndExport(Experiment exp, XSSFWorkbook workbook, int col0, String charSeries, EnumXLSExportItems datatype) {	
		List <XLSCapillaryResults> arrayList = getDataFromRois(exp, datatype, options.t0);	
		int colmax = xlsExportToWorkbook(exp, workbook, datatype.toString(), datatype, col0, charSeries, arrayList);
		if (options.onlyalive) {
			trimDeadsFromArrayList(exp, arrayList);
			xlsExportToWorkbook(exp, workbook, datatype.toString()+"_alive", datatype, col0, charSeries, arrayList);
		}
		return colmax;
	}
	
	private List <XLSCapillaryResults> getDataFromRois(Experiment exp, EnumXLSExportItems xlsoption, boolean optiont0) {	
		List <XLSCapillaryResults> resultsList = new ArrayList <XLSCapillaryResults> ();	
		Capillaries capillaries = exp.seqKymos.capillaries;
		double scalingFactorToPhysicalUnits = capillaries.desc.volume / exp.seqKymos.capillaries.desc.pixels;
		
		for (int t=0; t < capillaries.capillariesArrayList.size(); t++) {
			Capillary cap = capillaries.capillariesArrayList.get(t);
			XLSCapillaryResults results = new XLSCapillaryResults();
			results.name = cap.capillaryRoi.getName(); // exp.seqKymos.getFileNameNoPath(t);
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
						double dvalue = getLastValueOfPreviousExp(exp.seqKymos.getFileName(t), exp.previousExperiment, xlsoption, optiont0);
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
			lastValue =  getLastValueOfPreviousExp( capName,  exp.previousExperiment, xlsoption, optiont0);
		Capillaries capillaries = exp.seqKymos.capillaries;
		
		for (int t=0; t< exp.seqKymos.seq.getSizeT(); t++) {
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
			double valuePreviousSeries = results.data.get(results.data.size()-1) * scalingFactorToPhysicalUnits;
			lastValue = lastValue + valuePreviousSeries;
			return lastValue;
		}
		return lastValue;
	}
	
	private void trimDeadsFromArrayList(Experiment exp, List <XLSCapillaryResults> resultsArrayList) {
		for (XYTaSeries flypos: exp.seqCamData.cages.flyPositionsList) {
			String cagenumberString = flypos.roi.getName().substring(4);
			int cagenumber = Integer.parseInt(cagenumberString);
			if (cagenumber == 0 || cagenumber == 9)
				continue;
			
			for (XLSCapillaryResults capillaryResult : resultsArrayList) {
				if (getCageFromCapillaryName (capillaryResult.name) == cagenumber) {
					if (!isAliveInNextBout(exp.nextExperiment, cagenumber)) {
						int ilastalive = flypos.getLastIntervalAlive();
						trimArrayLength(capillaryResult.data, ilastalive);
					}
				}
			}
		}		
	}
	
	private boolean isAliveInNextBout(Experiment exp, int cagenumber) {
		boolean isalive = false;
		if (exp != null) {
			for (XYTaSeries flypos: exp.seqCamData.cages.flyPositionsList) {
				String cagenumberString = flypos.roi.getName().substring(4);
				if (Integer.parseInt(cagenumberString) != cagenumber)
					continue;
				
				isalive = true;
				break;
			}
		}
		return isalive;
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
	
	private int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, String title, 
			EnumXLSExportItems xlsExportOption, int col0, String charSeries, List <XLSCapillaryResults> arrayList) {
			
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
		
	private Point writeData (	Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt, boolean transpose, 
								String charSeries, List <XLSCapillaryResults> dataArrayList) {
		double scalingFactorToPhysicalUnits = exp.seqKymos.capillaries.desc.volume / exp.seqKymos.capillaries.desc.pixels;
		int col0 = pt.x;
		int row0 = pt.y;
		if (charSeries == null)
			charSeries = "t";
		int startFrame 	= (int) exp.seqCamData.analysisStart;
		int endFrame 	= (int) exp.seqCamData.analysisEnd;
		int step 		= expAll.step;
		FileTime imageTime = exp.seqCamData.getImageModifiedTime(startFrame);
		long imageTimeMinutes = imageTime.toMillis()/ 60000;
		long referenceFileTimeImageFirstMinutes = exp.fileTimeImageFirstMinute;
		long referenceFileTimeImageLastMinutes = exp.fileTimeImageLastMinute;
		if (options.absoluteTime) {
			referenceFileTimeImageFirstMinutes = expAll.fileTimeImageFirstMinute;
			referenceFileTimeImageLastMinutes = expAll.fileTimeImageLastMinute;
		}
			
		pt.x =0;
		long tspanMinutes = referenceFileTimeImageLastMinutes-referenceFileTimeImageFirstMinutes;
		long diff = getnearest(tspanMinutes, step)/ step;
		imageTimeMinutes = referenceFileTimeImageFirstMinutes;
		for (int i = 0; i<= diff; i++) {
			long diff2 = getnearest(imageTimeMinutes-referenceFileTimeImageFirstMinutes, step);
			pt.y = (int) (diff2/step + row0); 
			XLSUtils.setValue(sheet, pt, transpose, "t"+diff2);
			imageTimeMinutes += step;
		}
		
		for (int currentFrame=startFrame; currentFrame < endFrame; currentFrame+=  step * options.pivotBinStep) {	
			pt.x = col0;
			imageTime = exp.seqCamData.getImageModifiedTime(currentFrame);
			imageTimeMinutes = imageTime.toMillis()/ 60000;
//			long diff_current = getnearest(imageTimeMinutes-referenceFileTimeImageFirstMinutes, step);
//			pt.y = (int) (diff_current/step + row0);
			pt.y = (int) ((currentFrame - startFrame)/step + row0);
			XLSUtils.setValue(sheet, pt, transpose, imageTimeMinutes);
			pt.x++;
			if (exp.seqCamData.isFileStack()) {
				XLSUtils.setValue(sheet, pt, transpose, getShortenedName(exp.seqCamData, currentFrame) );
			}
			pt.x++;
			
			int colseries = pt.x;
			switch (option) {
			case TOPLEVEL_LR:
			case TOPLEVELDELTA_LR:
			case SUMGULPS_LR:
				for (int idataArray=0; idataArray< dataArrayList.size()-1; idataArray+=2) {
					int colL = getColFromKymoFileName(dataArrayList.get(idataArray).name);
					if (colL >= 0)
						pt.x = colseries + colL;			
					List<Integer> dataL = dataArrayList.get(idataArray).data ;
					List<Integer> dataR = dataArrayList.get(idataArray+1).data;
					if (dataL != null && dataR != null) {
						int j = (currentFrame - startFrame)/step;
						if (j < dataL.size() && j < dataR.size()) {
							double value = (dataL.get(j)+dataR.get(j))*scalingFactorToPhysicalUnits;
							XLSUtils.setValue(sheet, pt, transpose, value);
							Point pt0 = new Point(pt);
							pt0.x ++;
							int colR = getColFromKymoFileName(dataArrayList.get(idataArray+1).name);
							if (colR >= 0)
								pt0.x = colseries + colR;
							value = (dataL.get(j)-dataR.get(j))*scalingFactorToPhysicalUnits/value;
							XLSUtils.setValue(sheet, pt0, transpose, value);
						}
					}
					pt.x++;
					pt.x++;
				}
				break;
			default:
				for (int idataArray=0; idataArray< dataArrayList.size(); idataArray++) {
					int col = getColFromKymoFileName(dataArrayList.get(idataArray).name);
					if (col >= 0)
						pt.x = colseries + col;			
					List<Integer> data = dataArrayList.get(idataArray).data;
					if (data != null) {
						int j = (currentFrame - startFrame)/step;
						if (j < data.size()) {
							double value = data.get(j)*scalingFactorToPhysicalUnits;
							XLSUtils.setValue(sheet, pt, transpose, value);
						}
					}
					pt.x++;
				}
				break;
			}
		}
		return pt;
	}
		
}