package plugins.fmp.multicafe;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Experiment;


public class MCSequence_Intervals extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	JSpinner 	startAnalysisJSpinner	= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1)); 
	JSpinner 	endAnalysisJSpinner		= new JSpinner(new SpinnerNumberModel(99999999, 1, 99999999, 1));
	JSpinner 	stepAnalysisJSpinner	= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	JRadioButton  isFixedFrame			= new JRadioButton("keep the same intervals for all experiment", false);
	JRadioButton  isFloatingFrame		= new JRadioButton("analyze complete files", true);
		
	
	void init(GridLayout capLayout) {
		setLayout(capLayout);	

		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		FlowLayout layout1 = (FlowLayout) panel1.getLayout();
		layout1.setVgap(0);
		panel1.add(new JLabel("Analyze from ", SwingConstants.RIGHT));
		panel1.add(startAnalysisJSpinner);
		panel1.add(new JLabel(" to "));
		panel1.add(endAnalysisJSpinner);
		panel1.add(new JLabel(" step "));
		panel1.add(stepAnalysisJSpinner );
		add(GuiUtil.besidesPanel(panel1));
		
		JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		FlowLayout layout2 = (FlowLayout) panel2.getLayout();
		layout2.setVgap(0);
		panel2.add(isFixedFrame);
		panel2.add(isFloatingFrame);
		ButtonGroup group = new ButtonGroup();
		group.add(isFloatingFrame);
		group.add(isFixedFrame);
		
		add(GuiUtil.besidesPanel(panel2));
		
		defineActionListeners();
		startAnalysisJSpinner.setEnabled(false); 
		endAnalysisJSpinner.setEnabled(false);
	}
	
	
	private void defineActionListeners() {
		isFixedFrame.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
				startAnalysisJSpinner.setEnabled(true); 
				endAnalysisJSpinner.setEnabled(true);
			}});
		isFloatingFrame.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
				startAnalysisJSpinner.setEnabled(false); 
				endAnalysisJSpinner.setEnabled(false);
			}});
	}
		
	public void setAnalyzeFrameToDialog (Experiment exp) {
		startAnalysisJSpinner.setValue((int) exp.startFrame);
		endAnalysisJSpinner.setValue((int) exp.endFrame);
		stepAnalysisJSpinner.setValue(exp.stepFrame);		
	}
	
	void getAnalyzeFrameFromDialog (Experiment exp) {		
		exp.startFrame 	= (int) startAnalysisJSpinner.getValue();
		exp.endFrame 	= (int) endAnalysisJSpinner.getValue();
		exp.stepFrame	= (int) stepAnalysisJSpinner.getValue();
	}
	
	public void setEndFrameToDialog (int end) {
		endAnalysisJSpinner.setValue(end);		
	}
	
	int getAnalysisStep() {
		return (int) stepAnalysisJSpinner.getValue();
	}
	
	boolean getIsFixedFrame() {
		return isFixedFrame.isSelected();
	}
	
	int getStartFrame() {
		return (int) startAnalysisJSpinner.getValue();
	}
	
	int getEndFrame() {
		return (int) endAnalysisJSpinner.getValue();
	}
}
