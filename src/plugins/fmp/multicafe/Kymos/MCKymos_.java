package plugins.fmp.multicafe.Kymos;

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
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafeSequence.Experiment;


public class MCKymos_ extends JPanel implements PropertyChangeListener, ChangeListener {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1122367183829360097L;

	JTabbedPane 			tabsPane 		= new JTabbedPane();
	public Create 	tabCreate 		= new Create();
	public Display	tabDisplay 		= new Display();
	public LoadSave 			tabFile 		= new LoadSave();
	
	private MultiCAFE parent0 = null;

	public void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout capLayout = new GridLayout(3, 1);
		
		tabCreate.init(capLayout, parent0);
		tabCreate.addPropertyChangeListener(this);
		tabsPane.addTab("Build kymos", null, tabCreate, "Build kymographs from ROI lines placed over capillaries");
		
		tabDisplay.init(capLayout, parent0);
		tabDisplay.addPropertyChangeListener(this);
		tabsPane.addTab("Display", null, tabDisplay, "Display options of data & kymographs");

		tabFile.init(capLayout, parent0);
		tabFile.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, tabFile, "Load/Save xml file with capillaries descriptors");

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
			tabsPane.setSelectedIndex(2);
		}
		else if (event.getPropertyName().equals("KYMOS_CREATE")) {
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (exp != null) {
				tabDisplay.transferCapillaryNamesToComboBox(exp.capillaries.capillariesArrayList);
				tabsPane.setSelectedIndex(1);
			}
		}
		else if (event.getPropertyName().equals("KYMOS_SAVE")) {
			tabsPane.setSelectedIndex(1);
		}
		
		else if (event.getPropertyName().equals("SEQ_CHGBIN")) {
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (exp == null)
				return;
			String localString = (String) tabDisplay.availableResultsCombo.getSelectedItem();
			parent0.paneSequence.tabClose.closeCurrentExperiment();
			parent0.expList.expListResultsSubPath = localString;
			exp.resultsSubPath = localString;
			parent0.paneSequence.openExperiment(exp);
		}
	}
	
	void tabbedCapillariesAndKymosSelected() {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp == null || exp.seqCamData == null)
			return;
		int iselected = tabsPane.getSelectedIndex();
		if (iselected == 0) {
			Viewer v = exp.seqCamData.seq.getFirstViewer();
			if (v != null)
				v.toFront();
		} else if (iselected == 1) {
			parent0.paneKymos.tabDisplay.displayUpdateOnSwingThread();
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent event) {
		if (event.getSource() == tabsPane)
			tabbedCapillariesAndKymosSelected();
	}

}
