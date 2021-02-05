package plugins.fmp.multicafe.dlg.capillaries;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.component.PopupPanel;
import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.SequenceKymosUtils;



public class MCCapillaries_ extends JPanel implements PropertyChangeListener, ChangeListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 853047648249832145L;
	public	PopupPanel 	capPopupPanel	= null;
			JTabbedPane tabsPane 		= new JTabbedPane();
			Create 		tabCreate 		= new Create();
	public 	LoadSave 	tabFile 		= new LoadSave();
			Adjust 		tabAdjust 		= new Adjust();
			Infos		tabInfos		= new Infos();
	private int 		ID_INFOS 		= 1;
	private int 		ID_ADJUST 		= 2;
	private MultiCAFE 	parent0 		= null;

	
	public void init (JPanel mainPanel, String string, MultiCAFE parent0) 
	{
		this.parent0 = parent0;
		capPopupPanel = new PopupPanel(string);
		
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(capPopupPanel);
		
		GridLayout capLayout = new GridLayout(3, 1);
		int order = 0;
		tabCreate.init(capLayout, parent0);
		tabCreate.addPropertyChangeListener(this);
		tabsPane.addTab("Create", null, tabCreate, "Create lines defining capillaries");
		order++;
		
		ID_INFOS=order;
		tabInfos.init(capLayout, parent0);
		tabInfos.addPropertyChangeListener(this);
		tabsPane.addTab("Infos", null, tabInfos, "Define pixel conversion unit of images and capillaries content");
		order++;
		
		tabAdjust.init(capLayout, parent0);
		tabAdjust.addPropertyChangeListener(this);
		tabsPane.addTab("Adjust", null, tabAdjust, "Adjust ROIS position to the capillaries");
		order++;
		
		tabFile.init(capLayout, parent0);
		tabFile.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, tabFile, "Load/Save xml file with capillaries descriptors");
		order++;
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(tabsPane);
		tabsPane.addChangeListener(this );
		
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
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) 
	{
		if (event.getPropertyName().equals("CAP_ROIS_OPEN")) 
		{
			Experiment exp = parent0.expList.getCurrentExperiment();
			displayCapillariesInformation(exp);
		  	tabsPane.setSelectedIndex(ID_INFOS);
		  	parent0.paneSequence.tabIntervals.displayCamDataIntervals(exp);
		}			  
		else if (event.getPropertyName().equals("CAP_ROIS_SAVE")) 
		{
			tabsPane.setSelectedIndex(ID_INFOS);
		}
		else if (event.getPropertyName().equals("CAPILLARIES_NEW")) 
		{
			parent0.paneSequence.tabDisplay.viewCapillariesCheckBox.setSelected(true);
			firePropertyChange("CAPILLARIES_NEW", false, true);
			tabsPane.setSelectedIndex(ID_INFOS);
		}

	}
	
	public void displayCapillariesInformation(Experiment exp) 
	{
		SwingUtilities.invokeLater(new Runnable() 
		{ 
			public void run() 
			{
				ProgressFrame progress = new ProgressFrame("Display capillaries information");
				updateDialogs( exp);
				parent0.paneSequence.tabDisplay.viewCapillariesCheckBox.setSelected(true);
				parent0.paneSequence.updateDialogs(exp);
				progress.close();
			}});
	}
	
	public void updateDialogs(Experiment exp) 
	{
		if (exp != null) 
		{
			SequenceKymosUtils.transferCamDataROIStoKymo(exp);
			exp.capillaries.desc_old.copy(exp.capillaries.desc);
			tabInfos.setAllDescriptors(exp.capillaries);
			tabCreate.setGroupingAndNumber(exp.capillaries);
			parent0.paneSequence.tabInfosSeq.setExperimentsInfosToDialog(exp);
		}
	}
	
	public void getDialogCapillariesInfos(Experiment exp) 
	{
		tabInfos.getDescriptors(exp.capillaries);
		tabCreate.getGrouping(exp.capillaries);
	}

	@Override
	public void stateChanged(ChangeEvent arg0) 
	{
		JTabbedPane tabbedPane = (JTabbedPane) arg0.getSource();
        int selectedIndex = tabbedPane.getSelectedIndex();
        tabAdjust.roisDisplayrefBar(selectedIndex == ID_ADJUST);
        parent0.paneSequence.tabDisplay.viewCapillariesCheckBox.setSelected(selectedIndex == ID_INFOS);
	}

}
