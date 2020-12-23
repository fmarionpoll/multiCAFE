package plugins.fmp.multicafe.dlg.kymos;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;


import icy.gui.util.GuiUtil;




public class Infos extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1530811745749103710L;
	JSpinner 	pivotBinSize				= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	private 	JComboBox<String> binUnit 	= new JComboBox<String> (new String[] {"ms", "s", "min", "h", "day"});
	JButton		resetButton					= new JButton("Apply");
	
	void init(GridLayout capLayout) {
		setLayout(capLayout);

		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);
		
		JPanel panel1 = new JPanel(layout1);
		panel1.add(new JLabel("  bin size "));
		panel1.add(pivotBinSize);
		panel1.add(binUnit);
		binUnit.setSelectedIndex(2);
		add(panel1); 
		
		JPanel panel2 = new JPanel(layout1);
		panel2.add(resetButton);
		add(GuiUtil.besidesPanel(panel2));

	}
	
	public int getBuildStep() {
		int buildStep = ((int) pivotBinSize.getValue()) * getBinSize();
		return buildStep;
	}
	
	private int getBinSize() {
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
