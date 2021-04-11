package plugins.fmp.multicafe.dlg.experiment;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import icy.gui.frame.IcyFrame;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;




public class MCExperiment_ extends JPanel implements PropertyChangeListener, ViewerListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6826269677524125173L;
	
	public	PopupPanel 		capPopupPanel	= null;
	public 	JTabbedPane 	tabsPane 		= new JTabbedPane();
	public 	Options 		tabOptions 		= new Options();
	public 	Infos			tabInfosSeq		= new Infos();
	public 	Intervals		tabIntervals	= new Intervals();
	public 	Analyze			tabAnalyze		= new Analyze();
	public 	LoadSave		panelFiles		= new LoadSave();
	
	private MultiCAFE 		parent0 		= null;



	public void init (JPanel mainPanel, String string, MultiCAFE parent0) 
	{
		this.parent0 = parent0;

		capPopupPanel = new PopupPanel(string);			
		capPopupPanel.expand();
		mainPanel.add(capPopupPanel);
		GridLayout tabsLayout = new GridLayout(2, 1);
		
		JPanel filesPanel = panelFiles.initPanel(parent0, this);
		
		tabInfosSeq.init(tabsLayout, parent0);
		tabsPane.addTab("Infos", null, tabInfosSeq, "Define infos for this experiment/box");
		tabInfosSeq.addPropertyChangeListener(this);
		
		tabIntervals.init(tabsLayout, parent0);
		tabsPane.addTab("Intervals", null, tabIntervals, "View/define stack image intervals");
		tabIntervals.addPropertyChangeListener(this);
		
		tabAnalyze.init(tabsLayout);
		tabsPane.addTab("Analyze", null, tabAnalyze, "Define analysis intervals");
		tabAnalyze.addPropertyChangeListener(this);

		tabOptions.init(tabsLayout, parent0);
		tabsPane.addTab("Options", null, tabOptions, "Options to display data");
		tabOptions.addPropertyChangeListener(this);
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
//		capPanel.add(sequencePanel0, BorderLayout.PAGE_START);
		capPanel.add(filesPanel, BorderLayout.CENTER);
		capPanel.add(tabsPane, BorderLayout.PAGE_END);	
		
		capPopupPanel.addComponentListener(new ComponentAdapter() 
		{
			@Override
			public void componentResized(ComponentEvent e) 
			{
				parent0.mainFrame.revalidate();
				parent0.mainFrame.pack();
				parent0.mainFrame.repaint();
			}
		});
		
		defineActionListeners();		
	}
	
	private void defineActionListeners() 
	{
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) 
	{
		if (event.getPropertyName().equals("SEQ_CLOSE")) 
		{
			System.out.println("SEQ_CLOSE");
		}
	}
		
	public void updateDialogs(Experiment exp) 
	{
		tabIntervals.displayCamDataIntervals(exp);
		tabInfosSeq.setExperimentsInfosToDialog(exp);

		updateViewerForSequenceCam(exp);
		parent0.paneKymos.tabDisplay.updateResultsAvailable(exp);
	}

	public void getExperimentInfosFromDialog(Experiment exp) 
	{
		tabInfosSeq.getExperimentInfosFromDialog(exp);
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
			placeViewerNextToDialogBox(v, parent0.mainFrame);
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
				Experiment exp = (Experiment) parent0.expList.getSelectedItem();
				int idCurrentExp = exp.seqCamData.seq.getId();
				if (idViewer == idCurrentExp) 
				{
					int t = v.getPositionT(); 
					v.setTitle(exp.seqCamData.getDecoratedImageName(t));
					if (parent0.paneCages.bTrapROIsEdit) 
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
