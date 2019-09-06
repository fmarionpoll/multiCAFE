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
import icy.gui.frame.progress.ProgressFrame;
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
	MCSequence_Analysis	browseTab 	= new MCSequence_Analysis();
	MCSequence_Close 	closeTab 	= new MCSequence_Close();
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
		
		browseTab.init(capLayout);
		tabsPane.addTab("Browse", null, browseTab, "Browse stack and adjust analysis parameters");
		browseTab.addPropertyChangeListener(this);

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
			ProgressFrame progress = new ProgressFrame("Open sequence...");
			openSequenceCamFromCombo(); 
			progress.close();
		}
		else if (event.getPropertyName() .equals ("SEQ_OPENFILE")) {
			ProgressFrame progress = new ProgressFrame("Open file...");
			SequenceCamData seqCamData = parent0.openSequenceCam(null);
			parent0.updateDialogsAfterOpeningSequenceCam(seqCamData);
			if (seqCamData != null) {
				infosTab.expListComboBox.removeAllItems();
				addSequenceCamToCombo();
			}
			progress.close();
		}
		else if (event.getPropertyName().equals("SEQ_ADDFILE")) {
			ProgressFrame progress = new ProgressFrame("Add file...");
			SequenceCamData seqCamData = parent0.openSequenceCam(null);
			parent0.updateDialogsAfterOpeningSequenceCam(seqCamData);
			if (seqCamData != null) {
				addSequenceCamToCombo();
			}
			progress.close();
		 }
		 else if (event.getPropertyName().equals("UPDATE")) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
			updateViewerForSequenceCam(exp.seqCamData);
			browseTab.getAnalyzeFrameAndStepFromDialog(exp.seqCamData);
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
	
	private void addSequenceCamToCombo(String strItem) {
		int nitems = infosTab.expListComboBox.getItemCount();
		boolean alreadystored = false;
		for (int i=0; i < nitems; i++) {
			if (strItem.equalsIgnoreCase(infosTab.expListComboBox.getItemAt(i))) {
				alreadystored = true;
				break;
			}
		}
		if(!alreadystored) 
			infosTab.expListComboBox.addItem(strItem);
	}
	
	void addSequenceCamToCombo() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		String strItem = Paths.get(exp.seqCamData.getFileName()).toString();
		if (strItem != null) {
			addSequenceCamToCombo(strItem);
			infosTab.expListComboBox.setSelectedItem(strItem);
			updateViewerForSequenceCam(exp.seqCamData);
			loadMeasuresAndKymos();
			XMLPreferences guiPrefs = parent0.getPreferences("gui");
			guiPrefs.put("lastUsedPath", strItem);
		}
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
		browseTab.endFrameJSpinner.setValue((int)seqCamData.analysisEnd);
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
