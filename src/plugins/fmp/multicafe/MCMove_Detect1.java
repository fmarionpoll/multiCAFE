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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import icy.roi.ROI2D;

import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.XYTaSeries;
import plugins.fmp.multicafeTools.DetectFlies1_series;
import plugins.fmp.multicafeTools.DetectFlies_Options;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;
import plugins.fmp.multicafeTools.OverlayThreshold;



public class MCMove_Detect1 extends JPanel implements ChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6066671006689527651L;

	private MultiCAFE parent0;
	
	private JButton startComputationButton 	= new JButton("Detect / Stop");

	private JComboBox<String> colorChannelComboBox = new JComboBox<String> (new String[] {"Red", "Green", "Blue"});
	private JComboBox<TransformOp> backgroundComboBox = new JComboBox<> (new TransformOp[]  {TransformOp.NONE, TransformOp.REF_PREVIOUS, TransformOp.REF_T0});
	private JSpinner thresholdSpinner		= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JSpinner jitterTextField 		= new JSpinner(new SpinnerNumberModel(5, 0, 255, 1));
	private JCheckBox objectLowsizeCheckBox = new JCheckBox("object > ");
	private JSpinner objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 100000, 1));
	private JCheckBox objectUpsizeCheckBox 	= new JCheckBox("object < ");
	private JSpinner objectUpsizeSpinner	= new JSpinner(new SpinnerNumberModel(500, 0, 100000, 1));
	private JCheckBox whiteMiceCheckBox 	= new JCheckBox("white object");
	private JCheckBox overlayCheckBox = new JCheckBox("overlay");
	private JCheckBox ALLCheckBox = new JCheckBox("ALL series", false);
	
	private OverlayThreshold 	ov 					= null;
	private DetectFlies1_series detectFlies1Thread 	= null;
	private Thread 				thread 				= null;
	private int 				currentExp 			= -1;

	// -----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		JPanel dummyPanel = new JPanel();
		dummyPanel.add( GuiUtil.besidesPanel(whiteMiceCheckBox, overlayCheckBox, ALLCheckBox) );
		
		FlowLayout layout = (FlowLayout) dummyPanel.getLayout();
		layout.setVgap(0);
		dummyPanel.validate();
		add( GuiUtil.besidesPanel( startComputationButton, dummyPanel));
		
		JLabel videochannel = new JLabel("video channel ");
		videochannel.setHorizontalAlignment(SwingConstants.RIGHT);
		colorChannelComboBox.setSelectedIndex(1);
		JLabel backgroundsubtraction = new JLabel("bkgnd subtraction ");
		backgroundsubtraction.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( videochannel, colorChannelComboBox, backgroundsubtraction, backgroundComboBox));
		
		JLabel thresholdLabel = new JLabel("detect threshold ");
		thresholdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( thresholdLabel, thresholdSpinner, objectLowsizeCheckBox, objectLowsizeSpinner));
		
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel jitterlabel = new JLabel("jitter <= ");
		jitterlabel.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( jitterlabel, jitterTextField , objectUpsizeCheckBox, objectUpsizeSpinner) );
		
		defineActionListeners();
		thresholdSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		overlayCheckBox.addItemListener(new ItemListener() {
		      public void itemStateChanged(ItemEvent e) {
	    	  	Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
	  			if (overlayCheckBox.isSelected() && exp != null) {
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
		if (detectFlies1Thread == null)
			return false;
		DetectFlies_Options detect = new DetectFlies_Options();
		detect.btrackWhite 		= whiteMiceCheckBox.isSelected();
		detect.blimitLow 		= objectLowsizeCheckBox.isSelected();
		detect.blimitUp 		= objectUpsizeCheckBox.isSelected();
		detect.limitLow 		= (int) objectLowsizeSpinner.getValue();
		detect.limitUp 			= (int) objectUpsizeSpinner.getValue();
		detect.jitter 			= (int) jitterTextField.getValue();
		detect.videoChannel 	= colorChannelComboBox.getSelectedIndex();
		detect.transformop		= (TransformOp) backgroundComboBox.getSelectedItem();
		detect.threshold		= (int) thresholdSpinner.getValue();
		Experiment exp 			= parent0.expList.getExperiment(parent0.currentExperimentIndex);	
		exp.seqCamData.analysisStep = exp.step;
		exp.seqCamData.analysisStart = exp.startFrame;
		exp.seqCamData.analysisEnd = exp.endFrame;
		detect.seqCamData 		= exp.seqCamData;
		
		detectFlies1Thread.stopFlag 	= false;
		detectFlies1Thread.detect 		= detect;
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
	
	void startComputation() {
		parent0.paneSequence.tabInfos.transferExperimentNamesToExpList(parent0.expList, false);
		if (parent0.currentExperimentIndex >= parent0.expList.experimentList.size())
				parent0.currentExperimentIndex = parent0.expList.experimentList.size()-1;
		currentExp = parent0.currentExperimentIndex;
		Experiment exp = parent0.expList.getExperiment(currentExp);
		parent0.paneSequence.tabClose.closeExp(exp);
		
		detectFlies1Thread = new DetectFlies1_series();		
			
		initTrackParameters();
		cleanPreviousDetections();
		detectFlies1Thread.buildBackground	= false;
		detectFlies1Thread.detectFlies		= true;
		
		thread = new Thread(null, detectFlies1Thread, "+++detect_levels");
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
			detectFlies1Thread.stopFlag = true;
			try {
				thread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		Experiment exp = parent0.expList.getExperiment(currentExp);
		parent0.paneSequence.openExperiment(exp);
	}

	void stopComputation() {
		if (detectFlies1Thread != null)
			detectFlies1Thread.stopFlag = true;
	}

}

