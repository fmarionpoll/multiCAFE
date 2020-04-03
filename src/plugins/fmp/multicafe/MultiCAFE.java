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
	IcyFrame 		mainFrame 		= new IcyFrame("MultiCAFE 3-April-2020", true, true, true, true);
	ExperimentList	expList 		= new ExperimentList();
	int				currentExperimentIndex	= -1;
	
	MCSequence_ 	paneSequence 	= new MCSequence_();
	MCCapillaries_ 	paneCapillaries	= new MCCapillaries_();
	MCKymos_		paneKymos		= new MCKymos_();
	MCLevels_ 		paneLevels 		= new MCLevels_();
	MCSpots_		paneSpots		= new MCSpots_();
	MCMove_ 		paneMove 		= new MCMove_();
	MCExcel_		paneExcel		= new MCExcel_();
	
	//-------------------------------------------------------------------
	
	@Override
	public void run() {		
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);

		paneSequence.init(mainPanel, "SOURCE DATA", this);
		paneSequence.addPropertyChangeListener(this);

		paneCapillaries.init(mainPanel, "CAPILLARIES", this);
		paneCapillaries.addPropertyChangeListener(this);	
		
		paneKymos.init(mainPanel, "KYMOGRAPHS", this);
		paneKymos.addPropertyChangeListener(this);
		
		paneLevels.init(mainPanel, "MEASURE TOP LEVEL & GULPS", this);
		paneLevels.addPropertyChangeListener(this);

//		paneSpots.init(mainPanel, "MEASURE SPOTS", this);
//		paneSpots.addPropertyChangeListener(this);

		paneMove.init(mainPanel, "DETECT FLIES", this);
		paneMove.addPropertyChangeListener(this);
		
		paneExcel.init(mainPanel, "MEASURES -> EXCEL FILE (XLSX)", this);
		paneExcel.addPropertyChangeListener(this);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	@Override	
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEventType.POSITION_CHANGED)) {
			if (event.getDim() == DimensionId.T) {
				Experiment exp = expList.experimentList.get(currentExperimentIndex);
				Viewer v = event.getSource(); 
				int id = v.getSequence().getId();
				int t = v.getPositionT();
				if (id == exp.seqCamData.seq.getId())
					v.setTitle(exp.seqCamData.getDecoratedImageName(t));
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
			Experiment exp = expList.getExperiment(currentExperimentIndex);
			if (exp != null) {
				paneSequence.tabIntervals.setAnalyzeFrameToDialog(exp);
				paneKymos.tabCreate.setBuildKymosParametersToDialog(exp);
			}
		}
		else if (arg0.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
			paneKymos.tabDisplay.displayUpdateOnSwingThread();
			paneKymos.tabDisplay.viewKymosCheckBox.setSelected(true);
		}
		else if (arg0.getPropertyName() .equals("SAVE_KYMOSMEASURES")) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				Experiment exp = expList.getExperiment(currentExperimentIndex);
				if (exp != null)
					paneLevels.tabFile.saveKymosMeasures(exp);
			}});
		}
	} 
	
	SequenceCamData openSequenceCam(String filename) {
		Experiment exp = null;
		currentExperimentIndex = expList.getPositionOfCamFileName(filename);
		if (currentExperimentIndex < 0) {
			exp = new Experiment();
			currentExperimentIndex = expList.addExperiment(exp);
			exp.seqCamData = new SequenceCamData();
		} else {
			if (currentExperimentIndex > expList.experimentList.size()-1)
				currentExperimentIndex = expList.experimentList.size()-1;
			exp = expList.getExperiment(currentExperimentIndex);
			if (exp == null)
				return null;
		}
		exp.seqCamData = exp.openSequenceCamData(filename);
		if (exp.seqCamData != null && exp.seqCamData.seq != null) {
			addSequence(exp.seqCamData.seq);
			exp.seqCamData.seq.getFirstViewer().addListener( this );
		} else {
			System.out.println("seqcamdata or seq of seqcamdata is null!");
		}
		return exp.seqCamData;
	}
	
	void updateDialogsAfterOpeningSequenceCam(SequenceCamData seqCamData) {
		if (seqCamData != null) {
			paneSequence.transferSequenceCamDataToDialogs(seqCamData);	
			paneLevels.transferSequenceCamDataToDialogs(seqCamData);
		}
	}

	void loadPreviousMeasures(boolean loadCapillaries, boolean loadKymographs, boolean loadCages, boolean loadMeasures) {
		Experiment exp = expList.getExperiment(currentExperimentIndex);
		if (exp == null)
			return;
		ProgressFrame progress = new ProgressFrame("load descriptors");
		paneCapillaries.loadCapillaries_();
		progress.close();
		
		if (loadCapillaries) {
			progress = new ProgressFrame("load capillary measures");
			paneLevels.tabFile.loadKymosMeasures(exp);
			progress.close();
		}

		if (loadKymographs) {
			progress = new ProgressFrame("load kymographs");
			paneKymos.tabDisplay.viewKymosCheckBox.setSelected(true);
			paneKymos.tabFile.loadDefaultKymos(exp);
			progress.close();
			if (paneSequence.tabOpen.graphsCheckBox.isSelected())
				SwingUtilities.invokeLater(new Runnable() { public void run() {
				    	paneLevels.tabGraphs.xyDisplayGraphs(exp);
				}});
		}
		
		if (loadCages) {
			progress = new ProgressFrame("load fly positions");
			exp.loadDrosotrack();
			progress.close();
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				paneMove.tabGraphics.moveCheckbox.setEnabled(true);
				paneMove.tabGraphics.displayResultsButton.setEnabled(true);
			}});
		}
		progress.close();
	}

}

