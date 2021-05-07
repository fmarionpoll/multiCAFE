package plugins.fmp.multicafe2.dlg.experiment;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSColumnHeader;


public class Infos  extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<String>	comment1_JCombo		= new JComboBox<String>();
	private JComboBox<String>	comment2_JCombo		= new JComboBox<String>();
	private JComboBox<String> 	boxID_JCombo		= new JComboBox<String>();
	private JComboBox<String> 	experiment_JCombo 	= new JComboBox<String>();
	private JLabel				experimentCheck		= new JLabel("Experiment");
	private JLabel				boxIDCheck			= new JLabel("Box ID");
	private JLabel				comment1Check		= new JLabel("Comment1");
	private JLabel				comment2Check		= new JLabel("Comt2");
	private JButton		openButton	= new JButton("Load...");
	private JButton		saveButton	= new JButton("Save...");
	
	private MultiCAFE2 			parent0 			= null;
	boolean 					disableChangeFile 	= false;
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		this.parent0 = parent0;
		setLayout(capLayout);

		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap (1);
		JPanel panel0 = new JPanel (flowlayout);
		panel0.add(experimentCheck);
		experimentCheck.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel0.add(experiment_JCombo);
		panel0.add(boxIDCheck);
		boxIDCheck.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel0.add(boxID_JCombo);
		panel0.add(openButton);
		add(panel0);
		
		JPanel panel1 = new JPanel(flowlayout);
		panel1.add(comment1Check);
		comment1Check.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel1.add(comment1_JCombo);
		panel1.add(comment2Check);
		comment2Check.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel1.add(comment2_JCombo);
		panel1.add(saveButton);
		add (panel1);

		boxID_JCombo.setEditable(true);
		experiment_JCombo.setEditable(true);	
		comment1_JCombo.setEditable(true);
		comment2_JCombo.setEditable(true);
		
		defineActionListeners();
	}
		
	private void defineActionListeners() 
	{
		boxID_JCombo.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				String newtext = (String) boxID_JCombo.getSelectedItem();
				int nexpts = parent0.expListCombo.getItemCount();
				if (nexpts > 0 && parent0.paneExcel.tabOptions.collateSeriesCheckBox.isSelected()) 
				{
					String exptName = (String) experiment_JCombo.getSelectedItem();
					for (int i = 0; i < nexpts; i++) 
					{
						Experiment exp = parent0.expListCombo.getItemAt(i);
						if (newtext.equals(exp.getField(EnumXLSColumnHeader.BOXID)) 
								&& exptName != null && exptName .equals(exp.getField(EnumXLSColumnHeader.EXPT)) ) 
						{
							addItemToComboIfNew(exp.getField(EnumXLSColumnHeader.EXPT), experiment_JCombo);
							addItemToComboIfNew(exp.getField(EnumXLSColumnHeader.COMMENT1), comment1_JCombo);
							addItemToComboIfNew(exp.getField(EnumXLSColumnHeader.COMMENT2), comment2_JCombo);
							break;
						}
					}
				}
			}});
		
		openButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					exp.xmlLoadMCExperiment ();
					setExperimentsInfosToDialog(exp);
				}
			}});
		
		saveButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null) {
					getExperimentInfosFromDialog(exp);
					exp.xmlSaveMCExperiment();
				}
			}});
			
	}
		
	// set/ get
	
	public void setExperimentsInfosToDialog(Experiment exp) 
	{
		if (exp.getField(EnumXLSColumnHeader.BOXID) .equals(".."))
			exp.setField(EnumXLSColumnHeader.BOXID, exp.capillaries.desc.old_boxID);
		addItemToComboIfNew(exp.getField(EnumXLSColumnHeader.BOXID), boxID_JCombo);
		
		if (exp.getField(EnumXLSColumnHeader.EXPT).equals(".."))
			exp.setField(EnumXLSColumnHeader.EXPT, exp.capillaries.desc.old_experiment);
		addItemToComboIfNew(exp.getField(EnumXLSColumnHeader.EXPT), experiment_JCombo);
		
		if (exp.getField(EnumXLSColumnHeader.COMMENT1) .equals(".."))
			exp.setField(EnumXLSColumnHeader.COMMENT1, exp.capillaries.desc.old_comment1);
		addItemToComboIfNew(exp.getField(EnumXLSColumnHeader.COMMENT1), comment1_JCombo);
		
		if (exp.getField(EnumXLSColumnHeader.COMMENT2) .equals(".."))
			exp.setField(EnumXLSColumnHeader.COMMENT2, exp.capillaries.desc.old_comment2);
		addItemToComboIfNew(exp.getField(EnumXLSColumnHeader.COMMENT2), comment2_JCombo);
	}

	public void getExperimentInfosFromDialog(Experiment exp) 
	{
		exp.setField(EnumXLSColumnHeader.BOXID, (String) boxID_JCombo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.EXPT, (String) experiment_JCombo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.COMMENT1, (String) comment1_JCombo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.COMMENT2, (String) comment2_JCombo.getSelectedItem());
	}
	
	private void addItemToComboIfNew(String text, JComboBox<String> combo) 
	{
		if (text == null)
			return;
		combo.setSelectedItem(text);
		if (combo.getSelectedIndex() < 0) 
		{
			boolean found = false;
			for (int i=0; i < combo.getItemCount(); i++) 
			{
				int comparison = text.compareTo(combo.getItemAt(i));
				if (comparison > 0)
					continue;
				if (comparison < 0) 
				{
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
	
	void updateCombos () 
	{
		addItemToComboIfNew((String) boxID_JCombo.getSelectedItem(), boxID_JCombo);
		addItemToComboIfNew((String) experiment_JCombo.getSelectedItem(), experiment_JCombo);
		addItemToComboIfNew((String) comment1_JCombo.getSelectedItem(), comment1_JCombo);
		addItemToComboIfNew((String) comment2_JCombo.getSelectedItem(), comment2_JCombo);
	}
	
	void initCombosWithExpList()
	{
		parent0.expListCombo.getHeaderToCombo(experiment_JCombo, EnumXLSColumnHeader.EXPT); 
		parent0.expListCombo.getHeaderToCombo(comment1_JCombo, EnumXLSColumnHeader.COMMENT1);
		parent0.expListCombo.getHeaderToCombo(comment2_JCombo, EnumXLSColumnHeader.COMMENT2);
		parent0.expListCombo.getHeaderToCombo(boxID_JCombo, EnumXLSColumnHeader.BOXID);
	}
	
	void clearCombos()
	{
		experiment_JCombo.removeAllItems(); 
		comment1_JCombo.removeAllItems();
		comment2_JCombo.removeAllItems();
		boxID_JCombo.removeAllItems();
	}

}

