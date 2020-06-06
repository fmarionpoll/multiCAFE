package plugins.fmp.multicafe;

import java.awt.Color;
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

import icy.gui.util.GuiUtil;
import icy.util.StringUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeTools.BuildKymographs_series;
import plugins.fmp.multicafeTools.BuildKymographs_Options;
import plugins.fmp.multicafeTools.EnumStatusComputation;


public class MCKymos_Create extends JPanel implements PropertyChangeListener { 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	private String 			detectString 				= "Start";
	
	JButton 				startComputationButton 	= new JButton("Start");

	JSpinner 				diskRadiusSpinner 			= new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
	JCheckBox 				doRegistrationCheckBox 		= new JCheckBox("registration", false);
	JCheckBox				updateViewerCheckBox 		= new JCheckBox("update viewer", true);
	JCheckBox				ALLCheckBox 				= new JCheckBox("ALL series", false);
	
	EnumStatusComputation 	sComputation 				= EnumStatusComputation.START_COMPUTATION; 
	private MultiCAFE 		parent0						= null;
	private BuildKymographs_series thread 				= null;

	// -----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(
				startComputationButton, ALLCheckBox));
		add(GuiUtil.besidesPanel(
				new JLabel("area around ROIs", SwingConstants.RIGHT), 
				diskRadiusSpinner, 
				updateViewerCheckBox, 
				doRegistrationCheckBox
				));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
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
	
	public void setBuildKymosParametersToDialog (Experiment exp) {
		parent0.paneSequence.tabIntervals.setAnalyzeFrameToDialog (exp);
	}
	
	private boolean initBuildParameters(Experiment exp) {
		if (thread == null)
			return false;
		
		BuildKymographs_Options options = thread.options;
		options.expList = parent0.expList; 
		if (ALLCheckBox.isSelected()) {
			options.expList.index0 = 0;
			options.expList.index1 = parent0.expList.getSize()-1;
		} else {
			options.expList.index0 = parent0.currentExperimentIndex;
			options.expList.index1 = options.expList.index0;
		}
		options.expList = parent0.expList; 
		
		options.stepFrame = parent0.paneSequence.tabIntervals.getStepFrame();
		options.isFrameFixed = parent0.paneSequence.tabIntervals.getIsFixedFrame();
		options.startFrame = parent0.paneSequence.tabIntervals.getStartFrame();
		options.endFrame = parent0.paneSequence.tabIntervals.getEndFrame();
				
		options.diskRadius 	= (int) diskRadiusSpinner.getValue();
		options.doRegistration = doRegistrationCheckBox.isSelected();
		options.updateViewerDuringComputation = updateViewerCheckBox.isSelected();
		options.parent0Rect = parent0.mainFrame.getBoundsInternal();
		return true;
	}
		
	private void startComputation() {
		parent0.currentExperimentIndex = parent0.paneSequence.expListComboBox.getSelectedIndex();
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp == null) 
			return;
		parent0.paneSequence.tabClose.closeExp(exp);
		
		thread = new BuildKymographs_series();	
		parent0.paneSequence.transferExperimentNamesToExpList(parent0.expList, true);
		initBuildParameters(exp);
		
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		thread.buildBackground	= false;
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
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp != null)
				parent0.paneSequence.openExperiment(exp);
			startComputationButton.setText(detectString);
		 }
	}
	

}
