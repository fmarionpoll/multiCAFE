package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;


public class MCExcel_Move  extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	JCheckBox 	xyCenterCheckBox 	= new JCheckBox("XY vs image", false);
	JCheckBox 	xyCageCheckBox 		= new JCheckBox("XY vs top cage", true);
	JCheckBox 	xyTipCapsCheckBox 	= new JCheckBox("XY vs tip capillaries", false);
	JCheckBox 	distanceCheckBox 	= new JCheckBox("distance", true);
	JCheckBox 	aliveCheckBox 		= new JCheckBox("alive", true);
	JCheckBox 	pauseCheckBox 		= new JCheckBox("pause intervals", true);
	
	JButton 	exportToXLSButton 	= new JButton("save XLS");
	JCheckBox	onlyaliveCheckBox   = new JCheckBox("dead=empty");	
	
	void init(GridLayout capLayout) {	
		setLayout(capLayout);
		add(GuiUtil.besidesPanel( xyCenterCheckBox, xyCageCheckBox, xyTipCapsCheckBox, new JLabel(" ")));
		add(GuiUtil.besidesPanel( distanceCheckBox, aliveCheckBox,  pauseCheckBox, new JLabel(" "))); 
		add(GuiUtil.besidesPanel( onlyaliveCheckBox, new JLabel(" "), new JLabel(" "), exportToXLSButton )); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton.addActionListener (new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				firePropertyChange("EXPORT_MOVEDATA", false, true);
			}});
	}
	
}
