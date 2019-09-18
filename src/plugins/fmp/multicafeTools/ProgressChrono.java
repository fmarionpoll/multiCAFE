package plugins.fmp.multicafeTools;

import icy.gui.frame.progress.ProgressFrame;

public class ProgressChrono extends ProgressFrame {

	private int nbframes;
	private int t0;
	long startTimeInNs;
	String descriptionString;
    	
	
	public ProgressChrono(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public void startChrono() {
		this.descriptionString = "Tracking computation";
        startTimeInNs = System.nanoTime();
		t0 =  (int) (getNanos() / 1000000000f);
	}
	
	public void initStuff(int nbframes) {
		this.nbframes = nbframes;
		setLength(nbframes);
		startChrono();
	}
	
	public void updatePositionAndTimeLeft (int currentframe) {
		int pos = (int)(100d * (double)currentframe / nbframes);
		setPosition( currentframe );
		int nbSeconds =  (int) (getNanos() / 1000000000f);
		double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
		setMessage( "Processing: " + pos + "% - Estimated time left: " + (int) timeleft + " s");
	}
	
	public void updatePosition (int currentframe) {
		int pos = (int)(100d * (double)currentframe / nbframes);
		setPosition( currentframe );
		int nbSeconds =  (int) (getNanos() / 1000000000f);
		double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
		setMessage( "Processing frame " + currentframe + " / " + nbframes + " - Estimated time left: " + (int) timeleft + " s");
	}
	
	public int getSecondsSinceStart() {
		int nbSeconds =  (int) (getNanos() / 1000000000f);
		return (nbSeconds-t0);
	}
	
	public long getNanos()
    {
        return System.nanoTime() - startTimeInNs;
    }

}
