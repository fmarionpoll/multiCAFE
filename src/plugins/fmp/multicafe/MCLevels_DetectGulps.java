package plugins.fmp.multicafe;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeTools.DetectGulps;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCLevels_DetectGulps extends JPanel {
	/**
	 * 
	 */
	private static final long 	serialVersionUID 				= -5590697762090397890L;
	
	JCheckBox				detectAllGulpsCheckBox 			= new JCheckBox ("all images", true);
	JComboBox<TransformOp> 	transformForGulpsComboBox 		= new JComboBox<TransformOp> (new TransformOp[] {TransformOp.XDIFFN /*, TransformOp.YDIFFN, TransformOp.XYDIFFN	*/});
	JSpinner				startSpinner					= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner				endSpinner						= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	JCheckBox				buildDerivativeCheckBox 		= new JCheckBox ("build derivative", true);
	JCheckBox				detectGulpsCheckBox 			= new JCheckBox ("detect gulps", true);
	
	private JCheckBox		partCheckBox 					= new JCheckBox ("detect from", false);
	private JButton			displayTransform2Button			= new JButton("Display");
	private JSpinner		spanTransf2Spinner				= new JSpinner(new SpinnerNumberModel(3, 0, 500, 1));
	private JSpinner 		detectGulpsThresholdSpinner		= new JSpinner(new SpinnerNumberModel(90, 0, 500, 1));
	private JButton 		detectGulpsButton 				= new JButton("Detect");
	private MultiCAFE 		parent0;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
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
		
		detectAllGulpsCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		detectAllGulpsCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
		add( GuiUtil.besidesPanel(new JLabel(" "), new JLabel(" "),  detectAllGulpsCheckBox, detectGulpsButton));

		transformForGulpsComboBox.setSelectedItem(TransformOp.XDIFFN);
		defineListeners();
	}
	
	private void defineListeners() {

		transformForGulpsComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				kymosDisplayFiltered2();
				kymosDetectGulps(false);
			}});
		
		detectGulpsButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				kymosDisplayFiltered2();
				kymosDetectGulps(true);
			}});
		
		displayTransform2Button.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				kymosDisplayFiltered2();
				parent0.kymosPane.displayTab.viewKymosCheckBox.setSelected(true);
			}});
		
	}

	// get/set
		
	void kymosDisplayFiltered2() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		List<Capillary> capList = seqKymos.capillaries.capillariesArrayList;
		for (int t=0; t < seqKymos.seq.getSizeT(); t++) {
			getInfosFromDialog(capList.get(t));		
		}
		
		TransformOp transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
		parent0.levelsPane.kymosBuildFiltered(0, 2, transform, (int) spanTransf2Spinner.getValue());
	}
	
	void kymosDetectGulps(boolean detectGulps) {
		DetectGulps_Options options 	= new DetectGulps_Options();
		options.detectGulpsThreshold 	= (int) detectGulpsThresholdSpinner.getValue();
		options.transformForGulps 		= (TransformOp) transformForGulpsComboBox.getSelectedItem();
		options.detectAllGulps 			= detectAllGulpsCheckBox.isSelected();
		options.firstkymo 				= parent0.kymosPane.displayTab.kymographNamesComboBox.getSelectedIndex();
		options.buildGulps				= detectGulpsCheckBox.isSelected();
		if (!detectGulps)
			options.buildGulps = false;
		options.buildDerivative			= buildDerivativeCheckBox.isSelected();
		options.analyzePartOnly			= partCheckBox.isSelected();
		options.startPixel				= (int) startSpinner.getValue();
		options.endPixel				= (int) endSpinner.getValue();
		
		DetectGulps detect = new DetectGulps();
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		exp.seqKymos.transferKymosRoisToMeasures();
		detect.detectGulps(options, exp.seqKymos);
	}

	void setInfos(Capillary cap) {
		DetectGulps_Options options = cap.gulpsOptions;
		detectGulpsThresholdSpinner.setValue(options.detectGulpsThreshold);
		transformForGulpsComboBox.setSelectedItem(options.transformForGulps);
		detectAllGulpsCheckBox.setSelected(options.detectAllGulps);
	}
	
	void getInfosFromDialog(Capillary cap) {
		DetectGulps_Options options = cap.gulpsOptions;
		options.detectGulpsThreshold = (int) detectGulpsThresholdSpinner.getValue();
		options.transformForGulps = (TransformOp) transformForGulpsComboBox.getSelectedItem();
		options.detectAllGulps = detectAllGulpsCheckBox.isSelected();
	}
	

}
