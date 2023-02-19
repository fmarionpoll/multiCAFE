package plugins.fmp.multicafe2.tools.Sequence.Transforms;

import icy.sequence.Sequence;
import plugins.fmp.multicafe2.tools.Sequence.SequenceTransformFunction;
import plugins.fmp.multicafe2.tools.Sequence.SequenceTransformInterface;
import plugins.fmp.multicafe2.tools.Sequence.SequenceTransformOptions;

public class RGB_to_HSB extends SequenceTransformFunction implements SequenceTransformInterface
{
	int channelOut = -1;
	
	public RGB_to_HSB(int channelOut)
	{
		this.channelOut = channelOut;
	}
	
	@Override
	public void getTransformedSequence(Sequence seq, SequenceTransformOptions options) 
	{
	}


}
