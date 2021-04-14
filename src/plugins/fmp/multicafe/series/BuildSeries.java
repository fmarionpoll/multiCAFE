package plugins.fmp.multicafe.series;


import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;
import icy.gui.frame.progress.ProgressFrame;
import icy.main.Icy;
import icy.system.thread.Processor;
import plugins.fmp.multicafe.dlg.JComponents.ExperimentCombo;
import plugins.fmp.multicafe.experiment.Experiment;


public abstract class BuildSeries extends SwingWorker<Integer, Integer> 
{

	public Options_BuildSeries 	options 		= new Options_BuildSeries();
	public boolean 				stopFlag 		= false;
	public boolean 				threadRunning 	= false;
	int selectedExperimentIndex = -1;
		
	
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
			exp.setBinSubDirectory(options.binSubPath);
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
		Icy.getMainInterface().getMainFrame().getInspector().setVirtualMode(true);
		options.expList.setSelectedIndex(selectedExperimentIndex);
    }
	
	abstract void analyzeExperiment(Experiment exp);
	
    protected void waitAnalyzeExperimentCompletion(Processor processor, List<Future<?>> futures,  ProgressFrame progressBar) 
    {
   	 	try 
   	 	{
	   		 int frame= 1;
	   		 int nframes = futures.size();
	   		 for (Future<?> future : futures) 
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

}
