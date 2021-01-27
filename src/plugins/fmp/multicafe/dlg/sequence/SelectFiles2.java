package plugins.fmp.multicafe.dlg.sequence;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafe.tools.Directories;



public class SelectFiles2 extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4172927636287523049L;
	
	private	IcyFrame 	dialogFrame 			= null;
	private	JLabel		comment1				= new JLabel("Select  existing item or enter");
	private	JLabel		comment2				= new JLabel("'results' name to create");
	private	JLabel		comment3				= new JLabel("a new experiment");

	private JButton 	openSelectedButton		= new JButton("Validate choice");
	private JComboBox<String> dirJCombo			= new JComboBox<String>();
			MCSequence_ parent1					= null;
	
	
	public void initialize (MCSequence_ paneSequence, List<String> expList) {
		parent1 = paneSequence;
		addPropertyChangeListener(parent1);

		List<String> list = Directories.reduceFullNameToLastDirectory(expList);
		for (String fileName: list) {
			dirJCombo.addItem(fileName);
		}
		
		dialogFrame = new IcyFrame ("Select or Create", true, true);
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		dialogFrame.setLayout(new BorderLayout());
		dialogFrame.add(mainPanel, BorderLayout.CENTER);
		
		mainPanel.add(comment1);
		mainPanel.add(comment2);
		mainPanel.add(comment3);
		mainPanel.add(dirJCombo);
		mainPanel.add(openSelectedButton);
		
		addActionListeners();
		
		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);

		dirJCombo.setEditable(true);
		dirJCombo.showPopup();	
	}
	
	void close() {
		dialogFrame.close();
	}
	
	void addActionListeners() {
		openSelectedButton.addActionListener(new ActionListener()  {
	        @Override
	        public void actionPerformed(ActionEvent arg0) {
	        	parent1.name = (String) dirJCombo.getSelectedItem();
				firePropertyChange("DIRECTORY_SELECTED", false, true);
				close();
	        }});
	}

}
