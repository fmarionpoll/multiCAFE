package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;

public class MCExcel_Options extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1814896922714679663L;
	
	JCheckBox 	exportAllFilesCheckBox 	= new JCheckBox("all experiments", true);
	JCheckBox	transposeCheckBox 		= new JCheckBox("transpose", true);
	JCheckBox 	aliveCheckBox 			= new JCheckBox("alive", true);
	
	JCheckBox   collateSeriesCheckBox	= new JCheckBox("collate series", true);
	JCheckBox   padIntervalsCheckBox	= new JCheckBox("pad intervals", true);
	
	JCheckBox	absoluteTimeCheckBox 	= new JCheckBox("absolute time", false);
	JCheckBox	pivotCheckBox 			= new JCheckBox("pivot", false);
	JSpinner 	pivotBinStep			= new JSpinner(new SpinnerNumberModel(60, 1, 10000, 10));
	JLabel		pivotStepText			= new JLabel("binning step: ", SwingConstants.RIGHT);
	
	
	void init(GridLayout capLayout) {	
		setLayout(capLayout);
		
		add(GuiUtil.besidesPanel( exportAllFilesCheckBox, collateSeriesCheckBox, absoluteTimeCheckBox, new JLabel(" ")));
		add(GuiUtil.besidesPanel(  transposeCheckBox, padIntervalsCheckBox, pivotCheckBox, new JLabel(" ") )); 
		add(GuiUtil.besidesPanel(  aliveCheckBox, 	pivotStepText,	pivotBinStep, new JLabel(" "), new JLabel(" "))); 

		pivotCheckBox.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent event) {
		        JCheckBox cb = (JCheckBox) event.getSource();
		        boolean isSelected = cb.isSelected();
		        if (isSelected)
		        	transposeCheckBox.setSelected(true);
		    }});
		
		   collateSeriesCheckBox.addActionListener(new ActionListener() {
			    @Override
			    public void actionPerformed(ActionEvent event) {
			        JCheckBox cb = (JCheckBox) event.getSource();
			        boolean isSelected = cb.isSelected();
			        padIntervalsCheckBox.setEnabled(isSelected);
			    }});
		
	}

}
