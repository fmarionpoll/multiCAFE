package plugins.fmp.multicafe.series;

import java.util.ArrayList;

import plugins.fmp.multicafe.experiment.Capillary;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceKymos;



public class CurvesClipSameLengthWithinCage_series extends BuildSeries 
{
	void analyzeExperiment(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCcapillaries();
		if (exp.loadKymographs(false)) 
		{
			SequenceKymos seqKymos = exp.seqKymos;
			ArrayList<Integer> listCageID = new ArrayList<Integer> (seqKymos.nTotalFrames);
			for (int t= 0; t< seqKymos.nTotalFrames; t++) 
			{
				Capillary tcap = exp.capillaries.capillariesArrayList.get(t);
				int tcage = tcap.capCageID;
				if (findCageID(tcage, listCageID)) 
					continue;
				listCageID.add(tcage);
				int minLength = findMinLength(exp, t, tcage);
				for (int tt = t; tt< seqKymos.nTotalFrames; tt++) 
				{
					Capillary ttcap = exp.capillaries.capillariesArrayList.get(tt);
					int ttcage = ttcap.capCageID;
					if (ttcage == tcage && ttcap.ptsTop.polylineLimit.npoints > minLength)
						ttcap.cropMeasuresToNPoints(minLength);
				}
			}
			exp.capillaries.xmlSaveCapillaries_Measures(exp.getKymosBinFullDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
	
	boolean findCageID(int cageID, ArrayList<Integer> listCageID) 
	{
		boolean found = false;
		for (int iID: listCageID) 
		{
			if (iID == cageID) 
			{
				found = true;
				break;
			}
		}
		return found;
	}
	
	private int findMinLength (Experiment exp, int t, int tcage ) 
	{
		Capillary tcap = exp.capillaries.capillariesArrayList.get(t);
		int minLength = tcap.ptsTop.polylineLimit.npoints;
		for (int tt = t; tt< exp.capillaries.capillariesArrayList.size(); tt++) 
		{
			Capillary ttcap = exp.capillaries.capillariesArrayList.get(tt);
			int ttcage = ttcap.capCageID;
			if (ttcage == tcage) 
			{
				int dataLength = ttcap.ptsTop.polylineLimit.npoints;
				if (dataLength < minLength)
					minLength = dataLength;
			}
		}
		return minLength;
	}
}