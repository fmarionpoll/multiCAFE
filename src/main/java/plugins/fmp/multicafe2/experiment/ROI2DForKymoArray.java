package plugins.fmp.multicafe2.experiment;

import java.util.ArrayList;


public class ROI2DForKymoArray extends ArrayList<Object> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ArrayList<Long[]> intervals = new ArrayList<Long[]>();
	
	public void addIfNew(Long[] interval) {		
		int iprevious = -1;
		for (int i= 0; i < intervals.size(); i++)
		{
			if (interval[1] == intervals.get(i).clone()[0]) 
				return;
			if (interval[1] < intervals.get(i).clone()[0])
			{
				if (iprevious < 0) 
					iprevious = 0;
				intervals.add(iprevious, interval);
				return;
			}
			iprevious ++;
		}
		intervals.add(interval);
	}
	
	public int size() {
		return intervals.size();
	}
	
}
