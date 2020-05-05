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



public class MCMove_ extends JPanel implements PropertyChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3457738144388946607L;
	
	JTabbedPane 		tabsPane	= new JTabbedPane();
	MCMove_BuildROIs 	tabBuildROIs= new MCMove_BuildROIs();
	MCMove_BuildROIs2 	tabBuildROIs2= new MCMove_BuildROIs2();
	MCMove_Infos		tabInfos	= new MCMove_Infos();
	MCMove_Detect1 		tabDetect1 	= new MCMove_Detect1();
	MCMove_Detect2 		tabDetect2 	= new MCMove_Detect2();
	MCMove_File 		tabFile 	= new MCMove_File();
	MCMove_Graphs 		tabGraphics = new MCMove_Graphs();
	int 				iTAB_INFOS 	= 2;
	int 				iTAB_DETECT1= 3;
	MultiCAFE 			parent0		= null;

	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout capLayout = new GridLayout(4, 1);
		
		int iTab = 0;
		tabBuildROIs.init(capLayout, parent0);
		tabBuildROIs.addPropertyChangeListener(this);
		tabsPane.addTab("Cages", null, tabBuildROIs, "Define cages using ROI polygons placed over each cage");
		
		iTab++;
		tabBuildROIs2.init(capLayout, parent0);
		tabBuildROIs2.addPropertyChangeListener(this);
		tabsPane.addTab("Cages (bis)", null, tabBuildROIs2, "Define cages using a ROI polygons and limits detection");

		iTab++;
		iTAB_INFOS = iTab;
		tabInfos.init(parent0);
		tabInfos.addPropertyChangeListener(this);
		tabsPane.addTab("Infos", null, tabInfos, "Infos about cages");
		
		iTab++;
		iTAB_DETECT1 = iTab;
		tabDetect1.init(capLayout, parent0);
		tabDetect1.addPropertyChangeListener(this);
		tabsPane.addTab("Detect1", null, tabDetect1, "Detect flies position using thresholding on image overlay");
		
		iTab++;
		tabDetect2.init(capLayout, parent0);
		tabDetect2.addPropertyChangeListener(this);
		tabsPane.addTab("Detect2", null, tabDetect2, "Detect flies position using background subtraction");
		
		iTab++;
		tabGraphics.init(capLayout, parent0);		
		tabGraphics.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, tabGraphics, "Display results as graphics");

		iTab++;
		tabFile.init(capLayout, parent0);
		tabFile.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, tabFile, "Load/save cages and flies position");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
		
		tabsPane.addChangeListener(new ChangeListener() {
			@Override 
	        public void stateChanged(ChangeEvent e) {
	            int selectedIndex = tabsPane.getSelectedIndex();
	            tabDetect1.overlayCheckBox.setSelected(selectedIndex == iTAB_DETECT1);
	            if (selectedIndex == iTAB_INFOS && tabInfos.tableView.getRowCount() > 0) {
	            	tabInfos.tableView.changeSelection(0, 1, false, false);
	            }
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
			tabBuildROIs.updateNColumnsFieldFromSequence();
		}
	}

	
	
}

