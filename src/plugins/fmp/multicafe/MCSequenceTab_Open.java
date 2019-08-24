package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.GuiUtil;
import icy.preferences.XMLPreferences;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeTools.MulticafeTools;


public class MCSequenceTab_Open extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6565346204580890307L;
	private JButton 	openButton				= new JButton("Open...");
	private JButton 	addButton				= new JButton("Add...");
	private JButton		searchButton 				= new JButton("Search for files...");
	private JButton		closeButton				= new JButton("Close search dialog");
	private JCheckBox	capillariesCheckBox		= new JCheckBox("capillaries", true);
	private JCheckBox	cagesCheckBox			= new JCheckBox("cages", true);
	private JCheckBox	kymographsCheckBox		= new JCheckBox("kymographs", true);
	private JCheckBox	measuresCheckBox		= new JCheckBox("measures", true);
	JCheckBox			graphsCheckBox			= new JCheckBox("graphs", true);

	private JTextField 	filterTextField 		= new JTextField("capillarytrack");
	private JButton 	findButton				= new JButton("Select root directory and search...");
	private JButton 	clearSelectedButton		= new JButton("Clear selected");
	private JButton 	clearAllButton			= new JButton("Clear all");
	private JButton 	addSelectedButton		= new JButton("Add selected");
	private JButton 	addAllButton			= new JButton("Add all");
	private JList<String> directoriesJList		= new JList<String>(new DefaultListModel<String>());
	
	public List<String> selectedNames 			= null;
	IcyFrame 			mainFrame 				= null;
	private MultiCAFE 	parent0 				= null;
	private boolean 	isSearchRunning 		= false;

	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		add( GuiUtil.besidesPanel(openButton, addButton));
		add( GuiUtil.besidesPanel(searchButton, closeButton));
		add( GuiUtil.besidesPanel(capillariesCheckBox, kymographsCheckBox, cagesCheckBox, measuresCheckBox, graphsCheckBox));
		
		searchButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	if (selectedNames == null)
            		selectedNames = new ArrayList<String> ();
            	showDialog();
            }});
		closeButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	closeDialog();
            	firePropertyChange("SEARCH_CLOSED", false, true);
            }});
		openButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	if(parent0.sequencePane.infosTab.expListComboBox.getItemCount() > 0 )
            		parent0.sequencePane.closeTab.closeAll();
            	firePropertyChange("SEQ_OPENFILE", false, true);
            }});
		addButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	firePropertyChange("SEQ_ADDFILE", false, true);
            }});
	}
	
	boolean isCheckedLoadPreviousProfiles() {
		return capillariesCheckBox.isSelected();
	}
	
	boolean isCheckedLoadKymographs() {
		return kymographsCheckBox.isSelected();
	}
	
	boolean isCheckedLoadCages() {
		return cagesCheckBox.isSelected();
	}
	
	boolean isCheckedLoadMeasures() {
		return measuresCheckBox.isSelected();
	}
	
	private void closeDialog() {
		mainFrame.close();
		mainFrame = null;
	}
	
	private void showDialog() {
		if (mainFrame != null) {
			mainFrame.close();
			mainFrame = null;
		}
		
		mainFrame = new IcyFrame ("Dialog box to select files", true, true);
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		
		mainPanel.add(GuiUtil.besidesPanel(findButton, filterTextField));
		
		directoriesJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		directoriesJList.setLayoutOrientation(JList.VERTICAL);
		directoriesJList.setVisibleRowCount(20);
		JScrollPane scrollPane = new JScrollPane(directoriesJList);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.add(GuiUtil.besidesPanel(scrollPane));
		
		mainPanel.add(GuiUtil.besidesPanel(clearSelectedButton, clearAllButton));
		mainPanel.add(GuiUtil.besidesPanel(addSelectedButton, addAllButton));
		
		addActionListeners();
		
		mainFrame.pack();
		mainFrame.addToDesktopPane();
		mainFrame.requestFocus();
		mainFrame.center();
		mainFrame.setVisible(true);
	}
	
	void addActionListeners() {
		findButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)  {	
    			final String pattern = filterTextField.getText();
    			if (isSearchRunning) 
    				return;
    	      	SwingUtilities.invokeLater(new Runnable() { public void run() {
    	      		isSearchRunning = true;
    	    		ProgressFrame progress = new ProgressFrame("Browsing directories to find files matching the searched name...");
    	    		getListofFilesMatchingPattern(pattern);
    	    		progress.close();
    	    		isSearchRunning = false;
    	      	}});
            }});
		clearSelectedButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	List<String> selectedItems = directoriesJList.getSelectedValuesList();
    		    removeListofNamesFromList (selectedItems);
            }});
		clearAllButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	((DefaultListModel<String>) directoriesJList.getModel()).removeAllElements();
            }});
		addSelectedButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	List<String> selectedItems = directoriesJList.getSelectedValuesList();
    			addNamesToSelectedList(selectedItems);
    			removeListofNamesFromList(selectedItems);
            }});
		addAllButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
    			List<String> allItems = new ArrayList <String> (directoriesJList.getModel().getSize());
    			for(int i = 0; i< directoriesJList.getModel().getSize();i++) {
    			    String name = directoriesJList.getModel().getElementAt(i);
    				allItems.add(name);
    				}
    			addNamesToSelectedList(allItems);
    			((DefaultListModel<String>) directoriesJList.getModel()).removeAllElements();
    			mainFrame.close();
    			firePropertyChange("SEARCH_CLOSED", false, true);
            }});
	}
		
	private void addNamesToSelectedList(List<String> stringList) {
		
		for (String name : stringList) {
			String directory = Paths.get(name).getParent().toString();
			selectedNames.add(directory);
		}
	}
	
	private void removeListofNamesFromList(List<String> selectedItems) {
		for (String oo: selectedItems)
	    	 ((DefaultListModel<String>) directoriesJList.getModel()).removeElement(oo);
	}
	
 	private void getListofFilesMatchingPattern(String pattern) {
		XMLPreferences guiPrefs = parent0.getPreferences("gui");
		String lastUsedPathString = guiPrefs.get("lastUsedPath", "");
		File dir = MulticafeTools.chooseDirectory(lastUsedPathString);
		if (dir == null) {
			return;
		}
		lastUsedPathString = dir.getAbsolutePath();
		guiPrefs.put("lastUsedPath", lastUsedPathString);
		try {
			Files.walk(Paths.get(lastUsedPathString))
			.filter(Files::isRegularFile)		
			.forEach((f)->{
			    String fileName = f.toString();
			    if( fileName.contains(pattern)) {
			    	addNameToListIfNew(fileName);
			    }
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addNameToListIfNew(String fileName) {	
		int ilast = ((DefaultListModel<String>) directoriesJList.getModel()).getSize();
		boolean found = false;
		for (int i=0; i < ilast; i++) {
			String oo = ((DefaultListModel<String>) directoriesJList.getModel()).getElementAt(i);
			if (oo.equalsIgnoreCase (fileName)) {
				found = true;
				break;
			}
		}
		if (!found)
			((DefaultListModel<String>) directoriesJList.getModel()).addElement(fileName);
	}
	
	void loadMeasuresAndKymos() {
		ThreadUtil.bgRun( new Runnable() { @Override public void run() {  
			parent0.loadPreviousMeasures(
					isCheckedLoadPreviousProfiles(), 
					isCheckedLoadKymographs(),
					isCheckedLoadCages(),
					isCheckedLoadMeasures());
		}});

}

}
