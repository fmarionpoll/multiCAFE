package plugins.fmp.multicafeDlg.levels;

import java.awt.FlowLayout;
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
import icy.gui.viewer.Viewer;
import plugins.fmp.multicafeDlg.MultiCAFE;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeTools.ToExcel.EnumXLSExportType;
import plugins.fmp.multicafeTools.ToExcel.XYMultiChart;


public class Graphs extends JPanel {
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
	private JButton 	displayResultsButton 	= new JButton("Display results");
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		setLayout(capLayout);
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		
		JPanel panel = new JPanel(layout);
		panel.add(limitsCheckbox);
		panel.add(derivativeCheckbox);
		panel.add(consumptionCheckbox);
		panel.add(deltaCheckbox);
		add( GuiUtil.besidesPanel(panel));
		add(GuiUtil.besidesPanel(displayResultsButton, new JLabel(" "))); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		displayResultsButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null) {
					exp.seqKymos.validateRois();
					exp.seqKymos.transferKymosRoisToCapillaries(exp.capillaries);
					xyDisplayGraphs(exp);
				}
			}});
	}
	
	public void xyDisplayGraphs(Experiment exp) {
		Viewer v = exp.seqCamData.seq.getFirstViewer();
		if (v == null)
			return;
		final Rectangle rectv = v.getBounds();
		Point ptRelative = new Point(0, rectv.height); 
		int dx = 5;
		int dy = 5; // 230
		if (limitsCheckbox.isSelected() 
				&& isThereAnyDataToDisplay(exp, EnumXLSExportType.TOPLEVEL)
				&& isThereAnyDataToDisplay(exp, EnumXLSExportType.BOTTOMLEVEL)) {
			topandbottomChart = xyDisplayGraphsItem(exp, "top + bottom levels", 
					EnumXLSExportType.TOPLEVEL, 
					topandbottomChart, rectv, ptRelative);
			ptRelative.translate(dx, dy);
		}
		if (deltaCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExportType.TOPLEVELDELTA)) {
			deltaChart = xyDisplayGraphsItem(exp, "top delta t -(t-1)", 
					EnumXLSExportType.TOPLEVELDELTA, 
					deltaChart, rectv, ptRelative);
			ptRelative.translate(dx, dy);
		}
		if (derivativeCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExportType.DERIVEDVALUES)) {
			derivativeChart = xyDisplayGraphsItem(exp, "Derivative", 
					EnumXLSExportType.DERIVEDVALUES, 
					derivativeChart, rectv, ptRelative);
			ptRelative.translate(dx, dy); 
		}
		if (consumptionCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExportType.SUMGULPS)) {
			sumgulpsChart = xyDisplayGraphsItem(exp, "Cumulated gulps", 
					EnumXLSExportType.SUMGULPS, 
					sumgulpsChart, rectv, ptRelative);
			ptRelative.translate(dx, dy); 
		}
	}
	


	private XYMultiChart xyDisplayGraphsItem(Experiment exp, String title, EnumXLSExportType option, XYMultiChart iChart, Rectangle rectv, Point ptRelative ) {	
		if (iChart != null) 
			iChart.mainChartFrame.dispose();
		iChart = new XYMultiChart();
		iChart.createPanel(title);
		if (ptRelative != null)
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
		iChart.displayData(exp, option);
		iChart.mainChartFrame.toFront();
		return iChart;
	}
	
	private boolean isThereAnyDataToDisplay(Experiment exp, EnumXLSExportType option) {
		boolean flag = false;
		Capillaries capillaries = exp.capillaries;
		for (Capillary cap: capillaries.capillariesArrayList) {
			flag = cap.isThereAnyMeasuresDone(option);
			if (flag)
				break;
		}
		return flag;
	}
	
	public void closeAll() {
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

