package plugins.fmp.multicafe2.dlg.experiment;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

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

//	SortedComboBoxModel 		model1 			= new SortedComboBoxModel();
//	SortedComboBoxModel 		model2 			= new SortedComboBoxModel();
//	SortedComboBoxModel 		model3 			= new SortedComboBoxModel();
//	SortedComboBoxModel 		model4 			= new SortedComboBoxModel();
//	
//	private JComboBox<String>	cmt1Combo		= new JComboBox<String>(model1);
//	private JComboBox<String>	comt2Combo		= new JComboBox<String>(model2);
//	private JComboBox<String> 	boxIDCombo		= new JComboBox<String>(model3);
//	private JComboBox<String> 	exptCombo 		= new JComboBox<String>(model4);
	
	private JComboBox<String>	cmt1Combo		= new JComboBox<String>();
	private JComboBox<String>	comt2Combo		= new JComboBox<String>();
	private JComboBox<String> 	boxIDCombo		= new JComboBox<String>();
	private JComboBox<String> 	exptCombo 		= new JComboBox<String>();
	
	private JLabel				experimentCheck	= new JLabel("Expt");
	private JLabel				boxIDCheck		= new JLabel("Box ID");
	private JLabel				comment1Check	= new JLabel("Stim");
	private JLabel				comment2Check	= new JLabel("Conc");
	private JButton				openButton		= new JButton("Load...");
	private JButton				saveButton		= new JButton("Save...");
	
	private MultiCAFE2 			parent0 		= null;
	boolean 					disableChangeFile = false;
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		this.parent0 = parent0;
		setLayout(capLayout);

		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap (1);
		JPanel panel0 = new JPanel (flowlayout);
		panel0.add(experimentCheck);
		experimentCheck.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel0.add(exptCombo);
		panel0.add(boxIDCheck);
		boxIDCheck.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel0.add(boxIDCombo);
		panel0.add(openButton);
		add(panel0);
		
		JPanel panel1 = new JPanel(flowlayout);
		panel1.add(comment1Check);
		comment1Check.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel1.add(cmt1Combo);
		panel1.add(comment2Check);
		comment2Check.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel1.add(comt2Combo);
		panel1.add(saveButton);
		add (panel1);

		boxIDCombo.setEditable(true);
		exptCombo.setEditable(true);	
		cmt1Combo.setEditable(true);
		comt2Combo.setEditable(true);
		
		defineActionListeners();
		defineItemListeners();
	}
		
	private void defineItemListeners() 
	{
//		boxIDCombo.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                if(e.getStateChange() == ItemEvent.SELECTED) {
//                	Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
//    				if (exp != null) 
//    				{
//    					exp.setField(EnumXLSColumnHeader.BOXID, (String) boxIDCombo.getSelectedItem());
////    					System.out.println("display combo item: " + (String) boxIDCombo.getSelectedItem());
//    					boxIDCombo.setSelectedItem(boxIDCombo.getSelectedItem());
//    				}
//                }
//            }
//        });
//		
//		exptCombo.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                if(e.getStateChange() == ItemEvent.SELECTED) {
//                	Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
//    				if (exp != null)
//    					exp.setField(EnumXLSColumnHeader.EXPT, (String) exptCombo.getSelectedItem());
//                }
//            }
//        });
//		
//		cmt1Combo.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                if(e.getStateChange() == ItemEvent.SELECTED) {
//                	Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
//    				if (exp != null)
//    					exp.setField(EnumXLSColumnHeader.COMMENT1, (String) cmt1Combo.getSelectedItem());
//                }
//            }
//        });
//		
//		comt2Combo.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                if(e.getStateChange() == ItemEvent.SELECTED) {
//                	Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
//    				if (exp != null)
//    					exp.setField(EnumXLSColumnHeader.COMMENT2, (String) comt2Combo.getSelectedItem());
//                }
//            }
//        });
		
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
	}
	
	private void setInfoCombo(Experiment exp, JComboBox<String> combo, EnumXLSColumnHeader field, String altText) 
	{
		String text = exp.getField(field);
		if (text .equals(".."))
			exp.setField(field, altText);
		addItemToComboIfNew(text, combo);
		combo.getModel().setSelectedItem( text );
//		combo.setSelectedItem(text);
	}

	public void getExperimentInfosFromDialog(Experiment exp) 
	{
		exp.setField(EnumXLSColumnHeader.BOXID, (String) boxIDCombo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.EXPT, (String) exptCombo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.COMMENT1, (String) cmt1Combo.getSelectedItem());
		exp.setField(EnumXLSColumnHeader.COMMENT2, (String) comt2Combo.getSelectedItem());
	}
	
	private void addItemToComboIfNew(String text, JComboBox<String> combo) 
	{
		if (text == null)
			return;
//		SortedComboBoxModel model = (SortedComboBoxModel) combo.getModel();
//		if (model.getIndexOf(text) == -1 )
//			model.addElement(text);
		
		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) combo.getModel();
		if (model.getIndexOf(text) == -1 ) 
		{
			List<String> content = new ArrayList<> (model.getSize()+1);
			for (int i=0; i< model.getSize(); i++)
				content.add(model.getElementAt(i));
			content.add(text);
			java.util.Collections.sort(content);
			model.removeAllElements();
			model.addAll(content);		  
		}
//		combo.setModel(model);
	}	
		
	void initInfosCombos()
	{
		parent0.expListCombo.getFieldValuesToCombo(exptCombo, EnumXLSColumnHeader.EXPT); 
		parent0.expListCombo.getFieldValuesToCombo(cmt1Combo, EnumXLSColumnHeader.COMMENT1);
		parent0.expListCombo.getFieldValuesToCombo(comt2Combo, EnumXLSColumnHeader.COMMENT2);
		parent0.expListCombo.getFieldValuesToCombo(boxIDCombo, EnumXLSColumnHeader.BOXID);
	}
	
	void clearCombos()
	{
		exptCombo.removeAllItems(); 
		cmt1Combo.removeAllItems();
		comt2Combo.removeAllItems();
		boxIDCombo.removeAllItems();
	}



}

