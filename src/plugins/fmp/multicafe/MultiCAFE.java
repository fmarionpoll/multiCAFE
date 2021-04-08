package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.gui.viewer.ViewerListener;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;

import plugins.fmp.multicafe.dlg.cages.MCCages_;
import plugins.fmp.multicafe.dlg.capillaries.MCCapillaries_;
import plugins.fmp.multicafe.dlg.excel.MCExcel_;
import plugins.fmp.multicafe.dlg.kymos.MCKymos_;
import plugins.fmp.multicafe.dlg.levels.MCLevels_;
import plugins.fmp.multicafe.dlg.sequence.MCSequence_;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.workinprogress_gpu.MCSpots_;



public class MultiCAFE extends PluginActionable implements ViewerListener 
{
	public IcyFrame 		mainFrame 		= new IcyFrame("MultiCAFE April 7, 2021", true, true, true, true);
	public ExperimentList 	expList 		= new ExperimentList();
	
	public MCSequence_ 		paneSequence 	= new MCSequence_();
	public MCCapillaries_ 	paneCapillaries	= new MCCapillaries_();
	public MCKymos_			paneKymos		= new MCKymos_();
	public MCLevels_ 		paneLevels 		= new MCLevels_();
	public MCSpots_			paneSpots		= new MCSpots_();
	public MCCages_ 		paneCages 		= new MCCages_();
	public MCExcel_			paneExcel		= new MCExcel_();
	
	public 	JTabbedPane 	tabsPane 		= new JTabbedPane();
	
	//-------------------------------------------------------------------
	
	@Override
	public void run() 
	{		
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		paneSequence.init(mainPanel, "Source", this);
		paneCapillaries.init(mainPanel, "Capillaries: define, edit", this);
		paneKymos.init(mainPanel, "Capillaries: build kymographs", this);
		paneLevels.init(mainPanel, "Capillaries: measure levels", this);
//		paneSpots.init(mainPanel, "MEASURE SPOTS", this);
		paneCages.init(mainPanel, "Cages", this);
		paneExcel.init(mainPanel, "Export", this);
		
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.WEST);
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}	 
	
	public Experiment openExperimentFromString(String filename) 
	{
		Experiment exp = expList.getExperiment(filename);
		if (exp == null) 
		{
			exp = new Experiment(filename);
			expList.addExperiment(exp);
		}
		exp.setExperimentDirectory(filename);
		exp.setImagesDirectory(Experiment.getImagesDirectoryAsParentFromFileName(filename));
		exp.openSequenceCamData();
		if (exp.seqCamData != null && exp.seqCamData.seq != null) 
		{
			updateViewerForSequenceCam(exp);
		} 
		else 
		{
			System.out.println("seqCamData or seq of seqCamData is null!");
		}
		return exp;
	}
	
	public void updateViewerForSequenceCam(Experiment exp) 
	{
		Sequence seq = exp.seqCamData.seq;
		Viewer v = seq.getFirstViewer();
		if (v == null) 
		{
			v = new Viewer(exp.seqCamData.seq, true);
		}
		if (v != null) {
			placeViewerNextToDialogBox(v, mainFrame);
			v.toFront();
			v.requestFocus();
			v.addListener( this );
			v.setTitle(exp.seqCamData.getDecoratedImageName(0));
		}
	}
	
	private void placeViewerNextToDialogBox(Viewer v, IcyFrame mainFrame) 
	{
		Rectangle rectv = v.getBoundsInternal();
		Rectangle rect0 = mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		v.setBounds(rectv);
	}

	@Override	
	public void viewerChanged(ViewerEvent event) 
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED)) 
		{
			if (event.getDim() == DimensionId.T) 
			{
				Viewer v = event.getSource(); 
				int idViewer = v.getSequence().getId(); 
				Experiment exp = expList.getCurrentExperiment();
				int idCurrentExp = exp.seqCamData.seq.getId();
				if (idViewer == idCurrentExp) 
				{
					int t = v.getPositionT(); 
					v.setTitle(exp.seqCamData.getDecoratedImageName(t));
					if (paneCages.bTrapROIsEdit) 
						exp.saveDetRoisToPositions();
					exp.updateROIsAt(t);
				}
			}
		}
	}

	@Override
	public void viewerClosed(Viewer viewer) 
	{
		viewer.removeListener(this);
	}

}

