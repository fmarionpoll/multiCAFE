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
import plugins.fmp.multicafeSequence.Experiment;


public class MCKymos_ extends JPanel implements PropertyChangeListener, ChangeListener {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1122367183829360097L;

	JTabbedPane 			tabsPane 		= new JTabbedPane();
	MCKymos_Create 	createTab 		= new MCKymos_Create();
	MCKymos_Display	displayTab 		= new MCKymos_Display();
	MCKymos_File 		fileTab 		= new MCKymos_File();
	
	private MultiCAFE parent0 = null;

	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout capLayout = new GridLayout(3, 1);
		
		createTab.init(capLayout, parent0);
		createTab.addPropertyChangeListener(this);
		tabsPane.addTab("Build kymos", null, createTab, "Build kymographs from ROI lines placed over capillaries");
		
		displayTab.init(capLayout, parent0);
		displayTab.addPropertyChangeListener(this);
		tabsPane.addTab("Display", null, displayTab, "Display options of data & kymographs");

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
		if (event.getPropertyName().equals("KYMOS_OPEN")) {
			displayTab.viewKymosCheckBox.setSelected(true);
			tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName().equals("KYMOS_CREATE")) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
			displayTab.viewKymosCheckBox.setSelected(true);
			displayTab.transferCapillaryNamesToComboBox(exp.seqKymos.capillaries.capillariesArrayList);
			tabsPane.setSelectedIndex(2);
		
		}
		else if (event.getPropertyName().equals("KYMOS_OK")) {
			tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName().equals("KYMOS_SAVE")) {
			tabsPane.setSelectedIndex(1);
		}
	}
	
	void tabbedCapillariesAndKymosSelected() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		if (exp.seqCamData == null)
			return;
		int iselected = tabsPane.getSelectedIndex();
		if (iselected == 0) {
			Viewer v = exp.seqCamData.seq.getFirstViewer();
			if (v != null)
				v.toFront();
		} else if (iselected == 1) {
			parent0.kymosPane.displayTab.displayUpdateOnSwingThread();
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent event) {
		if (event.getSource() == tabsPane)
			tabbedCapillariesAndKymosSelected();
	}

}
