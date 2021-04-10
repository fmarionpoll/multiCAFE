package plugins.fmp.multicafe.dlg.experiment;

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



public class SelectFiles2 extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4172927636287523049L;
	private	IcyFrame 	dialogFrame 		= null;
	private	JLabel		comment	 			= new JLabel("<html>Select  existing item or enter 'resultsNEW' name to create a new experiment</html>");
	private JButton 	validateButton		= new JButton("Validate");
	private JComboBox<String> dirJCombo		= new JComboBox<String>();
			String		resultDirectory		= null;
	
	
	public void initialize ( List<String> expList) 
	{
		loadComboWithDirectoriesShortNames(expList);
		dialogFrame = new IcyFrame ("Select or Create", true, true);
		
		JPanel panel = GuiUtil.generatePanel();
		panel.add(GuiUtil.besidesPanel(comment));
		panel.add(GuiUtil.besidesPanel(dirJCombo, validateButton));
		dialogFrame.add(panel);
		
		dialogFrame.pack();
		dialogFrame.addToDesktopPane();
		dialogFrame.requestFocus();
		dialogFrame.center();
		dialogFrame.setVisible(true);

		dirJCombo.setEditable(true);
		
		addActionListeners();
	}
	
	void close() 
	{
		dialogFrame.close();
	}
	
	void addActionListeners() 
	{
		validateButton.addActionListener(new ActionListener()  
		{
	        @Override
	        public void actionPerformed(ActionEvent arg0) 
	        {
	        	resultDirectory = (String) dirJCombo.getSelectedItem();
				firePropertyChange("DIRECTORY_SELECTED", false, true);
	        }});
	}
	
	void loadComboWithDirectoriesShortNames(List<String> expList) 
	{
		dirJCombo.removeAllItems();
		List<String> list = Directories.reduceFullNameToLastDirectory(expList);
		for (String fileName: list) 
			dirJCombo.addItem(fileName);
	}

}
