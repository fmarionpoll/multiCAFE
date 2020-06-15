package plugins.fmp.multicafe;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.viewer.Viewer;
import icy.roi.ROI;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Experiment;



public class MCSequence_Display  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8137492850312895195L;
	JCheckBox 	viewCapillariesCheckBox = new JCheckBox("capillaries", true);
	JCheckBox 	viewCagesCheckbox 		= new JCheckBox("cages", true);
	JCheckBox 	viewFlyCheckbox 		= new JCheckBox("flies position", true);
	JComboBox<String> viewResultsCombo	= new JComboBox <String>();
	private MultiCAFE parent0 = null;

	
	void init(GridLayout capLayout, MultiCAFE parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		JPanel panel1 = new JPanel (layout);
		panel1.add(new JLabel(" ROIs: "));
		panel1.add(viewCapillariesCheckBox);
		panel1.add(viewCagesCheckbox);
		panel1.add(viewFlyCheckbox);
		add(panel1);
		
		JPanel panel2 = new JPanel(layout);
		panel2.add(new JLabel("available views :"));
		panel2.add(viewResultsCombo);
		add(panel2);
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		viewCapillariesCheckBox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			displayROIsCategory(viewCapillariesCheckBox.isSelected(), "line");
		} } );
		
		viewCagesCheckbox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			displayROIsCategory(viewCagesCheckbox.isSelected(), "cage");
		} } );
		
		viewFlyCheckbox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			displayROIsCategory(viewFlyCheckbox.isSelected(), "det");
		} } );
		
		viewResultsCombo.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
			if (exp == null)
				return;
			String localString = (String) viewResultsCombo.getSelectedItem();
			if (localString != null && !localString.contentEquals(exp.resultsSubPath)) {
				firePropertyChange("SEQ_CHGBIN", false, true);
			}
		} } );
	}
	
	private void displayROIsCategory(boolean isVisible, String pattern) {
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
		if (exp == null)
			return;
		Viewer v = exp.seqCamData.seq.getFirstViewer();
		IcyCanvas canvas = v.getCanvas();
		List<Layer> layers = canvas.getLayers(false);
		if (layers == null)
			return;
		ThreadUtil.bgRun(new Runnable() {
			@Override
			public void run() {
				for (Layer layer: layers) {
					ROI roi = layer.getAttachedROI();
					if (roi == null)
						continue;
					String cs = roi.getName();
					if (cs.contains(pattern))  
						layer.setVisible(isVisible);
				}
			}
		});
	}
	
	void updateResultsAvailable(Experiment exp) {
		viewResultsCombo.removeAllItems();
		for (int i = 0; i < exp.resultsDirList.size(); i++) {
			String dirName = exp.resultsDirList.get(i);
			viewResultsCombo.addItem(dirName);
			}
	}

}
