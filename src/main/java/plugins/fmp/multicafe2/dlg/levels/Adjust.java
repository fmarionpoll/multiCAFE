package plugins.fmp.multicafe2.dlg.levels;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import icy.util.StringUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.series.AdjustMeasuresToDimensions_series;
import plugins.fmp.multicafe2.series.CropMeasuresToDimensions_series;
import plugins.fmp.multicafe2.series.ClipMeasuresWithinSameCageToSameLength_series;
import plugins.fmp.multicafe2.series.CurvesRestoreLength_series;
import plugins.fmp.multicafe2.series.Options_BuildSeries;


public class Adjust extends JPanel  implements PropertyChangeListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2580935598417087197L;
	private MultiCAFE2 			parent0;
	private JCheckBox			allSeriesCheckBox = new JCheckBox("ALL series", false);
	
	private String				adjustString  	= new String("Resize levels to Kymographs");
	private String				cropString  	= new String("Crop levels to Kymograph");
	
	private String				clipString 		= new String("Clip curves within cage to the shortest curve");
	private String				restoreString	= new String("Restore curves");
	
	private JButton 			adjustButton 	= new JButton(adjustString);
	private JButton 			restoreButton 	= new JButton(restoreString);
	private JButton 			clipButton 		= new JButton(clipString);
	private JButton				cropButton		= new JButton(cropString);
	private String				stopString		= new String("STOP ");
	
	private AdjustMeasuresToDimensions_series threadAdjust = null;
	private CurvesRestoreLength_series threadRestore = null;
	private ClipMeasuresWithinSameCageToSameLength_series threadClip = null;
	private CropMeasuresToDimensions_series threadCrop = null;
	
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);	
		this.parent0 = parent0;
		
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		
		JPanel panel0 = new JPanel(layout);
		panel0.add(adjustButton);
		panel0.add(cropButton);
		add(panel0);

		JPanel panel1 = new JPanel(layout);
		panel1.add(clipButton);
		panel1.add(restoreButton);
		add(panel1);
		
		JPanel panel2 = new JPanel(layout);
		panel2.add(allSeriesCheckBox);
		add(panel2);
		
		defineListeners();
	}
	
	private void defineListeners() 
	{
		adjustButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				if (adjustButton.getText() .equals(adjustString))
					series_adjustDimensionsStart();
				else 
					series_adjustDimensionsStop();
			}});
		
		cropButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				if (cropButton.getText() .equals(cropString))
					series_cropDimensionsStart();
				else 
					series_cropDimensionsStop();
			}});

		
		restoreButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				if (restoreButton.getText() .equals(restoreString))
					series_restoreStart();
				else 
					series_restoreStop();
			}});
		
		clipButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				if (restoreButton.getText() .equals(restoreString))
					series_clipStart();
				else 
					series_clipStop();
			}});
			
		allSeriesCheckBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Color color = Color.BLACK;
				if (allSeriesCheckBox.isSelected()) 
					color = Color.RED;
				allSeriesCheckBox.setForeground(color);
				adjustButton.setForeground(color);
				clipButton.setForeground(color);
				restoreButton.setForeground(color);
		}});
	}

	void restoreClippedPoints(Experiment exp) 
	{
		SequenceKymos seqKymos = exp.seqKymos;
		int t = seqKymos.currentFrame;
		Capillary cap = exp.capillaries.capillariesArrayList.get(t);
		cap.restoreClippedMeasures();
		
		seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsTop);
		seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsBottom);
		seqKymos.updateROIFromCapillaryMeasure(cap, cap.ptsDerivative);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) 
		 {
//			Experiment exp = parent0.expListCombo.getItemAt(parent0.expListCombo.getSelectedIndex());
//			parent0.paneExperiment.panelLoadSave.openExperiment(exp);	
			if (adjustButton.getText() .contains (stopString))
				adjustButton.setText(adjustString);
			else if (restoreButton.getText().contains(stopString))
				restoreButton.setText(restoreString);
			else if (clipButton.getText() .contains(stopString))
				clipButton.setText(clipString);
			else if (cropButton.getText() .contains(stopString))
				cropButton.setText(cropString);
		 }	 
	}
	
	private void series_adjustDimensionsStop() 
	{	
		if (threadAdjust != null && !threadAdjust.stopFlag) {
			threadAdjust.stopFlag = true;
		}
	}
	
	private void series_cropDimensionsStop() 
	{	
		if (threadCrop != null && !threadCrop.stopFlag) {
			threadCrop.stopFlag = true;
		}
	}
	
	private void series_restoreStop() 
	{
		if (threadRestore != null && !threadRestore.stopFlag) {
			threadRestore.stopFlag = true;
		}
	}
	
	private void series_clipStop() 
	{
		if (threadClip != null && !threadClip.stopFlag) {
			threadClip.stopFlag = true;
		}
	}
	
	private boolean initBuildParameters(Options_BuildSeries options) 
	{
		int index  = parent0.expListCombo.getSelectedIndex();
		Experiment exp = parent0.expListCombo.getItemAt(index);
		if (exp == null)
			return false;
		
		parent0.paneExperiment.panelLoadSave.closeViewsForCurrentExperiment(exp);
		options.expList = parent0.expListCombo; 
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		if (allSeriesCheckBox.isSelected())
			options.expList.index1 = parent0.expListCombo.getItemCount()-1;
		else
			options.expList.index1 = options.expList.index0; 
		
		options.isFrameFixed= parent0.paneExperiment.tabAnalyze.getIsFixedFrame();
		options.t_firstMs 	= parent0.paneExperiment.tabAnalyze.getStartMs();
		options.t_lastMs 	= parent0.paneExperiment.tabAnalyze.getEndMs();
		options.t_binMs		= parent0.paneExperiment.tabAnalyze.getBinMs();
				
		options.parent0Rect = parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory = parent0.paneKymos.tabDisplay.getBinSubdirectory() ;
		return true;
	}
	
	private void series_adjustDimensionsStart() 
	{
		threadAdjust = new AdjustMeasuresToDimensions_series();
		Options_BuildSeries options= threadAdjust.options;
		if (initBuildParameters (options)) 
		{
			threadAdjust.addPropertyChangeListener(this);
			threadAdjust.execute();
			adjustButton.setText(stopString + adjustString);
		}
	}
	
	private void series_cropDimensionsStart() 
	{
		threadCrop = new CropMeasuresToDimensions_series();
		Options_BuildSeries options= threadCrop.options;
		if (initBuildParameters (options)) 
		{
			threadCrop.addPropertyChangeListener(this);
			threadCrop.execute();
			cropButton.setText(stopString + cropString);
		}
	}
	
	private void series_restoreStart() 
	{
		threadRestore = new CurvesRestoreLength_series();
		Options_BuildSeries options= threadRestore.options;
		if (initBuildParameters (options)) 
		{
			threadRestore.addPropertyChangeListener(this);
			threadRestore.execute();
			restoreButton.setText(stopString + restoreString);
		}
	}
	
	private void series_clipStart() 
	{
		threadClip = new ClipMeasuresWithinSameCageToSameLength_series();
		Options_BuildSeries options= threadClip.options;
		if (initBuildParameters (options)) 
		{
			threadClip.addPropertyChangeListener(this);
			threadClip.execute();
			clipButton.setText(stopString + clipString);
		}
	}
	
}
