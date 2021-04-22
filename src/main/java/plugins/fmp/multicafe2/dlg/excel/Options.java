package plugins.fmp.multicafe2.dlg.excel;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import plugins.fmp.multicafe2.dlg.JComponents.JComboMs;




public class Options extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1814896922714679663L;
	
	JCheckBox 	exportAllFilesCheckBox 	= new JCheckBox("all experiments", true);
	public JCheckBox collateSeriesCheckBox = new JCheckBox("collate series", false);
	JCheckBox   padIntervalsCheckBox	= new JCheckBox("pad intervals", false);
	JCheckBox	absoluteTimeCheckBox 	= new JCheckBox("absolute time", false);
	JCheckBox	transposeCheckBox 		= new JCheckBox("transpose", true);
	JSpinner 	binSize					= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	JComboMs 	binUnit 				= new JComboMs();
	JComboMs 	intervalsUnit 			= new JComboMs();
	JSpinner 	startJSpinner			= new JSpinner(new SpinnerNumberModel(0., 0., 10000., 1.)); 
	JSpinner 	endJSpinner				= new JSpinner(new SpinnerNumberModel(99999999., 1., 99999999., 1.));
	JRadioButton  	isFixedFrame		= new JRadioButton("from ", false);
	JRadioButton  	isFloatingFrame		= new JRadioButton("all", true);
	
	
	
	void init(GridLayout capLayout) 
	{	
		setLayout(capLayout);
		
		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);
		
		JPanel panel0 = new JPanel(layout1);
		panel0.add(exportAllFilesCheckBox);
		panel0.add(transposeCheckBox);
		panel0.add(collateSeriesCheckBox);
		panel0.add(padIntervalsCheckBox);
		panel0.add(absoluteTimeCheckBox);
		add(panel0);
		padIntervalsCheckBox.setEnabled(false);

		JPanel panel1 = new JPanel(layout1);
		panel1.add(new JLabel("Analyze "));
		panel1.add(isFloatingFrame);
		panel1.add(isFixedFrame);
		panel1.add(startJSpinner);
		panel1.add(new JLabel(" to "));
		panel1.add(endJSpinner);
		panel1.add(intervalsUnit);
		add(panel1); 
		
		JPanel panel2 = new JPanel(layout1);
		panel2.add(new JLabel("bin size "));
		panel2.add(binSize);
		panel2.add(binUnit);
		binUnit.setSelectedIndex(2);
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
		
		isFixedFrame.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				enableIntervalButtons(true);
			}});
	
		isFloatingFrame.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				enableIntervalButtons(false);
			}});
	}
	
	private void enableIntervalButtons(boolean isSelected) 
	{
		startJSpinner.setEnabled(isSelected);
        endJSpinner.setEnabled(isSelected);
        intervalsUnit.setEnabled(isSelected);
	}
	
	int getExcelBuildStep() 
	{
		int buildStep = ((int) binSize.getValue()) * binUnit.getMsUnitValue();
		return buildStep;
	}
	
	long getStartAllMs() 
	{
		long startAll = (long) (((double) startJSpinner.getValue()) * intervalsUnit.getMsUnitValue());
		return startAll;
	}
	
	long getEndAllMs() 
	{
		long endAll = (long) (((double) endJSpinner.getValue()) * intervalsUnit.getMsUnitValue());
		return endAll;
	}

}
