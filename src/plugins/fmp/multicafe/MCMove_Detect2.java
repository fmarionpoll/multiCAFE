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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import icy.system.thread.ThreadUtil;
import icy.util.StringUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeTools.DetectFlies2_series;
import plugins.fmp.multicafeTools.DetectFlies_Options;




public class MCMove_Detect2 extends JPanel implements ChangeListener, PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiCAFE parent0;
	
	private JButton 	buildBackgroundButton 	= new JButton("Build ref. / Stop");
	private String 		detectString 			= "Detect";
	private JButton 	startComputationButton 	= new JButton(detectString);
	private JSpinner 	thresholdSpinner		= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JSpinner 	thresholdBckgSpinner	= new JSpinner(new SpinnerNumberModel(40, 0, 255, 10));
	
	private JSpinner 	jitterTextField 		= new JSpinner(new SpinnerNumberModel(5, 0, 255, 1));
	private JCheckBox 	objectLowsizeCheckBox 	= new JCheckBox("object >");
	private JSpinner 	objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 100000, 1));
	private JCheckBox 	objectUpsizeCheckBox 	= new JCheckBox("object <");
	private JSpinner 	objectUpsizeSpinner		= new JSpinner(new SpinnerNumberModel(500, 0, 100000, 1));
	private JCheckBox 	viewsCheckBox 			= new JCheckBox("view ref img", true);
	private JButton 	loadButton 				= new JButton("Load...");
	private JButton 	saveButton 				= new JButton("Save...");
	private JCheckBox 	ALLCheckBox 			= new JCheckBox("ALL series", false);
	
	private DetectFlies2_series detectFlies2Thread 	= null;
	private int 				currentExp 			= -1;
	

	// ----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		add( GuiUtil.besidesPanel(startComputationButton,  ALLCheckBox));
		
		JPanel panel1 = new JPanel();
		FlowLayout layout1 = (FlowLayout) panel1.getLayout();
		layout1.setVgap(0);
		panel1.add( buildBackgroundButton);
		
		JPanel panel0 = new JPanel();
		panel0.setLayout(new FlowLayout());
		panel0.add(loadButton);
		panel0.add(saveButton);
		FlowLayout layout0 = (FlowLayout) panel0.getLayout();
		layout0.setVgap(0);
		panel0.validate();
		panel1.add(panel0);
		
		panel1.validate();
		
		JPanel panel2 = new JPanel();
		FlowLayout layout2 = (FlowLayout) panel2.getLayout();
		panel2.setLayout(layout2);
		panel2.add(new JLabel("threshold for bckgnd "));
		panel2.add(thresholdBckgSpinner);
		panel2.add(viewsCheckBox);
		layout2.setVgap(0);
		panel2.validate();
		
		add( GuiUtil.besidesPanel(panel1,  panel2 ));
		
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel(new JLabel("threshold ", 
				SwingConstants.RIGHT), 
				thresholdSpinner, 
				objectLowsizeCheckBox, 
				objectLowsizeSpinner));
		
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( new JLabel("jitter <= ", SwingConstants.RIGHT), 
				jitterTextField , 
				objectUpsizeCheckBox, 
				objectUpsizeSpinner) );
		
		defineActionListeners();
		thresholdSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		startComputationButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				if (startComputationButton.getText() .equals(detectString))
					startComputation();
				else
					stopComputation();
			}});
		
		buildBackgroundButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				builBackgroundImage();
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
				if (exp != null)
					exp.loadReferenceImage();
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
		if (e.getSource() == thresholdSpinner) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp != null)
				exp.cages.detect.threshold = (int) thresholdSpinner.getValue();
		}
	}
	
	private boolean initTrackParameters() {
		DetectFlies_Options detect = new DetectFlies_Options();
		detect.btrackWhite 		= true;
		detect.blimitLow 		= objectLowsizeCheckBox.isSelected();
		detect.blimitUp 		= objectUpsizeCheckBox.isSelected();
		detect.limitLow 		= (int) objectLowsizeSpinner.getValue();
		detect.limitUp 			= (int) objectUpsizeSpinner.getValue();
		detect.jitter 			= (int) jitterTextField.getValue();
		detect.threshold		= (int) thresholdSpinner.getValue();
		detect.parent0Rect 		= parent0.mainFrame.getBoundsInternal();
		Experiment exp 			= parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp != null) {
		parent0.paneSequence.tabIntervals.getAnalyzeFrameFromDialog(exp);
			detect.stepFrame = exp.stepFrame;
			detect.startFrame = exp.startFrame;
			detect.endFrame = exp.endFrame;
		}
		detect.expList = parent0.expList; 
		detect.expList.index0 = parent0.currentExperimentIndex;
		detect.expList.index1 = detect.expList.index0;
		if (ALLCheckBox.isSelected()) {
			detect.expList.index0 = 0;
			detect.expList.index1 = parent0.expList.experimentList.size()-1;
		}
		detect.seqCamData = exp.seqCamData;	
		detect.initParametersForDetection(exp);
		
		if (detectFlies2Thread == null)
			detectFlies2Thread = new DetectFlies2_series();
		detectFlies2Thread.stopFlag 	= false;
		detectFlies2Thread.detect 		= detect;
		detectFlies2Thread.viewInternalImages = viewsCheckBox.isSelected();
		return true;
	}
	
	void builBackgroundImage() {
		if (detectFlies2Thread == null)
			detectFlies2Thread = new DetectFlies2_series();
		if (detectFlies2Thread.threadRunning) {
			stopComputation();
			return;
		}
		initTrackParameters();
		detectFlies2Thread.buildBackground	= true;
		detectFlies2Thread.detectFlies		= false;
		ThreadUtil.bgRun(detectFlies2Thread);
	}
	
	void startComputation() {
		parent0.paneSequence.transferExperimentNamesToExpList(parent0.expList, true);
		if (parent0.currentExperimentIndex >= parent0.expList.experimentList.size())
			parent0.currentExperimentIndex = parent0.expList.experimentList.size()-1;
		currentExp = parent0.currentExperimentIndex;
		Experiment exp = parent0.expList.getExperiment(currentExp);
		if (exp != null)
			parent0.paneSequence.tabClose.closeExp(exp);
		
		detectFlies2Thread = new DetectFlies2_series();		
		initTrackParameters();
		detectFlies2Thread.buildBackground	= false;
		detectFlies2Thread.detectFlies		= true;
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
