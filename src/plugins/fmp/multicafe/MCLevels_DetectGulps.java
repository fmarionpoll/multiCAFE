package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import icy.util.StringUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeTools.DetectGulps_series;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCLevels_DetectGulps extends JPanel  implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long 	serialVersionUID 		= -5590697762090397890L;
	
	JCheckBox				detectAllGulpsCheckBox 		= new JCheckBox ("all kymographs", true);
	JComboBox<TransformOp> 	transformForGulpsComboBox 	= new JComboBox<TransformOp> (new TransformOp[] {TransformOp.XDIFFN /*, TransformOp.YDIFFN, TransformOp.XYDIFFN	*/});
	JSpinner				startSpinner				= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner				endSpinner					= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	JCheckBox				buildDerivativeCheckBox 	= new JCheckBox ("build derivative", true);
	JCheckBox				detectGulpsCheckBox 		= new JCheckBox ("detect gulps", true);
	
	private JCheckBox		partCheckBox 				= new JCheckBox ("detect from", false);
	private JButton			displayTransform2Button		= new JButton("Display");
	private JSpinner		spanTransf2Spinner			= new JSpinner(new SpinnerNumberModel(3, 0, 500, 1));
	private JSpinner 		detectGulpsThresholdSpinner	= new JSpinner(new SpinnerNumberModel(90, 0, 500, 1));
	private String 			detectString 				= "        Detect     ";
	private JButton 		detectButton 				= new JButton(detectString);
	private JCheckBox		allSeriesCheckBox 			= new JCheckBox("ALL series", false);
	private DetectGulps_series 	thread 					= null;
	private MultiCAFE 		parent0;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		JPanel panel0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		((FlowLayout)panel0.getLayout()).setVgap(0);
		panel0.add( detectButton);
		panel0.add( allSeriesCheckBox);
		panel0.add(detectAllGulpsCheckBox);
		add( GuiUtil.besidesPanel(panel0 ));
		
		add( GuiUtil.besidesPanel(new JLabel("threshold", SwingConstants.RIGHT), detectGulpsThresholdSpinner, transformForGulpsComboBox, displayTransform2Button));
		
		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		((FlowLayout)panel1.getLayout()).setVgap(0);
		panel1.add(buildDerivativeCheckBox);
		panel1.add(detectGulpsCheckBox);
		panel1.add(partCheckBox);
		panel1.add(startSpinner);
		panel1.add(new JLabel("to"));
		panel1.add(endSpinner);
		add( panel1);

		transformForGulpsComboBox.setSelectedItem(TransformOp.XDIFFN);
		defineActionListeners();
	}
	
	private void defineActionListeners() {

		transformForGulpsComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				kymosDisplayFiltered2();
				series_detectGulpsStart(false);
			}});
		
		detectButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				if (detectButton.getText() .equals(detectString))
					series_detectGulpsStart(true);
				else 
					series_detectGulpsStop();
			}});
		
		displayTransform2Button.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				kymosDisplayFiltered2();
			}});
		
		allSeriesCheckBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				Color color = Color.BLACK;
				if (allSeriesCheckBox.isSelected()) 
					color = Color.RED;
				allSeriesCheckBox.setForeground(color);
				detectButton.setForeground(color);
		}});
		
	}
		
	void kymosDisplayFiltered2() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp == null) 
			return;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		
		TransformOp transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
		int zChannelDestination = 2;
		exp.kymosBuildFiltered(0, zChannelDestination, transform, (int) spanTransf2Spinner.getValue());
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	void series_detectGulpsStart(boolean detectGulps) {
		kymosDisplayFiltered2();
		
		parent0.currentExperimentIndex = parent0.paneSequence.expListComboBox.getSelectedIndex();
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp == null)
			return;

		exp.transferRoisToMeasures();
		exp.saveExperimentMeasures();
		parent0.paneSequence.tabClose.closeExp(exp);
		thread = new DetectGulps_series();
		parent0.paneSequence.tabIntervals.getAnalyzeFrameFromDialog(exp);
		exp.seqKymos.transferKymosRoisToCapillaries(exp.capillaries);
		
		DetectGulps_Options options = thread.options;
		options.expList = new ExperimentList(); 
		parent0.paneSequence.transferExperimentNamesToExpList(options.expList, true);		
		if (allSeriesCheckBox.isSelected()) {
			options.expList.index0 = 0;
			options.expList.index1 = options.expList.getSize()-1;
		} else {
			options.expList.index0 = parent0.currentExperimentIndex;
			options.expList.index1 = parent0.currentExperimentIndex;
		}
//		if (!allKymosCheckBox.isSelected())
//			options.firstKymo = exp.seqKymos.currentFrame;
//		else 
//			options.firstKymo = 0;
		options.firstkymo 				= parent0.paneKymos.tabDisplay.kymographNamesComboBox.getSelectedIndex();
		options.detectGulpsThreshold 	= (int) detectGulpsThresholdSpinner.getValue();
		options.transformForGulps 		= (TransformOp) transformForGulpsComboBox.getSelectedItem();
		options.detectAllGulps 			= detectAllGulpsCheckBox.isSelected();
		options.spanDiff				= (int) spanTransf2Spinner.getValue();
		options.buildGulps				= detectGulpsCheckBox.isSelected();
		if (!detectGulps)
			options.buildGulps = false;
		options.buildDerivative			= buildDerivativeCheckBox.isSelected();
		options.analyzePartOnly			= partCheckBox.isSelected();
		options.startPixel				= (int) startSpinner.getValue();
		options.endPixel				= (int) endSpinner.getValue();
		options.parent0Rect 			= parent0.mainFrame.getBoundsInternal();
		
		thread.addPropertyChangeListener(this);
		thread.execute();
		detectButton.setText("STOP");
	}

	void setInfos(Capillary cap) {
		DetectGulps_Options options = cap.gulpsOptions;
		detectGulpsThresholdSpinner.setValue(options.detectGulpsThreshold);
		transformForGulpsComboBox.setSelectedItem(options.transformForGulps);
		detectAllGulpsCheckBox.setSelected(options.detectAllGulps);
	}

	private void series_detectGulpsStop() {	
		if (thread != null && !thread.stopFlag) {
			thread.stopFlag = true;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			Experiment exp = parent0.expList.getExperiment(parent0.paneSequence.expListComboBox.getSelectedIndex());
			parent0.paneSequence.openExperiment(exp);
			detectButton.setText(detectString);
		 }
	}
	

}
