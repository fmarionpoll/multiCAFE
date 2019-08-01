package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;


public class MCSequenceTab_Close  extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7576474358794296471L;
	private JButton		closeAllButton			= new JButton("Close views");
	private MultiCAFE parent0 = null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0  = parent0;
		add( GuiUtil.besidesPanel(closeAllButton, new JLabel(" ")));
		
		closeAllButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				closeAll();
				firePropertyChange("SEQ_CLOSE", false, true);
			}});
	}
		
	void closeAll() {
		
		if (parent0.vkymos != null) {
			ArrayList<Viewer> viewerList = parent0.vkymos.getViewers();
			for (Viewer v: viewerList)
				v.close();
			parent0.vkymos.close();
			parent0.vkymos = null;
		}
		
		parent0.movePane.graphicsTab.closeAll();
		parent0.kymographsPane.graphsTab.closeAll();

		if (parent0.vSequence != null) {
			parent0.vSequence.removeAllROI();
			parent0.vSequence.removeListener(parent0);
			parent0.vSequence.close();
			parent0.vSequence.capillaries.capillariesArrayList.clear();
		}

		parent0.capillariesPane.optionsTab.kymographNamesComboBox.removeAllItems();
	}

}
