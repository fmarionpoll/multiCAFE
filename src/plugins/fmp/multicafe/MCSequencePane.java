package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.preferences.XMLPreferences;
import plugins.fmp.multicafeSequence.SequenceCamData;


public class MCSequencePane extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6826269677524125173L;
	
	private JTabbedPane 	tabsPane 	= new JTabbedPane();
	MCSequenceTab_Open 		openTab 	= new MCSequenceTab_Open();
	MCSequenceTab_Infos		infosTab	= new MCSequenceTab_Infos();
	MCSequenceTab_Browse	browseTab 	= new MCSequenceTab_Browse();
	MCSequenceTab_Close 	closeTab 	= new MCSequenceTab_Close();
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
			openSequenceCamFromCombo(); 
		}
		else if (event.getPropertyName() .equals ("SEQ_OPENFILE")) {
			if (createSequenceCamFromString(null)) {
				infosTab.experimentComboBox.removeAllItems();
				addSequenceCamToComboAndLoadData();
			}
		}
		else if (event.getPropertyName().equals("SEQ_ADDFILE")) {
			if (createSequenceCamFromString(null)) {
				addSequenceCamToComboAndLoadData();
			}
		 }
		 else if (event.getPropertyName().equals("UPDATE")) {
			browseTab.getBrowseItems(parent0.seqCamData);
			Viewer v =parent0.seqCamData.seq.getFirstViewer();
			v.toFront();
			v.requestFocus();
		 }
		 else if (event.getPropertyName().equals("SEQ_SAVEMEAS")) {
			 firePropertyChange("SEQ_SAVEMEAS", false, true);
		 }
		 else if (event.getPropertyName().equals("SEQ_CLOSE")) {
			tabsPane.setSelectedIndex(0);
			infosTab.experimentComboBox.removeAllItems();
			firePropertyChange("SEQ_CLOSE", false, true);
		 }
		 else if (event.getPropertyName().equals("SEARCH_CLOSED")) {
			int index = infosTab.experimentComboBox.getSelectedIndex();
			if (index < 0)
				index = 0;
			infosTab.disableChangeFile = true;
			for (String name: openTab.selectedNames) {
				 addSequenceCamToCombo(name);
			}
			openTab.selectedNames.clear();
			if (infosTab.experimentComboBox.getItemCount() > 0) {
				infosTab.experimentComboBox.setSelectedIndex(index);
				infosTab.updateBrowseInterface();
				infosTab.disableChangeFile = false;
				openSequenceCamFromCombo();
			}
		 }
	}
	
	private void openSequenceCamFromCombo() {
		String filename = (String) infosTab.experimentComboBox.getSelectedItem();
		createSequenceCamFromString(filename);
		updateViewerForSequenceCam();
		addSequenceCamToCombo(parent0.seqCamData.getFileName());
		firePropertyChange("SEQ_OPENED", false, true);
		tabsPane.setSelectedIndex(1);
	}
	
	void addSequenceCamToCombo(String strItem) {
		int nitems = infosTab.experimentComboBox.getItemCount();
		boolean alreadystored = false;
		for (int i=0; i < nitems; i++) {
			if (strItem.equalsIgnoreCase(infosTab.experimentComboBox.getItemAt(i))) {
				alreadystored = true;
				break;
			}
		}
		if(!alreadystored) 
			infosTab.experimentComboBox.addItem(strItem);
	}
	
	void addSequenceCamToComboAndLoadData() {
		String strItem = parent0.seqCamData.getFileName();
		if (strItem != null) {
			addSequenceCamToCombo(strItem);
			infosTab.experimentComboBox.setSelectedItem(strItem);
			updateViewerForSequenceCam();
			firePropertyChange("SEQ_OPENED", false, true);
		}
	}
	
	boolean createSequenceCamFromString (String filename) {
		if (parent0.seqCamData != null)
			parent0.seqCamData.seq.close();		
		parent0.seqCamData = new SequenceCamData();
		
		String path = parent0.seqCamData.loadSequence(filename);
		if (path != null) {
			parent0.seqCamData.setParentDirectoryAsFileName() ;
			browseTab.endFrameJSpinner.setValue((int)parent0.seqCamData.analysisEnd);
			XMLPreferences guiPrefs = parent0.getPreferences("gui");
			guiPrefs.put("lastUsedPath", path);
			parent0.addSequence(parent0.seqCamData.seq);
			parent0.seqCamData.seq.getFirstViewer().addListener( parent0 );
		}
		return (path != null);
	}
	
	private void updateViewerForSequenceCam() {
		Viewer v = parent0.seqCamData.seq.getFirstViewer();
		if (v != null) {
			Rectangle rectv = v.getBoundsInternal();
			Rectangle rect0 = parent0.mainFrame.getBoundsInternal();
			rectv.setLocation(rect0.x+ rect0.width, rect0.y);
			v.setBounds(rectv);
		}
	}
	
}
