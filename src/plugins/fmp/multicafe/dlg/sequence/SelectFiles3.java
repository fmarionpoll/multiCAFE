package plugins.fmp.multicafe.dlg.sequence;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafe.tools.Directories;



public class SelectFiles3 extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4172927636287523049L;
	
	private	IcyFrame 	dialogFrame 			= null;
	private JButton		newResultsButton		= new JButton("Add new directory");
	private JButton 	openSelectedButton		= new JButton("Open selected");
	private JList<String> directoriesJList		= new JList<String>(new DefaultListModel<String>());
			MCSequence_ parent1					= null;
	
	
	public void initialize (MCSequence_ paneSequence, List<String> expList) {
		parent1 = paneSequence;
		addPropertyChangeListener(parent1);

		dialogFrame = new IcyFrame ("Select directory or create new one", true, true);
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		dialogFrame.setLayout(new BorderLayout());
		dialogFrame.add(mainPanel, BorderLayout.CENTER);
		
		directoriesJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		directoriesJList.setLayoutOrientation(JList.VERTICAL);
		directoriesJList.setVisibleRowCount(expList.size());
		List<String> list = Directories.reduceFullNameToLastDirectory(expList);
		for (String fileName: list) {
			addNameToListIfNew(fileName);
		}
		mainPanel.add(openSelectedButton);
		mainPanel.add(newResultsButton);
		
		JScrollPane scrollPane = new JScrollPane(directoriesJList);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.add(GuiUtil.besidesPanel(scrollPane));	
		
		addActionListeners();
		
		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);

	}
	
	void close() {
		dialogFrame.close();
	}
	
	void addActionListeners() {
		openSelectedButton.addActionListener(new ActionListener()  {
	        @Override
	        public void actionPerformed(ActionEvent arg0) {
	        	parent1.name = directoriesJList.getSelectedValue();
				firePropertyChange("DIRECTORY_SELECTED", false, true);
	        }});
		newResultsButton.addActionListener(new ActionListener()  {
	        @Override
	        public void actionPerformed(ActionEvent arg0) {
	        	
	        }});
	}

	private void addNameToListIfNew(String fileName) {	
		int ilast = ((DefaultListModel<String>) directoriesJList.getModel()).getSize();
		boolean found = false;
		for (int i=0; i < ilast; i++) {
			String oo = ((DefaultListModel<String>) directoriesJList.getModel()).getElementAt(i);
			if (oo.equalsIgnoreCase (fileName)) {
				found = true;
				break;
			}
		}
		if (!found)
			((DefaultListModel<String>) directoriesJList.getModel()).addElement(fileName);
	}


	

}
