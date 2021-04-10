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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.SequenceNameListRenderer;
import plugins.fmp.multicafe.tools.Directories;

public class PanelFiles extends JPanel implements PropertyChangeListener, ItemListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -690874563607080412L;
	
	private JButton 		openButton		= new JButton("Open...");
	private JButton 		addButton		= new JButton("Add...");
	private JButton			searchButton 	= new JButton("Search...");
	private JButton			closeButton		= new JButton("Close");
	public List<String> 	selectedNames 	= new ArrayList<String> ();
	private SelectFiles 	dialogSelect 	= null;
	
	private JButton  		previousButton	= new JButton("<");
	private JButton			nextButton		= new JButton(">");
	private	SelectFiles2 	dialogSelect2 	= null;
	String					name			= null;
	String 					imagesDirectory	= null;
	
	private MultiCAFE 	parent0 = null;
	private MCExperiment_ parent1 = null;
	
//	
	JPanel initPanel( MultiCAFE parent0, MCExperiment_ parent1) 
	{
		this.parent0 = parent0;
		this.parent1 = parent1;

		SequenceNameListRenderer renderer = new SequenceNameListRenderer();
		parent0.expList.setRenderer(renderer);
		int bWidth = 26; 
		int height = 20;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		nextButton.setPreferredSize(new Dimension(bWidth, height));
		
		JPanel sequencePanel0 = new JPanel(new BorderLayout());
		sequencePanel0.add(previousButton, BorderLayout.LINE_START);
		sequencePanel0.add(parent0.expList, BorderLayout.CENTER);
//		parent0.expList.setPrototypeDisplayValue("XXXXxxxxxxxxxxxxxxxxx______________XXXXXXXXXXXXXXX");
		sequencePanel0.add(nextButton, BorderLayout.LINE_END);
		
		JPanel sequencePanel = new JPanel(new BorderLayout());
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		JPanel subPanel = new JPanel(layout);
		subPanel.add(openButton);
		subPanel.add(addButton);
		subPanel.add(searchButton);
		sequencePanel.add(subPanel, BorderLayout.LINE_START);
		sequencePanel.add(closeButton, BorderLayout.LINE_END);
	
		defineActionListeners();
		openButton.addPropertyChangeListener(parent1);
		addButton.addPropertyChangeListener(parent1);
		searchButton.addPropertyChangeListener(parent1);
		closeButton.addPropertyChangeListener(parent1);
		parent0.expList.addItemListener(this);
		
		JPanel twoLinesPanel = new JPanel (new GridLayout(2, 1));
		twoLinesPanel.add(sequencePanel0);
		twoLinesPanel.add(sequencePanel);
		return twoLinesPanel;
	}
	
	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("DIRECTORY_SELECTED")) 
		{
			name = imagesDirectory + File.separator + dialogSelect2.resultDirectory;
			dialogSelect2.close();
//			Experiment exp =(Experiment)  parent0.expList.getSelectedItem();
//			if (exp == null)
//				openSeqCamData(name);
//			loadMeasuresAndKymos(exp);
		}
		else if (evt.getPropertyName().equals("SEARCH_CLOSED")) 
		{
			int index = parent0.expList.getSelectedIndex();
			if (index < 0)
				index = 0;
			parent1.tabInfosSeq.disableChangeFile = true;
			for (String name: selectedNames) 
			{
				Experiment exp = new Experiment(name);
				parent0.expList.addItem(exp);
			}
			selectedNames.clear();
			if (parent0.expList.getItemCount() > 0) 
			{
				parent0.expList.setSelectedIndex(index);
				parent1.tabInfosSeq.disableChangeFile = false;
			}
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
        		parent0.paneExperiment.panelFiles.closeExp(exp); 
    		}});
		}
		updateBrowseInterface();
	}
	
	void closeAll() 
	{
		closeCurrentExperiment();
		parent0.expList.removeAllItems();
	}
	
	public void closeExp(Experiment exp) 
	{
		if (exp != null) 
		{
			parent0.paneExperiment.tabInfosSeq.getExperimentInfosFromDialog(exp);
			if (exp.seqCamData != null) 
			{
				exp.xmlSaveMCExperiment();
				exp.saveExperimentMeasures(exp.getKymosDirectory());
			}
			exp.closeExperiment();
		}
		parent0.paneCages.tabGraphics.closeAll();
		parent0.paneLevels.tabGraphs.closeAll();
		parent0.paneKymos.tabDisplay.kymosComboBox.removeAllItems();
	}
	
	public void closeCurrentExperiment() 
	{
		if (parent0.expList.getSelectedIndex() < 0)
			return;
		Experiment exp =(Experiment)  parent0.expList.getSelectedItem();
		if (exp != null)
			closeExp(exp);
	}
	
	void updateBrowseInterface() 
	{
		int isel = parent0.expList.getSelectedIndex();		
	    boolean flag1 = (isel == 0? false: true);
		boolean flag2 = (isel == (parent0.expList.getItemCount() -1)? false: true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
	}
	
	boolean openExperimentFromCombo() 
	{
		Experiment exp = (Experiment) parent0.expList.getSelectedItem();
		boolean flag = true;
		if (exp.seqCamData != null) {
			parent1.updateDialogs(exp);
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
	
	public void openExperiment(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.openSequenceCamData();
		if (exp.seqCamData != null && exp.seqCamData.seq != null) 
		{
			parent0.addSequence(exp.seqCamData.seq);
			parent1.updateViewerForSequenceCam(exp);
			loadMeasuresAndKymos(exp);
			parent0.paneKymos.tabDisplay.updateResultsAvailable(exp);
		}
	}
	
	void loadMeasuresAndKymos(Experiment exp) 
	{
		if (exp == null)
			return;
		parent0.paneCapillaries.tabFile.loadCapillaries_File(exp);
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
				}
				if (parent0.paneExperiment.tabOptions.graphsCheckBox.isSelected())
					SwingUtilities.invokeLater(new Runnable() { public void run() 
					{
						parent0.paneLevels.tabGraphs.xyDisplayGraphs(exp);
					}});
			}
		}
		if (parent1.tabOptions.cagesCheckBox.isSelected()) 
		{
			parent0.paneCages.tabFile.loadCages(exp);
		}
	}
	// ------------------------
	
	private void defineActionListeners() 
	{
		parent0.expList.addActionListener(new ActionListener () 
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
            	dialogSelect = new SelectFiles();
            	dialogSelect.initialize(parent0);
            }});
		
		openButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	closeAll();
    			parent0.expList.removeAllItems();
    			parent0.expList.updateUI();
    			Experiment exp = getDirectoriesFromSourceName();
    			int item = parent0.expList.addExperiment(exp);
    			parent0.expList.setSelectedIndex(item);
            }});
		
		addButton.addActionListener(new ActionListener()  
		{
            @Override
            public void actionPerformed(ActionEvent arg0) 
            {
            	Experiment exp = getDirectoriesFromSourceName();
    			int item = parent0.expList.addExperiment(exp);
    			parent0.expList.setSelectedIndex(item);
            }});
		
		closeButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				closeAll();
				parent1.tabsPane.setSelectedIndex(0);
				parent0.expList.removeAllItems();
				parent0.expList.updateUI();
			}});
		
		nextButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
			if (parent0.expList.getSelectedIndex() < (parent0.expList.getItemCount() -1)) 
				parent0.expList.setSelectedIndex(parent0.expList.getSelectedIndex()+1);
			updateBrowseInterface();
			}});
		
		previousButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
			if (parent0.expList.getSelectedIndex() > 0) 
				parent0.expList.setSelectedIndex(parent0.expList.getSelectedIndex()-1);
			updateBrowseInterface();
			}});
	}
	
	private Experiment getDirectoriesFromSourceName()
	{
		List<String> imagesList = SequenceCamData.getV2ImagesListFromDialog(null);
		String imagesDirectory = Directories.clipNameToDirectory(imagesList.get(0));
		String resultsDirectory = getV2ResultsDirectoryDialog(imagesDirectory, Experiment.RESULTS);
		String binDirectory = getV2BinDirectoryDialog(resultsDirectory);
	    Experiment exp = new Experiment (imagesList, resultsDirectory, binDirectory);
	    return exp;
	}
	
	private String getV2BinDirectoryDialog(String parentDirectory) 
	{
		List<String> expList = Directories.getSortedListOfSubDirectoriesWithTIFF(parentDirectory);
	    String name = parentDirectory;
	    if (expList.size() > 1) 
	    {
	    	dialogSelect2 = new SelectFiles2();
	    	dialogSelect2.addPropertyChangeListener(this);
	    	dialogSelect2.initialize(expList);
	    }
	    else 
	    {
	    	name += File.separator + Experiment.BIN+ "60";
	    }
	    return name;
	}
	
	private String getV2ResultsDirectoryDialog(String parentDirectory, String filter) 
	{
		List<String> expList = Directories.fetchSubDirectoriesMatchingFilter(parentDirectory, filter);
	    String name = parentDirectory;
	    if (expList.size() > 1) 
	    {
	    	dialogSelect2 = new SelectFiles2();
	    	dialogSelect2.addPropertyChangeListener(this);
	    	dialogSelect2.initialize(expList);
	    }
	    else 
	    {
	    	name += File.separator + filter;
	    }
	    return name;
	}



	
}
