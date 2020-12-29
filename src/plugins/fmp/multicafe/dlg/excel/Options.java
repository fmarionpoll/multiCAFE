package plugins.fmp.multicafe.dlg.excel;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;




public class Options extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1814896922714679663L;
	
	JCheckBox 	exportAllFilesCheckBox 	= new JCheckBox("all experiments", true);
	public JCheckBox   collateSeriesCheckBox	= new JCheckBox("collate series", false);
	JCheckBox   padIntervalsCheckBox	= new JCheckBox("pad intervals", false);
	JCheckBox	absoluteTimeCheckBox 	= new JCheckBox("absolute time", false);
	JCheckBox	transposeCheckBox 		= new JCheckBox("transpose", true);
	JSpinner 	pivotBinSize			= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	String [] scale = new String[] {"ms", "s", "min", "h", "day"};
	JComboBox<String> binUnit 			= new JComboBox<String> (scale);
	JComboBox<String> intervalsUnit 	= new JComboBox<String> (scale);
	JSpinner 	startJSpinner			= new JSpinner(new SpinnerNumberModel(0., 0., 10000., 1.)); 
	JSpinner 	endJSpinner				= new JSpinner(new SpinnerNumberModel(99999999., 1., 99999999., 1.));
	JRadioButton  	isFixedFrame		= new JRadioButton("from ", false);
	JRadioButton  	isFloatingFrame		= new JRadioButton("all", true);
	
	
	
	void init(GridLayout capLayout) {	
		setLayout(capLayout);
		
		FlowLayout flowLayout1 = new FlowLayout(FlowLayout.LEFT);
		flowLayout1.setVgap(0);
		
		JPanel panel0 = new JPanel(flowLayout1);
		panel0.add(exportAllFilesCheckBox);
		panel0.add(transposeCheckBox);
		panel0.add(collateSeriesCheckBox);
		panel0.add(padIntervalsCheckBox);
		panel0.add(absoluteTimeCheckBox);
		add(panel0);
		padIntervalsCheckBox.setEnabled(false);

		JPanel panel1 = new JPanel(flowLayout1);
		panel1.add(new JLabel("  bin size "));
		panel1.add(pivotBinSize);
		panel1.add(binUnit);
		binUnit.setSelectedIndex(2);
		add(panel1); 
		
		JPanel panel2 = new JPanel(flowLayout1);
		panel2.add(new JLabel("Analyze "));
		panel2.add(isFloatingFrame);
		panel2.add(isFixedFrame);
		panel2.add(startJSpinner);
		panel2.add(new JLabel(" to "));
		panel2.add(endJSpinner);
		panel2.add(intervalsUnit);
		add(panel2); 
		
		enableIntervalButtons(false);
		ButtonGroup group = new ButtonGroup();
		group.add(isFloatingFrame);
		group.add(isFixedFrame);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		collateSeriesCheckBox.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent event) {
		        JCheckBox cb = (JCheckBox) event.getSource();
		        boolean isSelected = cb.isSelected();
		        padIntervalsCheckBox.setEnabled(isSelected);
		    }});
		
		isFixedFrame.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			enableIntervalButtons(true);
		}});
	
		isFloatingFrame.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			enableIntervalButtons(false);
			}});
	}
	
	private void enableIntervalButtons(boolean isSelected) {
		startJSpinner.setEnabled(isSelected);
        endJSpinner.setEnabled(isSelected);
        intervalsUnit.setEnabled(isSelected);
	}
	
	int getExcelBuildStep() {
		int buildStep = ((int) pivotBinSize.getValue()) * getMsUnitValue(binUnit);
		return buildStep;
	}
	
	int getMsUnitValue(JComboBox<String> cb) {
		int binsize = 1;
		int iselected = cb.getSelectedIndex();
		switch (iselected) {
		case 1: binsize = 1000; break;
		case 2: binsize = 1000 * 60; break;
		case 3: binsize = 1000 * 60 * 60; break;
		case 4: binsize = 1000 * 60 * 60 * 24; break;
		case 0:
		default:
			break;
		}
		return binsize;
	}
	
	long getStartAllMs() {
		long startAll = ((int) startJSpinner.getValue()) * getMsUnitValue(intervalsUnit);
		return startAll;
	}
	long getEndAllMs() {
		long endAll = ((int) endJSpinner.getValue()) * getMsUnitValue(intervalsUnit);
		return endAll;
	}

}
