package plugins.fmp.multicafe2.dlg.levels;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import icy.main.Icy;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;

import plugins.nherve.maskeditor.MaskEditor;
import plugins.nherve.toolbox.image.feature.DefaultClusteringAlgorithmImpl;
import plugins.nherve.toolbox.image.feature.IcySupportRegion;
import plugins.nherve.toolbox.image.feature.SegmentableIcyBufferedImage;
import plugins.nherve.toolbox.image.feature.Signature;
import plugins.nherve.toolbox.image.feature.clustering.KMeans;
import plugins.nherve.toolbox.image.feature.descriptor.ColorPixel;
import plugins.nherve.toolbox.image.feature.descriptor.DefaultDescriptorImpl;
import plugins.nherve.toolbox.image.feature.region.GridFactory;
import plugins.nherve.toolbox.image.feature.region.SupportRegionException;
import plugins.nherve.toolbox.image.feature.signature.SignatureException;
import plugins.nherve.toolbox.image.feature.signature.VectorSignature;
import plugins.nherve.toolbox.image.mask.MaskException;
import plugins.nherve.toolbox.image.segmentation.DefaultSegmentationAlgorithm;
import plugins.nherve.toolbox.image.segmentation.Segmentation;
import plugins.nherve.toolbox.image.segmentation.SegmentationException;
import plugins.nherve.toolbox.image.toolboxes.ColorSpaceTools;
import plugins.fmp.multicafe2.MultiCAFE2;
import plugins.fmp.multicafe2.experiment.Experiment;


public class DetectLevelsK  extends JPanel 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6031521157029550040L;
	JSpinner			startSpinner			= new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
	JSpinner			endSpinner				= new JSpinner(new SpinnerNumberModel(3, 1, 1000, 1));
	private JCheckBox	allKymosCheckBox 		= new JCheckBox ("all kymographs", true);
	private String 		detectString 			= "        Detect     ";
	private JButton 	detectButton 			= new JButton(detectString);
	private JCheckBox	fromCheckBox 			= new JCheckBox (" from (pixel)", false);
	private JCheckBox 	allSeriesCheckBox 		= new JCheckBox("ALL (current to last)", false);
	private JCheckBox	leftCheckBox 			= new JCheckBox ("L", true);
	private JCheckBox	rightCheckBox 			= new JCheckBox ("R", true);
	private JButton		displayButton			= new JButton("Display");
	
	private JComboBox<String> cbColorSpace = new JComboBox<String> (new String[] {
			ColorSpaceTools.COLOR_SPACES[ColorSpaceTools.RGB],
			ColorSpaceTools.COLOR_SPACES[ColorSpaceTools.RGB_TO_HSV],
			ColorSpaceTools.COLOR_SPACES[ColorSpaceTools.RGB_TO_H1H2H3]
			});
	private JSpinner 	tfNbCluster2  = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
	private JSpinner 	tfNbIteration2 = new JSpinner(new SpinnerNumberModel(100, 1, 999, 1));
	private JSpinner 	tfStabCrit2 = new JSpinner(new SpinnerNumberModel(0.001, 0.001, 100., .1));
	private JCheckBox 	cbSendMaskDirectly = new JCheckBox("Send to editor");
	private Thread 		currentlyRunning;
	
	private MultiCAFE2 	parent0 	= null;
	
	// -----------------------------------------------------
	
	void init(GridLayout capLayout, MultiCAFE2 parent0) 
	{
		setLayout(capLayout);
		this.parent0 = parent0;
		
		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT); 
		
		JPanel panel0 = new JPanel(layoutLeft);
		((FlowLayout)panel0.getLayout()).setVgap(0);
		panel0.add(detectButton);
		panel0.add(allSeriesCheckBox);
		panel0.add(allKymosCheckBox);
		panel0.add(leftCheckBox);
		panel0.add(rightCheckBox);
		add(panel0);
		
		JPanel panel01 = new JPanel(layoutLeft);
		panel01.add (new JLabel("Color space"));
		panel01.add (cbColorSpace);
		panel01.add (new JLabel ("Clusters"));
		panel01.add (tfNbCluster2);
		panel01.add (new JLabel ("Max iterations"));
		panel01.add (tfNbIteration2);
		panel01.add(displayButton);
		
		add (panel01);
		
		JPanel panel1 = new JPanel(layoutLeft);
		panel1.add (new JLabel ("Stabilization"));
		panel1.add( tfStabCrit2);
		panel1.add(fromCheckBox);
		panel1.add(startSpinner);
		panel1.add(new JLabel("to"));
		panel1.add(endSpinner);
		add( panel1);
		
		defineActionListeners();
		currentlyRunning = null;
		cbSendMaskDirectly.setSelected(false);
	}
	
	private void defineActionListeners() 
	{	
		displayButton.addActionListener(new ActionListener () 
		{ 
			@Override public void actionPerformed( final ActionEvent e ) 
			{ 
				Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
				if (exp != null && (currentlyRunning == null)) 
				{
					runKMeans(exp);
				}
			}});
		
	}
	
	private Segmentation doClustering(Sequence currentSequence) 
			throws SupportRegionException, SegmentationException, MaskException, NumberFormatException, SignatureException 
	{
		Segmentation seg = null;
		seg = doClusteringKM(currentSequence);
		seg.reInitColors(currentSequence.getImage(0, 0));
		return seg;
	}

	private Segmentation doClusteringKM(Sequence currentSequence) 
			throws SupportRegionException, SegmentationException, MaskException, NumberFormatException, SignatureException 
	{
		int nbc2 = (int) tfNbCluster2.getValue();
		int nbi2 = (int) tfNbIteration2.getValue();
		double stab2 = (double) tfStabCrit2.getValue();
		int cs = ColorSpaceTools.RGB;
		switch(cbColorSpace.getSelectedIndex()) 
		{
		case 1:
			cs = ColorSpaceTools.RGB_TO_HSV;
			break;
		case 2:
			cs = ColorSpaceTools.RGB_TO_H1H2H3;
			break;
		default:
			cs = ColorSpaceTools.RGB;
			break;
		}

//			log("Working on " + ColorSpaceTools.COLOR_SPACES[cs]);

		SegmentableIcyBufferedImage img = new SegmentableIcyBufferedImage(currentSequence.getFirstImage());

		KMeans km2 = new KMeans(nbc2, nbi2, stab2);
		km2.setLogEnabled(false);

		Segmentation seg = null;

		DefaultDescriptorImpl<SegmentableIcyBufferedImage, ? extends Signature> col = null;

		ColorPixel cd = new ColorPixel(false);
		cd.setColorSpace(cs);
		col = cd;

		col.setLogEnabled(false);

		GridFactory factory = new GridFactory(GridFactory.ALGO_ONLY_PIXELS);
		factory.setLogEnabled(false);
		List<IcySupportRegion> lRegions = factory.extractRegions(img);
		IcySupportRegion[] regions = new IcySupportRegion[lRegions.size()];
		int r = 0;
		for (IcySupportRegion sr : lRegions) 
		{
			regions[r++] = sr;
		}

		seg = doSingleClustering(img, regions, col, km2);
		return seg;
	}
	
	private Segmentation doSingleClustering(SegmentableIcyBufferedImage img, IcySupportRegion[] regions, DefaultDescriptorImpl<SegmentableIcyBufferedImage, ? extends Signature> descriptor, DefaultClusteringAlgorithmImpl<VectorSignature> algo) throws SupportRegionException, SegmentationException 
	{
		DefaultSegmentationAlgorithm<SegmentableIcyBufferedImage> segAlgo = new DefaultSegmentationAlgorithm<SegmentableIcyBufferedImage>(descriptor, algo);
		segAlgo.setLogEnabled(false);

		Segmentation seg = segAlgo.segment(img, regions);

		return seg;
	}
	
	private void runKMeans(Experiment exp) 
	{
		displayButton.setEnabled(false);
		currentlyRunning = new Thread() 
		{
			@Override
			public void run() 
			{
				try {
					final Sequence seq = exp.seqKymos.seq;
					final Segmentation segmentation = doClustering(seq);
					if (cbSendMaskDirectly.isSelected()) 
					{
						final MaskEditor maskEditorPlugin = MaskEditor.getRunningInstance(true);
						currentlyRunning = null;
						Runnable r = new Runnable() 
						{
							@Override
							public void run() 
							{
								maskEditorPlugin.setSegmentationForSequence(seq, segmentation);
								maskEditorPlugin.switchOpacityOn();
								displayButton.setEnabled(true);
							}
						};
						SwingUtilities.invokeAndWait(r);
					} 
					else 
					{
						SwimmingObject result = new SwimmingObject(segmentation);
						Icy.getMainInterface().getSwimmingPool().add(result);
						currentlyRunning = null;
						Runnable r = new Runnable() 
						{
							@Override
							public void run() 
							{
								displayButton.setEnabled(true);
							}
						};
						SwingUtilities.invokeAndWait(r);
					}
				} catch (SupportRegionException e1) {
					System.out.println(e1.getClass().getName() + " : " + e1.getMessage());
				} catch (SegmentationException e1) {
					System.out.println(e1.getClass().getName() + " : " + e1.getMessage());
				} catch (InterruptedException e1) {
					System.out.println(e1.getClass().getName() + " : " + e1.getMessage());
				} catch (InvocationTargetException e1) {
					System.out.println(e1.getClass().getName() + " : " + e1.getMessage());
				} catch (MaskException e1) {
					System.out.println(e1.getClass().getName() + " : " + e1.getMessage());
				} catch (NumberFormatException e) {
					System.out.println(e.getClass().getName() + " : " + e.getMessage());
				} catch (SignatureException e) {
					System.out.println(e.getClass().getName() + " : " + e.getMessage());
				}
			}
		};
		currentlyRunning.start();
	}
}
