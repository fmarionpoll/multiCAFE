package plugins.fmp.multicafe;



import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import icy.gui.frame.IcyFrame;
import plugins.fmp.multicafeSequence.CapillaryTableModel;

public class MCCapillaries_Table  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8611587540329642259L;
	IcyFrame 					dialogFrame 			= null;
    private JTable 				tableView = new JTable();
	private CapillaryTableModel viewModel = null;

	
	
	public void initialize (MultiCAFE parent0) {		
		viewModel = new CapillaryTableModel(parent0);
	    tableView.setModel(viewModel);
	    tableView.setPreferredScrollableViewportSize(new Dimension(500, 400));
	    tableView.setFillsViewportHeight(true);
	    TableColumnModel columnModel = tableView.getColumnModel();
	    for (int i=0; i<3; i++)
	    	setFixedColumnProperties(columnModel.getColumn(i));
        JScrollPane scrollPane = new JScrollPane(tableView);
        
		dialogFrame = new IcyFrame ("Edit capillaries", true, true);	
		/*
		 *  https://stackoverflow.com/questions/15194991/how-to-put-a-table-and-3-buttons-in-a-jframe
		 * 
		 */
		JPanel topPanel = new JPanel();
		JPanel btnPanel = new JPanel();
        
		dialogFrame.add(scrollPane);
		
		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);
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
