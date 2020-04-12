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
	JCheckBox 	aliveCheckBox 			= new JCheckBox("alive", false);
	
	JCheckBox   collateSeriesCheckBox	= new JCheckBox("collate series", false);
	JCheckBox   padIntervalsCheckBox	= new JCheckBox("pad intervals", false);
	
	JCheckBox	absoluteTimeCheckBox 	= new JCheckBox("absolute time", false);
	JSpinner 	pivotBinStep			= new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
	JLabel		pivotStepText			= new JLabel("binning step: ", SwingConstants.RIGHT);
	
	
	void init(GridLayout capLayout) {	
		setLayout(capLayout);
		
		add(GuiUtil.besidesPanel( exportAllFilesCheckBox, collateSeriesCheckBox, absoluteTimeCheckBox, new JLabel(" ")));
		add(GuiUtil.besidesPanel(  transposeCheckBox, padIntervalsCheckBox, new JLabel(" "), new JLabel(" ") )); 
		add(GuiUtil.besidesPanel(  aliveCheckBox, 	pivotStepText,	pivotBinStep, new JLabel(" "), new JLabel(" "))); 
		
	   collateSeriesCheckBox.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent event) {
		        JCheckBox cb = (JCheckBox) event.getSource();
		        boolean isSelected = cb.isSelected();
		        padIntervalsCheckBox.setEnabled(isSelected);
		    }});
		
	}

}
