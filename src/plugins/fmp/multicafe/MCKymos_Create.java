package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;

import loci.formats.FormatException;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;

import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeSequence.SequenceKymosUtils;
import plugins.fmp.multicafeTools.BuildKymographs;
import plugins.fmp.multicafeTools.EnumStatusComputation;


public class MCKymos_Create extends JPanel { 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	JButton 				kymoStartComputationButton 	= new JButton("Start");
	JButton 				kymosStopComputationButton 	= new JButton("Stop");
	JSpinner 				diskRadiusSpinner 			= new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
	JSpinner 				stepJSpinner 				= new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
	JCheckBox 				doRegistrationCheckBox 		= new JCheckBox("registration", false);
	JCheckBox				updateViewerCheckBox 		= new JCheckBox("update viewer", true);
	JCheckBox				ALLCheckBox 				= new JCheckBox("ALL series", false);
	
	EnumStatusComputation 	sComputation 				= EnumStatusComputation.START_COMPUTATION; 
	private MultiCAFE 		parent0						= null;
	private BuildKymographs	buildKymographsThread 		= null;
	private Thread 			thread 						= null;
	int index0 = 0;
	int index1 = 0;
	int index= 0;
	int indexold = 0;
	boolean loopRunning = false;
	ExperimentList expList = null;


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
		add(GuiUtil.besidesPanel(new JLabel("step ", SwingConstants.RIGHT), stepJSpinner, new JLabel (" "), ALLCheckBox));
		ALLCheckBox.setForeground(Color.RED);
		ALLCheckBox.setEnabled(false);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		kymoStartComputationButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				if (!ALLCheckBox.isSelected())
					kymosBuildStart();
				else
					series_kymosBuildStart();
		}});
		
		kymosStopComputationButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				if (!ALLCheckBox.isSelected())
					kymosBuildStop();
				else
					series_kymosBuildStop();
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
	
	//------------------------------------
	

	private void kymosBuildStart() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		if (exp.seqCamData == null) 
			return;
		if (exp.seqKymos != null) {
			exp.seqKymos.seq.removeAllROI();
			exp.seqKymos.seq.close();
		}
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		parent0.sequencePane.intervalsTab.getAnalyzeFrameFromDialog (exp);
		getBuildKymosParametersFromDialog (exp);
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
		parent0.kymosPane.displayTab.viewKymosCheckBox.setSelected(true);
		parent0.kymosPane.displayTab.displayViews (true);
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

	// -----------------------------------
	
	private void series_kymosBuildStart() {
		if (expList == null) {
			expList = new ExperimentList(); 
			parent0.sequencePane.infosTab.transferExperimentNamesToExpList(expList);
			index0 = 0;
			index1 = expList.experimentList.size();
			index = 0;
		}	
		Experiment exp = series_loadExperimentData(index);	
		if (exp != null) {
//			initInputSequenceViewer(exp);
			series_launchCalculation(exp);
		}
	}
	
	/*
	private void initInputSequenceViewer (Experiment exp) {
		ThreadUtil.invoke (new Runnable() {
			@Override
			public void run() {
				viewer1 = new Viewer(exp.seqCamData.seq, true);
			}
		}, true);
		if (viewer1 == null) {
			viewer1 = exp.seqCamData.seq.getFirstViewer(); 
			if (!viewer1.isInitialized()) {
				try {
					Thread.sleep(1000);
					if (!viewer1.isInitialized())
						System.out.println("Viewer still not initialized after 1 s waiting");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Rectangle rectv = viewer1.getBoundsInternal();
		Rectangle rect0 = parent0.mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		viewer1.setBounds(rectv);
	}
*/
	
	private Experiment series_loadExperimentData(int indexExp) {
		Experiment exp = expList.getExperiment(index);
		if (null != exp.seqCamData.loadSequence(exp.experimentFileName)) {
			exp.loadFileIntervalsFromSeqCamData();
			if (exp.seqKymos != null) {
				exp.seqKymos.seq.removeAllROI();
				exp.seqKymos.seq.close();
			}
			return exp;
		}
		else
			return null;
	}
	
	private void series_launchCalculation (Experiment exp) {
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		getBuildKymosParametersFromDialog (exp);
		exp.seqKymos = new SequenceKymos();
		exp.seqKymos.updateCapillariesFromCamData(exp.seqCamData);
		setStartButton(false);
		series_kymosBuildKymographs();	
	}
	
	private void series_kymosBuildStop() {	
		if (thread != null && thread.isAlive()) {
			buildKymographsThread.stopFlag = true;
			try {
				thread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		parent0.kymosPane.displayTab.viewKymosCheckBox.setSelected(true);
		parent0.kymosPane.displayTab.displayViews (true);
	}
	
	private void series_resetUserInterface() {
		sComputation = EnumStatusComputation.START_COMPUTATION;
		firePropertyChange( "KYMOS_CREATE", false, true);
		setStartButton(true);
		firePropertyChange( "KYMOS_OK", false, true);
	}
	
	private void series_kymosBuildKymographs() {	
		if (expList == null) {
			System.out.println("expList is null - operation aborted");
			return;
		}
		Experiment exp = expList.getExperiment(index);
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
		
		Thread waitcompletionThread = new Thread(null, new Runnable() { public void run() {
			try { 
				thread.join();
			}
			catch(Exception e){;} 
			finally { 
				series_kymosBuildStop();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (index < index1) {
							loopRunning = true;
							series_saveComputation();
							index++;
							kymoStartComputationButton.setEnabled(true);
							if (!buildKymographsThread.stopFlag)
								kymoStartComputationButton.doClick();
						} 
						else {
							loopRunning = false;
							series_loadExperimentData(parent0.currentIndex);
							expList = null;
							series_resetUserInterface();
						}
					}});
				
			}
		}}, "+++waitforcompletion");
		waitcompletionThread.start();
	}
	
	private void series_saveComputation() {
		Experiment exp = expList.getExperiment(index);
		Path dir = Paths.get(exp.seqCamData.getDirectory());
		dir = dir.resolve("results");
		String directory = dir.toAbsolutePath().toString();
		if (Files.notExists(dir))  {
			try {
				Files.createDirectory(dir);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Creating directory failed: "+ directory);
				return;
			}
		}

		ProgressFrame progress = new ProgressFrame("Save kymographs");		
		for (int t = 0; t < exp.seqKymos.seq.getSizeT(); t++) {
			Capillary cap = exp.seqKymos.capillaries.capillariesArrayList.get(t);
			progress.setMessage( "Save kymograph file : " + cap.getName());	
			String filename = directory + File.separator + cap.getName() + ".tiff";
			File file = new File (filename);
			IcyBufferedImage image = exp.seqKymos.seq.getImage(t, 0);
			try {
				Saver.saveImage(image, file, true);
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		progress.close();
	}

}
