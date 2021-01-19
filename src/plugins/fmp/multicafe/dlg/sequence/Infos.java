package plugins.fmp.multicafe.dlg.sequence;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Experiment;


public class Infos  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<String>	comment1_JCombo		= new JComboBox<String>();
	private JComboBox<String>	comment2_JCombo		= new JComboBox<String>();
	private JComboBox<String> 	boxID_JCombo		= new JComboBox<String>();
	private JComboBox<String> 	experiment_JCombo 	= new JComboBox<String>();
	private MultiCAFE 			parent0 			= null;
	boolean 					disableChangeFile 	= false;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		this.parent0 = parent0;
		setLayout(capLayout);

		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap(0);
		JPanel panel0 = new JPanel (flowlayout);
		panel0.add(new JLabel("Experiment "));
		panel0.add(experiment_JCombo);
		panel0.add(new JLabel("  Box ID "));
		panel0.add(boxID_JCombo);
		add(panel0);
		
		JPanel panel1 = new JPanel(flowlayout);
		panel1.add(new JLabel("Comment1   ")); 
		panel1.add(comment1_JCombo);
		add( panel1);
		
		JPanel panel2 = new JPanel(flowlayout);
		panel2.add(new JLabel("Comment2   ")); 
		panel2.add(comment2_JCombo);
		add( panel2);

		boxID_JCombo.setEditable(true);
		experiment_JCombo.setEditable(true);	
		
		comment1_JCombo.setEditable(true);
		comment2_JCombo.setEditable(true);
		
		defineActionListeners();
	}
		
	private void defineActionListeners() {
		boxID_JCombo.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			String newtext = (String) boxID_JCombo.getSelectedItem();
			int nexpts = parent0.expList.getExperimentListSize();
			if (nexpts > 0 && parent0.paneExcel.tabOptions.collateSeriesCheckBox.isSelected()) {
				String exptName = (String) experiment_JCombo.getSelectedItem();
				for (int i = 0; i < nexpts; i++) {
					Experiment exp = parent0.expList.getExperimentFromList(i);
					if (newtext.equals(exp.exp_boxID) && exptName != null && exptName .equals(exp.experiment) ) {
						addItem(experiment_JCombo, exp.experiment);
						addItem(comment1_JCombo, exp.comment1);
						addItem(comment2_JCombo, exp.comment2);
						break;
					}
				}
			}
		} } );
	}
		
	// set/ get
	
	public void setExperimentsInfosToDialog(Experiment exp) {
		if (exp.exp_boxID .equals(".."))
			exp.exp_boxID = exp.capillaries.desc.old_boxID;
		addItem(boxID_JCombo, exp.exp_boxID);
		
		if (exp.experiment.equals(".."))
			exp.experiment = exp.capillaries.desc.old_experiment;
		addItem(experiment_JCombo, exp.experiment);
		
		if (exp.comment1 .equals(".."))
			exp.comment1 = exp.capillaries.desc.old_comment1;
		addItem(comment1_JCombo, exp.comment1);
		
		if (exp.comment2 .equals(".."))
			exp.comment2 = exp.capillaries.desc.old_comment2;
		addItem(comment2_JCombo, exp.comment2);
	}

	public void getExperimentInfosFromDialog(Experiment exp) {
		exp.exp_boxID = (String) boxID_JCombo.getSelectedItem();
		exp.experiment = (String) experiment_JCombo.getSelectedItem();
		exp.comment1 = (String) comment1_JCombo.getSelectedItem();
		exp.comment2 = (String) comment2_JCombo.getSelectedItem();
	}
	
	private void addItem(JComboBox<String> combo, String text) {
		if (text == null)
			return;
		combo.setSelectedItem(text);
		if (combo.getSelectedIndex() < 0) {
			boolean found = false;
			for (int i=0; i < combo.getItemCount(); i++) {
				int comparison = text.compareTo(combo.getItemAt(i));
				if (comparison > 0)
					continue;
				if (comparison < 0) {
					found = true;
					combo.insertItemAt(text, i);
					break;
				}
			}
			if (!found)
				combo.addItem(text);
			combo.setSelectedItem(text);
		}
	}	
	
	void updateCombos () {
		addItem(boxID_JCombo, (String) boxID_JCombo.getSelectedItem());
		addItem(experiment_JCombo, (String) experiment_JCombo.getSelectedItem());
		addItem(comment1_JCombo, (String) comment1_JCombo.getSelectedItem());
		addItem(comment2_JCombo, (String) comment2_JCombo.getSelectedItem());
	}

}

