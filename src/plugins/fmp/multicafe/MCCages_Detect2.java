package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.dialog.MessageDialog;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.util.StringUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeTools.DetectFlies2_series;
import plugins.fmp.multicafeTools.DetectFlies_Options;




public class MCCages_Detect2 extends JPanel implements ChangeListener, PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiCAFE parent0;
	
	private String 		detectString 			= "Run..";
	private JButton 	startComputationButton 	= new JButton(detectString);
	private JSpinner 	thresholdDiffSpinner	= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JSpinner 	thresholdBckgSpinner	= new JSpinner(new SpinnerNumberModel(40, 0, 255, 10));
	
	private JSpinner 	jitterTextField 		= new JSpinner(new SpinnerNumberModel(5, 0, 255, 1));
	private JSpinner 	objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 100000, 1));
	private JCheckBox 	objectLowsizeCheckBox 	= new JCheckBox("object >");
	private JCheckBox 	objectUpsizeCheckBox 	= new JCheckBox("object <");
	private JSpinner 	objectUpsizeSpinner		= new JSpinner(new SpinnerNumberModel(500, 0, 100000, 1));
	private JCheckBox 	viewsCheckBox 			= new JCheckBox("view ref img", true);
	private JButton 	loadButton 				= new JButton("Load...");
	private JButton 	saveButton 				= new JButton("Save...");
	private JCheckBox 	ALLCheckBox 			= new JCheckBox("ALL series", false);
	private JCheckBox 	backgroundCheckBox 		= new JCheckBox("(re)build background", false);
	private JCheckBox 	detectCheckBox 			= new JCheckBox("detect flies", true);
	
	private DetectFlies2_series detectFlies2Thread 	= null;
	private int 		currentExp 				= -1;
	
	// ----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		
		JPanel panel = new JPanel(layout);
		panel.add(startComputationButton);
		panel.add(ALLCheckBox);
		panel.add(backgroundCheckBox);
		panel.add(detectCheckBox);
		add( GuiUtil.besidesPanel(panel));
		
		JPanel panel1 = new JPanel(layout);
		panel1.add(loadButton);
		panel1.add(saveButton);
		panel1.add(new JLabel("threshold for bckgnd "));
		panel1.add(thresholdBckgSpinner);
		panel1.add(viewsCheckBox);
		panel1.validate();
		add( GuiUtil.besidesPanel(panel1));
		
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel(new JLabel("threshold ", 
				SwingConstants.RIGHT), 
				thresholdDiffSpinner, 
				objectLowsizeCheckBox, 
				objectLowsizeSpinner));
		
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( new JLabel("jitter <= ", SwingConstants.RIGHT), 
				jitterTextField , 
				objectUpsizeCheckBox, 
				objectUpsizeSpinner) );
		
		defineActionListeners();
		thresholdDiffSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		startComputationButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				if (startComputationButton.getText() .equals(detectString)) 
					startComputation();
				else
					stopComputation();
			}});
		
		saveButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				if (exp != null)
					exp.saveReferenceImage();
			}});
		
		loadButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				if (exp != null) { 
					boolean flag = exp.loadReferenceImage(); 
					if (flag) {
						Viewer v = new Viewer(exp.seqBackgroundImage, true);
						Rectangle rectv = exp.seqCamData.seq.getFirstViewer().getBoundsInternal();
						v.setBounds(rectv);
					} else {
						 MessageDialog.showDialog("Reference file not found on disk",
	                                MessageDialog.ERROR_MESSAGE);
					}
				}
			}});
		
		ALLCheckBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				Color color = Color.BLACK;
				if (ALLCheckBox.isSelected()) 
					color = Color.RED;
				ALLCheckBox.setForeground(color);
				startComputationButton.setForeground(color);
		}});
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdDiffSpinner) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp != null)
				exp.cages.detect.threshold = (int) thresholdDiffSpinner.getValue();
		}
	}
	
	private boolean initTrackParameters() {
		if (detectFlies2Thread == null)
			return false;
		DetectFlies_Options detect = new DetectFlies_Options();
		detect.btrackWhite 		= true;
		detect.blimitLow 		= objectLowsizeCheckBox.isSelected();
		detect.blimitUp 		= objectUpsizeCheckBox.isSelected();
		detect.limitLow 		= (int) objectLowsizeSpinner.getValue();
		detect.limitUp 			= (int) objectUpsizeSpinner.getValue();
		detect.jitter 			= (int) jitterTextField.getValue();
		detect.thresholdDiff	= (int) thresholdDiffSpinner.getValue();
		detect.thresholdBckgnd	= (int) thresholdBckgSpinner.getValue();
		detect.parent0Rect 		= parent0.mainFrame.getBoundsInternal();
		
		detect.forceBuildBackground	= backgroundCheckBox.isSelected();
		detect.detectFlies	= detectCheckBox.isSelected();
		detect.stepFrame 	= parent0.paneSequence.tabIntervals.getStepFrame();
		detect.isFrameFixed = parent0.paneSequence.tabIntervals.getIsFixedFrame();
		detect.startFrame 	= parent0.paneSequence.tabIntervals.getStartFrame();
		detect.endFrame 	= parent0.paneSequence.tabIntervals.getEndFrame();

		detect.expList = new ExperimentList(); 
		parent0.paneSequence.transferExperimentNamesToExpList(detect.expList, true);		
		if (ALLCheckBox.isSelected()) {
			detect.expList.index0 = 0;
			detect.expList.index1 = detect.expList.getSize()-1;
		} else {
			detect.expList.index0 = parent0.currentExperimentIndex;
			detect.expList.index1 = detect.expList.index0;
		}
		
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp != null) 
			parent0.paneSequence.tabIntervals.getAnalyzeFrameFromDialog(exp);
		detect.initParametersForDetection(exp);
		
		detectFlies2Thread.stopFlag = false;
		detectFlies2Thread.detect 	= detect;
		detectFlies2Thread.viewInternalImages = viewsCheckBox.isSelected();
		return true;
	}
	
	void startComputation() {
		parent0.currentExperimentIndex = parent0.paneSequence.expListComboBox.getSelectedIndex();
		currentExp = parent0.currentExperimentIndex;
		Experiment exp = parent0.expList.getExperiment(currentExp);
		if (exp == null)
			return;
		parent0.paneCages.tabFile.saveCagesAndMeasures(exp);
		parent0.paneSequence.tabClose.closeExp(exp);
		
		detectFlies2Thread = new DetectFlies2_series();		
		initTrackParameters();
		detectFlies2Thread.addPropertyChangeListener(this);
		detectFlies2Thread.execute();
		startComputationButton.setText("STOP");
	}
	
	private void stopComputation() {	
		if (detectFlies2Thread != null && !detectFlies2Thread.stopFlag) {
			detectFlies2Thread.stopFlag = true;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			Experiment exp = parent0.expList.getExperiment(currentExp);
			if (exp != null)
				parent0.paneSequence.openExperiment(exp);
			startComputationButton.setText(detectString);
		 }
	}
	

}
