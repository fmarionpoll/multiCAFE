package plugins.fmp.multicafe.capillaries;



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
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.CapillaryTableModel;
import plugins.fmp.multicafeSequence.Experiment;

public class Table  extends JPanel {
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
	private MultiCAFE 			parent0 		= null; 
	private List <Capillary> 	capillariesArrayCopy = null;
	
	
	public void initialize (MultiCAFE parent0, List <Capillary> capCopy) {
		this.parent0 = parent0;
		capillariesArrayCopy = capCopy;
		
		viewModel = new CapillaryTableModel(parent0.expList);
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
        topPanel.add(panel1);
        JPanel panel2 = new JPanel (flowLayout);
        panel2.add(getCageNoButton);
        panel2.add(getNfliesButton);
        panel2.add(duplicateLRButton);
        panel2.add(duplicateAllButton);
        topPanel.add(panel2);
        
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
		pasteButton.setEnabled(capillariesArrayCopy.size() > 0);
	}
	
	private void defineActionListeners() {
		copyButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				capillariesArrayCopy.clear();
				for (Capillary cap: exp.capillaries.capillariesArrayList ) 
					capillariesArrayCopy.add(cap);
				pasteButton.setEnabled(true);
			}});
		
		pasteButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				for (Capillary capFrom: capillariesArrayCopy ) {
					capFrom.valid = false;
					for (Capillary capTo: exp.capillaries.capillariesArrayList) {
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
				viewModel.fireTableDataChanged();
			}});
		
		duplicateLRButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				int rowIndex = tableView.getSelectedRow();
				int columnIndex = tableView.getSelectedColumn();
				if (rowIndex >= 0) {
					Capillary cap0 = exp.capillaries.capillariesArrayList.get(rowIndex);
					
					String side = cap0.getCapillarySide();
					int modulo2 = 0;
					if (side.equals("L"))
						modulo2 = 0;
					else if (side.equals("R"))
						modulo2 = 1;
					else
						modulo2 = Integer.parseInt(cap0.getCapillarySide()) % 2;
					
					for (Capillary cap: exp.capillaries.capillariesArrayList) {
						if (cap.getCapillaryName().equals(cap0.getCapillaryName()))
							continue;
						if ((exp.capillaries.desc.grouping == 2) && (!cap.getCapillarySide().equals(side)))
							continue;
						else {
							try {
							int mod = Integer.parseInt(cap.getCapillarySide()) % 2;
							if (mod != modulo2)
								continue;
							} catch (NumberFormatException nfe) {
								if (!cap.getCapillarySide().equals(side))
									continue;
							}
						}
			        	switch (columnIndex) {
			            case 2: cap.capNFlies = cap0.capNFlies; break;
			            case 3: cap.capVolume = cap0.capVolume; break;
			            case 4: cap.capStimulus = cap0.capStimulus; break;
			            case 5: cap.capConcentration = cap0.capConcentration; break;
			            default: break;
			        	}					
					}
				}
			}});
		
		duplicateAllButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				int rowIndex = tableView.getSelectedRow();
				int columnIndex = tableView.getSelectedColumn();
				if (rowIndex >= 0) {
					Capillary cap0 = exp.capillaries.capillariesArrayList.get(rowIndex);	
					for (Capillary cap: exp.capillaries.capillariesArrayList) {
						if (cap.getCapillaryName().equals(cap0.getCapillaryName()))
							continue;
						switch (columnIndex) {
			            case 2: cap.capNFlies = cap0.capNFlies; break;
			            case 3: cap.capVolume = cap0.capVolume; break;
			            case 4: cap.capStimulus = cap0.capStimulus; break;
			            case 5: cap.capConcentration = cap0.capConcentration; break;
			            default: break;
			        	}					
					}
				}
			}});
		
		getNfliesButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp.cages.cageList.size() > 0) {
					exp.cages.transferNFliesFromCagesToCapillaries(exp.capillaries.capillariesArrayList);
					viewModel.fireTableDataChanged();
				}
			}});
		
		getCageNoButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				exp.cages.setCageNbFromName(exp.capillaries.capillariesArrayList);
				viewModel.fireTableDataChanged();
			}});
	}
	
	void close() {
		dialogFrame.close();
	}
	
	private void setFixedColumnProperties (TableColumn column) {
        column.setResizable(false);
        column.setPreferredWidth(50);
        column.setMaxWidth(50);
        column.setMinWidth(30);
	}
}
