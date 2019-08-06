package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.Capillary;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCKymosTab_DetectLimits  extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6329863521455897561L;
	
	JComboBox<String> 	directionComboBox 		= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	JCheckBox			detectAllCheckBox 		= new JCheckBox ("all", true);
	private JSpinner 	detectTopSpinner 		= new JSpinner(new SpinnerNumberModel(35, 1, 255, 1));
	JComboBox<TransformOp> transformForLevelsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.RGB,
			TransformOp.GBMINUS_2R, TransformOp.RBMINUS_2G, TransformOp.RGMINUS_2B, 
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});
	
	private JButton		displayTransform1Button	= new JButton("Display");
	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private JButton 	detectTopButton 		= new JButton("Detect");
	MultiCAFE 			parent0 				= null;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		add( GuiUtil.besidesPanel(directionComboBox, detectTopSpinner, transformForLevelsComboBox, displayTransform1Button )); 
		add( GuiUtil.besidesPanel(new JLabel("span ", SwingConstants.RIGHT), spanTopSpinner, new JLabel(" "), new JLabel(" ")));
		add( GuiUtil.besidesPanel( detectTopButton,  detectAllCheckBox, new JLabel(" ")));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		transformForLevelsComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				if (parent0.vSequence != null) {
					kymosDisplayFiltered1();
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		detectTopButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				kymosDisplayFiltered1();
				MCBuildDetect_LimitsOptions options = new MCBuildDetect_LimitsOptions();
				options.transformForLevels 		= (TransformOp) transformForLevelsComboBox.getSelectedItem();
				options.directionUp 			= (directionComboBox.getSelectedIndex() == 0);
				options.detectLevelThreshold 	= (int) getDetectLevelThreshold();
				options.detectAllImages 		= detectAllCheckBox.isSelected();
				options.firstImage 				= parent0.capillariesPane.optionsTab.kymographNamesComboBox.getSelectedIndex();

				MCBuildDetect_Limits detect = new MCBuildDetect_Limits();
				detect.detectCapillaryLevels(options, parent0.vkymos);
				firePropertyChange("KYMO_DETECT_TOP", false, true);
			}});		
		displayTransform1Button.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				kymosDisplayFiltered1();
				firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
			}});
	}
	
	// -------------------------------------------------
	
	int getDetectLevelThreshold() {
		return (int) detectTopSpinner.getValue();
	}

	void setDetectLevelThreshold (int threshold) {
		detectTopSpinner.setValue(threshold);
	}
	
	int getSpanDiffTop() {
		return (int) spanTopSpinner.getValue() ;
	}
		
	void kymosDisplayFiltered1() {
		if (parent0.vkymos == null)
			return;
		TransformOp transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		for (int t=0; t < parent0.vkymos.seq.getSizeT(); t++) {
			Capillary cap = parent0.vkymos.capillaries.capillariesArrayList.get(t);
			getInfosFromDialog(cap);		
		}
		parent0.kymographsPane.kymosBuildFiltered(0, 1, transform, getSpanDiffTop());
	}
	
	void setInfosToDialog(Capillary cap) {

		MCBuildDetect_LimitsOptions options = cap.limitsOptions;
		transformForLevelsComboBox.setSelectedItem(options.transformForLevels);
		int index =options.directionUp ? 0:1;
		directionComboBox.setSelectedIndex(index);
		setDetectLevelThreshold(options.detectLevelThreshold);
		detectTopSpinner.setValue(options.detectLevelThreshold);
		detectAllCheckBox.setSelected(options.detectAllImages);
	}
	
	void getInfosFromDialog(Capillary cap) {

		MCBuildDetect_LimitsOptions options = cap.limitsOptions;
		options.transformForLevels = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		options.directionUp = (directionComboBox.getSelectedIndex() == 0) ;
		options.detectLevelThreshold = getDetectLevelThreshold();
		options.detectAllImages = detectAllCheckBox.isSelected();
	}
}
