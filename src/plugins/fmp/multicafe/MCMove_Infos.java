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
	String[] columnNames = {"Cage #", "nflies", "comment"};
    Object[][] data = null;
    JTable table = new JTable(data, columnNames);
	private MultiCAFE parent0 = null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
//		setLayout(capLayout);
		this.parent0 = parent0;
		setLayout(new GridLayout(1,0));	
        
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);
        initTable();
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
	 
        //Add the scroll pane to this panel.
        add(scrollPane);
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		
	}
	
	void initTable () {
		Object [][] data = new Object [3][10];
		for (int i=0; i < 10; i++) {
			data[0][i] = new String("i");
			int j = 1;
			if (i == 0 || i == 9)
				j = 0;
			data[1][i] = j;
			data [2][i] = new String("comment");
		}
	}

}
