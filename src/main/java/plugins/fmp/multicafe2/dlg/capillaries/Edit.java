package plugins.fmp.multicafe2.dlg.capillaries;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import plugins.fmp.multicafe2.MultiCAFE2;

public class Edit extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7582410775062671523L;
	
	private JLabel		toLabel 				= new JLabel(" to ");
	private JCheckBox	fromCheckBox 			= new JCheckBox (" from frame", false);
	JSpinner			startSpinner			= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner			endSpinner				= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	
	private MultiCAFE2 	parent0 				= null;
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);	
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		
		JPanel panel0 = new JPanel(flowLayout);
		
		panel0.add(fromCheckBox);
		panel0.add(startSpinner);
		startSpinner.setPreferredSize(new Dimension (40, 20));
		panel0.add(toLabel);
		panel0.add(endSpinner);
		endSpinner.setPreferredSize(new Dimension (40, 20));
		
		boolean status = false;
		fromCheckBox.setEnabled(status);
		startSpinner.setEnabled(status);
		toLabel.setEnabled(status);
		endSpinner.setEnabled(status);
		
		add(panel0);
		
		
		defineActionListeners();
		this.setParent0(parent0);
	}
	
	private void defineActionListeners() 
	{
//		addPolygon2DButton.addActionListener(new ActionListener () 
//		{ 
//			@Override public void actionPerformed( final ActionEvent e ) 
//			{ 
//				create2DPolygon();
//			}});
		
	}

	public MultiCAFE2 getParent0() {
		return parent0;
	}

	public void setParent0(MultiCAFE2 parent0) {
		this.parent0 = parent0;
	}
	
//	private void EnableBinWidthItems(boolean status) {
//		width_between_capillariesJSpinner.setEnabled(status);
//		width_between_capillariesLabel.setEnabled(status);
//		width_intervalJSpinner.setEnabled(status);
//		width_intervalLabel.setEnabled(status);
//	}
	
	// set/ get	


}