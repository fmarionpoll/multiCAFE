package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Experiment;



public class MCSequence_Close  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7576474358794296471L;
	private JButton		closeAllButton			= new JButton("Close views");
	private MultiCAFE 	parent0 				= null;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0  = parent0;
		add( GuiUtil.besidesPanel(closeAllButton, new JLabel(" ")));
		closeAllButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				closeAll();
				firePropertyChange("CLOSE_ALL", false, true);
			}});
	}
	
	void closeAll() {
		closeCurrentExperiment();
		parent0.expList.clear();
		parent0.currentExperimentIndex = -1;
	}
	
	void closeExp(Experiment exp) {
		if (exp != null) {
			parent0.paneSequence.tabInfos.getExperimentInfosFromDialog(exp);
			parent0.paneSequence.tabIntervals.getAnalyzeFrameFromDialog (exp);
			if (exp.seqCamData != null) 
				exp.xmlSaveExperiment();
			exp.closeSequences();
		}
		parent0.paneMove.tabGraphics.closeAll();
		parent0.paneLevels.tabGraphs.closeAll();
		parent0.paneKymos.tabDisplay.kymographNamesComboBox.removeAllItems();
	}
	
	void closeCurrentExperiment() {
		if (parent0.currentExperimentIndex < 0)
			return;
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp != null)
			closeExp(exp);
	}

}
