package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Cages;
import plugins.fmp.multicafeSequence.Experiment;


public class XLSExportMoveResults extends XLSExport {

	public void exportToFile(String filename, XLSExportOptions opt) {
		
		System.out.println("XLS move output");
		options = opt;
		options.expList.loadAllExperiments(true, true);
		options.expList.chainExperiments(options.collateSeries);
		expAll = options.expList.getStartAndEndFromAllExperiments(options);
		int col_max = 1;
		int col_end = 0;
		int iSeries = 0;
		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		int nbexpts = options.expList.getSize();
		progress.setLength(nbexpts);
		
		try { 
			XSSFWorkbook workbook = xlsInitWorkbook();
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = options.expList.getExperiment(index);
				
				progress.setMessage("Export experiment "+ (index+1) +" of "+ nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);
			
				if (options.xyImage)
					col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries, EnumXLSExportType.XYIMAGE);
				if (options.xyTopCage) 
					col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries, EnumXLSExportType.XYTOPCAGE);
				if (options.xyTipCapillaries) 
					col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries, EnumXLSExportType.XYTIPCAPS);
				if (options.distance) 
					col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries, EnumXLSExportType.DISTANCE);
				if (options.alive) 
					col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries,  EnumXLSExportType.ISALIVE);
				
				if (col_end > col_max)
					col_max = col_end;
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

	public int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, int col0, String charSeries, EnumXLSExportType xlsExportOption) {
		XSSFSheet sheet = workBook.getSheet(xlsExportOption.toString());
		if (sheet == null) {
			sheet = workBook.createSheet(xlsExportOption.toString());
			outputFieldDescriptors(sheet);
		}
		Point pt = new Point(col0, 0);
		if (options.collateSeries)
			pt.x = options.expList.getStackColumnPosition(exp, col0);
		if (exp.previousExperiment == null)
			writeExperimentDescriptors(exp, charSeries, sheet, pt, xlsExportOption);
		else
			pt.y += 17;
		
		pt = writeData(exp, sheet, pt, xlsExportOption, options.transpose, charSeries);
		return pt.x;
	}

	private Point writeData (Experiment exp, XSSFSheet sheet, Point pt_main, EnumXLSExportType option, boolean transpose, String charSeries) {
		int col0 = pt_main.x;
		int row0 = pt_main.y;
		if (charSeries == null)
			charSeries = "t";

		int startFrame 	= 0;
		int endFrame 	= exp.getSeqCamSizeT()-1;
		int step 		= options.buildExcelBinStep;
		
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
			long diff3 = getnearest(imageTimeMinutes-referenceFileTimeImageFirstMinutes, step);
			XLSUtils.setValue(sheet, pt_main, transpose, "t"+diff3);
			imageTimeMinutes += step ;
			pt_main.y++;
		}
		
		pt_main.y = row_y0 -1;
		int currentFrame = 0;
		int previousIndex = 0;
		int currentIndex = 0;
		
//		System.out.println("output "+exp.experimentFileName +" startFrame=" + startFrame +" endFrame="+endFrame);
		for (currentFrame=startFrame; currentFrame< endFrame; currentFrame+= options.buildExcelBinStep) {
			pt_main.x = col0;
			pt_main.y++;
			imageTimeMinutes = exp.seqCamData.getImageFileTime(currentFrame).toMillis()/ 60000;
			XLSUtils.setValue(sheet, pt_main, transpose, imageTimeMinutes);
			pt_main.x++;
			if (exp.seqCamData.isFileStack()) 
				XLSUtils.setValue(sheet, pt_main, transpose, getShortenedName(exp.seqCamData, currentFrame) );
			pt_main.x++;
			
			int colseries = pt_main.x;
			Cages cages = exp.cages;
			currentIndex = currentFrame - startFrame;
			switch (option) {
			case DISTANCE:
				for (Cage cage: cages.cageList ) {
					int col = getColFromCageName(cage) * 2;
					if (col >= 0)
						pt_main.x = colseries + col;
					Double value = cage.flyPositions.getDistanceBetweenValidPoints(previousIndex, currentIndex);
					XLSUtils.setValue(sheet, pt_main, transpose, value);
					pt_main.x++;
					XLSUtils.setValue(sheet, pt_main, transpose, value);
					pt_main.x++;
				}
				break;
				
			case ISALIVE:
				for (Cage cage: cages.cageList ) {
					int col = getColFromCageName(cage)*2;
					if (col >= 0)
						pt_main.x = colseries + col;
					int value = cage.flyPositions.isAliveAt(currentIndex);
					if (value > 0) {
						XLSUtils.setValue(sheet, pt_main, transpose, value );
						pt_main.x++;
						XLSUtils.setValue(sheet, pt_main, transpose, value);
						pt_main.x++;
					}
					else
						pt_main.x += 2;
				}
				break;

			case XYIMAGE:
			case XYTOPCAGE:
			case XYTIPCAPS:
			default:
				for (Cage cage: cages.cageList ) {
					Point2D pt0 = new Point2D.Double(0, 0);
					switch (option) {
					case XYTOPCAGE:
						pt0 = cage.getCenterTopCage();
						break;
					case XYTIPCAPS: 
						pt0 = cage.getCenterTipCapillaries(exp.capillaries);
						break;
					default:
						break;
					}
					int col = getColFromCageName(cage)*2;
					if (col >= 0)
						pt_main.x = colseries + col;			
					Point2D point = cage.flyPositions.getPointNearestTo(currentIndex);
					if (point != null) 
						XLSUtils.setValue(sheet, pt_main, transpose, point.getX() - pt0.getX());
					pt_main.x++;
					if (point != null) 
						XLSUtils.setValue(sheet, pt_main, transpose, point.getY() - pt0.getY());
					pt_main.x++;
				}
				break;
			}
			previousIndex = currentIndex;
		} 
		return pt_main;
	}
}
