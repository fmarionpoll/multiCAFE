package plugins.fmp.multicafe2.dlg.capillaries;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.CapillariesWithTime;
import plugins.fmp.multicafe2.experiment.Experiment;

public class Edit extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7582410775062671523L;
	
	private JButton		editCapillariesButton	= new JButton("Change capillaries position with time");
	private MultiCAFE2 	parent0 				= null;
	private EditCapillariesTable 		editCapillariesTable = null;
	private List <CapillariesWithTime> 	capillariesArrayCopy = new ArrayList<CapillariesWithTime>();
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);	
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		flowLayout.setVgap(0);
		
		JPanel panel1 = new JPanel(flowLayout);
		panel1.add(editCapillariesButton);
		add(panel1);
		
		defineActionListeners();
		this.setParent0(parent0);
	}
	
	private void defineActionListeners() 
	{
		editCapillariesButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null)
				{
					exp.capillaries.transferDescriptionToCapillaries();
					if (editCapillariesTable == null)
						editCapillariesTable = new EditCapillariesTable();
					editCapillariesTable.initialize(parent0, capillariesArrayCopy);
				}
			}});
	}

	public MultiCAFE2 getParent0() {
		return parent0;
	}

	public void setParent0(MultiCAFE2 parent0) {
		this.parent0 = parent0;
	}
	

}