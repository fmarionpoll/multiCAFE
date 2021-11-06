package plugins.fmp.multicafe2.experiment;

import java.util.List;

public class CapillariesWithTime {
	public List <Capillary>	capillariesList	= null;	// the capillary (source)
	public long 			start			= 0;
	public long				end				= -1;
	public boolean 			selected		= false;
	
	public CapillariesWithTime(List <Capillary>	capillariesList) {
		this.capillariesList = capillariesList;
	}
	
	public boolean IsIntervalWithinLimits(long index) {
		if (start < 0)
			return true;
		if (index > end || index < start)
			return false;
		else
			return true;
	}
}
