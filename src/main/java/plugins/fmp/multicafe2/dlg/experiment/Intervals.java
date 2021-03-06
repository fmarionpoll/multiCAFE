package plugins.fmp.multicafe2.dlg.experiment;

import java.awt.Dimension;
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

import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.JComboMs;
import plugins.fmp.multicafe2.experiment.Experiment;



public class Intervals extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	JSpinner 	startFrameJSpinner	= new JSpinner(new SpinnerNumberModel(0., 0., 10000., 1.)); 
	JSpinner 	endFrameJSpinner	= new JSpinner(new SpinnerNumberModel(99999999., 1., 99999999., 1.));
	JSpinner 	binSizeJSpinner		= new JSpinner(new SpinnerNumberModel(1., 0., 1000., 1.));
	JComboMs 	binUnit 			= new JComboMs();
	JButton		applyButton 		= new JButton("Apply changes");
	JButton		refreshButton 		= new JButton("Refresh");
	private MultiCAFE2 	parent0 	= null;
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;

		int bWidth = 50;
		int bHeight = 21;
		binSizeJSpinner.setPreferredSize(new Dimension(bWidth, bHeight));
		
		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(1);
		
		JPanel panel0 = new JPanel(layout1);
		panel0.add(new JLabel("Frame ", SwingConstants.RIGHT));
		panel0.add(startFrameJSpinner);
		panel0.add(new JLabel(" to "));
		panel0.add(endFrameJSpinner);
		add(panel0);
		
		JPanel panel1 = new JPanel(layout1);
		panel1.add(new JLabel("Time between frames ", SwingConstants.RIGHT));
		panel1.add(binSizeJSpinner);
		panel1.add(binUnit);
		add(panel1);

		panel1.add(refreshButton);
		panel1.add(applyButton);
	
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		applyButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Experiment exp =(Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					exp.camBinImage_Ms = (long) (((double) binSizeJSpinner.getValue())* binUnit.getMsUnitValue());
			}});
			
		refreshButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					exp.loadFileIntervalsFromSeqCamData();
					refreshBinSize(exp);
				}
			}});
	}
	
	public void displayCamDataIntervals (Experiment exp) 
	{
		refreshBinSize(exp);
		
		double divisor = exp.camBinImage_Ms;
		double dFirst = exp.offsetFirstCol_Ms/divisor;
		startFrameJSpinner.setValue(dFirst);
		if(exp.offsetLastCol_Ms == 0)
			exp.offsetLastCol_Ms = (long) (exp.getSeqCamSizeT() * divisor);
		double dLast = exp.offsetLastCol_Ms/divisor;
		endFrameJSpinner.setValue(dLast);
		if (exp.camBinImage_Ms == 0)
			exp.loadFileIntervalsFromSeqCamData();
	}
	
	private void refreshBinSize(Experiment exp) 
	{
		binUnit.setSelectedIndex(1);
		binSizeJSpinner.setValue(exp.camBinImage_Ms/(double)binUnit.getMsUnitValue());
	}
}
