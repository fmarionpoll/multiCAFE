package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeTools.DetectGulps;
import plugins.fmp.multicafeTools.DetectGulps_Options;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DLine;

public class MCKymosAnalyzeTab_DetectGulps extends JPanel {
	/**
	 * 
	 */
	private static final long 	serialVersionUID 				= -5590697762090397890L;
	
	JCheckBox				detectAllGulpsCheckBox 			= new JCheckBox ("all images", true);
	JCheckBox				viewGulpsThresholdCheckBox 		= new JCheckBox ("(view) threshold", false);
	JComboBox<TransformOp> 	transformForGulpsComboBox 		= new JComboBox<TransformOp> (new TransformOp[] {TransformOp.XDIFFN /*, TransformOp.YDIFFN, TransformOp.XYDIFFN	*/});
	JSpinner				startSpinner					= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner				endSpinner						= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	
	private JCheckBox		partCheckBox 					= new JCheckBox ("detect from", false);
	private JButton			displayTransform2Button			= new JButton("Display");
	private JSpinner		spanTransf2Spinner				= new JSpinner(new SpinnerNumberModel(3, 0, 500, 1));
	private JSpinner 		detectGulpsThresholdSpinner		= new JSpinner(new SpinnerNumberModel(90, 0, 500, 1));
	private JButton 		detectGulpsButton 				= new JButton("Detect");
	private ROI2DLine		roiThreshold 					= new ROI2DLine ();
	private MultiCAFE 		parent0;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		add( GuiUtil.besidesPanel( viewGulpsThresholdCheckBox, detectGulpsThresholdSpinner, transformForGulpsComboBox, displayTransform2Button));
		
		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		((FlowLayout)panel1.getLayout()).setVgap(0);
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
				roisDisplayAllThresholds(viewGulpsThresholdCheckBox.isSelected());
			}});
		
		detectGulpsButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				kymosDisplayFiltered2();
				kymosDetectGulps(true);
				roisDisplayAllThresholds(viewGulpsThresholdCheckBox.isSelected());
			}});
		
		displayTransform2Button.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				kymosDisplayFiltered2();
				parent0.buildKymosPane.optionsTab.viewKymosCheckBox.setSelected(true);
			}});
		
		viewGulpsThresholdCheckBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				roisDisplayAllThresholds(viewGulpsThresholdCheckBox.isSelected());
			}});
		
		detectGulpsThresholdSpinner.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent arg0) {
				Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
				if (exp.seqKymos != null && viewGulpsThresholdCheckBox.isSelected()) {
					int thresholdValue = (int) detectGulpsThresholdSpinner.getValue();
					roiDisplayThreshold(true, exp.seqKymos, thresholdValue);
					if (detectAllGulpsCheckBox.isSelected())
						roisDisplayAllThresholds(viewGulpsThresholdCheckBox.isSelected());
				}
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
		parent0.kymographsPane.kymosBuildFiltered(0, 2, transform, (int) spanTransf2Spinner.getValue());
	}
	
	void kymosDetectGulps(boolean detectGulps) {
		DetectGulps_Options options 	= new DetectGulps_Options();
		options.detectGulpsThreshold 	= (int) detectGulpsThresholdSpinner.getValue();
		options.transformForGulps 		= (TransformOp) transformForGulpsComboBox.getSelectedItem();
		options.detectAllGulps 			= detectAllGulpsCheckBox.isSelected();
		options.firstkymo 				= parent0.buildKymosPane.optionsTab.kymographNamesComboBox.getSelectedIndex();
		options.computeDiffnAndDetect	= detectGulps;
		options.analyzePartOnly		= partCheckBox.isSelected();
		options.startPixel			= (int) startSpinner.getValue();
		options.endPixel			= (int) endSpinner.getValue();
		
		DetectGulps detect = new DetectGulps();
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
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
	
	void roisDisplayAllThresholds(boolean display) {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		if (exp.seqKymos == null)
			return;
		ThreadUtil.bgRun( new Runnable() { @Override public void run() { 
				final int thresholdValue = (int) detectGulpsThresholdSpinner.getValue();
				roiDisplayThreshold(display, exp.seqKymos, thresholdValue);
			}});
	}
	
	void roiDisplayThreshold(boolean display, SequenceKymos seq, int thresholdValue) {
		if (display)
		{
			if (!seq.seq.contains(roiThreshold)) {
				roiThreshold.setName("derivativeThresh");
				roiThreshold.setColor(Color.ORANGE);
				roiThreshold.setStroke(1);
				roiThreshold.setOpacity((float) 0.2);
				seq.seq.addROI(roiThreshold);
			}
			//int seqheight = seq.seq.getHeight()/2;
			//double value = seqheight - thresholdValue;
			double value = thresholdValue;
			Line2D refLineUpper = new Line2D.Double (0, value, seq.seq.getWidth(), value);
			roiThreshold.setLine(refLineUpper);
		}
		else 
		{
			seq.seq.removeROI(roiThreshold);
		}
	}

}
