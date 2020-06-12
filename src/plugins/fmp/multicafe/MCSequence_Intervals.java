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
	JSpinner 		startFrameJSpinner	= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1)); 
	JSpinner 		endFrameJSpinner	= new JSpinner(new SpinnerNumberModel(99999999, 1, 99999999, 1));
	JRadioButton  	isFixedFrame		= new JRadioButton("keep the same intervals for all experiment", false);
	JRadioButton  	isFloatingFrame		= new JRadioButton("analyze complete experiments", true);
	JLabel			stepFrameLabel			= new JLabel(" step = 1");	
	
	void init(GridLayout capLayout) {
		setLayout(capLayout);	

		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);
		
		JPanel panel1 = new JPanel(layout1);
		panel1.add(new JLabel("From ", SwingConstants.RIGHT));
		panel1.add(startFrameJSpinner);
		panel1.add(new JLabel(" to "));
		panel1.add(endFrameJSpinner);
		panel1.add(stepFrameLabel);
		add(GuiUtil.besidesPanel(panel1));
		
		JPanel panel2 = new JPanel(layout1);
		panel2.add(isFixedFrame);
		add(GuiUtil.besidesPanel(panel2));
		

		JPanel panel3 = new JPanel(layout1);
		panel3.add(isFloatingFrame);
		add(GuiUtil.besidesPanel(panel3));
		
		ButtonGroup group = new ButtonGroup();
		group.add(isFloatingFrame);
		group.add(isFixedFrame);
		
		defineActionListeners();
		startFrameJSpinner.setEnabled(false); 
		endFrameJSpinner.setEnabled(false);
	}
	
	
	private void defineActionListeners() {
		isFixedFrame.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
				startFrameJSpinner.setEnabled(true); 
				endFrameJSpinner.setEnabled(true);
			}});
		isFloatingFrame.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
				startFrameJSpinner.setEnabled(false); 
				endFrameJSpinner.setEnabled(false);
			}});
	}
		
	public void setAnalyzeFrameToDialog (Experiment exp) {
		exp.checkKymoFrameStep();
		startFrameJSpinner.setValue((int) exp.getKymoFrameStart());
		if (exp.getKymoFrameEnd() == 0)
			exp.setKymoFrameEnd(exp.getSeqCamSizeT());
		endFrameJSpinner.setValue((int) exp.getKymoFrameEnd());
		if (exp.getKymoFrameStep() <= 0 )
			exp.setKymoFrameStep(1);
		stepFrameLabel.setText(" step=" +exp.getKymoFrameStep());
	}
	
	void getAnalyzeFrameFromDialog (Experiment exp) {		
		exp.setKymoFrameStart ((int) startFrameJSpinner.getValue());
		exp.setKymoFrameEnd ( (int) endFrameJSpinner.getValue());
	}
	
	public void setEndFrameToDialog (int end) {
		endFrameJSpinner.setValue(end);		
	}
	
	boolean getIsFixedFrame() {
		return isFixedFrame.isSelected();
	}
	
	int getStartFrame() {
		return (int) startFrameJSpinner.getValue();
	}
	
	int getEndFrame() {
		return (int) endFrameJSpinner.getValue();
	}
}
