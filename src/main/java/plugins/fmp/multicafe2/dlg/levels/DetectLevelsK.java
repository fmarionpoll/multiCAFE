package plugins.fmp.multicafe2.dlg.levels;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import plugins.nherve.toolbox.image.toolboxes.ColorSpaceTools;
import plugins.fmp.multicafe2.MultiCAFE2;


public class DetectLevelsK  extends JPanel implements PropertyChangeListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6031521157029550040L;
	JSpinner			startSpinner			= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner			endSpinner				= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	private JCheckBox	allKymosCheckBox 		= new JCheckBox ("all kymographs", true);
	private String 		detectString 			= "        Detect     ";
	private JButton 	detectButton 			= new JButton(detectString);
	private JCheckBox	fromCheckBox 			= new JCheckBox (" from (pixel)", false);
	private JCheckBox 	allSeriesCheckBox 		= new JCheckBox("ALL (current to last)", false);
	private JCheckBox	leftCheckBox 			= new JCheckBox ("L", true);
	private JCheckBox	rightCheckBox 			= new JCheckBox ("R", true);

	private JComboBox<String> cbColorSpace = new JComboBox<String> (new String[] {
			ColorSpaceTools.COLOR_SPACES[ColorSpaceTools.RGB],
			ColorSpaceTools.COLOR_SPACES[ColorSpaceTools.RGB_TO_HSV],
			ColorSpaceTools.COLOR_SPACES[ColorSpaceTools.RGB_TO_H1H2H3]
			});
	private JSpinner 	tfNbCluster2  = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
	private JSpinner 	tfNbIteration2 = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
	private JSpinner 	tfStabCrit2 = new JSpinner(new SpinnerNumberModel(0.001, 0.001, 1000, .1));
	private MultiCAFE2 	parent0 	= null;
	
	// -----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT); 
		
		JPanel panel0 = new JPanel(layoutLeft);
		((FlowLayout)panel0.getLayout()).setVgap(0);
		panel0.add(detectButton);
		panel0.add(allSeriesCheckBox);
		panel0.add(allKymosCheckBox);
		panel0.add(leftCheckBox);
		panel0.add(rightCheckBox);
		add(panel0);
		
		JPanel panel01 = new JPanel(layoutLeft);
		panel01.add (new JLabel("Color space"));
		panel01.add (cbColorSpace);
		panel01.add (new JLabel ("Nb clusters"));
		panel01.add (tfNbCluster2);
		panel01.add (new JLabel ("N max iterations"));
		panel01.add (tfNbIteration2);
		
		add (panel01);
		
		JPanel panel1 = new JPanel(layoutLeft);
		panel1.add (new JLabel ("Stab. criterion"));
		panel1.add( tfStabCrit2);
		panel1.add(fromCheckBox);
		panel1.add(startSpinner);
		panel1.add(new JLabel("to"));
		panel1.add(endSpinner);
		add( panel1);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{	
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		
	}


}
