package plugins.fmp.multicafe2.dlg.capillaries;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import icy.gui.frame.IcyFrame;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;

import plugins.kernel.roi.roi2d.ROI2DPolygon;

import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.CapillariesWithTimeTableModel;
import plugins.fmp.multicafe2.experiment.CapillariesWithTime;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;



public class EditCapillariesTable extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID 		= 1L;
	
	IcyFrame 					dialogFrame 		= null;
	private String				explanation 		= "Move to image, edit capillaries position and save";

    private JButton				addItem				= new JButton("Add");
    private JButton				deleteItem			= new JButton("Delete");
    private JButton				saveCapillaries   	= new JButton("Save capillaries");
    private JCheckBox			showFrameButton		= new JCheckBox("Show frame");
    private JButton				fitToFrameButton	= new JButton("Fit capillaries to frame");
    
	private MultiCAFE2 			parent0 			= null; 
	private final String 		dummyname 			= "perimeter_enclosing_capillaries";
	private ROI2DPolygon 		envelopeRoi 		= null;
	private ROI2DPolygon 		envelopeRoi_initial	= null;
	private CapillariesWithTimeTableModel capillariesWithTimeTablemodel = null;
	
	
	
	
	public void initialize (MultiCAFE2 parent0, List <CapillariesWithTime> capCopy, Point pt) 
	{
		this.parent0 = parent0;
		capillariesWithTimeTablemodel = new CapillariesWithTimeTableModel(parent0.expListCombo);
		
		JTable tableView = new JTable();    
		tableView.setModel(capillariesWithTimeTablemodel);
	    tableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    tableView.setPreferredScrollableViewportSize(new Dimension(300, 200));
	    tableView.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(tableView);
        
		JPanel topPanel = new JPanel(new GridLayout(4, 1));
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT); 
		JPanel panel0 = new JPanel (flowLayout);
		panel0.add(new JLabel(explanation));
		topPanel.add(panel0);
		
		JPanel panel1 = new JPanel (flowLayout);
		panel1.add(new JLabel("Last row:"));
		panel1.add(addItem);
		panel1.add(deleteItem);
		topPanel.add(panel1);
        
        JPanel panel2 = new JPanel (flowLayout);
        panel2.add(showFrameButton);
        panel2.add(fitToFrameButton);
        panel2.add(saveCapillaries);
        topPanel.add(panel2);
        
        JPanel panel3 = new JPanel (flowLayout);
        panel3.add(saveCapillaries);
        topPanel.add(panel3);
        
        JPanel tablePanel = new JPanel();
		tablePanel.add(scrollPane);
        
		dialogFrame = new IcyFrame ("Edit capillaries position with time", true, true);	
		dialogFrame.add(topPanel, BorderLayout.NORTH);
		dialogFrame.add(tablePanel, BorderLayout.CENTER);
		dialogFrame.setLocation(pt);
		
		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.setVisible(true);
		defineActionListeners();
		
		fitToFrameButton.setEnabled(false);	
	}
	
	private void defineActionListeners() 
	{
		
		fitToFrameButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				moveAllCapillaries();
			}});
		
		showFrameButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				boolean show = showFrameButton.isSelected();
				fitToFrameButton.setEnabled(show);
				showFrame(show) ;
	  			
			}});
	}
	
	void close() {
		dialogFrame.close();
	}
	
	private void moveAllCapillaries() {
		if (envelopeRoi == null) return;
		Point2D pt0 = envelopeRoi_initial.getPosition2D();
		Point2D pt1 = envelopeRoi.getPosition2D();
		envelopeRoi_initial = new ROI2DPolygon(envelopeRoi.getPolygon2D());
		double deltaX = pt1.getX() - pt0.getX();
		double deltaY = pt1.getY() - pt0.getY();
		shiftPositionOfCapillaries(deltaX, deltaY);		
	}
	
	private void shiftPositionOfCapillaries(double deltaX, double deltaY) {
		Sequence seq = getCurrentSequence();
		if (seq == null)
			return;
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi : listRois) {
			if (!roi.getName().contains("line")) 
				continue;
			Point2D point2d = roi.getPosition2D();
			roi.setPosition2D(new Point2D.Double(point2d.getX()+deltaX, point2d.getY()+ deltaY));
		}
	}
	
	private Sequence getCurrentSequence() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return null;
		return exp.seqCamData.seq;
	}
	
	private void showFrame(boolean show) {
		Sequence seq = getCurrentSequence();
		if (seq == null)
			return;
		
		if (show)
			addCapillariesFrame(seq);
		else 
			removeCapillariesFrame(seq);
	}
	
	private void addCapillariesFrame(Sequence seq) {
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		ROI2D roi1 = listRois.get(0);
		ROI2D roi2 = listRois.get(listRois.size()-1);
		double xmin = roi1.getBounds().getX();
		double xmax = xmin;
		for (ROI2D roi : listRois) {
			if (!roi.getName().contains("line")) 
				continue;
			double x = roi.getBounds().getX();
			if (x < xmin) {
				xmin = x;
				roi1 = roi;
				continue;
			}
			if (x > xmax) {
				xmax = x;
				roi2 = roi;
				continue;
			}
		}
		Polygon2D polygon = getPolygon2DFromROIs(roi1, roi2);
		envelopeRoi_initial = new ROI2DPolygon (polygon);
		envelopeRoi = new ROI2DPolygon(polygon);
		envelopeRoi.setName(dummyname);
		envelopeRoi.setColor(Color.YELLOW);
		
		seq.addROI(envelopeRoi);
		seq.setSelectedROI(envelopeRoi);
	}
	
	private void removeCapillariesFrame(Sequence seq) {
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName().equals(dummyname)) {
				seq.removeROI(roi);
				break;
			}
		}
		envelopeRoi = null;
	}
	
	private Polygon2D getPolygon2DFromROIs(ROI2D roi1, ROI2D roi2) {
		List<Point2D> listPoints = new ArrayList<Point2D>();
		listPoints.add(getFirstPoint(roi1));
		listPoints.add(getLastPoint(roi1));
		listPoints.add(getFirstPoint(roi2));
		listPoints.add(getLastPoint(roi2));
		Polygon2D polygon = new Polygon2D(listPoints);
		Polygon2D roiPolygon = ROI2DUtilities.orderVerticesofPolygon (polygon.getPolygon());
		return roiPolygon;
	}
	
	private Point2D getFirstPoint(ROI2D roi) {
		Rectangle rect = roi.getBounds();
		return new Point2D.Double(rect.getX(), rect.getY());
	}
	
	private Point2D getLastPoint(ROI2D roi) {
		Rectangle rect = roi.getBounds();
		return new Point2D.Double(rect.getX() + rect.getWidth(), rect.getY()+rect.getHeight());
	}
	
	
	
}
