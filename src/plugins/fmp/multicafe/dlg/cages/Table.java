package plugins.fmp.multicafe.dlg.cages;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Cage;
import plugins.fmp.multicafe.sequence.CageTableModel;
import plugins.fmp.multicafe.sequence.Experiment;

public class Table extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7599620793495187279L;
	IcyFrame 					dialogFrame 	= null;
    private JTable 				tableView 		= new JTable();
	private CageTableModel 		viewModel 		= null;
	private JButton				copyButton 		= new JButton("Copy table");
	private JButton				pasteButton 	= new JButton("Paste");
	private MultiCAFE 			parent0 		= null; 
	private List <Cage> 		cageArrayCopy 	= null;
	
	// -------------------------
	
	public void initialize (MultiCAFE parent0, List <Cage> cageCopy) {
		this.parent0 = parent0;
		cageArrayCopy = cageCopy;
		
		viewModel = new CageTableModel(parent0.expList);
	    tableView.setModel(viewModel);
	    tableView.setPreferredScrollableViewportSize(new Dimension(500, 400));
	    tableView.setFillsViewportHeight(true);
	    TableColumnModel columnModel = tableView.getColumnModel();
	    for (int i=0; i<2; i++)
	    	setFixedColumnProperties(columnModel.getColumn(i));
        JScrollPane scrollPane = new JScrollPane(tableView);
        
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(copyButton);
        topPanel.add(pasteButton);
        
        JPanel tablePanel = new JPanel();
		tablePanel.add(scrollPane);
        
		dialogFrame = new IcyFrame ("Edit capillaries", true, true);	
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
	
	private void defineActionListeners() {
		copyButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				cageArrayCopy.clear();
				for (Cage cage: exp.cages.cageList ) {
					cageArrayCopy.add(cage);
				}
				pasteButton.setEnabled(true);
			}});
		
		pasteButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				for (Cage cageFrom: cageArrayCopy ) {
					cageFrom.valid = false;
					for (Cage cageTo: exp.cages.cageList) {
						if (!cageFrom.roi.getName().equals (cageTo.roi.getName()))
							continue;
						cageFrom.valid = true;
						cageTo.cageNFlies = cageFrom.cageNFlies;
						cageTo.strCageComment = cageFrom.strCageComment;
					}
				}
				viewModel.fireTableDataChanged();
			}});
	}
	
	void close() {
		dialogFrame.close();
		Experiment exp = parent0.expList.getCurrentExperiment();
		exp.cages.transferNFliesFromCagesToCapillaries(exp.capillaries.capillariesArrayList);
		parent0.paneCapillaries.tabFile.saveCapillaries(exp);
	}
	
	private void setFixedColumnProperties (TableColumn column) {
        column.setResizable(false);
        column.setPreferredWidth(50);
        column.setMaxWidth(50);
        column.setMinWidth(30);
	}


}
