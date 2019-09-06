package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeTools.EnumListType;
import plugins.fmp.multicafeTools.XYMultiChart;

public class MCKymosAnalyze_Graphs extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;
	private XYMultiChart topandbottomChart 		= null;
	private XYMultiChart deltaChart 			= null;
	private XYMultiChart derivativeChart 		= null;
	private XYMultiChart sumgulpsChart 			= null;
	private MultiCAFE 	parent0 				= null;
	
	private JCheckBox 	limitsCheckbox 			= new JCheckBox("top/bottom", true);
	private JCheckBox 	derivativeCheckbox 		= new JCheckBox("derivative", false);
	private JCheckBox 	consumptionCheckbox 	= new JCheckBox("consumption", false);
	private JCheckBox 	deltaCheckbox 			= new JCheckBox("delta (Vt - Vt-1)", false);
	
	private JButton 	displayFeedingActivitiesButton 	= new JButton("Display results");
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(limitsCheckbox, derivativeCheckbox, consumptionCheckbox, deltaCheckbox));
		add(GuiUtil.besidesPanel(displayFeedingActivitiesButton, new JLabel(" "))); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		displayFeedingActivitiesButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
				exp.seqKymos.roisSaveEdits();
				xyDisplayGraphs();
			}});
	}
	
	void xyDisplayGraphs() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		final Rectangle rectv = exp.seqCamData.seq.getFirstViewer().getBounds();
		Point ptRelative = new Point(0,rectv.height);
		final int deltay = 230;

		SequenceKymos seqKymos = exp.seqKymos;
		if (limitsCheckbox.isSelected() && isThereAnyDataToDisplay(seqKymos, EnumListType.topAndBottom)) {
			topandbottomChart = xyDisplayGraphsItem("top + bottom levels", 
					EnumListType.topAndBottom, 
					topandbottomChart, rectv, ptRelative);
			ptRelative.y += deltay;
		}
		if (deltaCheckbox.isSelected()&& isThereAnyDataToDisplay(seqKymos, EnumListType.topLevelDelta)) {
			deltaChart = xyDisplayGraphsItem("top delta t -(t-1)", 
					EnumListType.topLevelDelta, 
					deltaChart, rectv, ptRelative);
			ptRelative.y += deltay;
		}
		if (derivativeCheckbox.isSelected()&& isThereAnyDataToDisplay(seqKymos, EnumListType.derivedValues)) {
			derivativeChart = xyDisplayGraphsItem("Derivative", 
					EnumListType.derivedValues, 
					derivativeChart, rectv, ptRelative);
			ptRelative.y += deltay; 
		}
		if (consumptionCheckbox.isSelected()&& isThereAnyDataToDisplay(seqKymos, EnumListType.cumSum)) {
			sumgulpsChart = xyDisplayGraphsItem("Cumulated gulps", 
					EnumListType.cumSum, 
					sumgulpsChart, rectv, ptRelative);
			ptRelative.y += deltay; 
		}
	}

	private XYMultiChart xyDisplayGraphsItem(String title, EnumListType option, XYMultiChart iChart, Rectangle rectv, Point ptRelative ) {	
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		if (iChart != null && iChart.mainChartPanel.isValid()) {
			iChart.fetchNewData(exp.seqKymos, option);
		}
		else {
			iChart = new XYMultiChart();
			iChart.createPanel(title);
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
			iChart.displayData(exp.seqKymos, option);
		}
		iChart.mainChartFrame.toFront();
		return iChart;
	}
	
	private boolean isThereAnyDataToDisplay(SequenceKymos sequence, EnumListType option) {
		boolean flag = false;
		Capillaries capillaries = sequence.capillaries;
		for (Capillary cap: capillaries.capillariesArrayList) {
			flag = cap.isThereAnyMeasuresDone(option);
			if (flag)
				break;
		}
		return flag;
	}
	
	void closeAll() {
		if (topandbottomChart != null) 
			topandbottomChart.mainChartFrame.dispose();
		if (derivativeChart != null) 
			derivativeChart.mainChartFrame.close();
		if (sumgulpsChart != null) 
			sumgulpsChart.mainChartFrame.close();
		if (deltaChart != null) 
			deltaChart.mainChartFrame.close();

		topandbottomChart  = null;
		derivativeChart = null;
		sumgulpsChart  = null;
		deltaChart = null;
	}
}

