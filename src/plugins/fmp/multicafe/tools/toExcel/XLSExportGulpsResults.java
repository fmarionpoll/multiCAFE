package plugins.fmp.multicafe.tools.toExcel;

import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.ss.util.CellReference;
import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.sequence.Experiment;


public class XLSExportGulpsResults  extends XLSExport {

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
				
				if (options.sumGulps) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.SUMGULPS);
				if (options.sum_ratio_LR && options.sumGulps) 	
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.SUMGULPS_LR);
				if (options.isGulps)
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.ISGULPS);
				if (options.tToNextGulp)
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TTOGULP);
				if (options.tToNextGulp_LR)
					getDataAndExport(exp, column, charSeries, EnumXLSExportType.TTOGULP_LR);
				
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

}
