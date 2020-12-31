package plugins.fmp.multicafe.dlg.cages;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import icy.gui.dialog.MessageDialog;
import icy.gui.viewer.Viewer;
import icy.roi.ROI2D;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Cage;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.kernel.roi.roi2d.ROI2DPoint;



public class Edit extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private MultiCAFE parent0;
	
	private JButton 	findButton		= new JButton(new String("Find missed detection"));
	private JButton 	validateButton 	= new JButton(new String("Validate changed ROIs"));
	
	// ----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		
		JPanel panel0 = new JPanel(flowLayout);
		panel0.add(findButton);
		add(panel0);
		
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(validateButton);
		add(panel1);
		
		
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		validateButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null) {
					for (Cage cage : exp.cages.cageList) {
						cage.transferRoisToPositions();
					}
				}
			}});
		
		findButton.addActionListener(new ActionListener () {
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				if (exp != null) 
					findFirstMissed(exp);
			}});
	}
	
	void findFirstMissed (Experiment exp) {
		List<ROI2D> roiList = exp.seqCamData.seq.getROI2Ds();
		String filter = "det";
		for ( ROI2D roi : roiList ) {
			String csName = roi.getName();
			if (roi instanceof ROI2DPoint && csName.contains( filter)) { 
				Point2D point = ((ROI2DPoint) roi).getPoint();
				if (point.getX() == -1 && point.getY() == -1 && roi.getColor() != Color.RED) {
					roi.setColor(Color.RED);
					roi.setSelected(true);
					int cageNumber = getCageNumberFromName(csName);
					if (cageNumber >= 0) {
						Cage cage = exp.cages.getCageFromNumber(cageNumber);
						Rectangle rect = cage.cageRoi.getBounds();
						Point2D point2 = new Point2D.Double(rect.x+rect.width/2, rect.y+rect.height/2);
						roi.setPosition2D(point2);
					}
					Viewer viewer = exp.seqCamData.seq.getFirstViewer();
					viewer.setPositionT(roi.getT());
					return;
				}
			}
		}
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

}
