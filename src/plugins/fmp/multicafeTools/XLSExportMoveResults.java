package plugins.fmp.multicafeTools;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Cages;
import plugins.fmp.multicafeSequence.Experiment;


public class XLSExportMoveResults extends XLSExport {

	public void exportToFile(String filename, XLSExportOptions opt) {
		System.out.println("XLS move output");
		options = opt;
		
		int col_max = 1;
		int col_end = 0;
		int iSeries = 0;
		options.expList.loadAllExperiments(true, true);
		options.expList.chainExperiments(options.collateSeries);
		expAll = options.expList.getStartAndEndFromAllExperiments(options);
		
		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		int nbexpts = options.expList.getSize();
		progress.setLength(nbexpts);
		
		try { 
			workbook = xlsInitWorkbook();
			for (int index = options.firstExp; index <= options.lastExp; index++) {
				Experiment exp = options.expList.getExperiment(index);
				
				progress.setMessage("Export experiment "+ (index+1) +" of "+ nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);
			
				if (options.xyImage)
					col_end = xlsExportToWorkbook(exp, col_max, charSeries, EnumXLSExportType.XYIMAGE);
				if (options.xyTopCage) 
					col_end = xlsExportToWorkbook(exp, col_max, charSeries, EnumXLSExportType.XYTOPCAGE);
				if (options.xyTipCapillaries) 
					col_end = xlsExportToWorkbook(exp, col_max, charSeries, EnumXLSExportType.XYTIPCAPS);
				if (options.distance) 
					col_end = xlsExportToWorkbook(exp, col_max, charSeries, EnumXLSExportType.DISTANCE);
				if (options.alive) 
					col_end = xlsExportToWorkbook(exp, col_max, charSeries,  EnumXLSExportType.ISALIVE);
				
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

	public int xlsExportToWorkbook(Experiment exp, int col0, String charSeries, EnumXLSExportType xlsExportOption) {
		XSSFSheet sheet = xlsInitSheet(xlsExportOption.toString());
		
		Point pt = new Point(col0, 0);
		if (options.collateSeries)
			pt.x = options.expList.getStackColumnPosition(exp, col0);
		if (exp.previousExperiment == null)
			writeExperimentDescriptors(exp, charSeries, sheet, pt, xlsExportOption);
		else
			pt.y += 17;
		
		pt = writeData(exp, sheet, pt, xlsExportOption, options.transpose);
		return pt.x;
	}

	private Point writeData (Experiment exp, XSSFSheet sheet, Point pt_main, EnumXLSExportType option, boolean transpose) {
		int col0 = pt_main.x;
		int startFrame 	= 0;
		int endFrame 	= exp.getSeqCamSizeT()-1;
		int currentFrame = 0;
		for (currentFrame=startFrame; currentFrame< endFrame; currentFrame+= options.buildExcelBinStep) {
			pt_main.x = col0;
			pt_main.y++;
			pt_main.x++;
			pt_main.x++;
			int colseries = pt_main.x;
			Cages cages = exp.cages;
			switch (option) {
				case DISTANCE:
					for (Cage cage: cages.cageList ) {
						int col = getColFromCageName(cage) * 2;
						if (col >= 0)
							pt_main.x = colseries + col;
						int currentTimeIndex = currentFrame - startFrame;
						int previousTimeIndex = currentTimeIndex - options.buildExcelBinStep;
						Double value = cage.flyPositions.getDistanceBetween2Points(previousTimeIndex, currentTimeIndex);
						if (!Double.isNaN(value))
							XLSUtils.setValue(sheet, pt_main, transpose, value);
						pt_main.x++;
						if (!Double.isNaN(value))
							XLSUtils.setValue(sheet, pt_main, transpose, value);
						pt_main.x++;
					}
					break;
				case ISALIVE:
					for (Cage cage: cages.cageList ) {
						int col = getColFromCageName(cage)*2;
						if (col >= 0)
							pt_main.x = colseries + col;
						int value = cage.flyPositions.isAliveAtTimeIndex(currentFrame - startFrame);						
						XLSUtils.setValue(sheet, pt_main, transpose, value );
						pt_main.x++;
						XLSUtils.setValue(sheet, pt_main, transpose, value);
						pt_main.x++;
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
						int currentIndex = currentFrame - startFrame;
						Point2D point = cage.flyPositions.getPointAt(currentIndex);
						if (point != null) 
							XLSUtils.setValue(sheet, pt_main, transpose, point.getX() - pt0.getX());
						pt_main.x++;
						if (point != null) 
							XLSUtils.setValue(sheet, pt_main, transpose, point.getY() - pt0.getY());
						pt_main.x++;
					}
					break;
			}	
		} 
		return pt_main;
	}
}
