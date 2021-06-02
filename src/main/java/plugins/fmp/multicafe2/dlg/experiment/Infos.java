package plugins.fmp.multicafe2.dlg.experiment;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.SortedComboBoxModel;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSColumnHeader;


public class Infos  extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<String>	cmt1Combo		= new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String>	comt2Combo		= new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> 	boxIDCombo		= new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> 	exptCombo 		= new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> 	strainCombo 	= new JComboBox<String>(new SortedComboBoxModel());
	private JComboBox<String> 	sexCombo 		= new JComboBox<String>(new SortedComboBoxModel());
	
	private JLabel			experimentCheck	= new JLabel(EnumXLSColumnHeader.EXPT.toString());
	private JLabel			boxIDCheck		= new JLabel(EnumXLSColumnHeader.BOXID.toString());
	private JLabel			comment1Check	= new JLabel(EnumXLSColumnHeader.COMMENT1.toString());
	private JLabel			comment2Check	= new JLabel(EnumXLSColumnHeader.COMMENT2.toString());
	private JLabel			strainCheck		= new JLabel(EnumXLSColumnHeader.STRAIN.toString());
	private JLabel			sexCheck		= new JLabel(EnumXLSColumnHeader.SEX.toString());
	
	
	private JButton				openButton		= new JButton("Load...");
	private JButton				saveButton		= new JButton("Save...");
	
	private MultiCAFE2 			parent0 		= null;
	boolean 					disableChangeFile = false;
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		this.parent0 = parent0;
		GridBagLayout layoutThis = new GridBagLayout();
		setLayout(layoutThis);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.BASELINE;
		c.ipadx = 0;
		c.ipady = 0;
		c.insets = new Insets(1, 2, 1, 2); 
		int delta1 = 1;
		int delta2 = 3;
		
		// line 0
		c.gridx = 0;
		c.gridy = 0;
		add(experimentCheck, c);
		c.gridx += delta1;
		add(exptCombo, c);
		c.gridx += delta2;
		add(boxIDCheck, c);
		c.gridx += delta1;
		add(boxIDCombo, c);
		c.gridx += delta2;
		add(openButton, c);
		// line 1
		c.gridy = 1;
		c.gridx = 0;
		add(comment1Check, c);
		c.gridx += delta1;
		add(cmt1Combo, c);
		c.gridx += delta2;
		add(comment2Check, c);
		c.gridx += delta1;
		add(comt2Combo, c);
		c.gridx += delta2;
		add(saveButton, c);
		// line 2
		c.gridy = 2;
		c.gridx = 0;
		add(strainCheck, c);
		c.gridx += delta1;
		add(strainCombo, c);
		c.gridx += delta2;
		add(sexCheck, c);
		c.gridx += delta1;
		add(sexCombo, c);

		boxIDCombo.setEditable(true);
		exptCombo.setEditable(true);	
		cmt1Combo.setEditable(true);
		comt2Combo.setEditable(true);
		strainCombo.setEditable(true);
		sexCombo.setEditable(true);
		
		defineActionListeners();
	}
		
	
	private void defineActionListeners() 
	{
		openButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{
					exp.xmlLoadMCExperiment ();
					setExperimentInfosToDialog(exp);
				}
			}});
		
		saveButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{
					getExperimentInfosFromDialog(exp);
					exp.xmlSaveMCExperiment();
				}
			}});
	}
		
	// set/ get
	
	public void setExperimentInfosToDialog(Experiment exp) 
	{
		setInfoCombo(exp, boxIDCombo, EnumXLSColumnHeader.BOXID, exp.capillaries.desc.old_boxID); 
		setInfoCombo(exp, exptCombo, EnumXLSColumnHeader.EXPT, exp.capillaries.desc.old_experiment) ;
		setInfoCombo(exp, cmt1Combo, EnumXLSColumnHeader.COMMENT1, exp.capillaries.desc.old_comment1) ;
		setInfoCombo(exp, comt2Combo, EnumXLSColumnHeader.COMMENT2, exp.capillaries.desc.old_comment2) ;
		setInfoCombo(exp, strainCombo, EnumXLSColumnHeader.STRAIN, exp.capillaries.desc.old_strain) ;
		setInfoCombo(exp, sexCombo, EnumXLSColumnHeader.SEX, exp.capillaries.desc.old_sex) ;
	}
	
	private void setInfoCombo(Experiment exp, JComboBox<String> combo, EnumXLSColumnHeader field, String altText) 
	{
		String text = exp.getField(field);
		if (text .equals(".."))
			exp.setField(field, altText);
		addItemToComboIfNew(text, combo);
		combo.setSelectedItem(text);
	}

	public void getExperimentInfosFromDialog(Experiment exp) 
	{
		exp.setField(EnumXLSColumnHeader.BOXID, (String) boxIDCombo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.EXPT, (String) exptCombo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.COMMENT1, (String) cmt1Combo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.COMMENT2, (String) comt2Combo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.STRAIN, (String) strainCombo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.SEX, (String) sexCombo.getSelectedItem());
	}
	
	private void addItemToComboIfNew(String toAdd, JComboBox<String> combo) 
	{
		if (toAdd == null)
			return;
		SortedComboBoxModel model = (SortedComboBoxModel) combo.getModel();
		if (model.getIndexOf(toAdd) == -1 )
			model.addElement(toAdd);
    }	
		
	void initInfosCombos()
	{
		parent0.expListCombo.getFieldValuesToCombo(exptCombo, EnumXLSColumnHeader.EXPT); 
		parent0.expListCombo.getFieldValuesToCombo(cmt1Combo, EnumXLSColumnHeader.COMMENT1);
		parent0.expListCombo.getFieldValuesToCombo(comt2Combo, EnumXLSColumnHeader.COMMENT2);
		parent0.expListCombo.getFieldValuesToCombo(boxIDCombo, EnumXLSColumnHeader.BOXID);
		parent0.expListCombo.getFieldValuesToCombo(strainCombo, EnumXLSColumnHeader.STRAIN);
		parent0.expListCombo.getFieldValuesToCombo(sexCombo, EnumXLSColumnHeader.SEX);
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			setExperimentInfosToDialog(exp);
	}
	
	void clearCombos()
	{
		exptCombo.removeAllItems(); 
		cmt1Combo.removeAllItems();
		comt2Combo.removeAllItems();
		boxIDCombo.removeAllItems();
		strainCombo.removeAllItems();
		sexCombo.removeAllItems();
	}



}

