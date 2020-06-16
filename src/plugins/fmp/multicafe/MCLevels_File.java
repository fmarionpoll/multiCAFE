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
				if (exp != null && loadCapillaries_Measures(exp)) {
					exp.transferCapillariesToROIs();
					firePropertyChange("MEASURES_OPEN", false, true);
				}
			}}); 
		
		saveMeasuresButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				if (exp != null) {
					exp.transferRoisToMeasures();
					exp.saveExperimentMeasures();
					firePropertyChange("MEASURES_SAVE", false, true);
				}
			}});	
	}

	boolean loadCapillaries_Measures(Experiment exp) {
		boolean flag = true;
		if (exp.seqKymos != null ) {
			boolean readOK = exp.xmlLoadMCCapillaries_Measures();
			if (readOK) {
				SwingUtilities.invokeLater(new Runnable() { public void run() {
					parent0.paneSequence.tabInfosSeq.setExperimentsInfosToDialog(exp);
					parent0.paneSequence.tabIntervals.setAnalyzeFrameToDialog(exp);
					parent0.paneKymos.tabCreate.setBuildKymosParametersToDialog(exp);
				}});
			}
		}
		return flag;
	}
	
	
}
