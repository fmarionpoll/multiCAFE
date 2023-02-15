package plugins.fmp.multicafe2.dlg.capillaries;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import icy.util.StringUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.series.BuildSeriesOptions;
import plugins.fmp.multicafe2.series.DetectLevels;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;



public class Experimental extends JPanel implements PropertyChangeListener 
{
	private static final long serialVersionUID = 1L;


	
	private JCheckBox	pass1CheckBox 			= new JCheckBox ("pass1", true);
	private JComboBox<String> direction1ComboBox= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JSpinner 	threshold1Spinner 		= new JSpinner(new SpinnerNumberModel(35, 1, 255, 1));
	JComboBox<EnumImageTransformations> transform01ComboBox = new JComboBox<EnumImageTransformations> (
		new EnumImageTransformations[] {
			EnumImageTransformations.R_RGB, EnumImageTransformations.G_RGB, EnumImageTransformations.B_RGB, 
			EnumImageTransformations.R2MINUS_GB, EnumImageTransformations.G2MINUS_RB, EnumImageTransformations.B2MINUS_RG, EnumImageTransformations.RGB,
			EnumImageTransformations.GBMINUS_2R, EnumImageTransformations.RBMINUS_2G, EnumImageTransformations.RGMINUS_2B, EnumImageTransformations.RGB_DIFFS,
			EnumImageTransformations.H_HSB, EnumImageTransformations.S_HSB, EnumImageTransformations.B_HSB
			
		});
	private JButton		displayTransform1Button	= new JButton("Display");
	


	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private String 		detectString 			= "        Detect     ";
	private JButton 	detectButton 			= new JButton(detectString);

	
	private MultiCAFE2 	parent0 				= null;
	private DetectLevels threadDetectLevels 	= null;
	
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
		panel01.add(pass1CheckBox);
		panel01.add(direction1ComboBox);
		((JLabel) direction1ComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		panel01.add(threshold1Spinner);
		panel01.add(transform01ComboBox);
		panel01.add(displayTransform1Button);
		add (panel01);
		
		JPanel panel02 = new JPanel(layoutLeft);
		add (panel02);

		
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{	
		transform01ComboBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqCamData != null) 
				{
					kymosDisplayFiltered01(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
	
		detectButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				if (detectButton.getText().equals(detectString))
					startComputation();
				else 
					stopComputation();
			}});	
		
		displayTransform1Button.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{ 
					kymosDisplayFiltered01(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		
	}
	
	// -------------------------------------------------
	
	int getSpanDiffTop() 
	{
		return (int) spanTopSpinner.getValue() ;
	}
		
	void kymosDisplayFiltered01(Experiment exp) 
	{
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		EnumImageTransformations transform01 = (EnumImageTransformations) transform01ComboBox.getSelectedItem();
		
		List<Capillary> capList = exp.capillaries.capillariesList;
		for (int t=0; t < exp.seqKymos.seq.getSizeT(); t++) 
			getInfosFromDialog(capList.get(t));		
		
		int zChannelDestination = 1;
		exp.kymosBuildFiltered01(0, zChannelDestination, transform01, getSpanDiffTop());
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	
		
	void setInfosToDialog(Capillary cap) 
	{
		BuildSeriesOptions options = cap.limitsOptions;
		
		pass1CheckBox.setSelected(options.pass1);
		
		transform01ComboBox.setSelectedItem(options.transform01);
		int index = options.directionUp1 ? 0:1;
		direction1ComboBox.setSelectedIndex(index);
		threshold1Spinner.setValue(options.detectLevel1Threshold);
	
	}
	
	void getInfosFromDialog(Capillary cap) 
	{
		BuildSeriesOptions capOptions 		= cap.limitsOptions;
		capOptions.pass1 					= pass1CheckBox.isSelected();

		capOptions.transform01 				= (EnumImageTransformations) transform01ComboBox.getSelectedItem();
		capOptions.directionUp1 			= (direction1ComboBox.getSelectedIndex() == 0) ;
		capOptions.detectLevel1Threshold 	= (int) threshold1Spinner.getValue();
	}
	
	private BuildSeriesOptions initBuildParameters(Experiment exp) 
	{	
		BuildSeriesOptions options = new BuildSeriesOptions();
		// list of stack experiments
		options.expList = parent0.expListCombo; 
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		options.expList.index1 = parent0.expListCombo.getSelectedIndex();
		// list of kymographs
		parent0.paneKymos.tabDisplay.indexImagesCombo = parent0.paneKymos.tabDisplay.kymographsCombo.getSelectedIndex();

		options.kymoFirst = 0;
		options.kymoLast = parent0.paneKymos.tabDisplay.kymographsCombo.getItemCount()-1;

		// other parameters
		options.pass1 				= pass1CheckBox.isSelected();
		options.transform01 		= (EnumImageTransformations) transform01ComboBox.getSelectedItem();
		options.directionUp1 		= (direction1ComboBox.getSelectedIndex() == 0);
		options.detectLevel1Threshold= (int) threshold1Spinner.getValue();
		
		options.spanDiffTop			= getSpanDiffTop();
		options.parent0Rect 		= parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory 	= parent0.expListCombo.expListBinSubDirectory ;
		return options;
	}
	
	void startComputation() 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();	
		if (exp != null)
		{
			threadDetectLevels = new DetectLevels();
			threadDetectLevels.options = initBuildParameters(exp);
			
			threadDetectLevels.addPropertyChangeListener(this);
			threadDetectLevels.execute();
			detectButton.setText("STOP");
		}
	}

	private void stopComputation() 
	{	
		if (threadDetectLevels != null && !threadDetectLevels.stopFlag) 
			threadDetectLevels.stopFlag = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) 
		 {
			detectButton.setText(detectString);
			parent0.paneKymos.tabDisplay.selectKymographImage(parent0.paneKymos.tabDisplay.indexImagesCombo);
			parent0.paneKymos.tabDisplay.indexImagesCombo = -1;
		 }
	}

}
