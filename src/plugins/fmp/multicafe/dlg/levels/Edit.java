package plugins.fmp.multicafe.dlg.levels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polyline2D;
import icy.util.StringUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Capillary;
import plugins.fmp.multicafe.sequence.CapillaryLimits;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.sequence.SequenceKymos;
import plugins.fmp.multicafe.series.BuildSeries_Options;
import plugins.fmp.multicafe.tools.AdjustMeasuresDimensions_series;



public class Edit  extends JPanel  implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2580935598417087197L;
	private MultiCAFE 			parent0;
	private boolean[] 			isInside		= null;
//	private ArrayList<ROI> 		listGulpsSelected = null;
	private JComboBox<String> 	roiTypeCombo 	= new JComboBox<String> (new String[] 
			{" top level", "bottom level", "top & bottom levels", "derivative", "gulps" });
	private JButton 			deleteButton 	= new JButton("Cut & interpolate");
	private JButton 			cropButton 		= new JButton("Crop from left");
	private JButton 			restoreButton 	= new JButton("Restore");
	private JCheckBox			allSeriesCheckBox = new JCheckBox("ALL series", false);
	private String 				adjustString 	= "Adjust dimensions";
	private JButton 			adjustButton 	= new JButton(adjustString);
	private AdjustMeasuresDimensions_series thread = null;
	
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BorderLayout());
		panel1.add(new JLabel("Apply to ", SwingConstants.LEFT), BorderLayout.WEST); 
		panel1.add(roiTypeCombo, BorderLayout.CENTER);
		
		add(GuiUtil.besidesPanel(new JLabel(" "), panel1));
		add(GuiUtil.besidesPanel(new JLabel(" "), deleteButton));
		
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BorderLayout());
		panel2.add(cropButton, BorderLayout.CENTER); 
		panel2.add(restoreButton, BorderLayout.EAST);
		
		JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		((FlowLayout)panel3.getLayout()).setVgap(0);
		panel3.add(adjustButton);
		panel3.add(allSeriesCheckBox);
		add(GuiUtil.besidesPanel(panel3, panel2));

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
				if (!allSeriesCheckBox.isSelected()) {
					Experiment exp =  parent0.paneSequence.getSelectedExperimentFromCombo();
					adjustCapillaryMeasuresDimensions(exp);
				}
				else {
					if (adjustButton.getText() .equals(adjustString))
						series_detectLimitsStart();
					else 
						series_detectLimitsStop();
				}
			}});
		
		cropButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp =  parent0.paneSequence.getSelectedExperimentFromCombo();
				cropPointsToLeftLimit(exp);
			}});
		
		restoreButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp =  parent0.paneSequence.getSelectedExperimentFromCombo();
				restoreCroppedPoints(exp);
			}});
		
		allSeriesCheckBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				Color color = Color.BLACK;
				if (allSeriesCheckBox.isSelected()) 
					color = Color.RED;
				allSeriesCheckBox.setForeground(color);
				adjustButton.setForeground(color);
		}});
	}

	void cropPointsToLeftLimit(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		int t = seqKymos.currentFrame;
		ROI2D roiRef = seqKymos.seq.getSelectedROI2D();
		if (roiRef == null)
			return;

		Capillary cap = exp.capillaries.capillariesArrayList.get(t);
		seqKymos.transferKymosRoisToCapillaries(exp.capillaries);		
		
		int lastX = findLastXLeftOfRoi(cap, roiRef);
		cap.cropMeasuresToNPoints(lastX+1);
		
		seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsTop);
		seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsBottom);
		seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsDerivative);
	}
	
	int findLastXLeftOfRoi(Capillary cap, ROI2D roiRef) {
		int lastX = -1;
		Rectangle2D rectRef = roiRef.getBounds2D();
		double xleft = rectRef.getX();
		
		Polyline2D polyline = cap.ptsTop.polylineLimit;
		for (int i=0; i < polyline.npoints; i++) {
			if (polyline.xpoints[i] < xleft)
				continue;
			lastX = i-1;
			break;
		}
		return lastX;
	}
	
	void restoreCroppedPoints(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		int t = seqKymos.currentFrame;
		Capillary cap = exp.capillaries.capillariesArrayList.get(t);
		cap.restoreCroppedMeasures();
		
		seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsTop);
		seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsBottom);
		seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsDerivative);
	}
		
	List <ROI> selectGulpsWithinRoi(ROI2D roiReference, Sequence seq, int t) {
		List <ROI> allRois = seq.getROIs();
		List<ROI> listGulpsSelected = new ArrayList<ROI>();
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
		return listGulpsSelected;
	}
	
	void deleteGulps(SequenceKymos seqKymos, List <ROI> listGulpsSelected) {
		Sequence seq = seqKymos.seq;
		if (seq == null || listGulpsSelected == null)
			return;
		for (ROI roi: listGulpsSelected) {
			seq.removeROI(roi);
		}
	}
	
	void deletePointsIncluded(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		int t = seqKymos.currentFrame;
		ROI2D roi = seqKymos.seq.getSelectedROI2D();
		if (roi == null)
			return;
		
		seqKymos.transferKymosRoisToCapillaries(exp.capillaries);
		Capillary cap = exp.capillaries.capillariesArrayList.get(t);
		String optionSelected = (String) roiTypeCombo.getSelectedItem();
		if (optionSelected .contains("gulp")) {
			List<ROI> listGulpsSelected = selectGulpsWithinRoi(roi, seqKymos.seq, seqKymos.currentFrame);
			deleteGulps(seqKymos, listGulpsSelected);
			seqKymos.removeROIsAtT(t);
			List<ROI2D> listOfRois = cap.transferMeasuresToROIs();
			seqKymos.seq.addROIs (listOfRois, false);
		} else {
			if (optionSelected .contains("top")) 
				removeAndUpdate(seqKymos, cap, cap.ptsTop, roi);
			if (optionSelected.contains("bottom"))
				removeAndUpdate(seqKymos, cap, cap.ptsBottom, roi);
			if (optionSelected.contains("deriv"))
				removeAndUpdate(seqKymos, cap, cap.ptsDerivative, roi);
		}
	}
	
	private void removeAndUpdate(SequenceKymos seqKymos, Capillary cap, CapillaryLimits caplimits, ROI2D roi) {
		removeMeasuresEnclosedInRoi(caplimits, roi);
		seqKymos.updateROIFromCapillaryMeasure(cap, caplimits);
	}
	
	void removeMeasuresEnclosedInRoi(CapillaryLimits caplimits, ROI2D roi) {
		Polyline2D polyline = caplimits.polylineLimit;
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
			caplimits.polylineLimit = new Polyline2D(xpoints, ypoints, npointsOutside);	
		} else {
			caplimits.polylineLimit = null;
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
	
	void adjustCapillaryMeasuresDimensions (Experiment exp) {
		if (!exp.checkStepConsistency()) {
			parent0.paneKymos.tabCreate.setBuildKymosParametersToDialog(exp);
		}
		exp.adjustCapillaryMeasuresDimensions();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			Experiment exp = parent0.expList.getExperiment(parent0.paneSequence.expListComboBox.getSelectedIndex());
			parent0.paneSequence.openExperiment(exp);
			adjustButton.setText(adjustString);
		 }	 
	}
	
	private void series_detectLimitsStop() {	
		if (thread != null && !thread.stopFlag) {
			thread.stopFlag = true;
		}
	}
	
	void series_detectLimitsStart() {
		int index  = parent0.paneSequence.expListComboBox.getSelectedIndex();
		Experiment exp = parent0.expList.getExperiment(index);
		if (exp == null)
			return;
		parent0.expList.currentExperimentIndex = index;
		parent0.paneSequence.tabClose.closeExp(exp);
		thread = new AdjustMeasuresDimensions_series();
		
		parent0.paneSequence.transferExperimentNamesToExpList(parent0.expList, true);
		parent0.paneSequence.tabIntervals.getAnalyzeFrameFromDialog(exp);
		BuildSeries_Options options= thread.options;
		options.expList = new ExperimentList(); 
		parent0.paneSequence.transferExperimentNamesToExpList(options.expList, true);		
		if (allSeriesCheckBox.isSelected()) {
			options.expList.index0 = 0;
			options.expList.index1 = options.expList.getSize()-1;
		} else {
			options.expList.index0 = parent0.expList.currentExperimentIndex;
			options.expList.index1 = parent0.expList.currentExperimentIndex;
		}
		options.parent0Rect = parent0.mainFrame.getBoundsInternal();
		
		thread.addPropertyChangeListener(this);
		thread.execute();
		adjustButton.setText("STOP");
	}
	
	
	
	
	
}
