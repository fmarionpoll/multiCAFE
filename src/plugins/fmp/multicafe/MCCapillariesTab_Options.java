package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.main.ActiveViewerListener;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI;
import icy.sequence.Sequence;
import plugins.fmp.multicafeSequence.Capillary;



public class MCCapillariesTab_Options extends JPanel implements ActiveViewerListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2103052112476748890L;
	JComboBox<String> kymographNamesComboBox = new JComboBox<String> (new String[] {"none"});
	JButton 	updateButton 			= new JButton("Update");
	JButton  	previousButton		 	= new JButton("<");
	JButton		nextButton				= new JButton(">");
	JCheckBox 	viewKymosCheckBox 		= new JCheckBox("View kymos");
	JCheckBox 	viewLevelsCheckbox 		= new JCheckBox("capillary levels", true);
	JCheckBox 	viewDerivativeCheckbox 	= new JCheckBox("derivative", true);
	JCheckBox 	viewGulpsCheckbox 		= new JCheckBox("gulps", true);

	private MultiCAFE parent0 = null;

	void init(GridLayout capLayout, MultiCAFE parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		
		JPanel k2Panel = new JPanel();
		k2Panel.setLayout(new BorderLayout());
		k2Panel.add(previousButton, BorderLayout.WEST); 
		int bWidth = 30;
		int height = 10;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		k2Panel.add(kymographNamesComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, height)); 
		k2Panel.add(nextButton, BorderLayout.EAST);
		add(GuiUtil.besidesPanel( viewKymosCheckBox, k2Panel));
		
		add(GuiUtil.besidesPanel( viewLevelsCheckbox, viewGulpsCheckbox, updateButton));
		add(GuiUtil.besidesPanel( viewDerivativeCheckbox, new JLabel(" "), new JLabel(" ")));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		updateButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			displayUpdateOnSwingThread();
		} } );
		
		kymographNamesComboBox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			displayUpdateOnSwingThread();
		} } );
		
		viewDerivativeCheckbox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplay("derivative", viewDerivativeCheckbox.isSelected());
		} } );

		viewGulpsCheckbox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplay("gulp", viewGulpsCheckbox.isSelected());
		} } );
		
		viewLevelsCheckbox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplay("level", viewLevelsCheckbox.isSelected());
		} } );
		
		viewKymosCheckBox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			displayViews(viewKymosCheckBox.isSelected());
		} } );
		
		nextButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			int isel = kymographNamesComboBox.getSelectedIndex()+1;
			if (isel < kymographNamesComboBox.getItemCount())
				selectKymograph(isel);
		} } );
		
		previousButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			int isel = kymographNamesComboBox.getSelectedIndex()-1;
			if (isel < kymographNamesComboBox.getItemCount())
				selectKymograph(isel);
		} } );
	}
		
	// ---------------------------
	void transferFileNamesToComboBox() {
		kymographNamesComboBox.removeAllItems();
		if (parent0.vkymos != null) {
			for (Capillary cap: parent0.vkymos.capillaries.capillariesArrayList) 
				kymographNamesComboBox.addItem(cap.getName());
		}
	}
	
	void transferRoisNamesToComboBox(ArrayList <Capillary> capillaryArrayList) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				kymographNamesComboBox.removeAllItems();
				for (Capillary cap: capillaryArrayList)
					kymographNamesComboBox.addItem(cap.getName());	
			}});
	}
	
	private void roisDisplay(String filter, boolean visible) {

		ArrayList<Viewer>vList =  parent0.vkymos.getViewers();
		IcyCanvas canvas = vList.get(0).getCanvas();
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
		if (parent0.vkymos == null 
			||parent0.vkymos.getSizeT() < 1) {
			System.out.println("displayON() skipped");
			return;
		}

		Rectangle rectMaster = parent0.vSequence.getFirstViewer().getBounds();
		int deltax = 5 + rectMaster.width;
		int deltay = 5;
		
		ArrayList<Viewer>vList = parent0.vkymos.getViewers();
		if (vList.size() == 0) 
		{
			Viewer v = new Viewer(parent0.vkymos, true);
			v.addListener(parent0);
			Rectangle rectDataView = v.getBounds();
			rectDataView.height = rectMaster.height;
			IcyBufferedImage img = parent0.vkymos.getFirstImage();
			rectDataView.width = 100;
			if (img != null)
				rectDataView.width = 20 + img.getSizeX() * rectMaster.height / img.getSizeY();
			rectDataView.translate(
					rectMaster.x + deltax - rectDataView.x, 
					rectMaster.y + deltay - rectDataView.y);
			v.setBounds(rectDataView);
		}
		Icy.getMainInterface().addActiveViewerListener(this);
	}
	
	void displayOFF() {

		if (parent0.vkymos == null) 
			return;
		ArrayList<Viewer>vList =  parent0.vkymos.getViewers();
		if (vList.size() > 0) 
		{
			for (Viewer v: vList) 
				v.close();
			vList.clear();
		}
		Icy.getMainInterface().removeActiveViewerListener(this);
	}
	
	void displayUpdateOnSwingThread() {		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				displayUpdate();
			}
		});
	}
	
	void displayUpdate() {	

		if (parent0.vkymos == null || kymographNamesComboBox.getItemCount() < 1)
			return;	
		displayON();
		int itemupfront = kymographNamesComboBox.getSelectedIndex();
		if (itemupfront < 0) {
			itemupfront = 0;
			kymographNamesComboBox.setSelectedIndex(0);
		}
		parent0.vkymos.displayImageAt(itemupfront);
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
		int icurrent = kymographNamesComboBox.getSelectedIndex();
		if (isel < 0)
			isel = 0;
		if (isel >= parent0.vkymos.getSizeT() )
			isel = parent0.vkymos.getSizeT() -1;
		if (icurrent != isel) {
			kymographNamesComboBox.setSelectedIndex(isel);
		}

		Viewer v =  Icy.getMainInterface().getFirstViewer(parent0.vkymos);
		v.setPositionT(isel);
		v.setTitle(parent0.vkymos.getDecoratedImageName(isel));
	}

	@Override
	public void viewerActivated(Viewer viewer) {
	}

	@Override
	public void viewerDeactivated(Viewer viewer) {
		if (viewer != null) {
			Sequence seq = viewer.getSequence();
			if (seq != null)
				seq.setSelectedROI(null);
		}
	}

	@Override
	public void activeViewerChanged(ViewerEvent event) {		
	}
}
