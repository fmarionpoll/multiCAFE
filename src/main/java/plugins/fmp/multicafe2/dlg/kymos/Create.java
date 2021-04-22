package plugins.fmp.multicafe2.dlg.kymos;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.util.StringUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.series.BuildKymographs_series;
import plugins.fmp.multicafe2.series.Options_BuildSeries;
import plugins.fmp.multicafe2.tools.EnumStatusComputation;



public class Create extends JPanel implements PropertyChangeListener 
{ 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	private String 			detectString 			= "Start";
			JButton 		startComputationButton 	= new JButton("Start");
			JSpinner		diskRadiusSpinner 		= new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
			JCheckBox 		doRegistrationCheckBox 	= new JCheckBox("registration", false);
			JCheckBox		allSeriesCheckBox 		= new JCheckBox("ALL series (current to last)", false);
	EnumStatusComputation 	sComputation 			= EnumStatusComputation.START_COMPUTATION; 
	private MultiCAFE2 		parent0					= null;
	private BuildKymographs_series threadBuildKymo 	= null;

	// -----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);	
		this.parent0 = parent0;
		
		FlowLayout layout1 = new FlowLayout(FlowLayout.LEFT);
		layout1.setVgap(0);
		
		JPanel panel1 = new JPanel(layout1);
		panel1.add(startComputationButton);
		panel1.add(allSeriesCheckBox);
		add(panel1);
		
		JPanel panel2 = new JPanel(layout1);
		panel2.add(new JLabel("area around ROIs", SwingConstants.RIGHT));
		panel2.add(diskRadiusSpinner);  
		panel2.add(doRegistrationCheckBox);
		add(panel2);
			
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		startComputationButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				if (startComputationButton.getText() .equals(detectString))
					startComputation();
				else
					stopComputation();
		}});

		allSeriesCheckBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Color color = Color.BLACK;
				if (allSeriesCheckBox.isSelected()) 
					color = Color.RED;
				allSeriesCheckBox.setForeground(color);
				startComputationButton.setForeground(color);
		}});
	}
		
	private Options_BuildSeries initBuildParameters() 
	{
		Options_BuildSeries options  = new Options_BuildSeries();
		options.expList = parent0.expListCombo; 
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		if (allSeriesCheckBox.isSelected())
			options.expList.index1 = parent0.expListCombo.getItemCount()-1;
		else
			options.expList.index1 = options.expList.index0; 
		options.isFrameFixed 	= parent0.paneExperiment.tabAnalyze.getIsFixedFrame();
		options.t_firstMs 		= parent0.paneExperiment.tabAnalyze.getStartMs();
		options.t_lastMs 		= parent0.paneExperiment.tabAnalyze.getEndMs();
		options.t_binMs			= parent0.paneExperiment.tabAnalyze.getBinMs();
				
		options.diskRadius 		= (int) diskRadiusSpinner.getValue();
		options.doRegistration 	= doRegistrationCheckBox.isSelected();
		options.doCreateBinDir 	= true;
		options.parent0Rect 	= parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory = Experiment.BIN+options.t_binMs/1000 ;
		return options;
	}
		
	private void startComputation() 
	{
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		
		threadBuildKymo = new BuildKymographs_series();	
		threadBuildKymo.options = initBuildParameters();
		
		threadBuildKymo.addPropertyChangeListener(this);
		threadBuildKymo.execute();
		startComputationButton.setText("STOP");
	}
	
	private void stopComputation() 
	{	
		if (threadBuildKymo != null && !threadBuildKymo.stopFlag) {
			threadBuildKymo.stopFlag = true;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			startComputationButton.setText(detectString);
		 }
	}
	

}
