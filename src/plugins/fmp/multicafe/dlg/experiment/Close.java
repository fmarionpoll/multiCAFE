package plugins.fmp.multicafe.dlg.experiment;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;



public class Close  extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7576474358794296471L;
	private JButton		closeAllButton			= new JButton("Close views");
	private MultiCAFE 	parent0 				= null;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) 
	{
		setLayout(capLayout);
		this.parent0  = parent0;
		add( GuiUtil.besidesPanel(closeAllButton, new JLabel(" ")));
		closeAllButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				closeAll();
				firePropertyChange("CLOSE_ALL", false, true);
			}});
	}
	
	void closeAll() 
	{
		closeCurrentExperiment();
		parent0.expList.removeAllItems();
	}
	
	public void closeExp(Experiment exp) 
	{
		if (exp != null) 
		{
			parent0.paneSequence.tabInfosSeq.getExperimentInfosFromDialog(exp);
			if (exp.seqCamData != null) 
			{
				exp.xmlSaveMCExperiment();
				exp.saveExperimentMeasures(exp.getKymosDirectory());
			}
			exp.closeExperiment();
		}
		parent0.paneCages.tabGraphics.closeAll();
		parent0.paneLevels.tabGraphs.closeAll();
		parent0.paneKymos.tabDisplay.kymosComboBox.removeAllItems();
	}
	
	public void closeCurrentExperiment() 
	{
		if (parent0.expList.getSelectedIndex() < 0)
			return;
		Experiment exp =(Experiment)  parent0.expList.getSelectedItem();
		if (exp != null)
			closeExp(exp);
	}

}
