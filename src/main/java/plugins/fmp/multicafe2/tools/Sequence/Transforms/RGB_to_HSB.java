package plugins.fmp.multicafe2.tools.Sequence.Transforms;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceCursor;
import icy.sequence.VolumetricImage;
import icy.util.OMEUtil;
import plugins.fmp.multicafe2.tools.Sequence.SequenceTransformFunction;
import plugins.fmp.multicafe2.tools.Sequence.SequenceTransformInterface;
import plugins.fmp.multicafe2.tools.Sequence.SequenceTransformOptions;

public class RGB_to_HSB extends SequenceTransformFunction implements SequenceTransformInterface
{
	int channelOut = -1;
	int zIn = 0;
	int zOut = 1;
	
	public RGB_to_HSB(int channelOut)
	{
		this.channelOut = channelOut;
	}
	
	@Override
	public void getTransformedSequence(Sequence colorSeq, SequenceTransformOptions options) 
	{
		Sequence graySeq = new Sequence(OMEUtil.createOMEXMLMetadata(colorSeq.getOMEXMLMetadata()));
        for (int t = 0; t < colorSeq.getSizeT(); t++) // T
        {
        	graySeq.addImage(t, new IcyBufferedImage(colorSeq.getSizeX(), colorSeq.getSizeY(), 1, colorSeq.getDataType_()));
        }
        
		SequenceCursor colorSeqCursor = new SequenceCursor(colorSeq);
	    SequenceCursor graySeqCursor = new SequenceCursor(graySeq);
		
	    for (int t = 0; t < colorSeq.getSizeT(); t++) // T
        {	
	    	VolumetricImage colorVol = colorSeq.getVolumetricImage(t);
	    	VolumetricImage grayVol = getTransformedImage (colorVol, options);
	    	colorSeq.addVolumetricImage(t, grayVol);
        }
	}
	
	VolumetricImage getTransformedImage(VolumetricImage colorVol, SequenceTransformOptions options) 
	{
		Sequence graySeq = new Sequence(OMEUtil.createOMEXMLMetadata(colorSeq.getOMEXMLMetadata()));
        for (int t = 0; t < colorSeq.getSizeT(); t++) // T
        {
        	graySeq.addImage(t, new IcyBufferedImage(colorSeq.getSizeX(), colorSeq.getSizeY(), 1, colorSeq.getDataType_()));
        }
        
		SequenceCursor colorSeqCursor = new SequenceCursor(colorSeq);
	    SequenceCursor graySeqCursor = new SequenceCursor(graySeq);
		
	    int k = zIn;
	    for (int t = 0; t < colorSeq.getSizeT(); t++) // T
        {	
            for (int iy = 0; iy < colorSeq.getSizeY(); iy++) // Y
            {
                for (int ix = 0; ix < colorSeq.getSizeX(); ix++) // X
                {
                    double valueSum = 0d;
                    for (int c = 0; c < colorSeq.getSizeC(); c++) // C
                    {
                        // 5. get pixel value at channel c using cursor
                        valueSum += colorSeqCursor.get(ix, iy, k, t, c);
                    }
                    // 6. Set pixel value to average of channels
                    graySeqCursor.setSafe(ix, iy, k, t, 0, valueSum / colorSeq.getSizeC());
                }
            }
        }
	}


}
