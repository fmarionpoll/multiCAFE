package plugins.fmp.multicafe;

import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;



public class MCSequence_Open extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6565346204580890307L;
	private JButton 	openButton				= new JButton("Open...");
	private JButton 	addButton				= new JButton("Add...");
	private JButton		searchButton 			= new JButton("Search for files...");
	private JButton		closeButton				= new JButton("Close search dialog");
	private JCheckBox	kymographsCheckBox		= new JCheckBox("kymographs", true);
	JCheckBox			capillariesCheckBox		= new JCheckBox("capillaries", true);
	JCheckBox			cagesCheckBox			= new JCheckBox("cages", true);
	JCheckBox			measuresCheckBox		= new JCheckBox("measures", true);
	JCheckBox			graphsCheckBox			= new JCheckBox("graphs", true);
	
	public List<String> selectedNames 			= new ArrayList<String> ();
	private MCSequence_SelectFiles dialog 			= null;
	
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		
		add( GuiUtil.besidesPanel(openButton, addButton));
		add( GuiUtil.besidesPanel(searchButton, closeButton));
		
		JPanel panel = new JPanel();
		FlowLayout layout = new FlowLayout();
		layout.setVgap(0);
		panel.setLayout(layout);
		panel.add(capillariesCheckBox);
		panel.add(kymographsCheckBox);
		panel.add(cagesCheckBox);
		panel.add(measuresCheckBox);
		panel.add(graphsCheckBox);
		panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		add( GuiUtil.besidesPanel(panel));

		searchButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	selectedNames = new ArrayList<String> ();
            	dialog = new MCSequence_SelectFiles();
            	dialog.initialize(parent0);
            }});
		closeButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	dialog.close();
            	firePropertyChange("SEARCH_CLOSED", false, true);
            }});
		openButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	if(parent0.paneSequence.tabInfos.expListComboBox.getItemCount() > 0 )
            		parent0.paneSequence.tabClose.closeAll();
            	firePropertyChange("SEQ_OPENFILE", false, true);
            }});
		addButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	firePropertyChange("SEQ_ADDFILE", false, true);
            }});
	}
	
	boolean isCheckedLoadPreviousProfiles() {
		return capillariesCheckBox.isSelected();
	}
	
	boolean isCheckedLoadKymographs() {
		return kymographsCheckBox.isSelected();
	}
	
	boolean isCheckedLoadCages() {
		return cagesCheckBox.isSelected();
	}
	
	boolean isCheckedLoadMeasures() {
		return measuresCheckBox.isSelected();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("SEARCH_CLOSED")) {
			closeButton.doClick();
		}
		
	}
	
	
}
