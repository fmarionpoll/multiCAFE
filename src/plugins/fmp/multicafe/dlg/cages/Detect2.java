package plugins.fmp.multicafe.dlg.cages;

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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import icy.gui.dialog.MessageDialog;
import icy.gui.viewer.Viewer;
import icy.util.StringUtil;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Cage;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.series.Options_BuildSeries;
import plugins.fmp.multicafe.series.DetectFlies2_series;





public class Detect2 extends JPanel implements ChangeListener, PropertyChangeListener, PopupMenuListener {
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiCAFE 	parent0					=null;
	
	private String 		detectString 			= "Detect..";
	private JButton 	startComputationButton 	= new JButton(detectString);
	private JSpinner 	thresholdDiffSpinner	= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JSpinner 	thresholdBckgSpinner	= new JSpinner(new SpinnerNumberModel(40, 0, 255, 10));
	
	private JSpinner 	jitterTextField 		= new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
	private JSpinner 	objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 9999, 1));
	private JSpinner 	objectUpsizeSpinner		= new JSpinner(new SpinnerNumberModel(500, 0, 9999, 1));
	private JCheckBox 	objectLowsizeCheckBox 	= new JCheckBox("object > ");
	private JCheckBox 	objectUpsizeCheckBox 	= new JCheckBox("object < ");
	private JCheckBox 	viewsCheckBox 			= new JCheckBox("view ref img", true);
	private JButton 	loadButton 				= new JButton("Load...");
	private JButton 	saveButton 				= new JButton("Save...");
	private JCheckBox 	allCheckBox 			= new JCheckBox("ALL (current to last)", false);
	private JCheckBox 	backgroundCheckBox 		= new JCheckBox("background", false);
	private JCheckBox 	detectCheckBox 			= new JCheckBox("flies", true);
	private JSpinner 	limitRatioSpinner		= new JSpinner(new SpinnerNumberModel(4, 0, 1000, 1));
	private JComboBox<String> allCagesComboBox = new JComboBox<String> (new String[] {"all cages"});
	
	private DetectFlies2_series detectFlies2Thread 	= null;
	private int 		currentExp 				= -1;
	
	// ----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(startComputationButton);
		panel1.add(allCagesComboBox);
		panel1.add(allCheckBox);
		panel1.add(backgroundCheckBox);
		panel1.add(detectCheckBox);
		add(panel1);
		
		allCagesComboBox.addPopupMenuListener(this);
		
		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(loadButton);
		panel2.add(saveButton);
		panel2.add(new JLabel("threshold for background "));
		panel2.add(thresholdBckgSpinner);
		panel2.add(viewsCheckBox);
		panel2.validate();
		add(panel2);
		
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		JPanel panel3 = new JPanel(flowLayout);
		panel3.add(objectLowsizeCheckBox);
		panel3.add(objectLowsizeSpinner);
		panel3.add(objectUpsizeCheckBox);
		panel3.add(objectUpsizeSpinner);
		panel3.add(new JLabel("threshold ", SwingConstants.RIGHT));
		panel3.add(thresholdDiffSpinner);
		add( panel3);
		
		JPanel panel4 = new JPanel(flowLayout);
		panel4.add(new JLabel("length/width<", SwingConstants.RIGHT));
		panel4.add(limitRatioSpinner);
		panel4.add(new JLabel("         jitter <= ", SwingConstants.RIGHT));
		panel4.add(jitterTextField);
		add(panel4);
		
		defineActionListeners();
		thresholdDiffSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		startComputationButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				if (startComputationButton.getText() .equals(detectString)) 
					startComputation();
				else
					stopComputation();
			}});
		
		saveButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null)
					exp.saveReferenceImage();
			}});
		
		loadButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null) { 
					boolean flag = exp.loadReferenceImage(); 
					if (flag) {
						Viewer v = new Viewer(exp.seqBackgroundImage, true);
						Rectangle rectv = exp.seqCamData.seq.getFirstViewer().getBoundsInternal();
						v.setBounds(rectv);
					} else {
						 MessageDialog.showDialog("Reference file not found on disk",
	                                MessageDialog.ERROR_MESSAGE);
					}
				}
			}});
		
		allCheckBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) {
				Color color = Color.BLACK;
				if (allCheckBox.isSelected()) 
					color = Color.RED;
				allCheckBox.setForeground(color);
				startComputationButton.setForeground(color);
		}});
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdDiffSpinner) {
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (exp != null)
				exp.cages.detect_threshold = (int) thresholdDiffSpinner.getValue();
		}
	}
	
	private boolean initTrackParameters() {
		if (detectFlies2Thread == null)
			return false;
		detectFlies2Thread.options = new Options_BuildSeries();
		Options_BuildSeries options = detectFlies2Thread.options;
		
		options.btrackWhite 		= true;
		options.blimitLow 		= objectLowsizeCheckBox.isSelected();
		options.blimitUp 		= objectUpsizeCheckBox.isSelected();
		options.limitLow 		= (int) objectLowsizeSpinner.getValue();
		options.limitUp 		= (int) objectUpsizeSpinner.getValue();
		options.limitRatio		= (int) limitRatioSpinner.getValue();
		options.jitter 			= (int) jitterTextField.getValue();
		options.thresholdDiff	= (int) thresholdDiffSpinner.getValue();
		options.thresholdBckgnd	= (int) thresholdBckgSpinner.getValue();
		options.parent0Rect 	= parent0.mainFrame.getBoundsInternal();
		options.resultsSubPath 	= (String) parent0.paneKymos.tabDisplay.availableResultsCombo.getSelectedItem() ;
		
		options.forceBuildBackground = backgroundCheckBox.isSelected();
		options.detectFlies		= detectCheckBox.isSelected();
		options.isFrameFixed 	= parent0.paneSequence.tabAnalyze.getIsFixedFrame();
		options.t_firstMs 		= parent0.paneSequence.tabAnalyze.getStartMs();
		options.t_lastMs 			= parent0.paneSequence.tabAnalyze.getEndMs();
		options.t_binMs			= parent0.paneSequence.tabAnalyze.getBinMs();

		options.expList = new ExperimentList(); 
		parent0.paneSequence.transferExperimentNamesToExpList(options.expList, true);		
		options.expList.index0 = parent0.expList.currentExperimentIndex;
		if (allCheckBox.isSelected())
			options.expList.index1 = options.expList.getExperimentListSize()-1;
		else 
			options.expList.index1 = options.expList.index0;
		options.detectCage 		= allCagesComboBox.getSelectedIndex() - 1;
		
		detectFlies2Thread.stopFlag = false;
		detectFlies2Thread.viewInternalImages = viewsCheckBox.isSelected();
		return true;
	}
	
	void startComputation() {
		currentExp =parent0.paneSequence.expListComboBox.getSelectedIndex();
		Experiment exp = parent0.expList.getExperimentFromList(currentExp);
		if (exp == null)
			return;
		parent0.expList.currentExperimentIndex = currentExp;
		parent0.paneSequence.tabClose.closeExp(exp);
		
		detectFlies2Thread = new DetectFlies2_series();		
		initTrackParameters();
		detectFlies2Thread.addPropertyChangeListener(this);
		detectFlies2Thread.execute();
		startComputationButton.setText("STOP");
	}
	
	private void stopComputation() {	
		if (detectFlies2Thread != null && !detectFlies2Thread.stopFlag) {
			detectFlies2Thread.stopFlag = true;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		 if (StringUtil.equals("thread_ended", evt.getPropertyName())) {
			Experiment exp = parent0.expList.getExperimentFromList(currentExp);
			if (exp != null)
				parent0.paneSequence.openExperiment(exp);
			startComputationButton.setText(detectString);
		 }
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		int nitems = 1;
		currentExp = parent0.paneSequence.expListComboBox.getSelectedIndex();
		Experiment exp = parent0.expList.getExperimentFromList(currentExp);
		if (exp != null )	
			nitems =  exp.cages.cageList.size() +1;
		if (allCagesComboBox.getItemCount() != nitems) {
			allCagesComboBox.removeAllItems();
			allCagesComboBox.addItem("all cages");
			for (Cage cage: exp.cages.cageList) {
				allCagesComboBox.addItem(cage.getCageNumber());
			}
		}		
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		// TODO Auto-generated method stub
		
	}
	

}
