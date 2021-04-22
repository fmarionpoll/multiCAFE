package plugins.fmp.multicafe2.dlg.experiment;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import plugins.fmp.multicafe2.dlg.JComponents.JComboMs;

public class Analyze  extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	JRadioButton  	isFixedFrame		= new JRadioButton("from ", false);
	JRadioButton  	isFloatingFrame		= new JRadioButton("all", true);

	JSpinner 	startJSpinner			= new JSpinner(new SpinnerNumberModel(0., 0., 10000., 1.)); 
	JSpinner 	endJSpinner				= new JSpinner(new SpinnerNumberModel(99999999., 1., 99999999., 1.));
	JComboMs 	intervalsUnit 			= new JComboMs();
	JSpinner 	binSize					= new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
	JComboMs 	binUnit 				= new JComboMs();
	
	
	void init(GridLayout capLayout) 
	{
		setLayout(capLayout);

		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);

		JPanel panel1 = new JPanel(layout1);
		panel1.add(new JLabel("Analyze "));
		panel1.add(isFloatingFrame);
		panel1.add(isFixedFrame);

		panel1.add(startJSpinner);
		panel1.add(new JLabel(" to "));
		panel1.add(endJSpinner);
		panel1.add(intervalsUnit);
		add(panel1); 
		intervalsUnit.setSelectedIndex(2);
		
		JPanel panel2 = new JPanel(layout1);
		panel2.add(new JLabel("bin size "));
		panel2.add(binSize);
		panel2.add(binUnit);
		add(panel2); 
		binUnit.setSelectedIndex(2);
		
		enableIntervalButtons(false);
		ButtonGroup group = new ButtonGroup();
		group.add(isFloatingFrame);
		group.add(isFixedFrame);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		isFixedFrame.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				enableIntervalButtons(true);
			}});
	
		isFloatingFrame.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				enableIntervalButtons(false);
			}});
	}
	
	private void enableIntervalButtons(boolean isSelected) 
	{
		startJSpinner.setEnabled(isSelected);
        endJSpinner.setEnabled(isSelected);
        intervalsUnit.setEnabled(isSelected);
	}

	public boolean getIsFixedFrame() 
	{
		return isFixedFrame.isSelected();
	}
	
	public long	getStartMs() 
	{
		return (long) ((double)startJSpinner.getValue() * binUnit.getMsUnitValue());
	}
	
	public long	getEndMs() 
	{
		return (long) ((double)endJSpinner.getValue() * binUnit.getMsUnitValue());
	}
	
	public long getBinMs() 
	{
		return (long)((int) binSize.getValue() * binUnit.getMsUnitValue());
	}
}
	

