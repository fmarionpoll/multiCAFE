package plugins.fmp.multicafe2.tools;

import icy.gui.frame.progress.ProgressFrame;

public class ProgressChrono extends ProgressFrame 
{
	private int nbframes;
	private int t0;
	long startTimeInNs;
	String descriptionString;
	String message = new String ("Processing frame ");
    	
	public ProgressChrono(String message) 
	{
		super(message);
	}

	public void startChrono() 
	{
		this.descriptionString = "Tracking computation";
        startTimeInNs = System.nanoTime();
		t0 =  (int) (getNanos() / 1000000000f);
	}
	
	public void initChrono(int nbframes) 
	{
		this.nbframes = nbframes;
		setLength(nbframes);
		startChrono();
	}
	
	public void updatePositionAndTimeLeft (int currentframe) 
	{
		int pos = (int)(100d * (double)currentframe / nbframes);
		setPosition( currentframe );
		int nbSeconds =  (int) (getNanos() / 1000000000f);
		double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
		setMessage( "Processing: " + pos + "% - Estimated time left: " + (int) timeleft + " s");
	}
	
	public void updatePosition (int currentframe) 
	{
		int pos = (int)(100d * (double)currentframe / nbframes);
		setPosition( currentframe );
		int nbSeconds =  (int) (getNanos() / 1000000000f);
		double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
		setMessage( message + currentframe + " / " + nbframes + " - Estimated time left: " + (int) timeleft + " s");
	}
	
	public int getSecondsSinceStart() 
	{
		int nbSeconds =  (int) (getNanos() / 1000000000f);
		return (nbSeconds-t0);
	}
	
	public void setMessageFirstPart(String text) {
		message = text;
	}
	
	public long getNanos() 
	{
        return System.nanoTime() - startTimeInNs;
    }

}
