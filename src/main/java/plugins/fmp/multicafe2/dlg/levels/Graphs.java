package plugins.fmp.multicafe2.dlg.levels;

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
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillaries;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.chart.XYMultiChart;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSExportType;


public class Graphs extends JPanel implements SequenceListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;
	private XYMultiChart chartTopandbottom 		= null;
	private XYMultiChart chartDelta 			= null;
	private XYMultiChart chartDerivative 		= null;
	private XYMultiChart chartSumgulps 			= null;
	private MultiCAFE2 	parent0 				= null;
	
	private JCheckBox 	limitsCheckbox 			= new JCheckBox("top/bottom", true);
	private JCheckBox 	derivativeCheckbox 		= new JCheckBox("derivative", false);
	private JCheckBox 	consumptionCheckbox 	= new JCheckBox("consumption", false);
	private JCheckBox 	deltaCheckbox 			= new JCheckBox("delta (Vt - Vt-1)", false);
	private JCheckBox 	correctEvaporationCheckbox 	= new JCheckBox("correct evaporation", false);
	private JButton 	displayResultsButton 	= new JButton("Display results");
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{	
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
		add(panel);
		JPanel panel1 = new JPanel(layout);
		panel1.add(correctEvaporationCheckbox);
		add(panel1);
		
		add(GuiUtil.besidesPanel(displayResultsButton, new JLabel(" "))); 
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		displayResultsButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{
					exp.seqKymos.validateRois();
					exp.seqKymos.transferKymosRoisToCapillaries_Measures(exp.capillaries);
					xyDisplayGraphs(exp);
				}
			}});
	}
	
	public void xyDisplayGraphs(Experiment exp) 
	{
		Viewer v = exp.seqCamData.seq.getFirstViewer();
		if (v == null)
			return;
		
		final Rectangle rectv = v.getBounds();
		Point ptRelative = new Point(0, rectv.height); 
		int dx = 5;
		int dy = 5; 
		exp.seqKymos.seq.addListener(this);
		
		if (limitsCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExportType.TOPLEVEL)
				&& isThereAnyDataToDisplay(exp, EnumXLSExportType.BOTTOMLEVEL))  
		{
			chartTopandbottom = xyDisplayGraphsItem(exp, "top + bottom levels", 
					EnumXLSExportType.TOPLEVEL, 
					chartTopandbottom, rectv, ptRelative);
			ptRelative.translate(dx, dy);
		}
		else if (chartTopandbottom != null) 
			closeChart(chartTopandbottom);
		
		if (deltaCheckbox.isSelected() && isThereAnyDataToDisplay(exp, EnumXLSExportType.TOPLEVELDELTA))  
		{
			chartDelta = xyDisplayGraphsItem(exp, "top delta t -(t-1)", 
					EnumXLSExportType.TOPLEVELDELTA, 
					chartDelta, rectv, ptRelative);
			ptRelative.translate(dx, dy);
		}
		else if (chartDelta != null) 
			closeChart(chartDelta);
		
		if (derivativeCheckbox.isSelected()&& isThereAnyDataToDisplay(exp, EnumXLSExportType.DERIVEDVALUES))   
		{
			chartDerivative = xyDisplayGraphsItem(exp, "Derivative", 
					EnumXLSExportType.DERIVEDVALUES, 
					chartDerivative, rectv, ptRelative);
			ptRelative.translate(dx, dy); 
		}
		else if (chartDerivative != null) 
			closeChart(chartDerivative);
		
		if (consumptionCheckbox.isSelected()&& isThereAnyDataToDisplay(exp, EnumXLSExportType.SUMGULPS))  
		{
			chartSumgulps = xyDisplayGraphsItem(exp, "Cumulated gulps", 
					EnumXLSExportType.SUMGULPS, 
					chartSumgulps, rectv, ptRelative);
			ptRelative.translate(dx, dy); 
		}
		else if (chartSumgulps != null) 
			closeChart(chartSumgulps);
	}
	
	private XYMultiChart xyDisplayGraphsItem(Experiment exp, String title, EnumXLSExportType option, XYMultiChart iChart, Rectangle rectv, Point ptRelative ) 
	{	
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
	
	public void closeAllCharts() 
	{
		chartTopandbottom = closeChart (chartTopandbottom); 
		chartDerivative = closeChart (chartDerivative); 
		chartSumgulps = closeChart (chartSumgulps); 
		chartDelta = closeChart (chartDelta);	
	}
	
	private XYMultiChart closeChart(XYMultiChart chart) 
	{
		if (chart != null) 
			chart.mainChartFrame.dispose();
		chart = null;
		return chart;
	}

	private boolean isThereAnyDataToDisplay(Experiment exp, EnumXLSExportType option) 
	{
		boolean flag = false;
		Capillaries capillaries = exp.capillaries;
		for (Capillary cap: capillaries.capillariesArrayList) 
		{
			flag = cap.isThereAnyMeasuresDone(option);
			if (flag)
				break;
		}
		return flag;
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) 
	{
	}

	@Override
	public void sequenceClosed(Sequence sequence) 
	{
		sequence.removeListener(this);
		closeAllCharts();
	}
}

