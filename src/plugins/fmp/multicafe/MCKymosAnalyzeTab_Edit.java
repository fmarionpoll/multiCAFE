package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import icy.roi.ROI2D;
import plugins.fmp.multicafeSequence.SequenceKymos;

public class MCKymosAnalyzeTab_Edit  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2580935598417087197L;
	private MultiCAFE 	parent0;
	private JComboBox<String> 	roiTypeCombo = new JComboBox<String> (new String[] {" upper level", "lower level", "derivative", "gulps" });
	
	private JButton 	selectButton 	= new JButton("Select points");
	private JButton 	deleteButton 	= new JButton("Delete");
	private JButton		replaceButton	= new JButton("Replace");
	private JButton		moveButton		= new JButton("Move vertically");
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(new JLabel("Source:"), new JLabel(" "), selectButton, deleteButton));
		add(GuiUtil.besidesPanel(roiTypeCombo, new JLabel(" "), new JLabel(" "), moveButton));
		add(GuiUtil.besidesPanel(new JLabel(" "), new JLabel(" "), new JLabel(" "), replaceButton));
		
		selectButton.setEnabled(false);
		moveButton.setEnabled(false);
		replaceButton.setEnabled(false);
		
		defineListeners();
	}
	
	private void defineListeners() {

		deleteButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				deletePointsIncluded();
			}});
	}

	void deletePointsIncluded() {
		SequenceKymos seqKymos = parent0.expList.getExperiment(parent0.currentIndex).seqKymos;
		ROI2D roi = seqKymos.seq.getSelectedROI2D();
		if (roi == null)
			return;
		
	}
}
