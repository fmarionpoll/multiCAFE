package plugins.fmp.multicafe2.dlg.experiment;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSColumnHeader;


public class Filter  extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<String>	comment1Combo		= new JComboBox<String>();
	private JComboBox<String>	comment2Combo		= new JComboBox<String>();
	private JComboBox<String> 	boxIDCombo			= new JComboBox<String>();
	private JComboBox<String> 	experimentCombo 	= new JComboBox<String>();
	private JCheckBox			experimentCheck		= new JCheckBox("Expmt");
	private JCheckBox			boxIDCheck			= new JCheckBox("BoxID");
	private JCheckBox			comment1Check		= new JCheckBox("Cmt1 ");
	private JCheckBox			comment2Check		= new JCheckBox("Cmt2 ");
	private JButton				applyButton 		= new JButton("Apply");
	private JButton				clearButton			= new JButton("Clear");
	
	private MultiCAFE2 			parent0 			= null;
			boolean 			disableChangeFile 	= false;
			List<Experiment> 	expList 			= new ArrayList<Experiment>();
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		this.parent0 = parent0;
		setLayout(capLayout);
			
		FlowLayout flowlayout = new FlowLayout(FlowLayout.LEFT);
		flowlayout.setVgap(0);
		
		int bWidth = 100;
		int bHeight = 21;
		
		JPanel panel0 = new JPanel (flowlayout);
		panel0.add(experimentCheck);
		experimentCheck.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel0.add(experimentCombo);
		experimentCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		panel0.add(boxIDCheck);
		boxIDCheck.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel0.add(boxIDCombo);
		boxIDCombo.setPreferredSize(new Dimension(bWidth, bHeight));
		panel0.add(applyButton);
		add(panel0);
		
		JPanel panel1 = new JPanel(flowlayout);
		panel1.add(comment1Check);
		comment1Check.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel1.add(comment1Combo);
		comment1Combo.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(comment2Check);
		comment2Check.setHorizontalTextPosition(SwingConstants.RIGHT);
		panel1.add(comment2Combo);
		comment2Combo.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(clearButton);
		add (panel1);
		
//		boxIDCombo.setEditable(true);
//		experimentCombo.setEditable(true);	
//		comment1Combo.setEditable(true);
//		comment2Combo.setEditable(true);
		
		defineActionListeners();
	}
	
	public void initLists() 
	{
		expList = parent0.expListCombo.getAllExperiments();
		parent0.expListCombo.getHeaderToCombo(experimentCombo, EnumXLSColumnHeader.EXPT); 
		parent0.expListCombo.getHeaderToCombo(comment1Combo, EnumXLSColumnHeader.COMMENT1);
		parent0.expListCombo.getHeaderToCombo(comment2Combo, EnumXLSColumnHeader.COMMENT2);
		parent0.expListCombo.getHeaderToCombo(boxIDCombo, EnumXLSColumnHeader.BOXID);
	}
	
	
	private void defineActionListeners() 
	{
		applyButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				parent0.paneExperiment.panelLoadSave.filteredCheck.setEnabled(true);
				parent0.paneExperiment.panelLoadSave.filteredCheck.setSelected(true);
				filterExperimentList(true);
			}});
		
		clearButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				parent0.paneExperiment.panelLoadSave.filteredCheck.setSelected(false);
				filterExperimentList(false);
			}});
	}
	
	public void filterExperimentList(boolean yes)
	{
		if (yes)
		{
			initializeExperimentComboWithList(filterAllItems());
		}
		else
		{
			initializeExperimentComboWithList(expList);
		}
	}
	
	private List<Experiment> filterAllItems() 
	{
		List<Experiment> filteredList = new ArrayList<Experiment>();
		if (expList.size() == 0)
			expList = parent0.expListCombo.getAllExperiments();
		filteredList.addAll(expList);
		if (experimentCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.EXPT, (String) experimentCombo.getSelectedItem());
		if (boxIDCheck.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.BOXID, (String) boxIDCombo.getSelectedItem());
		if (comment1Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.COMMENT1, (String) comment1Combo.getSelectedItem());
		if (comment2Check.isSelected())
			filterItem(filteredList, EnumXLSColumnHeader.COMMENT2, (String) comment2Combo.getSelectedItem());
		return filteredList;
	}
	
	void initializeExperimentComboWithList (List<Experiment> listExp)
	{
		parent0.expListCombo.removeAllItems();
		for (Experiment exp: listExp)
		{
			parent0.expListCombo.addItem(exp);
		}
		if (parent0.expListCombo.getItemCount() > 0)
			parent0.expListCombo.setSelectedIndex(0);
	}
	
	void filterItem(List<Experiment> filteredList, EnumXLSColumnHeader header, String filter)
	{
		Iterator <Experiment> iterator = filteredList.iterator();
		while(iterator.hasNext()) 
		{
			Experiment exp = iterator.next();
			if (!exp.getField(header).equals(filter)) 
				iterator.remove();
		}
	}
	


}

