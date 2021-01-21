package plugins.fmp.multicafe.series;


import plugins.fmp.multicafe.sequence.Experiment;



public class AdjustMeasuresDimensions_series  extends BuildSeries {

	void analyzeExperiment(Experiment exp) {
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCcapillaries();
		if (exp.loadKymographs()) {
			exp.adjustCapillaryMeasuresDimensions();
			exp.saveExperimentMeasures(exp.getExperimentDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}


}
