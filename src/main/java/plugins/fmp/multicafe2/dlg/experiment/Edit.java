package plugins.fmp.multicafe2.dlg.experiment;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.ExperimentCombo;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSColumnHeader;

public class Edit   extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<EnumXLSColumnHeader>	fieldNamesCombo = new JComboBox<EnumXLSColumnHeader>
			(new EnumXLSColumnHeader[] {
					EnumXLSColumnHeader.EXP_EXPT
					, EnumXLSColumnHeader.EXP_BOXID
					, EnumXLSColumnHeader.EXP_STIM
					, EnumXLSColumnHeader.EXP_CONC
					, EnumXLSColumnHeader.EXP_STRAIN
					, EnumXLSColumnHeader.EXP_SEX
					, EnumXLSColumnHeader.CAP_STIM
					, EnumXLSColumnHeader.CAP_CONC
					});
	
	private JComboBox<String>	fieldOldValuesCombo	= new JComboBox<String>();
	private JTextField			newValueTextField 	= new JTextField (10);
	private JButton				applyButton 		= new JButton("Apply");
	private MultiCAFE2 			parent0 			= null;
			boolean 			disableChangeFile 	= false;
			ExperimentCombo 	editExpList 		= new ExperimentCombo();
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		this.parent0 = parent0;
		setLayout(capLayout);
			
		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap(1);
		
		int bWidth = 100;
		int bHeight = 21;
		
		JPanel panel0 = new JPanel (flowlayout);
		panel0.add(new JLabel("Field name "));
		panel0.add(fieldNamesCombo);
		fieldNamesCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		add(panel0);
				
		JPanel panel1 = new JPanel(flowlayout);
		panel1.add(new JLabel("Field value "));
		panel1.add(fieldOldValuesCombo);
		fieldOldValuesCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(new JLabel(" replace with "));
		panel1.add(newValueTextField);
		panel1.add(applyButton);
		add (panel1);
	
		defineActionListeners();
	}
	
	public void initEditCombos() 
	{
		editExpList.setExperimentsFromList(parent0.expListCombo.getExperimentsAsList()); 
		editExpList.getFieldValuesToCombo(fieldOldValuesCombo, (EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem());
	}
	
	private void defineActionListeners() 
	{
		applyButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				applyChange();
				newValueTextField.setText("");
				initEditCombos();
			}});
		
		fieldNamesCombo.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				editExpList.getFieldValuesToCombo(fieldOldValuesCombo, (EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem());
			}});
	}
	
	void applyChange() 
	{
		int nExperiments = editExpList.getItemCount();
		EnumXLSColumnHeader fieldEnumCode = (EnumXLSColumnHeader) fieldNamesCombo.getSelectedItem();
		String oldValue = (String) fieldOldValuesCombo.getSelectedItem();
		String newValue = newValueTextField.getText();
		
		for (int i = 0; i < nExperiments; i++)
		{
			Experiment exp = editExpList.getItemAt(i);
			exp.replaceFieldValue(fieldEnumCode, oldValue, newValue);
			
			String pattern = exp.getExperimentField(fieldEnumCode);
 
			if (pattern .equals(oldValue)) 
			{
				 exp.setExperimentField(fieldEnumCode, newValueTextField.getText());
				 exp.xmlSaveMCExperiment();
			}
		}
	}
	


}


