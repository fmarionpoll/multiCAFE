package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;


import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Experiment;


public class MCSequence_Infos  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<String>	commentJCombo		= new JComboBox<String>();
	private JComboBox<String> 	boxID_JCombo		= new JComboBox<String>();
	private JComboBox<String> 	experimentJCombo 	= new JComboBox<String>();

	boolean 					disableChangeFile 	= false;
	
	
	void init(GridLayout capLayout) {
		setLayout(capLayout);

		add( GuiUtil.besidesPanel(
				createComboPanel("Experiment ", experimentJCombo),  
				createComboPanel("  Box ID ",  boxID_JCombo)));
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel("Comment   ", SwingConstants.RIGHT), BorderLayout.WEST); 
		panel.add(commentJCombo, BorderLayout.CENTER);
		add( GuiUtil.besidesPanel(panel));

		boxID_JCombo.setEditable(true);
		experimentJCombo.setEditable(true);	
		commentJCombo.setEditable(true);

	}
	
	
	private JPanel createComboPanel(String text, JComboBox<String> combo) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel(text, SwingConstants.RIGHT), BorderLayout.WEST); 
		panel.add(combo, BorderLayout.CENTER);
		return panel;
	}
		
	// set/ get
	
	void setExperimentsInfosToDialog(Experiment exp) {
		if (exp.boxID .equals(".."))
			exp.boxID = exp.capillaries.desc.old_boxID;
		addItem(boxID_JCombo, exp.boxID);
		if (exp.experiment.equals(".."))
			exp.experiment = exp.capillaries.desc.old_experiment;
		addItem(experimentJCombo, exp.experiment);
		if (exp.comment .equals(".."))
			exp.comment = exp.capillaries.desc.old_comment;
		addItem(commentJCombo, exp.comment);
	}

	void getExperimentInfosFromDialog(Experiment exp) {
		exp.boxID = (String) boxID_JCombo.getSelectedItem();
		exp.experiment = (String) experimentJCombo.getSelectedItem();
		exp.comment = (String) commentJCombo.getSelectedItem();
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
		addItem(experimentJCombo, (String) experimentJCombo.getSelectedItem());
		addItem(commentJCombo, (String) commentJCombo.getSelectedItem());
	}

}

