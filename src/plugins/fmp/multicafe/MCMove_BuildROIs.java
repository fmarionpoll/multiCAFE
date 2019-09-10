package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.roi.ROI;
import icy.roi.ROI2D;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class MCMove_BuildROIs extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private JButton 	addPolygon2DButton 		= new JButton("Draw Polygon2D");
	private JButton createROIsFromPolygonButton = new JButton("Create/add (from Polygon 2D)");
	private JSpinner nbcagesTextField 	= new JSpinner(new SpinnerNumberModel(10, 0, 10000, 1));
	private JSpinner width_cageTextField 	= new JSpinner(new SpinnerNumberModel(10, 0, 10000, 1));
	private JSpinner width_intervalTextField = new JSpinner(new SpinnerNumberModel(2, 0, 10000, 1));
	private int 	nbcages 				= 10;
	private int 	width_cage 				= 10;
	private int 	width_interval 			= 2;

	private MultiCAFE parent0;
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		add( GuiUtil.besidesPanel(addPolygon2DButton, createROIsFromPolygonButton));
		JLabel ncagesLabel = new JLabel("N cages ");
		JLabel cagewidthLabel = new JLabel("cage width ");
		JLabel btwcagesLabel = new JLabel("between cages ");
		ncagesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cagewidthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		btwcagesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( ncagesLabel, nbcagesTextField, new JLabel(" "), new JLabel(" ")));
		add( GuiUtil.besidesPanel( cagewidthLabel,  width_cageTextField, new JLabel(" "), new JLabel(" ")));
		add( GuiUtil.besidesPanel( btwcagesLabel, width_intervalTextField, new JLabel(" "), new JLabel(" ") ));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		
		createROIsFromPolygonButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				addROISCreatedFromSelectedPolygon();
			}});
		addPolygon2DButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				create2DPolygon();
			}});
	}
	
	void updateFromSequence() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		int nrois = exp.seqCamData.cages.cageLimitROIList.size();	
		if (nrois > 0) {
			nbcagesTextField.setValue(nrois);
			nbcages = nrois;
		}
	}

	private void create2DPolygon() {
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		final String dummyname = "perimeter_enclosing_capillaries";
		ArrayList<ROI2D> listRois = exp.seqCamData.seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName() .equals(dummyname))
				return;
		}

		Rectangle rect = exp.seqCamData.seq.getBounds2D();
		List<Point2D> points = new ArrayList<Point2D>();
		int rectleft = rect.x + rect.width /6;
		int rectright = rect.x + rect.width*5 /6;
		if (exp.seqKymos.capillaries.capillariesArrayList.size() > 0) {
			Rectangle bound0 = exp.seqKymos.capillaries.capillariesArrayList.get(0).capillaryRoi.getBounds();
			int last = exp.seqKymos.capillaries.capillariesArrayList.size() - 1;
			Rectangle bound1 = exp.seqKymos.capillaries.capillariesArrayList.get(last).capillaryRoi.getBounds();
			rectleft = bound0.x;
			rectright = bound1.x + bound1.width;
			int diff = (rectright - rectleft)*2/60;
			rectleft -= diff;
			rectright += diff;
			
		}
		
		points.add(new Point2D.Double(rectleft, rect.y + rect.height *2/3));
		points.add(new Point2D.Double(rectright, rect.y + rect.height *2/3));
		points.add(new Point2D.Double(rectright, rect.y + rect.height - 4));
		points.add(new Point2D.Double(rectleft, rect.y + rect.height - 4 ));
		ROI2DPolygon roi = new ROI2DPolygon(points);
		roi.setName(dummyname);
		exp.seqCamData.seq.addROI(roi);
		exp.seqCamData.seq.setSelectedROI(roi);
	}
		
	private void addROISCreatedFromSelectedPolygon() {
		// read values from text boxes
		try { 
			nbcages = (int) nbcagesTextField.getValue();
			width_cage = (int) width_cageTextField.getValue();
			width_interval = (int) width_intervalTextField.getValue();
		}catch( Exception e ) { new AnnounceFrame("Can't interpret one of the ROI parameters value"); }

		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		SequenceCamData seqCamData = exp.seqCamData;
		ROI2D roi = seqCamData.seq.getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame for the cages must be a ROI2D POLYGON");
			return;
		}
		Polygon roiPolygon = MulticafeTools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		seqCamData.seq.removeROI(roi);

		// generate cage frames
		int span = nbcages*width_cage + (nbcages-1)*width_interval;
		String cageRoot = "cage";
		int iRoot = -1;
		for (ROI iRoi: seqCamData.seq.getROIs()) {
			if (iRoi.getName().contains(cageRoot)) {
				String left = iRoi.getName().substring(4);
				int item = Integer.parseInt(left);
				iRoot = Math.max(iRoot, item);
			}
		}
		iRoot++;

		for (int i=0; i< nbcages; i++) {
			List<Point2D> points = new ArrayList<>();
			double span0 = (width_cage+ width_interval)*i;
			double xup = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
			double yup = roiPolygon.ypoints[0] +  (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
			Point2D.Double point0 = new Point2D.Double (xup, yup);
			points.add(point0);

			xup = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
			yup = roiPolygon.ypoints[1] +  (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) *span0 /span ;
			Point2D.Double point1 = new Point2D.Double (xup, yup);
			points.add(point1);

			double span1 = span0 + width_cage ;

			xup = roiPolygon.xpoints[1]+ (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) *span1 /span;
			yup = roiPolygon.ypoints[1]+  (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) *span1 /span;
			Point2D.Double point4 = new Point2D.Double (xup, yup);
			points.add(point4);

			xup = roiPolygon.xpoints[0]+ (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) *span1 /span;
			yup = roiPolygon.ypoints[0]+  (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) *span1 /span;
			Point2D.Double point3 = new Point2D.Double (xup, yup);
			points.add(point3);

			ROI2DPolygon roiP = new ROI2DPolygon (points);
			roiP.setName(cageRoot+String.format("%03d", iRoot));
			iRoot++;
			seqCamData.seq.addROI(roiP);
		}

		seqCamData.cages.fromROISToCages(seqCamData);
	}

}