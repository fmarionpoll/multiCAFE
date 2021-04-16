package plugins.fmp.multicafe.dlg.experiment;

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

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.dlg.JComponents.SequenceNameListRenderer;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.ExperimentDirectories;
import plugins.fmp.multicafe.tools.Directories;

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
	public List<String> 	selectedNames 	= new ArrayList<String> ();
	private SelectFiles1 	dialogSelect 	= null;
	
	private JButton  		previousButton	= new JButton("<");
	private JButton			nextButton		= new JButton(">");

	private MultiCAFE 		parent0 		= null;
	private MCExperiment_ 	parent1 		= null;
	private int 			listenerIndex 	= -1;
	
	

	JPanel initPanel( MultiCAFE parent0, MCExperiment_ parent1) 
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
		layout.setVgap(0);
		JPanel subPanel = new JPanel(layout);
		subPanel.add(openButton);
		subPanel.add(searchButton);
		subPanel.add(createButton);
		sequencePanel.add(subPanel, BorderLayout.LINE_START);
		sequencePanel.add(closeButton, BorderLayout.LINE_END);
	
		defineActionListeners();
		parent0.expListCombo.addItemListener(this);
		
		JPanel twoLinesPanel = new JPanel (new GridLayout(2, 1));
		twoLinesPanel.add(sequencePanel0);
		twoLinesPanel.add(sequencePanel);
		
		// TODO implement and test
		createButton.setEnabled(false);
		
		return twoLinesPanel;
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("SELECT1_CLOSED")) 
		{
			parent1.tabInfosSeq.disableChangeFile = true;
			if (selectedNames.size() < 1)
				return;
			
			ExperimentDirectories eDAF = getDirectoriesFromExptPath(selectedNames.get(0), null);
        	int index = addExperimentFrom3NamesAnd2Lists(eDAF);
        	parent0.expListCombo.setSelectedIndex(index);
        	final String binSubDirectory = parent0.expListCombo.expListBinSubPath;
        	
        	SwingUtilities.invokeLater(new Runnable() { public void run() 
			{	
				for (int i=1; i < selectedNames.size(); i++) 
				{
					ExperimentDirectories eDAF = getDirectoriesFromExptPath(selectedNames.get(i), binSubDirectory);
		        	addExperimentFrom3NamesAnd2Lists(eDAF);
				}
				selectedNames.clear();
				parent1.tabInfosSeq.disableChangeFile = false;
				updateBrowseInterface();
				}});
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			parent1.tabInfosSeq.updateCombos();
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
	}
	
	public void closeViewsForCurrentExperiment(Experiment exp) 
	{
		if (exp != null) 
		{
			parent0.paneExperiment.tabInfosSeq.getExperimentInfosFromDialog(exp);
			if (exp.seqCamData != null) 
			{
				exp.xmlSaveMCExperiment();
				exp.saveExperimentMeasures(exp.getKymosBinFullDirectory());
			}
			exp.closeSequences();
		}
		parent0.paneCages.tabGraphics.closeAllCharts();
		parent0.paneLevels.tabGraphs.closeAllCharts();
		parent0.paneKymos.tabDisplay.imagesComboBox.removeAllItems();
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
			exp.seqCamData.seq.addListener(this);
		}
		listenerIndex = isel;
	}
	
	boolean openExperimentFromCombo() 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		boolean flag = true;
		if (exp.seqCamData != null) 
		{
			exp.loadCamDataImages();
			exp.xmlLoadMCCapillaries_Only();
			exp.capillaries.transferCapillaryRoiToSequence(exp.seqCamData.seq);
			parent1.updateViewerForSequenceCam(exp);
			
			parent1.updateExpDialogs(exp);
			loadMeasuresAndKymos(exp);
			parent0.paneLevels.updateDialogs(exp);
		}
		else 
		{
			flag = false;
			System.out.println("Error: no jpg files found for this experiment\n");
		}
		return flag;
	}
	
	void loadMeasuresAndKymos(Experiment exp) 
	{
		if (exp == null)
			return;
		parent0.paneCapillaries.updateDialogs(exp);
		if (parent1.tabOptions.kymographsCheckBox.isSelected()) 
		{
			boolean flag = parent0.paneKymos.tabFile.loadDefaultKymos(exp);
			parent0.paneKymos.updateDialogs(exp);
			if (flag) { 
				if (parent1.tabOptions.measuresCheckBox.isSelected()) 
				{
					parent0.paneLevels.tabFileLevels.loadCapillaries_Measures(exp);
					parent0.paneLevels.updateDialogs(exp);
					if (parent0.paneExperiment.tabOptions.graphsCheckBox.isSelected())
						SwingUtilities.invokeLater(new Runnable() { public void run() 
						{
							parent0.paneLevels.tabGraphs.xyDisplayGraphs(exp);
						}});
				}
			}
		}
		if (parent1.tabOptions.cagesCheckBox.isSelected()) 
			parent0.paneCages.tabFile.loadCages(exp);
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
            	ExperimentDirectories eDAF = getDirectoriesFromDialog(null);
            	if (eDAF != null) 
            	{
	            	int item = addExperimentFrom3NamesAnd2Lists(eDAF);
	            	parent0.expListCombo.setSelectedIndex(item);
            	}
            }});
		
		openButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	ExperimentDirectories eDAF = getDirectoriesFromDialog(null);
            	if (eDAF != null) 
            	{
            		int item = addExperimentFrom3NamesAnd2Lists(eDAF);
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
			if (parent0.expListCombo.getSelectedIndex() < (parent0.expListCombo.getItemCount() -1)) 
				parent0.expListCombo.setSelectedIndex(parent0.expListCombo.getSelectedIndex()+1);
			updateBrowseInterface();
			}});
		
		previousButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
			if (parent0.expListCombo.getSelectedIndex() > 0) 
				parent0.expListCombo.setSelectedIndex(parent0.expListCombo.getSelectedIndex()-1);
			updateBrowseInterface();
			}});
	}
	
	private int addExperimentFrom3NamesAnd2Lists(ExperimentDirectories eDAF) 
	{
		Experiment exp = new Experiment (eDAF);
		int item = parent0.expListCombo.addExperiment(exp);
		return item;
	}
	
	private ExperimentDirectories getDirectoriesFromDialog(String rootDirectory)
	{
		ExperimentDirectories eDAF = new ExperimentDirectories();
		
		eDAF.cameraImagesList = ExperimentDirectories.getV2ImagesListFromDialog(rootDirectory);
		if (eDAF.cameraImagesList == null || eDAF.cameraImagesList.size() < 1)
			return null;
		eDAF.cameraImagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(eDAF.cameraImagesList, "jpg");
		eDAF.cameraImagesDirectory = Directories.getDirectoryFromName(eDAF.cameraImagesList.get(0));
		
		eDAF.resultsDirectory = getV2ResultsDirectoryDialog(eDAF.cameraImagesDirectory, Experiment.RESULTS);
		eDAF.binSubDirectory = updateV2BinSubDirectory(eDAF.resultsDirectory);
		String kymosDir = eDAF.resultsDirectory + File.separator + eDAF.binSubDirectory;
		eDAF.kymosImagesList = ExperimentDirectories.getV2ImagesListFromPath(kymosDir);
		eDAF.kymosImagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(eDAF.kymosImagesList, "tiff");
		// TODO wrong if any bin
		return eDAF;
	}
	
	private ExperimentDirectories getDirectoriesFromExptPath(String exptDirectory, String binSubDirectory)
	{
		ExperimentDirectories eDAF = new ExperimentDirectories();

		String strDirectory = Experiment.getImagesDirectoryAsParentFromFileName(exptDirectory);
		eDAF.cameraImagesList = ExperimentDirectories.getV2ImagesListFromPath(strDirectory);
		eDAF.cameraImagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(eDAF.cameraImagesList, "jpg");
		eDAF.cameraImagesDirectory = Directories.getDirectoryFromName(eDAF.cameraImagesList.get(0));
		
		eDAF.resultsDirectory = exptDirectory;
		if (binSubDirectory == null)
			eDAF.binSubDirectory = updateV2BinSubDirectory(eDAF.resultsDirectory);
		else 
			eDAF.binSubDirectory = binSubDirectory;
		String kymosDir = eDAF.resultsDirectory + File.separator + eDAF.binSubDirectory;
		eDAF.kymosImagesList = ExperimentDirectories.getV2ImagesListFromPath(kymosDir);
		eDAF.kymosImagesList = ExperimentDirectories.keepOnlyAcceptedNames_List(eDAF.kymosImagesList, "tiff");
		// TODO wrong if any bin
		return eDAF;
	}
	
	private String updateV2BinSubDirectory(String parentDirectory) 
	{
		List<String> expList = Directories.getSortedListOfSubDirectoriesWithTIFF(parentDirectory);
		cleanUpResultsDirectory(parentDirectory, expList);
		
	    String subDirectory = null;
	    if (expList.size() > 1) {
	    	if (parent0.expListCombo.expListBinSubPath == null)
	    		subDirectory = selectSubDirDialog(expList, "Select item", Experiment.BIN, false);
	    }
	    else if (expList.size() == 1) {
	    	subDirectory = expList.get(0);
		    if (!subDirectory.contains(Experiment.BIN)) 
		    	Directories.moveTIFFAndLINEfilesToSubdirectory(parentDirectory, subDirectory );
	    }
	    else 
	    	subDirectory = Experiment.BIN + "60";
	    if (parent0.expListCombo.expListBinSubPath != null) 
	    	subDirectory = parent0.expListCombo.expListBinSubPath;
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
	    		Directories.moveTIFFAndLINEfilesToSubdirectory(parentDirectory, subDirectory );
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
	
	private String getV2ResultsDirectoryDialog(String parentDirectory, String filter) 
	{
		List<String> expList = Directories.fetchSubDirectoriesMatchingFilter(parentDirectory, filter);
	    String name = null;
	    if (expList.size() > 1) 
	    	name = selectSubDirDialog(expList, "Select item or type "+Experiment.RESULTS+"xxx", Experiment.RESULTS, true);
	    else if (expList.size() == 1)
	    	name = expList.get(0);
	    else 
	    	name = parentDirectory + File.separator + filter;
	    return name;
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) {

		if (sequenceEvent.getSourceType() == SequenceEventSourceType.SEQUENCE_DATA )
		{
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
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

	@Override
	public void sequenceClosed(Sequence sequence) {
		sequence.removeListener(this);
	}

	
}