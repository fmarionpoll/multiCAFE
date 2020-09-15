package plugins.fmp.multicafeDlg.cages;

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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeDlg.MultiCAFE;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.XYTaSeries;
import plugins.fmp.multicafeTools.Chart.YPosMultiChart;
import plugins.fmp.multicafeTools.ToExcel.EnumXLSExportType;


public class Graphs extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;

	private YPosMultiChart ypositionsChart	= null;
	private YPosMultiChart distanceChart	= null;
	private YPosMultiChart aliveChart		= null;
	private YPosMultiChart sleepChart		= null;
	
	private MultiCAFE parent0 = null;
	
	public JCheckBox	moveCheckbox		= new JCheckBox("y position", true);	
	private JCheckBox	distanceCheckbox	= new JCheckBox("distance t/t+1", false);
	JCheckBox			aliveCheckbox		= new JCheckBox("fly alive", true);
	JCheckBox			sleepCheckbox		= new JCheckBox("sleep", true);
	JSpinner 			aliveThresholdSpinner = new JSpinner(new SpinnerNumberModel(50.0, 0., 100000., .1));
	public JButton 		displayResultsButton= new JButton("Display results");
	private JButton		viewAsRoisButton	= new JButton("view detected flies as ROIs");

	
	void init(GridLayout capLayout, MultiCAFE parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(2);
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(moveCheckbox);
		panel1.add(distanceCheckbox);
		panel1.add(aliveCheckbox);
		panel1.add(sleepCheckbox);
		add(panel1);
		
		JPanel panel2 = new JPanel (flowLayout);
		panel2.add(new JLabel("Alive threshold"));
		panel2.add(aliveThresholdSpinner);
		add(panel2);
		
		add(GuiUtil.besidesPanel(displayResultsButton, new JLabel(" "))); 
		
		JPanel panel4 = new JPanel(flowLayout);
		panel4.add(viewAsRoisButton);
		add(panel4);
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		displayResultsButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				xyDisplayGraphs();
				firePropertyChange("DISPLAY_RESULTS", false, true);
			}});
		
		viewAsRoisButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null)
					exp.displayDetectedFliesAsRois();
			}});
	}

	private void xyDisplayGraphs() {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp == null)
			return;
		final Rectangle rectv = exp.seqCamData.seq.getFirstViewer().getBounds();
		Point ptRelative = new Point(0,30);
		final int deltay = 230;
	
		if (moveCheckbox.isSelected() ) {
			ypositionsChart = displayYPos("flies Y positions", ypositionsChart, rectv, ptRelative,  exp, EnumXLSExportType.XYTOPCAGE);
			ptRelative.y += deltay;
		}
		if (distanceCheckbox.isSelected()) {
			distanceChart = displayYPos("distance between positions at t+1 and t", distanceChart, rectv, ptRelative, exp, EnumXLSExportType.DISTANCE);
			ptRelative.y += deltay;
		}
		if (aliveCheckbox.isSelected()) {
			double threshold = (double) aliveThresholdSpinner.getValue();		
			for (Cage cage: exp.cages.cageList) {
				XYTaSeries posSeries = cage.flyPositions;
				posSeries.moveThreshold = threshold;
				posSeries.computeIsAlive();
			}
			aliveChart = displayYPos("flies alive", aliveChart, rectv, ptRelative, exp, EnumXLSExportType.ISALIVE);	
			ptRelative.y += deltay;
		}
		if (sleepCheckbox.isSelected()) {	
			for (Cage cage: exp.cages.cageList) {
				XYTaSeries posSeries = cage.flyPositions;
				posSeries.computeSleep();
			}
			sleepChart = displayYPos("flies asleep", sleepChart, rectv, ptRelative, exp, EnumXLSExportType.SLEEP);	
			ptRelative.y += deltay;
		}
	}

	
	private YPosMultiChart displayYPos(String title, YPosMultiChart iChart, Rectangle rectv, Point ptRelative, Experiment exp, EnumXLSExportType option) {
		if (iChart == null || !iChart.mainChartPanel.isValid()) {
			iChart = new YPosMultiChart();
			iChart.createPanel(title);
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
		}
		iChart.displayData(exp.cages.cageList, option);
		iChart.mainChartFrame.toFront();
		return iChart;
	}

	
	public void closeAll() {
		if (ypositionsChart != null) {
			ypositionsChart.mainChartFrame.close();
			ypositionsChart = null;
		}
		
		if (distanceChart != null) {
			distanceChart.mainChartFrame.close();
			distanceChart = null;
		}

		if (aliveChart != null) {
			aliveChart.mainChartFrame.close();
			aliveChart = null;
		}
		
		if (sleepChart != null) {
			sleepChart.mainChartFrame.close();
			sleepChart = null;
		}
	}
}
