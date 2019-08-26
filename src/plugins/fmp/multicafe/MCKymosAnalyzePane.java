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
import icy.image.IcyBufferedImage;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeSequence.SequenceKymosUtils;
import plugins.fmp.multicafeTools.ImageTransformTools;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCKymosAnalyzePane extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	private JTabbedPane 	tabsPane 	= new JTabbedPane();
	MCKymosAnalyzeTab_File 		fileTab 	= new MCKymosAnalyzeTab_File();
	MCKymosAnalyzeTab_DetectLimits limitsTab 	= new MCKymosAnalyzeTab_DetectLimits();
	MCKymosAnalyzeTab_DetectGulps 	gulpsTab 	= new MCKymosAnalyzeTab_DetectGulps();
	MCKymosAnalyzeTab_Graphs 		graphsTab 	= new MCKymosAnalyzeTab_Graphs();
	ImageTransformTools 	tImg = null;
	private MultiCAFE 		parent0 = null;

	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		
		this.parent0 = parent0;
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
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

	void kymosBuildFiltered(int zChannelSource, int zChannelDestination, TransformOp transformop, int spanDiff) {
		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(spanDiff);
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		SequenceKymos seqKymos = exp.seqKymos;
		int nimages = seqKymos.seq.getSizeT();
		seqKymos.seq.beginUpdate();
		tImg.setSequence(seqKymos);
		seqKymos.transferAnalysisParametersToCapillaries();
		Capillaries capillaries = seqKymos.capillaries;
		if (capillaries.capillariesArrayList.size() != nimages) {
			SequenceKymosUtils.transferCamDataROIStoKymo(seqCamData, seqKymos);
		}
		
		for (int t= 0; t < nimages; t++) {
			Capillary cap = capillaries.capillariesArrayList.get(t);
			cap.indexImage = t;
			IcyBufferedImage img = seqKymos.seq.getImage(t, zChannelSource);
			IcyBufferedImage img2 = tImg.transformImage (img, transformop);
			img2 = tImg.transformImage(img2, TransformOp.RTOGB);
			
			if (seqKymos.seq.getSizeZ(0) < (zChannelDestination+1)) 
				seqKymos.seq.addImage(t, img2);
			else
				seqKymos.seq.setImage(t, zChannelDestination, img2);
		}
		
		if (zChannelDestination == 1)
			seqKymos.capillaries.limitsOptions.transformForLevels = transformop;
		else
			seqKymos.capillaries.gulpsOptions.transformForGulps = transformop;
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
		seqKymos.seq.dataChanged();
		seqKymos.seq.endUpdate();
	}
}
