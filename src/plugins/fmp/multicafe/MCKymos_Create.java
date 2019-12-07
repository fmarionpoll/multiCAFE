package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;

import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeTools.BuildKymographs_series;
import plugins.fmp.multicafeTools.BuildKymographs_Options;
import plugins.fmp.multicafeTools.EnumStatusComputation;
import plugins.fmp.multicafeTools.ProgressChrono;


public class MCKymos_Create extends JPanel { 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	JButton 				kymoStartComputationButton 	= new JButton("Start");
	JButton 				kymosStopComputationButton 	= new JButton("Stop");
	JButton					updateStepButton			= new JButton("Save step");
	JSpinner 				diskRadiusSpinner 			= new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
	JSpinner 				stepJSpinner 				= new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
	JCheckBox 				doRegistrationCheckBox 		= new JCheckBox("registration", false);
	JCheckBox				updateViewerCheckBox 		= new JCheckBox("update viewer", true);
	JCheckBox				ALLCheckBox 				= new JCheckBox("ALL series", false);
	
	EnumStatusComputation 	sComputation 				= EnumStatusComputation.START_COMPUTATION; 
	private MultiCAFE 		parent0						= null;
	private BuildKymographs_series	buildKymographsThread2 = null;
	private Thread 			thread 						= null;
	private int currentExp = -1;



	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(
				kymoStartComputationButton, 
				kymosStopComputationButton));
		add(GuiUtil.besidesPanel(
				new JLabel("area around ROIs", SwingConstants.RIGHT), 
				diskRadiusSpinner, 
				updateViewerCheckBox, 
				doRegistrationCheckBox
				));
		add(GuiUtil.besidesPanel(new JLabel("step ", SwingConstants.RIGHT), stepJSpinner, updateStepButton, ALLCheckBox));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		kymoStartComputationButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				series_kymosBuildStart();
		}});
		
		kymosStopComputationButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				series_kymosBuildStop();
		}});
		
		updateStepButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				updateStepInXMLExperiments((int) stepJSpinner.getValue());
		}});
		
		ALLCheckBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				Color color = Color.BLACK;
				if (ALLCheckBox.isSelected()) 
					color = Color.RED;
				ALLCheckBox.setForeground(color);
				kymoStartComputationButton.setForeground(color);
				updateStepButton.setForeground(color);
		}});
	}
	
	void setBuildKymosParametersToDialog (Experiment exp) {
		stepJSpinner.setValue(exp.step);
	}
	
	void getBuildKymosParametersFromDialog (Experiment exp) {
		exp.step = (int) stepJSpinner.getValue();
		exp.seqCamData.analysisStep = (int) stepJSpinner.getValue();
	}
	
	private void setStartButton(boolean enableStart) {
		kymoStartComputationButton.setEnabled(enableStart );
		kymosStopComputationButton.setEnabled(!enableStart);
	}
	
	// -----------------------------------
	
	private void series_kymosBuildStart() {
		buildKymographsThread2 = new BuildKymographs_series();	
		BuildKymographs_Options options = buildKymographsThread2.options;
		
		parent0.paneSequence.tabInfos.transferExperimentNamesToExpList(parent0.expList, false);
		options.expList = parent0.expList; 
		options.expList.index0 = parent0.currentExperimentIndex;
		options.expList.index1 = options.expList.index0;
		if (ALLCheckBox.isSelected()) {
			options.expList.index0 = 0;
			options.expList.index1 = parent0.expList.experimentList.size()-1;
		}
		options.expList = parent0.expList; 
		options.analyzeStep = (int) stepJSpinner.getValue();
		options.diskRadius 	= (int) diskRadiusSpinner.getValue();
		options.doRegistration = doRegistrationCheckBox.isSelected();
		options.updateViewerDuringComputation = updateViewerCheckBox.isSelected();
		
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		setStartButton(false);
		currentExp = parent0.currentExperimentIndex;
		Experiment exp = parent0.expList.getExperiment(currentExp);
		parent0.paneSequence.tabClose.closeExp(exp);
		
		series_kymosBuildKymographs();	
	}
	
	private void series_kymosBuildStop() {	
		if (thread != null && thread.isAlive()) {
			buildKymographsThread2.stopFlag = true;
			try {
				thread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		parent0.paneKymos.tabDisplay.viewKymosCheckBox.setSelected(true);
		parent0.paneKymos.tabDisplay.displayViews (true);
	}
	
	private void series_resetUserInterface() {
		Experiment exp = parent0.expList.getExperiment(currentExp);
		parent0.openExperiment(exp);
		sComputation = EnumStatusComputation.START_COMPUTATION;
		firePropertyChange( "KYMOS_CREATE", false, true);
		setStartButton(true);
	}
	
	private void series_kymosBuildKymographs() {	
		BuildKymographs_Options options = buildKymographsThread2.options;
		if (options.expList == null) {
			System.out.println("expList is null - operation aborted");
			return;
		}
		
		thread = new Thread(null, buildKymographsThread2, "+++buildkymos");
		thread.start();
		Thread waitcompletionThread = new Thread(null, new Runnable() {
			public void run() {
				try { 
					thread.join();
					}
				catch(Exception e){;} 
				finally { 
					series_kymosBuildStop();
					series_resetUserInterface();
				}
			}}, "+++waitforcompletion");
		waitcompletionThread.start();
	}
	
	private void updateStepInXMLExperiments(int step ) {
		parent0.paneSequence.tabInfos.transferExperimentNamesToExpList(parent0.expList, true);
		int i0 = 0;
		int i1 = parent0.expList.experimentList.size();
		if (!ALLCheckBox.isSelected()) {
			i0 = parent0.currentExperimentIndex;
			i1 = i0+1;
		}
		ProgressChrono progressBar = new ProgressChrono("Compute kymographs");
		progressBar.initStuff(i1);
		progressBar.setMessageFirstPart("Processing XML MCexperiment ");
		
		for (int index = i0; index < i1; index++) {
			progressBar.updatePosition(index);
			Experiment exp = parent0.expList.experimentList.get(index);
			exp.step =step;
			exp.xmlSaveExperiment();
		}
		progressBar.close();
	}

}
