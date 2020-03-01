package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.preferences.XMLPreferences;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeTools.ComboBoxWide;
import plugins.fmp.multicafeTools.ComboBoxWithIndexTextRenderer;


public class MCSequence_ extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6826269677524125173L;
	
	private JTabbedPane 		tabsPane 		= new JTabbedPane();
	MCSequence_Open 			tabOpen 		= new MCSequence_Open();
	MCSequence_Infos			tabInfos		= new MCSequence_Infos();
	public MCSequence_Intervals	tabIntervals	= new MCSequence_Intervals();
	MCSequence_Display			tabDisplay 		= new MCSequence_Display();
	MCSequence_Close 			tabClose 		= new MCSequence_Close();
	private JButton  			previousButton	= new JButton("<");
	private JButton				nextButton		= new JButton(">");
	ComboBoxWide		 		expListComboBox	= new ComboBoxWide();
	private MultiCAFE 			parent0 		= null;
	
	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		JPanel k2Panel = new JPanel();
		k2Panel.setLayout(new BorderLayout());
		k2Panel.add(new JLabel("Stack "), BorderLayout.WEST); 
		k2Panel.add(previousButton, BorderLayout.EAST); 
		int bWidth = 30;
		int height = 10;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		
		JPanel k3Panel = new JPanel();
		k3Panel.setLayout(new BorderLayout());
		k3Panel.add(k2Panel, BorderLayout.WEST);
		k3Panel.add(expListComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, height)); 
		k3Panel.add(nextButton, BorderLayout.EAST);
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
			
		capPopupPanel.expand();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout tabsLayout = new GridLayout(3, 1);
		
		tabOpen.init(tabsLayout, parent0);
		tabsPane.addTab("Open/Add", null, tabOpen, "Open one or several stacks of .jpg files");
		tabOpen.addPropertyChangeListener(this);
		
		tabInfos.init(tabsLayout);
		tabsPane.addTab("Infos", null, tabInfos, "Define infos for this experiment/box");
		tabInfos.addPropertyChangeListener(this);
		
		tabIntervals.init(tabsLayout);
		tabsPane.addTab("Intervals", null, tabIntervals, "Browse and analysis parameters");
		tabIntervals.addPropertyChangeListener(this);

		tabDisplay.init(tabsLayout, parent0);
		tabsPane.addTab("Display", null, tabDisplay, "Display ROIs");
		tabDisplay.addPropertyChangeListener(this);

		tabClose.init(tabsLayout, parent0);
		tabsPane.addTab("Close", null, tabClose, "Close file and associated windows");
		tabClose.addPropertyChangeListener(this);

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(k3Panel, BorderLayout.PAGE_START);
		capPanel.add(tabsPane, BorderLayout.PAGE_END);	
		
		capPopupPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				parent0.mainFrame.revalidate();
				parent0.mainFrame.pack();
				parent0.mainFrame.repaint();
			}
		});
		//expListComboBox.setPrototypeDisplayValue("text here");
		ComboBoxWithIndexTextRenderer renderer = new ComboBoxWithIndexTextRenderer();
		expListComboBox.setRenderer(renderer);
		
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		expListComboBox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (expListComboBox.getItemCount() == 0 || tabInfos.disableChangeFile) {
				updateBrowseInterface();
				return;
			}
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			String oldtext = exp.seqCamData.getDirectory();
			String newtext = (String) expListComboBox.getSelectedItem();
			if (!newtext.contains(oldtext)) {
        		ThreadUtil.bgRun( new Runnable() { @Override public void run() {
	        		parent0.paneSequence.tabClose.closeExp(exp); //saveAndClose(exp);
        		}});
        		tabInfos.updateCombos();
				parent0.paneCapillaries.tabInfos.updateCombos();
				parent0.currentExperimentIndex = parent0.expList.getPositionOfCamFileName(newtext);			
				openSequenceCamFromCombo();
				updateBrowseInterface();
			}
		} } );
		
		nextButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (expListComboBox.getSelectedIndex() < (expListComboBox.getItemCount() -1)) {
				expListComboBox.setSelectedIndex(expListComboBox.getSelectedIndex()+1);
			}
		} } );
		
		previousButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (expListComboBox.getSelectedIndex() > 0) {
				expListComboBox.setSelectedIndex(expListComboBox.getSelectedIndex()-1);
			}
		} } );
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName() .equals ("SEQ_OPENFILE")) {
			openSeqCamData();
		}
		else if (event.getPropertyName().equals("SEQ_ADDFILE")) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp == null) {
				openSeqCamData();
			} else {
				addSeqCamData();
			}
		}
		else if (event.getPropertyName().equals("UPDATE")) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			updateViewerForSequenceCam(exp.seqCamData);
			tabIntervals.getAnalyzeFrameFromDialog(exp);
		}

		else if (event.getPropertyName().equals("SEQ_CLOSE")) {
			System.out.println("SEQ_CLOSE");
		}
		else if (event.getPropertyName().equals("CLOSE_ALL")) {
			tabsPane.setSelectedIndex(0);
			expListComboBox.removeAllItems();
			expListComboBox.updateUI();
		}
		else if (event.getPropertyName().equals("SEARCH_CLOSED")) {
			int index = expListComboBox.getSelectedIndex();
			if (index < 0)
				index = 0;
			tabInfos.disableChangeFile = true;
			for (String name: tabOpen.selectedNames) {
				 addSequenceCamToCombo(name);
			}
			tabOpen.selectedNames.clear();
			if (expListComboBox.getItemCount() > 0) {
				expListComboBox.setSelectedIndex(index);
				updateBrowseInterface();
				tabInfos.disableChangeFile = false;
				openSequenceCamFromCombo();
			}
		}
	}
	
	private void openSeqCamData() {
		SequenceCamData seqCamData = parent0.openSequenceCam(null);
		if (seqCamData != null && seqCamData.seq != null) {
			parent0.updateDialogsAfterOpeningSequenceCam(seqCamData);
			expListComboBox.removeAllItems();
			if (addSequenceCamToCombo()) {
				loadMeasuresAndKymos();
				tabsPane.setSelectedIndex(1);
			}
		}
	}
	
	private void addSeqCamData() {
		Experiment exp0 = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		SequenceCamData seqCamData = parent0.openSequenceCam(null);
		if (seqCamData != null) {
			if (addSequenceCamToCombo()) {
				parent0.updateDialogsAfterOpeningSequenceCam(exp0.seqCamData);
				parent0.paneSequence.tabClose.closeExp(exp0);
			}
			loadMeasuresAndKymos();
			tabsPane.setSelectedIndex(1);
		}
	}
	
	void openExperiment(Experiment exp) {
		exp.xmlLoadExperiment();
		String filename = exp.experimentFileName;
		exp.seqCamData = exp.openSequenceCamData(filename);
		if (exp.seqCamData != null && exp.seqCamData.seq != null) {
			parent0.addSequence(exp.seqCamData.seq);
			updateViewerForSequenceCam(exp.seqCamData);
			loadMeasuresAndKymos();
		}
	}
	
	void openSequenceCamFromCombo() {
		SequenceCamData seqCamData = parent0.openSequenceCam((String) expListComboBox.getSelectedItem());
		parent0.updateDialogsAfterOpeningSequenceCam(seqCamData);
		ThreadUtil.bgRun( new Runnable() { @Override public void run() {  
			loadMeasuresAndKymos();
		}});
		tabsPane.setSelectedIndex(1);
	}
	
	private int addSequenceCamToCombo(String strItem) {
		int item = findIndexItemInCombo(strItem);
		if(item < 0) { 
			expListComboBox.addItem(strItem);
			item = findIndexItemInCombo(strItem);
		}
		return item;
	}
	
	private int findIndexItemInCombo(String strItem) {
		int nitems = expListComboBox.getItemCount();
		int item = -1;
		for (int i=0; i < nitems; i++) {
			if (strItem.equalsIgnoreCase(expListComboBox.getItemAt(i))) {
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
			expListComboBox.setSelectedItem(strItem);
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
			v.addListener( parent0 );
		}
	}
	
	public void transferSequenceCamDataToDialogs(SequenceCamData seqCamData) {
		tabIntervals.endFrameJSpinner.setValue((int)seqCamData.analysisEnd);
		updateViewerForSequenceCam(seqCamData);
	}

	void loadMeasuresAndKymos() {
		parent0.loadPreviousMeasures(
					tabOpen.isCheckedLoadPreviousProfiles(), 
					tabOpen.isCheckedLoadKymographs(),
					tabOpen.isCheckedLoadCages(),
					tabOpen.isCheckedLoadMeasures());
	}
	
	void getExperimentInfosFromDialog(Experiment exp) {
		tabInfos.getExperimentInfosFromDialog(exp);
		tabIntervals.getAnalyzeFrameFromDialog (exp);
	}
	
	void updateBrowseInterface() {
		int isel = expListComboBox.getSelectedIndex();
		boolean flag1 = (isel == 0? false: true);
		boolean flag2 = (isel == (expListComboBox.getItemCount() -1)? false: true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
	}
	
	void transferExperimentNamesToExpList(ExperimentList expList, boolean clearOldList) {
		if (clearOldList)
			expList.experimentList.clear();
		int nitems = expListComboBox.getItemCount();
		for (int i=0; i< nitems; i++) {
			String filename = expListComboBox.getItemAt(i);
			Experiment exp = expList.addNewExperiment(filename);
			exp.xmlLoadExperiment();
		}
	}
}
