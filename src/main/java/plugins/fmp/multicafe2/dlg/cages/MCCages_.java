package plugins.fmp.multicafe2.dlg.cages;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.component.PopupPanel;
import plugins.fmp.multicafe2.MultiCAFE2;



public class MCCages_ extends JPanel implements PropertyChangeListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3457738144388946607L;
	
			BuildROIs 		tabBuildROIs	= new BuildROIs();
			BuildROIs2 		tabBuildROIs2	= new BuildROIs2();
			Display			tabInfos		= new Display();
			Detect1 		tabDetect1 		= new Detect1();
			Detect2 		tabDetect2 		= new Detect2();
			Detect3 		tabDetect3 		= new Detect3();
			Edit			tabEdit			= new Edit();
	public 	LoadSave 		tabFile 		= new LoadSave();
	public 	CageGraphs 		tabGraphics 	= new CageGraphs();
	public	PopupPanel 		capPopupPanel	= null;
			JTabbedPane 	tabsPane		= new JTabbedPane();
			int				previouslySelected	= -1;
	public 	boolean			bTrapROIsEdit	= false;
			int 			iTAB_CAGE2		= 1;
			int 			iTAB_INFOS 		= iTAB_CAGE2+1;
			int 			iTAB_DETECT1	= iTAB_INFOS+1;
			int 			iTAB_DETECT2	= iTAB_DETECT1+1;
			int				iTAB_DETECT3	= iTAB_DETECT2+1;
			int				iTAB_EDIT		= iTAB_DETECT3+1;
			
			MultiCAFE2 		parent0			= null;

	
	public void init (JPanel mainPanel, String string, MultiCAFE2 parent0) 
	{
		this.parent0 = parent0;
		
		capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		
		mainPanel.add(capPopupPanel);
		GridLayout capLayout = new GridLayout(4, 1);
		
		int iTab = 0;
		tabBuildROIs.init(capLayout, parent0);
		tabBuildROIs.addPropertyChangeListener(this);
		tabsPane.addTab("Cages", null, tabBuildROIs, "Define cages using ROI polygons placed over each cage");
		
		iTab++;
		iTAB_CAGE2	= iTab;
		tabBuildROIs2.init(capLayout, parent0);
		tabBuildROIs2.addPropertyChangeListener(this);
		tabsPane.addTab("Cages (bis)", null, tabBuildROIs2, "Define cages using limits detection within a user drawn ROI polygon");

		iTab++;
		iTAB_INFOS = iTab;
		tabInfos.init(parent0);
		tabInfos.addPropertyChangeListener(this);
		tabsPane.addTab("Infos", null, tabInfos, "Display infos about cages and flies positions");
		
		iTab++;
		iTAB_DETECT1 = iTab;
		tabDetect1.init(capLayout, parent0);
		tabDetect1.addPropertyChangeListener(this);
		tabsPane.addTab("Detect1", null, tabDetect1, "Detect flies position using thresholding on image overlay");
		
		iTab++;
		iTAB_DETECT2 = iTab;
		tabDetect2.init(capLayout, parent0);
		tabDetect2.addPropertyChangeListener(this);
		tabsPane.addTab("Detect2", null, tabDetect2, "Detect flies position using background subtraction");
		
		iTab++;
		iTAB_DETECT3 = iTab;
		tabDetect3.init(capLayout, parent0);
		tabDetect3.addPropertyChangeListener(this);
		tabsPane.addTab("Detect3", null, tabDetect3, "Build background without flies");
		
		iTab++;
		iTAB_EDIT	= iTab;
		tabEdit.init(capLayout, parent0);
		tabEdit.addPropertyChangeListener(this);
		tabsPane.addTab("Edit", null, tabEdit, "Edit flies detection");
	
		iTab++;
		tabGraphics.init(capLayout, parent0);		
		tabGraphics.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, tabGraphics, "Display results as graphics");

		iTab++;
		tabFile.init(capLayout, parent0);
		tabFile.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, tabFile, "Load/save cages and flies position");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(tabsPane);
		tabsPane.setSelectedIndex(0);
		
		tabsPane.addChangeListener(new ChangeListener() {
			@Override 
	        public void stateChanged(ChangeEvent e) 
			{
	            int selectedIndex = tabsPane.getSelectedIndex();
	            tabDetect1.overlayCheckBox.setSelected(selectedIndex == iTAB_DETECT1);
	            if (selectedIndex == iTAB_DETECT1 || selectedIndex == iTAB_DETECT2) 
	            {
	            	parent0.paneExperiment.capPopupPanel.expand();
	    			parent0.paneExperiment.tabsPane.setSelectedIndex(0);
	            }
	            
	            if (selectedIndex == iTAB_EDIT) 
	            {
	            	bTrapROIsEdit = true;
	            	parent0.paneExperiment.tabOptions.displayROIsCategory (false, "line");
	            	parent0.paneExperiment.tabOptions.displayROIsCategory(false, "cage");
	            } 
	            else 
	            {
	            	if (bTrapROIsEdit) 
	            	{
	            		parent0.paneExperiment.tabOptions.displayROIsCategory (parent0.paneExperiment.tabOptions.viewCapillariesCheckBox.isSelected(), "line");
		            	parent0.paneExperiment.tabOptions.displayROIsCategory(parent0.paneExperiment.tabOptions.viewCagesCheckbox.isSelected(), "cage");
	            	}
	            	bTrapROIsEdit = false;
	            }

	            boolean activateOverlay = false;
	            if (selectedIndex == iTAB_CAGE2) 
	            	activateOverlay = true;
	            tabBuildROIs2.overlayCheckBox.setSelected(activateOverlay);
	            previouslySelected = selectedIndex;
	        }});
		
		capPopupPanel.addComponentListener(new ComponentAdapter() 
		{
			@Override
			public void componentResized(ComponentEvent e) 
			{
				parent0.mainFrame.revalidate();
				parent0.mainFrame.pack();
				parent0.mainFrame.repaint();
			}});
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		if (evt.getPropertyName().equals("LOAD_DATA")) 
		{
			tabBuildROIs.updateNColumnsFieldFromSequence();
		}
	}

	
	
}

