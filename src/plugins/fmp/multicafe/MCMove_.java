package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.component.PopupPanel;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;



public class MCMove_ extends JPanel implements PropertyChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3457738144388946607L;
	
	JTabbedPane 				tabsPane	= new JTabbedPane();
	private MCMove_BuildROIs 	tabBuildROIs= new MCMove_BuildROIs();
	private MCMove_Detect1 		tabDetect1 	= new MCMove_Detect1();
	private MCMove_Detect2 		tabDetect2 	= new MCMove_Detect2();
	MCMove_File 				tabFile 	= new MCMove_File();
	MCMove_Graphs 				tabGraphics = new MCMove_Graphs();
	
	MultiCAFE parent0 = null;

	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout capLayout = new GridLayout(4, 1);
		
		tabBuildROIs.init(capLayout, parent0);
		tabBuildROIs.addPropertyChangeListener(this);
		tabsPane.addTab("Cages", null, tabBuildROIs, "Define cages using ROI polygons placed over each cage");

		tabDetect1.init(capLayout, parent0);
		tabDetect1.addPropertyChangeListener(this);
		tabsPane.addTab("Detect1", null, tabDetect1, "Detect flies position using thresholding on image overlay");
		
		tabDetect2.init(capLayout, parent0);
		tabDetect2.addPropertyChangeListener(this);
		tabsPane.addTab("Detect2", null, tabDetect2, "Detect flies position using background subtraction");
		
		tabGraphics.init(capLayout, parent0);		
		tabGraphics.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, tabGraphics, "Display results as graphics");

		tabFile.init(capLayout, parent0);
		tabFile.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, tabFile, "Load/save cages and flies position");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
		
		tabsPane.addChangeListener(new ChangeListener() {
			@Override 
	        public void stateChanged(ChangeEvent e) {
	            int itab = tabsPane.getSelectedIndex();
	            tabDetect1.overlayCheckBox.setSelected(itab == 1);
	        }
	    });
		
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
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("LOAD_DATA")) {
			tabBuildROIs.updateFromSequence();
		}
	}

	boolean loadDefaultCages(Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		String path = seqCamData.getDirectory();
		boolean flag = tabFile.loadCages(path+File.separator+"drosotrack.xml");
		return flag;
	}
	
	boolean saveDefaultCages(Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		String directory = seqCamData.getDirectory();
		String filename = directory + File.separator+"drosotrack.xml";
		return exp.cages.xmlWriteCagesToFileNoQuestion(filename);
	}
}

