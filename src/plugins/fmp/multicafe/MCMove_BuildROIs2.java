package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import icy.roi.ROI2D;

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
	private static final long serialVersionUID = -121724000730795396L;
	private JButton addPolygon2DButton 			= new JButton("Draw Polygon2D");
	private JButton createROIsFromPolygonButton = new JButton("Create/add (from Polygon 2D)");
	private JSpinner thresholdSpinner 			= new JSpinner(new SpinnerNumberModel(60, 0, 10000, 1));
	private JCheckBox overlayCheckBox			= new JCheckBox("Overlay ", false);
	private JCheckBox whiteBackGroundCheckBox	= new JCheckBox("white background", false);
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
		
		add( GuiUtil.besidesPanel(addPolygon2DButton, createROIsFromPolygonButton));
		JLabel videochannel = new JLabel("filter operation ");
		videochannel.setHorizontalAlignment(SwingConstants.RIGHT);
		transformForLevelsComboBox.setSelectedIndex(2);
		add( GuiUtil.besidesPanel( videochannel, transformForLevelsComboBox, whiteBackGroundCheckBox, new JLabel(" ")));
		add( GuiUtil.besidesPanel( overlayCheckBox,  thresholdSpinner, new JLabel(" "), new JLabel(" ")));
		
		defineActionListeners();
		thresholdSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		createROIsFromPolygonButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				if (exp != null) {
					createROIsFromSelectedPolygon(exp);
					exp.cages.getCagesFromROIs(exp.seqCamData);
					exp.cages.setFirstAndLastCageToZeroFly();
				}
			}});
		
		addPolygon2DButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				if (exp != null)
					create2DPolygon(exp);
			}});
	
		transformForLevelsComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				if (exp != null)
					updateOverlay(exp);
			}});
		
		overlayCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
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
		      }});
	}

	// -----------------------------------

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
	}
	
	// -----------------------------------
	
	private void create2DPolygon(Experiment exp) {
		final String dummyname = "perimeter_enclosing";
		ROI2DPolygon roiArea = findRoiArea(exp);
		if (roiArea != null)
			return;
				
		Rectangle rect = exp.seqCamData.seq.getBounds2D();
		List<Point2D> points = new ArrayList<Point2D>();
		int rectleft = rect.x + rect.width /6;
		int rectright = rect.x + rect.width*5 /6;
		int recttop = rect.y + rect.height *2/3; 
		if (exp.capillaries.capillariesArrayList.size() > 0) {
			Rectangle bound0 = exp.capillaries.capillariesArrayList.get(0).roi.getBounds();
			int last = exp.capillaries.capillariesArrayList.size() - 1;
			Rectangle bound1 = exp.capillaries.capillariesArrayList.get(last).roi.getBounds();
			rectleft = bound0.x;
			rectright = bound1.x + bound1.width;
			int diff = (rectright - rectleft)*2/60;
			rectleft -= diff;
			rectright += diff;
			recttop = bound0.y+ bound0.height- (bound0.height /8);
		}
		points.add(new Point2D.Double(rectleft, recttop));
		points.add(new Point2D.Double(rectright, recttop));
		points.add(new Point2D.Double(rectright, rect.y + rect.height - 4));
		points.add(new Point2D.Double(rectleft, rect.y + rect.height - 4 ));
		
		roiArea = new ROI2DPolygon(points);
		roiArea.setName(dummyname);
		exp.seqCamData.seq.addROI(roiArea);
		exp.seqCamData.seq.setSelectedROI(roiArea);
	}
	
	private ROI2DPolygon findRoiArea(Experiment exp) {
		final String dummyname = "perimeter_enclosing";
		ArrayList<ROI2D> listRois = exp.seqCamData.seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName() .equals(dummyname))
				return (ROI2DPolygon) roi;
		}
		return (ROI2DPolygon) null;
	}
		
	private void createROIsFromSelectedPolygon(Experiment exp) {
		ROI2DPolygon roiArea = findRoiArea(exp);
		if (roiArea == null)
			return;
		if (exp.seqCamData.cacheThresholdedImage == null)
			return;
		exp.cages.removeAllRoiCagesFromSequence(exp.seqCamData);

		Rectangle rectGrid = roiArea.getBounds();
		IcyBufferedImage img0 = IcyBufferedImageUtil.convertToType(exp.seqCamData.cacheThresholdedImage, DataType.INT, false);
		Blobs blobs = new Blobs(IcyBufferedImageUtil.getSubImage(img0, rectGrid));
		blobs.getPixelsConnected ();
		blobs.getBlobsConnected();
		blobs.fillBlanksPixelsWithinBlobs ();
	
		List<Integer> blobsfound = new ArrayList<Integer> ();
		for (Capillary cap : exp.capillaries.capillariesArrayList) {
			Point2D pt = cap.getCapillaryTipWithinROI2D(roiArea);
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
					roiP.setColor(Color.RED);
					exp.seqCamData.seq.addROI(roiP);
				}
			}
		}
	}
	

	

	

	
	
	

	

	
	
	

	
		

}
