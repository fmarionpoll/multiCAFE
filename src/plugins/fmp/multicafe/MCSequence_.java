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
	MCSequence_Open 		openTab 	= new MCSequence_Open();
	MCSequence_Infos		infosTab	= new MCSequence_Infos();
	MCSequence_Intervals	intervalsTab = new MCSequence_Intervals();
	MCSequence_Display		displayTab 	= new MCSequence_Display();
	MCSequence_Close 		closeTab 	= new MCSequence_Close();
	private MultiCAFE 		parent0 	= null;
	
	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.expand();
		
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout capLayout = new GridLayout(3, 1);
		
		openTab.init(capLayout, parent0);
		tabsPane.addTab("Open/Add", null, openTab, "Open one or several stacks of .jpg files");
		openTab.addPropertyChangeListener(this);
		
		infosTab.init(capLayout, parent0);
		tabsPane.addTab("Infos", null, infosTab, "Define infos for this experiment/box");
		infosTab.addPropertyChangeListener(this);
		
		intervalsTab.init(capLayout);
		tabsPane.addTab("Intervals", null, intervalsTab, "Browse and analysis parameters");
		intervalsTab.addPropertyChangeListener(this);

		displayTab.init(capLayout, parent0);
		tabsPane.addTab("Display", null, displayTab, "Display ROIs");
		displayTab.addPropertyChangeListener(this);

		closeTab.init(capLayout, parent0);
		tabsPane.addTab("Close", null, closeTab, "Close file and associated windows");
		closeTab.addPropertyChangeListener(this);

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
				infosTab.expListComboBox.removeAllItems();
				if (addSequenceCamToCombo()) {
					loadMeasuresAndKymos();
					tabsPane.setSelectedIndex(1);
				}
			}
		}
		else if (event.getPropertyName().equals("SEQ_ADDFILE")) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			SequenceCamData seqCamData = parent0.openSequenceCam(null);
			if (seqCamData != null) {
				if (addSequenceCamToCombo()) {
					parent0.updateDialogsAfterOpeningSequenceCam(seqCamData);
					ThreadUtil.bgRun( new Runnable() { @Override public void run() {
		        		parent0.sequencePane.closeTab.closeExp(exp); //saveAndClose(exp);
		    		}});
				}
			}
		}
		else if (event.getPropertyName().equals("UPDATE")) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			updateViewerForSequenceCam(exp.seqCamData);
			intervalsTab.getAnalyzeFrameFromDialog(exp);
			parent0.kymosPane.createTab.getBuildKymosParametersFromDialog(exp);
		}

		else if (event.getPropertyName().equals("SEQ_CLOSE")) {
			tabsPane.setSelectedIndex(0);
			infosTab.expListComboBox.removeAllItems();
		}
		else if (event.getPropertyName().equals("SEARCH_CLOSED")) {
			int index = infosTab.expListComboBox.getSelectedIndex();
			if (index < 0)
				index = 0;
			infosTab.disableChangeFile = true;
			for (String name: openTab.selectedNames) {
				 addSequenceCamToCombo(name);
			}
			openTab.selectedNames.clear();
			if (infosTab.expListComboBox.getItemCount() > 0) {
				infosTab.expListComboBox.setSelectedIndex(index);
				infosTab.updateBrowseInterface();
				infosTab.disableChangeFile = false;
				openSequenceCamFromCombo();
			}
		}
	}
	
	private void openSequenceCamFromCombo() {
		SequenceCamData seqCamData = parent0.openSequenceCam((String) infosTab.expListComboBox.getSelectedItem());
		parent0.updateDialogsAfterOpeningSequenceCam(seqCamData);
		loadMeasuresAndKymos();
		tabsPane.setSelectedIndex(1);
	}
	
	private int addSequenceCamToCombo(String strItem) {
		int item = findIndexItemInCombo(strItem);
		if(item < 0) { 
			infosTab.expListComboBox.addItem(strItem);
			item = findIndexItemInCombo(strItem);
		}
		return item;
	}
	
	private int findIndexItemInCombo(String strItem) {
		int nitems = infosTab.expListComboBox.getItemCount();
		int item = -1;
		for (int i=0; i < nitems; i++) {
			if (strItem.equalsIgnoreCase(infosTab.expListComboBox.getItemAt(i))) {
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
			infosTab.expListComboBox.setSelectedItem(strItem);
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
		intervalsTab.endFrameJSpinner.setValue((int)seqCamData.analysisEnd);
		updateViewerForSequenceCam(seqCamData);
	}

	void loadMeasuresAndKymos() {
		ThreadUtil.bgRun( new Runnable() { @Override public void run() {  
			parent0.loadPreviousMeasures(
					openTab.isCheckedLoadPreviousProfiles(), 
					openTab.isCheckedLoadKymographs(),
					openTab.isCheckedLoadCages(),
					openTab.isCheckedLoadMeasures());
		}});

	}
}
