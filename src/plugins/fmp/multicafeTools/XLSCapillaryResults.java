package plugins.fmp.multicafeTools;


import java.util.Arrays;
import java.util.List;

public class XLSCapillaryResults {
	String				name 		= null;
	EnumXLSExportType 	exportType 	= null;
	List<Integer > 		data 		= null;
	double [] 			values_out	= null;
	boolean[]			padded_out	= null;
	
	public XLSCapillaryResults (String name, EnumXLSExportType exportType) {
		this.name = name;
		this.exportType = exportType;
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
	
	public void initValuesArray(int dimension) {
		values_out = new double [dimension];
		Arrays.fill(values_out, Double.NaN);
		padded_out = new boolean [dimension];
		Arrays.fill(padded_out, false);
	}
	
	public void clearValues (int fromindex) {
		int toindex = values_out.length;
		if (fromindex < toindex) {
			Arrays.fill(values_out, fromindex,  toindex, Double.NaN);
			Arrays.fill(padded_out, fromindex,  toindex, false);
		}
	}
	
	
}
