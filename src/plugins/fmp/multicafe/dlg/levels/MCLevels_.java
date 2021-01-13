package plugins.fmp.multicafe.dlg.levels;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.tools.ImageTransformTools.TransformOp;


public class MCLevels_ extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	public	PopupPanel capPopupPanel	= null;
	private JTabbedPane tabsPane 		= new JTabbedPane();
	public LoadSave 	tabFileLevels	= new LoadSave();
	DetectLevels 		tabDetectLevels = new DetectLevels();
//	DetectLevels2 		tabDetectLevels2 = new DetectLevels2();
	
	DetectGulps 		tabDetectGulps 	= new DetectGulps();
	Edit				tabEdit			= new Edit();
	Adjust				tabAdjust		= new Adjust();
	public Graphs 		tabGraphs 		= new Graphs();

	
	public void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		
		capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(capPopupPanel);

		GridLayout capLayout = new GridLayout(3, 1);
		
		tabDetectLevels.init(capLayout, parent0);
		tabDetectLevels.addPropertyChangeListener(this);
		tabsPane.addTab("Levels", null, tabDetectLevels, "Find limits of the columns of liquid");
		
//		tabDetectLevels2.init(capLayout, parent0);
//		tabDetectLevels2.addPropertyChangeListener(this);
//		tabsPane.addTab("Levels2", null, tabDetectLevels2, "Find limits of the columns of liquid");
		
		tabDetectGulps.init(capLayout, parent0);	
		tabsPane.addTab("Gulps", null, tabDetectGulps, "Detect gulps");
		tabDetectGulps.addPropertyChangeListener(this);
		
		tabEdit.init(capLayout, parent0);
		tabEdit.addPropertyChangeListener(this);
		tabsPane.addTab("Edit", null, tabEdit, "Edit Rois / measures");

		tabAdjust.init(capLayout, parent0);
		tabAdjust.addPropertyChangeListener(this);
		tabsPane.addTab("Adjust", null, tabAdjust, "Adjust measures on series and across series");

		
		tabGraphs.init(capLayout, parent0);
		tabGraphs.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, tabGraphs, "Display results as a graph");
		
		tabFileLevels.init(capLayout, parent0);
		tabFileLevels.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, tabFileLevels, "Load/Save kymographs");
						
		capPanel.add(tabsPane);
		tabDetectLevels.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
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
	
	public void transferSeqKymoDataToDialogs(Experiment exp) {
		int lastpixel = exp.seqKymos.imageWidthMax - 1;
		tabDetectLevels.startSpinner.setValue(0);
		tabDetectLevels.endSpinner.setValue(lastpixel);
		tabDetectGulps.startSpinner.setValue(0);
		tabDetectGulps.endSpinner.setValue(lastpixel);
	}
}
