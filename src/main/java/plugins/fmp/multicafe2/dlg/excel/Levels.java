package plugins.fmp.multicafe2.dlg.excel;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;




public class Levels extends JPanel  
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	JButton 	exportToXLSButton 	= new JButton("save XLS (v1)");
	JButton 	exportToXLSButton2 	= new JButton("save XLS");
	JCheckBox 	topLevelCheckBox 	= new JCheckBox("top", true);
	JCheckBox 	topLevelDeltaCheckBox 	= new JCheckBox("delta top", false);
	
	JCheckBox 	bottomLevelCheckBox = new JCheckBox("bottom", false);
	JCheckBox 	lrPICheckBox 		= new JCheckBox("L+R & pref index", true);
	JCheckBox 	lrRatioCheckBox 	= new JCheckBox("L+R & ratio", true);
	JCheckBox 	sumPerCageCheckBox 	= new JCheckBox("sum per cage", false);
	JCheckBox 	derivativeCheckBox  = new JCheckBox("derivative", false);
	JCheckBox	t0CheckBox			= new JCheckBox("t-t0", true);
	JCheckBox	onlyaliveCheckBox   = new JCheckBox("dead=empty", false);	
	JCheckBox	subtractEvaporationCheckBox = new JCheckBox("subtract evaporation", true);
	
	void init(GridLayout capLayout) 
	{	
		setLayout(capLayout);
		
		FlowLayout flowLayout0 = new FlowLayout(FlowLayout.LEFT);
		flowLayout0.setVgap(0);
		JPanel panel0 = new JPanel(flowLayout0);
		panel0.add(topLevelCheckBox);
		panel0.add(topLevelDeltaCheckBox);
		panel0.add(bottomLevelCheckBox);
		panel0.add(t0CheckBox);
		panel0.add(onlyaliveCheckBox);
		panel0.add(subtractEvaporationCheckBox);
		add(panel0);
		
		JPanel panel1 = new JPanel(flowLayout0);
		panel1.add(lrPICheckBox);
		panel1.add(lrRatioCheckBox);
		panel1.add(sumPerCageCheckBox);
		
		add(panel1);
		
		FlowLayout flowLayout2 = new FlowLayout(FlowLayout.RIGHT);
		flowLayout2.setVgap(0);
		JPanel panel2 = new JPanel(flowLayout2);
		panel2.add(exportToXLSButton2);
		add(panel2);
		
		defineActionListeners();
	}
	
	private void defineActionListeners() 
	{
		exportToXLSButton2.addActionListener (new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				firePropertyChange("EXPORT_KYMOSDATA", false, true);
			}});
	
		lrPICheckBox.addActionListener (new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				if (lrPICheckBox.isSelected())
					sumPerCageCheckBox.setSelected(false);
			}});
		
		lrRatioCheckBox.addActionListener (new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				if (lrRatioCheckBox.isSelected())
					sumPerCageCheckBox.setSelected(false);
			}});
		
		sumPerCageCheckBox.addActionListener (new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				if (sumPerCageCheckBox.isSelected())
					lrPICheckBox.setSelected(false);
					lrRatioCheckBox.setSelected(false);
			}});
	}

}
