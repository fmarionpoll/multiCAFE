package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.sequence.Capillary;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.SequenceKymos;

public class CurvesRestoreLength_series extends BuildSeries {

	void analyzeExperiment(Experiment exp) {
		exp.xmlLoadMCExperiment();
		exp.xmlLoadMCcapillaries();
		if (exp.loadKymographs()) {
			SequenceKymos seqKymos = exp.seqKymos;
			for (int t= 0; t< seqKymos.nTotalFrames; t++) {
				Capillary cap = exp.capillaries.capillariesArrayList.get(t);
				cap.restoreCroppedMeasures();
			}
			exp.capillaries.xmlSaveCapillaries_Measures(exp.getKymosDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
}
