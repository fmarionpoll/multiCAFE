package plugins.fmp.multicafe;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JCheckBox;
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

	private MultiCAFE parent0 = null;

	
	void init(GridLayout capLayout, MultiCAFE parent0) {	
		
		setLayout(capLayout);
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		setLayout(layout);
		this.parent0 = parent0;
	
		add(new JLabel(" ROIs: "));
		add(viewCapillariesCheckBox);
		add(viewCagesCheckbox);
		add(viewFlyCheckbox);
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

}
