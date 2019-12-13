package plugins.fmp.multicafe;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;



public class MCLevels_File  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3973928400949966679L;

	private JButton		loadMeasuresButton		= new JButton("Load");
	private JButton		saveMeasuresButton		= new JButton("Save");
	private MultiCAFE 	parent0 				= null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
	
		JLabel loadsaveText3 = new JLabel ("-> File (xml) ", SwingConstants.RIGHT); 
		loadsaveText3.setFont(FontUtil.setStyle(loadsaveText3.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel(new JLabel (" "), loadsaveText3,  loadMeasuresButton, saveMeasuresButton));

		defineActionListeners();
	}
	
	private void defineActionListeners() {
		loadMeasuresButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				if (loadKymosMeasures(exp)) {
					transferCapillariesToROIs(exp);
					firePropertyChange("MEASURES_OPEN", false, true);
				}
			}}); 
		
		saveMeasuresButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				transferRoisToMeasures(exp);
				saveKymosMeasures(exp);
				firePropertyChange("MEASURES_SAVE", false, true);
			}});	
	}

	boolean loadKymosMeasures(Experiment exp) {
		String directory = exp.seqCamData.getDirectory();
		boolean flag = true;
		if (exp.seqKymos != null ) {
			boolean readOK = exp.xmlLoadKymos_Measures(directory);
			if (readOK) {
				SwingUtilities.invokeLater(new Runnable() { public void run() {
					parent0.paneSequence.tabInfos.setExperimentsInfosToDialog(exp);
					parent0.paneSequence.tabIntervals.setAnalyzeFrameToDialog(exp);
					parent0.paneKymos.tabCreate.setBuildKymosParametersToDialog(exp);
				}});
			}
		}
		return flag;
	}
	
	boolean transferRoisToMeasures(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		boolean flag = true;
		if (seqKymos != null && seqKymos.seq != null) {
			seqKymos.transferKymosRoisToMeasures(exp.capillaries);
		}
		return flag;
	}
	
	boolean transferCapillariesToROIs(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		boolean flag = true;
		if (seqKymos != null && seqKymos.seq != null) {
			seqKymos.transferMeasuresToKymosRois(exp.capillaries);
		}
		return flag;
	}
	
	void saveKymosMeasures(Experiment exp) {
		exp.saveExperimentMeasures();
	}
}
