package plugins.fmp.multicafeTools;


import java.util.Arrays;
import java.util.List;

public class XLSResults {
	String				name 		= null;
	int					nflies		= 1;
	EnumXLSExportType 	exportType 	= null;
	List<Integer > 		data 		= null;
	int					binsize		= 1;
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
	
	public double getAt(int indexData, double scale) {			
		double value = Double.NaN;
		if (indexData < data.size()) {
			value = data.get(indexData) * scale;
		}
		return value;
	}
	
	public double getLast(double scale) {			
		double value = Double.NaN;
		if (data.size()>0) {
			value = data.get(data.size()-1) * scale;
		}
		return value;
	}
	
	private void initValuesArray(int dimension) {
		values_out = new double [dimension];
		Arrays.fill(values_out, Double.NaN);
		padded_out = new boolean [dimension];
		Arrays.fill(padded_out, false);
	}
	
	public void clearValues (int fromindex) {
		int toindex = values_out.length;
		if (fromindex > 0 && fromindex < toindex) {
			Arrays.fill(values_out, fromindex,  toindex, Double.NaN);
			Arrays.fill(padded_out, fromindex,  toindex, false);
		}
	}
	
	
}
