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



public class MCKymosTab_File  extends JPanel {
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
				if (loadKymosMeasures()) {
					firePropertyChange("MEASURES_OPEN", false, true);
				}
			}}); 
		
		saveMeasuresButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				saveKymosMeasures();
				firePropertyChange("MEASURES_SAVE", false, true);
			}});	
	}

	boolean loadKymosMeasures() {
		String directory = parent0.seqCamData.getDirectory();
		boolean flag = true;
		SequenceKymos seqk = parent0.seqKymos;
		if (seqk != null && seqk.seq != null) {
			seqk.seq.removeAllROI();
			seqk.xmlReadCapillaryTrack(directory);
			seqk.transferMeasuresToKymosRois();
		}
		return flag;
	}
	
	void saveKymosMeasures() {
		SequenceKymos seqk = parent0.seqKymos;
		if (seqk != null) {
			seqk.getAnalysisParametersFromCamData(parent0.seqCamData);
			seqk.roisSaveEdits();
			//SequenceKymosUtils.saveKymosMeasures(parent0.seqKymos, parent0.seqCamData.getDirectory());
			String name = parent0.seqCamData.getDirectory()+ File.separator + "capillarytrack.xml";
			seqk.xmlWriteCapillaryTrack(name);
		}
	}
}
