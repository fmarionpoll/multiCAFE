package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.roi.ROI;
import plugins.fmp.multicafeSequence.Capillaries;

public class MCCapillariesTab_Units extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4950182090521600937L;

	JCheckBox					visibleCheckBox				= new JCheckBox("ROIs visible", true);
	private JSpinner 			capillaryVolumeTextField	= new JSpinner(new SpinnerNumberModel(5., 0., 100., 1.));
	private JSpinner 			capillaryPixelsTextField	= new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
	private JComboBox<String> 	stimulusRJCombo				= new JComboBox<String>();
	private JComboBox<String> 	concentrationRJCombo 		= new JComboBox<String>();
	private JComboBox<String> 	stimulusLJCombo				= new JComboBox<String>();
	private JComboBox<String> 	concentrationLJCombo 		= new JComboBox<String>();
	
	private MultiCAFE parent0;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		
		add( GuiUtil.besidesPanel(
				visibleCheckBox,
				new JLabel("volume (µl) ", SwingConstants.RIGHT), 
				capillaryVolumeTextField,  
				new JLabel("length (pixels) ", SwingConstants.RIGHT), 
				capillaryPixelsTextField));
		
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
		
		this.parent0 = parent0;
		defineActionListeners();
	}
			
	private void defineActionListeners() {
		visibleCheckBox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			roisDisplayLine(visibleCheckBox.isSelected());
		} } );
	}
			
	private JPanel createComboPanel(String text, JComboBox<String> combo) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel(text, SwingConstants.RIGHT), BorderLayout.WEST); 
		panel.add(combo, BorderLayout.CENTER);
		return panel;
	}

	
	private void roisDisplayLine(boolean isVisible) {
		ArrayList<Viewer>vList =  parent0.seqCamData.seq.getViewers();
		Viewer v = vList.get(0);
		IcyCanvas canvas = v.getCanvas();
		List<Layer> layers = canvas.getLayers(false);
		if (layers == null)
			return;
		for (Layer layer: layers) {
			ROI roi = layer.getAttachedROI();
			if (roi == null)
				continue;
			String cs = roi.getName();
			if (cs.contains("line"))  
				layer.setVisible(isVisible);
		}
	}
		
	// set/ get
	
	void setCapillariesInfosToDialog(Capillaries cap) {
		capillaryVolumeTextField.setValue( cap.volume);
		capillaryPixelsTextField.setValue( cap.pixels);
		addItem(stimulusRJCombo, cap.stimulusR);
		addItem(concentrationRJCombo, cap.concentrationR);
		addItem(stimulusLJCombo, cap.stimulusL);
		addItem(concentrationLJCombo, cap.concentrationL);
	}

	
	private double getCapillaryVolume() {
		return (double) capillaryVolumeTextField.getValue();
	}
	
	private int getCapillaryPixelLength() {
		return (int) capillaryPixelsTextField.getValue(); 
	}
	
	void getCapillariesInfos(Capillaries cap) {
		cap.volume = getCapillaryVolume();
		cap.pixels = getCapillaryPixelLength();
		cap.stimulusR = (String) stimulusRJCombo.getSelectedItem();
		cap.concentrationR = (String) concentrationRJCombo.getSelectedItem();
		cap.stimulusL = (String) stimulusLJCombo.getSelectedItem();
		cap.concentrationL = (String) concentrationLJCombo.getSelectedItem();
	}
	
	private void addItem(JComboBox<String> combo, String text) {
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
