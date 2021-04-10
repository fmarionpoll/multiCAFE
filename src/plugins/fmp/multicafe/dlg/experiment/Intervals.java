package plugins.fmp.multicafe.dlg.experiment;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.JComboMs;



public class Intervals extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	JSpinner 	startFrameJSpinner	= new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1)); 
	JSpinner 	endFrameJSpinner	= new JSpinner(new SpinnerNumberModel(99999999, 1, 99999999, 1));
	JSpinner 	binSize				= new JSpinner(new SpinnerNumberModel(1., 0., 1000., 1.));
	JComboMs 	binUnit 			= new JComboMs();
	JButton		applyButton 		= new JButton("Apply changes");
	JButton		refreshButton 		= new JButton("Refresh values");
	private MultiCAFE 	parent0 	= null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);
		
		JPanel panel0 = new JPanel(layout1);
		panel0.add(new JLabel("Frame ", SwingConstants.RIGHT));
		panel0.add(startFrameJSpinner);
		panel0.add(new JLabel(" to "));
		panel0.add(endFrameJSpinner);
		add(panel0);
		
		JPanel panel1 = new JPanel(layout1);
		panel1.add(new JLabel("Time between frames ", SwingConstants.RIGHT));
		panel1.add(binSize);
		panel1.add(binUnit);
		add(panel1);

		JPanel panel2 = new JPanel(layout1);
		panel2.add(refreshButton);
		panel2.add(applyButton);
		add(panel2);
	
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		applyButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Experiment exp =(Experiment)  parent0.expList.getSelectedItem();
				if (exp == null)
					return;
				exp.camBinImage_Ms = (long) (((double) binSize.getValue())* binUnit.getMsUnitValue());
			}});
			
		refreshButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Experiment exp =(Experiment)  parent0.expList.getSelectedItem();
				if (exp == null)
					return;
				exp.loadFileIntervalsFromSeqCamData();
				refreshBinSize(exp);
			}});
	}
	
	public void displayCamDataIntervals (Experiment exp) 
	{
		startFrameJSpinner.setValue(0);
		endFrameJSpinner.setValue(exp.getSeqCamSizeT());
		if (exp.camBinImage_Ms == 0)
			exp.loadFileIntervalsFromSeqCamData();
		refreshBinSize(exp);
	}
	
	private void refreshBinSize(Experiment exp) 
	{
		binUnit.setSelectedIndex(1);
		binSize.setValue(exp.camBinImage_Ms/(double)binUnit.getMsUnitValue());
	}
}
