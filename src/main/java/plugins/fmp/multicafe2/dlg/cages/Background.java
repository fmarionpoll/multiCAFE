package plugins.fmp.multicafe2.dlg.cages;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import icy.gui.dialog.MessageDialog;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImageUtil;
import icy.util.StringUtil;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.series.BuildSeriesOptions;
import plugins.fmp.multicafe2.series.BuildBackground;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.Overlay.OverlayThreshold;

public class Background extends JPanel implements ChangeListener, PropertyChangeListener
{
	private static final long serialVersionUID = 1L;

	private MultiCAFE2 	parent0					= null;
	
	private String 		detectString 			= "Build background...";
	private JButton 	startComputationButton 	= new JButton(detectString);

	private JSpinner 	thresholdBckgSpinner	= new JSpinner(new SpinnerNumberModel(40, 0, 255, 1));


	private JCheckBox 	viewsCheckBox 			= new JCheckBox("view ref img", true);
	private JButton 	loadButton 				= new JButton("Load...");
	private JButton 	saveButton 				= new JButton("Save...");
	private JCheckBox 	allCheckBox 			= new JCheckBox("ALL (current to last)", false);


	private BuildBackground flyDetect3 				= null;
	private OverlayThreshold ov 				= null;
	
	// ----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(startComputationButton);
		panel1.add(allCheckBox);
		add(panel1);
		
		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(new JLabel("threshold for background "));
		panel2.add(thresholdBckgSpinner);
		panel2.add(viewsCheckBox);
		panel2.validate();
		add(panel2);
		
		JPanel panel3 = new JPanel(flowLayout);
		panel3.add(loadButton);
		panel3.add(saveButton);
		add( panel3);
		
		defineActionListeners();

		thresholdBckgSpinner.addChangeListener(this);
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
		
		saveButton.addActionListener(new ActionListener () 
		{
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
					exp.saveReferenceImage(exp.seqCamData.refImage);
			}});
		
		loadButton.addActionListener(new ActionListener () 
		{
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null) 
				{ 
					boolean flag = exp.loadReferenceImage(); 
					if (flag) 
					{
						Viewer v = new Viewer(exp.seqReference, true);
						Rectangle rectv = exp.seqCamData.seq.getFirstViewer().getBoundsInternal();
						v.setBounds(rectv);
					} 
					else 
					{
						 MessageDialog.showDialog("Reference file not found on disk",
	                                MessageDialog.ERROR_MESSAGE);
					}
				}
			}});
		
		allCheckBox.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{
				Color color = Color.BLACK;
				if (allCheckBox.isSelected()) 
					color = Color.RED;
				allCheckBox.setForeground(color);
				startComputationButton.setForeground(color);
		}});
	}

	@Override
	public void stateChanged(ChangeEvent e) 
	{
		if (e.getSource() == thresholdBckgSpinner) 
		{
			Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
			if (exp != null) 
			{
				int threshold = (int) thresholdBckgSpinner.getValue();
				updateOverlay(exp, threshold);
			}
		}
	}
	
	public void updateOverlay (Experiment exp, int threshold) 
	{
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData == null)
			return;
		if (ov == null) {
			ov = new OverlayThreshold(seqCamData);
			exp.seqCamData.refImage = IcyBufferedImageUtil.getCopy(exp.seqCamData.getSeqImage(0, 0));
		}
		else 
		{
			seqCamData.seq.removeOverlay(ov);
			ov.setSequence(seqCamData);
		}
		ov.setReferenceImage(exp.seqCamData.refImage);
		seqCamData.seq.addOverlay(ov);	
		boolean ifGreater = true; 
		EnumImageTransformations transformOp = EnumImageTransformations.SUBTRACT; //SUBTRACT_REF;
		ov.setThresholdSingle(threshold, transformOp, ifGreater);
		ov.painterChanged();	
	}
	
	private BuildSeriesOptions initTrackParameters() 
	{
		BuildSeriesOptions options = flyDetect3.options;
		options.expList 		= parent0.expListCombo;	
		options.expList.index0 	= parent0.expListCombo.getSelectedIndex();
		if (allCheckBox.isSelected())
			options.expList.index1 = options.expList.getItemCount()-1;
		else
			options.expList.index1 = parent0.expListCombo.getSelectedIndex();
		parent0.paneKymos.tabDisplay.indexImagesCombo = parent0.paneKymos.tabDisplay.kymographsCombo.getSelectedIndex();
		
		options.btrackWhite 	= true;
		options.thresholdBckgnd	= (int) thresholdBckgSpinner.getValue();		
		options.forceBuildBackground = true;
		options.detectFlies		= false;
		
		options.parent0Rect 	= parent0.mainFrame.getBoundsInternal();
		options.binSubDirectory = parent0.paneKymos.tabDisplay.getBinSubdirectory() ;
		
		options.isFrameFixed 	= parent0.paneExcel.tabCommonOptions.getIsFixedFrame();
		options.t_firstMs 		= parent0.paneExcel.tabCommonOptions.getStartMs();
		options.t_lastMs 		= parent0.paneExcel.tabCommonOptions.getEndMs();
		options.t_binMs			= parent0.paneExcel.tabCommonOptions.getBinMs();

		return options;
	}
	
	void startComputation() 
	{
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		parent0.paneExperiment.panelLoadSave.closeViewsForCurrentExperiment(exp);
		
		flyDetect3 = new BuildBackground();		
		flyDetect3.options = initTrackParameters();
		flyDetect3.stopFlag = false;

		flyDetect3.addPropertyChangeListener(this);
		flyDetect3.execute();
		startComputationButton.setText("STOP");
	}
	
	private void stopComputation() 
	{	
		if (flyDetect3 != null && !flyDetect3.stopFlag) 
			flyDetect3.stopFlag = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) 
		 {
			startComputationButton.setText(detectString);
			parent0.paneKymos.tabDisplay.selectKymographImage(parent0.paneKymos.tabDisplay.indexImagesCombo);
			parent0.paneKymos.tabDisplay.indexImagesCombo = -1;
		 }
	}

}
