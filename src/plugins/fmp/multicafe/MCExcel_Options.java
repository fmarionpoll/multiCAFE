package plugins.fmp.multicafe;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;




public class MCExcel_Options extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1814896922714679663L;
	
	JCheckBox 	exportAllFilesCheckBox 	= new JCheckBox("all experiments", true);
	JCheckBox	transposeCheckBox 		= new JCheckBox("transpose", true);
	
	JCheckBox   collateSeriesCheckBox	= new JCheckBox("collate series", false);
	JCheckBox   padIntervalsCheckBox	= new JCheckBox("pad intervals", false);
	
	JCheckBox	absoluteTimeCheckBox 	= new JCheckBox("absolute time", false);
	JSpinner 	pivotBinStep			= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	
	
	void init(GridLayout capLayout) {	
		setLayout(capLayout);
		
		FlowLayout flowLayout1 = new FlowLayout(FlowLayout.LEFT);
		flowLayout1.setVgap(0);
		JPanel panel0 = new JPanel(flowLayout1);
		panel0.add(exportAllFilesCheckBox);
		panel0.add(transposeCheckBox);
		panel0.add(collateSeriesCheckBox);
		panel0.add(padIntervalsCheckBox);
		add(panel0);

		JPanel panel1 = new JPanel(flowLayout1);
		panel1.add(absoluteTimeCheckBox);
		panel1.add(new JLabel("bin size(min) "));
		panel1.add(pivotBinStep);
		add(panel1); 
		
	   collateSeriesCheckBox.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent event) {
		        JCheckBox cb = (JCheckBox) event.getSource();
		        boolean isSelected = cb.isSelected();
		        padIntervalsCheckBox.setEnabled(isSelected);
		    }});
		
	}

}
