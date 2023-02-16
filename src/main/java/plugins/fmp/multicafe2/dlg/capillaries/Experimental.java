package plugins.fmp.multicafe2.dlg.capillaries;


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

import icy.image.IcyBufferedImage;
import icy.util.StringUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.series.BuildSeriesOptions;
import plugins.fmp.multicafe2.series.DetectLevels;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;



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
	private JButton		displayTransformButton	= new JButton("Display");	
	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private String 		detectString 			= "        run Ozu     ";
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
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqCamData != null) 
				{
					displayFilteredImage(exp);
				}
			}});
	
		detectButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
//				if (detectButton.getText().equals(detectString))
//					startComputation();
//				else 
//					stopComputation();
			}});	
		
		displayTransformButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{ 
					displayFilteredImage(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		
	}
	
	// -------------------------------------------------
	
	int getSpanDiffTop() 
	{
		return (int) spanTopSpinner.getValue() ;
	}
		
	void displayFilteredImage(Experiment exp) 
	{
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData == null)
			return;
		EnumImageTransformations transform01 = (EnumImageTransformations) transformComboBox.getSelectedItem();
			
		int zChannelDestination = 1;
		buildFiltered01(seqCamData, 0, zChannelDestination, transform01, getSpanDiffTop());
		seqCamData.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	void buildFiltered01(SequenceCamData seqCamData, int zChannelSource, int zChannelDestination, EnumImageTransformations transformop1, int spanDiff) 
	{
		int nimages = seqCamData.seq.getSizeT();
		seqCamData.seq.beginUpdate();

		ImageTransformInterface transform = transformop1.getFunction();
		if (transform == null)
			return;
		
		for (int t= 0; t < nimages; t++) 
		{
			IcyBufferedImage img = seqCamData.getSeqImage(t, zChannelSource);
			IcyBufferedImage img2 = transform.transformImage (img, null);
			if (seqCamData.seq.getSizeZ(0) < (zChannelDestination+1)) 
				seqCamData.seq.addImage(t, img2);
			else
				seqCamData.seq.setImage(t, zChannelDestination, img2);
		}
		
		seqCamData.seq.dataChanged();
		seqCamData.seq.endUpdate();
	}
		
	void setInfosToDialog(Capillary cap) 
	{
		BuildSeriesOptions options = cap.limitsOptions;
		
		transformComboBox.setSelectedItem(options.transform01);
		int index = options.directionUp1 ? 0:1;
		directionComboBox.setSelectedIndex(index);
		thresholdSpinner.setValue(options.detectLevel1Threshold);
	
	}
	
	void getInfosFromDialog(Capillary cap) 
	{
		BuildSeriesOptions capOptions 		= cap.limitsOptions;

		capOptions.transform01 				= (EnumImageTransformations) transformComboBox.getSelectedItem();
		capOptions.directionUp1 			= (directionComboBox.getSelectedIndex() == 0) ;
		capOptions.detectLevel1Threshold 	= (int) thresholdSpinner.getValue();
	}
	
	private BuildSeriesOptions initBuildParameters(Experiment exp) 
	{	
		BuildSeriesOptions options = new BuildSeriesOptions();
		// list of stack experiments
		options.expList = parent0.expListCombo; 
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		options.expList.index1 = parent0.expListCombo.getSelectedIndex();


		// other parameters
		options.transform01 		= (EnumImageTransformations) transformComboBox.getSelectedItem();
		options.directionUp1 		= (directionComboBox.getSelectedIndex() == 0);
		options.detectLevel1Threshold= (int) thresholdSpinner.getValue();
		
		options.spanDiffTop			= getSpanDiffTop();
		options.parent0Rect 		= parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory 	= parent0.expListCombo.expListBinSubDirectory ;
		return options;
	}
	
	void startComputation() 
	{
//		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();	
//		if (exp != null)
//		{
//			threadDetectLevels = new DetectLevels();
//			threadDetectLevels.options = initBuildParameters(exp);
//			
//			threadDetectLevels.addPropertyChangeListener(this);
//			threadDetectLevels.execute();
//			detectButton.setText("STOP");
//		}
	}

	private void stopComputation() 
	{	
//		if (threadDetectLevels != null && !threadDetectLevels.stopFlag) 
//			threadDetectLevels.stopFlag = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
//		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) 
//		 {
//			detectButton.setText(detectString);
//			parent0.paneKymos.tabDisplay.selectKymographImage(parent0.paneKymos.tabDisplay.indexImagesCombo);
//			parent0.paneKymos.tabDisplay.indexImagesCombo = -1;
//		 }
	}

}
