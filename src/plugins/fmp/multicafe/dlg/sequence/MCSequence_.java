package plugins.fmp.multicafe.dlg.sequence;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import icy.gui.component.PopupPanel;
import icy.gui.frame.IcyFrame;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.SequenceNameListRenderer;
import plugins.fmp.multicafe.tools.Directories;



public class MCSequence_ extends JPanel implements PropertyChangeListener, ItemListener, ViewerListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6826269677524125173L;
	
	public	PopupPanel 		capPopupPanel	= null;
	public 	JTabbedPane 	tabsPane 		= new JTabbedPane();
	public 	Open 			tabOpen 		= new Open();
	public 	Infos			tabInfosSeq		= new Infos();
	public 	Intervals		tabIntervals	= new Intervals();
	public 	Analyze			tabAnalyze		= new Analyze();
	public 	Display			tabDisplay 		= new Display();
	public 	Close 			tabClose 		= new Close();
//	private JLabel			text 			= new JLabel("Experiment ");
	private JButton  		previousButton	= new JButton("<");
	private JButton			nextButton		= new JButton(">");
//	private JButton			clearButton  	= new JButton("Close");
	
	private MultiCAFE 		parent0 		= null;
	private	SelectFiles2 	dialogSelect2 	= null;
			String			name			= null;
			String 			imagesDirectory	= null;
	
	
	
	public void init (JPanel mainPanel, String string, MultiCAFE parent0) 
	{
		this.parent0 = parent0;
		
		SequenceNameListRenderer renderer = new SequenceNameListRenderer();
		parent0.expList.setRenderer(renderer);
		int bWidth = 26; //28;
		int height = 20;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		nextButton.setPreferredSize(new Dimension(bWidth, height));
		
		JPanel sequencePanel2 = new JPanel(new BorderLayout());
		sequencePanel2.add(previousButton, BorderLayout.LINE_START);
		sequencePanel2.add(parent0.expList, BorderLayout.CENTER);
//		parent0.expList.setPrototypeDisplayValue("XXXXxxxxxxxxxxxxxxxxx______________XXXXXXXXXXXXXXX");
		sequencePanel2.add(nextButton, BorderLayout.LINE_END);
		
//		JPanel sequencePanel = new JPanel(new BorderLayout());
//		sequencePanel.add(text, BorderLayout.LINE_START);
//		sequencePanel.add(clearButton, BorderLayout.LINE_END);

		
		capPopupPanel = new PopupPanel(string);			
		capPopupPanel.expand();
		mainPanel.add(capPopupPanel);
		GridLayout tabsLayout = new GridLayout(3, 1);
		
		tabOpen.init(tabsLayout, parent0);
		tabsPane.addTab("Open", null, tabOpen, "Open one or several stacks of .jpg files");
		tabOpen.addPropertyChangeListener(this);
		
		tabInfosSeq.init(tabsLayout, parent0);
		tabsPane.addTab("Infos", null, tabInfosSeq, "Define infos for this experiment/box");
		tabInfosSeq.addPropertyChangeListener(this);
		
		tabIntervals.init(tabsLayout, parent0);
		tabsPane.addTab("Intervals", null, tabIntervals, "View/define stack image intervals");
		tabIntervals.addPropertyChangeListener(this);
		
		tabAnalyze.init(tabsLayout);
		tabsPane.addTab("Analyze", null, tabAnalyze, "Define analysis intervals");
		tabAnalyze.addPropertyChangeListener(this);

		tabDisplay.init(tabsLayout, parent0);
		tabsPane.addTab("Display", null, tabDisplay, "Display ROIs");
		tabDisplay.addPropertyChangeListener(this);

		tabClose.init(tabsLayout, parent0);
		tabsPane.addTab("Close", null, tabClose, "Close file and associated windows");
		tabClose.addPropertyChangeListener(this);

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPanel.add(sequencePanel2, BorderLayout.PAGE_START);
//		capPanel.add(sequencePanel, BorderLayout.CENTER);
		capPanel.add(tabsPane, BorderLayout.PAGE_END);	
		
		capPopupPanel.addComponentListener(new ComponentAdapter() 
		{
			@Override
			public void componentResized(ComponentEvent e) 
			{
				parent0.mainFrame.revalidate();
				parent0.mainFrame.pack();
				parent0.mainFrame.repaint();
			}
		});
		
		parent0.expList.addItemListener(this);
		
		defineActionListeners();		
	}
	
	private void defineActionListeners() 
	{
		parent0.expList.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				updateBrowseInterface();
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
	
	@Override
	public void propertyChange(PropertyChangeEvent event) 
	{
		if (event.getPropertyName() .equals ("SEQ_OPENFILE")) 
		{
			tabClose.closeAll();
			parent0.expList.removeAllItems();
			parent0.expList.updateUI();
			addExperiment();
		}
		else if (event.getPropertyName().equals("SEQ_ADDFILE")) 
		{
			int item = addExperiment();
			parent0.expList.setSelectedIndex(item);
		}
		else if (event.getPropertyName().equals("SEQ_CLOSE")) 
		{
			System.out.println("SEQ_CLOSE");
		}
		else if (event.getPropertyName().equals("CLOSE_ALL")) 
		{
			tabsPane.setSelectedIndex(0);
			parent0.expList.removeAllItems();
			parent0.expList.updateUI();
		}
		else if (event.getPropertyName().equals("SEARCH_CLOSED")) 
		{
			int index = parent0.expList.getSelectedIndex();
			if (index < 0)
				index = 0;
			tabInfosSeq.disableChangeFile = true;
			for (String name: tabOpen.selectedNames) 
			{
				Experiment exp = new Experiment(name);
				parent0.expList.addItem(exp);
			}
			tabOpen.selectedNames.clear();
			if (parent0.expList.getItemCount() > 0) 
			{
				parent0.expList.setSelectedIndex(index);
				tabInfosSeq.disableChangeFile = false;
			}
		}
		else if (event.getPropertyName().equals("DIRECTORY_SELECTED")) 
		{
			name = imagesDirectory + File.separator + dialogSelect2.resultDirectory;
			dialogSelect2.close();
//			Experiment exp =(Experiment)  parent0.expList.getSelectedItem();
//			if (exp == null)
//				openSeqCamData(name);
//			loadMeasuresAndKymos(exp);
		}
	}
	
	private int addExperiment()
	{
		List<String> imagesList = SequenceCamData.getV2ImagesListFromDialog(null);
		String imagesDirectory = Directories.clipNameToDirectory(imagesList.get(0));
		String resultsDirectory = getV2ResultsDirectoryDialog(imagesDirectory, Experiment.RESULTS);
		String binDirectory = getV2BinDirectoryDialog(resultsDirectory);
	    Experiment exp = new Experiment (imagesList, resultsDirectory, binDirectory);
	    return parent0.expList.addExperiment(exp);
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
	

	public void openExperiment(Experiment exp) 
	{
		exp.xmlLoadMCExperiment();
		exp.openSequenceCamData();
		if (exp.seqCamData != null && exp.seqCamData.seq != null) 
		{
			parent0.addSequence(exp.seqCamData.seq);
			updateViewerForSequenceCam(exp);
			loadMeasuresAndKymos(exp);
			parent0.paneKymos.tabDisplay.updateResultsAvailable(exp);
		}
	}
	
	boolean openExperimentFromCombo() 
	{
		Experiment exp = (Experiment) parent0.expList.getSelectedItem();
		boolean flag = true;
		if (exp.seqCamData != null) {
			updateDialogs(exp);
			loadMeasuresAndKymos(exp);
			parent0.paneLevels.updateDialogs(exp);
			tabsPane.setSelectedIndex(1);
		}
		else 
		{
			flag = false;
			System.out.println("Error: no jpg files found for this experiment\n");
		}
		return flag;
	}
		
	public void updateDialogs(Experiment exp) 
	{
		tabIntervals.displayCamDataIntervals(exp);
		tabInfosSeq.setExperimentsInfosToDialog(exp);

		updateViewerForSequenceCam(exp);
		parent0.paneKymos.tabDisplay.updateResultsAvailable(exp);
	}

	void loadMeasuresAndKymos(Experiment exp) 
	{
		if (exp == null)
			return;
		parent0.paneCapillaries.tabFile.loadCapillaries_File(exp);
		parent0.paneCapillaries.updateDialogs(exp);
		
		if (tabOpen.isCheckedLoadKymographs()) 
		{
			boolean flag = parent0.paneKymos.tabFile.loadDefaultKymos(exp);
			parent0.paneKymos.updateDialogs(exp);
			if (flag) { 
				if (tabOpen.isCheckedLoadMeasures()) 
				{
					parent0.paneLevels.tabFileLevels.loadCapillaries_Measures(exp);
					parent0.paneLevels.updateDialogs(exp);
				}
				if (parent0.paneSequence.tabOpen.graphsCheckBox.isSelected())
					SwingUtilities.invokeLater(new Runnable() { public void run() 
					{
						parent0.paneLevels.tabGraphs.xyDisplayGraphs(exp);
					}});
			}
		}
		if (tabOpen.isCheckedLoadCages()) 
		{
			parent0.paneCages.tabFile.loadCages(exp);
		}
	}
	
	public void getExperimentInfosFromDialog(Experiment exp) 
	{
		tabInfosSeq.getExperimentInfosFromDialog(exp);
	}
	
	void updateBrowseInterface() 
	{
		int isel = parent0.expList.getSelectedIndex();		
	    boolean flag1 = (isel == 0? false: true);
		boolean flag2 = (isel == (parent0.expList.getItemCount() -1)? false: true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			tabInfosSeq.updateCombos();
			openExperimentFromCombo();
		} else {
			Experiment exp = (Experiment) e.getItem();
			ThreadUtil.bgRun( new Runnable() { @Override public void run() 
    		{
        		parent0.paneSequence.tabClose.closeExp(exp); 
    		}});
		}
		updateBrowseInterface();
	}
	
	public Experiment openExperimentFromString(String filename) 
	{
		Experiment exp = parent0.expList.getExperimentFromExptName(filename);
		if (exp == null) 
		{
			exp = new Experiment(filename);
			parent0.expList.addExperiment(exp);
		}
		exp.setExperimentDirectory(filename);
		exp.setImagesDirectory(Experiment.getImagesDirectoryAsParentFromFileName(filename));
		exp.openSequenceCamData();
		if (exp.seqCamData != null && exp.seqCamData.seq != null) 
		{
			updateViewerForSequenceCam(exp);
		} 
		else 
		{
			System.out.println("seqCamData or seq of seqCamData is null!");
		}
		return exp;
	}
	
	public void updateViewerForSequenceCam(Experiment exp) 
	{
		Sequence seq = exp.seqCamData.seq;
		Viewer v = seq.getFirstViewer();
		if (v == null) 
		{
			v = new Viewer(exp.seqCamData.seq, true);
		}
		if (v != null) {
			placeViewerNextToDialogBox(v, parent0.mainFrame);
			v.toFront();
			v.requestFocus();
			v.addListener( this );
			v.setTitle(exp.seqCamData.getDecoratedImageName(0));
		}
	}
	
	private void placeViewerNextToDialogBox(Viewer v, IcyFrame mainFrame) 
	{
		Rectangle rectv = v.getBoundsInternal();
		Rectangle rect0 = mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		v.setBounds(rectv);
	}

	@Override	
	public void viewerChanged(ViewerEvent event) 
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED)) 
		{
			if (event.getDim() == DimensionId.T) 
			{
				Viewer v = event.getSource(); 
				int idViewer = v.getSequence().getId(); 
				Experiment exp = (Experiment) parent0.expList.getSelectedItem();
				int idCurrentExp = exp.seqCamData.seq.getId();
				if (idViewer == idCurrentExp) 
				{
					int t = v.getPositionT(); 
					v.setTitle(exp.seqCamData.getDecoratedImageName(t));
					if (parent0.paneCages.bTrapROIsEdit) 
						exp.saveDetRoisToPositions();
					exp.updateROIsAt(t);
				}
			}
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) 
	{
		viewer.removeListener(this);
	}
}
