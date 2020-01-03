package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

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
import icy.roi.ROI2D;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.XYTaSeries;
import plugins.fmp.multicafeTools.DetectFlies2_series;
import plugins.fmp.multicafeTools.DetectFlies_Options;
import plugins.fmp.multicafeTools.OverlayThreshold;



public class MCMove_Detect2 extends JPanel implements ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiCAFE parent0;
	
	private JButton 	buildBackgroundButton 	= new JButton("Build ref. / Stop");
	private JButton 	startComputationButton 	= new JButton("    Detect flies / Stop    ");
	private JSpinner 	thresholdSpinner		= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JSpinner 	jitterTextField 		= new JSpinner(new SpinnerNumberModel(5, 0, 255, 1));
	private JCheckBox 	objectLowsizeCheckBox 	= new JCheckBox("object >");
	private JSpinner 	objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 100000, 1));
	private JCheckBox 	objectUpsizeCheckBox 	= new JCheckBox("object <");
	private JSpinner 	objectUpsizeSpinner		= new JSpinner(new SpinnerNumberModel(500, 0, 100000, 1));
	public 	JCheckBox 	imageOverlayCheckBox	= new JCheckBox("overlay", true);
	private JCheckBox 	viewsCheckBox 			= new JCheckBox("view ref img", true);
	private JButton 	loadButton 				= new JButton("Load...");
	private JButton 	saveButton 				= new JButton("Save...");
	private JCheckBox 	ALLCheckBox 			= new JCheckBox("ALL series", false);
	
	private OverlayThreshold ov 				= null;
	private DetectFlies2_series 	detectFlies2Thread 	= null;
	private int 			currentExp 			= -1;
	private Thread 			thread 				= null;
	

	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		add( GuiUtil.besidesPanel(startComputationButton,  ALLCheckBox));
		
		JPanel panel1 = new JPanel();
		JPanel panel0 = new JPanel();
		panel0.setLayout(new FlowLayout());
		panel0.add(loadButton);
		panel0.add(saveButton);
		FlowLayout layout1 = (FlowLayout) panel0.getLayout();
		layout1.setVgap(0);
		panel0.validate();
		panel1.add( buildBackgroundButton);
		panel1.add(panel0);
		FlowLayout layout11 = (FlowLayout) panel1.getLayout();
		layout11.setVgap(0);
		panel1.validate();
		JPanel panel2 = new JPanel();
		panel2.add(viewsCheckBox);
		panel2.add(imageOverlayCheckBox);
		FlowLayout layout2 = (FlowLayout) panel2.getLayout();
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
		imageOverlayCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		    	if (imageOverlayCheckBox.isSelected() && exp != null) {
		    		if (ov == null)
		    			ov = new OverlayThreshold(exp.seqCamData);
						exp.seqCamData.seq.addOverlay(ov);
						updateOverlay();
					}
					else
						removeOverlay();
		    }});

		startComputationButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				startComputation();
			}});
		
		buildBackgroundButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				builBackgroundImage();
			}});
		
		saveButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				exp.saveReferenceImage();
			}});
		
		loadButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
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
	
	public void updateOverlay () {
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData == null)
			return;
		if (ov == null) 
			ov = new OverlayThreshold(seqCamData);
		else {
			seqCamData.seq.removeOverlay(ov);
			ov.setSequence(seqCamData);
		}
		seqCamData.seq.addOverlay(ov);	
		ov.setThresholdSingle(seqCamData.cages.detect.threshold);
		ov.painterChanged();
	}
	
	public void removeOverlay() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp != null && exp.seqCamData != null && exp.seqCamData.seq != null)
			exp.seqCamData.seq.removeOverlay(ov);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			exp.seqCamData.cages.detect.threshold = (int) thresholdSpinner.getValue();
			updateOverlay();
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
		exp.seqCamData.analysisStep = exp.step;
		exp.seqCamData.analysisStart = exp.startFrame;
		exp.seqCamData.analysisEnd = exp.endFrame;
		detect.expList = parent0.expList; 
		detect.expList.index0 = parent0.currentExperimentIndex;
		detect.expList.index1 = detect.expList.index0;
		if (ALLCheckBox.isSelected()) {
			detect.expList.index0 = 0;
			detect.expList.index1 = parent0.expList.experimentList.size()-1;
		}
		detect.seqCamData 		= exp.seqCamData;	
		detect.initParametersForDetection();
		
		if (detectFlies2Thread == null)
			detectFlies2Thread = new DetectFlies2_series();
		detectFlies2Thread.stopFlag 	= false;
		detectFlies2Thread.detect 		= detect;
		detectFlies2Thread.viewInternalImages = viewsCheckBox.isSelected();
		return true;
	}
	
	private void cleanPreviousDetections() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		for (Cage cage: seqCamData.cages.cageList) {
			cage.flyPositions = new XYTaSeries();
		}
		ArrayList<ROI2D> list = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (roi.getName().contains("det")) {
				seqCamData.seq.removeROI(roi);
			}
		}
	}

	void builBackgroundImage() {
		if (detectFlies2Thread == null)
			detectFlies2Thread = new DetectFlies2_series();
		if (detectFlies2Thread.threadRunning) {
			series_detectFliesStop();
			return;
		}
		initTrackParameters();
		detectFlies2Thread.buildBackground	= true;
		detectFlies2Thread.detectFlies		= false;
		ThreadUtil.bgRun(detectFlies2Thread);
	}
	
	void startComputation() {
		parent0.paneSequence.tabInfos.transferExperimentNamesToExpList(parent0.expList, false);
		if (parent0.currentExperimentIndex >= parent0.expList.experimentList.size())
			parent0.currentExperimentIndex = parent0.expList.experimentList.size()-1;
		currentExp = parent0.currentExperimentIndex;
		Experiment exp = parent0.expList.getExperiment(currentExp);
		parent0.paneSequence.tabClose.closeExp(exp);
		
		detectFlies2Thread = new DetectFlies2_series();		
		initTrackParameters();
		cleanPreviousDetections();
		detectFlies2Thread.buildBackground	= false;
		detectFlies2Thread.detectFlies		= true;
		
		thread = new Thread(null, detectFlies2Thread, "+++detect_levels");
		thread.start();
		Thread waitcompletionThread = new Thread(null, new Runnable() {
			public void run() {
				try { 
					thread.join();
					}
				catch(Exception e){;} 
				finally { 
					series_detectFliesStop();
				}
			}}, "+++waitforcompletion");
		waitcompletionThread.start();
	}
	
	private void series_detectFliesStop() {	
		if (thread != null && thread.isAlive()) {
			detectFlies2Thread.stopFlag = true;
			try {
				thread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		Experiment exp = parent0.expList.getExperiment(currentExp);
		parent0.paneSequence.openExperiment(exp);
	}
	



}
