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
import plugins.kernel.roi.roi2d.ROI2DEllipse;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeTools.OverlayThreshold;



public class MCMove_BuildROIs2  extends JPanel implements ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -121724000730795396L;
	private JButton addPolygon2DButton 			= new JButton("Draw Polygon2D");
	private JButton createROIsFromPolygonButton = new JButton("Create/add (from Polygon 2D)");
	private JSpinner thresholdSpinner 			= new JSpinner(new SpinnerNumberModel(60, 0, 10000, 1));
	private JCheckBox overlayCheckBox			= new JCheckBox("Overlay ", false);
	private JComboBox<String> colorChannelComboBox = new JComboBox<String> (new String[] {"Red", "Green", "Blue"});
	
	private OverlayThreshold 	ov 				= null;
	private	ROI2DPolygon 		roiArea 		= null;
	private MultiCAFE 			parent0			= null;
	
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		add( GuiUtil.besidesPanel(addPolygon2DButton, createROIsFromPolygonButton));
		JLabel videochannel = new JLabel("video channel ");
		videochannel.setHorizontalAlignment(SwingConstants.RIGHT);
		colorChannelComboBox.setSelectedIndex(2);
		add( GuiUtil.besidesPanel( videochannel, colorChannelComboBox, new JLabel(" "), new JLabel(" ")));
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
		if (ov == null) 
			ov = new OverlayThreshold(seqCamData);
		else {
			seqCamData.seq.removeOverlay(ov);
			ov.setSequence(seqCamData);
		}
		seqCamData.seq.addOverlay(ov);	
		ov.setThresholdSingle(exp.cages.detect.threshold, false);
		ov.painterChanged();
	}
	
	public void removeOverlay(Experiment exp) {
		if (exp.seqCamData != null && exp.seqCamData.seq != null)
			exp.seqCamData.seq.removeOverlay(ov);
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp != null) {
				exp.cages.detect.threshold = (int) thresholdSpinner.getValue();
				updateOverlay(exp);
			}
		}
	}
	
	// -----------------------------------
	
	private void create2DPolygon(Experiment exp) {
		final String dummyname = "perimeter_enclosing";
		ArrayList<ROI2D> listRois = exp.seqCamData.seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName() .equals(dummyname))
				return;
		}
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
		
	private void createROIsFromSelectedPolygon(Experiment exp) {
		if (roiArea == null)
			return;
		if (exp.seqCamData.cacheThresholdedImage == null)
			return;
		exp.cages.removeAllRoiCagesFromSequence(exp.seqCamData);

		Rectangle rectGrid = roiArea.getBounds();
		IcyBufferedImage img = IcyBufferedImageUtil.getSubImage(exp.seqCamData.cacheThresholdedImage, rectGrid);
		byte [] binaryData = img.getDataXYAsByte(0);
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		getPixelsConnected (sizeX, sizeY, binaryData);
		getBlobsConnected(sizeX, sizeY, binaryData);
		
		for (Capillary cap : exp.capillaries.capillariesArrayList) {
			Point2D pt = cap.getCapillaryTipWithinROI2D(roiArea);
			if (pt != null) {
				int ix = (int) (pt.getX() - rectGrid.x);
				int iy = (int) (pt.getY() - rectGrid.y);
				byte blobi = binaryData[ix + sizeX*iy];
				Rectangle leafBlobRect = getBlobRectangle(blobi, sizeX, sizeY, binaryData);
				String name = "xcage_" + blobi;
				ROI2DEllipse roiP = addLeafROIinGridRectangle (leafBlobRect, rectGrid, name);
				exp.seqCamData.seq.addROI(roiP);
			}
		}
	}
	
	
	private int getPixelsConnected (int sizeX, int sizeY, byte [] binaryData) {
		byte blobnumber = 1;
		for (int iy= 0; iy < sizeY; iy++) {
			for (int ix = 0; ix < sizeX; ix++) {					
				if (binaryData[ix + sizeX*iy] < 0) 
					continue;
				int ioffset = ix + sizeX*iy;
				int ioffsetpreviousrow = ix + sizeX*(iy-1);
				if ((iy > 0) && (ix > 0) && (binaryData[ioffsetpreviousrow-1] > 0)) 
					binaryData[ioffset] = binaryData[ioffsetpreviousrow-1];
				else if ((iy > 0) && (binaryData[ioffsetpreviousrow] > 0))
					binaryData[ioffset] = binaryData[ioffsetpreviousrow];
				else if ((iy > 0) && ((ix+1) < sizeX) &&  (binaryData[ioffsetpreviousrow+1] > 0))
					binaryData[ioffset] = binaryData[ioffsetpreviousrow+1];
				else if ((ix > 0) && (binaryData[ioffset-1] > 0))
					binaryData[ioffset] = binaryData[ioffset-1];
				else { // new blob number
					binaryData[ioffset] = blobnumber;
					blobnumber++;
				}						
			}
		}
		return (int) blobnumber -1;
	}
	
	private void getBlobsConnected (int sizeX, int sizeY, byte[] binaryData) {
		for (int iy= 0; iy < sizeY; iy++) {
			for (int ix = 0; ix < sizeX; ix++) {					
				if (binaryData[ix + sizeX*iy] < 0) 
					continue;
				int ioffset = ix + sizeX*iy;
				int ioffsetpreviousrow = ix + sizeX*(iy-1);
				byte val = binaryData[ioffset];
				if ((iy > 0) && (ix > 0) && (binaryData[ioffsetpreviousrow-1] > 0)) 
					if (binaryData[ioffsetpreviousrow-1] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffsetpreviousrow-1], val, binaryData) ;
				else if ((iy > 0) && (binaryData[ioffsetpreviousrow] > 0))
					if (binaryData[ioffsetpreviousrow] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffsetpreviousrow], val, binaryData) ;
				else if ((iy > 0) && ((ix+1) < sizeX) &&  (binaryData[ioffsetpreviousrow+1] > 0))
					if (binaryData[ioffsetpreviousrow+1] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffsetpreviousrow+1], val, binaryData) ;
				else if ((ix>0) && (binaryData[ioffset-1] > 0))
					if (binaryData[ioffset-1] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffset-1], val, binaryData) ;					
			}
		}
	}
	
	private void changeAllBlobNumber1Into2 (byte oldvalue, byte newvalue, byte [] binaryData) {
		for (int i=0; i< binaryData.length; i++) {
			if (binaryData[i] == oldvalue)
				binaryData[i] = newvalue;
		}
	}
	
	private Rectangle getBlobRectangle(byte blobNumber, int sizeX, int sizeY, byte [] binaryData) {
		Rectangle rect = new Rectangle(0, 0, 0, 0);
		int [] arrayX = new int [sizeX];
		int [] arrayY = new int [sizeY];
		for (int iy= 0; iy < sizeY; iy++) {
			for (int ix = 0; ix < sizeX; ix++) {					
				if (binaryData[ix + sizeX*iy] != blobNumber) 
					continue;
				arrayX[ix] ++;
				arrayY[iy]++;
			}
		}
		for (int i=0; i< sizeX; i++)
			if (arrayX[i] > 0) {
				rect.x = i;
				break;
			}
		for (int i = sizeX-1; i >=0; i--)
			if (arrayX[i] > 0) {
				rect.width = i-rect.x +1;
				break;
			}
		
		for (int i=0; i< sizeY; i++)
			if (arrayY[i] > 0) {
				rect.y = i;
				break;
			}
		for (int i = sizeY-1; i >=0; i--)
			if (arrayY[i] > 0) {
				rect.height = i-rect.y +1;
				break;
			}
		return rect;
	}
	
	private ROI2DEllipse addLeafROIinGridRectangle (Rectangle leafBlobRect, Rectangle rectGrid, String name) {
		double xleft = rectGrid.getX()+ leafBlobRect.getX();
		double xright = xleft + leafBlobRect.getWidth();
		double ytop = rectGrid.getY() + leafBlobRect.getY();
		double ybottom = ytop + leafBlobRect.getHeight();
		
		Point2D.Double point0 = new Point2D.Double (xleft , ytop);
		Point2D.Double point1 = new Point2D.Double (xleft , ybottom);
		Point2D.Double point2 = new Point2D.Double (xright , ybottom);
		Point2D.Double point3 = new Point2D.Double (xright , ytop);
		
		List<Point2D> points = new ArrayList<>();
		points.add(point0);
		points.add(point1);
		points.add(point2);
		points.add(point3);
		
		ROI2DEllipse roiP = new ROI2DEllipse (points.get(0), points.get(2));
		roiP.setName("leaf"+name);
		roiP.setColor(Color.RED);
		return roiP;
	}
		

}
