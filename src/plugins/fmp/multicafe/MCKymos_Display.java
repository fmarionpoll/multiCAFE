package plugins.fmp.multicafe;

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
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI;

import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;





public class MCKymos_Display extends JPanel implements ViewerListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2103052112476748890L;
	JComboBox<String> kymographNamesComboBox = new JComboBox<String> (new String[] {"none"});
	JButton 	updateButton 			= new JButton("Update");
	JButton  	previousButton		 	= new JButton("<");
	JButton		nextButton				= new JButton(">");
	JCheckBox 	viewLevelsCheckbox 		= new JCheckBox("top/bottom level (green)", true);
	JCheckBox 	viewDerivativeCheckbox 	= new JCheckBox("derivative (yellow)", true);
	JCheckBox 	viewGulpsCheckbox 		= new JCheckBox("gulps (red)", true);
	JComboBox<String> availableResultsCombo	= new JComboBox <String>();
	private MultiCAFE parent0 			= null;
	boolean 	actionAllowed			= true;

	
	void init(GridLayout capLayout, MultiCAFE parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		
		JPanel panel1 = new JPanel (layout);
		panel1.add(new JLabel("available views:"));
		panel1.add(availableResultsCombo);
		panel1.add(new JLabel(" kymograph:"));
		int bWidth = 30;
		int bHeight = 21;
		panel1.add(previousButton, BorderLayout.WEST); 
		previousButton.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(kymographNamesComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, bHeight)); 
		panel1.add(nextButton, BorderLayout.EAST);
		add(GuiUtil.besidesPanel(panel1));
		
		JPanel panel2 = new JPanel (layout);
		add(GuiUtil.besidesPanel(panel2));
		
		JPanel panel3 = new JPanel (layout);
		panel3.add(viewLevelsCheckbox);
		panel3.add(viewDerivativeCheckbox);
		panel3.add(viewGulpsCheckbox);
		add(GuiUtil.besidesPanel(panel3));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
//		updateButton.addActionListener(new ActionListener () { 
//			@Override public void actionPerformed( final ActionEvent e ) { 
//			displayUpdateOnSwingThread();
//		} } );
		
		kymographNamesComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			displayUpdateOnSwingThread();
		} } );
		
		viewDerivativeCheckbox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplay("deriv", viewDerivativeCheckbox.isSelected());
		} } );

		viewGulpsCheckbox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplay("gulp", viewGulpsCheckbox.isSelected());
		} } );
		
		viewLevelsCheckbox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplay("level", viewLevelsCheckbox.isSelected());
		} } );
		
		nextButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			int isel = kymographNamesComboBox.getSelectedIndex()+1;
			if (isel < kymographNamesComboBox.getItemCount())
				selectKymograph(isel);
		} } );
		
		previousButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			int isel = kymographNamesComboBox.getSelectedIndex()-1;
			if (isel < kymographNamesComboBox.getItemCount())
				selectKymograph(isel);
		} } );
		
		availableResultsCombo.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (!actionAllowed || exp == null)
				return;
			String localString = (String) availableResultsCombo.getSelectedItem();
			if (localString != null && !localString.contentEquals(exp.resultsSubPath)) {
				firePropertyChange("SEQ_CHGBIN", false, true);
			}
		} } );
	}
		
	void transferCapillaryNamesToComboBox(List <Capillary> capillaryArrayList) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				kymographNamesComboBox.removeAllItems();
				Collections.sort(capillaryArrayList); 
				for (Capillary cap: capillaryArrayList) 
					kymographNamesComboBox.addItem(cap.roi.getName());	
			}});
	}
	
	public void displayRoisAccordingToUserSelection() {
		roisDisplay("deriv", viewDerivativeCheckbox.isSelected());
		roisDisplay("gulp", viewGulpsCheckbox.isSelected());
		roisDisplay("level", viewLevelsCheckbox.isSelected());

	}
	
	private void roisDisplay(String filter, boolean visible) {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp == null) 
			return;		
		Viewer v= exp.seqKymos.seq.getFirstViewer();
		if (v == null)
			return;
		IcyCanvas canvas = v.getCanvas();
		List<Layer> layers = canvas.getLayers(false);
		if (layers != null) {	
			for (Layer layer: layers) {
				ROI roi = layer.getAttachedROI();
				if (roi != null) {
					String cs = roi.getName();
					if (cs.contains(filter))  
						layer.setVisible(visible);
				}
			}
		}
	}

	void displayON() {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp == null)
			return;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null || seqKymos.seq == null ) {
			return;
		}
		
		ArrayList<Viewer>vList = seqKymos.seq.getViewers();
		if (vList.size() == 0) {
			Viewer viewer = exp.seqCamData.seq.getFirstViewer();
			if (viewer == null)
				return;
			Rectangle rectMaster = viewer.getBounds();
			int deltax = 5 + rectMaster.width;
			int deltay = 5;

			Viewer v = new Viewer(seqKymos.seq, true);
			v.addListener(this);
			Rectangle rectDataView = v.getBounds();
			rectDataView.height = rectMaster.height;
			IcyBufferedImage img = seqKymos.seq.getFirstImage();
			rectDataView.width = 100;
			if (img != null) {
				rectDataView.width = 20 + img.getSizeX() * rectMaster.height / img.getSizeY();
			}
			int desktopwidth = Icy.getMainInterface().getMainFrame().getDesktopWidth();
			if (rectDataView.width > desktopwidth) {
				int height = img.getSizeY() * desktopwidth / rectDataView.width;
				int width = img.getSizeX() * height / rectDataView.height;
				rectDataView.setSize(width, height *3 /2);
				rectDataView.x = 0;
				rectDataView.y = rectMaster.y + rectMaster.height;
			} else {
			rectDataView.translate(
					rectMaster.x + deltax - rectDataView.x, 
					rectMaster.y + deltay - rectDataView.y);
			}
			v.setBounds(rectDataView);
		}
	}
	
	void displayOFF() {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp == null || exp.seqKymos == null) 
			return;
		ArrayList<Viewer>vList =  exp.seqKymos.seq.getViewers();
		if (vList.size() > 0) {
			for (Viewer v: vList) 
				v.close();
			vList.clear();
		}
	}
	
	public void displayUpdateOnSwingThread() {		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				displayUpdate();
			}});
	}
	
	void displayUpdate() {	
		if (kymographNamesComboBox.getItemCount() < 1)
			return;	
		displayON();
		int itemupfront = kymographNamesComboBox.getSelectedIndex();
		if (itemupfront < 0) {
			itemupfront = 0;
			kymographNamesComboBox.setSelectedIndex(0);
		}
		selectKymograph(itemupfront); 
	}

	void displayViews (boolean bEnable) {
		updateButton.setEnabled(bEnable);
		previousButton.setEnabled(bEnable);
		nextButton.setEnabled(bEnable);
		kymographNamesComboBox.setEnabled(bEnable);
		if (bEnable)
			displayUpdateOnSwingThread(); 
		else
			displayOFF();
	}

	void selectKymograph(int isel) {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp == null) 
			return;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null || seqKymos.seq == null)
			return;
		
		if (isel < 0)
			isel = 0;
		if (isel >= seqKymos.seq.getSizeT() )
			isel = seqKymos.seq.getSizeT() -1;
		
		int icurrent = kymographNamesComboBox.getSelectedIndex();
		if (icurrent != isel) {
			seqKymos.validateRoisAtT(icurrent);
			kymographNamesComboBox.setSelectedIndex(isel);
		}
		seqKymos.currentFrame = isel;
		Viewer v = seqKymos.seq.getFirstViewer();
		if (v != null) {
			if( v.getPositionT() != isel)
				v.setPositionT(isel);
			String name = exp.seqCamData.getCSFileName() +": " + (String) kymographNamesComboBox.getSelectedItem();
			v.setTitle(name);
			parent0.paneKymos.tabDisplay.displayRoisAccordingToUserSelection();
		}
	}

	@Override
	public void viewerChanged(ViewerEvent event) {
		if ( event.getType() == ViewerEvent.ViewerEventType.POSITION_CHANGED ) {
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (exp == null) 
				return;

			Viewer v = exp.seqKymos.seq.getFirstViewer();
			int t = v.getPositionT();
			selectKymograph(t);
		}
		
	}

	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
	}

	void updateResultsAvailable(Experiment exp) {
		actionAllowed = false;
		availableResultsCombo.removeAllItems();
		List<String> list = new ArrayList<String> (exp.resultsDirList);
		for (int i = 0; i < list.size(); i++) {
			String dirName = list.get(i);
			availableResultsCombo.addItem(dirName);
		}
		availableResultsCombo.setSelectedItem(exp.resultsSubPath);
		actionAllowed = true;
	}
}
