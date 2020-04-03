package plugins.fmp.multicafeTools;


import java.util.List;

public class XLSCapillaryResults {
	String			name = null;
	List<Integer > 	data = null;
	
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
	
	
}
