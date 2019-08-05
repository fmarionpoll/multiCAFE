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
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeSequence.SequenceVirtual;


// SequenceListener?
public class MultiCAFE extends PluginActionable implements ViewerListener, PropertyChangeListener, SequenceListener
{
	SequenceVirtual 			vSequence 			= null;
	SequencePlus				vkymos				= null;
	
	IcyFrame mainFrame = new IcyFrame("MultiCAFE analysis 05-August-2019", true, true, true, true);
	
	MCSequencePane 				sequencePane 		= new MCSequencePane();
	MCCapillariesPane 			capillariesPane 	= new MCCapillariesPane();
	MCKymosPane 				kymographsPane 		= new MCKymosPane();
	MCMovePane 					movePane 			= new MCMovePane();
	MCExcelPane					excelPane			= new MCExcelPane();

	//-------------------------------------------------------------------
	
	@Override
	public void run() 
	{		
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

	void roisSaveEdits() 
	{
		if (vkymos != null && vkymos.hasChanged) {
			vkymos.validateRois();
			// TODO? vkymos.getArrayListFromRois(EnumArrayListType.cumSum, -1);
			vkymos.hasChanged = false;
		}
	}

	@Override	
	public void viewerChanged(ViewerEvent event)
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED)) {
			if (event.getDim() == DimensionId.T)        
            	vSequence.currentFrame = event.getSource().getPositionT() ;
		}
	}

	@Override
	public void viewerClosed(Viewer viewer)
	{
		viewer.removeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) 
	{
		if (arg0.getPropertyName().equals("SEQ_OPENED")) {
			loadPreviousMeasures(
					sequencePane.openTab.isCheckedLoadPreviousProfiles(), 
					sequencePane.openTab.isCheckedLoadKymographs(),
					sequencePane.openTab.isCheckedLoadCages(),
					sequencePane.openTab.isCheckedLoadMeasures());
		}
		else if (arg0.getPropertyName().equals("CAPILLARIES_OPEN")) {
		  	sequencePane.browseTab.setBrowseItems(this.vSequence);
		}
		else if (arg0.getPropertyName() .equals("KYMO_DISPLAYFILTERED")) {
			capillariesPane.optionsTab.displayUpdateOnSwingThread();
			capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
		else if (arg0.getPropertyName().equals("SEQ_SAVEMEAS")) {
			capillariesPane.getCapillariesInfos(vSequence.capillaries);
			sequencePane.infosTab.getCapillariesInfos(vSequence.capillaries);
			if (capillariesPane.capold.isChanged(vSequence.capillaries)) {
				capillariesPane.saveDefaultCapillaries();
				kymographsPane.fileTab.saveKymosMeasures();
				movePane.saveDefaultCages();
			}
		}
		else if (arg0.getPropertyName() .equals("EXPORT_TO_EXCEL")) {
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				kymographsPane.fileTab.saveKymosMeasures();
			}});
		}
	} 
	
	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {
//		Sequence seq = sequenceEvent.getSequence();
//		SequenceEventSourceType seqSourceType = sequenceEvent.getSourceType();
//		switch(seqSourceType) {
//		case SEQUENCE_TYPE:
//		case SEQUENCE_META:
//		case SEQUENCE_COLORMAP:
//		case SEQUENCE_COMPONENTBOUNDS:
//		case SEQUENCE_DATA:
//		case SEQUENCE_ROI:
//		case SEQUENCE_OVERLAY:
//		default:
//			break;
//        
//		}
//		SequenceEventType seqEventType = sequenceEvent.getType();
//		switch (seqEventType) {
//		case ADDED:
//			break;
//		case CHANGED:
//			break;
//		case REMOVED:
//			break;
//		default:
//			break;
//		}
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequencePane.closeTab.closeAll();
	}
	
	private void loadPreviousMeasures(boolean loadCapillaries, boolean loadKymographs, boolean loadCages, boolean loadMeasures) {
		
		if (loadCapillaries) {
			if( !capillariesPane.loadDefaultCapillaries()) 
				return;
			sequencePane.browseTab.setBrowseItems(this.vSequence);
			capillariesPane.unitsTab.visibleCheckBox.setSelected(true);
		}
		
		if (loadKymographs) {
			MCKymosTab_File.flag = true;
			MCKymosTab_File.isRunning = true;
			MCKymosTab_File.isInterrupted = false;
			
			ThreadUtil.bgRun( new Runnable() { @Override public void run() { 
				
				if ( !capillariesPane.fileTab.loadDefaultKymos()) {
					return;
				}
				if (loadMeasures) {
					if (MCKymosTab_File.isRunning) {
						MCKymosTab_File.isInterrupted = true;
					}
					kymographsPane.fileTab.openKymosMeasures();
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
				if (vSequence.cages != null && vSequence.cages.flyPositionsList.size() > 0) {
					double threshold = vSequence.cages.flyPositionsList.get(0).threshold;
					movePane.graphicsTab.aliveThresholdSpinner.setValue(threshold);
				}
				progress.close();
			}});
		}
	}

}

