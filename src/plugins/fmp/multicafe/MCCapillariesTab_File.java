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
		boolean flag = false;
		if (parent0.seqKymos == null)
			parent0.seqKymos = new SequenceKymos();
		flag = parent0.seqKymos.xmlLoadCapillaryTrack(parent0.seqCamData.getDirectory());
		if (flag) {
			SequenceKymosUtils.transferKymoCapillariesToCamData (parent0.seqCamData, parent0.seqKymos);
		} else {
			String filename = parent0.seqCamData.getDirectory() + File.separator + "roislines.xml";
			flag = parent0.seqCamData.xmlReadROIs(filename);
			if (flag) {
				parent0.seqKymos.xmlReadRoiLineParameters(filename);
				SequenceKymosUtils.transferCamDataROIStoKymo(parent0.seqCamData, parent0.seqKymos);
			}
		}
		return flag;
	}
	
	boolean saveCapillaryTrack() {
		parent0.capillariesPane.infosTab.getCapillariesInfosFromDialog(parent0.seqKymos.capillaries);
		parent0.sequencePane.infosTab.getCapillariesInfosFromDialog(parent0.seqKymos.capillaries);
		parent0.capillariesPane.buildarrayTab.getCapillariesInfosFromDialog(parent0.seqKymos.capillaries);
		parent0.sequencePane.browseTab.getAnalyzeFrameAndStep (parent0.seqCamData);
		if (parent0.seqKymos.capillaries.capillariesArrayList.size() == 0)
			parent0.seqKymos.updateCapillariesFromCamData(parent0.seqCamData);
		return parent0.seqKymos.xmlSaveCapillaryTrack(parent0.seqCamData.getDirectory());
	}

}
