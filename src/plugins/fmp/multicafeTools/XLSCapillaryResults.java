package plugins.fmp.multicafeTools;


import java.util.Arrays;
import java.util.List;

public class XLSCapillaryResults {
	String				name 		= null;
	EnumXLSExportType 	exportType 	= null;
	List<Integer > 		data 		= null;
	double [] 			values		= null;
	
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
		values = new double [dimension];
		Arrays.fill(values, Double.NaN);
	}
	
	
}
