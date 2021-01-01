package plugins.fmp.multicafe.dlg.cages;

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

import icy.util.StringUtil;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.sequence.SequenceCamData;
import plugins.fmp.multicafe.series.Options_BuildSeries;
import plugins.fmp.multicafe.series.DetectFlies1_series;
import plugins.fmp.multicafe.tools.OverlayThreshold;
import plugins.fmp.multicafe.tools.ImageTransformTools.TransformOp;



public class Detect1 extends JPanel implements ChangeListener, PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6066671006689527651L;

	private MultiCAFE 	parent0					= null;
	private String 		detectString 			= "Detect...";
	private JButton 	startComputationButton 	= new JButton(detectString);

	private JComboBox<String> colorChannelComboBox = new JComboBox<String> (new String[] {"Red", "Green", "Blue"});
	private JComboBox<TransformOp> backgroundComboBox = new JComboBox<> (new TransformOp[]  {TransformOp.NONE, TransformOp.REF_PREVIOUS, TransformOp.REF_T0});
	private JSpinner 	thresholdSpinner		= new JSpinner(new SpinnerNumberModel(60, 0, 255, 10));
	private JSpinner 	jitterTextField 		= new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
	private JSpinner 	objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 9999, 1));
	private JSpinner 	objectUpsizeSpinner		= new JSpinner(new SpinnerNumberModel(500, 0, 9999, 1));
	private JCheckBox 	objectLowsizeCheckBox 	= new JCheckBox("object > ");
	private JCheckBox 	objectUpsizeCheckBox 	= new JCheckBox("object < ");
	private JSpinner 	limitRatioSpinner		= new JSpinner(new SpinnerNumberModel(4, 0, 1000, 1));
	
	private JCheckBox 	whiteMiceCheckBox 		= new JCheckBox("white object");
	JCheckBox 			overlayCheckBox 		= new JCheckBox("overlay");
	private JCheckBox 	allCheckBox 			= new JCheckBox("ALL (current to last)", false);
	
	private OverlayThreshold 	ov 				= null;
	private DetectFlies1_series thread 			= null;
	private int 				currentExp 		= -1;

	// -----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(startComputationButton);
		panel1.add(allCheckBox);
		panel1.validate();
		add(panel1);
		
		JPanel panel2 = new JPanel(flowLayout);
		colorChannelComboBox.setSelectedIndex(1);
		panel2.add(new JLabel("video channel ", SwingConstants.RIGHT));
		panel2.add(colorChannelComboBox);
		panel2.add(new JLabel("bkgnd subtraction ", SwingConstants.RIGHT));
		panel2.add(backgroundComboBox);
		panel2.add(new JLabel("threshold ", SwingConstants.RIGHT));
		panel2.add(thresholdSpinner);
		add(panel2);
		
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		JPanel panel3 = new JPanel(flowLayout);
		panel3.add(objectLowsizeCheckBox);
		panel3.add(objectLowsizeSpinner);
		panel3.add(objectUpsizeCheckBox);
		panel3.add(objectUpsizeSpinner);
		panel3.add(whiteMiceCheckBox);
		add( panel3);
		
		JPanel panel4 = new JPanel(flowLayout);
		panel4.add(new JLabel("length/width<", SwingConstants.RIGHT));
		panel4.add(limitRatioSpinner);
		panel4.add(new JLabel("         jitter <= ", SwingConstants.RIGHT));
		panel4.add(jitterTextField);
		panel4.add(overlayCheckBox);
		add(panel4);
		
		defineActionListeners();
		thresholdSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		overlayCheckBox.addItemListener(new ItemListener() {
		      public void itemStateChanged(ItemEvent e) {
		    	  Experiment exp = parent0.expList.getCurrentExperiment();
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
		
		allCheckBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				Color color = Color.BLACK;
				if (allCheckBox.isSelected()) 
					color = Color.RED;
				allCheckBox.setForeground(color);
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
		ov.setThresholdSingle(exp.cages.detect_threshold, true);
		ov.painterChanged();	
	}
	
	public void removeOverlay(Experiment exp) {
		if (exp.seqCamData != null && exp.seqCamData.seq != null)
			exp.seqCamData.seq.removeOverlay(ov);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (exp != null) {
				exp.cages.detect_threshold = (int) thresholdSpinner.getValue();
				updateOverlay(exp);
			}
		}
	}
	
	private boolean initTrackParameters() {
		if (thread == null)
			return false;
		thread.options = new Options_BuildSeries();
		Options_BuildSeries options = thread.options;
		options.btrackWhite 	= whiteMiceCheckBox.isSelected();
		options.blimitLow 		= objectLowsizeCheckBox.isSelected();
		options.blimitUp 		= objectUpsizeCheckBox.isSelected();
		options.limitLow 		= (int) objectLowsizeSpinner.getValue();
		options.limitUp 		= (int) objectUpsizeSpinner.getValue();
		options.limitRatio		= (int) limitRatioSpinner.getValue();
		options.jitter 			= (int) jitterTextField.getValue();
		options.videoChannel 	= colorChannelComboBox.getSelectedIndex();
		options.transformop		= (TransformOp) backgroundComboBox.getSelectedItem();
		options.threshold		= (int) thresholdSpinner.getValue();
		
		options.isFrameFixed 	= parent0.paneSequence.tabAnalyze.getIsFixedFrame();
		options.t_firstMs 		= parent0.paneSequence.tabAnalyze.getStartMs();
		options.t_lastMs 			= parent0.paneSequence.tabAnalyze.getEndMs();
		options.t_binMs			= parent0.paneSequence.tabAnalyze.getBinMs();

		options.parent0Rect 		= parent0.mainFrame.getBoundsInternal();
		options.resultsSubPath = (String) parent0.paneKymos.tabDisplay.availableResultsCombo.getSelectedItem() ;
		options.expList = new ExperimentList(); 
		parent0.paneSequence.transferExperimentNamesToExpList(options.expList, true);		
		options.expList.index0 = parent0.expList.currentExperimentIndex;
		if (allCheckBox.isSelected())
			options.expList.index1 = options.expList.getSize()-1;
		else
			options.expList.index1 = options.expList.index0;
		
		thread.stopFlag 	= false;
		return true;
	}
	
	void startComputation() {
		currentExp = parent0.paneSequence.expListComboBox.getSelectedIndex();
		
		Experiment exp = parent0.expList.getExperiment(currentExp);
		if (exp == null) 
			return;
		parent0.expList.currentExperimentIndex = currentExp;
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

