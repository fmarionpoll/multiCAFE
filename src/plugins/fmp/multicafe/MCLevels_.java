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
	private JTabbedPane 	tabsPane 		= new JTabbedPane();
	MCLevels_File 			tabFile 		= new MCLevels_File();
	MCLevels_DetectLimits 	tabDetectLimits = new MCLevels_DetectLimits();
	MCLevels_DetectGulps 	tabDetectGulps 	= new MCLevels_DetectGulps();
	MCLevels_Edit			tabEdit			= new MCLevels_Edit();
	MCLevels_Graphs 		tabGraphs 		= new MCLevels_Graphs();

	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));

		GridLayout capLayout = new GridLayout(3, 1);
		
		tabDetectLimits.init(capLayout, parent0);
		tabDetectLimits.addPropertyChangeListener(this);
		tabsPane.addTab("Limits", null, tabDetectLimits, "Find limits of the columns of liquid");
		
		tabDetectGulps.init(capLayout, parent0);	
		tabsPane.addTab("Gulps", null, tabDetectGulps, "Detect gulps");
		tabDetectGulps.addPropertyChangeListener(this);
		
		tabEdit.init(capLayout, parent0);
		tabEdit.addPropertyChangeListener(this);
		tabsPane.addTab("Edit", null, tabEdit, "Edit Rois / measures");
		
		tabGraphs.init(capLayout, parent0);
		tabGraphs.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, tabGraphs, "Display results as a graph");
		
		tabFile.init(capLayout, parent0);
		tabFile.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, tabFile, "Load/Save kymographs");
						
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		tabDetectLimits.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
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
		else if (arg0.getPropertyName().equals("MEASURES_SAVE")) {
			tabsPane.setSelectedIndex(0);
		}
	}
	
	void transferSequenceCamDataToDialogs(SequenceCamData seqCamData) {
		tabDetectLimits.startSpinner.setValue((int)seqCamData.analysisStart);
		tabDetectLimits.endSpinner.setValue((int)seqCamData.analysisEnd);
		tabDetectGulps.startSpinner.setValue((int)seqCamData.analysisStart);
		tabDetectGulps.endSpinner.setValue((int)seqCamData.analysisEnd);
	}
}
