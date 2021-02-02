package plugins.fmp.multicafe.dlg.kymos;

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
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Capillary;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.SequenceKymos;





public class Display extends JPanel implements ViewerListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2103052112476748890L;
	public 	JComboBox<String> kymosComboBox = new JComboBox<String> (new String[] {"none"});
	private MultiCAFE 	parent0 			= null;
			JButton 	updateButton 			= new JButton("Update");
			JButton  	previousButton		 	= new JButton("<");
			JButton		nextButton				= new JButton(">");
			JCheckBox 	viewLevelsCheckbox 		= new JCheckBox("top/bottom level (green)", true);
			JCheckBox 	viewDerivativeCheckbox 	= new JCheckBox("derivative (yellow)", true);
			JCheckBox 	viewGulpsCheckbox 		= new JCheckBox("gulps (red)", true);
			JComboBox<String> binsCombo	= new JComboBox <String>();
			boolean 	actionAllowed			= true;

	
	void init(GridLayout capLayout, MultiCAFE parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		
		JPanel panel1 = new JPanel (layout);
		panel1.add(new JLabel("views"));
		panel1.add(binsCombo);
		panel1.add(new JLabel(" kymograph"));
		int bWidth = 30;
		int bHeight = 21;
		panel1.add(previousButton, BorderLayout.WEST); 
		previousButton.setPreferredSize(new Dimension(bWidth, bHeight));
		panel1.add(kymosComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, bHeight)); 
		panel1.add(nextButton, BorderLayout.EAST);
		add(panel1);
		
		JPanel panel2 = new JPanel (layout);
		add(panel2);
		
		JPanel panel3 = new JPanel (layout);
		panel3.add(viewLevelsCheckbox);
		panel3.add(viewDerivativeCheckbox);
		panel3.add(viewGulpsCheckbox);
		add(panel3);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {		
		kymosComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			displayUpdateOnSwingThread();
		}});
		
		viewDerivativeCheckbox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplay("deriv", viewDerivativeCheckbox.isSelected());
		}});

		viewGulpsCheckbox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplay("gulp", viewGulpsCheckbox.isSelected());
		}});
		
		viewLevelsCheckbox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplay("level", viewLevelsCheckbox.isSelected());
		}});
		
		nextButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			int isel = kymosComboBox.getSelectedIndex()+1;
			if (isel < kymosComboBox.getItemCount())
				selectKymograph(isel);
		}});
		
		previousButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
			int isel = kymosComboBox.getSelectedIndex()-1;
			if (isel < kymosComboBox.getItemCount())
				selectKymograph(isel);
		}});
		
		binsCombo.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (!actionAllowed || exp == null)
				return;
			String localString = (String) binsCombo.getSelectedItem();
			if (localString != null) 
				firePropertyChange("SEQ_CHGBIN", false, true);
		}});
	}
		
	public void transferCapillaryNamesToComboBox(Experiment exp ) {
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			kymosComboBox.removeAllItems();
			Collections.sort(exp.capillaries.capillariesArrayList); 
			int ncapillaries = exp.capillaries.capillariesArrayList.size();
			for (int i=0; i< ncapillaries; i++) {
				Capillary cap = exp.capillaries.capillariesArrayList.get(i);
				kymosComboBox.addItem(cap.roi.getName());
			}
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
			Viewer v = new Viewer(seqKymos.seq, true);
			v.setRepeat(false);
			v.addListener(this);
			
			Viewer vCamData = exp.seqCamData.seq.getFirstViewer();
			if (vCamData == null)
				return;
			Rectangle rectMaster = vCamData.getBounds();
			int deltax = 5 + rectMaster.width;
			int deltay = 5;

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
		if (kymosComboBox.getItemCount() < 1)
			return;	
		displayON();
		int itemupfront = kymosComboBox.getSelectedIndex();
		if (itemupfront < 0) {
			itemupfront = 0;
			kymosComboBox.setSelectedIndex(0);
		}
		selectKymograph(itemupfront); 
	}

	void displayViews (boolean bEnable) {
		updateButton.setEnabled(bEnable);
		previousButton.setEnabled(bEnable);
		nextButton.setEnabled(bEnable);
		kymosComboBox.setEnabled(bEnable);
		if (bEnable)
			displayUpdateOnSwingThread(); 
		else
			displayOFF();
	}

	public void selectKymograph(int isel) {
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
		
		int icurrent = kymosComboBox.getSelectedIndex();
		if (icurrent != isel) {
			seqKymos.validateRoisAtT(icurrent);
			kymosComboBox.setSelectedIndex(isel);
		}
		seqKymos.currentFrame = isel;
		Viewer v = seqKymos.seq.getFirstViewer();
		if (v != null) {
			if( v.getPositionT() != isel)
				v.setPositionT(isel);
			String name = exp.seqCamData.getCSCamFileName() +": " + (String) kymosComboBox.getSelectedItem();
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

	public void updateResultsAvailable(Experiment exp) {
		actionAllowed = false;
		binsCombo.removeAllItems();
		List<String> list = exp.getListOfSubDirectoriesWithTIFF();
		for (int i = 0; i < list.size(); i++) {
			String dirName = list.get(i);
			if (dirName == null || dirName .contains(exp.RESULTS))
				dirName = ".";
			binsCombo.addItem(dirName);
		}
		String select = exp.getBinSubDirectory();
		if (select == null)
			select = ".";
		binsCombo.setSelectedItem(select);
		actionAllowed = true;
	}
	
	public String getBinSubdirectory() {
		String name = (String) binsCombo.getSelectedItem();
		if (name != null && !name .contains("bin_"))
			name = null;
		return name;
	}
	
}
