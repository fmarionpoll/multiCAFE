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
	private int 			currentExp 					= -1;



	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(
				startComputationButton, new JLabel(" ")));
		add(GuiUtil.besidesPanel(
				new JLabel("area around ROIs", SwingConstants.RIGHT), 
				diskRadiusSpinner, 
				updateViewerCheckBox, 
				doRegistrationCheckBox
				));
		add(GuiUtil.besidesPanel(new JLabel(" "), new JLabel(" "), new JLabel(" "), ALLCheckBox));
		
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
		parent0.paneSequence.tabIntervals.getAnalyzeFrameFromDialog (exp);
	}
	
	
	private boolean initBuildParameters(Experiment exp) {
		if (thread == null)
			return false;
		
		BuildKymographs_Options options = thread.options;
		options.expList = parent0.expList; 
		options.expList.index0 = currentExp;
		options.expList.index1 = currentExp;
		if (ALLCheckBox.isSelected()) {
			options.expList.index0 = 0;
			options.expList.index1 = parent0.expList.experimentList.size()-1;
		}
		options.expList = parent0.expList; 
		options.analyzeStep = exp.step;
		options.startFrame = exp.startFrame;
		options.endFrame = exp.endFrame;
		
		options.diskRadius 	= (int) diskRadiusSpinner.getValue();
		options.doRegistration = doRegistrationCheckBox.isSelected();
		options.updateViewerDuringComputation = updateViewerCheckBox.isSelected();
		return true;
	}
		
	private void startComputation() {
		thread = new BuildKymographs_series();	
		parent0.paneSequence.transferExperimentNamesToExpList(parent0.expList, false);
		if (parent0.currentExperimentIndex >= parent0.expList.experimentList.size())
			parent0.currentExperimentIndex = parent0.expList.experimentList.size()-1;
		currentExp = parent0.currentExperimentIndex;
		Experiment exp = parent0.expList.getExperiment(currentExp);
		parent0.paneSequence.tabClose.closeExp(exp);
		
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		parent0.paneSequence.tabIntervals.getAnalyzeFrameFromDialog(exp);
		parent0.paneSequence.tabClose.closeExp(exp);
		
		initBuildParameters(exp);
		
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
			Experiment exp = parent0.expList.getExperiment(currentExp);
			parent0.paneSequence.openExperiment(exp);
			startComputationButton.setText(detectString);
		 }
		
	}
	

}
