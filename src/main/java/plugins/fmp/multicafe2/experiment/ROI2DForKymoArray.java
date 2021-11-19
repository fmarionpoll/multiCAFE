package plugins.fmp.multicafe2.experiment;

import java.util.ArrayList;


public class ROI2DForKymoArray {
	
	public ArrayList<Long[]> intervals = new ArrayList<Long[]>();
	
	public int addIfNew(Long[] interval) {	
		
		int iprevious = -1;
		for (int i= 0; i < intervals.size(); i++) {
			if (interval[1] == intervals.get(i).clone()[0]) 
				return i;
			if (interval[1] < intervals.get(i).clone()[0]) {
				if (iprevious < 0) 
					iprevious = 0;
				intervals.add(iprevious, interval);
				return i;
			}
			iprevious ++;
		}
		intervals.add(interval);
		return intervals.size()-1;
	}
	
	public int size() {
		return intervals.size();
	}
	
}
