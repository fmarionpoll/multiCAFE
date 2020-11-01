package plugins.fmp.multicafe.series;


import plugins.fmp.multicafe.sequence.Experiment;



public class AdjustMeasuresDimensions_series  extends BuildSeries {

	void analyzeExperiment(Experiment exp) {
		exp.loadExperimentCapillariesData_ForSeries();
		if (exp.loadKymographs()) {
			exp.adjustCapillaryMeasuresDimensions();
			exp.saveExperimentMeasures(exp.getResultsDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}


}
