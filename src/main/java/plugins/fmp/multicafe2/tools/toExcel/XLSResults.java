package plugins.fmp.multicafe2.tools.toExcel;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import plugins.fmp.multicafe2.experiment.Capillary;


public class XLSResults 
{
	public String		name 		= null;
	String 				stimulus	= null;
	String 				concentration = null;
	int 				nadded		= 1;
	int 				cageID		= 0;
	boolean[]			padded_out	= null;
	
	public int 					dimension	= 0;
	public int					nflies		= 1;
	public EnumXLSExportType 	exportType 	= null;
	private ArrayList<Integer > dataInt 	= null;
	public double []			valuesOut	= null;
	
	
	
	public XLSResults (String name, int nflies, EnumXLSExportType exportType) 
	{
		this.name = name;
		this.nflies = nflies;
		this.exportType = exportType;
	}
	
	public XLSResults(String name, int nflies, EnumXLSExportType exportType, int nFrames) 
	{
		this.name = name;
		this.nflies = nflies;
		this.exportType = exportType;
		initValuesArray(nFrames);
	}
	
	void initValuesOutArray(int dimension, int val) 
	{
		this.dimension = dimension; 
		valuesOut = new double [dimension];
		Arrays.fill(valuesOut, Double.NaN);
	}
	
	private void initValuesArray(int dimension) 
	{
		this.dimension = dimension; 
		valuesOut = new double [dimension];
		Arrays.fill(valuesOut, Double.NaN);
		padded_out = new boolean [dimension];
		Arrays.fill(padded_out, false);
	}
	
	void clearValues (int fromindex) 
	{
		int toindex = valuesOut.length;
		if (fromindex > 0 && fromindex < toindex) 
		{
			Arrays.fill(valuesOut, fromindex,  toindex, Double.NaN);
			Arrays.fill(padded_out, fromindex,  toindex, false);
		}
	}
	
	void clearAll() 
	{
		dataInt = null;
		valuesOut = null;
		nflies = 0;
	}
	
	public void transferDataIntToValuesOut() 
	{
		if (dataInt.size() != valuesOut.length)
		{
			this.dimension = dataInt.size(); 
			valuesOut = new double [dimension];
		}
		for (int i = 0; i < dimension; i++)
			valuesOut[i] = dataInt.get(i);
	}
	
	public void copyValuesOut(XLSResults row) 
	{
		if (row.valuesOut.length != valuesOut.length)
		{
			this.dimension = row.dimension; 
			valuesOut = new double [dimension];
		}
		for (int i = 0; i < dimension; i++)
			valuesOut[i] = row.valuesOut[i];
	}
	
	public List<Integer> subtractT0 () 
	{
		if (dataInt == null || dataInt.size() < 1)
			return null;
		int item0 = dataInt.get(0);
		for (int index= 0; index < dataInt.size(); index++) 
		{
			int value = dataInt.get(index);
			dataInt.set(index, value-item0);
		}
		return dataInt;
	}
	
	boolean subtractDeltaT(int arrayStep, int binStep) {
		if (valuesOut == null || valuesOut.length < 2)
			return false;
		for (int index=0; index < valuesOut.length; index++) 
		{
			int timeIndex = index * arrayStep + binStep;
			int indexDelta = (int) (timeIndex/arrayStep);
			if (indexDelta < valuesOut.length) 
				valuesOut[index] = valuesOut[indexDelta] - valuesOut[index];
			else
				valuesOut[index] = Double.NaN;
		}
		return true;
	}
	
	void addDataToValOut(XLSResults result) 
	{
		if (result.valuesOut.length > valuesOut.length) 
		{
			System.out.println("Error: from len="+result.valuesOut.length + " to len="+ valuesOut.length);
			return;
		}
		for (int i=0; i < result.valuesOut.length; i++) 
			valuesOut[i] += result.valuesOut[i];	
		nflies ++;
	}
	
	void averageEvaporation() 
	{
		if (nflies != 0) 
		{
			for (int i=0; i < valuesOut.length; i++) 
				valuesOut[i] = valuesOut[i] / nflies;			
		}
		nflies = 1;
	}
	
	void subtractEvap(XLSResults evap) 
	{
		if (valuesOut == null)
			return;
		
		for (int i = 0; i < valuesOut.length; i++) 
		{
			if (i < evap.valuesOut.length)
				valuesOut[i] -= evap.valuesOut[i];			
		}
		evap.nflies = 1;
	}
	
	void addValues_out (XLSResults addedData) 
	{
		for (int i = 0; i < valuesOut.length; i++) 
		{
			if (addedData.valuesOut.length > i)
				valuesOut[i] += addedData.valuesOut[i];			
		}
		nadded += 1;
	}

	public void getCapillaryMeasuresForPass1(Capillary cap, EnumXLSExportType xlsOption, long kymoBinCol_Ms, int outputBinMs)
	{
		dataInt = cap.getCapillaryMeasuresForPass1(xlsOption, kymoBinCol_Ms, outputBinMs);
	}
	
	double getValueOut(int index) 
	{
		double data = Double.NaN;
		if (valuesOut != null && index < valuesOut.length) 
			data = valuesOut[index];
		return data;
	}
	
	int getDataInt(int index) 
	{
		int value = 0;
		if (dataInt != null && index < dataInt.size()) 
			value = dataInt.get(index);
		return value;
	}
	
	
}
