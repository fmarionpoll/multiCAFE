package plugins.fmp.multicafe;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;



public class MCCapillaries_Infos extends JPanel {
	/**
	 * 
	 */
	private static final long 	serialVersionUID 			= 4950182090521600937L;
	private JSpinner 			capillaryVolumeTextField	= new JSpinner(new SpinnerNumberModel(5., 0., 100., 1.));
	private JSpinner 			capillaryPixelsTextField	= new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
	private JButton				getLenButton				= new JButton ("pixels 1rst capillary");
	private JButton				editCapillariesButton		= new JButton("edit capillaries");
	private MultiCAFE 			parent0 					= null;
	private MCCapillaries_Table dialog 						= null;
	private List <Capillary> 	capillariesArrayCopy 		= new ArrayList<Capillary>();
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		JPanel panel0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
		panel0.add( new JLabel("volume (µl) ", SwingConstants.RIGHT));
		panel0.add( capillaryVolumeTextField);
		panel0.add( new JLabel("length (pixels) ", SwingConstants.RIGHT));
		panel0.add( capillaryPixelsTextField);
		panel0.add( getLenButton);
		add( GuiUtil.besidesPanel(panel0));
		
		JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
		panel1.add( editCapillariesButton);
		add( GuiUtil.besidesPanel(panel1));

		defineActionListeners();
	}
	
	private void defineActionListeners() {
		getLenButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				exp.updateCapillariesFromCamData();
				if (exp != null && exp.capillaries.capillariesArrayList.size() > 0) {
					Capillary cap = exp.capillaries.capillariesArrayList.get(0);
					ArrayList<Point2D> pts = cap.roi.getPoints();
					Point2D pt1 = pts.get(0);
					Point2D pt2 = pts.get(pts.size() -1);
					double npixels = Math.sqrt(
							(pt2.getY() - pt1.getY()) * (pt2.getY() - pt1.getY()) 
							+ (pt2.getX() - pt1.getX()) * (pt2.getX() - pt2.getX()));
					capillaryPixelsTextField.setValue((int) npixels);
				}
			}});
		
		editCapillariesButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				exp.capillaries.transferDescriptionToCapillaries();
				dialog = new MCCapillaries_Table();
            	dialog.initialize(parent0, capillariesArrayCopy);
			}});
	}

	// set/ get
	
	void setAllDescriptors(Capillaries cap) {
		capillaryVolumeTextField.setValue( cap.desc.volume);
		capillaryPixelsTextField.setValue( cap.desc.pixels);
	}

	private double getCapillaryVolume() {
		return (double) capillaryVolumeTextField.getValue();
	}
	
	private int getCapillaryPixelLength() {
		return (int) capillaryPixelsTextField.getValue(); 
	}
	
	void getDescriptors(Capillaries capList) {
		capList.desc.volume = getCapillaryVolume();
		capList.desc.pixels = getCapillaryPixelLength();
	}
	

	

						
}
