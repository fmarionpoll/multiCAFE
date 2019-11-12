package plugins.fmp.multicafe;

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
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeSequence.SequenceKymosUtils;



public class MCCapillaries_ extends JPanel implements PropertyChangeListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 853047648249832145L;
	
	JTabbedPane 			tabsPane 		= new JTabbedPane();
	MCCapillaries_Build 	buildarrayTab 	= new MCCapillaries_Build();
	MCCapillaries_File 		fileTab 		= new MCCapillaries_File();
	MCCapillaries_Adjust 	adjustTab 		= new MCCapillaries_Adjust();
	MCCapillaries_Infos		infosTab		= new MCCapillaries_Infos();

	private MultiCAFE parent0 = null;

	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		
		this.parent0 = parent0;
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.expand();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		
		GridLayout capLayout = new GridLayout(3, 1);
		
		buildarrayTab.init(capLayout, parent0);
		buildarrayTab.addPropertyChangeListener(this);
		tabsPane.addTab("Create", null, buildarrayTab, "Create lines defining capillaries");

		adjustTab.init(capLayout, parent0);
		adjustTab.addPropertyChangeListener(parent0);
		tabsPane.addTab("Adjust", null, adjustTab, "Adjust ROIS position to the capillaries");

		infosTab.init(capLayout);
		infosTab.addPropertyChangeListener(this);
		tabsPane.addTab("Infos", null, infosTab, "Define pixel conversion unit of images and capillaries content");

		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save xml file with capillaries descriptors");

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		
		tabsPane.addChangeListener(this );
		
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
		if (event.getPropertyName().equals("CAP_ROIS_OPEN")) {
			setCapillariesInfosToDialogs();
		  	tabsPane.setSelectedIndex(2);
		  	firePropertyChange("CAPILLARIES_OPEN", false, true);
		}			  
		else if (event.getPropertyName().equals("CAP_ROIS_SAVE")) {
			tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName().equals("CAPILLARIES_NEW")) {
			parent0.sequencePane.displayTab.viewCapillariesCheckBox.setSelected(true);
			firePropertyChange("CAPILLARIES_NEW", false, true);
			tabsPane.setSelectedIndex(2);
		}

	}
	
	boolean loadCapillaries_() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		boolean flag = fileTab.loadCapillaries_File(exp);
		if (flag) {
			SequenceKymos seqKymos = exp.seqKymos;
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				infosTab.setCapillariesInfosToDialog(seqKymos.capillaries);
				parent0.sequencePane.displayTab.viewCapillariesCheckBox.setSelected(true);
				buildarrayTab.setCapillariesInfosToDialog(seqKymos.capillaries);
				parent0.sequencePane.infosTab.setExperimentsInfosToDialog(exp, seqKymos.capillaries);
				parent0.sequencePane.intervalsTab.setAnalyzeFrameToDialog(exp);
				parent0.buildKymosPane.createTab.setBuildKymosParametersToDialog(exp);
			}});
		}
		return flag;
	}
	
	private void setCapillariesInfosToDialogs() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		SequenceKymos seqKymos = exp.seqKymos;
		SequenceKymosUtils.transferCamDataROIStoKymo(seqCamData, seqKymos);
		seqKymos.capillaries.desc_old.copy(seqKymos.capillaries.desc);
		infosTab.setCapillariesInfosToDialog(seqKymos.capillaries);
		buildarrayTab.setCapillariesInfosToDialog(seqKymos.capillaries);
		parent0.sequencePane.infosTab.setExperimentsInfosToDialog(exp, seqKymos.capillaries);
	}
	
	boolean saveCapillaries(Experiment exp) {
		return fileTab.saveCapillaries(exp);
	}
	
	void getCapillariesInfos(Capillaries cap) {
		infosTab.getCapillariesInfosFromDialog(cap);
		buildarrayTab.getCapillariesInfosFromDialog(cap);
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		JTabbedPane tabbedPane = (JTabbedPane) arg0.getSource();
        int selectedIndex = tabbedPane.getSelectedIndex();
        adjustTab.roisDisplayrefBar(selectedIndex == 1);
        parent0.sequencePane.displayTab.viewCapillariesCheckBox.setSelected(selectedIndex == 2);
	}

}
