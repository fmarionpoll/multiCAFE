package plugins.fmp.multicafe;

import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;

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
	}

}
