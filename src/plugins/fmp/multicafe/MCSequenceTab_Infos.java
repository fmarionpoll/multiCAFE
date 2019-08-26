package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Capillaries;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeTools.ComboBoxWide;
import plugins.fmp.multicafeTools.ComboBoxWithIndexTextRenderer;

public class MCSequenceTab_Infos  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2190848825783418962L;

	private JComboBox<String>	commentJCombo		= new JComboBox<String>();
	private JComboBox<String> 	boxID_JCombo		= new JComboBox<String>();
	private JComboBox<String> 	experimentJCombo 	= new JComboBox<String>();
	private JButton  			previousButton		= new JButton("<");
	private JButton				nextButton			= new JButton(">");
	JComboBox<String> 			expListComboBox	= new ComboBoxWide();
	boolean 					disableChangeFile 	= false;
	private MultiCAFE 			parent0 			= null;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		JPanel k2Panel = new JPanel();
		k2Panel.setLayout(new BorderLayout());
		k2Panel.add(previousButton, BorderLayout.WEST); 
		int bWidth = 30;
		int height = 10;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		k2Panel.add(expListComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, height)); 
		k2Panel.add(nextButton, BorderLayout.EAST);
		add(GuiUtil.besidesPanel( k2Panel));
		
		add( GuiUtil.besidesPanel(
				createComboPanel("Experiment ", experimentJCombo),  
				createComboPanel("  Box ID ",  boxID_JCombo)));
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel("Comment   ", SwingConstants.RIGHT), BorderLayout.WEST); 
		panel.add(commentJCombo, BorderLayout.CENTER);
		add( GuiUtil.besidesPanel(panel));

		boxID_JCombo.setEditable(true);
		experimentJCombo.setEditable(true);	
		commentJCombo.setEditable(true);
		
		defineActionListeners();
		
		ComboBoxWithIndexTextRenderer renderer = new ComboBoxWithIndexTextRenderer();
		expListComboBox.setRenderer(renderer);

		expListComboBox.addItemListener(new ItemListener() {
	        public void itemStateChanged(ItemEvent arg0) {
	        	if (arg0.getStateChange() == ItemEvent.DESELECTED) {
	        		ThreadUtil.bgRun( new Runnable() { @Override public void run() {
		        		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		        		parent0.sequencePane.closeTab.saveAndClose(exp);
	        		}});
	        	}
	        	else if (arg0.getStateChange () == ItemEvent.SELECTED) {
	        		updateBrowseInterface();
	        	}
	        }
	    });
	}
	
	private void defineActionListeners() {
		expListComboBox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (expListComboBox.getItemCount() == 0 || disableChangeFile)
				return;
			updateCombos();
			parent0.capillariesPane.infosTab.updateCombos();
			// TODO save capillaries, measures, etc here?
			Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
			String oldtext = exp.seqCamData.getFileName();
			String newtext = (String) expListComboBox.getSelectedItem();
			if (!newtext.equals(oldtext)) {
				parent0.previousIndex = parent0.currentIndex;
				parent0.currentIndex = parent0.expList.getPositionOfCamFileName(newtext);
				firePropertyChange("SEQ_OPEN", false, true);
			}
			updateBrowseInterface();
		} } );
		
		nextButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if ( expListComboBox.getSelectedIndex() < (expListComboBox.getItemCount() -1)) {
				expListComboBox.setSelectedIndex(expListComboBox.getSelectedIndex()+1);
			}
		} } );
		
		previousButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			if (expListComboBox.getSelectedIndex() > 0) {
				expListComboBox.setSelectedIndex(expListComboBox.getSelectedIndex()-1);
			}
		} } );
	}
	
	void updateBrowseInterface() {
		int isel = expListComboBox.getSelectedIndex();
		boolean flag1 = (isel == 0? false: true);
		boolean flag2 = (isel == (expListComboBox.getItemCount() -1)? false: true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
	}
	
	private JPanel createComboPanel(String text, JComboBox<String> combo) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(new JLabel(text, SwingConstants.RIGHT), BorderLayout.WEST); 
		panel.add(combo, BorderLayout.CENTER);
		return panel;
	}
		
	// set/ get
	
	void setCapillariesInfosToDialog(Capillaries cap) {

		addItem(boxID_JCombo, cap.desc.boxID);
		addItem(experimentJCombo, cap.desc.experiment);
		addItem(commentJCombo, cap.desc.comment);
	}

	void getCapillariesInfosFromDialog(Capillaries cap) {

		cap.desc.boxID = (String) boxID_JCombo.getSelectedItem();
		cap.desc.experiment = (String) experimentJCombo.getSelectedItem();
		cap.desc.comment = (String) commentJCombo.getSelectedItem();
	}
	
	private void addItem(JComboBox<String> combo, String text) {
		if (text == null)
			return;
		combo.setSelectedItem(text);
		if (combo.getSelectedIndex() < 0) {
			boolean found = false;
			for (int i=0; i < combo.getItemCount(); i++) {
				int comparison = text.compareTo(combo.getItemAt(i));
				if (comparison > 0)
					continue;
				if (comparison < 0) {
					found = true;
					combo.insertItemAt(text, i);
					break;
				}
			}
			if (!found)
				combo.addItem(text);
			combo.setSelectedItem(text);
		}
	}	
	
	void updateCombos () {
		addItem(boxID_JCombo, (String) boxID_JCombo.getSelectedItem());
		addItem(experimentJCombo, (String) experimentJCombo.getSelectedItem());
		addItem(commentJCombo, (String) commentJCombo.getSelectedItem());
	}
	

}

