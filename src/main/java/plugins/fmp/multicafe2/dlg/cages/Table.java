package plugins.fmp.multicafe2.dlg.cages;

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
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import icy.gui.frame.IcyFrame;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.CageTableModel;
import plugins.fmp.multicafe2.experiment.Cage;
import plugins.fmp.multicafe2.experiment.Experiment;

public class Table extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7599620793495187279L;
	IcyFrame 					dialogFrame 	= null;
    private JTable 				tableView 		= new JTable();
	private CageTableModel 		viewModel 		= null;
	private JButton				copyButton 		= new JButton("Copy table");
	private JButton				pasteButton 	= new JButton("Paste");
	private JButton				duplicateAllButton = new JButton("Duplicate cell to all");
	private MultiCAFE2 			parent0 		= null; 
	private List <Cage> 		cageArrayCopy 	= null;
	
	// -------------------------
	
	public void initialize (MultiCAFE2 parent0, List <Cage> cageCopy) 
	{
		this.parent0 = parent0;
		cageArrayCopy = cageCopy;
		
		viewModel = new CageTableModel(parent0.expListCombo);
	    tableView.setModel(viewModel);
	    tableView.setPreferredScrollableViewportSize(new Dimension(500, 400));
	    tableView.setFillsViewportHeight(true);
	    TableColumnModel columnModel = tableView.getColumnModel();
	    for (int i=0; i<2; i++)
	    	setFixedColumnProperties(columnModel.getColumn(i));
        JScrollPane scrollPane = new JScrollPane(tableView);
        
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT); 
		JPanel panel1 = new JPanel (flowLayout);
		panel1.add(copyButton);
        panel1.add(pasteButton);
        topPanel.add(panel1);
        
        JPanel panel2 = new JPanel (flowLayout);
        panel2.add(duplicateAllButton);
        topPanel.add(panel2);
        
        JPanel tablePanel = new JPanel();
		tablePanel.add(scrollPane);
        
		dialogFrame = new IcyFrame ("Cages properties", true, true);	
		dialogFrame.add(topPanel, BorderLayout.NORTH);
		dialogFrame.add(tablePanel, BorderLayout.CENTER);
		
		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);
		defineActionListeners();
		pasteButton.setEnabled(cageArrayCopy.size() > 0);
	}
	
	private void defineActionListeners() 
	{
		copyButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				cageArrayCopy.clear();
				for (Cage cage: exp.cages.cageList ) 
				{
					cageArrayCopy.add(cage);
				}
				pasteButton.setEnabled(true);
			}});
		
		pasteButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				for (Cage cageFrom: cageArrayCopy ) 
				{
					cageFrom.valid = false;
					for (Cage cageTo: exp.cages.cageList) 
					{
						if (!cageFrom.cageRoi.getName().equals (cageTo.cageRoi.getName()))
							continue;
						cageFrom.valid 			= true;
						cageTo.cageNFlies 		= cageFrom.cageNFlies;
						cageTo.cageAge 			= cageFrom.cageAge;
						cageTo.strCageComment 	= cageFrom.strCageComment;
						cageTo.strCageSex 		= cageFrom.strCageSex;
						cageTo.strCageStrain 	= cageFrom.strCageStrain;
					}
				}
				viewModel.fireTableDataChanged();
			}});
		
		duplicateAllButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				int rowIndex = tableView.getSelectedRow();
				int columnIndex = tableView.getSelectedColumn();
				if (rowIndex >= 0) 
				{
					Cage cage0 = exp.cages.cageList.get(rowIndex);	
					for (Cage cage: exp.cages.cageList) 
					{
						if (cage.cageRoi.getName().equals(cage0.cageRoi.getName()))
							continue;
						switch (columnIndex) {
			            case 1: cage.cageNFlies 	= cage0.cageNFlies; break;
			            case 2: cage.strCageStrain 	= cage0.strCageStrain; break;
			            case 3: cage.strCageSex 	= cage0.strCageSex; break;
			            case 4: cage.cageAge 		= cage0.cageAge; break;
			            case 5: cage.strCageComment = cage0.strCageComment; break;
			            default: break;
			        	}					
					}
				}
			}});
	}
	
	void close() {
		dialogFrame.close();
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		exp.cages.transferNFliesFromCagesToCapillaries(exp.capillaries.capillariesArrayList);
		parent0.paneCapillaries.tabFile.saveCapillaries_file(exp);
	}
	
	private void setFixedColumnProperties (TableColumn column) 
	{
        column.setResizable(false);
        column.setPreferredWidth(50);
        column.setMaxWidth(50);
        column.setMinWidth(30);
	}

}