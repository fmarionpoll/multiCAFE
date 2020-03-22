package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.CapillaryLimits;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;




public class MCLevels_Edit  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2580935598417087197L;
	private MultiCAFE 			parent0;
	private boolean[] 			isInside		= null;
	private ArrayList<ROI> 		listGulpsSelected = null;
	private JComboBox<String> 	roiTypeCombo 	= new JComboBox<String> (new String[] 
			{" top level", "bottom level", "top & bottom levels", "derivative", "gulps" });
	private JButton 			deleteButton 	= new JButton("Delete");
	private JButton 			adjustButton 	= new JButton("Adjust dimensions");

	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(adjustButton, new JLabel(" "), new JLabel("Source ", SwingConstants.RIGHT), roiTypeCombo));
		add(GuiUtil.besidesPanel(new JLabel(" "), new JLabel(" "), new JLabel(" "),  deleteButton));
		defineListeners();
	}
	
	private void defineListeners() {
		deleteButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp =  parent0.paneSequence.getSelectedExperimentFromCombo();
				deletePointsIncluded(exp);
			}});
		adjustButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp =  parent0.paneSequence.getSelectedExperimentFromCombo();
				adjustDimensions(exp);
			}});
	}

	void selectGulpsWithinRoi(ROI2D roiReference, Sequence seq, int t) {
		List <ROI> allRois = seq.getROIs();
		listGulpsSelected = new ArrayList<ROI>();
		for (ROI roi: allRois) {
			roi.setSelected(false);
			if (roi instanceof ROI2D) {
				if (((ROI2D) roi).getT() != t)
					continue;
				if (roi.getName().contains("gulp")) {
					listGulpsSelected.add(roi);
					roi.setSelected(true);
				}
			}
		}
	}
	
	void deleteGulps(Sequence seq) {
		if (seq == null || listGulpsSelected == null)
			return;
		for (ROI roi: listGulpsSelected) {
			seq.removeROI(roi);
		}
		listGulpsSelected = null;
	}
	
	void deletePointsIncluded(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		int t= seqKymos.currentFrame;
		ROI2D roi = seqKymos.seq.getSelectedROI2D();
		if (roi == null)
			return;
		
		seqKymos.transferKymosRoisToCapillaries(exp.capillaries);
		Capillary cap = exp.capillaries.capillariesArrayList.get(t);
		String optionSelected = (String) roiTypeCombo.getSelectedItem();
		if (optionSelected .contains("gulp")) {
			selectGulpsWithinRoi(roi, seqKymos.seq, seqKymos.currentFrame);
			deleteGulps(seqKymos.seq);
			seqKymos.removeROIsAtT(t);
			List<ROI> listOfRois = cap.transferMeasuresToROIs();
			seqKymos.seq.addROIs (listOfRois, false);
		} else {
			CapillaryLimits caplimits = null;
			if (optionSelected .contains("top")) 
				caplimits = cap.ptsTop;
			if (optionSelected.contains("bottom"))
				caplimits =cap.ptsBottom;
			if (optionSelected.contains("deriv"))
				caplimits =cap.ptsDerivative;
			removeMeasuresEnclosedInRoi(caplimits, roi);
			seqKymos.updateROIFromCapillaryMeasure(cap, caplimits);
		}
	}
	
	void removeMeasuresEnclosedInRoi(CapillaryLimits caplimits, ROI2D roi) {
		Polyline2D polyline = caplimits.polyline;
		int npointsOutside = polyline.npoints - getPointsWithinROI(polyline, roi);
		if (npointsOutside > 0) {
			double [] xpoints = new double [npointsOutside];
			double [] ypoints = new double [npointsOutside];
			int index = 0;
			for (int i=0; i < polyline.npoints; i++) {
				if (!isInside[i]) {
					xpoints[index] = polyline.xpoints[i];
					ypoints[index] = polyline.ypoints[i];
					index++;
				}
			}
			caplimits.polyline = new Polyline2D(xpoints, ypoints, npointsOutside);	
		} else {
			caplimits.polyline = null;
		}
	}
	
	int getPointsWithinROI(Polyline2D polyline, ROI2D roi) {
		isInside = new boolean [polyline.npoints];
		int npointsInside= 0;
		for (int i=0; i< polyline.npoints; i++) {
			isInside[i] = (roi.contains(polyline.xpoints[i], polyline.ypoints[i]));
			npointsInside += isInside[i]? 1: 0;
		}
		return npointsInside;
	}
	
	void adjustDimensions (Experiment exp) {
		if (!exp.checkStepConsistency()) {
			parent0.paneKymos.tabCreate.setBuildKymosParametersToDialog(exp);
		}
		int imageSize = exp.seqKymos.imageWidthMax;
		for (Capillary cap: exp.capillaries.capillariesArrayList) {
			cap.ptsTop.adjustToImageWidth(imageSize);
			cap.ptsBottom.adjustToImageWidth(imageSize);
			cap.ptsDerivative.adjustToImageWidth(imageSize);
			cap.gulpsRois = null;
			// TODO: deal with gulps.. (simply remove?)
		}
		exp.seqKymos.seq.removeAllROI();
		exp.seqKymos.transferCapillariesToKymosRois(exp.capillaries);
	}
	
}
