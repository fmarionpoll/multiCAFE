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
	JCheckBox 				doCreateCheckBox 			= new JCheckBox("force creation of results_bin", false);
	JCheckBox				allCheckBox 				= new JCheckBox("ALL series (current to last)", false);
	JSpinner 				stepFrameJSpinner			= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	
	EnumStatusComputation 	sComputation 				= EnumStatusComputation.START_COMPUTATION; 
	private MultiCAFE 		parent0						= null;
	private BuildKymographs_series thread 				= null;

	// -----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		
		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);
		
		JPanel panel1 = new JPanel(layout1);
		panel1.add(startComputationButton);
		panel1.add(allCheckBox);
		add(GuiUtil.besidesPanel(panel1));
		
		JPanel panel2 = new JPanel(layout1);
		panel2.add(new JLabel("area around ROIs", SwingConstants.RIGHT));
		panel2.add(diskRadiusSpinner);  
		panel2.add(doRegistrationCheckBox);
		add(GuiUtil.besidesPanel(panel2));
		
		JPanel panel3 = new JPanel(layout1);
		panel3.add(new JLabel(" step "));
		panel3.add(stepFrameJSpinner );
		panel3.add(doCreateCheckBox);
		add(GuiUtil.besidesPanel(panel3));		
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

		allCheckBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				Color color = Color.BLACK;
				if (allCheckBox.isSelected()) 
					color = Color.RED;
				allCheckBox.setForeground(color);
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
		options.expList.index0 = parent0.expList.currentExperimentIndex;
		if (allCheckBox.isSelected())
			options.expList.index1 = parent0.expList.getSize()-1;
		else
			options.expList.index1 = options.expList.index0; 
		
		options.stepFrame 		= (int) stepFrameJSpinner.getValue();
		options.isFrameFixed 	= parent0.paneSequence.tabIntervals.getIsFixedFrame();
		options.startFrame 		= parent0.paneSequence.tabIntervals.getStartFrame();
		options.endFrame 		= parent0.paneSequence.tabIntervals.getEndFrame();
				
		options.diskRadius 		= (int) diskRadiusSpinner.getValue();
		options.doRegistration 	= doRegistrationCheckBox.isSelected();
		options.doCreateResults_bin = doCreateCheckBox.isSelected();
		options.parent0Rect 	= parent0.mainFrame.getBoundsInternal();
		options.resultsSubPath = (String) parent0.paneKymos.tabDisplay.availableResultsCombo.getSelectedItem() ;
		return true;
	}
		
	private void startComputation() {
		int current = parent0.paneSequence.expListComboBox.getSelectedIndex();
		Experiment exp = parent0.expList.getExperiment(current);
		if (exp == null) 
			return;
		parent0.expList.currentExperimentIndex = current;
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
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (exp != null) {
				exp.setKymoFrameStep((int) stepFrameJSpinner.getValue());
				parent0.paneSequence.openExperiment(exp);
			}
			startComputationButton.setText(detectString);
		 }
	}
	

}
