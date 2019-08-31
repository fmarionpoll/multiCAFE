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
import plugins.fmp.multicafeTools.DetectLimits;
import plugins.fmp.multicafeTools.DetectLimits_Options;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCKymosAnalyzeTab_DetectLimits  extends JPanel {

	/**
	 * 
	 */
	JSpinner			startSpinner			= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner			endSpinner				= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	JComboBox<TransformOp> transformForLevelsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.RGB,
			TransformOp.GBMINUS_2R, TransformOp.RBMINUS_2G, TransformOp.RGMINUS_2B, 
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});

	private static final long serialVersionUID = -6329863521455897561L;
	private JComboBox<String> 	directionComboBox= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JCheckBox	detectAllCheckBox 		= new JCheckBox ("all images", true);
	private JSpinner 	thresholdSpinner 		= new JSpinner(new SpinnerNumberModel(35, 1, 255, 1));
	private JButton		displayTransform1Button	= new JButton("Display");
	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private JButton 	detectButton 			= new JButton("Detect");
	private MultiCAFE 	parent0 				= null;
	private JCheckBox	partCheckBox 			= new JCheckBox ("detect from", false);
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		add( GuiUtil.besidesPanel(directionComboBox, thresholdSpinner, transformForLevelsComboBox, displayTransform1Button ));
		
		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		((FlowLayout)panel1.getLayout()).setVgap(0);	
		panel1.add(partCheckBox);
		panel1.add(startSpinner);
		panel1.add(new JLabel("to"));
		panel1.add(endSpinner);
		add( panel1);
		
		detectAllCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		detectAllCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
		add( GuiUtil.besidesPanel(new JLabel(" "), new JLabel(" "), detectAllCheckBox, detectButton));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		transformForLevelsComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
				if (exp != null && exp.seqCamData != null) {
					kymosDisplayFiltered1();
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		
		detectButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				kymosDisplayFiltered1();
				
				Experiment exp 				= parent0.expList.getExperiment(parent0.currentIndex);
				
				DetectLimits_Options options= new DetectLimits_Options();
				options.transformForLevels 	= (TransformOp) transformForLevelsComboBox.getSelectedItem();
				options.directionUp 		= (directionComboBox.getSelectedIndex() == 0);
				options.detectLevelThreshold= (int) getDetectLevelThreshold();
				options.detectAllImages 	= detectAllCheckBox.isSelected();
				options.firstImage 			= parent0.buildKymosPane.optionsTab.kymographNamesComboBox.getSelectedIndex();
				options.analyzePartOnly		= partCheckBox.isSelected();
				options.startPixel			= (int) startSpinner.getValue();
				options.endPixel			= (int) endSpinner.getValue();
				
				DetectLimits detect 		= new DetectLimits();
				detect.detectCapillaryLevels(options, exp.seqKymos);
				firePropertyChange("KYMO_DETECT_TOP", false, true);
			}});	
		
		displayTransform1Button.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				kymosDisplayFiltered1();
				firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
			}});
	}
	
	// -------------------------------------------------
	
	int getDetectLevelThreshold() {
		return (int) thresholdSpinner.getValue();
	}

	void setDetectLevelThreshold (int threshold) {
		thresholdSpinner.setValue(threshold);
	}
	
	int getSpanDiffTop() {
		return (int) spanTopSpinner.getValue() ;
	}
		
	void kymosDisplayFiltered1() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		if (exp.seqKymos == null)
			return;
		TransformOp transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		List<Capillary> capList = exp.seqKymos.capillaries.capillariesArrayList;
		for (int t=0; t < exp.seqKymos.seq.getSizeT(); t++) {
			getInfosFromDialog(capList.get(t));		
		}
		parent0.kymographsPane.kymosBuildFiltered(0, 1, transform, getSpanDiffTop());
	}
	
	void setInfosToDialog(Capillary cap) {

		DetectLimits_Options options = cap.limitsOptions;
		transformForLevelsComboBox.setSelectedItem(options.transformForLevels);
		int index =options.directionUp ? 0:1;
		directionComboBox.setSelectedIndex(index);
		setDetectLevelThreshold(options.detectLevelThreshold);
		thresholdSpinner.setValue(options.detectLevelThreshold);
		detectAllCheckBox.setSelected(options.detectAllImages);
	}
	
	void getInfosFromDialog(Capillary cap) {

		DetectLimits_Options options = cap.limitsOptions;
		options.transformForLevels = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		options.directionUp = (directionComboBox.getSelectedIndex() == 0) ;
		options.detectLevelThreshold = getDetectLevelThreshold();
		options.detectAllImages = detectAllCheckBox.isSelected();
	}
}
