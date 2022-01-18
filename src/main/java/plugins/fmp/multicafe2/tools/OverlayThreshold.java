package plugins.fmp.multicafe2.tools;

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
import icy.sequence.SequenceListener;

public class OverlayThreshold extends Overlay implements SequenceListener 
{
	public ImageOperations 		imgOp 	= null;
	private float 				opacity = 0.3f;
	private OverlayColorMask	map 	= new OverlayColorMask ("", new Color(0x00FF0000, true));
	
	// ---------------------------------------------
	
	public OverlayThreshold() 
	{
		super("ThresholdOverlay");	
	}
	
	public OverlayThreshold(SequenceCamData seq) 
	{
		super("ThresholdOverlay");
		setSequence(seq);
	}
	
	public void setSequence (SequenceCamData seq) 
	{
		if (seq == null)
			return;
		if (imgOp == null)
			imgOp = new ImageOperations (seq);
		imgOp.setSequence(seq);
	}
	
	public void setTransform (EnumImageTransformations transf) 
	{
		imgOp.setTransform( transf);
	}
	
	public void setThresholdSingle (int threshold, boolean ifGreater) 
	{
		imgOp.setThresholdSingle(threshold, ifGreater);
	}
	
	public void setThresholdTransform (int threshold, EnumImageTransformations transformop, boolean ifGreater) 
	{
		imgOp.setThresholdSingle(threshold, ifGreater);
		imgOp.setTransform(transformop);
	}
	
	public void setThresholdColor (ArrayList <Color> colorarray, int distancetype, int threshold) 
	{
		imgOp.setColorArrayThreshold(colorarray, distancetype, threshold);
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) 
	{
		if ((canvas instanceof IcyCanvas2D) && g != null) 
		{
			IcyBufferedImage thresholdedImage = imgOp.runImageOperation();
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


