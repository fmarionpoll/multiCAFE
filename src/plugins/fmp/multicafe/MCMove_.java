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
	private MCMove_BuildROIs 	buildROIsTab= new MCMove_BuildROIs();
	private MCMove_Detect1 		detectTab1 	= new MCMove_Detect1();
	private MCMove_Detect2 		detectTab2 	= new MCMove_Detect2();
	MCMove_File 				fileTab 	= new MCMove_File();
	MCMove_Graphs 				graphicsTab = new MCMove_Graphs();
	
	MultiCAFE parent0 = null;

	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout capLayout = new GridLayout(4, 1);
		
		buildROIsTab.init(capLayout, parent0);
		buildROIsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Cages", null, buildROIsTab, "Define cages using ROI polygons placed over each cage");

		detectTab1.init(capLayout, parent0);
		detectTab1.addPropertyChangeListener(this);
		tabsPane.addTab("Detect1", null, detectTab1, "Detect flies position using thresholding on image overlay");
		
		detectTab2.init(capLayout, parent0);
		detectTab2.addPropertyChangeListener(this);
		tabsPane.addTab("Detect2", null, detectTab2, "Detect flies position using background subtraction");
		
		graphicsTab.init(capLayout, parent0);		
		graphicsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, graphicsTab, "Display results as graphics");

		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/save cages and flies position");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
		
		tabsPane.addChangeListener(new ChangeListener() {
			@Override 
	        public void stateChanged(ChangeEvent e) {
	            int itab = tabsPane.getSelectedIndex();
	            detectTab2.imageOverlayCheckBox.setSelected(itab == 1);
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
			buildROIsTab.updateFromSequence();
		}
	}

	boolean loadDefaultCages(Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		String path = seqCamData.getDirectory();
		boolean flag = fileTab.loadCages(path+File.separator+"drosotrack.xml");
		return flag;
	}
	
	boolean saveDefaultCages(Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		String directory = seqCamData.getDirectory();
		String filename = directory + File.separator+"drosotrack.xml";
		return seqCamData.cages.xmlWriteCagesToFileNoQuestion(filename);
	}
}

