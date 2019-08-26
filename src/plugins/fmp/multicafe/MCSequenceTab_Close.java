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
	
	void saveAndClose(Experiment exp) {
		if (exp != null) {
			SequenceCamData seqCamData = exp.seqCamData;
			SequenceKymos seqKymos = exp.seqKymos;	
			System.out.println("close and save seqCamdata document ="+ seqCamData.getFileName());
			checkIfLoadingNotFinished(exp);
			if (seqKymos != null 
					&& seqKymos.capillaries != null 
					&& seqKymos.capillaries.capillariesArrayList.size() > 0) {
				parent0.capillariesPane.getCapillariesInfos(seqKymos.capillaries);
				parent0.sequencePane.infosTab.getCapillariesInfosFromDialog(seqKymos.capillaries);
				if (seqKymos.capillaries.desc.isChanged(seqKymos.capillaries.desc_old)) {
					parent0.capillariesPane.saveCapillaryTrack(exp);
					parent0.kymographsPane.fileTab.saveKymosMeasures(exp);
					parent0.movePane.saveDefaultCages(exp);
				}
				seqKymos.seq.removeAllROI();
				seqKymos.seq.close();
				seqCamData.seq.removeAllROI();
				seqCamData.seq.close();
			}
		}
		parent0.movePane.graphicsTab.closeAll();
		parent0.kymographsPane.graphsTab.closeAll();
		parent0.buildKymosPane.optionsTab.kymographNamesComboBox.removeAllItems();
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
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
		SequenceKymos seqKymos = parent0.expList.getSeqKymos(parent0.currentIndex);
		seqKymos.seq.removeAllROI();
		seqKymos.seq.close();
		parent0.movePane.graphicsTab.closeAll();
		parent0.kymographsPane.graphsTab.closeAll();
		parent0.buildKymosPane.optionsTab.kymographNamesComboBox.removeAllItems();
		seqCamData.seq.removeAllROI();
		seqCamData.seq.close();
	}

}
