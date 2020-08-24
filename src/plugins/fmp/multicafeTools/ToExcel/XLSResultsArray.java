package plugins.fmp.multicafeTools.ToExcel;

import java.util.ArrayList;
import java.util.List;

import plugins.fmp.multicafeSequence.Capillary;

public class XLSResultsArray {
	List <XLSResults> 	resultsArrayList 	= null;
	XLSResults 			evapL				= null;
	XLSResults 			evapR				= null;
	boolean				sameLR				= true;
	String				stim				= null;
	String				conc				= null;
	
	public XLSResultsArray (int size) {
		resultsArrayList = new ArrayList <XLSResults> (size);
	}
	
	void add(XLSResults results) {
		resultsArrayList.add(results);
	}
	
	void checkIfSameStimulusAndConcentration(Capillary cap) {
		if (!sameLR)
			return;
		if (stim == null)
			stim = cap.capStimulus;
		if (conc == null)
			conc = cap.capConcentration;
		sameLR &= stim .equals(cap.capStimulus);
		sameLR &= conc .equals(cap.capConcentration);
	}
	
	XLSResults get(int index) {
		if (index >= resultsArrayList.size())
			return null;
		return resultsArrayList.get(index);
	}
	
	void subtractEvaporation() {
		int dimension = 0;
		for (XLSResults result: resultsArrayList) {
			if (result.data == null)
				continue;
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
			if (result.data == null || result.nflies > 0)
				continue;
			String side = result.name.substring(result.name.length() -1);
			if (sameLR || side.equals("L"))
				evapL.addDataToValInt(result);
			else
				evapR.addDataToValInt(result);
		}
		evapL.averageEvaporation();
		evapR.averageEvaporation();
	}
	
	
	private void subtractEvaporationLocal() {
		for (XLSResults result: resultsArrayList) {
			String side = result.name.substring(result.name.length() -1);
			if (sameLR || side.equals("L"))
				result.subtractEvap(evapL);
			else
				result.subtractEvap(evapR);
		}
	}
	
	
}
