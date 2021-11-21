package plugins.fmp.multicafe2.experiment;

import java.util.ArrayList;


public class ROI2DIntervals {
	
	public ArrayList<Long[]> intervals = new ArrayList<Long[]>();
	
	public int addIfNew(Long[] interval) {		
		int iprevious = -1;
		for (int i = 0; i < intervals.size(); i++) {
			if (interval[0] == intervals.get(i)[0]) 
				return i;
			if (interval[0] < intervals.get(i)[0]) {
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
	
	public int findStartItem(long start) {
		for (int i = 0; i < intervals.size(); i++) {
			if (start == intervals.get(i)[0]) 
				return i;
		}
		return -1;
	}

	public Long[] get(int i) {
		return intervals.get(i);
	}
	
}
