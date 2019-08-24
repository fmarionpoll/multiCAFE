package plugins.fmp.multicafe;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.SequenceCamData;



public class MCMoveTab_File extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;

	private JButton	openROIsButton			= new JButton("Load...");
	private JButton	saveROIsButton			= new JButton("Save...");
	private MultiCAFE parent0;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		JLabel 	loadsaveText1 = new JLabel ("-> File (xml) ");
		loadsaveText1.setHorizontalAlignment(SwingConstants.RIGHT); 
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		JLabel emptyText1	= new JLabel (" ");
		add(GuiUtil.besidesPanel( emptyText1, loadsaveText1, openROIsButton, saveROIsButton));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
	
		openROIsButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentExp);
				seqCamData.cages.xmlReadCagesFromFile(seqCamData);
				firePropertyChange("LOAD_DATA", false, true);	
			}});
		
		saveROIsButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentExp);
				seqCamData.storeAnalysisParametersToCages();
				seqCamData.xmlWriteDrosoTrackDefault();
			}});
	}

	boolean cageRoisOpen(String csFileName) {
		
		boolean flag = false;
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentExp);
		if (csFileName == null)
			flag = seqCamData.xmlReadDrosoTrackDefault();
		else
			flag = seqCamData.xmlReadDrosoTrack(csFileName);
		return flag;
	}
	
	boolean cageRoisSave() {
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentExp);
		return seqCamData.cages.xmlWriteCagesToFile("drosotrack.xml", seqCamData.getDirectory());
	}
}