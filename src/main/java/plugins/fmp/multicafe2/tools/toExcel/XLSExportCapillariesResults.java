package plugins.fmp.multicafe2.tools.toExcel;

import java.awt.Point;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe2.experiment.Capillaries;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;



public class XLSExportCapillariesResults extends XLSExport 
{	
	public void exportToFile(String filename, XLSExportOptions opt) 
	{	
		System.out.println("XLS capillary measures output");
		options = opt;
		expList = options.expList;
		
		boolean loadCapillaries = true;
		boolean loadDrosoTrack =  options.onlyalive;
		expList.loadAllExperiments(loadCapillaries, loadDrosoTrack);
		expList.chainExperimentsUsingKymoIndexes(options.collateSeries);
		expList.setFirstImageForAllExperiments(options.collateSeries);
		expAll = expList.getMsColStartAndEndFromAllExperiments(options);
	
		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		int nbexpts = expList.getItemCount();
		progress.setLength(nbexpts);

		try 
		{ 
			int column = 1;
			int iSeries = 0;
			workbook = xlsInitWorkbook();
			for (int index = options.firstExp; index <= options.lastExp; index++) 
			{
				Experiment exp = expList.getItemAt(index);
				if (exp.chainToPreviousExperiment != null)
					continue;
				progress.setMessage("Export experiment "+ (index+1) +" of "+ nbexpts);
				String charSeries = CellReference.convertNumToColString(iSeries);
				
				if (options.topLevel) 
				{	
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPRAW);
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL);
				}
				
				if (options.lrPI && options.topLevel) 		
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVEL_LR);
				if (options.topLevelDelta) 	
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA);
				if (options.lrPI && options.topLevelDelta) 	
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.TOPLEVELDELTA_LR);
				

				if (options.bottomLevel) 	
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.BOTTOMLEVEL);		
				if (options.derivative) 	
					getCapillaryDataAndExport(exp, column, charSeries, EnumXLSExportType.DERIVEDVALUES);	
				
				if (!options.collateSeries || exp.chainToPreviousExperiment == null)
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
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}
	
	public void getCapillaryDescriptorsForOneExperiment( Experiment exp, EnumXLSExportType xlsOption) 
	{
		getExperimentDescriptors(exp, xlsOption);
		
		expAll.chainFirstImage_Ms = exp.chainFirstImage_Ms;
		Experiment expi = exp.chainToNextExperiment;
		expAll.chainFirstImage_Ms = exp.chainFirstImage_Ms;
		while (expi != null ) 
		{
			expAll.capillaries.mergeLists(expi.capillaries);
			expi = expi.chainToNextExperiment;
		}

		int nFrames = (int) ((expAll.camLastImage_Ms - expAll.camFirstImage_ms)/options.buildExcelStepMs  +1) ;
		int ncapillaries = expAll.capillaries.capillariesList.size();
		rowListForOneExp = new XLSResultsArray(ncapillaries);
		for (int i = 0; i < ncapillaries; i++) 
		{
			Capillary cap 		= expAll.capillaries.capillariesList.get(i);
			XLSResults row 		= new XLSResults (cap.getRoiName(), cap.capNFlies, xlsOption, nFrames);
			row.stimulus 		= cap.capStimulus;
			row.concentration 	= cap.capConcentration;
			row.cageID 			= cap.capCageID;
			rowListForOneExp.addRow(row);
		}
		rowListForOneExp.sortRowsByName();
	}
	
	public  XLSResultsArray getCapillaryDataFromOneExperimentSeries(
			Experiment exp, 
			EnumXLSExportType xlsExportType) 
	{	
		getCapillaryDescriptorsForOneExperiment (exp, xlsExportType);
		Experiment expi = exp.getFirstChainedExperiment(true); 
		
		while (expi != null) 
		{
			int nOutputFrames = getNOutputFrames(expi);
			if (nOutputFrames > 1)
			{
				XLSResultsArray resultsArrayList = new XLSResultsArray (expi.capillaries.capillariesList.size());
				Capillaries caps = expi.capillaries;
				options.compensateEvaporation = false;
				switch (xlsExportType) 
				{
					case BOTTOMLEVEL:
					case NBGULPS:
					case AMPLITUDEGULPS:
					case TTOGULP:
					case TTOGULP_LR:
						resultsArrayList.getResults1(caps, xlsExportType, 
								nOutputFrames, exp.kymoBinCol_Ms, options);
						break;
						
					case TOPRAW:
						resultsArrayList.getResults_T0(caps, xlsExportType, 
								nOutputFrames, exp.kymoBinCol_Ms, options);
						break;
						
					case TOPLEVEL:
					case TOPLEVEL_LR:
					case TOPLEVELDELTA:
					case TOPLEVELDELTA_LR:
						options.compensateEvaporation = options.subtractEvaporation;
						resultsArrayList.getResults_T0(caps, xlsExportType, 
								nOutputFrames, exp.kymoBinCol_Ms, options);
						break;
						
					case DERIVEDVALUES:
					case SUMGULPS:
					case SUMGULPS_LR:
						resultsArrayList.getResults1(caps, xlsExportType, 
								nOutputFrames, exp.kymoBinCol_Ms, options);
						break;
						
					case AUTOCORREL:
					case AUTOCORREL_LR:
					case CROSSCORREL:
					case CROSSCORREL_LR:
						resultsArrayList.getResults1(caps, xlsExportType, 
								nOutputFrames, exp.kymoBinCol_Ms, options);
						break;
						
					default:
						break;
				}
				addResultsTo_rowsForOneExp(expi, resultsArrayList);
			}
			expi = expi.chainToNextExperiment;
		}
		
		switch (xlsExportType) 
		{
			case TOPLEVELDELTA:
			case TOPLEVELDELTA_LR:
				rowListForOneExp.subtractDeltaT(1, 1); //options.buildExcelStepMs);
				break;
			default:
				break;
		}
		
		return rowListForOneExp;
	}
	
	public int getCapillaryDataAndExport(Experiment exp, int col0, String charSeries, EnumXLSExportType xlsExport) 
	{	
		getCapillaryDataFromOneExperimentSeries(exp, xlsExport);
		XSSFSheet sheet = xlsInitSheet(xlsExport.toString(), xlsExport);
		int colmax = xlsExportCapillaryResultsArrayToSheet(sheet, xlsExport, col0, charSeries);
		
		if (options.onlyalive) 
		{
			trimDeadsFromArrayList(exp);
			sheet = xlsInitSheet(xlsExport.toString()+"_alive", xlsExport);
			xlsExportCapillaryResultsArrayToSheet(sheet, xlsExport, col0, charSeries);
		}
		
		if (options.sumPerCage) 
		{
			combineDataForOneCage(exp);
			sheet = xlsInitSheet(xlsExport.toString()+"_cage", xlsExport);
			xlsExportCapillaryResultsArrayToSheet(sheet, xlsExport, col0, charSeries);
		}
		
		return colmax;
	}

	private int xlsExportCapillaryResultsArrayToSheet(
			XSSFSheet sheet, 
			EnumXLSExportType xlsExportOption, 
			int col0, 
			String charSeries) 
	{
		Point pt = new Point(col0, 0);
		writeExperiment_descriptors(expAll, true, charSeries, sheet, pt, xlsExportOption);
		pt = writeExperiment_data(sheet, xlsExportOption, pt);
		return pt.x;
	}
}
