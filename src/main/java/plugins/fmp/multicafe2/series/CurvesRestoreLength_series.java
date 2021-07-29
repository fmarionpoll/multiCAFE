package plugins.fmp.multicafe2.series;

import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;

public class CurvesRestoreLength_series extends BuildSeries 
{
	void analyzeExperiment(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCCapillaries();
		if (exp.loadKymographs()) 
		{
			SequenceKymos seqKymos = exp.seqKymos;
			for (int t= 0; t< seqKymos.nTotalFrames; t++) 
			{
				Capillary cap = exp.capillaries.capillariesArrayList.get(t);
				cap.restoreClippedMeasures();
			}
			exp.capillaries.xmlSaveCapillaries_Measures(exp.getKymosBinFullDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
}
