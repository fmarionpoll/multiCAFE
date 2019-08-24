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
import icy.image.IcyBufferedImageUtil;
import icy.image.ImageUtil;
import icy.roi.ROI2D;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeTools.DetectFlies;
import plugins.fmp.multicafeTools.DetectFlies_Options;
import plugins.fmp.multicafeTools.OverlayThreshold;



public class MCMoveTab_Detect extends JPanel implements ChangeListener {
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
	public 	JCheckBox 	thresholdedImageCheckBox= new JCheckBox("overlay", true);
	private JCheckBox 	viewsCheckBox 			= new JCheckBox("view ref img", true);
	
	private JButton 	loadButton 	= new JButton("Load...");
	private JButton 	saveButton 	= new JButton("Save...");
	
	private OverlayThreshold 		ov = null;
	private DetectFlies 	trackAllFliesThread = null;
	
	
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
		dummyPanel.add( GuiUtil.besidesPanel(viewsCheckBox, thresholdedImageCheckBox ) );
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
		
		thresholdedImageCheckBox.addItemListener(new ItemListener() {
		      public void itemStateChanged(ItemEvent e) {
		    	  SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
		    	  if (thresholdedImageCheckBox.isSelected()) {
						if (ov == null)
							ov = new OverlayThreshold(seqCamData);
						if (seqCamData != null)
							seqCamData.seq.addOverlay(ov);
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
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
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
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
		seqCamData.seq.removeOverlay(ov);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
			seqCamData.cages.detect.threshold = Integer.parseInt(thresholdSpinner.getValue().toString());
			updateOverlay();
		}
	}
	
	private boolean initTrackParameters() {
		if (trackAllFliesThread == null)
			return false;
		DetectFlies_Options detect = new DetectFlies_Options();
		detect.btrackWhite 		= true;
		detect.blimitLow 		= objectLowsizeCheckBox.isSelected();
		detect.blimitUp 		= objectUpsizeCheckBox.isSelected();
		detect.limitLow 		= (int) objectLowsizeSpinner.getValue();
		detect.limitUp 			= (int) objectUpsizeSpinner.getValue();
		detect.jitter 			= (int) jitterTextField.getValue();
		trackAllFliesThread.seqCamData 	= parent0.expList.getSeqCamData(parent0.currentIndex);;		
		trackAllFliesThread.stopFlag 	= false;
		trackAllFliesThread.detect 		= detect;
		trackAllFliesThread.viewInternalImages = viewsCheckBox.isSelected();
		return true;
	}
	
	private void cleanPreviousDetections() {
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
		seqCamData.cages.flyPositionsList.clear();
		ArrayList<ROI2D> list = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (roi.getName().contains("det")) {
				seqCamData.seq.removeROI(roi);
			}
		}
	}

	void builBackgroundImage() {
		if (trackAllFliesThread == null)
			trackAllFliesThread = new DetectFlies();
		if (trackAllFliesThread.threadRunning) {
			stopComputation();
			return;
		}
		initTrackParameters();
		trackAllFliesThread.buildBackground	= true;
		trackAllFliesThread.detectFlies		= false;
		ThreadUtil.bgRun(trackAllFliesThread);
	}
	
	void startComputation() {
		if (trackAllFliesThread == null)
			trackAllFliesThread = new DetectFlies();		
		if (trackAllFliesThread.threadRunning) {
			stopComputation();
			return;
		}	
		initTrackParameters();
		cleanPreviousDetections();
		trackAllFliesThread.buildBackground	= false;
		trackAllFliesThread.detectFlies		= true;
		ThreadUtil.bgRun(trackAllFliesThread);
	}
	
	void stopComputation() {
		if (trackAllFliesThread != null)
			trackAllFliesThread.stopFlag = true;
	}
	
	void loadRef () {
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
		String path = seqCamData.getDirectory()+ File.separator+"results"+File.separator+"referenceImage.jpg";
		File inputfile = new File(path);
		BufferedImage image = ImageUtil.load(inputfile, true);
		if (image == null) {
			System.out.println("image not loaded / not found");
			return;
		}
		seqCamData.refImage=  IcyBufferedImage.createFrom(image);
		if (trackAllFliesThread != null && trackAllFliesThread.rectangleAllCages != null && trackAllFliesThread.seqReference != null)
			trackAllFliesThread.seqReference.seq.setImage(0,  0, IcyBufferedImageUtil.getSubImage(
					seqCamData.refImage, 
					trackAllFliesThread.rectangleAllCages));
	}
	
	void saveRef () {
		SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
		String path = seqCamData.getDirectory()+ File.separator+"results"+File.separator+"referenceImage.jpg";
		File outputfile = new File(path);
		RenderedImage image = ImageUtil.toRGBImage(seqCamData.refImage);
		boolean success = ImageUtil.save(image, "jpg", outputfile);
		if (success)
			System.out.println("successfully saved background.jpg image");
	}

}
