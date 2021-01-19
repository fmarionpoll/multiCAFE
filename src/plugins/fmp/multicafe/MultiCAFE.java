package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Rectangle;
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
import icy.sequence.Sequence;
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
	public IcyFrame 		mainFrame 		= new IcyFrame("MultiCAFE 17-Jan-2021", true, true, true, true);
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
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("CAPILLARIES_OPEN")) {
			Experiment exp = expList.getCurrentExperiment();
			if (exp != null) {
				paneSequence.tabIntervals.displayCamDataIntervals(exp);
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
		if (exp == null) {
			exp = new Experiment(filename);
			expList.addExperiment(exp);
		}
		exp.openSequenceCamData(filename);
		if (exp.seqCamData != null && exp.seqCamData.seq != null) {
			updateViewerForSequenceCam(exp);
		} else {
			System.out.println("seqCamData or seq of seqCamData is null!");
		}
		return exp;
	}
	
	public void updateViewerForSequenceCam(Experiment exp) {
		Sequence seq = exp.seqCamData.seq;
		Viewer v = seq.getFirstViewer();
		if (v == null) {
			v = new Viewer(exp.seqCamData.seq, true);
		}
		if (v != null) {
			placeViewerNextToDialogBox(v, mainFrame);
			v.toFront();
			v.requestFocus();
			v.addListener( this );
			v.setTitle(exp.seqCamData.getDecoratedImageName(0));
		}
	}
	
	private void placeViewerNextToDialogBox(Viewer v, IcyFrame mainFrame) {
		Rectangle rectv = v.getBoundsInternal();
		Rectangle rect0 = mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		v.setBounds(rectv);
	}

	public void loadPreviousMeasures(boolean loadCapillaries, boolean loadKymographs, boolean loadCages, boolean loadMeasures) {
		Experiment exp = expList.getCurrentExperiment();
		if (exp == null)
			return;
		
		boolean flag = exp.xmlLoadMCcapillaries_Only();
		if (flag)
			paneCapillaries.displayCapillariesInformation(exp);

		if (loadCapillaries) {
			paneLevels.tabFileLevels.loadCapillaries_Measures(exp);
		}

		if (loadKymographs) {
			if (paneKymos.tabFile.loadDefaultKymos(exp)) {
		        paneKymos.tabDisplay.transferCapillaryNamesToComboBox(exp.capillaries.capillariesArrayList);
			}
			paneSequence.tabIntervals.displayCamDataIntervals(exp);
			paneKymos.tabIntervals.displayKymoIntervals(exp);

			if (paneSequence.tabOpen.graphsCheckBox.isSelected())
				SwingUtilities.invokeLater(new Runnable() { public void run() {
				    paneLevels.tabGraphs.xyDisplayGraphs(exp);
				}});
		}
		
		if (loadCages) {
			ProgressFrame progress = new ProgressFrame("load fly positions");
			exp.loadDrosotrack();
			paneCages.tabGraphics.moveCheckbox.setEnabled(true);
			paneCages.tabGraphics.displayResultsButton.setEnabled(true);
			exp.updateROIsAt(0);
			progress.close();
		}
	}
	
	@Override	
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEventType.POSITION_CHANGED)) {
			if (event.getDim() == DimensionId.T) {
				Viewer v = event.getSource(); 
				int idViewer = v.getSequence().getId(); 
				Experiment exp = expList.getCurrentExperiment();
				int idCurrentExp = exp.seqCamData.seq.getId();
				if (idViewer == idCurrentExp) {
					int t = v.getPositionT(); 
					v.setTitle(exp.seqCamData.getDecoratedImageName(t));
					if (paneCages.trapROIsEdit) 
						exp.saveDetRoisToPositions();
					exp.updateROIsAt(t);
				}
			}
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
	}

}

