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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.util.StringUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceKymos;
import plugins.fmp.multicafe2.series.DetectGulps_series;
import plugins.fmp.multicafe2.series.Options_BuildSeries;
import plugins.fmp.multicafe2.tools.ImageTransformTools.TransformOp;



public class DetectGulps extends JPanel  implements PropertyChangeListener 
{
	/**
	 * 
	 */
	private static final long 	serialVersionUID 		= -5590697762090397890L;
	
	JCheckBox				allKymosCheckBox 			= new JCheckBox ("all kymographs", true);
	JComboBox<TransformOp> 	transformForGulpsComboBox 	= new JComboBox<TransformOp> (new TransformOp[] {TransformOp.XDIFFN /*, TransformOp.YDIFFN, TransformOp.XYDIFFN	*/});
	JSpinner				startSpinner				= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner				endSpinner					= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	JCheckBox				buildDerivativeCheckBox 	= new JCheckBox ("derivative", true);
	JCheckBox				detectGulpsCheckBox 		= new JCheckBox ("gulps", true);
	
	private JCheckBox		partCheckBox 				= new JCheckBox ("from (pixel)", false);
	private JButton			displayTransform2Button		= new JButton("Display");
	private JSpinner		spanTransf2Spinner			= new JSpinner(new SpinnerNumberModel(3, 0, 500, 1));
	private JSpinner 		detectGulpsThresholdSpinner	= new JSpinner(new SpinnerNumberModel(90, 0, 500, 1));
	private String 			detectString 				= "        Detect     ";
	private JButton 		detectButton 				= new JButton(detectString);
	private JCheckBox 		allCheckBox 				= new JCheckBox("ALL (current to last)", false);
	private DetectGulps_series 	threadDetectGulps 					= null;
	private MultiCAFE2 		parent0;
	
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT); 
		layoutLeft.setVgap(0);
		
		JPanel panel0 = new JPanel(layoutLeft);
		panel0.add( detectButton);
		panel0.add( allCheckBox);
		panel0.add(allKymosCheckBox);
		panel0.add(buildDerivativeCheckBox);
		panel0.add(detectGulpsCheckBox);
		add(panel0 );
		
		JPanel panel01 = new JPanel(layoutLeft);
		panel01.add(new JLabel("threshold", SwingConstants.RIGHT));
		panel01.add(detectGulpsThresholdSpinner);
		panel01.add(transformForGulpsComboBox);
		panel01.add(displayTransform2Button);
		add (panel01);
		
		JPanel panel1 = new JPanel(layoutLeft);
		panel1.add(partCheckBox);
		panel1.add(startSpinner);
		panel1.add(new JLabel("to"));
		panel1.add(endSpinner);
		add( panel1);

		transformForGulpsComboBox.setSelectedItem(TransformOp.XDIFFN);
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		transformForGulpsComboBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				kymosDisplayFiltered2();
				startComputation(false);
			}});
		
		detectButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				if (detectButton.getText() .equals(detectString))
					startComputation(true);
				else 
					stopComputation();
			}});
		
		displayTransform2Button.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				kymosDisplayFiltered2();
			}});
		
		allCheckBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Color color = Color.BLACK;
				if (allCheckBox.isSelected()) 
					color = Color.RED;
				allCheckBox.setForeground(color);
				detectButton.setForeground(color);
		}});
		
	}
		
	void kymosDisplayFiltered2() 
	{
		Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
		if (exp == null) 
			return;
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;	
		TransformOp transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
		int zChannelDestination = 2;
		exp.kymosBuildFiltered(0, zChannelDestination, transform, (int) spanTransf2Spinner.getValue());
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	private Options_BuildSeries initBuildParameters(Experiment exp ) 
	{	
		Options_BuildSeries options = threadDetectGulps.options;
		options.expList = parent0.expListCombo; 
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		
		if (allCheckBox.isSelected()) 
			options.expList.index1 = options.expList.getItemCount()-1;
		else
			options.expList.index1 = parent0.expListCombo.getSelectedIndex();

		options.detectAllKymos = allKymosCheckBox.isSelected();
		parent0.paneKymos.tabDisplay.indexImagesCombo = parent0.paneKymos.tabDisplay.imagesComboBox.getSelectedIndex();
		if (!allKymosCheckBox.isSelected()) {
			options.firstKymo = parent0.paneKymos.tabDisplay.imagesComboBox.getSelectedIndex();
			options.lastKymo = options.firstKymo;
		}
		else
		{
			options.firstKymo = 0;
			options.lastKymo = parent0.paneKymos.tabDisplay.imagesComboBox.getItemCount()-1;
		}
		options.detectGulpsThreshold 	= (int) detectGulpsThresholdSpinner.getValue();
		options.transformForGulps 		= (TransformOp) transformForGulpsComboBox.getSelectedItem();
		options.detectAllGulps 	= allKymosCheckBox.isSelected();
		options.spanDiff		= (int) spanTransf2Spinner.getValue();
		options.buildGulps		= detectGulpsCheckBox.isSelected();
		options.buildDerivative	= buildDerivativeCheckBox.isSelected();
		options.analyzePartOnly	= partCheckBox.isSelected();
		options.startPixel		= (int) startSpinner.getValue();
		options.endPixel		= (int) endSpinner.getValue();
		options.parent0Rect 	= parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory 		= (String) parent0.paneKymos.tabDisplay.getBinSubdirectory() ;
		return options;
	}
	
	void startComputation(boolean detectGulps) 
	{
		kymosDisplayFiltered2();	
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();	
		exp.saveExperimentMeasures(exp.getKymosBinFullDirectory());
		
		threadDetectGulps = new DetectGulps_series();
		threadDetectGulps.options = initBuildParameters(exp);
		if (!detectGulps)
			threadDetectGulps.options.buildGulps 	= false;
		
		threadDetectGulps.addPropertyChangeListener(this);
		threadDetectGulps.execute();
		detectButton.setText("STOP");
	}

	void setInfos(Capillary cap) 
	{
		Options_BuildSeries options = cap.getGulpsOptions();
		detectGulpsThresholdSpinner.setValue(options.detectGulpsThreshold);
		transformForGulpsComboBox.setSelectedItem(options.transformForGulps);
		allKymosCheckBox.setSelected(options.detectAllGulps);
	}

	private void stopComputation() 
	{	
		if (threadDetectGulps != null && !threadDetectGulps.stopFlag) {
			threadDetectGulps.stopFlag = true;
		}
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
