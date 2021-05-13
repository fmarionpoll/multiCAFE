package plugins.fmp.multicafe2.dlg.experiment;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import icy.gui.viewer.Viewer;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.SequenceNameListRenderer;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.ExperimentDirectories;
import plugins.fmp.multicafe2.tools.Directories;

public class LoadSave extends JPanel implements PropertyChangeListener, ItemListener, SequenceListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -690874563607080412L;
	
	private JButton 		createButton	= new JButton("Create...");
	private JButton 		openButton		= new JButton("Open...");
	private JButton			searchButton 	= new JButton("Search...");
	private JButton			closeButton		= new JButton("Close");
	protected JCheckBox		filteredCheck	= new JCheckBox("List filtered");
	
	public List<String> 	selectedNames 	= new ArrayList<String> ();
	private SelectFiles1 	dialogSelect 	= null;
	
	private JButton  		previousButton	= new JButton("<");
	private JButton			nextButton		= new JButton(">");

	private MultiCAFE2 		parent0 		= null;
	private MCExperiment_ 	parent1 		= null;
	private int 			listenerIndex 	= -1;
	
	

	JPanel initPanel( MultiCAFE2 parent0, MCExperiment_ parent1) 
	{
		this.parent0 = parent0;
		this.parent1 = parent1;

		SequenceNameListRenderer renderer = new SequenceNameListRenderer();
		parent0.expListCombo.setRenderer(renderer);
		int bWidth = 26; 
		int height = 20;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		nextButton.setPreferredSize(new Dimension(bWidth, height));
		
		JPanel sequencePanel0 = new JPanel(new BorderLayout());
		sequencePanel0.add(previousButton, BorderLayout.LINE_START);
		sequencePanel0.add(parent0.expListCombo, BorderLayout.CENTER);
		sequencePanel0.add(nextButton, BorderLayout.LINE_END);
		
		JPanel sequencePanel = new JPanel(new BorderLayout());
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(1);
		JPanel subPanel = new JPanel(layout);
		subPanel.add(openButton);
		subPanel.add(createButton);
		subPanel.add(searchButton);
		subPanel.add(closeButton);
		subPanel.add(filteredCheck);
		sequencePanel.add(subPanel, BorderLayout.LINE_START);
	
		defineActionListeners();
		parent0.expListCombo.addItemListener(this);
		
		JPanel twoLinesPanel = new JPanel (new GridLayout(2, 1));
		twoLinesPanel.add(sequencePanel0);
		twoLinesPanel.add(sequencePanel);

		return twoLinesPanel;
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("SELECT1_CLOSED")) 
		{
			parent1.tabInfos.disableChangeFile = true;
			if (selectedNames.size() < 1)
				return;
			
			ExperimentDirectories eDAF0 = getDirectoriesFromExptPath(selectedNames.get(0), null);
        	int index = addExperimentFrom3NamesAnd2Lists(eDAF0);
        	parent0.expListCombo.setSelectedIndex(index);
        	final String binSubDirectory = parent0.expListCombo.expListBinSubDirectory;
        	
        	SwingUtilities.invokeLater(new Runnable() { public void run() 
			{	
	        	parent1.tabInfos.disableChangeFile = false;
	        	for (int i=1; i < selectedNames.size(); i++) 
				{
					ExperimentDirectories eDAF = getDirectoriesFromExptPath(selectedNames.get(i), binSubDirectory);
		        	addExperimentFrom3NamesAnd2Lists(eDAF);
				}
				selectedNames.clear();
				updateBrowseInterface();
		     	parent1.tabInfos.disableChangeFile = true;
		     	parent1.tabInfos.initCombosWithExpList();
			}});
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			parent1.tabInfos.updateCombos();
			openExperimentFromCombo();
		} 
		else 
		{
			Experiment exp = (Experiment) e.getItem();
			ThreadUtil.bgRun( new Runnable() { @Override public void run() 
    		{
        		closeViewsForCurrentExperiment(exp); 
    		}});
		}
	}
	
	void closeAllExperiments() 
	{
		closeCurrentExperiment();
		parent0.expListCombo.removeAllItems();
		parent1.tabFilter.clearAllCheckBoxes ();
		parent1.tabFilter.savedExpList.removeAllItems();
		parent1.tabInfos.clearCombos();
		filteredCheck.setSelected(false);
	}
	
	public void closeViewsForCurrentExperiment(Experiment exp) 
	{
		if (exp != null) 
		{
			parent0.paneExperiment.tabInfos.getExperimentInfosFromDialog(exp);
			if (exp.seqCamData != null) 
			{
				exp.xmlSaveMCExperiment();
			}
			exp.closeSequences();
		}
		parent0.paneKymos.tabDisplay.kymographsCombo.removeAllItems();
	}
	
	public void closeCurrentExperiment() 
	{
		if (parent0.expListCombo.getSelectedIndex() < 0)
			return;
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			closeViewsForCurrentExperiment(exp);
	}
	
	void updateBrowseInterface() 
	{
		int isel = parent0.expListCombo.getSelectedIndex();		
	    boolean flag1 = (isel == 0? false: true);
		boolean flag2 = (isel == (parent0.expListCombo.getItemCount() -1)? false: true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
		
		if (isel >= 0 && listenerIndex != isel) 
		{
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null)
				exp.seqCamData.seq.addListener(this);
		}
		listenerIndex = isel;
	}
	
	boolean openExperimentFromCombo() 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return false;
		
		exp.xmlLoadMCExperiment();
		boolean flag = true;
		if (exp.seqCamData != null) 
		{
			exp.loadCamDataImages();
			exp.xmlLoadMCCapillaries_Only();
			exp.capillaries.transferCapillaryRoiToSequence(exp.seqCamData.seq);
			parent1.updateViewerForSequenceCam(exp);
			parent1.updateExpDialogs(exp);
			parent0.paneCapillaries.updateDialogs(exp);
			
			if (parent1.tabOptions.kymographsCheckBox.isSelected() && flag) 
				flag &= loadKymos(exp);
			if (parent1.tabOptions.measuresCheckBox.isSelected() && flag) 
				flag &= loadMeasures(exp);
			if (parent0.paneExperiment.tabOptions.graphsCheckBox.isSelected() && flag)
				displayGraphs(exp);
				
			if (parent1.tabOptions.cagesCheckBox.isSelected()) 
				parent0.paneCages.tabFile.loadCages(exp);
			
			parent0.paneLevels.updateDialogs(exp);
		}
		else 
		{
			flag = false;
			System.out.println("Error: no jpg files found for this experiment\n");
		}
		return flag;
	}
	
	private boolean loadKymos(Experiment exp) 
	{
		boolean flag = parent0.paneKymos.tabFile.loadDefaultKymos(exp);
		parent0.paneKymos.updateDialogs(exp);
		return flag;
	}
	
	private boolean loadMeasures(Experiment exp) 
	{
		boolean flag = parent0.paneLevels.tabFileLevels.loadCapillaries_Measures(exp);
		parent0.paneLevels.updateDialogs(exp);
		return flag;
	}
	
	private void displayGraphs(Experiment exp) 
	{
		SwingUtilities.invokeLater(new Runnable() { public void run() 
		{
			parent0.paneLevels.tabGraphs.xyDisplayGraphs(exp);
		}});	
	}
	
	// ------------------------
	
	private void defineActionListeners() 
	{
		parent0.expListCombo.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				updateBrowseInterface();
			}});
		
		searchButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	selectedNames = new ArrayList<String> ();
            	dialogSelect = new SelectFiles1();
            	dialogSelect.initialize(parent0);
            }});
		
		createButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	ExperimentDirectories eDAF = getDirectoriesFromDialog(null, true);
            	if (eDAF != null) 
            	{
	            	int item = addExperimentFrom3NamesAnd2Lists(eDAF);
	            	parent1.tabInfos.initCombosWithExpList();
	            	parent0.expListCombo.setSelectedIndex(item);
            	}
            }});
		
		openButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	ExperimentDirectories eDAF = getDirectoriesFromDialog(null, false);
            	if (eDAF != null) 
            	{
            		int item = addExperimentFrom3NamesAnd2Lists(eDAF);
            		parent1.tabInfos.initCombosWithExpList();
            		parent0.expListCombo.setSelectedIndex(item);
            	}
            }});
		
		closeButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				closeAllExperiments();
				parent1.tabsPane.setSelectedIndex(0);
				parent0.expListCombo.removeAllItems();
				parent0.expListCombo.updateUI();
			}});
		
		nextButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
			if (parent0.expListCombo.getSelectedIndex() < (parent0.expListCombo.getItemCount()-1)) 
				parent0.expListCombo.setSelectedIndex(parent0.expListCombo.getSelectedIndex()+1);
			else 
				updateBrowseInterface();
			}});
		
		previousButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
			if (parent0.expListCombo.getSelectedIndex() > 0) 
				parent0.expListCombo.setSelectedIndex(parent0.expListCombo.getSelectedIndex()-1);
			else 
				updateBrowseInterface();
			}});
		
		filteredCheck.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	parent1.tabFilter.filterExperimentList(filteredCheck.isSelected());
            }});
	}
	
	private int addExperimentFrom3NamesAnd2Lists(ExperimentDirectories eDAF) 
	{
		Experiment exp = new Experiment (eDAF);
		int item = parent0.expListCombo.addExperiment(exp, false);
		return item;
	}
		
	private ExperimentDirectories getDirectoriesFromDialog(String rootDirectory, boolean createResults)
	{
		ExperimentDirectories eDAF = new ExperimentDirectories();
		
		eDAF.cameraImagesList = ExperimentDirectories.getV2ImagesListFromDialog(rootDirectory);
		if (!checkCameraImagesList(eDAF)) 
			return null;
		
		eDAF.cameraImagesDirectory = Directories.getDirectoryFromName(eDAF.cameraImagesList.get(0));
		
		eDAF.resultsDirectory = getV2ResultsDirectoryDialog(eDAF.cameraImagesDirectory, Experiment.RESULTS, createResults);
		eDAF.binSubDirectory = getV2BinSubDirectory(eDAF.resultsDirectory, null);
		
		String kymosDir = eDAF.resultsDirectory + File.separator + eDAF.binSubDirectory;
		eDAF.kymosImagesList = ExperimentDirectories.getV2ImagesListFromPath(kymosDir);
		eDAF.kymosImagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(eDAF.kymosImagesList, "tiff");
		// TODO wrong if any bin
		return eDAF;
	}
	
	private boolean checkCameraImagesList(ExperimentDirectories eDAF) 
	{
		boolean isOK = false;
		if (!(eDAF.cameraImagesList == null)) 
		{
			boolean imageFound = false;
			String jpg = "jpg";
			String grabs = "grabs";
			String grabsDirectory = null;
			for (String name: eDAF.cameraImagesList) 
			{
				if (name.toLowerCase().endsWith(jpg)) 
				{
					imageFound = true;
					break;
				}
				if (name.toLowerCase().endsWith(grabs))
					grabsDirectory = name;
			}
			if (imageFound) 
			{
				eDAF.cameraImagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(eDAF.cameraImagesList, "jpg");
				isOK = true;
			}
			else if (grabsDirectory != null)
			{
				eDAF.cameraImagesList = ExperimentDirectories.getV2ImagesListFromPath(grabsDirectory);
				isOK = checkCameraImagesList(eDAF);
			}
		}
		return isOK;
	}
	
	private ExperimentDirectories getDirectoriesFromExptPath(String exptDirectory, String binSubDirectory)
	{
		ExperimentDirectories eDAF = new ExperimentDirectories();

		String strDirectory = Experiment.getImagesDirectoryAsParentFromFileName(exptDirectory);
		eDAF.cameraImagesList = ExperimentDirectories.getV2ImagesListFromPath(strDirectory);
		
		eDAF.cameraImagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(eDAF.cameraImagesList, "jpg");
		eDAF.cameraImagesDirectory = Directories.getDirectoryFromName(eDAF.cameraImagesList.get(0));
		
		eDAF.resultsDirectory =  getV2ResultsDirectory(eDAF.cameraImagesDirectory, exptDirectory);
		eDAF.binSubDirectory = getV2BinSubDirectory(eDAF.resultsDirectory, binSubDirectory);
		
		String kymosDir = eDAF.resultsDirectory + File.separator + eDAF.binSubDirectory;
		eDAF.kymosImagesList = ExperimentDirectories.getV2ImagesListFromPath(kymosDir);
		eDAF.kymosImagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(eDAF.kymosImagesList, "tiff"); 
		// TODO wrong if any bin
		return eDAF;
	}
	
	private String getV2ResultsDirectory(String parentDirectory, String resultsSubDirectory) 
	{
		 if (!resultsSubDirectory.contains(Experiment.RESULTS)) 
			 resultsSubDirectory = parentDirectory + File.separator + Experiment.RESULTS;
		
	    return resultsSubDirectory;
	}
	
	private String getV2BinSubDirectory(String parentDirectory, String binSubDirectory) 
	{
		List<String> expList = Directories.getSortedListOfSubDirectoriesWithTIFF(parentDirectory);
		cleanUpResultsDirectory(parentDirectory, expList);
		
	    String subDirectory = binSubDirectory;
	    if (subDirectory == null) 
	    {
		    if (expList.size() > 1) 
		    {
		    	if (parent0.expListCombo.expListBinSubDirectory == null)
		    		subDirectory = selectSubDirDialog(expList, "Select item", Experiment.BIN, false);
		    }
		    else if (expList.size() == 1 ) 
		    {
		    	subDirectory = expList.get(0); 
			    if (!subDirectory.contains(Experiment.BIN)) 
			    	subDirectory = Experiment.BIN + "60";
		    }
		    else 
		    	subDirectory = Experiment.BIN + "60";
	    }
	    if (parent0.expListCombo.expListBinSubDirectory != null) 
	    	subDirectory = parent0.expListCombo.expListBinSubDirectory;
	    return subDirectory;
	}
	
	private void cleanUpResultsDirectory(String parentDirectory, List <String> expList)
	{
		if (expList == null)
			return;
		for (String subDirectory: expList) 
		{
	    	if (subDirectory .contains(Experiment.RESULTS)) {
	    		subDirectory = Experiment.BIN + "60";
	    		Directories.move_TIFFfiles_ToSubdirectory(parentDirectory, subDirectory );
	    		Directories.move_xmlLINEfiles_ToSubdirectory(parentDirectory, subDirectory, true );
	    	}
		}
	}
	
	private String selectSubDirDialog(List<String> expList, String title, String type, boolean editable)
	{
		Object[] array = expList.toArray();
		JComboBox<Object> jcb = new JComboBox <Object> (array);
		jcb.setEditable(editable);
		JOptionPane.showMessageDialog( null, jcb, title, JOptionPane.QUESTION_MESSAGE);
		return (String) jcb.getSelectedItem();
	}
	
	private String getV2ResultsDirectoryDialog(String parentDirectory, String filter, boolean createResults) 
	{
		List<String> expList = Directories.fetchSubDirectoriesMatchingFilter(parentDirectory, filter);
		expList = Directories.reduceFullNameToLastDirectory(expList);
	    String name = null;
	    if (createResults || expList.size() > 1) 
	    {
	    	name = selectSubDirDialog(expList, "Select item or type "+Experiment.RESULTS+"xxx", Experiment.RESULTS, true);
	    }
	    else if (expList.size() == 1)
	    	name = expList.get(0);
	    else 
	    	name = filter;
	    return parentDirectory + File.separator + name;
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {

		if (sequenceEvent.getSourceType() == SequenceEventSourceType.SEQUENCE_DATA )
		{
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null)
			{
				if (exp.seqCamData.seq != null 
				&& sequenceEvent.getSequence() == exp.seqCamData.seq)
				{
					Viewer v = exp.seqCamData.seq.getFirstViewer();
					int t = v.getPositionT(); 
					v.setTitle(exp.seqCamData.getDecoratedImageName(t));
				}
				else if (exp.seqKymos.seq != null 
					&& sequenceEvent.getSequence() == exp.seqKymos.seq)
				{
					Viewer v = exp.seqKymos.seq.getFirstViewer();
					int t = v.getPositionT(); 
					String title = parent0.paneKymos.tabDisplay.getKymographTitle(t);
					v.setTitle(title);
				}
			}
		}
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequence.removeListener(this);
	}

	
}
