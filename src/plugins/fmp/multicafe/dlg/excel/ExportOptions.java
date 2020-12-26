package plugins.fmp.multicafe.dlg.excel;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;




public class ExportOptions extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1814896922714679663L;
	
	JCheckBox 	exportAllFilesCheckBox 	= new JCheckBox("all experiments", true);
	public JCheckBox   collateSeriesCheckBox	= new JCheckBox("collate series", false);
	JCheckBox   padIntervalsCheckBox	= new JCheckBox("pad intervals", false);
	JCheckBox	absoluteTimeCheckBox 	= new JCheckBox("absolute time", false);
	JSpinner 	pivotBinSize			= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	private 	JComboBox<String> binUnit 	= new JComboBox<String> (new String[] {"ms", "s", "min", "h", "day"});
	
	
	
	void init(GridLayout capLayout) {	
		setLayout(capLayout);
		
		FlowLayout flowLayout1 = new FlowLayout(FlowLayout.LEFT);
		flowLayout1.setVgap(0);
		JPanel panel0 = new JPanel(flowLayout1);
		panel0.add(exportAllFilesCheckBox);
		panel0.add(collateSeriesCheckBox);
		panel0.add(padIntervalsCheckBox);
		add(panel0);

		JPanel panel1 = new JPanel(flowLayout1);
		panel1.add(absoluteTimeCheckBox);
		
		panel1.add(new JLabel("  bin size "));
		panel1.add(pivotBinSize);
		panel1.add(binUnit);
		binUnit.setSelectedIndex(2);
		add(panel1); 
		
	   collateSeriesCheckBox.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent event) {
		        JCheckBox cb = (JCheckBox) event.getSource();
		        boolean isSelected = cb.isSelected();
		        padIntervalsCheckBox.setEnabled(isSelected);
		    }});
	}
	
	int getExcelBuildStep() {
		int buildStep = ((int) pivotBinSize.getValue()) * getBinSize();
		return buildStep;
	}
	
	int getBinSize() {
		int binsize = 1;
		int iselected = binUnit.getSelectedIndex();
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

}
