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
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;


public class MCSequence_Intervals extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	
	private JSpinner 	startFrameJSpinner	= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1)); 
	JSpinner 			endFrameJSpinner	= new JSpinner(new SpinnerNumberModel(99999999, 1, 99999999, 1));
	JSpinner 			stepJSpinner		= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	private JButton 	updateButton 		= new JButton("Update");
	
	
	void init(GridLayout capLayout) {
		setLayout(capLayout);	
		add(GuiUtil.besidesPanel( 
				new JLabel("start ", SwingConstants.RIGHT), startFrameJSpinner, 
				new JLabel(" "), updateButton ));
		add(GuiUtil.besidesPanel( 
				new JLabel("end ", SwingConstants.RIGHT), endFrameJSpinner, 
				new JLabel(" "), new JLabel(" ") ));
		add(GuiUtil.besidesPanel( 
				new JLabel("step", SwingConstants.RIGHT), stepJSpinner, 
				new JLabel(" "), new JLabel(" ") ));
		updateButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			firePropertyChange("UPDATE", false, true);
		} } );

	}
		
	public void setAnalyzeFrameToDialog (Experiment exp) {
		SequenceCamData seq = exp.seqCamData;
		endFrameJSpinner.setValue((int) seq.analysisEnd);
		startFrameJSpinner.setValue((int) seq.analysisStart);
		stepJSpinner.setValue(seq.analysisStep);
		exp.startFrame = (int) seq.analysisStart;
		exp.endFrame = (int) seq.analysisEnd;
		exp.stepFrame = (int) seq.analysisStep;		
	}
	
	void getAnalyzeFrameFromDialog (Experiment exp) {		
		exp.startFrame 	= (int) startFrameJSpinner.getValue();
		exp.endFrame 	= (int) endFrameJSpinner.getValue();
		exp.stepFrame	= (int) stepJSpinner.getValue();
		SequenceCamData seq = exp.seqCamData;
		if (seq != null) {
			seq.analysisStart 	= (int) startFrameJSpinner.getValue();
			seq.analysisEnd 	= (int) endFrameJSpinner.getValue();
			seq.analysisStep 	= (int) stepJSpinner.getValue();
		}
	}

}
