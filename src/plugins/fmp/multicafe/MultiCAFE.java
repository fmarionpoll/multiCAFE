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
public class MultiCAFE extends PluginActionable implements ViewerListener, PropertyChangeListener
{
	IcyFrame mainFrame = new IcyFrame("MultiCAFE analysis 22-August-2019", true, true, true, true);
	
	ExperimentList				expList 			= new ExperimentList();
	int							currentIndex		= -1;
	int							previousIndex		= -1;
	
	MCSequencePane 				sequencePane 		= new MCSequencePane();
	MCCapillariesPane 			capillariesPane 	= new MCCapillariesPane();
	MCBuildKymosPane			buildKymosPane		= new MCBuildKymosPane();
	MCKymosPane 				kymographsPane 		= new MCKymosPane();
	MCMovePane 					movePane 			= new MCMovePane();
	MCExcelPane					excelPane			= new MCExcelPane();
	

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
		  	sequencePane.browseTab.setAnalyzeFrameAndStepToDialog(expList.getSeqCamData(currentIndex));
		}
		else if (arg0.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
			buildKymosPane.optionsTab.displayUpdateOnSwingThread();
			buildKymosPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
		else if (arg0.getPropertyName() .equals("EXPORT_TO_EXCEL")) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				Experiment exp = expList.getExperiment(currentIndex);
				kymographsPane.fileTab.saveKymosMeasures(exp);
			}});
		}
	} 

	public void loadPreviousMeasures(boolean loadCapillaries, boolean loadKymographs, boolean loadCages, boolean loadMeasures) {
		ProgressFrame progress = new ProgressFrame("Load capillaries & kymographs");
		Experiment exp = expList.getExperiment(currentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		System.out.println("load seqCamdata document ="+ seqCamData.getFileName());
		
		if (loadCapillaries) {
			progress.setMessage("1/3 - load capillaries and measures");
			System.out.println("loadCapillaryTrack");
			exp.loadCapillaryTrack();
			System.out.println("loadCapillaryTrack done - update dialogs");
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				sequencePane.browseTab.setAnalyzeFrameAndStepToDialog(seqCamData);
				sequencePane.infosTab.setCapillariesInfosToDialog(exp.seqKymos.capillaries);
				capillariesPane.infosTab.setCapillariesInfosToDialog(exp.seqKymos.capillaries);
				capillariesPane.infosTab.visibleCheckBox.setSelected(true);
				}});
		}

		if (loadKymographs) {
			progress.setMessage("2/3 - load kymographs");
			exp.loadKymographs();
			kymographsPane.fileTab.transferMeasuresToROIs();	
			if (sequencePane.openTab.graphsCheckBox.isSelected())
				SwingUtilities.invokeLater(new Runnable() { public void run() {
				    	kymographsPane.graphsTab.xyDisplayGraphs();
				}});
		}
		
		if (loadCages) {
			progress.setMessage("3/3 - load cages");
			exp.loadDrosotrack();
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				movePane.graphicsTab.moveCheckbox.setEnabled(true);
				movePane.graphicsTab.displayResultsButton.setEnabled(true);
				if (seqCamData.cages != null && seqCamData.cages.flyPositionsList.size() > 0) {
					double threshold = seqCamData.cages.flyPositionsList.get(0).threshold;
					movePane.graphicsTab.aliveThresholdSpinner.setValue(threshold);
				}
			}});
		}

		progress.close();
	}

}

