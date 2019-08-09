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
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.SequenceKymosUtils;



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
		String directory = parent0.vSequence.getDirectory();
		boolean flag = true;
		if (parent0.vkymos.seq != null) {
			parent0.vkymos.seq.removeAllROI();
			for (Capillary cap: parent0.vkymos.capillaries.capillariesArrayList) {
				boolean flag2 = parent0.vkymos.loadXMLKymographAnalysis(cap, directory);
				if (flag2 ) {
					parent0.vkymos.seq.addROIs(cap.getROIsFromMeasures(), false);
				} else {
					System.out.println("load measures -> failed or not found in directory: " + directory);
					flag = false;
				}					
			}
			if (parent0.vkymos.seq.getSizeT() >0 ) {
				if (parent0.vkymos.analysisEnd > parent0.vkymos.analysisStart) {
					parent0.vSequence.analysisStart = parent0.vkymos.analysisStart; 
					parent0.vSequence.analysisEnd 	= parent0.vkymos.analysisEnd;
					parent0.vSequence.analysisStep 	= parent0.vkymos.analysisStep;
				}
			}
		}
		return flag;
	}
	
	void saveKymosMeasures() {
		if (parent0.vkymos != null) {
			SequenceKymosUtils.transferSequenceInfoToKymos(parent0.vkymos, parent0.vSequence);
			SequenceKymosUtils.saveKymosMeasures(parent0.vkymos, parent0.vSequence.getDirectory());
		}
	}
}
