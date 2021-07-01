package plugins.fmp.multicafe2.dlg.kymos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.tools.Directories;


public class Display extends JPanel implements ViewerListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2103052112476748890L;
	
	public 	int			indexImagesCombo 		= -1;
	public 	JComboBox<String> kymographsCombo 	= new JComboBox <String> (new String[] {"none"});
			JComboBox<String> viewsCombo		= new JComboBox <String>();
			JButton 	updateButton 			= new JButton("Update");
			JButton  	previousButton		 	= new JButton("<");
			JButton		nextButton				= new JButton(">");
			JCheckBox 	viewLevelsCheckbox 		= new JCheckBox("top/bottom level (green)", true);
			JCheckBox 	viewDerivativeCheckbox 	= new JCheckBox("derivative (yellow)", true);
			JCheckBox 	viewGulpsCheckbox 		= new JCheckBox("gulps (red)", true);
	private MultiCAFE2 	parent0 				= null;
	private boolean		isActionEnabled			= true;	
	

	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{	
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		
		JPanel panel1 = new JPanel (layout);
		panel1.add(new JLabel("views"));
		panel1.add(viewsCombo);
		panel1.add(new JLabel(" kymograph"));
		int bWidth = 30;
		int bHeight = 21;
		panel1.add(previousButton, BorderLayout.WEST); 
		previousButton.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(kymographsCombo, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, bHeight)); 
		panel1.add(nextButton, BorderLayout.EAST);
		add(panel1);
		
		JPanel panel3 = new JPanel (layout);
		panel3.add(viewLevelsCheckbox);
		panel3.add(viewDerivativeCheckbox);
		panel3.add(viewGulpsCheckbox);
		add(panel3);
		
		defineActionListeners();
	}
	
	
	private void defineActionListeners()
	{		
		kymographsCombo.addActionListener(new ActionListener ()
		{ 
			@Override public void actionPerformed( final ActionEvent e )
			{ 
				if (isActionEnabled)
					displayUpdateOnSwingThread();
			}});
		
		viewDerivativeCheckbox.addActionListener(new ActionListener ()
		{ 
			@Override public void actionPerformed( final ActionEvent e )
			{ 
			displayROIs("deriv", viewDerivativeCheckbox.isSelected());
			}});

		viewGulpsCheckbox.addActionListener(new ActionListener ()
		{ 
			@Override public void actionPerformed( final ActionEvent e )
			{ 
			displayROIs("gulp", viewGulpsCheckbox.isSelected());
			}});
		
		viewLevelsCheckbox.addActionListener(new ActionListener ()
{ 
			@Override public void actionPerformed( final ActionEvent e )
{ 
			displayROIs("level", viewLevelsCheckbox.isSelected());
			}});
		
		nextButton.addActionListener(new ActionListener ()
		{ 
			@Override public void actionPerformed( final ActionEvent e )
			{ 
			int isel = kymographsCombo.getSelectedIndex()+1;
			if (isel < kymographsCombo.getItemCount())
				selectKymographImage(isel);
			}});
		
		previousButton.addActionListener(new ActionListener ()
		{ 
			@Override public void actionPerformed( final ActionEvent e )
			{ 
			int isel = kymographsCombo.getSelectedIndex()-1;
			if (isel < kymographsCombo.getItemCount())
				selectKymographImage(isel);
			}});
		
		viewsCombo.addActionListener(new ActionListener ()
		{ 
			@Override public void actionPerformed( final ActionEvent e )
			{
				String localString = (String) viewsCombo.getSelectedItem();
				if (localString != null && localString.contains("."))
					localString = null;
				if (isActionEnabled)
					changeBinSubdirectory(localString);
			}});
	}
	
	public void transferCapillaryNamesToComboBox(Experiment exp )
	{
		SwingUtilities.invokeLater(new Runnable() { public void run()
		{	
			kymographsCombo.removeAllItems();
			Collections.sort(exp.capillaries.capillariesArrayList); 
			int ncapillaries = exp.capillaries.capillariesArrayList.size();
			for (int i=0; i< ncapillaries; i++)
			{
				Capillary cap = exp.capillaries.capillariesArrayList.get(i);
				kymographsCombo.addItem(cap.roi.getName());
			}
		}});	
	}
	
	public void displayROIsAccordingToUserSelection()
	{
		displayROIs("deriv", viewDerivativeCheckbox.isSelected());
		displayROIs("gulp", viewGulpsCheckbox.isSelected());
		displayROIs("level", viewLevelsCheckbox.isSelected());
	}
	
	private void displayROIs(String filter, boolean visible)
	{
		Experiment exp = (Experiment)  parent0.expListCombo.getSelectedItem();
		if (exp == null) 
			return;		
		Viewer v= exp.seqKymos.seq.getFirstViewer();
		if (v == null)
			return;
		IcyCanvas canvas = v.getCanvas();
		List<Layer> layers = canvas.getLayers(false);
		if (layers != null)
		{	
			for (Layer layer: layers)
			{
				ROI roi = layer.getAttachedROI();
				if (roi != null)
				{
					String cs = roi.getName();
					if (cs.contains(filter))  
						layer.setVisible(visible);
				}
			}
		}
	}
	
	void displayON()
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
		{
			SequenceKymos seqKymos = exp.seqKymos;
			if (seqKymos == null || seqKymos.seq == null )
				return;
			
			ArrayList<Viewer>vList = seqKymos.seq.getViewers();
			if (vList.size() == 0)
			{
				Viewer v = new Viewer(seqKymos.seq, true);
				v.setRepeat(false);
				v.addListener(this);;
				placeKymoViewerNextToCamViewer(exp, v);
			}
		}
	}
	
	void placeKymoViewerNextToCamViewer(Experiment exp, Viewer v)
	{
		Viewer vCamData = exp.seqCamData.seq.getFirstViewer();
		if (vCamData == null)
			return;
		
		Rectangle rectMaster = vCamData.getBounds();
		int deltax = 5 + rectMaster.width;
		int deltay = 5;

		Rectangle rectDataView = v.getBounds();
		rectDataView.height = rectMaster.height;
		IcyBufferedImage img = exp.seqKymos.seq.getFirstImage();
		rectDataView.width = 100;
		if (img != null)
			rectDataView.width = 20 + img.getSizeX() * rectMaster.height / img.getSizeY();
		int desktopwidth = Icy.getMainInterface().getMainFrame().getDesktopWidth();
		if (rectDataView.width > desktopwidth)
		{
			int height = img.getSizeY() * desktopwidth / rectDataView.width;
			int width = img.getSizeX() * height / rectDataView.height;
			rectDataView.setSize(width, height *3 /2);
			rectDataView.x = 0;
			rectDataView.y = rectMaster.y + rectMaster.height;
		} 
		else
		{
		rectDataView.translate(
				rectMaster.x + deltax - rectDataView.x, 
				rectMaster.y + deltay - rectDataView.y);
		}
		v.setBounds(rectDataView);
	}
	
	void displayOFF()
	{
		Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
		if (exp == null || exp.seqKymos == null) 
			return;
		ArrayList<Viewer>vList =  exp.seqKymos.seq.getViewers();
		if (vList.size() > 0)
		{
			for (Viewer v: vList) 
				v.close();
			vList.clear();
		}
	}
	
	public void displayUpdateOnSwingThread()
	{		
		SwingUtilities.invokeLater(new Runnable() { public void run()
		{
			displayUpdate();
		}});
	}
	
	void displayUpdate()
	{	
		if (kymographsCombo.getItemCount() < 1)
			return;	
		displayON();
		int item = kymographsCombo.getSelectedIndex();
		if (item < 0) {
			item = indexImagesCombo >= 0 ? indexImagesCombo : 0;
			indexImagesCombo = -1;
			kymographsCombo.setSelectedIndex(item);
		}
		selectKymographImage(item); 
	}
	
	void displayViews(boolean bEnable)
	{
		updateButton.setEnabled(bEnable);
		previousButton.setEnabled(bEnable);
		nextButton.setEnabled(bEnable);
		kymographsCombo.setEnabled(bEnable);
		if (bEnable)
			displayUpdateOnSwingThread(); 
		else
			displayOFF();
	}
	
	public void selectKymographImage(int isel)
	{
		Experiment exp =(Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null) 
			return;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null || seqKymos.seq == null)
			return;
		
		if (isel < 0)
			isel = 0;
		if (isel >= seqKymos.seq.getSizeT() )
			isel = seqKymos.seq.getSizeT() -1;
		
		int icurrent = kymographsCombo.getSelectedIndex();
		if (icurrent != isel)
		{
			seqKymos.validateRoisAtT(icurrent);
			kymographsCombo.setSelectedIndex(isel);
		}
		seqKymos.currentFrame = isel;
		Viewer v = seqKymos.seq.getFirstViewer();
		if (v != null)
		{
			if( v.getPositionT() != isel)
				v.setPositionT(isel);
			
			v.setTitle(getKymographTitle(isel));
			parent0.paneKymos.tabDisplay.displayROIsAccordingToUserSelection();
		}
	}
	
	public String getKymographTitle(int t)
	{
		return parent0.expListCombo.expListBinSubDirectory + ": " 
				+((String) kymographsCombo.getSelectedItem()).substring(4);
	}
	
	@Override
	public void viewerChanged(ViewerEvent event)
	{
		if ( event.getType() == ViewerEvent.ViewerEventType.POSITION_CHANGED )
		{
			Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
			if (exp != null) 
			{
				Viewer v = exp.seqKymos.seq.getFirstViewer();
				int t = v.getPositionT();
				selectKymographImage(t);
			}
		}
	}

	@Override
	public void viewerClosed(Viewer viewer)
	{
		viewer.removeListener(this);
	}
	
	public void updateResultsAvailable(Experiment exp)
	{
		isActionEnabled = false;
		// isActionEnabled: hack to select the right directory and then add subsequent available dir without calling actionListener
		// see https://stackoverflow.com/questions/13434688/calling-additem-on-an-empty-jcombobox-triggers-actionperformed-event 
		// when JComboBox is empty, adding the first item will trigger setSelected(0)
		viewsCombo.removeAllItems();
		List<String> list = Directories.getSortedListOfSubDirectoriesWithTIFF(exp.getExperimentDirectory());
		for (int i = 0; i < list.size(); i++)
		{
			String dirName = list.get(i);
			if (dirName == null || dirName .contains(Experiment.RESULTS))
				dirName = ".";
			viewsCombo.addItem(dirName);
		}
		isActionEnabled = true;
		
		String select = exp.getBinSubDirectory();
		if (select == null)
			select = ".";
		viewsCombo.setSelectedItem(select);
	}
	
	public String getBinSubdirectory()
	{
		String name = (String) viewsCombo.getSelectedItem();
		if (name != null && !name .contains("bin_"))
			name = null;
		return name;
	}
	
	private void changeBinSubdirectory(String localString) 
	{
		Experiment exp = (Experiment)  parent0.expListCombo.getSelectedItem();
		if (exp == null 
			|| localString == null 
			|| exp.getBinSubDirectory() .contains(localString))
			return;
		
		parent0.expListCombo.expListBinSubDirectory = localString;
		exp.setBinSubDirectory(localString);
		exp.seqKymos.seq.close();
		exp.loadKymographs();
		displayON();
		parent0.paneKymos.updateDialogs(exp);
	}

}
