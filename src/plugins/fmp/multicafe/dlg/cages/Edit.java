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
import icy.util.StringUtil;
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
	private	int foundT = -1;
	private int foundCage = -1;
	
	
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
					findAllMissedPoints(exp);
			}});
		
		foundCombo.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (foundCombo.getItemCount() == 0) {
				return;
			}
			Experiment exp = parent0.expList.getCurrentExperiment();
			if (exp == null)
				return;

			String filter = (String) foundCombo.getSelectedItem();
			int indexT = StringUtil.parseInt(filter.substring(filter.indexOf("_")+1), -1);
			if (indexT < 0)
				return;
			selectImageT(exp, indexT);
			List<ROI2D> roiList = exp.seqCamData.seq.getROI2Ds();
			Collections.sort(roiList, new Comparators.ROI2D_T_Comparator());
			for ( ROI2D roi : roiList ) {
				String csName = roi.getName();
				if (roi instanceof ROI2DPoint && csName.equals( filter)) { 
					moveROItoCageCenter(exp, roi);
					selectImageT(exp, roi.getT());
					break;
					}
				}
			}});
	}
	
	void findFirstMissed (Experiment exp) {
		if (findFirst(exp)) {
			selectImageT(exp, foundT);
			Cage cage = exp.cages.getCageFromNumber(foundCage);
			String name = "det"+cage.getCageNumber()+"_"+ foundT;
			foundCombo.setSelectedItem(name);
		}
		else
			MessageDialog.showDialog("no missed point found", MessageDialog.INFORMATION_MESSAGE);
	}
	
	boolean findFirst(Experiment exp) {
		int dataSize = exp.seqCamData.nTotalFrames;
		foundT = -1;
		foundCage = -1;
		for (int indexT = 0; indexT < dataSize; indexT++) {
			for (Cage cage: exp.cages.cageList) {
				if (indexT >= cage.flyPositions.xytList.size())
					continue;
				Point2D point = cage.flyPositions.xytList.get(indexT).xyPoint;
				if (point.getX() == -1 && point.getY() == -1 ) {
					foundT = indexT;
					foundCage = cage.getCageNumberInteger();
					break;
				}
			}
		}
		return (foundT != -1);
	}
	
	void selectImageT(Experiment exp, int t) {
		Viewer viewer = exp.seqCamData.seq.getFirstViewer();
		viewer.setPositionT(t);
	}
	
	void findAllMissedPoints(Experiment exp) {
		foundCombo.removeAllItems();
		int dataSize = exp.seqCamData.nTotalFrames;
		for (int indexT = 0; indexT < dataSize; indexT++) {
			for (Cage cage: exp.cages.cageList) {
				if (indexT >= cage.flyPositions.xytList.size())
					continue;
				Point2D point = cage.flyPositions.xytList.get(indexT).xyPoint;
				if (point.getX() == -1 && point.getY() == -1 ) {
					String name = "det"+cage.getCageNumber()+"_"+ indexT;
					foundCombo.addItem(name);
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
	
	void moveROItoCageCenter(Experiment exp, ROI2D roi) {
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
