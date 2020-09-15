package plugins.fmp.multicafeDlg.excel;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class ExportGulps extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	JButton 	exportToXLSButton 	= new JButton("save XLS (v1)");
	JButton 	exportToXLSButton2 	= new JButton("save XLS");
	JCheckBox 	sumGulpsCheckBox 	= new JCheckBox("sum gulps", true);
	JCheckBox 	isGulpsCheckBox 	= new JCheckBox("gulps (0/1)", true);
	JCheckBox 	tToGulpCheckBox 	= new JCheckBox("t to gulp", true);
	JCheckBox 	tToGulpLRCheckBox 	= new JCheckBox("t to gulp L/R", true);
	JCheckBox 	sumCheckBox 		= new JCheckBox("L+R & ratio", true);
	JCheckBox	onlyaliveCheckBox   = new JCheckBox("dead=empty", false);	
	
	void init(GridLayout capLayout) {	
		setLayout(capLayout);
		
		FlowLayout flowLayout0 = new FlowLayout(FlowLayout.LEFT);
		flowLayout0.setVgap(0);
		JPanel panel0 = new JPanel(flowLayout0);
		panel0.add(sumGulpsCheckBox);
		panel0.add(isGulpsCheckBox);
		panel0.add(tToGulpCheckBox);
		panel0.add(tToGulpLRCheckBox);
		add(panel0);
		
		JPanel panel1 = new JPanel(flowLayout0);
		panel1.add(sumCheckBox);
		panel1.add(onlyaliveCheckBox);
		add(panel1);
		
		FlowLayout flowLayout2 = new FlowLayout(FlowLayout.RIGHT);
		flowLayout2.setVgap(0);
		JPanel panel2 = new JPanel(flowLayout2);
		panel2.add(exportToXLSButton2);
		add(panel2);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton2.addActionListener (new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				firePropertyChange("EXPORT_GULPSDATA", false, true);
			}});
	}

}
