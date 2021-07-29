package plugins.fmp.multicafe2.series;

import plugins.fmp.multicafe2.experiment.Experiment;

public class CropMeasuresToDimensions_series  extends BuildSeries  {
	void analyzeExperiment(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCCapillaries();
		if (exp.loadKymographs()) 
		{
			exp.cropCapillaryMeasuresDimensions();
			exp.saveExperimentMeasures(exp.getKymosBinFullDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
}
