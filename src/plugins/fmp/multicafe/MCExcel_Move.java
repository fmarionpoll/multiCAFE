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

	JCheckBox 	xyCenterCheckBox 	= new JCheckBox("XY vs image", true);
	JCheckBox 	xyCageCheckBox 		= new JCheckBox("XY vs top cage", true);
	JCheckBox 	xyTipCapsCheckBox 	= new JCheckBox("XY vs tip capillaries", true);
	JCheckBox 	distanceCheckBox 	= new JCheckBox("distance", false);
	JCheckBox 	aliveCheckBox 		= new JCheckBox("alive", true);
	JButton 	exportToXLSButton 	= new JButton("save XLS");
	
	void init(GridLayout capLayout) {	
		setLayout(capLayout);
		add(GuiUtil.besidesPanel( xyCenterCheckBox, xyCageCheckBox, xyTipCapsCheckBox, new JLabel(" ")));
		add(GuiUtil.besidesPanel( distanceCheckBox, new JLabel(" "), new JLabel(" "), new JLabel(" "), new JLabel(" "))); 
		add(GuiUtil.besidesPanel( aliveCheckBox, new JLabel(" "), new JLabel(" "), exportToXLSButton )); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton.addActionListener (new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				firePropertyChange("EXPORT_MOVEDATA", false, true);
			}});
	}
	
}
