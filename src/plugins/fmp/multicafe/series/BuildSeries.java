package plugins.fmp.multicafe.series;


import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import icy.gui.frame.progress.ProgressFrame;
import icy.main.Icy;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;


public abstract class BuildSeries extends SwingWorker<Integer, Integer> {

	public BuildSeries_Options 	options 		= new BuildSeries_Options();
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	
//	public VImageBufferThread bufferThread 	= null;
//	public long				analysisStart 	= 0;
//	public long 			analysisEnd		= 99999999;
//	public int 				analysisStep 	= 1;
//	public int 				currentFrame 	= 0;
//	public int				nTotalFrames 	= 0;
//	public boolean			bBufferON 		= false;
//	protected VideoImporter importer 		= null;
	
	
	
	@Override
	protected Integer doInBackground() throws Exception {
		System.out.println("start buildkymographsThread");
//		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(false);
		
        threadRunning = true;
		int nbiterations = 0;
		ExperimentList expList = options.expList;
		ProgressFrame progress = new ProgressFrame("Build kymographs");
			
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) {
			if (stopFlag)
				break;
			long startTimeInNs = System.nanoTime();
			Experiment exp = expList.getExperiment(index);
			progress.setMessage("Processing file: " + (index +1) + "//" + (expList.index1+1));
			System.out.println((index+1)+": " +exp.getExperimentFileName());
			exp.resultsSubPath = options.resultsSubPath;
			exp.getResultsDirectory();
			
			runMeasurement(exp);
			
			long endTime2InNs = System.nanoTime();
			System.out.println("process ended - duration: "+((endTime2InNs-startTimeInNs)/ 1000000000f) + " s");
		}		
		progress.close();
		threadRunning = false;
//		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(true);
		return nbiterations;
	}

	@Override
	protected void done() {
		int statusMsg = 0;
		try {
			statusMsg = get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} 
		if (!threadRunning || stopFlag) {
			firePropertyChange("thread_ended", null, statusMsg);
		} else {
			firePropertyChange("thread_done", null, statusMsg);
		}
		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(true); 
    }
	
	abstract void runMeasurement(Experiment exp);
	
	/*
	public void vImageBufferThread_START (int numberOfImageForBuffer) {
		vImageBufferThread_STOP();

		bufferThread = new VImageBufferThread(this, numberOfImageForBuffer);
		bufferThread.setName("Buffer Thread");
		bufferThread.setPriority(Thread.NORM_PRIORITY);
		bufferThread.start();
	}
	
	public void vImageBufferThread_STOP() {
		if (bufferThread != null)
		{
			bufferThread.interrupt();
			try {
				bufferThread.join();
			}
			catch (final InterruptedException e1) { e1.printStackTrace(); }
		}
		// TODO clean buffer by removing images?
	}

	public void cleanUpBufferAndRestart() {
		if (bufferThread == null)
			return;
		int depth = bufferThread.getFenetre();
		vImageBufferThread_STOP();
		for (int t = 0; t < nTotalFrames-1 ; t++) {
			removeImage(t, 0);
		}
		vImageBufferThread_START(depth);
	}
	
	public class VImageBufferThread extends Thread {

//		
//		pre-fetch files / companion to SequenceVirtual
//		

		private int fenetre = 20; // 100;
		private int span = fenetre/2;

		public VImageBufferThread() {
			bBufferON = true;
		}

		public VImageBufferThread(SequenceVirtual vseq, int depth) {
			fenetre = depth;
			span = fenetre/2 * analysisStep;
			bBufferON = true;
		}
		
		public void setFenetre (int depth) {
			fenetre = depth;
			span = fenetre/2 * analysisStep;
		}

		public int getFenetre () {
			return fenetre;
		}
		public int getStep() {
			return analysisStep;
		}

		public int getCurrentBufferLoadPercent()
		{
			int currentBufferPercent = 0;
			int frameStart = currentFrame-span; 
			int frameEnd = currentFrame + span;
			if (frameStart < 0) 
				frameStart = 0;
			if (frameEnd >= (int) nTotalFrames) 
				frameEnd = (int) nTotalFrames-1;

			float nbImage = 1;
			float nbImageLoaded = 1;
			for (int t = frameStart; t <= frameEnd; t+= analysisStep) {
				nbImage++;
				if (getImage(t, 0) != null)
					nbImageLoaded++;
			}
			currentBufferPercent = (int) (nbImageLoaded * 100f / nbImage);
			return currentBufferPercent;
		}

		@Override
		public void run()
		{
			try
			{
				while (!isInterrupted())
				{
					ThreadUtil.sleep(100);

					int frameStart 	= currentFrame - span;
					int frameEnd 	= currentFrame + span;
					if (frameStart < 0) 
						frameStart = 0;
					if (frameEnd > nTotalFrames) 
						frameEnd = nTotalFrames;
			
					// clean all images except those within the buffer 
					for (int t = 0; t < nTotalFrames-1 ; t+= analysisStep) { // t++) {
						if (t < frameStart || t > frameEnd)
							removeImage(t, 0);
						
						if (isInterrupted())
							return;
					}
					
					for (int t = frameStart; t < frameEnd ; t+= analysisStep) {	
						setVImage(t);
						if (isInterrupted())
							return;
					}
				}			
			}
			catch (final Exception e) 
			{ e.printStackTrace(); }
		}
	}
	
	public void setVImage(int t) {
		IcyBufferedImage ibuf = loadVImage(t);
		if (ibuf != null)
			super.setImage(t, 0, ibuf);
	}
	
	public IcyBufferedImage loadVImage(int t) {
		IcyBufferedImage ibufImage = super.getImage(t, 0);
		// not found : load from file
		if (ibufImage == null)
			return loadVImageFromFile (t);
		return ibufImage;
	}
	
	private IcyBufferedImage loadVImageFromFile(int t) {
		BufferedImage buf =null;
		if (status == EnumStatus.FILESTACK) {
			buf = ImageUtil.load(listFiles[t]);
			ImageUtil.waitImageReady(buf);
			if (buf == null)
				return null;
							
		}
		else if (status == EnumStatus.AVIFILE) {
			try {
				buf = importer.getImage(0, t);
			} catch (UnsupportedFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// --------------------------------
//		setImage(t, 0, buf);
		return IcyBufferedImage.createFrom(buf);
	}
	*/

}
