package plugins.fmp.multicafe;


import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import plugins.fmp.multicafeSequence.CageTableModel;




public class MCMove_Infos  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3325915033686366985L;
    private JTable tableView = new JTable();
	private AbstractTableModel viewModel;
	private MultiCAFE parent0 = null;
	
    
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
//		setLayout(capLayout);
		this.parent0 = parent0;
		setLayout(new GridLayout(1,0));		 
		viewModel = new CageTableModel(parent0);
        
	    tableView.setModel(viewModel);
	    tableView.setPreferredScrollableViewportSize(new Dimension(500, 70));
	    tableView.setFillsViewportHeight(true);

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(tableView);
        //Add the scroll pane to this panel.
        add(scrollPane);
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		
	}


}
