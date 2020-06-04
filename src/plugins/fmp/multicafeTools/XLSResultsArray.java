package plugins.fmp.multicafeTools;

import java.util.ArrayList;
import java.util.List;

public class XLSResultsArray {
	List <XLSResults> resultsArrayList = null;
	XLSResults evapL	= null;
	XLSResults evapR	= null;
	
	public XLSResultsArray (int size) {
		resultsArrayList = new ArrayList <XLSResults> (size);
	}
	
	public void add(XLSResults results) {
		resultsArrayList.add(results);
	}
	
	public XLSResults get(int index) {
		if (index >= resultsArrayList.size())
			return null;
		return resultsArrayList.get(index);
	}
	
	public void subtractEvaporation() {
		XLSResults results0 = resultsArrayList.get(0);
		if (results0.values_out == null)
			return;
		evapL = new XLSResults("L", 0, results0.exportType, results0.values_out.length, 1);
		evapR = new XLSResults("R", 0, results0.exportType, results0.values_out.length, 1);
		computeEvaporationFromResultsWithZeroFlies();
		subtractEvaporationLocal();
	}
	
	private void computeEvaporationFromResultsWithZeroFlies() {
		for (XLSResults result: resultsArrayList) {
			if (result.nflies > 0)
				continue;
			String side = result.name.substring(result.name.length() -1);
			if (side.equals("L"))
				addToEvap(result, evapL);
			else
				addToEvap(result, evapR);
		}
		averageEvaporation(evapL);
		averageEvaporation(evapR);
	}
	
	private void addToEvap(XLSResults fromResult, XLSResults evap) {
		if (fromResult.values_out.length != evap.values_out.length)
			System.out.println("Error: from len="+fromResult.values_out.length + " to len="+ evap.values_out.length);
		for (int i=0; i < fromResult.values_out.length; i++) {
			evap.values_out[i] += fromResult.values_out[i];			
		}
		evap.nflies ++;
	}
	
	private void averageEvaporation(XLSResults evap) {
		for (int i=0; i < evap.values_out.length; i++) {
			evap.values_out[i] = evap.values_out[i] / evap.nflies;			
		}
		evap.nflies = 1;
	}
	
	private void subtractEvaporationLocal() {
		for (XLSResults result: resultsArrayList) {
			String side = result.name.substring(result.name.length() -1);
			if (side.equals("L"))
				subtractEvap(result, evapL);
			else
				subtractEvap(result, evapR);
		}
	}
	
	private void subtractEvap(XLSResults result, XLSResults evap) {
		for (int i=0; i < evap.values_out.length; i++) {
			result.values_out[i] -= evap.values_out[i];			
		}
		evap.nflies = 1;
	}
}
