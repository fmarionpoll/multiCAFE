package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.usermodel.Row;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.XYTaSeries;

public class XLSExportMoveResults extends XLSExport {

	public void exportToFile(String filename, XLSExportOptions opt) {
		
		System.out.println("XLS move output");
		options = opt;
		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		
		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col_max = 0;
			int col_end = 0;
			int iSeries = 0;

			options.expList.readInfosFromAllExperiments();
			expAll = options.expList.getStartAndEndFromAllExperiments();
			expAll.step = options.expList.experimentList.get(0).seqCamData.analysisStep;
			
			progress.setMessage( "Load measures...");
			progress.setLength(options.expList.experimentList.size());

			for (int index = options.firstExp; index <= options.lastExp; index++) 
			{
				Experiment exp = options.expList.experimentList.get(index);
				String charSeries = CellReference.convertNumToColString(iSeries);
			
				if (options.xyCenter)  	col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries, EnumXLSExportItems.XYCENTER);
				if (options.distance) 	col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries, EnumXLSExportItems.DISTANCE);
				if (options.alive) 		col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries,  EnumXLSExportItems.ISALIVE);
				
				if (col_end > col_max)
					col_max = col_end;
				iSeries++;
				progress.incPosition();
			}
			
			if (options.transpose && options.pivot) { 
				progress.setMessage( "Build pivot tables... ");
				
				String sourceSheetName = null;
				if (options.alive) 
					sourceSheetName = EnumXLSExportItems.ISALIVE.toString();
				else if (options.xyCenter) 
					sourceSheetName = EnumXLSExportItems.XYCENTER.toString();
				else if (options.distance) 
					sourceSheetName = EnumXLSExportItems.DISTANCE.toString();
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
	
	private static List <ArrayList<Double>> getDataFromCages(Experiment exp, EnumXLSExportItems option) {

		List<ArrayList<Double>> arrayList = new ArrayList <ArrayList <Double>> ();
		
		for (XYTaSeries posxyt: exp.seqCamData.cages.flyPositionsList) {
			switch (option) {
			case DISTANCE: 
				arrayList.add((ArrayList<Double>) posxyt.getDoubleArrayList(EnumListType.distance));
				break;
			case ISALIVE:
				arrayList.add((ArrayList<Double>) posxyt.getDoubleArrayList(EnumListType.isalive));
				// TODO add threshold to cleanup data?
				break;
			case XYCENTER:
			default:
				arrayList.add((ArrayList<Double>) posxyt.getDoubleArrayList(EnumListType.xyPosition));
				break;
			}
		}
		return arrayList;
	}

	public int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, int col0, String charSeries, EnumXLSExportItems xlsExportOption) {

		List <ArrayList<Double >> arrayList = getDataFromCages(exp, xlsExportOption);

		XSSFSheet sheet = workBook.getSheet(xlsExportOption.toString());
		if (sheet == null) 
			sheet = workBook.createSheet(xlsExportOption.toString());
		
		Point pt = new Point(col0, 0);

		if (options.collateSeries) {
			pt.x = options.expList.getStackColumnPosition(exp, col0);
		}
		
		pt = writeGlobalInfos(exp, sheet, pt, options.transpose, xlsExportOption);
		pt = writeHeader(exp, sheet, pt, xlsExportOption, options.transpose, charSeries);
		pt = writeData(exp, sheet, pt, xlsExportOption, arrayList, options.transpose, charSeries);
		return pt.x;
	}
	
	private Point writeGlobalInfos(Experiment exp, XSSFSheet sheet, Point pt, boolean transpose, EnumXLSExportItems option) {
		
		int col0 = pt.x;
		
		XLSUtils.setValue(sheet, pt, transpose, "expt");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "name");
		File file = new File(exp.seqCamData.getFileName(0));
		String path = file.getParent();
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, path);
		pt.y++;
		
		pt.x=col0;
		Point pt1 = pt;
		XLSUtils.setValue(sheet, pt, transpose, "n_cages");
		pt1.x++;
		XLSUtils.setValue(sheet, pt, transpose, exp.seqCamData.cages.flyPositionsList.size());
		switch (option) {
		case DISTANCE:
			break;
		case ISALIVE:
			pt1.x++;
			XLSUtils.setValue(sheet, pt, transpose, "threshold");
			pt1.x++;
			XLSUtils.setValue(sheet, pt, transpose, exp.seqCamData.cages.detect.threshold);
			break;
		case XYCENTER:
		default:
			break;
		}

		pt.x=col0;
		pt.y++;
		return pt;
	}

	private Point writeHeader (Experiment exp, XSSFSheet sheet, Point pt, EnumXLSExportItems xlsExportOption, boolean transpose, String charSeries) {
		
		int col0 = pt.x;
		// TODO: check if this is ok
//		pt = writeGenericHeader(exp, sheet, xlsExportOption, pt, transpose, charSeries);
		if (exp.previousExperiment == null)
			writeExperimentDescriptors(exp, charSeries, sheet, pt, transpose);
		
		
		switch (xlsExportOption) {
		case DISTANCE:
			for (XYTaSeries posxyt: exp.seqCamData.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0);
				pt.x++;
			}
			break;
			
		case ISALIVE:
			for (XYTaSeries posxyt: exp.seqCamData.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0);
				pt.x++;
				XLSUtils.setValue(sheet, pt, transpose, name0);
				pt.x++;
			}
			break;
		case XYCENTER:
		default:
			for (XYTaSeries posxyt: exp.seqCamData.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0+".x");
				pt.x++;
				XLSUtils.setValue(sheet, pt, transpose, name0+".y");
				pt.x++;
			}
			break;
		}
		pt.x = col0;
		pt.y++;
		return pt;
	}
	
	private Point writeData (Experiment exp, XSSFSheet sheet, Point pt, EnumXLSExportItems option, List <ArrayList<Double >> dataArrayList, boolean transpose, String charSeries) {
	
		int col0 = pt.x;
		int row0 = pt.y;
		if (charSeries == null)
			charSeries = "t";
		int startFrame 	= (int) exp.seqCamData.analysisStart;
		int endFrame 	= (int) exp.seqCamData.analysisEnd;
		int step 		= expAll.step;
		
		FileTime imageTime = exp.seqCamData.getImageModifiedTime(startFrame);
		long imageTimeMinutes = imageTime.toMillis()/ 60000;
		if (options.absoluteTime && (col0 ==0)) {
			imageTimeMinutes = expAll.fileTimeImageLastMinutes;
			long diff = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinute, step)/ step;
			imageTimeMinutes = expAll.fileTimeImageFirstMinute;
			pt.x = col0;
			for (int i = 0; i<= diff; i++) {
				long diff2 = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinute, step);
				pt.y = (int) (diff2/step + row0); 
				XLSUtils.setValue(sheet, pt, transpose, "t"+diff2);
				imageTimeMinutes += step;
			}
		}
		
//		if (dataArrayList.size() == 0) {
//			pt.x = columnOfNextSeries(exp, option, col0);
//			return pt;
//		}
		
		for (int currentFrame=startFrame; currentFrame< endFrame; currentFrame+= step  * options.pivotBinStep) {
			
			pt.x = col0;
 
			long diff0 = (currentFrame - startFrame)/step;
			imageTime = exp.seqCamData.getImageModifiedTime(currentFrame);
			imageTimeMinutes = imageTime.toMillis()/ 60000;

			if (options.absoluteTime) {
				long diff = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinute, step);
				pt.y = (int) (diff/step + row0);
				diff0 = diff; //getnearest(imageTimeMinutes-exp.fileTimeImageFirst.toMillis()/60000, step);
			} else {
				pt.y = (int) diff0 + row0;
			}
			//XLSUtils.setValue(sheet, pt, transpose, "t"+diff0);
			pt.x++;
			XLSUtils.setValue(sheet, pt, transpose, imageTimeMinutes);
			pt.x++;
			if (exp.seqCamData.isFileStack()) {
				XLSUtils.setValue(sheet, pt, transpose, getShortenedName(exp.seqCamData, currentFrame) );
			}
			pt.x++;
			
			int t = (currentFrame - startFrame)/step;
			switch (option) {
			case DISTANCE:
				for (int idataArray=0; idataArray < dataArrayList.size(); idataArray++ ) 
				{
					XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(idataArray).get(t));
					pt.x++;
					XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(idataArray).get(t));
					pt.x++;
				}
				break;
			case ISALIVE:
				for (int idataArray=0; idataArray < dataArrayList.size(); idataArray++ ) 
				{
					Double value = dataArrayList.get(idataArray).get(t);
					if (value > 0) {
						XLSUtils.setValue(sheet, pt, transpose, value );
						pt.x++;
						XLSUtils.setValue(sheet, pt, transpose, value);
						pt.x++;
					}
					else
						pt.x += 2;
				}
				break;

			case XYCENTER:
			default:
				for (int iDataArray=0; iDataArray < dataArrayList.size(); iDataArray++ ) 
				{
					int iarray = t*2;
					XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(iDataArray).get(iarray));
					pt.x++;

					XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(iDataArray).get(iarray+1));
					pt.x++;
				}
				break;
			}
		} 
		pt.x = columnOfNextSeries(exp, option, col0);
		return pt;
	}

	private int columnOfNextSeries(Experiment exp, EnumXLSExportItems option, int currentcolumn) {
		int n = 2;
		int value = currentcolumn + exp.seqCamData.cages.cageLimitROIList.size() * n + 3;
		return value;
	}
	
	
}
