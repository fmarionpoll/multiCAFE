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
import plugins.fmp.multicafe.dlg.cages.MCCages_;
import plugins.fmp.multicafe.dlg.capillaries.MCCapillaries_;
import plugins.fmp.multicafe.dlg.excel.MCExcel_;
import plugins.fmp.multicafe.dlg.kymos.MCKymos_;
import plugins.fmp.multicafe.dlg.levels.MCLevels_;
import plugins.fmp.multicafe.dlg.sequence.MCSequence_;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.workinprogress_gpu.MCSpots_;



public class MultiCAFE extends PluginActionable implements ViewerListener, PropertyChangeListener {
	public IcyFrame 		mainFrame 		= new IcyFrame("MultiCAFE 21-Dec-2020", true, true, true, true);
	public ExperimentList 	expList 		= new ExperimentList();
	
	public MCSequence_ 		paneSequence 	= new MCSequence_();
	public MCCapillaries_ 	paneCapillaries	= new MCCapillaries_();
	public MCKymos_			paneKymos		= new MCKymos_();
	public MCLevels_ 		paneLevels 		= new MCLevels_();
	public MCSpots_			paneSpots		= new MCSpots_();
	public MCCages_ 		paneCages 		= new MCCages_();
	public MCExcel_			paneExcel		= new MCExcel_();
	
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
		
		paneLevels.init(mainPanel, "MEASURE LEVELS & GULPS", this);
		paneLevels.addPropertyChangeListener(this);

//		paneSpots.init(mainPanel, "MEASURE SPOTS", this);
//		paneSpots.addPropertyChangeListener(this);

		paneCages.init(mainPanel, "DETECT FLIES", this);
		paneCages.addPropertyChangeListener(this);
		
		paneExcel.init(mainPanel, "EXPORT TO XLSX FILE", this);
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
				Experiment exp = expList.getCurrentExperiment();
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
			Experiment exp = expList.getCurrentExperiment();
			if (exp != null) {
				paneSequence.tabIntervals.setAnalyzeFrameToDialog(exp);
				paneKymos.tabCreate.setBuildKymosParametersToDialog(exp);
			}
		}
		else if (arg0.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
			paneKymos.tabDisplay.displayUpdateOnSwingThread();
		}
		else if (arg0.getPropertyName() .equals("SAVE_KYMOSMEASURES")) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				Experiment exp = expList.getCurrentExperiment();
				if (exp != null)
					exp.saveExperimentMeasures(exp.getResultsDirectory());
			}});
		}
	} 
	
	public Experiment openExperimentFromString(String filename) {
		Experiment exp = expList.getExperimentFromFileName(filename);
		if (exp == null)
			return null;

		exp.seqCamData = exp.openSequenceCamData(filename);
		if (exp.seqCamData != null && exp.seqCamData.seq != null) {
			addSequence(exp.seqCamData.seq);
			exp.seqCamData.seq.getFirstViewer().addListener( this );
		} else {
			System.out.println("seqcamdata or seq of seqcamdata is null!");
		}
		return exp;
	}
	
	public void updateDialogsAfterOpeningSequenceCam(Experiment exp) {
		if (exp.seqCamData != null) {
			paneSequence.transferSequenceCamDataToDialogs(exp);	
			paneLevels.transferSequenceCamDataToDialogs(exp.seqCamData);
		}
	}

	public void loadPreviousMeasures(boolean loadCapillaries, boolean loadKymographs, boolean loadCages, boolean loadMeasures) {
		Experiment exp = expList.getCurrentExperiment();
		if (exp == null)
			return;
		ProgressFrame progress = new ProgressFrame("load descriptors");
		boolean flag = exp.xmlLoadMCcapillaries_Only();
		if (flag)
			paneCapillaries.displayCapillariesInformation(exp);
		progress.close();
		
		if (loadCapillaries) {
			progress = new ProgressFrame("load capillary measures");
			paneLevels.tabFileLevels.loadCapillaries_Measures(exp);
			progress.close();
		}

		if (loadKymographs) {
			progress = new ProgressFrame("load kymographs");
			if (paneKymos.tabFile.loadDefaultKymos(exp)) 
		        paneKymos.tabDisplay.transferCapillaryNamesToComboBox(exp.capillaries.capillariesArrayList);
			paneSequence.tabIntervals.setAnalyzeFrameToDialog(exp);
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
				paneCages.tabGraphics.moveCheckbox.setEnabled(true);
				paneCages.tabGraphics.displayResultsButton.setEnabled(true);
			}});
		}
		progress.close();
	}

}

