package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.SequenceCamData;


public class MCSequenceTab_Analysis extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	
	private JSpinner 	startFrameJSpinner		= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1)); 
	JSpinner 			endFrameJSpinner		= new JSpinner(new SpinnerNumberModel(99999999, 0, 99999999, 1));
	private JSpinner 	analyzeStepJSpinner 	= new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
	private JButton 	updateButton 			= new JButton("Update");
	
	
	void init(GridLayout capLayout) {
		setLayout(capLayout);	
		add(GuiUtil.besidesPanel( 
				new JLabel("start ", SwingConstants.RIGHT), startFrameJSpinner, 
				new JLabel("step ", SwingConstants.RIGHT) , analyzeStepJSpinner 				
				));
		add(GuiUtil.besidesPanel( 
				new JLabel("end ", SwingConstants.RIGHT), endFrameJSpinner, 
				new JLabel(" "), updateButton ));

		updateButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			firePropertyChange("UPDATE", false, true);
		} } );

	}
		
	void setAnalyzeFrameAndStepToDialog (SequenceCamData seq) {
		endFrameJSpinner.setValue((int) seq.analysisEnd);
		startFrameJSpinner.setValue((int) seq.analysisStart);
		analyzeStepJSpinner.setValue(seq.analysisStep);
	}
	
	void getAnalyzeFrameAndStepFromDialog (SequenceCamData seq) {
		seq.analysisStart 	= (int) startFrameJSpinner.getValue();
		seq.analysisEnd 	= (int) endFrameJSpinner.getValue();
		seq.analysisStep 	= (int) analyzeStepJSpinner.getValue();
	}

}
