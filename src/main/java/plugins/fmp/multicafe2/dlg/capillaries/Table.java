package plugins.fmp.multicafe2.dlg.capillaries;



import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import icy.gui.frame.IcyFrame;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.CapillaryTableModel;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;

public class Table  extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID 	= -8611587540329642259L;
	IcyFrame 					dialogFrame 	= null;
    private JTable 				tableView 		= new JTable();
	private CapillaryTableModel viewModel 		= null;
	private JButton				copyButton 		= new JButton("Copy table");
	private JButton				pasteButton 	= new JButton("Paste");
	private JButton				duplicateLRButton = new JButton("Duplicate cell to L/R");
	private JButton				duplicateAllButton = new JButton("Duplicate cell to all");
	private JButton				getNfliesButton = new JButton("Get n flies from cage");
	private JButton				getCageNoButton	= new JButton("Set cage n#");
	private JButton				noFliesButton = new JButton("Cages0/0: no flies");
	private MultiCAFE2 			parent0 		= null; 
	private List <Capillary> 	capillariesArrayCopy = null;
	
	
	public void initialize (MultiCAFE2 parent0, List <Capillary> capCopy) 
	{
		this.parent0 = parent0;
		capillariesArrayCopy = capCopy;
		
		viewModel = new CapillaryTableModel(parent0.expListCombo);
	    tableView.setModel(viewModel);
	    tableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    tableView.setPreferredScrollableViewportSize(new Dimension(500, 400));
	    tableView.setFillsViewportHeight(true);
	    TableColumnModel columnModel = tableView.getColumnModel();
	    for (int i=0; i<3; i++)
	    	setFixedColumnProperties(columnModel.getColumn(i));
        JScrollPane scrollPane = new JScrollPane(tableView);
        
		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT); 
		JPanel panel1 = new JPanel (flowLayout);
		panel1.add(copyButton);
        panel1.add(pasteButton);
        panel1.add(duplicateLRButton);
        panel1.add(duplicateAllButton);
       topPanel.add(panel1);
        
        JPanel panel2 = new JPanel (flowLayout);
        panel2.add(getCageNoButton);
        panel2.add(getNfliesButton);
        panel2.add(noFliesButton);
        topPanel.add(panel2);
        
        JPanel tablePanel = new JPanel();
		tablePanel.add(scrollPane);
        
		dialogFrame = new IcyFrame ("Capillaries properties", true, true);	
		dialogFrame.add(topPanel, BorderLayout.NORTH);
		dialogFrame.add(tablePanel, BorderLayout.CENTER);
		
		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);
		defineActionListeners();
		pasteButton.setEnabled(capillariesArrayCopy.size() > 0);
	}
	
	private void defineActionListeners() 
	{
		copyButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp =(Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					capillariesArrayCopy.clear();
					for (Capillary cap: exp.capillaries.capillariesArrayList ) 
						capillariesArrayCopy.add(cap);
					pasteButton.setEnabled(true);
				}
			}});
		
		pasteButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					for (Capillary capFrom: capillariesArrayCopy ) 
					{
						capFrom.valid = false;
						for (Capillary capTo: exp.capillaries.capillariesArrayList) 
						{
							if (!capFrom.roi.getName().equals (capTo.roi.getName()))
								continue;
							capFrom.valid = true;
							capTo.capCageID = capFrom.capCageID;
							capTo.capNFlies = capFrom.capNFlies;
							capTo.capVolume = capFrom.capVolume;
							capTo.capStimulus = capFrom.capStimulus;
							capTo.capConcentration = capFrom.capConcentration;
						}
					}
				}
				viewModel.fireTableDataChanged();
			}});
		
		noFliesButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					int ncapillaries =  exp.capillaries.capillariesArrayList.size();
					for (int i=0; i < ncapillaries; i++) 
					{
						Capillary cap = exp.capillaries.capillariesArrayList.get(i);
						if (i< 2 || i >= ncapillaries-2) {
							cap.capNFlies = 0;
						}
						else 
						{
							cap.capNFlies = 1;
						}
					}
					viewModel.fireTableDataChanged();
				}
			}});

		
		duplicateLRButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment)parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					int rowIndex = tableView.getSelectedRow();
					int columnIndex = tableView.getSelectedColumn();
					if (rowIndex >= 0) 
					{
						Capillary cap0 = exp.capillaries.capillariesArrayList.get(rowIndex);	
						String side = cap0.getCapillarySide();
						int modulo2 = 0;
						if (side.equals("L"))
							modulo2 = 0;
						else if (side.equals("R"))
							modulo2 = 1;
						else
							modulo2 = Integer.valueOf(cap0.getCapillarySide()) % 2;
						
						for (Capillary cap: exp.capillaries.capillariesArrayList) 
						{
							if (cap.getCapillaryName().equals(cap0.getCapillaryName()))
								continue;
							if ((exp.capillaries.desc.grouping == 2) && (!cap.getCapillarySide().equals(side)))
								continue;
							else 
							{
								try 
								{
									int mod = Integer.valueOf(cap.getCapillarySide()) % 2;
									if (mod != modulo2)
										continue;
								} 
								catch (NumberFormatException nfe) 
								{
									if (!cap.getCapillarySide().equals(side))
										continue;
								}
							}
				        	switch (columnIndex) 
				        	{
				            case 2: cap.capNFlies = cap0.capNFlies; break;
				            case 3: cap.capVolume = cap0.capVolume; break;
				            case 4: cap.capStimulus = cap0.capStimulus; break;
				            case 5: cap.capConcentration = cap0.capConcentration; break;
				            default: break;
				        	}					
						}
					}
				}
			}});
		
		duplicateAllButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					int rowIndex = tableView.getSelectedRow();
					int columnIndex = tableView.getSelectedColumn();
					if (rowIndex >= 0) 
					{
						Capillary cap0 = exp.capillaries.capillariesArrayList.get(rowIndex);	
						for (Capillary cap: exp.capillaries.capillariesArrayList) {
							if (cap.getCapillaryName().equals(cap0.getCapillaryName()))
								continue;
							switch (columnIndex) 
							{
				            case 2: cap.capNFlies = cap0.capNFlies; break;
				            case 3: cap.capVolume = cap0.capVolume; break;
				            case 4: cap.capStimulus = cap0.capStimulus; break;
				            case 5: cap.capConcentration = cap0.capConcentration; break;
				            default: break;
				        	}					
						}
					}
				}
			}});
		
		getNfliesButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.cages.cageList.size() > 0) 
				{
					exp.cages.transferNFliesFromCagesToCapillaries(exp.capillaries.capillariesArrayList);
					viewModel.fireTableDataChanged();
				}
			}});
		
		getCageNoButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					exp.cages.setCageNbFromName(exp.capillaries.capillariesArrayList);
					viewModel.fireTableDataChanged();
				}
			}});
	}
	
	void close() 
	{
		dialogFrame.close();
	}
	
	private void setFixedColumnProperties (TableColumn column) 
	{
        column.setResizable(false);
        column.setPreferredWidth(50);
        column.setMaxWidth(50);
        column.setMinWidth(30);
	}
}
