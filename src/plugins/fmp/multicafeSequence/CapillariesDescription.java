package plugins.fmp.multicafeSequence;

public class CapillariesDescription {
	public double 	volume 				= 5.;
	public int 		pixels 				= 5;
	public String 	sourceName 			= null;
	public long 	analysisStart 		= 0;
	public long 	analysisEnd 		= 0;
	public int 		analysisStep 		= 1;
	
	public String 	boxID				= new String("boxID");
	public String	experiment			= new String("experiment");
	public String 	comment				= new String("...");
	
	public int		grouping 			= 2;
	public String 	stimulusR			= new String("stimulusR");
	public String 	concentrationR		= new String("xmMR");
	public String 	stimulusL			= new String("stimulusL");
	public String 	concentrationL		= new String("xmML");

	public void copy (CapillariesDescription desc) {
		volume 			= desc.volume;
		pixels 			= desc.pixels;
		grouping 		= desc.grouping;
		analysisStart 	= desc.analysisStart;
		analysisEnd 	= desc.analysisEnd;
		analysisStep 	= desc.analysisStep;
		stimulusR 		= desc.stimulusR;
		stimulusL 		= desc.stimulusL;
		concentrationR 	= desc.concentrationR;
		concentrationL 	= desc.concentrationL;
		boxID 			= desc.boxID;
		experiment 		= desc.experiment;
		comment 		= desc.comment;
	}
	
	public boolean isChanged (CapillariesDescription desc) {
		boolean flag = false; 
		flag = (volume != desc.volume) || flag;
		flag = (pixels != desc.pixels) || flag;
		flag = (analysisStart != desc.analysisStart) || flag;
		flag = (analysisEnd != desc.analysisEnd) || flag;
		flag = (analysisStep != desc.analysisStep) || flag;
		flag = (stimulusR != null && !stimulusR .equals(desc.stimulusR)) || flag;
		flag = (concentrationR != null && !concentrationR .equals(desc.concentrationR)) || flag;
		flag = (stimulusL != null && !stimulusL .equals(desc.stimulusL)) || flag;
		flag = (concentrationL != null && !concentrationL .equals(desc.concentrationL)) || flag;
		flag = (boxID != null && !boxID .equals(desc.boxID)) || flag;
		flag = (experiment != null && !experiment .equals(desc.experiment)) || flag;
		flag = (comment != null && !comment .equals(desc.comment)) || flag;
		return flag;
	}
	
}
