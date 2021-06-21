package plugins.fmp.multicafe2.series;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.system.thread.Processor;
import plugins.fmp.multicafe2.dlg.JComponents.ExperimentCombo;
import plugins.fmp.multicafe2.experiment.Experiment;



public abstract class BuildSeries extends SwingWorker<Integer, Integer> 
{

	public 	Options_BuildSeries	options 		= new Options_BuildSeries();
	public	 boolean 			stopFlag 		= false;
	public 	boolean 			threadRunning 	= false;
			int 				selectedExperimentIndex = -1;
		
	
	@Override
	protected Integer doInBackground() throws Exception 
	{
		System.out.println("loop over experiments");
        threadRunning = true;
		int nbiterations = 0;
		ExperimentCombo expList = options.expList;
		ProgressFrame progress = new ProgressFrame("Analyze series");
		selectedExperimentIndex = expList.getSelectedIndex();
		expList.setSelectedIndex(-1);
		
		for (int index = expList.index0; index <= expList.index1; index++, nbiterations++) 
		{
			if (stopFlag)
				break;
			long startTimeInNs = System.nanoTime();
			
			Experiment exp = expList.getItemAt(index);
			progress.setMessage("Processing file: " + (index +1) + "//" + (expList.index1+1));
			System.out.println((index+1)+": " + exp.getExperimentDirectory());
			exp.setBinSubDirectory(options.binSubDirectory);
			boolean flag = exp.createDirectoryIfDoesNotExist(exp.getKymosBinFullDirectory());
			if (flag) 
			{
				analyzeExperiment(exp);
				long endTime2InNs = System.nanoTime();
				System.out.println("process ended - duration: "+((endTime2InNs-startTimeInNs)/ 1000000000f) + " s");
			}
			else 
			{
				System.out.println("process aborted - subdirectory not created: "+ exp.getKymosBinFullDirectory());
			}
		}		
		progress.close();
		threadRunning = false;
		expList.setSelectedIndex(selectedExperimentIndex);
		return nbiterations;
	}

	@Override
	protected void done() 
	{
		int statusMsg = 0;
		try 
		{
			statusMsg = get();
		} 
		catch (InterruptedException | ExecutionException e) 
		{
			e.printStackTrace();
		} 
		if (!threadRunning || stopFlag) 
		{
			firePropertyChange("thread_ended", null, statusMsg);
		} 
		else 
		{
			firePropertyChange("thread_done", null, statusMsg);
		}
    }
	
	abstract void analyzeExperiment(Experiment exp);
	
    protected void waitAnalyzeExperimentCompletion(Processor processor, ArrayList<Future<?>> futuresArray,  ProgressFrame progressBar) 
    {
   	 	try 
   	 	{
	   		 int frame= 1;
	   		 int nframes = futuresArray.size();
	   		 for (Future<?> future : futuresArray) 
	   		 {
	   			 if (progressBar != null)
	   				 progressBar.setMessage("Analyze frame: " + (frame) + "//" + nframes);
	   			 if (!future.isDone()) 
	   			 {
	   				 if (stopFlag) 
	   				 {
	   					 processor.shutdownNow();
	   					 break;
	   				 } else 
	   					 future.get();
	   			 }
	   			 frame += 1; 
	   		 }
        }
        catch (InterruptedException e) {
       	 	processor.shutdownNow();
        }
        catch (Exception e) 
   	 	{
       	 	throw new RuntimeException(e);
        }
   }

    protected boolean checkBoundsForCages(Experiment exp) 
	{
		boolean flag = true;
		if (options.isFrameFixed) 
		{
			exp.cages.detectFirst_Ms = options.t_firstMs;
			exp.cages.detectLast_Ms = options.t_lastMs;
			if (exp.cages.detectLast_Ms > exp.camLastImage_Ms)
				exp.cages.detectLast_Ms = exp.camLastImage_Ms;
		} 
		else 
		{
			exp.cages.detectFirst_Ms = exp.camFirstImage_Ms;
			exp.cages.detectLast_Ms = exp.camLastImage_Ms;
		}
		exp.cages.detectBin_Ms = options.t_binMs;
		exp.cages.detect_threshold = options.threshold;
		if (exp.cages.cageList.size() < 1 ) 
		{
			System.out.println("! skipped experiment with no cage: " + exp.getExperimentDirectory());
			flag = false;
		}
		return flag;
	}
    
    public IcyBufferedImage imageIORead(String name) 
	{
    	BufferedImage image = null;
		try 
		{
	    	image = ImageIO.read(new File(name));
		} 
		catch (IOException e) 
		{
			 e.printStackTrace();
		}
		return IcyBufferedImage.createFrom(image);
	}
    
}
