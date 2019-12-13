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
import plugins.fmp.multicafeSequence.Experiment;



public class MCMove_File extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;

	private JButton	openCagesButton			= new JButton("Load...");
	private JButton	saveCagesButton			= new JButton("Save...");
	private MultiCAFE parent0;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		JLabel 	loadsaveText1 = new JLabel ("-> File (xml) ");
		loadsaveText1.setHorizontalAlignment(SwingConstants.RIGHT); 
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		JLabel emptyText1	= new JLabel (" ");
		add(GuiUtil.besidesPanel( emptyText1, loadsaveText1, openCagesButton, saveCagesButton));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
	
		openCagesButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				exp.seqCamData.cages.xmlReadCagesFromFile(exp.seqCamData);
				firePropertyChange("LOAD_DATA", false, true);
				parent0.paneMove.tabsPane.setSelectedIndex(3);
			}});
		
		saveCagesButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				exp.seqCamData.storeAnalysisParametersToCages();
				saveCages(exp);
				parent0.paneMove.tabsPane.setSelectedIndex(3);
			}});
	}

	boolean loadCages(String csFileName) {
		
		boolean flag = false;
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (csFileName == null)
			flag = exp.seqCamData.xmlReadDrosoTrackDefault();
		else
			flag = exp.seqCamData.xmlReadDrosoTrack(csFileName);
		return flag;
	}
	
	boolean saveCages(Experiment exp) {
		return exp.saveFlyPositions();
	}
}