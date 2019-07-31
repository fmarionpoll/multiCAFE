package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.fmp.multicafeTools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DLine;

public class MCKymosTab_DetectGulps extends JPanel {

	/**
	 * 
	 */
	private static final long 	serialVersionUID 				= -5590697762090397890L;
	
	JCheckBox					detectAllGulpsCheckBox 			= new JCheckBox ("all", true);
	JCheckBox					viewGulpsThresholdCheckBox 		= new JCheckBox ("view threshold", true);
	
	private JButton				displayTransform2Button			= new JButton("Display");
	private JSpinner			spanTransf2Spinner				= new JSpinner(new SpinnerNumberModel(3, 0, 500, 1));
	private JSpinner 			detectGulpsThresholdSpinner		= new JSpinner(new SpinnerNumberModel(90, 0, 500, 1));
	private JButton 			detectGulpsButton 				= new JButton("Detect");
	JComboBox<TransformOp> 		transformForGulpsComboBox 		= new JComboBox<TransformOp> (new TransformOp[] {TransformOp.XDIFFN /*, TransformOp.YDIFFN, TransformOp.XYDIFFN	*/});
	private ROI2DLine			roiRefLineUpper 				= new ROI2DLine ();

	private MultiCAFE 			parent0;
	
	
	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		add( GuiUtil.besidesPanel( new JLabel("threshold ", SwingConstants.RIGHT), detectGulpsThresholdSpinner, transformForGulpsComboBox, displayTransform2Button));
		add( GuiUtil.besidesPanel( new JLabel(" "), viewGulpsThresholdCheckBox, new JLabel("span ", SwingConstants.RIGHT), spanTransf2Spinner));
		add( GuiUtil.besidesPanel( detectGulpsButton, detectAllGulpsCheckBox, new JLabel(" ") ));

		transformForGulpsComboBox.setSelectedItem(TransformOp.XDIFFN);
		defineActionListeners();
	}
	
	private void defineActionListeners() {

		transformForGulpsComboBox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
				kymosDisplayFiltered2();
			}});
		
		detectGulpsButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
				kymosDisplayFiltered2();
				
				MCBuildDetect_GulpsOptions options = new MCBuildDetect_GulpsOptions();
				options.detectGulpsThreshold 	= (int) detectGulpsThresholdSpinner.getValue();
				options.transformForGulps 		= (TransformOp) transformForGulpsComboBox.getSelectedItem();
				options.detectAllGulps 			= detectAllGulpsCheckBox.isSelected();
				options.firstkymo 				= parent0.capillariesPane.optionsTab.kymographNamesComboBox.getSelectedIndex();
				
				MCBuildDetect_Gulps detect = new MCBuildDetect_Gulps();
				detect.detectGulps(options, parent0.kymographArrayList);
				firePropertyChange("KYMO_DETECT_GULP", false, true);
			}});
		displayTransform2Button.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			kymosDisplayFiltered2();
			parent0.capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
			}});
		
		viewGulpsThresholdCheckBox.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			roisDisplayrefBar(viewGulpsThresholdCheckBox.isSelected());
			}});
	}
	

	// get/set
		
	void kymosDisplayFiltered2() {
		if (parent0.kymographArrayList == null)
			return;
		Collections.sort(parent0.kymographArrayList, new MulticafeTools.SequenceNameComparator()); 
		TransformOp transform;
		transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
		parent0.kymographsPane.kymosBuildFiltered(0, 2, transform, (int) spanTransf2Spinner.getValue());
	}
	
	void setInfos(SequencePlus seq) {
		detectGulpsThresholdSpinner.setValue(seq.detectGulpsThreshold);
		transformForGulpsComboBox.setSelectedItem(seq.transformForGulps);
		detectAllGulpsCheckBox.setSelected(seq.detectAllGulps);
	}
	
	void roisDisplayrefBar(boolean display) {
		if (parent0.kymographArrayList.size() == 0)
			return;
		
		int kymo = parent0.capillariesPane.optionsTab.kymographNamesComboBox.getSelectedIndex();
		SequencePlus seq = parent0.kymographArrayList.get(kymo);
		
		if (display)
		{
			int seqheight = seq.getHeight()/2;
			double value = seqheight - (int) detectGulpsThresholdSpinner.getValue();
			Line2D refLineUpper = new Line2D.Double (0, value, seq.getWidth(), value);

			roiRefLineUpper.setLine(refLineUpper);
			roiRefLineUpper.setName("refBarUpper");
			roiRefLineUpper.setColor(Color.YELLOW);

			seq.addROI(roiRefLineUpper);
		}
		else 
		{
			seq.removeROI(roiRefLineUpper);
		}
	}

}
