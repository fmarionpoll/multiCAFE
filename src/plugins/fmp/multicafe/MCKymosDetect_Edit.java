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

import icy.gui.util.GuiUtil;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.CapillaryLimits;
import plugins.fmp.multicafeSequence.SequenceKymos;



public class MCKymosDetect_Edit  extends JPanel {
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

	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(new JLabel("Source:"), new JLabel(" "), new JLabel(" "), deleteButton));
		add(GuiUtil.besidesPanel(roiTypeCombo, new JLabel(" "), new JLabel(" "), new JLabel(" ")));
		add(GuiUtil.besidesPanel(new JLabel(" "), new JLabel(" "), new JLabel(" "), new JLabel(" ")));
		
		defineListeners();
	}
	
	private void defineListeners() {
		deleteButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				parent0.expList.getExperiment(parent0.currentIndex).seqKymos.transferKymosRoisToMeasures();
				deletePointsIncluded();
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
	
	void deletePointsIncluded() {
		SequenceKymos seqKymos = parent0.expList.getExperiment(parent0.currentIndex).seqKymos;
		int t= seqKymos.currentFrame;
		ROI2D roi = seqKymos.seq.getSelectedROI2D();
		if (roi == null)
			return;
		
		String optionSelected = (String) roiTypeCombo.getSelectedItem();
		if (optionSelected .contains("gulp")) {
			selectGulpsWithinRoi(roi, seqKymos.seq, seqKymos.currentFrame);
			deleteGulps(seqKymos.seq);
		} else if (optionSelected .contains("top") && optionSelected .contains("bottom")) {
			deletePointsOfPolyLine(roi, "top", t, seqKymos);
			deletePointsOfPolyLine(roi, "bottom", t, seqKymos);
		} else if (optionSelected .contains("top")) {
			deletePointsOfPolyLine(roi, "top", t, seqKymos);
		} else if (optionSelected.contains("bottom")) {
			deletePointsOfPolyLine(roi, "bottom", t, seqKymos);
		} else if (optionSelected.contains("deriv")) {
			deletePointsOfPolyLine(roi, "deriv", t, seqKymos);
		}
	}
	
	void deletePointsOfPolyLine(ROI2D roi, String optionSelected, int t, SequenceKymos seqKymos) {
		Capillary cap = seqKymos.capillaries.capillariesArrayList.get(t);
		CapillaryLimits caplimits = null;
		
		if (optionSelected .contains("top")) {
			caplimits = cap.ptsTop;
		} else if (optionSelected.contains("bottom")) {
			caplimits = cap.ptsBottom;
		} else if (optionSelected.contains("deriv")) {
			caplimits = cap.ptsDerivative;
		}
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
		
		polyline = caplimits.polyline;
		List<ROI> allRois = seqKymos.seq.getROIs();
		for (ROI roii: allRois) {
			if (roii.getName().contains("selected"))
				seqKymos.seq.removeROI(roii);
			if (!(roii instanceof ROI2D))
				continue;
			if (((ROI2D) roii).getT() != t)
				continue;
			if (roii.getName().contains(caplimits.name))
				seqKymos.seq.removeROI(roii);
		}
		ROI2D newRoi = caplimits.transferPolyline2DToROI();
		seqKymos.seq.addROI(newRoi);
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
	
}
