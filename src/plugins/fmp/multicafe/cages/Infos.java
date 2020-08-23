package plugins.fmp.multicafe.cages;



import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafeSequence.Cage;
import plugins.fmp.multicafeSequence.Experiment;





public class Infos  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3325915033686366985L;
	private JButton				editCagesButton		= new JButton("edit cages");
	private MultiCAFE 			parent0 			= null;
	private Table 		dialog 				= null;
	private List <Cage> 		cagesArrayCopy 		= new ArrayList<Cage>();
	
    
	
	void init(MultiCAFE parent0) {
		this.parent0 = parent0;
		setLayout(new FlowLayout(FlowLayout.LEFT, 3, 1));	
		add(editCagesButton);
		add(new JLabel(" "));
		
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {		
		editCagesButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				Experiment exp = parent0.expList.getCurrentExperiment();
				exp.capillaries.transferDescriptionToCapillaries();
				exp.cages.transferNFliesFromCapillariesToCages(exp.capillaries.capillariesArrayList);
				dialog = new Table();
            	dialog.initialize(parent0, cagesArrayCopy);
			}});
	}

}
