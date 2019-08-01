package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;

public class MCKymosTab_DetectLimits  extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6329863521455897561L;
	
	JComboBox<String> 	directionComboBox 		= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	JCheckBox			detectAllLevelCheckBox 	= new JCheckBox ("all", true);
	private JTextField 	detectTopTextField 		= new JTextField("35");
	JComboBox<TransformOp> transformForLevelsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.RGB,
			TransformOp.GBMINUS_2R, TransformOp.RBMINUS_2G, TransformOp.RGMINUS_2B, 
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});
	
	private JButton		displayTransform1Button	= new JButton("Display");
	private JTextField	spanTopTextField		= new JTextField("3");
	private JButton 	detectTopButton 		= new JButton("Detect");
	MultiCAFE 			parent0 				= null;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		add( GuiUtil.besidesPanel(directionComboBox, detectTopTextField, transformForLevelsComboBox, displayTransform1Button )); 
		add( GuiUtil.besidesPanel(new JLabel("span ", SwingConstants.RIGHT), spanTopTextField, new JLabel(" "), new JLabel(" ")));
		add( GuiUtil.besidesPanel( detectTopButton,  detectAllLevelCheckBox, new JLabel(" ")));
		
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
				options.detectAllLevel 			= detectAllLevelCheckBox.isSelected();
				options.firstkymo 				= parent0.capillariesPane.optionsTab.kymographNamesComboBox.getSelectedIndex();

				MCBuildDetect_Limits detect = new MCBuildDetect_Limits();
				detect.detectCapillaryLevels(options, parent0.kymographArrayList);
				firePropertyChange("KYMO_DETECT_TOP", false, true);
			}});		
		displayTransform1Button.addActionListener(new ActionListener () { 
			@Override public void actionPerformed( final ActionEvent e ) { 
				kymosDisplayFiltered1();
				firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
			}});
	}
	
	// -------------------------------------------------
	
	double getDetectLevelThreshold() {
		double detectLevelThreshold = 0;
		try { detectLevelThreshold =  Double.parseDouble( detectTopTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the top threshold value."); }
		return detectLevelThreshold;
	}

	void setDetectLevelThreshold (double threshold) {
		detectTopTextField.setText(Double.toString(threshold));
	}
	
	int getSpanDiffTop() {
		int spanDiffTop = 0;
		try { spanDiffTop = Integer.parseInt( spanTopTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze step value."); }
		return spanDiffTop;
	}
		
	void kymosDisplayFiltered1() {
		if (parent0.kymographArrayList == null)
			return;
		Collections.sort(parent0.kymographArrayList, new MulticafeTools.SequenceNameComparator()); 
		TransformOp transform;
		transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		parent0.kymographsPane.kymosBuildFiltered(0, 1, transform, getSpanDiffTop());
	}
	
	void setInfos(SequencePlus seq) {
		transformForLevelsComboBox.setSelectedItem(seq.transformForLevels);
		int index =seq.directionUp ? 0:1;
		directionComboBox.setSelectedIndex(index);
		setDetectLevelThreshold(seq.detectLevelThreshold);
		detectTopTextField.setText(Integer.toString(seq.detectLevelThreshold));
		detectAllLevelCheckBox.setSelected(seq.detectAllLevel);
	}
}
