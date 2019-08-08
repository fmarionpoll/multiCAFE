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
import icy.image.IcyBufferedImage;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeTools.ImageTransformTools;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCKymosPane extends JPanel implements PropertyChangeListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	
	private JTabbedPane 	tabsPane 	= new JTabbedPane();
	MCKymosTab_File 		fileTab 	= new MCKymosTab_File();
	MCKymosTab_DetectLimits limitsTab 	= new MCKymosTab_DetectLimits();
	MCKymosTab_DetectGulps 	gulpsTab 	= new MCKymosTab_DetectGulps();
	MCKymosTab_Graphs 		graphsTab 	= new MCKymosTab_Graphs();
	
	ImageTransformTools tImg = null;
	private MultiCAFE parent0 = null;

	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		
		this.parent0 = parent0;
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.expand();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));

		GridLayout capLayout = new GridLayout(3, 1);
		
		limitsTab.init(capLayout, parent0);
		limitsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Liquid", null, limitsTab, "Find limits of the columns of liquid");
		
		gulpsTab.init(capLayout, parent0);	
		tabsPane.addTab("Gulps", null, gulpsTab, "Detect gulps");
		gulpsTab.addPropertyChangeListener(this);
		
		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save kymographs");
		
		graphsTab.init(capLayout, parent0);
		graphsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, graphsTab, "Display results as a graph");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabsPane.addChangeListener(this);
		
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		limitsTab.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
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
		if (arg0.getPropertyName().equals("KYMOS_OK")) {
			tabbedCapillariesAndKymosSelected();
			firePropertyChange( "KYMOS_OK", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMOS_SAVE")) {
			tabsPane.setSelectedIndex(2);
		}
		else if (arg0.getPropertyName().equals("MEASURES_OPEN")) {
			if (parent0.vkymos != null) 		
				firePropertyChange("MEASURES_OPEN", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMO_DISPLAY_FILTERED1")) {
			if (parent0.vkymos != null) {		
				firePropertyChange("KYMO_DISPLAYFILTERED", false, true);
			}
		}
		else if (arg0.getPropertyName().equals("KYMO_DETECT_TOP")) {
			firePropertyChange("MEASURETOP_OK", false, true);
		}
		else if (arg0.getPropertyName().equals("MEASURES_SAVE")) {
			tabsPane.setSelectedIndex(0);
		}
	}

	void tabbedCapillariesAndKymosSelected() {
		if (parent0.vSequence == null)
			return;
		int iselected = tabsPane.getSelectedIndex();
		if (iselected == 0) {
			Viewer v = parent0.vSequence.seq.getFirstViewer();
			v.toFront();
		} else if (iselected == 1) {
			parent0.capillariesPane.optionsTab.displayUpdateOnSwingThread();
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent event) {
		if (event.getSource() == tabsPane)
			tabbedCapillariesAndKymosSelected();
	}
	
	void kymosBuildFiltered(int zChannelSource, int zChannelDestination, TransformOp transformop, int spanDiff) {
		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(spanDiff);
		int nimages = parent0.vkymos.seq.getSizeT();
		parent0.vkymos.seq.beginUpdate();
		tImg.setSequence(parent0.vkymos);
		parent0.vkymos.updateCapillaries(nimages);
		Capillaries capillaries = parent0.vkymos.capillaries;
		if (capillaries.capillariesArrayList.size() != nimages) {
			capillaries.createCapillariesFromROIS(parent0.vSequence);
		}
		
		for (int t= 0; t < nimages; t++) {
			Capillary cap = capillaries.capillariesArrayList.get(t);
			cap.indexImage = t;
			IcyBufferedImage img = parent0.vkymos.seq.getImage(t, zChannelSource);
			IcyBufferedImage img2 = tImg.transformImage (img, transformop);
			img2 = tImg.transformImage(img2, TransformOp.RTOGB);
			
			if (parent0.vkymos.seq.getSizeZ(0) < (zChannelDestination+1)) 
				parent0.vkymos.seq.addImage(t, img2);
			else
				parent0.vkymos.seq.setImage(t, zChannelDestination, img2);
		}
		
		if (zChannelDestination == 1)
			parent0.vkymos.transformForLevels = transformop;
		else
			parent0.vkymos.transformForGulps = transformop;
		parent0.vkymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
		parent0.vkymos.seq.dataChanged();
		parent0.vkymos.seq.endUpdate();
	}
}
