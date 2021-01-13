package plugins.fmp.multicafe.dlg.cages;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import icy.gui.dialog.MessageDialog;
import icy.gui.viewer.Viewer;
import icy.roi.ROI2D;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Cage;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.tools.Comparators;
import plugins.kernel.roi.roi2d.ROI2DPoint;



public class Edit extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiCAFE 	parent0;
	private JButton 	findAllButton	= new JButton(new String("Find all missed points"));
	private JButton 	findButton		= new JButton(new String("Select next missed point"));
	private JButton 	validateButton 	= new JButton(new String("Validate changed ROIs"));
	private JComboBox<String> foundCombo = new JComboBox<String>();
	
	
	// ----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		FlowLayout flowLayout = new FlowLayout(FlowLayout.RIGHT);
		flowLayout.setVgap(0);
		
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(findAllButton);
		panel1.add(foundCombo);
		add(panel1);
		
		JPanel panel2 = new JPanel(flowLayout);
		panel2.add(findButton);
		add(panel2);
		
		JPanel panel3 = new JPanel(flowLayout);
		panel3.add(validateButton);
		add(panel3);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		validateButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null) 
					exp.saveDetRoisToPositions();
			}});
		
		findButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null) 
					findFirstMissed(exp);
			}});
		
		findAllButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null) 
					findAll(exp);
			}});
		
		foundCombo.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (foundCombo.getItemCount() == 0) {
				return;
			}
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (exp == null)
				return;

			String filter = (String) foundCombo.getSelectedItem();
			List<ROI2D> roiList = exp.seqCamData.seq.getROI2Ds();
			Collections.sort(roiList, new Comparators.ROI2D_T_Comparator());
			for ( ROI2D roi : roiList ) {
				String csName = roi.getName();
				if (roi instanceof ROI2DPoint && csName.equals( filter)) { 
					centerROIToCage(exp, roi);
					selectImageT(exp, roi.getT());
					break;
					}
				}
			}});
	}
	
	void findFirstMissed (Experiment exp) {
		List<ROI2D> roiList = exp.seqCamData.seq.getROI2Ds();
		Collections.sort(roiList, new Comparators.ROI2D_T_Comparator());
		
		String filter = "det";
		for ( ROI2D roi : roiList ) {
			String csName = roi.getName();
			if (roi instanceof ROI2DPoint && csName.contains( filter)) { 
				Point2D point = ((ROI2DPoint) roi).getPoint();
				if (point.getX() == -1 && point.getY() == -1 && roi.getColor() != Color.RED) {
					centerROIToCage(exp, roi);
					selectImageT(exp, roi.getT());
					foundCombo.setSelectedItem(roi.getName());
					return;
				}
			}
		}
		MessageDialog.showDialog("no missed point found", MessageDialog.INFORMATION_MESSAGE);
	} 
	
	void selectImageT(Experiment exp, int t) {
		Viewer viewer = exp.seqCamData.seq.getFirstViewer();
		viewer.setPositionT(t);
	}
	
	void findAll(Experiment exp) {
		foundCombo.removeAllItems();
		List<ROI2D> roiList = exp.seqCamData.seq.getROI2Ds();
		Collections.sort(roiList, new Comparators.ROI2D_T_Comparator());
		
		String filter = "det";
		for ( ROI2D roi : roiList ) {
			String csName = roi.getName();
			if (roi instanceof ROI2DPoint && csName.contains( filter)) { 
				Point2D point = ((ROI2DPoint) roi).getPoint();
				if (point.getX() == -1 && point.getY() == -1 ) {
					foundCombo.addItem(roi.getName());
				}
			}
		}
		if (foundCombo.getItemCount() == 0)
			MessageDialog.showDialog("no missed point found", MessageDialog.INFORMATION_MESSAGE);
	}
	
	private int getCageNumberFromName(String name) {
		int cagenumber = -1;
		String strCageNumber = name.substring(4, 6);
		try {
		    return Integer.parseInt(strCageNumber);
		  } catch (NumberFormatException e) {
		    return cagenumber;
		  }
	}
	
	void centerROIToCage(Experiment exp, ROI2D roi) {
		roi.setColor(Color.RED);
		exp.seqCamData.seq.setSelectedROI(roi);
		String csName = roi.getName();
		int cageNumber = getCageNumberFromName(csName);
		if (cageNumber >= 0) {
			Cage cage = exp.cages.getCageFromNumber(cageNumber);
			Rectangle rect = cage.cageRoi.getBounds();
			Point2D point2 = new Point2D.Double(rect.x+rect.width/2, rect.y+rect.height/2);
			roi.setPosition2D(point2);
		}
	}

}
