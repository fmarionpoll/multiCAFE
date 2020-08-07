package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.gui.util.GuiUtil;
import icy.util.StringUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeTools.DetectLevels_Options;
import plugins.fmp.multicafeTools.DetectLevels_series;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCLevels_DetectLimits extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID 	= -6329863521455897561L;
	JSpinner			startSpinner			= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner			endSpinner				= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	JComboBox<TransformOp> transformForLevelsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.RGB,
			TransformOp.GBMINUS_2R, TransformOp.RBMINUS_2G, TransformOp.RGMINUS_2B, TransformOp.RGB_DIFFS,
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});

	private JComboBox<String> directionComboBox	= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JCheckBox	allKymosCheckBox 		= new JCheckBox ("all kymographs", true);
	private JSpinner 	thresholdSpinner 		= new JSpinner(new SpinnerNumberModel(35, 1, 255, 1));
	private JButton		displayTransform1Button	= new JButton("Display");
	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private String 		detectString 			= "        Detect     ";
	private JButton 	detectButton 			= new JButton(detectString);
	private JCheckBox	partCheckBox 			= new JCheckBox (" from", false);
	private JCheckBox	allSeriesCheckBox 		= new JCheckBox("ALL series", false);
	private JCheckBox	leftCheckBox 			= new JCheckBox ("L", true);
	private JCheckBox	rightCheckBox 			= new JCheckBox ("R", true);

	private MultiCAFE 	parent0 				= null;
	private DetectLevels_series thread 			= null;

	// -----------------------------------------------------
		
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		JPanel panel0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		((FlowLayout)panel0.getLayout()).setVgap(0);
		panel0.add( detectButton);
		panel0.add( allSeriesCheckBox);
		panel0.add(allKymosCheckBox);
		panel0.add(leftCheckBox);
		panel0.add(rightCheckBox);
		add( GuiUtil.besidesPanel(panel0 ));
		
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		add( GuiUtil.besidesPanel(directionComboBox, thresholdSpinner, transformForLevelsComboBox, displayTransform1Button ));
		
		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		((FlowLayout)panel1.getLayout()).setVgap(0);	
		panel1.add(partCheckBox);
		panel1.add(startSpinner);
		panel1.add(new JLabel("to"));
		panel1.add(endSpinner);
		add( panel1);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		
		transformForLevelsComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null && exp.seqCamData != null) {
					kymosDisplayFiltered1(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		
		detectButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				if (detectButton.getText() .equals(detectString))
					series_detectLimitsStart();
				else 
					series_detectLimitsStop();
			}});	
		
		displayTransform1Button.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null) { 
					kymosDisplayFiltered1(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
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
		
	void kymosDisplayFiltered1(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		TransformOp transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		List<Capillary> capList = exp.capillaries.capillariesArrayList;
		for (int t=0; t < exp.seqKymos.seq.getSizeT(); t++) {
			getInfosFromDialog(capList.get(t));		
		}
		int zChannelDestination = 1;
		exp.kymosBuildFiltered(0, zChannelDestination, transform, getSpanDiffTop());
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	void setInfosToDialog(Capillary cap) {
		DetectLevels_Options options = cap.limitsOptions;
		transformForLevelsComboBox.setSelectedItem(options.transformForLevels);
		int index =options.directionUp ? 0:1;
		directionComboBox.setSelectedIndex(index);
		setDetectLevelThreshold(options.detectLevelThreshold);
		thresholdSpinner.setValue(options.detectLevelThreshold);
		allKymosCheckBox.setSelected(options.detectAllKymos);
		leftCheckBox.setSelected(options.detectL);
		rightCheckBox.setSelected(options.detectR);
	}
	
	void getInfosFromDialog(Capillary cap) {
		DetectLevels_Options options = cap.limitsOptions;
		options.transformForLevels = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		options.directionUp = (directionComboBox.getSelectedIndex() == 0) ;
		options.detectLevelThreshold = getDetectLevelThreshold();
		options.detectAllKymos = allKymosCheckBox.isSelected();
		options.detectL = leftCheckBox.isSelected();
		options.detectR = rightCheckBox.isSelected();
	}
	
	private DetectLevels_Options initBuildParameters(Experiment exp) {	
		parent0.paneSequence.tabIntervals.getAnalyzeFrameFromDialog(exp);
		DetectLevels_Options options= new DetectLevels_Options();
		options.expList = new ExperimentList(); 
		parent0.paneSequence.transferExperimentNamesToExpList(options.expList, true);		
		if (allSeriesCheckBox.isSelected()) {
			options.expList.index0 = 0;
			options.expList.index1 = options.expList.getSize()-1;
		} else {
			options.expList.index0 = parent0.expList.currentExperimentIndex;
			options.expList.index1 = parent0.expList.currentExperimentIndex;
		}
		if (!allKymosCheckBox.isSelected())
			options.firstKymo = exp.seqKymos.currentFrame;
		else 
			options.firstKymo = 0;
		
		options.transformForLevels 	= (TransformOp) transformForLevelsComboBox.getSelectedItem();
		options.directionUp 		= (directionComboBox.getSelectedIndex() == 0);
		options.detectLevelThreshold= (int) getDetectLevelThreshold();
		options.detectAllKymos 		= allKymosCheckBox.isSelected();
	
		options.analyzePartOnly		= partCheckBox.isSelected();
		options.startPixel			= (int) startSpinner.getValue() / exp.getKymoFrameStep();
		options.endPixel			= (int) endSpinner.getValue() / exp.getKymoFrameStep();
		options.spanDiffTop			= getSpanDiffTop();
		options.detectL 			= leftCheckBox.isSelected();
		options.detectR				= rightCheckBox.isSelected();
		options.parent0Rect 		= parent0.mainFrame.getBoundsInternal();
		options.resultsSubPath 		= (String) parent0.paneKymos.tabDisplay.availableResultsCombo.getSelectedItem() ;
		return options;
	}
	
	void series_detectLimitsStart() {
		int current = parent0.paneSequence.expListComboBox.getSelectedIndex();
		Experiment exp = parent0.expList.getExperiment(current);
		if (exp == null)
			return;
		parent0.expList.currentExperimentIndex = current;
		exp.saveExperimentMeasures(exp.getResultsDirectory());
		parent0.paneSequence.tabClose.closeExp(exp);
		thread = new DetectLevels_series();
		thread.options = initBuildParameters(exp);
		
		thread.addPropertyChangeListener(this);
		thread.execute();
		detectButton.setText("STOP");
	}

	private void series_detectLimitsStop() {	
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
//			if (!allKymosCheckBox.isSelected())
//				parent0.paneKymos.tabDisplay.selectKymograph(indexCurrentKymo);
		 }
	}
}
