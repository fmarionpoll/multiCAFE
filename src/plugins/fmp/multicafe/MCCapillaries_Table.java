package plugins.fmp.multicafe;


import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
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
//		this.parent0 = parent0;
		dialogFrame = new IcyFrame ("Edit capillaries", true, true);
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		dialogFrame.setLayout(new BorderLayout());
		
		viewModel = new CapillaryTableModel(parent0);
	    tableView.setModel(viewModel);
	    tableView.setPreferredScrollableViewportSize(new Dimension(500, 70));
	    tableView.setFillsViewportHeight(true);
	    TableColumnModel columnModel = tableView.getColumnModel();
        setFixedColumnProperties(columnModel.getColumn(0));
        setFixedColumnProperties(columnModel.getColumn(1));

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(tableView);
        //Add the scroll pane to this panel.
        dialogFrame.add(scrollPane);
        mainPanel.add(tableView);
		dialogFrame.add(mainPanel);
		
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
