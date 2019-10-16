package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.gui.viewer.ViewerListener;

import icy.plugin.abstract_.PluginActionable;
import icy.sequence.DimensionId;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceCamData;


// SequenceListener?
public class MultiCAFE extends PluginActionable implements ViewerListener, PropertyChangeListener {
	IcyFrame 					mainFrame 			= new IcyFrame("MultiCAFE analysis 16-oct-2019", true, true, true, true);
	ExperimentList				expList 			= new ExperimentList();
	int							currentIndex		= -1;
	
	MCSequence_ 				sequencePane 		= new MCSequence_();
	MCCapillaries_ 				capillariesPane 	= new MCCapillaries_();
	MCKymosBuild_				buildKymosPane		= new MCKymosBuild_();
	MCKymosDetect_ 				kymographsPane 		= new MCKymosDetect_();
	MCMove_ 					movePane 			= new MCMove_();
	MCExcel_					excelPane			= new MCExcel_();
	
	//-------------------------------------------------------------------
	
	@Override
	public void run() {		
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);

		sequencePane.init(mainPanel, "SOURCE DATA", this);
		sequencePane.addPropertyChangeListener(this);

		capillariesPane.init(mainPanel, "CAPILLARIES", this);
		capillariesPane.addPropertyChangeListener(this);	
		
		buildKymosPane.init(mainPanel, "KYMOGRAPHS", this);
		buildKymosPane.addPropertyChangeListener(this);
		
		kymographsPane.init(mainPanel, "MEASURE TOP LEVEL & GULPS", this);
		kymographsPane.addPropertyChangeListener(this);
		
		movePane.init(mainPanel, "DETECT FLIES", this);
		movePane.addPropertyChangeListener(this);
		
		excelPane.init(mainPanel, "MEASURES -> EXCEL FILE (XLSX)", this);
		excelPane.addPropertyChangeListener(this);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	@Override	
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEventType.POSITION_CHANGED)) {
			if (event.getDim() == DimensionId.T) {
				Experiment exp = expList.experimentList.get(currentIndex);
				Viewer v = event.getSource(); 
				int id = v.getSequence().getId();
				if (id == exp.seqCamData.seq.getId())
					v.setTitle(exp.seqCamData.getDecoratedImageName(v.getPositionT()));
				else
					v.setTitle(exp.seqKymos.getDecoratedImageName(v.getPositionT()));
			}
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("CAPILLARIES_OPEN")) {
			Experiment exp = expList.getExperiment(currentIndex);
			sequencePane.intervalsTab.setAnalyzeFrameAndStepToDialog(exp.seqCamData);
		}
		else if (arg0.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
			buildKymosPane.displayTab.displayUpdateOnSwingThread();
			buildKymosPane.displayTab.viewKymosCheckBox.setSelected(true);
		}
		else if (arg0.getPropertyName() .equals("SAVE_KYMOSMEASURES")) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				Experiment exp = expList.getExperiment(currentIndex);
				kymographsPane.fileTab.saveKymosMeasures(exp);
			}});
		}
	} 
	
	SequenceCamData openSequenceCam(String filename) {
		Experiment exp = null;
		SequenceCamData seqCamData = null; 
		currentIndex = expList.getPositionOfCamFileName(filename);
		if (currentIndex < 0) {
			seqCamData = new SequenceCamData();
			if (null != seqCamData.loadSequence(filename)) {
				exp = new Experiment(seqCamData);
				currentIndex = expList.addExperiment(exp);
			}
			
		} else {
			exp = expList.getExperiment(currentIndex);
			seqCamData = exp.openSequenceCamData(filename);
		}
		
		if (seqCamData.seq != null) {
			addSequence(seqCamData.seq);
			seqCamData.seq.getFirstViewer().addListener( this );
		}
		return seqCamData;
	}
	
	void updateDialogsAfterOpeningSequenceCam(SequenceCamData seqCamData) {
		if (seqCamData == null)
			return;
		sequencePane.transferSequenceCamDataToDialogs(seqCamData);
		kymographsPane.transferSequenceCamDataToDialogs(seqCamData);		
	}

	void loadPreviousMeasures(boolean loadCapillaries, boolean loadKymographs, boolean loadCages, boolean loadMeasures) {
		Experiment exp = expList.getExperiment(currentIndex);
		if (exp == null)
			return;
		SequenceCamData seqCamData = exp.seqCamData;
		
		if (loadCapillaries) {
			ProgressFrame progress = new ProgressFrame("load capillarytrack_measures.xml");
			kymographsPane.fileTab.loadKymosMeasures(exp);
			progress.close();
		}

		if (loadKymographs) {
			ProgressFrame progress = new ProgressFrame("load kymograph *.tiff");
			buildKymosPane.displayTab.viewKymosCheckBox.setSelected(true);
			buildKymosPane.fileTab.loadDefaultKymos(exp);
			progress.close();
			if (sequencePane.openTab.graphsCheckBox.isSelected())
				SwingUtilities.invokeLater(new Runnable() { public void run() {
				    	kymographsPane.graphsTab.xyDisplayGraphs();
				}});
		}
		
		if (loadCages) {
			ProgressFrame progress = new ProgressFrame("load drosotrack.xml");
			exp.loadDrosotrack();
			progress.close();
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				movePane.graphicsTab.moveCheckbox.setEnabled(true);
				movePane.graphicsTab.displayResultsButton.setEnabled(true);
				if (seqCamData.cages != null && seqCamData.cages.flyPositionsList.size() > 0) {
					double threshold = seqCamData.cages.flyPositionsList.get(0).threshold;
					movePane.graphicsTab.aliveThresholdSpinner.setValue(threshold);
				}
			}});
		}
	}

}

