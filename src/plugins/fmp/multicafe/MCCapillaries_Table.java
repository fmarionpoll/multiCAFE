package plugins.fmp.multicafe;



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
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.CapillaryTableModel;
import plugins.fmp.multicafeSequence.Experiment;

public class MCCapillaries_Table  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID 	= -8611587540329642259L;
	IcyFrame 					dialogFrame 	= null;
    private JTable 				tableView 		= new JTable();
	private CapillaryTableModel viewModel 		= null;
	private JButton				copyButton 		= new JButton("Copy table");
	private JButton				pasteButton 	= new JButton("Paste");
	private JButton				duplicateButton  = new JButton("Duplicate line");
	private MultiCAFE 			parent0 		= null; 
	private List <Capillary> 	capillariesArrayCopy = null;
	
	
	public void initialize (MultiCAFE parent0, List <Capillary> capCopy) {
		this.parent0 = parent0;
		capillariesArrayCopy = capCopy;
		
		viewModel = new CapillaryTableModel(parent0);
	    tableView.setModel(viewModel);
	    tableView.setPreferredScrollableViewportSize(new Dimension(500, 400));
	    tableView.setFillsViewportHeight(true);
	    TableColumnModel columnModel = tableView.getColumnModel();
	    for (int i=0; i<3; i++)
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
		pasteButton.setEnabled(capillariesArrayCopy.size() > 0);
	}
	
	private void defineActionListeners() {
		copyButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				capillariesArrayCopy.clear();
				for (Capillary cap: exp.capillaries.capillariesArrayList ) {
					capillariesArrayCopy.add(cap);
				}
				pasteButton.setEnabled(true);
			}});
		
		pasteButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				for (Capillary capFrom: capillariesArrayCopy ) {
					capFrom.valid = false;
					for (Capillary capTo: exp.capillaries.capillariesArrayList) {
						if (!capFrom.roi.getName().equals (capTo.roi.getName()))
							continue;
						capFrom.valid = true;
						capTo.cagenb = capFrom.cagenb;
						capTo.nflies = capFrom.nflies;
						capTo.volume = capFrom.volume;
						capTo.stimulus = capFrom.stimulus;
						capTo.concentration = capFrom.concentration;
					}
				}
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
