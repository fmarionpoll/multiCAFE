package plugins.fmp.multicafe.dlg.capillaries;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.roi.ROI2D;
import icy.type.geom.Polygon2D;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Capillaries;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.SequenceCamData;
import plugins.fmp.multicafe.sequence.SequenceKymosUtils;
import plugins.fmp.multicafe.tools.ROI2DUtilities;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;


public class Create extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	
	private JButton 	addPolygon2DButton 		= new JButton("Draw Polygon2D");
	private JButton 	createROIsFromPolygonButton2 = new JButton("Generate capillaries");
	private JRadioButton selectGroupedby2Button = new JRadioButton("grouped by 2");
	private JRadioButton selectRegularButton 	= new JRadioButton("evenly spaced");
	private JCheckBox	verticalCheckBox		= new JCheckBox("vertical orientation", true);
	private ButtonGroup buttonGroup2 			= new ButtonGroup();
	private JSpinner 	nbcapillariesJSpinner 	= new JSpinner(new SpinnerNumberModel(20, 0, 500, 1));
	private JSpinner 	width_between_capillariesJSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 10000, 1));
	private JSpinner 	width_intervalJSpinner = new JSpinner(new SpinnerNumberModel(53, 0, 10000, 1)); 
	private MultiCAFE 	parent0 				= null;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		
		add( GuiUtil.besidesPanel( addPolygon2DButton, createROIsFromPolygonButton2));
		buttonGroup2.add(selectGroupedby2Button);
		buttonGroup2.add(selectRegularButton);
		selectGroupedby2Button.setSelected(true);
		
		FlowLayout flowLayout1 = new FlowLayout(FlowLayout.LEFT);
		flowLayout1.setVgap(0);
		JPanel panel0 = new JPanel(flowLayout1);
		panel0.add(new JLabel ("N capillaries ", SwingConstants.RIGHT));
		panel0.add(nbcapillariesJSpinner);
		panel0.add(selectRegularButton);
		panel0.add(selectGroupedby2Button);
		panel0.add(verticalCheckBox);
		add(panel0);
		
		JPanel panel1 = new JPanel(flowLayout1);
		panel1.add(new JLabel("Pixels btw. caps ", SwingConstants.RIGHT));
		panel1.add(width_between_capillariesJSpinner);
		panel1.add(new JLabel("btw. groups ", SwingConstants.RIGHT));
		panel1.add(width_intervalJSpinner);
		add(panel1);
		
		defineActionListeners();
		this.parent0 = parent0;
	}
	
	private void defineActionListeners() {
		addPolygon2DButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
				create2DPolygon();
			}});
		
		createROIsFromPolygonButton2.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
				roisGenerateFromPolygon(verticalCheckBox.isSelected());
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null) {
					SequenceKymosUtils.transferCamDataROIStoKymo(exp);
					firePropertyChange("CAPILLARIES_NEW", false, true);
				}
			}});
		
		selectRegularButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
				boolean status = false;
				width_between_capillariesJSpinner.setEnabled(status);
				width_intervalJSpinner.setEnabled(status);
			}});
	}
	
	// set/ get	

	private int getNbCapillaries( ) {
		return (int) nbcapillariesJSpinner.getValue();
	}

	private int getWidthSmallInterval ( ) {
		return (int) width_between_capillariesJSpinner.getValue();
	}
	
	private int getWidthLongInterval() {
		return (int) width_intervalJSpinner.getValue();
	}
	
	private boolean getGroupedBy2() {
		return selectGroupedby2Button.isSelected();
	}
	
	private void setGroupedBy2(boolean flag) {
		selectGroupedby2Button.setSelected(flag);
		selectRegularButton.setSelected(!flag);
	}
	
	void setGroupingAndNumber(Capillaries cap) {
		setGroupedBy2(cap.desc.grouping == 2);
	}
	
	Capillaries getGrouping(Capillaries cap) {
		cap.desc.grouping = getGroupedBy2() ? 2: 1;
		return cap;
	}

	// ---------------------------------
	private void create2DPolygon() {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp == null)
			return;
		SequenceCamData seqCamData = exp.seqCamData;
		final String dummyname = "perimeter_enclosing_capillaries";
		ArrayList<ROI2D> listRois = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName() .equals(dummyname))
				return;
		}
		
		Rectangle rect = seqCamData.seq.getBounds2D();
		List<Point2D> points = new ArrayList<Point2D>();
		points.add(new Point2D.Double(rect.x + rect.width /5, rect.y + rect.height /5));
		points.add(new Point2D.Double(rect.x + rect.width*4 /5, rect.y + rect.height /5));
		points.add(new Point2D.Double(rect.x + rect.width*4 /5, rect.y + rect.height*2 /3));
		points.add(new Point2D.Double(rect.x + rect.width /5, rect.y + rect.height *2 /3));
		ROI2DPolygon roi = new ROI2DPolygon(points);
		roi.setName(dummyname);
		seqCamData.seq.addROI(roi);
		seqCamData.seq.setSelectedROI(roi);
	}
	
	private void swap (Polygon2D roiPolygon) {
		for (int i=0; i<roiPolygon.npoints; i++) {
			double val =  roiPolygon.xpoints[i];
			roiPolygon.xpoints[i] = roiPolygon.ypoints[i];
			roiPolygon.ypoints[i] = val;
		}
	}
	
	private void roisGenerateFromPolygon(boolean vertical) {
		Experiment exp = parent0.expList.getCurrentExperiment();
		if (exp == null)
			return;
		SequenceCamData seqCamData = exp.seqCamData;
		boolean statusGroup2Mode = false;
		if (getGroupedBy2()) statusGroup2Mode = true;
		// read values from text boxes
		int nbcapillaries = 20;
		int width_between_capillaries = 1;	// default value for statusGroup2Mode = false
		int width_interval = 0;				// default value for statusGroup2Mode = false

		try { 
			nbcapillaries = getNbCapillaries();
			if(statusGroup2Mode) {
				width_between_capillaries = getWidthSmallInterval();
				width_interval = getWidthLongInterval();
			}
		} catch( Exception e ) { new AnnounceFrame("Can't interpret one of the ROI parameters value"); }

		ROI2D roi = seqCamData.seq.getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame must be a ROI2D POLYGON");
			return;
		}
		
		Polygon2D roiPolygon = ROI2DUtilities.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		if (!vertical) {
			swap(roiPolygon);
		}
		seqCamData.seq.removeROI(roi);

		if (statusGroup2Mode) {	
			double span = (nbcapillaries/2)* (width_between_capillaries + width_interval) - width_interval;
			for (int i=0; i< nbcapillaries; i+= 2) {
				double span0 = (width_between_capillaries + width_interval)*i/2;
				double x0 = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
				double y0 = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
				if (x0 < 0) 
					x0= 0;
				if (y0 < 0) 
					y0=0;
				double x1 = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
				double y1 = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * span0 /span ;
				
				addROILine(seqCamData, "line"+i/2+"L", new Point2D.Double (x0, y0), new Point2D.Double (x1, y1));

				span0 += width_between_capillaries ;
				x0 = roiPolygon.xpoints[0]+ (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
				y0 = roiPolygon.ypoints[0]+ (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
				if (x0 < 0) 
					x0= 0;
				if (y0 < 0) 
					y0=0;
				
				x1 = roiPolygon.xpoints[1]+ (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span;
				y1 = roiPolygon.ypoints[1]+ (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * span0 /span;;
				addROILine(seqCamData, "line"+i/2+"R", new Point2D.Double (x0, y0), new Point2D.Double (x1, y1));

			}
		}
		else {
			double span = nbcapillaries-1;
			for (int i=0; i< nbcapillaries; i++) {
				double span0 = width_between_capillaries*i;
				double x0 = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
				double y0 = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;

				double x1 = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
				double y1 = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * span0 /span ;

				addROILine(seqCamData, "line"+i, new Point2D.Double (x0, y0), new Point2D.Double (x1, y1));
			}
		}
	}
	
	private void addROILine(SequenceCamData seqCamData, String name, Point2D point0, Point2D point1) {
		ROI2DLine roiL1 = new ROI2DLine (point0, point1);
		roiL1.setName(name);
		roiL1.setReadOnly(false);
		seqCamData.seq.addROI(roiL1, true);
	}
}
