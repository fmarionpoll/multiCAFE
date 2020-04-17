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
        String[] columnNames = {"Cage #",
                                "nflies",
                                "sex",
                                "age",
                                "strain", 
                                "comment"};
        Object[][] data = {
        {"Kathy", "Smith",
         "Snowboarding", new Integer(5), new Boolean(false), "comment"},
        {"John", "Doe",
         "Rowing", new Integer(3), new Boolean(true), "comment"},
        {"Sue", "Black",
         "Knitting", new Integer(2), new Boolean(false), "comment"},
        {"Jane", "White",
         "Speed reading", new Integer(20), new Boolean(true), "comment"},
        {"Joe", "Brown",
         "Pool", new Integer(10), new Boolean(false), "comment"}
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
