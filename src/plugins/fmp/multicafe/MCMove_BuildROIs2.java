package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

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

import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;

import icy.type.DataType;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeTools.OverlayThreshold;
import plugins.fmp.multicafeTools.Blobs;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;



public class MCMove_BuildROIs2  extends JPanel implements ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID 	= -121724000730795396L;
	private JButton 	createCagesButton 		= new JButton("Create/add cages");
	private JSpinner 	thresholdSpinner 		= new JSpinner(new SpinnerNumberModel(60, 0, 10000, 1));
	private JCheckBox 	overlayCheckBox			= new JCheckBox("Overlay ", false);
	private JCheckBox 	whiteBackGroundCheckBox	= new JCheckBox("white background", false);
	JComboBox<TransformOp> transformForLevelsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.RGB,
			TransformOp.GBMINUS_2R, TransformOp.RBMINUS_2G, TransformOp.RGMINUS_2B, 
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});
	private OverlayThreshold 	ov 				= null;
	private MultiCAFE 			parent0			= null;
	
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		add( GuiUtil.besidesPanel(createCagesButton, new JLabel(" ")));
		JLabel videochannel = new JLabel("filter operation ");
		videochannel.setHorizontalAlignment(SwingConstants.RIGHT);
		transformForLevelsComboBox.setSelectedIndex(2);
		add( GuiUtil.besidesPanel( videochannel, transformForLevelsComboBox, whiteBackGroundCheckBox, new JLabel(" ")));
		add( GuiUtil.besidesPanel( overlayCheckBox,  thresholdSpinner, new JLabel(" "), new JLabel(" ")));
		
		defineActionListeners();
		thresholdSpinner.addChangeListener(this);
		overlayCheckBox.addChangeListener(this);
	}
	
	
	private void defineActionListeners() {
		createCagesButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				if (exp != null) {
					exp.seqCamData.removeRoisContainingString(-1, "cage");
					exp.cages.removeCages();
					createROIsFromSelectedPolygon(exp);
					exp.cages.getCagesFromROIs(exp.seqCamData);
					exp.cages.setFirstAndLastCageToZeroFly();
				}
			}});
		
		transformForLevelsComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				if (exp != null)
					updateOverlay(exp);
			}});

	}
	

	public void updateOverlay (Experiment exp) {
		SequenceCamData seqCamData = exp.seqCamData;
		if (seqCamData == null)
			return;
		if (ov == null) {
			ov = new OverlayThreshold(seqCamData);
			seqCamData.seq.addOverlay(ov);
		}
		else {
			seqCamData.seq.removeOverlay(ov);
			ov.setSequence(seqCamData);
			seqCamData.seq.addOverlay(ov);
		}
		exp.cages.detect.threshold = (int) thresholdSpinner.getValue();
		ov.setThresholdTransform(
				exp.cages.detect.threshold,  
				(TransformOp) transformForLevelsComboBox.getSelectedItem(),
				whiteBackGroundCheckBox.isSelected());
		seqCamData.seq.overlayChanged(ov);
		seqCamData.seq.dataChanged();		
	}
	
	
	public void removeOverlay(Experiment exp) {
		if (exp.seqCamData != null && exp.seqCamData.seq != null)
			exp.seqCamData.seq.removeOverlay(ov);
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp != null)
				updateOverlay(exp);
		}

		else if (e.getSource() == overlayCheckBox)  {
    	  	Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
    	  	if (exp != null) {
	  			if (overlayCheckBox.isSelected()) {
					if (ov == null)
						ov = new OverlayThreshold(exp.seqCamData);
					exp.seqCamData.seq.addOverlay(ov);
					updateOverlay(exp);
				}
				else
					removeOverlay(exp);
    	  	}
		}
	}

	private void createROIsFromSelectedPolygon(Experiment exp) {
		if (exp.seqCamData.cacheThresholdedImage == null)
			return;
		exp.cages.removeAllRoiCagesFromSequence(exp.seqCamData);

		IcyBufferedImage img0 = IcyBufferedImageUtil.convertToType(exp.seqCamData.cacheThresholdedImage, DataType.INT, false);
		Rectangle rectGrid = new Rectangle(0,0, img0.getSizeX(), img0.getSizeY());
//		Blobs blobs = new Blobs(IcyBufferedImageUtil.getSubImage(img0, rectGrid));
		Blobs blobs = new Blobs(img0);
		blobs.getPixelsConnected ();
		blobs.getBlobsConnected();
		blobs.fillBlanksPixelsWithinBlobs ();
	
		List<Integer> blobsfound = new ArrayList<Integer> ();
		for (Capillary cap : exp.capillaries.capillariesArrayList) {
			Point2D pt = cap.getCapillaryLowestPoint();
			if (pt != null) {
				int ix = (int) (pt.getX() - rectGrid.x);
				int iy = (int) (pt.getY() - rectGrid.y);
				int blobi = blobs.getBlobAt(ix, iy);
				cap.cagenb = blobi;
				boolean found = false;
				for (int i: blobsfound) {
					if (i == blobi) {
						found = true;
						break;
					}
				}
				if (!found) {
					blobsfound.add(blobi);
					//ROI2DArea roiP = new ROI2DArea(blobs.getBlobBooleanMask2D(blobi));
					ROI2DPolygon roiP = new ROI2DPolygon (blobs.getBlobPolygon2D(blobi));
					roiP.translate(rectGrid.x, rectGrid.y);
					int cagenb = cap.getCageIndexFromRoiName();
					roiP.setName("cage" + String.format("%03d", cagenb));
					exp.seqCamData.seq.addROI(roiP);
				}
			}
		}
	}
	

	

	

	
	
	

	

	
	
	

	
		

}
