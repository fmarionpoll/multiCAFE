package plugins.fmp.multicafe;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeSequence.SequenceKymosUtils;


public class MCCapillariesTab_File extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4019075448319252245L;
	
	private JButton		openButtonCapillaries	= new JButton("Load...");
	private JButton		saveButtonCapillaries	= new JButton("Save...");
	private MultiCAFE 	parent0 				= null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		
		JLabel loadsaveText1 = new JLabel ("-> Capillaries (xml) ", SwingConstants.RIGHT);
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1, openButtonCapillaries, saveButtonCapillaries));
			
		this.parent0 = parent0;
		defineActionListeners();
	}
	
	private void defineActionListeners() {	
		openButtonCapillaries.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			firePropertyChange("CAPILLARIES_NEW", false, true);
		}}); 
		saveButtonCapillaries.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			firePropertyChange("CAP_ROIS_SAVE", false, true);
		}});	

	}
	
	boolean loadCapillaryTrack() {	
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentExp);
		SequenceKymos seqKymos = parent0.expList.getSeqKymos(parent0.currentExp);boolean flag = false;
		if (seqKymos == null)
			seqKymos = new SequenceKymos();
		flag = seqKymos.xmlLoadCapillaryTrack(seqCamData.getDirectory());
		
		if (flag) {
			SequenceKymosUtils.transferKymoCapillariesToCamData (seqCamData, seqKymos);	
		} else {
			String filename = seqCamData.getDirectory() + File.separator + "roislines.xml";
			flag = seqCamData.xmlReadROIs(filename);
			if (flag) {
				seqKymos.xmlReadRoiLineParameters(filename);
				SequenceKymosUtils.transferCamDataROIStoKymo(seqCamData, seqKymos);
			}
		}
		return flag;
	}
	
	boolean saveCapillaryTrack() {
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentExp);
		SequenceKymos seqKymos = parent0.expList.getSeqKymos(parent0.currentExp);
		parent0.capillariesPane.infosTab.getCapillariesInfosFromDialog(seqKymos.capillaries);
		parent0.sequencePane.infosTab.getCapillariesInfosFromDialog(seqKymos.capillaries);
		parent0.capillariesPane.buildarrayTab.getCapillariesInfosFromDialog(seqKymos.capillaries);
		parent0.sequencePane.browseTab.getAnalyzeFrameAndStep (seqCamData);
		seqKymos.updateCapillariesFromCamData(seqCamData);
		return seqKymos.xmlSaveCapillaryTrack(seqCamData.getDirectory());
	}

}
