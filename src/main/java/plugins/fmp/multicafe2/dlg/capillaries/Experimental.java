package plugins.fmp.multicafe2.dlg.capillaries;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.util.StringUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.series.BuildFilteredImages;
import plugins.fmp.multicafe2.series.BuildSeriesOptions;
import plugins.fmp.multicafe2.tools.EnumStatusComputation;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;



public class Experimental extends JPanel implements PropertyChangeListener 
{
	private static final long serialVersionUID = 1L;

	private JComboBox<String> directionComboBox = new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(35, 1, 255, 1));
	JComboBox<EnumImageTransformations> transformComboBox = new JComboBox<EnumImageTransformations> (
		new EnumImageTransformations[] {
			EnumImageTransformations.R_RGB, EnumImageTransformations.G_RGB, EnumImageTransformations.B_RGB, 
			EnumImageTransformations.H_HSB, EnumImageTransformations.S_HSB, EnumImageTransformations.B_HSB
		});
	
	private String 		calculateString 			= "Calculate";
	private JButton		displayTransformButton	= new JButton(calculateString);	
	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private String 		detectString 			= "        run Otsu     ";
	private JButton 	detectButton 			= new JButton(detectString);
	
	private MultiCAFE2 	parent0 				= null;
	EnumStatusComputation sComputation 			= EnumStatusComputation.START_COMPUTATION; 
	private BuildFilteredImages threadBuildFiltered = null;
	
	// -----------------------------------------------------
		
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT); 
		
		JPanel panel0 = new JPanel(layoutLeft);
		((FlowLayout)panel0.getLayout()).setVgap(0);
		panel0.add(detectButton);

		add(panel0);
		
		JPanel panel01 = new JPanel(layoutLeft);
		panel01.add(directionComboBox);
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		panel01.add(thresholdSpinner);
		panel01.add(transformComboBox);
		panel01.add(displayTransformButton);
		add (panel01);
		
		JPanel panel02 = new JPanel(layoutLeft);
		add (panel02);

		detectButton.setEnabled(false);
		directionComboBox.setEnabled(false);
		thresholdSpinner.setEnabled(false);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{	
		transformComboBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				displayFilteredImage();
			}});
		
		displayTransformButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				displayFilteredImage();
			}});
		
	}
	
	// -------------------------------------------------
	
	int getSpanDiffTop() 
	{
		return (int) spanTopSpinner.getValue() ;
	}
		
	void displayFilteredImage() 
	{
		Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
		if (exp == null || exp.seqCamData == null)
			return;
		
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData == null)
			return;

		if (displayTransformButton.getText() .equals(calculateString))
		{
			initBuildParameters();
			startComputation();
		} else {
			stopComputation();
		}
	}
	
	private BuildSeriesOptions initBuildParameters() 
	{	
		BuildSeriesOptions options = new BuildSeriesOptions();
		// list of stack experiments
		options.expList = parent0.expListCombo; 
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		options.expList.index1 = parent0.expListCombo.getSelectedIndex();

		// other parameters
		options.transform01 		= (EnumImageTransformations) transformComboBox.getSelectedItem();
		
		options.spanDiffTop			= 0;
		options.parent0Rect 		= parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory 	= parent0.expListCombo.expListBinSubDirectory ;
		return options;
	}
	
	void startComputation() 
	{
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp != null)
			parent0.paneCapillaries.tabFile.saveCapillaries_file(exp);
		
		threadBuildFiltered = new BuildFilteredImages();	
		threadBuildFiltered.options = initBuildParameters();
		
		threadBuildFiltered.addPropertyChangeListener(this);
		threadBuildFiltered.execute();
		displayTransformButton.setText("STOP");
	}

	private void stopComputation() 
	{	
		if (threadBuildFiltered != null && !threadBuildFiltered.stopFlag) 
			threadBuildFiltered.stopFlag = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) 
		 {
			 stopComputation();
			displayTransformButton.setText(calculateString);
			int zChannelDestination = 1;
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			exp.seqCamData.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
		 }
	}

}
