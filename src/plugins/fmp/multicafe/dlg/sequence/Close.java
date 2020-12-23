package plugins.fmp.multicafe.dlg.sequence;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Experiment;



public class Close  extends JPanel {
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
		parent0.expList.currentExperimentIndex = -1;
	}
	
	public void closeExp(Experiment exp) {
		if (exp != null) {
			parent0.paneSequence.tabInfosSeq.getExperimentInfosFromDialog(exp);
			parent0.paneSequence.tabIntervals.getCamDataIntervalsFromDialog (exp);
			if (exp.seqCamData != null) {
				exp.xmlSaveExperiment();
				exp.saveExperimentMeasures(exp.getResultsDirectory());
//				exp.xmlWriteDrosoTrackDefault(parent0.paneCages.tabFile.saveRoisCheckBox.isSelected());
			}
			exp.closeSequences();
		}
		parent0.paneCages.tabGraphics.closeAll();
		parent0.paneLevels.tabGraphs.closeAll();
		parent0.paneKymos.tabDisplay.kymographNamesComboBox.removeAllItems();
	}
	
	public void closeCurrentExperiment() {
		if (parent0.expList.currentExperimentIndex < 0)
			return;
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp != null)
			closeExp(exp);
	}

}
