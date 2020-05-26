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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.XYTaSeries;
import plugins.fmp.multicafeTools.EnumListType;
import plugins.fmp.multicafeTools.YPosMultiChart;


public class MCCages_Graphs extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7079184380174992501L;

	private YPosMultiChart ypositionsChart	= null;
	private YPosMultiChart distanceChart	= null;
	private YPosMultiChart aliveChart		= null;
	private MultiCAFE parent0 = null;
	
	JCheckBox			moveCheckbox		= new JCheckBox("y position", true);	
	private JCheckBox	distanceCheckbox	= new JCheckBox("distance t/t+1", true);
	JCheckBox			aliveCheckbox		= new JCheckBox("fly alive", true);
	JSpinner 			aliveThresholdSpinner = new JSpinner(new SpinnerNumberModel(50.0, 0., 100000., .1));
	JCheckBox			sleepCheckbox		= new JCheckBox("sleep", true);
	JButton 			displayResultsButton= new JButton("Display results");

	
	void init(GridLayout capLayout, MultiCAFE parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(moveCheckbox, distanceCheckbox, aliveCheckbox, sleepCheckbox));
		add(GuiUtil.besidesPanel(new JLabel(" "), new JLabel("Alive threshold"), aliveThresholdSpinner, new JLabel(" ")));
		
		add(GuiUtil.besidesPanel(displayResultsButton, new JLabel(" "))); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		displayResultsButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				xyDisplayGraphs();
				firePropertyChange("DISPLAY_RESULTS", false, true);
			}});
	}

	private void xyDisplayGraphs() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp == null)
			return;
		final Rectangle rectv = exp.seqCamData.seq.getFirstViewer().getBounds();
		Point ptRelative = new Point(0,30);
		final int deltay = 230;
	
		if (moveCheckbox.isSelected() ) {
			ypositionsChart = displayYPos("flies Y positions", ypositionsChart, rectv, ptRelative, 
					exp, EnumListType.xyPosition);
			ptRelative.y += deltay;
		}
		if (distanceCheckbox.isSelected()) {
			distanceChart = displayYPos("distance between positions at t+1 and t", distanceChart, rectv, ptRelative,
					exp, EnumListType.distance);
			ptRelative.y += deltay;
		}
		if (aliveCheckbox.isSelected()) {
			double threshold = (double) aliveThresholdSpinner.getValue();		
			for (Cage cage: exp.cages.cageList) {
				XYTaSeries posSeries = cage.flyPositions;
				posSeries.moveThreshold = threshold;
				posSeries.getDoubleArrayList(EnumListType.isalive);
			}
			aliveChart = displayYPos("flies alive", aliveChart, rectv, ptRelative, exp, EnumListType.isalive);	
			ptRelative.y += deltay;
		}
		if (sleepCheckbox.isSelected()) {
			//double threshold = (double) aliveThresholdSpinner.getValue();		
			for (Cage cage: exp.cages.cageList) {
				XYTaSeries posSeries = cage.flyPositions;
				//posSeries.moveThreshold = threshold;
				posSeries.getDoubleArrayList(EnumListType.sleep);
			}
			aliveChart = displayYPos("flies asleep", aliveChart, rectv, ptRelative, exp, EnumListType.sleep);	
			ptRelative.y += deltay;
		}
	}

	
	private YPosMultiChart displayYPos(String title, YPosMultiChart iChart, Rectangle rectv, Point ptRelative, Experiment exp, EnumListType option) {
		if (iChart == null || !iChart.mainChartPanel.isValid()) {
			iChart = new YPosMultiChart();
			iChart.createPanel(title);
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
		}
		iChart.displayData(exp.cages.cageList, option);
		iChart.mainChartFrame.toFront();
		return iChart;
	}

	
	void closeAll() {
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
	}
}
