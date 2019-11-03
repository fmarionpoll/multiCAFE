package plugins.fmp.multicafe;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.roi.ROI2D;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.XYTaSeries;
import plugins.fmp.multicafeTools.DetectFlies2;
import plugins.fmp.multicafeTools.DetectFlies_Options;
import plugins.fmp.multicafeTools.OverlayThreshold;



public class MCMove_Detect2 extends JPanel implements ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiCAFE parent0;
	
	private JButton 	buildBackgroundButton 	= new JButton("Build background / Stop");
	private JButton 	startComputationButton 	= new JButton("Detect flies / Stop");
	private JSpinner 	thresholdSpinner		= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JSpinner 	jitterTextField 		= new JSpinner(new SpinnerNumberModel(5, 0, 255, 1));
	private JCheckBox 	objectLowsizeCheckBox 	= new JCheckBox("object >");
	private JSpinner 	objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 100000, 1));
	private JCheckBox 	objectUpsizeCheckBox 	= new JCheckBox("object <");
	private JSpinner 	objectUpsizeSpinner		= new JSpinner(new SpinnerNumberModel(500, 0, 100000, 1));
	public 	JCheckBox 	imageOverlayCheckBox	= new JCheckBox("overlay", true);
	private JCheckBox 	viewsCheckBox 			= new JCheckBox("view ref img", true);
	private JButton 	loadButton 	= new JButton("Load...");
	private JButton 	saveButton 	= new JButton("Save...");
	
	private OverlayThreshold ov 				= null;
	private DetectFlies2 	detectFlies2Thread 	= null;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		panel.add(new JLabel(" -->", SwingConstants.RIGHT), FlowLayout.LEFT); 
		panel.add(loadButton);
		panel.add(saveButton);
		FlowLayout layout1 = (FlowLayout) panel.getLayout();
		layout1.setVgap(0);
		panel.validate();
		add( GuiUtil.besidesPanel(buildBackgroundButton,  panel));

		JPanel dummyPanel = new JPanel();
		dummyPanel.add( GuiUtil.besidesPanel(viewsCheckBox, imageOverlayCheckBox ) );
		FlowLayout layout = (FlowLayout) dummyPanel.getLayout();
		layout.setVgap(0);
		dummyPanel.validate();
		add( GuiUtil.besidesPanel(startComputationButton,  dummyPanel));
		
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel(new JLabel("threshold ", 
				SwingConstants.RIGHT), 
				thresholdSpinner, 
				objectLowsizeCheckBox, 
				objectLowsizeSpinner));
		
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( new JLabel("jitter <= ", SwingConstants.RIGHT), 
				jitterTextField , 
				objectUpsizeCheckBox, 
				objectUpsizeSpinner) );
		
		defineActionListeners();
		thresholdSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		
		imageOverlayCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		    	if (imageOverlayCheckBox.isSelected() && exp != null) {
		    		if (ov == null)
		    			ov = new OverlayThreshold(exp.seqCamData);
						exp.seqCamData.seq.addOverlay(ov);
						updateOverlay();
					}
					else
						removeOverlay();
		    }});

		startComputationButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				startComputation();
			}});
		
		buildBackgroundButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				builBackgroundImage();
			}});
		
		saveButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				saveRef();
			}});
		
		loadButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				loadRef();
			}});
	}
	
	public void updateOverlay () {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData == null)
			return;
		if (ov == null) 
			ov = new OverlayThreshold(seqCamData);
		else {
			seqCamData.seq.removeOverlay(ov);
			ov.setSequence(seqCamData);
		}
		seqCamData.seq.addOverlay(ov);	
		ov.setThresholdSingle(seqCamData.cages.detect.threshold);
		ov.painterChanged();
	}
	
	public void removeOverlay() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		if (exp != null && exp.seqCamData != null && exp.seqCamData.seq != null)
			exp.seqCamData.seq.removeOverlay(ov);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
			exp.seqCamData.cages.detect.threshold = (int) thresholdSpinner.getValue();
			updateOverlay();
		}
	}
	
	private boolean initTrackParameters() {
		DetectFlies_Options detect = new DetectFlies_Options();
		detect.btrackWhite 		= true;
		detect.blimitLow 		= objectLowsizeCheckBox.isSelected();
		detect.blimitUp 		= objectUpsizeCheckBox.isSelected();
		detect.limitLow 		= (int) objectLowsizeSpinner.getValue();
		detect.limitUp 			= (int) objectUpsizeSpinner.getValue();
		detect.jitter 			= (int) jitterTextField.getValue();
		detect.threshold		= (int) thresholdSpinner.getValue();
		Experiment exp 			= parent0.expList.getExperiment(parent0.currentIndex);
		detect.seqCamData 		= exp.seqCamData;	
		detect.initParametersForDetection();
		
		if (detectFlies2Thread == null)
			detectFlies2Thread = new DetectFlies2();
		detectFlies2Thread.stopFlag 	= false;
		detectFlies2Thread.detect 		= detect;
		detectFlies2Thread.viewInternalImages = viewsCheckBox.isSelected();
	
		return true;
	}
	
	private void cleanPreviousDetections() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		for (Cage cage: seqCamData.cages.cageList) {
			cage.flyPositions = new XYTaSeries();
		}
		ArrayList<ROI2D> list = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (roi.getName().contains("det")) {
				seqCamData.seq.removeROI(roi);
			}
		}
	}

	void builBackgroundImage() {
		if (detectFlies2Thread == null)
			detectFlies2Thread = new DetectFlies2();
		if (detectFlies2Thread.threadRunning) {
			stopComputation();
			return;
		}
		initTrackParameters();
		detectFlies2Thread.buildBackground	= true;
		detectFlies2Thread.detectFlies		= false;
		ThreadUtil.bgRun(detectFlies2Thread);
	}
	
	void startComputation() {
		if (detectFlies2Thread == null)
			detectFlies2Thread = new DetectFlies2();		
		if (detectFlies2Thread.threadRunning) {
			stopComputation();
			return;
		}	
		initTrackParameters();
		cleanPreviousDetections();
		detectFlies2Thread.buildBackground	= false;
		detectFlies2Thread.detectFlies		= true;
		ThreadUtil.bgRun(detectFlies2Thread);
	}
	
	void stopComputation() {
		if (detectFlies2Thread != null)
			detectFlies2Thread.stopFlag = true;
	}
	
	void loadRef () {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		String path = seqCamData.getDirectory()+ File.separator+"results"+File.separator+"referenceImage.jpg";
		File inputfile = new File(path);
		BufferedImage image = ImageUtil.load(inputfile, true);
		if (image == null) {
			System.out.println("image not loaded / not found");
			return;
		}
		seqCamData.refImage=  IcyBufferedImage.createFrom(image);
		initTrackParameters();
		
		detectFlies2Thread.displayRefViewers();
		
	}
	
	void saveRef () {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
				String path = exp.seqCamData.getDirectory()+ File.separator+"results"+File.separator+"referenceImage.jpg";
		File outputfile = new File(path);
		RenderedImage image = ImageUtil.toRGBImage(exp.seqCamData.refImage);
		boolean success = ImageUtil.save(image, "jpg", outputfile);
		if (success)
			System.out.println("successfully saved background.jpg image");
	}

}
