package plugins.fmp.multicafe;


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
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.SequenceKymos;

public class MCKymosBuildPane extends JPanel implements PropertyChangeListener, ChangeListener {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1122367183829360097L;

	JTabbedPane 				tabsPane 		= new JTabbedPane();

	MCKymosBuildTab_Create 	buildkymosTab 	= new MCKymosBuildTab_Create();
	MCKymosBuildTab_Options optionsTab 		= new MCKymosBuildTab_Options();
	MCKymosBuildTab_File 	fileTab 		= new MCKymosBuildTab_File();
	
	private MultiCAFE parent0 = null;

	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		
		GridLayout capLayout = new GridLayout(3, 1);
		

		buildkymosTab.init(capLayout, parent0);
		buildkymosTab.addPropertyChangeListener(this);
		tabsPane.addTab("Build kymos", null, buildkymosTab, "Build kymographs from ROI lines placed over capillaries");
		
		optionsTab.init(capLayout, parent0);
		optionsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Display", null, optionsTab, "Display options of data & kymographs");

		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save xml file with capillaries descriptors");

		tabsPane.addChangeListener(this);
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
		 if (event.getPropertyName().equals("KYMOS_OPEN") 
					|| event.getPropertyName().equals("KYMOS_CREATE")) {
			SequenceKymos seqKymos = parent0.expList.getSeqKymos(parent0.currentIndex);
			optionsTab.viewKymosCheckBox.setSelected(true);
				optionsTab.transferCapillaryNamesToComboBox(seqKymos.capillaries.capillariesArrayList);
				tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName().equals("KYMOS_OK")) {
			tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName().equals("KYMOS_SAVE")) {
			tabsPane.setSelectedIndex(2);
		}
	}
	
	void tabbedCapillariesAndKymosSelected() {
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
		if (seqCamData == null)
			return;
		int iselected = tabsPane.getSelectedIndex();
		if (iselected == 0) {
			Viewer v = seqCamData.seq.getFirstViewer();
			v.toFront();
		} else if (iselected == 1) {
			parent0.buildKymosPane.optionsTab.displayUpdateOnSwingThread();
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent event) {
		if (event.getSource() == tabsPane)
			tabbedCapillariesAndKymosSelected();
	}

}
