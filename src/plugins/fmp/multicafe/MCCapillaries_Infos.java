package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Capillaries;



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
	
	
	void init(GridLayout capLayout) {
		setLayout(capLayout);
		
		add( GuiUtil.besidesPanel(
				new JLabel(" "), //visibleCheckBox,
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
	}
				
	private JPanel createComboPanel(String text, JComboBox<String> combo) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel(text, SwingConstants.RIGHT), BorderLayout.WEST); 
		panel.add(combo, BorderLayout.CENTER);
		return panel;
	}

	// set/ get
	
	void setCapillariesInfosToDialog(Capillaries cap) {
		capillaryVolumeTextField.setValue( cap.desc.volume);
		capillaryPixelsTextField.setValue( cap.desc.pixels);
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
	
	void getCapillariesInfosFromDialog(Capillaries cap) {
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
