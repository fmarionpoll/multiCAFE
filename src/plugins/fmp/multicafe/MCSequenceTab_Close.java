package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;


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
		if (parent0.seqKymos != null && parent0.seqKymos.seq != null) {
			parent0.seqKymos.seq.close();
			parent0.seqKymos = null;
		}
		parent0.movePane.graphicsTab.closeAll();
		parent0.kymographsPane.graphsTab.closeAll();
		parent0.capillariesPane.optionsTab.kymographNamesComboBox.removeAllItems();

		if (parent0.seqCamData != null) {
			parent0.seqCamData.seq.close();
			parent0.seqCamData = null;
		}
	}

}
