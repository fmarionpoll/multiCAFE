package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCLevels_ extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	private JTabbedPane 	tabsPane 	= new JTabbedPane();
	MCLevels_File 			fileTab 	= new MCLevels_File();
	MCLevels_DetectLimits 	detectLimitsTab = new MCLevels_DetectLimits();
	MCLevels_DetectGulps 	detectGulpsTab 	= new MCLevels_DetectGulps();
	MCLevels_Edit			editTab		= new MCLevels_Edit();
	MCLevels_Graphs 		graphsTab 	= new MCLevels_Graphs();

	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));

		GridLayout capLayout = new GridLayout(3, 1);
		
		detectLimitsTab.init(capLayout, parent0);
		detectLimitsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Limits", null, detectLimitsTab, "Find limits of the columns of liquid");
		
		detectGulpsTab.init(capLayout, parent0);	
		tabsPane.addTab("Gulps", null, detectGulpsTab, "Detect gulps");
		detectGulpsTab.addPropertyChangeListener(this);
		
		editTab.init(capLayout, parent0);
		editTab.addPropertyChangeListener(this);
		tabsPane.addTab("Edit", null, editTab, "Edit Rois / measures");
		
		graphsTab.init(capLayout, parent0);
		graphsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, graphsTab, "Display results as a graph");
		
		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save kymographs");
						
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		detectLimitsTab.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
		tabsPane.setSelectedIndex(0);
		
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
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("MEASURES_OPEN")) {
			firePropertyChange("MEASURES_OPEN", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMO_DISPLAY_FILTERED1")) {
			firePropertyChange("KYMO_DISPLAYFILTERED", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMO_DETECT_TOP")) {
			firePropertyChange("MEASURETOP_OK", false, true);
		}
		else if (arg0.getPropertyName().equals("MEASURES_SAVE")) {
			tabsPane.setSelectedIndex(0);
		}
	}
	
	void transferSequenceCamDataToDialogs(SequenceCamData seqCamData) {
		detectLimitsTab.startSpinner.setValue((int)seqCamData.analysisStart);
		detectLimitsTab.endSpinner.setValue((int)seqCamData.analysisEnd);
		detectGulpsTab.startSpinner.setValue((int)seqCamData.analysisStart);
		detectGulpsTab.endSpinner.setValue((int)seqCamData.analysisEnd);
	}
}
