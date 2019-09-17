package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.SequenceKymos;


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
				firePropertyChange("SEQ_CLOSE", false, true);
			}});
	}
	
	void saveAndClose(Experiment exp) {
		if (exp != null) {
			SequenceCamData seqCamData = exp.seqCamData;
			SequenceKymos seqKymos = exp.seqKymos;	
			checkIfLoadingNotFinished(exp);
			if (seqKymos != null && seqKymos.seq != null) {
				seqKymos.seq.removeAllROI();
				seqKymos.seq.close();
				seqKymos.seq.closed();
			}
			if (seqCamData != null && seqCamData.seq != null) {
				seqCamData.seq.removeAllROI();
				seqCamData.seq.close();
				seqCamData.seq.closed();
			}
		}
		parent0.movePane.graphicsTab.closeAll();
		parent0.kymographsPane.graphsTab.closeAll();
		parent0.buildKymosPane.displayTab.kymographNamesComboBox.removeAllItems();
	}
	
	private void checkIfLoadingNotFinished(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos.isRunning_loadImages) {
			seqKymos.isInterrupted_loadImages = true;
			while (seqKymos.isRunning_loadImages) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	void closeAll() {
		if (parent0.currentIndex < 0)
			return;
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		SequenceKymos seqKymos = exp.seqKymos;
		seqKymos.seq.removeAllROI();
		seqKymos.seq.close();
		seqKymos.seq.closed();
		parent0.movePane.graphicsTab.closeAll();
		parent0.kymographsPane.graphsTab.closeAll();
		parent0.buildKymosPane.displayTab.kymographNamesComboBox.removeAllItems();
		seqCamData.seq.removeAllROI();
		seqCamData.seq.close();
		seqCamData.seq.closed();
	}

}
