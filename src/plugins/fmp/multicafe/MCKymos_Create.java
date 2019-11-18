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
import plugins.fmp.multicafeTools.BuildKymographs_series;
import plugins.fmp.multicafeTools.BuildKymographs_Options;
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
	private BuildKymographs_series	buildKymographsThread2 		= null;
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
		BuildKymographs_Options options = buildKymographsThread.options;	
		options.seqCamData 	= exp.seqCamData;
		options.seqKymos	= exp.seqKymos;
		options.analyzeStep = exp.seqCamData.analysisStep;
		options.startFrame 	= (int) exp.seqCamData.analysisStart;
		options.endFrame 	= (int) exp.seqCamData.analysisEnd;
		options.diskRadius 	= (int) diskRadiusSpinner.getValue();
		options.doRegistration= doRegistrationCheckBox.isSelected();
		options.updateViewerDuringComputation = updateViewerCheckBox.isSelected();
		
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
		buildKymographsThread2 = new BuildKymographs_series();
		
		BuildKymographs_Options options = buildKymographsThread2.options;
		options.expList = new ExperimentList(); 
		parent0.sequencePane.infosTab.transferExperimentNamesToExpList(options.expList);
		options.index0 = 0;
		options.index1 = buildKymographsThread.options.expList.experimentList.size();
		options.index = 0;
		options.analyzeStep = (int) stepJSpinner.getValue();
		options.diskRadius 	= (int) diskRadiusSpinner.getValue();
		options.doRegistration= doRegistrationCheckBox.isSelected();
		options.updateViewerDuringComputation = updateViewerCheckBox.isSelected();
		
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
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
		BuildKymographs_Options options = buildKymographsThread2.options;
		if (options.expList == null) {
			System.out.println("expList is null - operation aborted");
			return;
		}
		
		// start building kymos in a separate thread

		
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
						if (buildKymographsThread.options.index < buildKymographsThread.options.index1) {
							buildKymographsThread.options.loopRunning = true;
							series_saveComputation();
							buildKymographsThread.options.index++;
							kymoStartComputationButton.setEnabled(true);
							if (!buildKymographsThread.stopFlag)
								kymoStartComputationButton.doClick();
						} 
						else {
							buildKymographsThread.options.loopRunning = false;
							//series_loadExperimentData(parent0.currentIndex);
							buildKymographsThread.options.expList = null;
							series_resetUserInterface();
						}
					}});
				
			}
		}}, "+++waitforcompletion");
		waitcompletionThread.start();
	}
	
	private void series_saveComputation() {
		Experiment exp = buildKymographsThread.options.expList.getExperiment(buildKymographsThread.options.index);
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
