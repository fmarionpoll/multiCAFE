package plugins.fmp.multicafe;

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
import icy.gui.viewer.Viewer;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeSequence.SequenceKymosUtils;
import plugins.fmp.multicafeTools.BuildKymographs;
import plugins.fmp.multicafeTools.EnumStatusComputation;


public class MCKymosBuild_Create extends JPanel { 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	JButton 				kymoStartComputationButton 	= new JButton("Start");
	JButton 				kymosStopComputationButton 	= new JButton("Stop");
	JSpinner 				diskRadiusSpinner 			= new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
	JCheckBox 				doRegistrationCheckBox 		= new JCheckBox("registration", false);
	JCheckBox				updateViewerCheckBox 		= new JCheckBox("update viewer", true);
	EnumStatusComputation 	sComputation 				= EnumStatusComputation.START_COMPUTATION; 
	private MultiCAFE 		parent0						= null;
	private BuildKymographs	buildKymographsThread 		= null;
	private Thread 			thread 						= null;


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
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		kymoStartComputationButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			kymosBuildStart();
		}});
		
		kymosStopComputationButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			kymosBuildStop();
		}});	
	}
	
	private void setStartButton(boolean enableStart) {
		kymoStartComputationButton.setEnabled(enableStart );
		kymosStopComputationButton.setEnabled(!enableStart);
	}
	
	// -----------------------------------
	
	private void kymosBuildStart() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		if (exp.seqCamData == null) 
			return;
		if (exp.seqKymos != null) {
			exp.seqKymos.seq.removeAllROI();
			exp.seqKymos.seq.close();
		}
		
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		parent0.sequencePane.intervalsTab.getAnalyzeFrameAndStepFromDialog (exp.seqCamData);
		exp.seqKymos = new SequenceKymos();
		exp.seqKymos.updateCapillariesFromCamData(exp.seqCamData);
		setStartButton(false);
		kymosBuildKymographs();	
		Viewer v = exp.seqCamData.seq.getFirstViewer();
		v.toFront();
	}
	
	private void kymosBuildStop() {	
		if (thread != null && thread.isAlive()) {
			buildKymographsThread.stopFlag = true;
			try {
				thread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		parent0.buildKymosPane.displayTab.viewKymosCheckBox.setSelected(true);
		parent0.buildKymosPane.displayTab.displayViews (true);
	}
	
	private void resetUserInterface() {
		sComputation = EnumStatusComputation.START_COMPUTATION;
		firePropertyChange( "KYMOS_CREATE", false, true);
		setStartButton(true);
		firePropertyChange( "KYMOS_OK", false, true);
	}
	
	private void kymosBuildKymographs() {
				
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		buildKymographsThread = null;
		if (exp.seqKymos != null && exp.seqKymos.seq != null)
			exp.seqKymos.seq.close();
		exp.seqKymos = new SequenceKymos();
		SequenceKymosUtils.transferCamDataROIStoKymo(exp.seqCamData, exp.seqKymos);
		
		// start building kymos in a separate thread
		buildKymographsThread = new BuildKymographs();
		buildKymographsThread.options.seqCamData 	= exp.seqCamData;
		buildKymographsThread.options.seqKymos		= exp.seqKymos;
		buildKymographsThread.options.analyzeStep 	= exp.seqCamData.analysisStep;
		buildKymographsThread.options.startFrame 	= (int) exp.seqCamData.analysisStart;
		buildKymographsThread.options.endFrame 		= (int) exp.seqCamData.analysisEnd;
		buildKymographsThread.options.diskRadius 	= (int) diskRadiusSpinner.getValue();
		buildKymographsThread.options.doRegistration= doRegistrationCheckBox.isSelected();
		buildKymographsThread.options.updateViewerDuringComputation = updateViewerCheckBox.isSelected();
		
		thread = new Thread(null, buildKymographsThread, "+++buildkymos");
		thread.start();
		
		Thread waitcompletionThread = new Thread(null, new Runnable() {
			public void run() {
				try{ 
					thread.join();
					}
				catch(Exception e){;} 
				finally { 
					kymosBuildStop();
					resetUserInterface();
				}
			}}, "+++waitforcompletion");
		waitcompletionThread.start();
	}

}
