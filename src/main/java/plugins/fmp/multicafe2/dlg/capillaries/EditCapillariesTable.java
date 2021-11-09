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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import icy.gui.frame.IcyFrame;
import icy.gui.viewer.Viewer;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;

import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.dlg.JComponents.CapillariesWithTimeTableModel;
import plugins.fmp.multicafe2.experiment.CapillariesWithTime;
import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.ROI2DUtilities;



public class EditCapillariesTable extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID 		= 1L;
	
	IcyFrame 					dialogFrame 		= null;
	private String				explanation 		= "Move to image, edit capillaries position and save";

    private JButton				addItemButton				= new JButton("Add");
    private JButton				deleteItem			= new JButton("Delete");
    private JButton				saveCapillariesButton   	= new JButton("Save capillaries");
    private JCheckBox			showFrameButton		= new JCheckBox("Show frame");
    private JButton				fitToFrameButton	= new JButton("Fit capillaries to frame");
    private JTable 				tableView 			= new JTable();    
    
	private MultiCAFE2 			parent0 			= null; 
	private final String 		dummyname 			= "perimeter_enclosing_capillaries";
	private ROI2DPolygon 		envelopeRoi 		= null;
	private ROI2DPolygon 		envelopeRoi_initial	= null;
	private CapillariesWithTimeTableModel capillariesWithTimeTablemodel = null;
	
		
	
	public void initialize (MultiCAFE2 parent0, Point pt) 
	{
		this.parent0 = parent0;
		capillariesWithTimeTablemodel = new CapillariesWithTimeTableModel(parent0.expListCombo);
		
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
		panel1.add(addItemButton);
		panel1.add(deleteItem);
		topPanel.add(panel1);
        
        JPanel panel2 = new JPanel (flowLayout);
        panel2.add(showFrameButton);
        panel2.add(fitToFrameButton);
        panel2.add(saveCapillariesButton);
        topPanel.add(panel2);
        
        JPanel panel3 = new JPanel (flowLayout);
        panel3.add(saveCapillariesButton);
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
		defineSelectionListener();
		
		fitToFrameButton.setEnabled(false);	
	}
	
	private void defineActionListeners() {
		
		fitToFrameButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				moveAllCapillaries();
			}});
		
		showFrameButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				boolean show = showFrameButton.isSelected();
				fitToFrameButton.setEnabled(show);
				showFrame(show) ;
			}});
		
		addItemButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				addTableItem();
			}});
		
		saveCapillariesButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				int selectedRow = tableView.getSelectedRow();
				saveCapillaries(selectedRow);
			}});
	}
	
	private void defineSelectionListener() {
		tableView.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
            	if(!e.getValueIsAdjusting()) {
	                int selectedRow = tableView.getSelectedRow();
	                if (selectedRow < 0) {
	                	tableView.setRowSelectionInterval(0, 0);
	                }
	                else {
		                changeCapillaries(selectedRow);
		                boolean show = showFrameButton.isSelected();
						showFrame(show) ;
	                }
            	}
            }});
	}
	
	void close() {
		dialogFrame.close();
	}
	
	private void moveAllCapillaries() {
		if (envelopeRoi == null) 
			return;
		Point2D pt0 = envelopeRoi_initial.getPosition2D();
		Point2D pt1 = envelopeRoi.getPosition2D();
		envelopeRoi_initial = new ROI2DPolygon(envelopeRoi.getPolygon2D());
		double deltaX = pt1.getX() - pt0.getX();
		double deltaY = pt1.getY() - pt0.getY();
		shiftPositionOfCapillaries(deltaX, deltaY);		
	}
	
	private void shiftPositionOfCapillaries(double deltaX, double deltaY) {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();;
		if (exp == null) 
			return;
		Sequence seq = exp.seqCamData.seq;
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi : listRois) {
			if (!roi.getName().contains("line")) 
				continue;
			Point2D point2d = roi.getPosition2D();
			roi.setPosition2D(new Point2D.Double(point2d.getX()+deltaX, point2d.getY()+ deltaY));
		}
	}
	
	private void showFrame(boolean show) {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;
		
		if (show)
			addCapillariesFrame(exp.seqCamData.seq);
		else 
			removeCapillariesFrame(exp.seqCamData.seq);
	}
	
	private void addCapillariesFrame(Sequence seq) {
		Polygon2D polygon = ROI2DUtilities.getCapillariesFrame(seq.getROI2Ds());
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
	
	private void addTableItem() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();;
		if (exp == null) return;
		
		int nitems = exp.capillaries.capillariesWithTime.size();
		CapillariesWithTime capillaries = new CapillariesWithTime(exp.capillaries.capillariesWithTime.get(nitems-1).capillariesList);
		exp.capillaries.capillariesWithTime.add(capillaries);
	}
	
	private void changeCapillaries(int selectedRow) {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();;
		if (exp == null) 
			return;
		Sequence seq = exp.seqCamData.seq;
		
		System.out.println("changeCapîllaries ("+selectedRow+")");
		
		seq.removeAllROI();	
		CapillariesWithTime capillariesWithTime = exp.capillaries.capillariesWithTime.get(selectedRow);
		List<ROI2D> listRois = new ArrayList<ROI2D>();
		for (Capillary cap: capillariesWithTime.capillariesList)
			listRois.add(cap.getRoi());
		seq.addROIs(listRois, false);
		
		Viewer v = seq.getFirstViewer();
		v.setPositionT((int) capillariesWithTime.start);
	}
	
	private void saveCapillaries(int selectedRow) {
		System.out.println("saveCapîllaries ("+selectedRow+")");
		
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();;
		if (exp == null) 
			return;
		Sequence seq = exp.seqCamData.seq;
		
		List<ROI2D> listRois = seq.getROI2Ds();
		CapillariesWithTime capillariesWithTime = exp.capillaries.capillariesWithTime.get(selectedRow);
		for (ROI2D roi: listRois) {
			if (!roi.getName().contains("line")) 
				continue;
			Capillary cap = capillariesWithTime.getCapillaryFromName(roi.getName());
			if (cap != null) {
				ROI2D roilocal = (ROI2D) roi.getCopy();
				cap.setRoi(roilocal);
			}
		}
	}

	
}
