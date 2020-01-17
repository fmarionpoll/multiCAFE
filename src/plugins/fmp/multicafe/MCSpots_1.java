package plugins.fmp.multicafe;


import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCSpots_1 extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7614659720134775871L;

	private MultiCAFE 	parent0 				= null;
	private JButton 	subtractButton 			= new JButton("Subtract first column");
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		add( GuiUtil.besidesPanel(subtractButton));
				
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		
		subtractButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
				subtractFirstColumn(exp);
			}});	
	}
	
	// -------------------------------------------------
	
	void subtractFirstColumn(Experiment exp) {
		SequenceKymos seqKymos = exp.seqKymos;
		if (seqKymos == null)
			return;
		TransformOp transform = TransformOp.SUBFIRSTCOL;
//		List<Capillary> capList = exp.capillaries.capillariesArrayList;
//		for (int t=0; t < exp.seqKymos.seq.getSizeT(); t++) {
//			getInfosFromDialog(capList.get(t));		
//		}
		int zChannelDestination = 1;
		exp.kymosBuildFiltered(0, zChannelDestination, transform, 0);
		seqKymos.seq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
	}
}
