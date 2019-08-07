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
import plugins.fmp.multicafeSequence.SequencePlusUtils;



public class MCKymosTab_File  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3973928400949966679L;

	private JButton		openMeasuresButton		= new JButton("Load");
	private JButton		saveMeasuresButton		= new JButton("Save");
	private MultiCAFE 	parent0 				= null;
	static boolean 		flag 					= true;
	static boolean 		isInterrupted 			= false;
	static boolean 		isRunning 				= false;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
	
		JLabel loadsaveText3 = new JLabel ("-> File (xml) ", SwingConstants.RIGHT); 
		loadsaveText3.setFont(FontUtil.setStyle(loadsaveText3.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel(new JLabel (" "), loadsaveText3,  openMeasuresButton, saveMeasuresButton));

		defineActionListeners();
	}
	
	private void defineActionListeners() {
		openMeasuresButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				if (openKymosMeasures()) {
					firePropertyChange("MEASURES_OPEN", false, true);
				}
			}}); 
		
		saveMeasuresButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				saveKymosMeasures();
				firePropertyChange("MEASURES_SAVE", false, true);
			}});	
	}

	boolean openKymosMeasures() {
		String directory = parent0.vSequence.getDirectory();
		if (parent0.vkymos.seq != null) {
			parent0.vkymos.seq.beginUpdate();
			for (Capillary cap: parent0.vkymos.capillaries.capillariesArrayList) {
				boolean flag2 = true;
				if (flag2 = parent0.vkymos.loadXMLKymographAnalysis(cap, directory)) {
					parent0.vkymos.validateRois();
//	TODO??			parent0.vkymos.getArrayListFromRois(EnumArrayListType.cumSum);
				}
				else {
					System.out.println("load measures -> failed or not found in directory: " + directory);
				}
				
				if (!flag2)
					flag = false;
				if (isInterrupted) {
					isInterrupted = false;
					break;
				}
			}
			parent0.vkymos.seq.endUpdate();
			
			if (parent0.vkymos.seq.getSizeT() >0 ) {
				if (parent0.vkymos.analysisEnd > parent0.vkymos.analysisStart) {
					parent0.vSequence.analysisStart = parent0.vkymos.analysisStart; 
					parent0.vSequence.analysisEnd 	= parent0.vkymos.analysisEnd;
					parent0.vSequence.analysisStep 	= parent0.vkymos.analysisStep;
				}
			}
		}
		
		isRunning = false;
		return flag;
	}
	
	void saveKymosMeasures() {
		if (parent0.vkymos != null) {
			SequencePlusUtils.transferSequenceInfoToKymos(parent0.vkymos, parent0.vSequence);
			SequencePlusUtils.saveKymosMeasures(parent0.vkymos, parent0.vSequence.getDirectory());
		}
	}
}
