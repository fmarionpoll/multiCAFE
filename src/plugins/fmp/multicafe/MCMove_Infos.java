package plugins.fmp.multicafe;


import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;



public class MCMove_Infos  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3325915033686366985L;
	private MultiCAFE parent0 = null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
//		setLayout(capLayout);
		this.parent0 = parent0;
		setLayout(new GridLayout(1,0));		 
        String[] columnNames = {"Cage #", "nflies", "comment"};
        Object[][] data = {
        {"Kathy", 0, "comment"},
        {"John", 1, "Rowing"},
        {"Sue", 1, "Knitting"},
        {"Jane", 1, "comment"},
        {"Joe", 1, "comment"}
        };
 
        final JTable table = new JTable(data, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);
 
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
	 
        //Add the scroll pane to this panel.
        add(scrollPane);
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		
	}

}
