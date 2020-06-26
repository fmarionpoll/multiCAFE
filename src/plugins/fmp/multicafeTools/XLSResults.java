package plugins.fmp.multicafeTools;


import java.util.Arrays;
import java.util.List;

public class XLSResults {
	String				name 		= null;
	int					nflies		= 1;
	EnumXLSExportType 	exportType 	= null;
	List<Integer > 		data 		= null;
	int					binsize		= 1;
	int[]				valint		= null;
	double [] 			values_out	= null;
	boolean[]			padded_out	= null;
	
	public XLSResults (String name, int nflies, EnumXLSExportType exportType) {
		this.name = name;
		this.nflies = nflies;
		this.exportType = exportType;
	}
	
	public XLSResults(String name, int nflies, EnumXLSExportType exportType, int nFrames, int binsize) {
		this.name = name;
		this.nflies = nflies;
		this.exportType = exportType;
		this.binsize = binsize;
		initValuesArray(nFrames);
	}
	
	double getAt(int indexData, double scale) {			
		double value = Double.NaN;
		if (indexData < data.size()) {
			value = data.get(indexData) * scale;
		}
		return value;
	}
	
	double getLast(double scale) {			
		double value = Double.NaN;
		if (data.size()>0) {
			value = data.get(data.size()-1) * scale;
		}
		return value;
	}
	
	void initValIntArray(int dimension, int val) {
		valint = new int [dimension];
		Arrays.fill(valint, 0);
	}
	
	private void initValuesArray(int dimension) {
		values_out = new double [dimension];
		Arrays.fill(values_out, Double.NaN);
		padded_out = new boolean [dimension];
		Arrays.fill(padded_out, false);
	}
	
	void clearValues (int fromindex) {
		int toindex = values_out.length;
		if (fromindex > 0 && fromindex < toindex) {
			Arrays.fill(values_out, fromindex,  toindex, Double.NaN);
			Arrays.fill(padded_out, fromindex,  toindex, false);
		}
	}
	
	boolean subtractDeltaT(int arrayStep, int deltaT) {
		if (values_out == null || values_out.length < 2)
			return false;
		for (int index=0; index < values_out.length; index++) {
			int timeIndex = index * arrayStep + deltaT;
			int indexDelta = timeIndex/arrayStep;
			if (indexDelta < values_out.length) 
				values_out[index] = values_out[indexDelta] - values_out[index];
			else
				values_out[index] = Double.NaN;
		}
		return true;
	}
	
	void addDataToValInt(XLSResults result) {
		if (result.data.size() > valint.length) {
			System.out.println("Error: from len="+result.data.size() + " to len="+ valint.length);
			return;
		}
		for (int i=0; i < result.data.size(); i++) {
			valint[i] += result.data.get(i);			
		}
		nflies ++;
	}
	
	void averageEvaporation() {
		if (nflies != 0) {
			for (int i=0; i < valint.length; i++) {
				valint[i] = valint[i] / nflies;			
			}
		}
		nflies = 1;
	}
	
	void subtractEvap(XLSResults evap) {
		if (data == null)
			return;
		
		for (int i = 0; i < data.size(); i++) {
			if (evap.valint.length > i)
				data.set(i, data.get(i) - evap.valint[i]);			
		}
		evap.nflies = 1;
	}
}
