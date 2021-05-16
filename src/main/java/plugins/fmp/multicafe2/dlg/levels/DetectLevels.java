package plugins.fmp.multicafe2.dlg.levels;

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
import plugins.fmp.multicafe2.series.DetectLevels_series;
import plugins.fmp.multicafe2.series.Options_BuildSeries;
import plugins.fmp.multicafe2.tools.EnumTransformOp;



public class DetectLevels extends JPanel implements PropertyChangeListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID 	= -6329863521455897561L;
	JSpinner			startSpinner			= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
	JSpinner			endSpinner				= new JSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
	JComboBox<EnumTransformOp> transformForLevelsComboBox = new JComboBox<EnumTransformOp> (new EnumTransformOp[] {
			EnumTransformOp.R_RGB, EnumTransformOp.G_RGB, EnumTransformOp.B_RGB, 
			EnumTransformOp.R2MINUS_GB, EnumTransformOp.G2MINUS_RB, EnumTransformOp.B2MINUS_RG, EnumTransformOp.RGB,
			EnumTransformOp.GBMINUS_2R, EnumTransformOp.RBMINUS_2G, EnumTransformOp.RGMINUS_2B, EnumTransformOp.RGB_DIFFS,
			EnumTransformOp.H_HSB, EnumTransformOp.S_HSB, EnumTransformOp.B_HSB	});

	private JComboBox<String> directionComboBox	= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JCheckBox	allKymosCheckBox 		= new JCheckBox ("all kymographs", true);
	private JSpinner 	thresholdSpinner 		= new JSpinner(new SpinnerNumberModel(35, 1, 255, 1));
	private JButton		displayTransform1Button	= new JButton("Display");
	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private String 		detectString 			= "        Detect     ";
	private JButton 	detectButton 			= new JButton(detectString);
	private JCheckBox	fromCheckBox 			= new JCheckBox (" from (pixel)", false);
	private JCheckBox 	allSeriesCheckBox 		= new JCheckBox("ALL (current to last)", false);
	private JCheckBox	leftCheckBox 			= new JCheckBox ("L", true);
	private JCheckBox	rightCheckBox 			= new JCheckBox ("R", true);
	private JCheckBox	maxContrastCheckBox 	= new JCheckBox ("maximize contrast", false);
	
	private MultiCAFE2 	parent0 				= null;
	private DetectLevels_series threadDetectLevels = null;
	
	// -----------------------------------------------------
		
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT); 
		
		JPanel panel0 = new JPanel(layoutLeft);
		((FlowLayout)panel0.getLayout()).setVgap(0);
		panel0.add(detectButton);
		panel0.add(allSeriesCheckBox);
		panel0.add(allKymosCheckBox);
		panel0.add(leftCheckBox);
		panel0.add(rightCheckBox);
		panel0.add(maxContrastCheckBox);
		add(panel0);
		
		JPanel panel01 = new JPanel(layoutLeft);
		panel01.add(directionComboBox);
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		panel01.add(thresholdSpinner);
		panel01.add(transformForLevelsComboBox);
		panel01.add(displayTransform1Button);
		add (panel01);
		
		JPanel panel1 = new JPanel(layoutLeft);
		panel1.add(fromCheckBox);
		panel1.add(startSpinner);
		panel1.add(new JLabel("to"));
		panel1.add(endSpinner);
		add( panel1);
		
		maxContrastCheckBox.setVisible(false);
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{	
		transformForLevelsComboBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null && exp.seqCamData != null) 
				{
					kymosDisplayFiltered1(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		
		detectButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				if (detectButton.getText() .equals(detectString))
					startComputation();
				else 
					stopComputation();
			}});	
		
		displayTransform1Button.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp =(Experiment)  parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{ 
					kymosDisplayFiltered1(exp);
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		
		allSeriesCheckBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Color color = Color.BLACK;
				if (allSeriesCheckBox.isSelected()) 
					color = Color.RED;
				allSeriesCheckBox.setForeground(color);
				detectButton.setForeground(color);
		}});
	}
	
	// -------------------------------------------------
	
	int getDetectLevelThreshold() 
	{
		return (int) thresholdSpinner.getValue();
	}

	void setDetectLevelThreshold (int threshold) 
	{
		thresholdSpinner.setValue(threshold);
	}
	
	int getSpanDiffTop() 
	{
		return (int) spanTopSpinner.getValue() ;
	}
		
	void kymosDisplayFiltered1(Experiment exp) 
	{
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		EnumTransformOp transform = (EnumTransformOp) transformForLevelsComboBox.getSelectedItem();
		List<Capillary> capList = exp.capillaries.capillariesArrayList;
		for (int t=0; t < exp.seqKymos.seq.getSizeT(); t++) 
			getInfosFromDialog(capList.get(t));		
		
		int zChannelDestination = 1;
		exp.kymosBuildFiltered(0, zChannelDestination, transform, getSpanDiffTop());
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
	
	void setInfosToDialog(Capillary cap) 
	{
		Options_BuildSeries options = cap.limitsOptions;
		transformForLevelsComboBox.setSelectedItem(options.transformForLevels);
		int index = options.directionUp ? 0:1;
		directionComboBox.setSelectedIndex(index);
		setDetectLevelThreshold(options.detectLevelThreshold);
		thresholdSpinner.setValue(options.detectLevelThreshold);
		allKymosCheckBox.setSelected(options.detectAllKymos);
		leftCheckBox.setSelected(options.detectL);
		rightCheckBox.setSelected(options.detectR);
	}
	
	void getInfosFromDialog(Capillary cap) 
	{
		Options_BuildSeries options = cap.limitsOptions;
		options.transformForLevels = (EnumTransformOp) transformForLevelsComboBox.getSelectedItem();
		options.directionUp = (directionComboBox.getSelectedIndex() == 0) ;
		options.detectLevelThreshold = getDetectLevelThreshold();
		options.detectAllKymos = allKymosCheckBox.isSelected();
		options.detectL = leftCheckBox.isSelected();
		options.detectR = rightCheckBox.isSelected();
	}
	
	private Options_BuildSeries initBuildParameters(Experiment exp) 
	{	
		Options_BuildSeries options = new Options_BuildSeries();
		// list of stack experiments
		options.expList = parent0.expListCombo; 
		options.expList.index0 = parent0.expListCombo.getSelectedIndex();
		if (allSeriesCheckBox.isSelected()) 
			options.expList.index1 = options.expList.getItemCount()-1;
		else
			options.expList.index1 = parent0.expListCombo.getSelectedIndex();
		// list of kymographs
		options.detectAllKymos = allKymosCheckBox.isSelected();
		parent0.paneKymos.tabDisplay.indexImagesCombo = parent0.paneKymos.tabDisplay.kymographsCombo.getSelectedIndex();
		if (!allKymosCheckBox.isSelected()) 
		{
			options.firstKymo = parent0.paneKymos.tabDisplay.indexImagesCombo;
			options.lastKymo = options.firstKymo;
		}
		else
		{
			options.firstKymo = 0;
			options.lastKymo = parent0.paneKymos.tabDisplay.kymographsCombo.getItemCount()-1;
		}
		// other parameters
		options.transformForLevels 	= (EnumTransformOp) transformForLevelsComboBox.getSelectedItem();
		options.directionUp 		= (directionComboBox.getSelectedIndex() == 0);
		options.detectLevelThreshold= (int) getDetectLevelThreshold();
		
		options.analyzePartOnly		= fromCheckBox.isSelected();
		options.startPixel			= (int) startSpinner.getValue(); 
		options.endPixel			= (int) endSpinner.getValue(); 
		options.spanDiffTop			= getSpanDiffTop();
		options.detectL 			= leftCheckBox.isSelected();
		options.detectR				= rightCheckBox.isSelected();
		options.parent0Rect 		= parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory 	= parent0.expListCombo.expListBinSubDirectory ;
		return options;
	}
	
	void startComputation() 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();	
		if (exp != null)
		{
			threadDetectLevels = new DetectLevels_series();
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
			
			startSpinner.setValue(threadDetectLevels.options.startPixel); 
			endSpinner.setValue(threadDetectLevels.options.endPixel); 
		 }
	}

}
