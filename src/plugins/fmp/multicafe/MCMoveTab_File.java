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
				parent0.seqCamData.cages.xmlReadCagesFromFile(parent0.seqCamData);
				firePropertyChange("LOAD_DATA", false, true);	
			}});
		
		saveROIsButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				parent0.seqCamData.storeAnalysisParametersToCages();
				parent0.seqCamData.xmlWriteDrosoTrackDefault();
			}});
	}

	boolean cageRoisOpen(String csFileName) {
		
		boolean flag = false;
		if (csFileName == null)
			flag = parent0.seqCamData.xmlReadDrosoTrackDefault();
		else
			flag = parent0.seqCamData.xmlReadDrosoTrack(csFileName);
		return flag;
	}
	
	boolean cageRoisSave() {
		
		return parent0.seqCamData.cages.xmlWriteCagesToFile("drosotrack.xml", parent0.seqCamData.getDirectory());
	}
}