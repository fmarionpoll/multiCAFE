package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import icy.util.StringUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeTools.DetectFlies1_series;
import plugins.fmp.multicafeTools.DetectFlies_Options;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;
import plugins.fmp.multicafeTools.OverlayThreshold;



public class MCCages_Detect1 extends JPanel implements ChangeListener, PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6066671006689527651L;

	private MultiCAFE 	parent0					= null;
	private String 		detectString 			= "Detect";
	private JButton 	startComputationButton 	= new JButton(detectString);

	private JComboBox<String> colorChannelComboBox = new JComboBox<String> (new String[] {"Red", "Green", "Blue"});
	private JComboBox<TransformOp> backgroundComboBox = new JComboBox<> (new TransformOp[]  {TransformOp.NONE, TransformOp.REF_PREVIOUS, TransformOp.REF_T0});
	private JSpinner 	thresholdSpinner		= new JSpinner(new SpinnerNumberModel(60, 0, 255, 10));
	private JSpinner 	jitterTextField 		= new JSpinner(new SpinnerNumberModel(5, 0, 255, 1));
	private JCheckBox 	objectLowsizeCheckBox 	= new JCheckBox("object > ");
	private JSpinner 	objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 100000, 1));
	private JCheckBox 	objectUpsizeCheckBox 	= new JCheckBox("object < ");
	private JSpinner 	objectUpsizeSpinner		= new JSpinner(new SpinnerNumberModel(500, 0, 100000, 1));
	private JSpinner 	limitRatioSpinner		= new JSpinner(new SpinnerNumberModel(4, 0, 100, 1));
	
	private JCheckBox 	whiteMiceCheckBox 		= new JCheckBox("white object");
	JCheckBox 			overlayCheckBox 		= new JCheckBox("overlay");
	private JCheckBox 	ALLCheckBox 			= new JCheckBox("ALL series", false);
	
	private OverlayThreshold 	ov 				= null;
	private DetectFlies1_series thread 			= null;
	private int 				currentExp 		= -1;

	// -----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		JPanel dummyPanel1 = new JPanel(flowLayout);
		dummyPanel1.add( GuiUtil.besidesPanel(ALLCheckBox, whiteMiceCheckBox, overlayCheckBox) );
		dummyPanel1.validate();
		add( GuiUtil.besidesPanel( startComputationButton, dummyPanel1));
		
		JPanel dummyPanel2 = new JPanel(flowLayout);
		colorChannelComboBox.setSelectedIndex(1);
		dummyPanel2.add(new JLabel("video channel ", SwingConstants.RIGHT));
		dummyPanel2.add(colorChannelComboBox);
		dummyPanel2.add(new JLabel("bkgnd subtraction ", SwingConstants.RIGHT));
		dummyPanel2.add(backgroundComboBox);
		dummyPanel2.add(new JLabel("detect threshold ", SwingConstants.RIGHT));
		dummyPanel2.add(thresholdSpinner);
		add(dummyPanel2);
		//add( GuiUtil.besidesPanel( videochannel, colorChannelComboBox, backgroundsubtraction, backgroundComboBox));
		
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		JPanel dummyPanel3 = new JPanel(flowLayout);
		dummyPanel3.add(objectLowsizeCheckBox);
		dummyPanel3.add(objectLowsizeSpinner);
		dummyPanel3.add(objectUpsizeCheckBox);
		dummyPanel3.add(objectUpsizeSpinner);
		add( dummyPanel3);
		
		JPanel dummyPanel4 = new JPanel(flowLayout);
		dummyPanel4.add(new JLabel("ratio length/width < ", SwingConstants.RIGHT));
		dummyPanel4.add(limitRatioSpinner);
		JLabel jitterLabel = new JLabel("jitter <= ", SwingConstants.RIGHT);
		dummyPanel4.add(jitterLabel);
		dummyPanel4.add(jitterTextField);
		add(dummyPanel4);
		
		defineActionListeners();
		thresholdSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		overlayCheckBox.addItemListener(new ItemListener() {
		      public void itemStateChanged(ItemEvent e) {
		    	  Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		    	  	if (exp != null) {
			  			if (overlayCheckBox.isSelected()) {
							if (ov == null)
								ov = new OverlayThreshold(exp.seqCamData);
							exp.seqCamData.seq.addOverlay(ov);
							updateOverlay(exp);
						}
						else
							removeOverlay(exp);
		    	  	}
		      }});

		startComputationButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				if (startComputationButton.getText() .equals(detectString))
					startComputation();
				else
					stopComputation();
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
	
	public void updateOverlay (Experiment exp) {
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
		ov.setThresholdSingle(exp.cages.detect.threshold, true);
		ov.painterChanged();	
	}
	
	public void removeOverlay(Experiment exp) {
		if (exp.seqCamData != null && exp.seqCamData.seq != null)
			exp.seqCamData.seq.removeOverlay(ov);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp != null) {
				exp.cages.detect.threshold = (int) thresholdSpinner.getValue();
				updateOverlay(exp);
			}
		}
	}
	
	private boolean initTrackParameters() {
		if (thread == null)
			return false;
		DetectFlies_Options detect = new DetectFlies_Options();
		detect.btrackWhite 		= whiteMiceCheckBox.isSelected();
		detect.blimitLow 		= objectLowsizeCheckBox.isSelected();
		detect.blimitUp 		= objectUpsizeCheckBox.isSelected();
		detect.limitLow 		= (int) objectLowsizeSpinner.getValue();
		detect.limitUp 			= (int) objectUpsizeSpinner.getValue();
		detect.limitRatio		= (int) limitRatioSpinner.getValue();
		detect.jitter 			= (int) jitterTextField.getValue();
		detect.videoChannel 	= colorChannelComboBox.getSelectedIndex();
		detect.transformop		= (TransformOp) backgroundComboBox.getSelectedItem();
		detect.threshold		= (int) thresholdSpinner.getValue();
		detect.parent0Rect 		= parent0.mainFrame.getBoundsInternal();

		detect.stepFrame = parent0.paneSequence.tabIntervals.getStepFrame();
		detect.isFrameFixed = parent0.paneSequence.tabIntervals.getIsFixedFrame();
		detect.startFrame = parent0.paneSequence.tabIntervals.getStartFrame();
		detect.endFrame = parent0.paneSequence.tabIntervals.getEndFrame();
		
		detect.expList = new ExperimentList(); 
		parent0.paneSequence.transferExperimentNamesToExpList(detect.expList, true);		
		if (ALLCheckBox.isSelected()) {
			detect.expList.index0 = 0;
			detect.expList.index1 = detect.expList.getSize()-1;
		} else {
			detect.expList.index0 = parent0.currentExperimentIndex;
			detect.expList.index1 = detect.expList.index0;
		}
		
		thread.stopFlag 	= false;
		thread.detect 		= detect;
		return true;
	}
	
	void startComputation() {
		currentExp = parent0.paneSequence.expListComboBox.getSelectedIndex();
		parent0.currentExperimentIndex = currentExp;
		Experiment exp = parent0.expList.getExperiment(currentExp);
		if (exp == null) 
			return;
		parent0.paneSequence.tabClose.closeExp(exp);
		
		thread = new DetectFlies1_series();		
		parent0.paneSequence.transferExperimentNamesToExpList(parent0.expList, true);	
		initTrackParameters();
		
		thread.buildBackground	= false;
		thread.detectFlies		= true;
		thread.addPropertyChangeListener(this);
		thread.execute();
		startComputationButton.setText("STOP");
	}
	
	private void stopComputation() {	
		if (thread != null && !thread.stopFlag) {
			thread.stopFlag = true;
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

