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
		int dimension = 0;
		for (XLSResults result: resultsArrayList) {
			if (result.data.size() > dimension)
				dimension = result.data.size();
		}
		if (dimension== 0)
			return;
		evapL = new XLSResults("L", 0, null);
		evapL.initValIntArray(dimension, 0);
		evapR = new XLSResults("R", 0, null);
		evapR.initValIntArray(dimension, 0);
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
	
	private void addToEvap(XLSResults result, XLSResults evap) {
		if (result.data.size() > evap.valint.length) {
			System.out.println("Error: from len="+result.data.size() + " to len="+ evap.valint.length);
			return;
		}
		for (int i=0; i < result.data.size(); i++) {
			evap.valint[i] += result.data.get(i);			
		}
		evap.nflies ++;
	}
	
	private void averageEvaporation(XLSResults evap) {
		if (evap.nflies != 0) {
			for (int i=0; i < evap.valint.length; i++) {
				evap.valint[i] = evap.valint[i] / evap.nflies;			
			}
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
		for (int i=0; i < result.data.size(); i++) {
			result.data.set(i, result.data.get(i) - evap.valint[i]);			
		}
		evap.nflies = 1;
	}
}
