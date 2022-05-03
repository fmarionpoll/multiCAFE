package plugins.fmp.multicafe2.tools.Overlay;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.sequence.SequenceEvent.SequenceEventType;
import plugins.fmp.multicafe2.experiment.SequenceCamData;
import plugins.fmp.multicafe2.tools.ImageTransformations.EnumImageTransformations;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformInterface;
import plugins.fmp.multicafe2.tools.ImageTransformations.ImageTransformOptions;
import icy.sequence.SequenceListener;


public class OverlayThreshold extends Overlay implements SequenceListener 
{
	private float 					opacity 				= 0.3f;
	private OverlayColorMask		map 					= new OverlayColorMask ("", new Color(0x00FF0000, true));
	private ImageTransformOptions 	imageTransformOptions 	= new ImageTransformOptions();
	private ImageTransformInterface imageTransformFunction 	= EnumImageTransformations.NONE.getFunction();
	private ImageTransformInterface imageThresholdFunction 	= EnumImageTransformations.NONE.getFunction();
	private Sequence localSeq = null;
	
	// ---------------------------------------------
	
	public OverlayThreshold() 
	{
		super("ThresholdOverlay");	
	}
	
	public OverlayThreshold(SequenceCamData seqCamData) 
	{
		super("ThresholdOverlay");
		setSequence(seqCamData);
	}
	
	public void setSequence (SequenceCamData seqCamData) 
	{
		localSeq = seqCamData.seq;
		localSeq.addListener(this);
		imageTransformOptions.seqCamData = seqCamData;
	}
	
	public void setThresholdSingle (int threshold, boolean ifGreater) 
	{
		setThresholdTransform (threshold, EnumImageTransformations.THRESHOLD_SINGLE, ifGreater);
	}
	
	public void setThresholdTransform (int threshold, EnumImageTransformations transformop, boolean ifGreater) 
	{
		imageTransformOptions.setSingleThreshold(threshold, ifGreater);
		imageTransformOptions.transformOption = transformop;
		imageTransformFunction = transformop.getFunction();
		imageThresholdFunction = EnumImageTransformations.THRESHOLD_SINGLE.getFunction();
	}
	
	public void setThresholdColor (ArrayList <Color> colorarray, int distancetype, int threshold) 
	{
		imageTransformOptions.setColorArrayThreshold(distancetype, threshold, colorarray);
		imageTransformFunction = EnumImageTransformations.NONE.getFunction();
		imageThresholdFunction = EnumImageTransformations.THRESHOLD_COLORS.getFunction();
	}
	
	public IcyBufferedImage getTransformedImage(int t) 
	{
		IcyBufferedImage img = localSeq.getImage(t, 0);
		return imageThresholdFunction.run(
						imageTransformFunction.run(img, imageTransformOptions),
						imageTransformOptions);
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) 
	{
		if ((canvas instanceof IcyCanvas2D) && g != null) 
		{
			int posT = canvas.getPositionT();
			IcyBufferedImage thresholdedImage = getTransformedImage(posT);
			if (thresholdedImage != null) 
			{
				thresholdedImage.setColorMap(0, map);
				BufferedImage bufferedImage = IcyBufferedImageUtil.getARGBImage(thresholdedImage);
				Composite bck = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
				g.drawImage(bufferedImage, 0, 0, null);
				g.setComposite(bck);			
			}
		}
	}

	@Override
	public void sequenceChanged(SequenceEvent sequenceEvent) 
	{
		if (sequenceEvent.getSourceType() != SequenceEventSourceType.SEQUENCE_OVERLAY) 
			return;
        if (sequenceEvent.getSource() == this && sequenceEvent.getType() == SequenceEventType.REMOVED) {
            sequenceEvent.getSequence().removeListener(this);
            remove();
        }
	}

	@Override
	public void sequenceClosed(Sequence sequence) 
	{
		sequence.removeListener(this);
        remove();
	}

}


