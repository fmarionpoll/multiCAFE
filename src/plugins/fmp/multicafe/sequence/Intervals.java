package plugins.fmp.multicafe.sequence;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafeSequence.Experiment;


public class Intervals extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	private MultiCAFE parent0 			= null;
	JSpinner 		startFrameJSpinner	= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1)); 
	JSpinner 		endFrameJSpinner	= new JSpinner(new SpinnerNumberModel(99999999, 1, 99999999, 1));
	JRadioButton  	isFixedFrame		= new JRadioButton("keep the same intervals for all experiment", false);
	JRadioButton  	isFloatingFrame		= new JRadioButton("analyze complete experiments", true);
	JLabel			stepFrameLabel		= new JLabel(" step = 1");
	JButton			resetButton			= new JButton("Reset intervals");
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);
		
		JPanel panel1 = new JPanel(layout1);
		panel1.add(new JLabel("From ", SwingConstants.RIGHT));
		panel1.add(startFrameJSpinner);
		panel1.add(new JLabel(" to "));
		panel1.add(endFrameJSpinner);
		panel1.add(stepFrameLabel);
		panel1.add(resetButton);
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
		
		resetButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			Experiment exp = parent0.expList.getCurrentExperiment();
			exp.setKymoFrameStart(0);
			if (exp.seqCamData != null && exp.seqCamData.seq != null) {
				if (isFloatingFrame.isSelected()) {
					exp.setKymoFrameEnd(exp.seqCamData.seq.getSizeT()-1);
					int step = (int) Math.round((double) exp.getKymoFrameEnd() / exp.seqKymos.imageWidthMax);
					if (step > 0)
						exp.setKymoFrameStep(step);
				} else {
					 getAnalyzeFrameFromDialog (exp);
					 exp.setKymoFrameStep((int) parent0.paneKymos.tabCreate.stepFrameJSpinner.getValue());
				}
			} 
			setAnalyzeFrameToDialog(exp);
		}});
	}
		
	public void setAnalyzeFrameToDialog (Experiment exp) {
		startFrameJSpinner.setValue((int) exp.getKymoFrameStart());
		if (exp.getKymoFrameEnd() == 0)
			exp.setKymoFrameEnd(exp.getSeqCamSizeT());
		endFrameJSpinner.setValue((int) exp.getKymoFrameEnd());
		if (exp.getKymoFrameStep() <= 0 )
			exp.setKymoFrameStep(1);
		stepFrameLabel.setText(" step=" +exp.getKymoFrameStep());
	}
	
	public void getAnalyzeFrameFromDialog (Experiment exp) {		
		exp.setKymoFrameStart ((int) startFrameJSpinner.getValue());
		exp.setKymoFrameEnd ( (int) endFrameJSpinner.getValue());
	}
	
	public void setEndFrameToDialog (int end) {
		endFrameJSpinner.setValue(end);		
	}
	
	public boolean getIsFixedFrame() {
		return isFixedFrame.isSelected();
	}
	
	public int getStartFrame() {
		return (int) startFrameJSpinner.getValue();
	}
	
	public int getEndFrame() {
		return (int) endFrameJSpinner.getValue();
	}
}
