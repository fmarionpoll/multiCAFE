package plugins.fmp.multicafe.dlg.kymos;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Experiment;




public class Intervals extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1530811745749103710L;
	private MultiCAFE parent0 				= null;
	JSpinner 	pivotBinSize				= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	private 	JComboBox<String> binUnit 	= new JComboBox<String> (new String[] {"ms", "s", "min", "h", "day"});
	JButton		applyButton					= new JButton("Apply");
	JSpinner 	startColumnJSpinner	= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1)); 
	JSpinner 	endColumnJSpinner	= new JSpinner(new SpinnerNumberModel(99999999, 1, 99999999, 1));

	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);
		
		JPanel panel1 = new JPanel(layout1);
		panel1.add(new JLabel("Column ", SwingConstants.RIGHT));
		panel1.add(startColumnJSpinner);
		panel1.add(new JLabel(" to "));
		panel1.add(endColumnJSpinner);
		add(GuiUtil.besidesPanel(panel1));
		
		JPanel panel2 = new JPanel(layout1);
		panel2.add(new JLabel("  bin size "));
		panel2.add(pivotBinSize);
		panel2.add(binUnit);
		binUnit.setSelectedIndex(2);
		add(panel2); 
		
		JPanel panel3 = new JPanel(layout1);
		panel3.add(applyButton);
		add(GuiUtil.besidesPanel(panel3));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		applyButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			Experiment exp = parent0.expList.getCurrentExperiment();
//			exp.setKymoFrameStart(0);
//			if (exp.seqCamData != null && exp.seqCamData.seq != null) {
//				if (isFloatingFrame.isSelected()) {
//					exp.setKymoFrameEnd(exp.seqCamData.seq.getSizeT()-1);
//					int step = (int) Math.round((double) exp.getKymoFrameEnd() / exp.seqKymos.imageWidthMax);
//					if (step > 0)
//						exp.setKymoFrameStep(step);
//				} else {
//					 getAnalyzeFrameFromDialog (exp);
//					 exp.setKymoFrameStep((int) parent0.paneKymos.tabCreate.stepFrameJSpinner.getValue());
//				}
//			} 
//			setAnalyzeFrameToDialog(exp);
		}});
	}
	
//	public void setAnalyzeFrameToDialog (Experiment exp) {
//		startFrameJSpinner.setValue((int) exp.getKymoFrameStart());
//		if (exp.getKymoFrameEnd() == 0)
//			exp.setKymoFrameEnd(exp.getSeqCamSizeT());
//		endFrameJSpinner.setValue((int) exp.getKymoFrameEnd());
//		if (exp.getKymoFrameStep() <= 0 )
//			exp.setKymoFrameStep(1);
//		stepFrameLabel.setText(" step=" +exp.getKymoFrameStep());
//	}

	
	public int getBuildStep() {
		int buildStep = ((int) pivotBinSize.getValue()) * getBinSize();
		return buildStep;
	}
	
	private int getBinSize() {
		int binsize = 1;
		int iselected = binUnit.getSelectedIndex();
		switch (iselected) {
		case 1: binsize = 1000; break;
		case 2: binsize = 1000 * 60; break;
		case 3: binsize = 1000 * 60 * 60; break;
		case 4: binsize = 1000 * 60 * 60 * 24; break;
		case 0:
		default:
			break;
		}
		return binsize;
	}
	
	

}
