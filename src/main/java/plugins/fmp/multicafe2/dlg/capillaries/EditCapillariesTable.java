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
import icy.type.geom.Polygon2D;
import icy.type.geom.Polyline2D;

import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.CapillariesWithTimeTableModel;
import plugins.fmp.multicafe2.experiment.CapillariesWithTime;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
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
	private CapillariesWithTimeTableModel capillariesWithTimeTablemodel = null;
	private List <CapillariesWithTime> 	capillariesArrayCopy = null;
	private final String dummyname = "perimeter_enclosing_capillaries"; 
	
	
	public void initialize (MultiCAFE2 parent0, List <CapillariesWithTime> capCopy, Point pt) 
	{
		this.parent0 = parent0;
		capillariesArrayCopy = capCopy;
		
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
		
		fitToFrameButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
//				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
//				if (exp != null && exp.cages.cagesList.size() > 0) 
//				{
//					exp.cages.transferNFliesFromCagesToCapillaries(exp.capillaries.capillariesList);
//					capillariesWithTimeTablemodel.fireTableDataChanged();
//				}
			}});
		
		showFrameButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				boolean show = showFrameButton.isSelected();
				fitToFrameButton.setEnabled(show);
				showFrame(show) ;
	  			
			}});
	}
	
	void close() 
	{
		dialogFrame.close();
	}
	
	private void showFrame(boolean show) {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		SequenceCamData seqCamData = exp.seqCamData;
		if (show)
			addCapillariesFrame(seqCamData);
		else
			removeCapillariesFrame(seqCamData);
	}
	
	private void addCapillariesFrame(SequenceCamData seqCamData) {
		ArrayList<ROI2D> listRois = seqCamData.seq.getROI2Ds();
		Polygon2D polygon = getFirstPolygon2DFromROI(
				(ROI2DShape) listRois.get(0), 
				(ROI2DShape) listRois.get(listRois.size()-1));
		ROI2DPolygon roi = new ROI2DPolygon(polygon);
		roi.setName(dummyname);
		roi.setColor(Color.YELLOW);
		seqCamData.seq.addROI(roi);
		seqCamData.seq.setSelectedROI(roi);
	}
	
	private void removeCapillariesFrame(SequenceCamData seqCamData) {
		ArrayList<ROI2D> listRois = seqCamData.seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName().equals(dummyname)) {
				seqCamData.seq.removeROI(roi);
				break;
			}
		}
	}
	
	private Polygon2D getFirstPolygon2DFromROI(ROI2DShape roi1, ROI2DShape roi2) {
		List<Point2D> points = new ArrayList<Point2D>();
		points.add(getFirstPoint(roi1));
		points.add(getLastPoint(roi1));
		points.add(getFirstPoint(roi2));
		points.add(getLastPoint(roi2));
		Polygon2D polygon = new Polygon2D(points);
		Polygon2D roiPolygon = ROI2DUtilities.orderVerticesofPolygon (polygon.getPolygon());
		return roiPolygon;
	}
	
	private Point2D getFirstPoint(ROI2DShape roi) {
		ArrayList<Point2D> line = roi.getPoints();
		return line.get(0);
	}
	
	private Point2D getLastPoint(ROI2DShape roi) {
		ArrayList<Point2D> line = roi.getPoints();
		int last = line.size() -1;
		return line.get(last);
	}
	
	
	
}
