package plugins.fmp.multicafe.series;


import plugins.fmp.multicafe.sequence.Experiment;



public class AdjustMeasuresDimensions_series  extends BuildSeries {

	void runMeasurement(Experiment exp) {
		exp.loadExperimentCapillariesData_ForSeries();
		exp.displaySequenceData(options.parent0Rect, exp.seqCamData.seq);
		if (exp.loadKymographs()) {
			exp.adjustCapillaryMeasuresDimensions();
			exp.saveExperimentMeasures(exp.getResultsDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}


}
