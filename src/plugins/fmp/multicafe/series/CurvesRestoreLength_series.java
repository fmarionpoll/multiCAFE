package plugins.fmp.multicafe.series;

import plugins.fmp.multicafe.sequence.Capillary;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.SequenceKymos;

public class CurvesRestoreLength_series extends BuildSeries {

	void runMeasurement(Experiment exp) {
		exp.loadExperimentCapillariesData_ForSeries();
		if (exp.loadKymographs()) {
			SequenceKymos seqKymos = exp.seqKymos;
			for (int t= 0; t< seqKymos.nTotalFrames; t++) {
				Capillary cap = exp.capillaries.capillariesArrayList.get(t);
				cap.restoreCroppedMeasures();
				
				seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsTop);
				seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsBottom);
				seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsDerivative);
			}
			exp.saveExperimentMeasures(exp.getResultsDirectory());
		}
		exp.seqCamData.closeSequence();
		exp.seqKymos.closeSequence();
	}
}
