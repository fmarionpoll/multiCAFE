package plugins.fmp.multicafe.series;


import plugins.fmp.multicafe.experiment.Experiment;



public class AdjustMeasuresDimensions_series  extends BuildSeries 
{
	void analyzeExperiment(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCcapillaries();
		if (exp.loadKymographs()) 
		{
			exp.adjustCapillaryMeasuresDimensions();
			exp.saveExperimentMeasures(exp.getKymosDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}


}
