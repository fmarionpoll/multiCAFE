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
import plugins.fmp.multicafeSequence.SequenceCamData;
import plugins.fmp.multicafeSequence.SequenceKymos;
import plugins.fmp.multicafeTools.DetectLimits;
import plugins.fmp.multicafeTools.DetectLimits_Options;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;


public class MCKymosTab_DetectLimits  extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6329863521455897561L;
	
	private JComboBox<String> 	directionComboBox= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	private JCheckBox	detectAllCheckBox 		= new JCheckBox ("all", true);
	private JSpinner 	thresholdSpinner 		= new JSpinner(new SpinnerNumberModel(35, 1, 255, 1));
	private JButton		displayTransform1Button	= new JButton("Display");
	private JSpinner	spanTopSpinner			= new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
	private JButton 	detectButton 			= new JButton("Detect");
	private MultiCAFE 	parent0 				= null;
	JComboBox<TransformOp> transformForLevelsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.RGB,
			TransformOp.GBMINUS_2R, TransformOp.RBMINUS_2G, TransformOp.RGMINUS_2B, 
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});
	
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		add( GuiUtil.besidesPanel(directionComboBox, thresholdSpinner, transformForLevelsComboBox, displayTransform1Button )); 
		add( GuiUtil.besidesPanel(new JLabel("span ", SwingConstants.RIGHT), spanTopSpinner, new JLabel(" "), new JLabel(" ")));
		add( GuiUtil.besidesPanel( detectButton,  detectAllCheckBox, new JLabel(" ")));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		transformForLevelsComboBox.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				SequenceCamData seqCamData = parent0.expList.getSeqCamData(parent0.currentIndex);
				if (seqCamData != null) {
					kymosDisplayFiltered1();
					firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
				}
			}});
		
		detectButton.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				kymosDisplayFiltered1();
				DetectLimits_Options options = new DetectLimits_Options();
				options.transformForLevels 		= (TransformOp) transformForLevelsComboBox.getSelectedItem();
				options.directionUp 			= (directionComboBox.getSelectedIndex() == 0);
				options.detectLevelThreshold 	= (int) getDetectLevelThreshold();
				options.detectAllImages 		= detectAllCheckBox.isSelected();
				options.firstImage 				= parent0.buildKymosPane.optionsTab.kymographNamesComboBox.getSelectedIndex();

				DetectLimits detect = new DetectLimits();
				detect.detectCapillaryLevels(options, parent0.expList.getSeqKymos(parent0.currentIndex));
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
		return (int) thresholdSpinner.getValue();
	}

	void setDetectLevelThreshold (int threshold) {
		thresholdSpinner.setValue(threshold);
	}
	
	int getSpanDiffTop() {
		return (int) spanTopSpinner.getValue() ;
	}
		
	void kymosDisplayFiltered1() {
		SequenceKymos seqKymos = parent0.expList.getSeqKymos(parent0.currentIndex);
		if (seqKymos == null)
			return;
		TransformOp transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		for (int t=0; t < seqKymos.seq.getSizeT(); t++) {
			Capillary cap = seqKymos.capillaries.capillariesArrayList.get(t);
			getInfosFromDialog(cap);		
		}
		parent0.kymographsPane.kymosBuildFiltered(0, 1, transform, getSpanDiffTop());
	}
	
	void setInfosToDialog(Capillary cap) {

		DetectLimits_Options options = cap.limitsOptions;
		transformForLevelsComboBox.setSelectedItem(options.transformForLevels);
		int index =options.directionUp ? 0:1;
		directionComboBox.setSelectedIndex(index);
		setDetectLevelThreshold(options.detectLevelThreshold);
		thresholdSpinner.setValue(options.detectLevelThreshold);
		detectAllCheckBox.setSelected(options.detectAllImages);
	}
	
	void getInfosFromDialog(Capillary cap) {

		DetectLimits_Options options = cap.limitsOptions;
		options.transformForLevels = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		options.directionUp = (directionComboBox.getSelectedIndex() == 0) ;
		options.detectLevelThreshold = getDetectLevelThreshold();
		options.detectAllImages = detectAllCheckBox.isSelected();
	}
}
