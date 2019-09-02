package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;


public class MCKymosAnalyzeTab_Edit  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2580935598417087197L;
	private MultiCAFE 	parent0;
	private ROI2DPolyLine roiselected 	= null;
	private boolean[] 	isInside		= null;
	
	private JComboBox<String> roiTypeCombo = new JComboBox<String> (new String[] {" upper level", "lower level", "derivative", "gulps" });
	private JButton 	selectButton 	= new JButton("Select points");
	private JButton 	deleteButton 	= new JButton("Delete");
	private JButton		replaceButton	= new JButton("Replace");
	private JButton		moveButton		= new JButton("Move vertically");
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(new JLabel("Source:"), new JLabel(" "), selectButton, deleteButton));
		add(GuiUtil.besidesPanel(roiTypeCombo, new JLabel(" "), new JLabel(" "), moveButton));
		add(GuiUtil.besidesPanel(new JLabel(" "), new JLabel(" "), new JLabel(" "), replaceButton));

		moveButton.setEnabled(false);
		replaceButton.setEnabled(false);
		
		defineListeners();
	}
	
	private void defineListeners() {
		selectButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				selectPointsIncluded();
			}});
		deleteButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				deletePointsIncluded();
			}});
	}

	void selectPointsIncluded() {
		SequenceKymos seqKymos = parent0.expList.getExperiment(parent0.currentIndex).seqKymos;
		int t= seqKymos.currentFrame;
		Capillary cap = seqKymos.capillaries.capillariesArrayList.get(t);
		ROI2D roi = seqKymos.seq.getSelectedROI2D();
		if (roi == null)
			return;
		String name = roi.getName();
		if (name.contains("level") || name.contains("deriv") || name.contains("gulp"))
			return;
		roi.setColor(Color.BLUE);
		roi.setSelected(true);
		
		Polyline2D polyline = null;
		String sourceData = (String) roiTypeCombo.getSelectedItem();
		if (sourceData .contains("upper"))
			polyline = cap.ptsTop;
		else if (sourceData.contains("lower"))
			polyline = cap.ptsBottom;
		else if (sourceData.contains("deriv"))
			polyline = cap.ptsDerivative;
		
		int npointsInside = getPointsWithinROI(polyline, roi);
		if (npointsInside > 0) {
			double [] xpoints = new double [npointsInside];
			double [] ypoints = new double [npointsInside];
			int index = 0;
			for (int i=0; i < polyline.npoints; i++) {
				if (isInside[i]) {
					xpoints[index] = polyline.xpoints[i];
					ypoints[index] = polyline.ypoints[i];
					index++;
				}
			}
			roiselected = new ROI2DPolyLine(new Polyline2D(xpoints, ypoints, npointsInside));
			roiselected.setColor(Color.BLUE);
			roiselected.setName("selected");
			seqKymos.seq.addROI(roiselected);
			roiselected.setSelected(true);
		}
	}
	
	void deletePointsIncluded() {
		SequenceKymos seqKymos = parent0.expList.getExperiment(parent0.currentIndex).seqKymos;
		int t= seqKymos.currentFrame;
		Capillary cap = seqKymos.capillaries.capillariesArrayList.get(t);
		ROI2D roi = seqKymos.seq.getSelectedROI2D();
		if (roi == null)
			return;
		
		Polyline2D polyline = null;
		String sourceData = (String) roiTypeCombo.getSelectedItem();
		if (sourceData .contains("upper")) {
			polyline = cap.ptsTop;
		} else if (sourceData.contains("lower")) {
			polyline = cap.ptsBottom;
		} else if (sourceData.contains("deriv")) {
			polyline = cap.ptsDerivative;
		}
		
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
			polyline = new Polyline2D(xpoints, ypoints, npointsOutside);	
		} else {
			polyline = null;
		}
		String name = null;
		if (sourceData .contains("upper")) {
			cap.ptsTop = polyline;
			name = cap.ID_TOPLEVEL;
		} else if (sourceData.contains("lower")) {
			cap.ptsBottom = polyline;
			name = cap.ID_BOTTOMLEVEL;
		} else if (sourceData.contains("deriv")) {
			cap.ptsDerivative = polyline;
			name = cap.ID_DERIVATIVE;
		}
		
		seqKymos.seq.removeROI(roi);
		List<ROI> allRois = seqKymos.seq.getROIs();
		for (ROI roii: allRois) {
			if (roii.getName().contains("selected"))
				seqKymos.seq.removeROI(roii);
			if (!(roii instanceof ROI2D))
				continue;
			if (((ROI2D) roii).getT() != t)
				continue;
			if (roii.getName().contains(name))
				seqKymos.seq.removeROI(roii);
		}
		ROI2D newRoi = cap.transferPolyline2DToROI(name, polyline);
		seqKymos.seq.addROI(newRoi);
		seqKymos.validateRoisAtT(t);
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
