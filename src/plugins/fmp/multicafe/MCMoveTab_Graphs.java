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
import plugins.fmp.multicafeSequence.XYTaSeries;
import plugins.fmp.multicafeTools.EnumArrayListType;
import plugins.fmp.multicafeTools.YPosMultiChart;


public class MCMoveTab_Graphs extends JPanel {

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
	JButton 			displayResultsButton= new JButton("Display results");

	
	void init(GridLayout capLayout, MultiCAFE parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(moveCheckbox, distanceCheckbox, aliveCheckbox, new JLabel(" ")));
		add(GuiUtil.besidesPanel(new JLabel(" "), new JLabel("Alive threshold"), aliveThresholdSpinner, new JLabel(" ")));
		
		add(GuiUtil.besidesPanel(displayResultsButton, new JLabel(" "))); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		displayResultsButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				displayResultsButton.setEnabled(false);
				parent0.roisSaveEdits();
				xyDisplayGraphs();
				displayResultsButton.setEnabled(true);
				firePropertyChange("DISPLAY_RESULTS", false, true);
			}});
	}

	private void xyDisplayGraphs() {

		final Rectangle rectv = parent0.vSequence.seq.getFirstViewer().getBounds();
		Point ptRelative = new Point(0,30);
		final int deltay = 230;
	
		if (moveCheckbox.isSelected() ) {
			ypositionsChart = displayYPos("flies Y positions", ypositionsChart, rectv, ptRelative, 
					EnumArrayListType.xyPosition);
			ptRelative.y += deltay;
		}
		if (distanceCheckbox.isSelected()) {
			distanceChart = displayYPos("distance between positions at t+1 and t", distanceChart, rectv, ptRelative,
					EnumArrayListType.distance);
			ptRelative.y += deltay;
		}
		if (aliveCheckbox.isSelected()) {
			double threshold = (double) aliveThresholdSpinner.getValue();		
			for (XYTaSeries posSeries: parent0.vSequence.cages.flyPositionsList) {
				posSeries.threshold = threshold;
				posSeries.getDoubleArrayList(EnumArrayListType.isalive);
			}
			aliveChart = displayYPos("flies alive", aliveChart, rectv, ptRelative,
					EnumArrayListType.isalive);	
			ptRelative.y += deltay;
		}
	}

	
	private YPosMultiChart displayYPos(String title, YPosMultiChart iChart, Rectangle rectv, Point ptRelative, EnumArrayListType option) {
		if (iChart == null || !iChart.mainChartPanel.isValid()) {
			iChart = new YPosMultiChart();
			iChart.createPanel(title);
			iChart.setLocationRelativeToRectangle(rectv, ptRelative);
		}
		iChart.displayData(parent0.vSequence.cages.flyPositionsList, option);
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
