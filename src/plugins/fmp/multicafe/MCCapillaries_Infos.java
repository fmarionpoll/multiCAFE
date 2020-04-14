package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
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
	private static final long serialVersionUID = 4950182090521600937L;

//	JCheckBox					visibleCheckBox				= new JCheckBox("ROIs visible", true);
	private JSpinner 			capillaryVolumeTextField	= new JSpinner(new SpinnerNumberModel(5., 0., 100., 1.));
	private JSpinner 			capillaryPixelsTextField	= new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
	private JComboBox<String> 	stimulusRJCombo				= new JComboBox<String>();
	private JComboBox<String> 	concentrationRJCombo 		= new JComboBox<String>();
	private JComboBox<String> 	stimulusLJCombo				= new JComboBox<String>();
	private JComboBox<String> 	concentrationLJCombo 		= new JComboBox<String>();
	private JButton				getLenButton				= new JButton ("get pixels 1rst capillary");
	private MultiCAFE 			parent0 					= null;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		JPanel panel0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		((FlowLayout)panel0.getLayout()).setVgap(0);
		panel0.add( new JLabel("volume (µl) ", SwingConstants.RIGHT));
		panel0.add( capillaryVolumeTextField);
		panel0.add( new JLabel("length (pixels) ", SwingConstants.RIGHT));
		panel0.add( capillaryPixelsTextField);
		panel0.add( getLenButton);
		add( GuiUtil.besidesPanel(panel0));
		
		add( GuiUtil.besidesPanel(
				createComboPanel("stim(L) ", stimulusLJCombo),  
				createComboPanel("  conc(L) ", concentrationLJCombo)));
		
		add( GuiUtil.besidesPanel(
				createComboPanel("stim(R) ", stimulusRJCombo),  
				createComboPanel("  conc(R) ", concentrationRJCombo)));
		
		stimulusRJCombo.setEditable(true);
		concentrationRJCombo.setEditable(true);
		stimulusLJCombo.setEditable(true);
		concentrationLJCombo.setEditable(true);	
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		getLenButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				exp.updateCapillariesFromCamData();
				if (exp != null && exp.capillaries.capillariesArrayList.size() > 0) {
					Capillary cap = exp.capillaries.capillariesArrayList.get(0);
					ArrayList<Point2D> pts = cap.capillaryRoi.getPoints();
					Point2D pt1 = pts.get(0);
					Point2D pt2 = pts.get(pts.size() -1);
					double npixels = Math.sqrt(
							(pt2.getY() - pt1.getY()) * (pt2.getY() - pt1.getY()) 
							+ (pt2.getX() - pt1.getX()) * (pt2.getX() - pt2.getX()));					
/*					
					Line2D line = new Line2D.Double(pt1, pt2);
					double distance = line.getP1().distance(line.getP2());
					exp.seqCamData.seq.getPixelSizeX();
					Line2D line = new Line2D.Double(a1.getPosition(), a2.getPosition());
					// transform and display ticks
					lineDefinitionList.clear();
					AffineTransform originalTransform = g.getTransform();
					double realDistance = Math.sqrt((Math.pow(vx * distance * sequence.getPixelSizeX(), 2) + Math.pow(vy * distance * sequence.getPixelSizeY(), 2)));
					pixelString = " " + (int) distance + " px" + " / " + UnitUtil.getBestUnitInMeters(realDistance, 2, UnitPrefix.MICRO);
					Rectangle2D pixelBounds = GraphicsUtil.getStringBounds(g, font, pixelString);
					g.translate(distance / 2 - pixelBounds.getWidth() / 2, -convertScale(canvas, 20));
					double distance = line.getP1().distance(line.getP2());
*/
					capillaryPixelsTextField.setValue((int) npixels);
				}
			}});
	}
				
	private JPanel createComboPanel(String text, JComboBox<String> combo) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel(text, SwingConstants.RIGHT), BorderLayout.WEST); 
		panel.add(combo, BorderLayout.CENTER);
		return panel;
	}

	// set/ get
	
	void setAllDescriptors(Capillaries cap) {
		capillaryVolumeTextField.setValue( cap.desc.volume);
		capillaryPixelsTextField.setValue( cap.desc.pixels);
		setTextDescriptors(cap);
	}
	
	void setTextDescriptors(Capillaries cap) {
		addItem(stimulusRJCombo, cap.desc.stimulusR);
		addItem(concentrationRJCombo, cap.desc.concentrationR);
		addItem(stimulusLJCombo, cap.desc.stimulusL);
		addItem(concentrationLJCombo, cap.desc.concentrationL);
	}

	private double getCapillaryVolume() {
		return (double) capillaryVolumeTextField.getValue();
	}
	
	private int getCapillaryPixelLength() {
		return (int) capillaryPixelsTextField.getValue(); 
	}
	
	void getDescriptors(Capillaries cap) {
		cap.desc.volume = getCapillaryVolume();
		cap.desc.pixels = getCapillaryPixelLength();
		cap.desc.stimulusR = (String) stimulusRJCombo.getSelectedItem();
		cap.desc.concentrationR = (String) concentrationRJCombo.getSelectedItem();
		cap.desc.stimulusL = (String) stimulusLJCombo.getSelectedItem();
		cap.desc.concentrationL = (String) concentrationLJCombo.getSelectedItem();
	}
	
	private void addItem(JComboBox<String> combo, String text) {
		if (text == null)
			return;
		combo.setSelectedItem(text);
		if (combo.getSelectedIndex() < 0) {
			boolean found = false;
			for (int i=0; i < combo.getItemCount(); i++) {
				int comparison = text.compareTo(combo.getItemAt(i));
				if (comparison > 0)
					continue;
				if (comparison < 0) {
					found = true;
					combo.insertItemAt(text, i);
					break;
				}
			}
			if (!found)
				combo.addItem(text);
			combo.setSelectedItem(text);
		}
	}
	
	void updateCombos() {
		addItem(stimulusRJCombo, (String) stimulusRJCombo.getSelectedItem());
		addItem(concentrationRJCombo, (String) concentrationRJCombo.getSelectedItem());
		addItem(stimulusLJCombo, (String) stimulusLJCombo.getSelectedItem());
		addItem(concentrationLJCombo, (String) concentrationLJCombo.getSelectedItem());
	}
						
}
