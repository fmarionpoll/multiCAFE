package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Paths;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.preferences.XMLPreferences;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;


public class MCSequence_ extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6826269677524125173L;
	
	private JTabbedPane 	tabsPane 	= new JTabbedPane();
	MCSequence_Open 		tabOpen 	= new MCSequence_Open();
	MCSequence_Infos		tabInfos	= new MCSequence_Infos();
	MCSequence_Intervals	tabIntervals= new MCSequence_Intervals();
	MCSequence_Display		tabDisplay 	= new MCSequence_Display();
	MCSequence_Close 		tabClose 	= new MCSequence_Close();
	private MultiCAFE 		parent0 	= null;
	
	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.expand();
		
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout capLayout = new GridLayout(3, 1);
		
		tabOpen.init(capLayout, parent0);
		tabsPane.addTab("Open/Add", null, tabOpen, "Open one or several stacks of .jpg files");
		tabOpen.addPropertyChangeListener(this);
		
		tabInfos.init(capLayout, parent0);
		tabsPane.addTab("Infos", null, tabInfos, "Define infos for this experiment/box");
		tabInfos.addPropertyChangeListener(this);
		
		tabIntervals.init(capLayout);
		tabsPane.addTab("Intervals", null, tabIntervals, "Browse and analysis parameters");
		tabIntervals.addPropertyChangeListener(this);

		tabDisplay.init(capLayout, parent0);
		tabsPane.addTab("Display", null, tabDisplay, "Display ROIs");
		tabDisplay.addPropertyChangeListener(this);

		tabClose.init(capLayout, parent0);
		tabsPane.addTab("Close", null, tabClose, "Close file and associated windows");
		tabClose.addPropertyChangeListener(this);

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		
		capPopupPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				parent0.mainFrame.revalidate();
				parent0.mainFrame.pack();
				parent0.mainFrame.repaint();
			}
		});
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("SEQ_OPEN")) {
			openSequenceCamFromCombo(); 
		} 
		else if (event.getPropertyName() .equals ("SEQ_OPENFILE")) {
			SequenceCamData seqCamData = parent0.openSequenceCam(null);
			if (seqCamData != null && seqCamData.seq != null) {
				parent0.updateDialogsAfterOpeningSequenceCam(seqCamData);
				tabInfos.expListComboBox.removeAllItems();
				if (addSequenceCamToCombo()) {
					loadMeasuresAndKymos();
					tabsPane.setSelectedIndex(1);
				}
			}
		}
		else if (event.getPropertyName().equals("SEQ_ADDFILE")) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			exp.seqCamData = parent0.openSequenceCam(null);
			if (exp.seqCamData != null) {
				if (addSequenceCamToCombo()) {
					parent0.updateDialogsAfterOpeningSequenceCam(exp.seqCamData);
					ThreadUtil.bgRun( new Runnable() { @Override public void run() {
		        		parent0.paneSequence.tabClose.closeExp(exp); //saveAndClose(exp);
		    		}});
				}
			}
		}
		else if (event.getPropertyName().equals("UPDATE")) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			updateViewerForSequenceCam(exp.seqCamData);
			tabIntervals.getAnalyzeFrameFromDialog(exp);
			parent0.paneKymos.tabCreate.getBuildKymosParametersFromDialog(exp);
		}

		else if (event.getPropertyName().equals("SEQ_CLOSE")) {
			tabsPane.setSelectedIndex(0);
			tabInfos.expListComboBox.removeAllItems();
		}
		else if (event.getPropertyName().equals("SEARCH_CLOSED")) {
			int index = tabInfos.expListComboBox.getSelectedIndex();
			if (index < 0)
				index = 0;
			tabInfos.disableChangeFile = true;
			for (String name: tabOpen.selectedNames) {
				 addSequenceCamToCombo(name);
			}
			tabOpen.selectedNames.clear();
			if (tabInfos.expListComboBox.getItemCount() > 0) {
				tabInfos.expListComboBox.setSelectedIndex(index);
				tabInfos.updateBrowseInterface();
				tabInfos.disableChangeFile = false;
				openSequenceCamFromCombo();
			}
		}
	}
	
	void openSequenceCamFromCombo() {
		SequenceCamData seqCamData = parent0.openSequenceCam((String) tabInfos.expListComboBox.getSelectedItem());
		parent0.updateDialogsAfterOpeningSequenceCam(seqCamData);
		loadMeasuresAndKymos();
		tabsPane.setSelectedIndex(1);
	}
	
	private int addSequenceCamToCombo(String strItem) {
		int item = findIndexItemInCombo(strItem);
		if(item < 0) { 
			tabInfos.expListComboBox.addItem(strItem);
			item = findIndexItemInCombo(strItem);
		}
		return item;
	}
	
	private int findIndexItemInCombo(String strItem) {
		int nitems = tabInfos.expListComboBox.getItemCount();
		int item = -1;
		for (int i=0; i < nitems; i++) {
			if (strItem.equalsIgnoreCase(tabInfos.expListComboBox.getItemAt(i))) {
				item = i;
				break;
			}
		}
		return item;
	}
	
	boolean addSequenceCamToCombo() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp == null)
			return false;
		String filename = exp.seqCamData.getFileName();
		if (filename == null) 
			return false;
		String strItem = Paths.get(filename).toString();
		if (strItem != null) {
			addSequenceCamToCombo(strItem);
			tabInfos.expListComboBox.setSelectedItem(strItem);
			XMLPreferences guiPrefs = parent0.getPreferences("gui");
			guiPrefs.put("lastUsedPath", strItem);
		}
		return true;
	}
				
	void updateViewerForSequenceCam(SequenceCamData seqCamData) {
		Viewer v = seqCamData.seq.getFirstViewer();
		if (v != null) {
			Rectangle rectv = v.getBoundsInternal();
			Rectangle rect0 = parent0.mainFrame.getBoundsInternal();
			rectv.setLocation(rect0.x+ rect0.width, rect0.y);
			v.setBounds(rectv);
			v.toFront();
			v.requestFocus();
		}
	}
	
	void transferSequenceCamDataToDialogs(SequenceCamData seqCamData) {
		tabIntervals.endFrameJSpinner.setValue((int)seqCamData.analysisEnd);
		updateViewerForSequenceCam(seqCamData);
	}

	void loadMeasuresAndKymos() {
		ThreadUtil.bgRun( new Runnable() { @Override public void run() {  
			parent0.loadPreviousMeasures(
					tabOpen.isCheckedLoadPreviousProfiles(), 
					tabOpen.isCheckedLoadKymographs(),
					tabOpen.isCheckedLoadCages(),
					tabOpen.isCheckedLoadMeasures());
		}});

	}
}
