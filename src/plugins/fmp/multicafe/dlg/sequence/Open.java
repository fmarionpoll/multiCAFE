package plugins.fmp.multicafe.dlg.sequence;

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
import plugins.fmp.multicafe.MultiCAFE;



public class Open extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6565346204580890307L;
	private JButton 	openButton				= new JButton("Open...");
	private JButton 	addButton				= new JButton("Add...");
	private JButton		searchButton 			= new JButton("Search for files...");
	private JCheckBox	kymographsCheckBox		= new JCheckBox("kymographs", true);
	JCheckBox			capillariesCheckBox		= new JCheckBox("capillaries", true);
	JCheckBox			cagesCheckBox			= new JCheckBox("cages", true);
	JCheckBox			measuresCheckBox		= new JCheckBox("measures", true);
	public JCheckBox			graphsCheckBox			= new JCheckBox("graphs", true);
	
	public List<String> selectedNames 			= new ArrayList<String> ();
	private SelectFiles dialog 		= null;
	
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
		layout.setVgap(0);
		
		JPanel panel1 = new JPanel(layout);
		panel1.add(openButton);
		panel1.add(addButton);
		panel1.add( searchButton);
		add( GuiUtil.besidesPanel(panel1));
		
		JPanel panel2 = new JPanel(layout);
		panel2.add(capillariesCheckBox);
		panel2.add(kymographsCheckBox);
		panel2.add(cagesCheckBox);
		panel2.add(measuresCheckBox);
		panel2.add(graphsCheckBox);
		panel2.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		add( GuiUtil.besidesPanel(panel2));

		searchButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	selectedNames = new ArrayList<String> ();
            	dialog = new SelectFiles();
            	dialog.initialize(parent0);
            }});
		
		openButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0) {
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
			dialog.close();
        	firePropertyChange("SEARCH_CLOSED", false, true);
		}
		
	}
	
	
}
