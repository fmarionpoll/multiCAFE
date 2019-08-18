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
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeSequence.SequenceCamData;


// SequenceListener?
public class MultiCAFE extends PluginActionable implements ViewerListener, PropertyChangeListener
{
	IcyFrame mainFrame = new IcyFrame("MultiCAFE analysis 18-August-2019", true, true, true, true);
	
	SequenceCamData 			seqCamData 			= null;
	SequenceKymos				seqKymos			= null;
	
	MCSequencePane 				sequencePane 		= new MCSequencePane();
	MCCapillariesPane 			capillariesPane 	= new MCCapillariesPane();
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

		capillariesPane.init(mainPanel, "CAPILLARIES -> KYMOGRAPHS", this);
		capillariesPane.addPropertyChangeListener(this);	
				
		kymographsPane.init(mainPanel, "KYMOS -> MEASURE TOP LEVEL & GULPS", this);
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
				Viewer v = event.getSource(); 
				int id = v.getSequence().getId();
				if (id == seqCamData.seq.getId())
					v.setTitle(seqCamData.getDecoratedImageName(v.getPositionT()));
				else
					v.setTitle(seqKymos.getDecoratedImageName(v.getPositionT()));
			}
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("SEQ_OPENED")) {			
			seqCamData.prefetchImagesThread_START(100);
			loadPreviousMeasures(
					sequencePane.openTab.isCheckedLoadPreviousProfiles(), 
					sequencePane.openTab.isCheckedLoadKymographs(),
					sequencePane.openTab.isCheckedLoadCages(),
					sequencePane.openTab.isCheckedLoadMeasures());
		}
		else if (arg0.getPropertyName().equals("CAPILLARIES_OPEN")) {
		  	sequencePane.browseTab.setBrowseItems(this.seqCamData);
		}
		else if (arg0.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
			capillariesPane.optionsTab.displayUpdateOnSwingThread();
			capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
		else if (arg0.getPropertyName().equals("SEQ_SAVEMEAS")) {
			if (seqKymos != null 
					&& seqKymos.capillaries != null 
					&& seqKymos.capillaries.capillariesArrayList.size() > 0) {
				capillariesPane.getCapillariesInfos(seqKymos.capillaries);
				sequencePane.infosTab.getCapillariesInfosFromDialog(seqKymos.capillaries);
				if (capillariesPane.capold.isChanged(seqKymos.capillaries)) {
					capillariesPane.saveCapillaryTrack();
					kymographsPane.fileTab.saveKymosMeasures();
					movePane.saveDefaultCages();
				}
			}
		}
		else if (arg0.getPropertyName() .equals("EXPORT_TO_EXCEL")) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				kymographsPane.fileTab.saveKymosMeasures();
			}});
		}
	} 

	private void loadPreviousMeasures(boolean loadCapillaries, boolean loadKymographs, boolean loadCages, boolean loadMeasures) {
		if (loadCapillaries) {
			if( !capillariesPane.loadCapillaryTrack()) 
				return;
			sequencePane.browseTab.setBrowseItems(this.seqCamData);
			capillariesPane.unitsTab.visibleCheckBox.setSelected(true);
		}
		
		if (loadKymographs) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() { 
				if ( !capillariesPane.fileTab.loadDefaultKymos()) {
					return;
				}
				if (loadMeasures) {
					kymographsPane.fileTab.loadKymosMeasures();
					if (sequencePane.openTab.graphsCheckBox.isSelected())
						SwingUtilities.invokeLater(new Runnable() {
						    public void run() {
						    	kymographsPane.graphsTab.xyDisplayGraphs();
						}});
				}
			}});
		}
		
		if (loadCages) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				ProgressFrame progress = new ProgressFrame("Load cages and fly movements");
				movePane.loadDefaultCages();
				movePane.graphicsTab.moveCheckbox.setEnabled(true);
				movePane.graphicsTab.displayResultsButton.setEnabled(true);
				if (seqCamData.cages != null && seqCamData.cages.flyPositionsList.size() > 0) {
					double threshold = seqCamData.cages.flyPositionsList.get(0).threshold;
					movePane.graphicsTab.aliveThresholdSpinner.setValue(threshold);
				}
				progress.close();
			}});
		}
	}

}

