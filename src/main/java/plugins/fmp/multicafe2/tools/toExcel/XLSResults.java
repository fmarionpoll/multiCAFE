package plugins.fmp.multicafe2.tools.toExcel;


import java.util.Arrays;
import java.util.List;


public class XLSResults 
{
	public String		name 		= null;
	String 				stimulus	= null;
	String 				concentration = null;
	int 				nadded		= 1;
	int					nflies		= 1;
	int 				cageID		= 0;
	int 				dimension	= 0;
	EnumXLSExportType 	exportType 	= null;
	List<Integer > 		data 		= null;

	int[]				valint		= null;
	public double [] 	values_out	= null;
	boolean[]			padded_out	= null;
	
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
	
	void initValIntArray(int dimension, int val) 
	{
		this.dimension = dimension; 
		valint = new int [dimension];
		Arrays.fill(valint, 0);
	}
	
	private void initValuesArray(int dimension) 
	{
		this.dimension = dimension; 
		values_out = new double [dimension];
		Arrays.fill(values_out, Double.NaN);
		padded_out = new boolean [dimension];
		Arrays.fill(padded_out, false);
	}
	
	void clearValues (int fromindex) 
	{
		int toindex = values_out.length;
		if (fromindex > 0 && fromindex < toindex) 
		{
			Arrays.fill(values_out, fromindex,  toindex, Double.NaN);
			Arrays.fill(padded_out, fromindex,  toindex, false);
		}
	}
	
	void clearAll() 
	{
		data = null;
		values_out = null;
		nflies = 0;
	}
	
	public List<Integer>  subtractT0 () 
	{
		if (data == null || data.size() < 1)
			return null;
		int item0 = data.get(0);
		for (int index= 0; index < data.size(); index++) 
		{
			int value = data.get(index);
			data.set(index, value-item0);
		}
		return data;
	}
	
	boolean subtractDeltaT(int arrayStep, int binStep) {
		if (values_out == null || values_out.length < 2)
			return false;
		for (int index=0; index < values_out.length; index++) 
		{
			int timeIndex = index * arrayStep + binStep;
			int indexDelta = (int) (timeIndex/arrayStep);
			if (indexDelta < values_out.length) 
				values_out[index] = values_out[indexDelta] - values_out[index];
			else
				values_out[index] = Double.NaN;
		}
		return true;
	}
	
	void addDataToValInt(XLSResults result) 
	{
		if (result.data.size() > valint.length) 
		{
			System.out.println("Error: from len="+result.data.size() + " to len="+ valint.length);
			return;
		}
		for (int i=0; i < result.data.size(); i++) 
			valint[i] += result.data.get(i);	
		nflies ++;
	}
	
	void averageEvaporation() 
	{
		if (nflies != 0) 
		{
			for (int i=0; i < valint.length; i++) 
				valint[i] = valint[i] / nflies;			
		}
		nflies = 1;
	}
	
	void subtractEvap(XLSResults evap) 
	{
		if (data == null)
			return;
		for (int i = 0; i < data.size(); i++) 
		{
			if (evap.valint.length > i)
				data.set(i, data.get(i) - evap.valint[i]);			
		}
		evap.nflies = 1;
	}
	
	void addValues_out (XLSResults addedData) 
	{
		for (int i = 0; i < values_out.length; i++) 
		{
			if (addedData.values_out.length > i)
				values_out[i] += addedData.values_out[i];			
		}
		nadded += 1;
	}

	void getSumLR(XLSResults rowL, XLSResults rowR) 
	{
		int lenL = rowL.values_out.length;
		int lenR = rowR.values_out.length;
		int len = Math.max(lenL,  lenR);
		for (int index = 0; index < len; index++) 
		{
			double dataL = Double.NaN;
			double dataR = Double.NaN;
			double sum = Double.NaN;
			if (rowL.values_out != null && index < lenL) 
				dataL = rowL.values_out[index];
			if (rowR.values_out != null && index < lenR) 
				dataR = rowR.values_out[index];
			
//			sum = Math.abs(dataL)+Math.abs(dataR);
			sum = dataL + dataR;
			values_out[index]= sum;
		}
	}
	
	double getData(XLSResults row, int index) 
	{
		double data = Double.NaN;
		if (row.values_out != null && index < row.values_out.length) 
			data = row.values_out[index];
		return data;
	}
	
	void getPI_LR(XLSResults rowL, XLSResults rowR) 
	{
		int lenL = rowL.values_out.length;
		int lenR = rowR.values_out.length;
		int len = Math.min(lenL,  lenR);
		for (int index = 0; index < len; index++) 
		{
			double dataL = getData(rowL, index);
			double dataR = getData(rowR, index);
			boolean ratioOK = true;
			if (Double.isNaN(dataR) || Double.isNaN(dataL)) 
				ratioOK = false;		
			double ratio = Double.NaN;
			if (ratioOK) 
			{
				double sum = Math.abs(dataL)+Math.abs(dataR);
				if (sum != 0 && !Double.isNaN(sum))
					ratio = (dataL-dataR)/sum;
			}
			values_out[index]= ratio;
		}
	}
	
	void getRatio_LR(XLSResults rowL, XLSResults rowR) 
	{
		int lenL = rowL.values_out.length;
		int lenR = rowR.values_out.length;
		int len = Math.min(lenL,  lenR);
		for (int index = 0; index < len; index++) 
		{
			double dataL = getData(rowL, index);
			double dataR = getData(rowR, index);
			boolean ratioOK = true;
			if (Double.isNaN(dataR) || Double.isNaN(dataL)) 
				ratioOK = false;		
			double ratio = Double.NaN;
			if (ratioOK && dataR != 0)
					ratio = (dataL/dataR);
			values_out[index]= ratio;
		}
	}
	
	void getMinTimeToGulpLR(XLSResults rowL, XLSResults rowR) 
	{
		int lenL = rowL.values_out.length;
		int lenR = rowR.values_out.length;
		int len = Math.max(lenL,  lenR);
		for (int index = 0; index < len; index++) 
		{
			double dataMax = Double.NaN;
			double dataL = getData(rowL, index);
			double dataR = getData(rowR, index);		
			if (dataL <= dataR)
				dataMax = dataL;
			else if (dataL > dataR)
				dataMax = dataR;
			values_out[index]= dataMax;
		}
	}
	
	void getMaxTimeToGulpLR(XLSResults rowL, XLSResults rowR) 
	{
		int lenL = rowL.values_out.length;
		int lenR = rowR.values_out.length;
		int len = Math.max(lenL,  lenR);
		for (int index = 0; index < len; index++) 
		{
			double dataMin = Double.NaN;
			double dataL = getData(rowL, index);
			double dataR = getData(rowR, index);			
			if (dataL >= dataR)
				dataMin = dataL;
			else if (dataL < dataR)
				dataMin = dataR;
			values_out[index]= dataMin;
		}
	}

}
