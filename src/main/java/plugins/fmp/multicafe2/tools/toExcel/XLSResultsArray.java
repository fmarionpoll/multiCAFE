package plugins.fmp.multicafe2.tools.toExcel;

import java.util.ArrayList;
import java.util.Collections;

import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.tools.Comparators;


public class XLSResultsArray 
{
	ArrayList <XLSResults> 	resultsList = null;
	XLSResults 			evapL			= null;
	XLSResults 			evapR			= null;
	boolean				sameLR			= true;
	String				stim			= null;
	String				conc			= null;
	
	public XLSResultsArray (int size) 
	{
		resultsList = new ArrayList <XLSResults> (size);
	}
	
	public XLSResultsArray() 
	{
		resultsList = new ArrayList <XLSResults> ();
	}

	public int size() 
	{
		return resultsList.size();
	}
	
	public XLSResults getRow(int index) 
	{
		if (index >= resultsList.size())
			return null;
		return resultsList.get(index);
	}
	
	public XLSResults getNextRow(int irow) 
	{
		XLSResults rowL = resultsList.get(irow); 
		int cageL = getCageFromKymoFileName(rowL.name);
		XLSResults rowR = null;
		if (irow+1 < resultsList.size()) 
		{
			rowR = resultsList.get(irow+1);
			int cageR = getCageFromKymoFileName(rowR.name);
			if (cageR != cageL) 
				rowR = null;
		}
		return rowR;
	}
	
	protected int getCageFromKymoFileName(String name) 
	{
		if (!name .contains("line"))
			return -1;
		return Integer.valueOf(name.substring(4, 5));
	}
	
	public void addRow(XLSResults results) 
	{
		resultsList.add(results);
	}
	
	public void sortRowsByName() 
	{
		Collections.sort(resultsList, new Comparators.XLSResults_Name_Comparator());
	}
	
	public void checkIfSameStimulusAndConcentration(Capillary cap) 
	{
		if (!sameLR)
			return;
		if (stim == null)
			stim = cap.capStimulus;
		if (conc == null)
			conc = cap.capConcentration;
		sameLR &= stim .equals(cap.capStimulus);
		sameLR &= conc .equals(cap.capConcentration);
	}
	
	public void transferDataIntToValout() 
	{
		for (XLSResults result: resultsList) 
			result.transferDataIntToValuesOut(); 
	}
	
	void subtractEvaporation() 
	{
		int dimension = 0;
		for (XLSResults result: resultsList) 
		{
			if (result.valuesOut == null)
				continue;
			if (result.valuesOut.length  > dimension)
				dimension = result.valuesOut.length;
		}
		if (dimension== 0)
			return;
		evapL = new XLSResults("L", 0, null);
		evapL.initValuesOutArray(dimension, 0);
		evapR = new XLSResults("R", 0, null);
		evapR.initValuesOutArray(dimension, 0);
		computeEvaporationFromResultsWithZeroFlies();
		subtractEvaporationLocal();
	}
	
	private void computeEvaporationFromResultsWithZeroFlies() 
	{
		for (XLSResults result: resultsList) 
		{
			if (result.valuesOut == null || result.nflies > 0)
				continue;
			String side = result.name.substring(result.name.length() -1);
			if (sameLR || side.contains("L"))
				evapL.addDataToValOut(result);
			else
				evapR.addDataToValOut(result);
		}
		evapL.averageEvaporation();
		evapR.averageEvaporation();
	}
	
	private void subtractEvaporationLocal() 
	{
		for (XLSResults result: resultsList) 
		{
			String side = result.name.substring(result.name.length() -1);
			if (sameLR || side.contains("L"))
				result.subtractEvap(evapL);
			else
				result.subtractEvap(evapR);
		}
	}
	
	public void subtractDeltaT (int i, int j) 
	{
		for (XLSResults row: resultsList ) 
			row.subtractDeltaT(1, 1); //options.buildExcelStepMs);
	}
}
