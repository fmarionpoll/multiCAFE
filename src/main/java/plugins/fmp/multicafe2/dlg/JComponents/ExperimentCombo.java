package plugins.fmp.multicafe2.dlg.JComponents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JComboBox;

import icy.gui.frame.progress.ProgressFrame;
import icy.system.SystemUtil;
import icy.system.thread.Processor;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.tools.Comparators;
import plugins.fmp.multicafe2.tools.toExcel.EnumXLSColumnHeader;
import plugins.fmp.multicafe2.tools.toExcel.XLSExportOptions;

public class ExperimentCombo extends JComboBox<Experiment>
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public 	int 	index0 						= 0;
	public 	int 	index1 						= 0;
	public	int		maxSizeOfCapillaryArrays 	= 0;
	public 	String 	expListBinSubDirectory 		= null; 
	

	public ExperimentCombo () 
	{
	}
	
	@Override
	public void removeAllItems() 
	{
		super.removeAllItems();
		expListBinSubDirectory = null;
	}
	
	public Experiment getMsColStartAndEndFromAllExperiments(XLSExportOptions options) 
	{
		Experiment expAll = new Experiment();
		Experiment exp0 = getItemAt(0);
		if (options.fixedIntervals) 
		{
			expAll.camFirstImage_Ms = options.startAll_Ms;
			expAll.camLastImage_Ms = options.endAll_Ms;
		}
		else 
		{
			if (options.absoluteTime) 
			{
				Experiment expFirst =  exp0.getFirstChainedExperiment(options.collateSeries);
				expAll.setFileTimeImageFirst(expFirst.firstImage_FileTime);
				Experiment expLast = exp0.getLastChainedExperiment(options.collateSeries);
				expAll.setFileTimeImageLast(expLast.lastImage_FileTime);
				for (int i=0; i < getItemCount(); i++) 
				{
					Experiment exp = getItemAt(i);
					expFirst = exp.getFirstChainedExperiment(options.collateSeries);
					if (expAll.firstImage_FileTime.compareTo(expFirst.firstImage_FileTime) > 0) 
						expAll.setFileTimeImageFirst(expFirst.firstImage_FileTime);
					expLast = exp.getLastChainedExperiment(options.collateSeries);
					if (expAll.lastImage_FileTime .compareTo(expLast.lastImage_FileTime) <0)
						expAll.setFileTimeImageLast(expLast.lastImage_FileTime);
				}
				expAll.camFirstImage_Ms = expAll.firstImage_FileTime.toMillis();
				expAll.camLastImage_Ms = expAll.lastImage_FileTime.toMillis();	
			} 
			else 
			{
				expAll.camFirstImage_Ms = 0;
				expAll.camLastImage_Ms = exp0.offsetLastCol_Ms- exp0.offsetFirstCol_Ms;
				long firstOffset_Ms = 0;
				long lastOffset_Ms = 0;
				
				for (int i=0; i< getItemCount(); i++) 
				{
					Experiment exp = getItemAt(i);
					Experiment expFirst =  exp.getFirstChainedExperiment(options.collateSeries);
					firstOffset_Ms = expFirst.offsetFirstCol_Ms + expFirst.camFirstImage_Ms;
					exp.chainFirstImage_Ms = expFirst.camFirstImage_Ms + expFirst.offsetFirstCol_Ms;
					
					Experiment expLast =  exp.getLastChainedExperiment (options.collateSeries); 
					lastOffset_Ms = expLast.offsetLastCol_Ms + expLast.camFirstImage_Ms;
					
					long diff = lastOffset_Ms - firstOffset_Ms;
					if (diff < 1) 
					{
						System.out.println("Expt i=" + i + "  FileTime difference between last and first image < 1; set dt between images = 1 ms");
						diff = exp.seqCamData.seq.getSizeT();
					}
					if (expAll.camLastImage_Ms < diff) 
						expAll.camLastImage_Ms = diff;
				}
			}
		}
		return expAll;
	}
		
	public boolean loadAllExperiments(boolean loadCapillaries, boolean loadDrosoTrack) 
	{
		ProgressFrame progress = new ProgressFrame("Load experiment(s) parameters");
		int nexpts = getItemCount();
		int index = 1;
		maxSizeOfCapillaryArrays = 0;
		progress.setLength(nexpts);
		boolean flag = true;
		
	    final Processor processor = new Processor(SystemUtil.getNumberOfCPUs());
	    processor.setThreadName("buildkymo2");
	    processor.setPriority(Processor.NORM_PRIORITY);
        ArrayList<Future<?>> futuresArray = new ArrayList<Future<?>>(nexpts);
		futuresArray.clear();
		
		for (int i=0; i< getItemCount(); i++) 
		{
			final int it = i;
			futuresArray.add(processor.submit(new Runnable () 
			{
				
				@Override
				public void run() 
				{
			Experiment exp = getItemAt(it);
			progress.setMessage("Load experiment "+ index +" of "+ nexpts);
			exp.setBinSubDirectory(expListBinSubDirectory);
			if (expListBinSubDirectory == null)
				exp.checkKymosDirectory(exp.getBinSubDirectory());
//			flag &= 
			exp.openSequenceAndMeasures(loadCapillaries, loadDrosoTrack);
			if (maxSizeOfCapillaryArrays < exp.capillaries.capillariesList.size())
			{
				maxSizeOfCapillaryArrays = exp.capillaries.capillariesList.size();
				if (maxSizeOfCapillaryArrays % 2 != 0)
					maxSizeOfCapillaryArrays += 1;
			}
			progress.incPosition();
//			index++;
				}}));
		}
		waitFuturesCompletion(processor, futuresArray, progress);
		
		progress.close();
		return flag;
	}
	
	protected void waitFuturesCompletion(Processor processor, ArrayList<Future<?>> futuresArray,  ProgressFrame progressBar) 
    {  	
  		 int frame= 1;
  		 int nframes = futuresArray.size();
    	 while (!futuresArray.isEmpty())
         {
             final Future<?> f = futuresArray.get(futuresArray.size() - 1);
             if (progressBar != null)
   				 progressBar.setMessage("Analyze frame: " + (frame) + "//" + nframes);
             try
             {
                 f.get();
             }
             catch (ExecutionException e)
             {
                 System.out.println("series analysis - Warning: " + e);
             }
             catch (InterruptedException e)
             {
                 // ignore
             }
             futuresArray.remove(f);
             frame ++;
         }
   }
	
	public void chainExperimentsUsingCamIndexes(boolean collate) 
	{
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment expi = getItemAt(i);
			if (!collate) 
			{
				expi.chainToPrevious = null;
				expi.chainToNext = null;
				continue;
			}
			
			for (int j=0; j< getItemCount(); j++) 
			{
				if (i == j)
					continue;
				Experiment expj = getItemAt(j);
				if (!isSameDescriptors(expi, expj))
					continue;
				
				// same exp series: if before, insert eventually
				if (expj.camLastImage_Ms < expi.camFirstImage_Ms) 
				{
					if (expi.chainToPrevious == null)
						expi.chainToPrevious = expj;
					else if (expj.camLastImage_Ms > expi.chainToPrevious.camLastImage_Ms ) 
					{
						(expi.chainToPrevious).chainToNext = expj;
						expj.chainToPrevious = expi.chainToPrevious;
						expj.chainToNext = expi;
						expi.chainToPrevious = expj;
					}
					continue;
				}
				// same exp series: if after, insert eventually
				if (expj.camFirstImage_Ms >= expi.camLastImage_Ms) 
				{
					if (expi.chainToNext == null)
						expi.chainToNext = expj;
					else if (expj.camFirstImage_Ms < expi.chainToNext.camFirstImage_Ms ) 
					{
						(expi.chainToNext).chainToPrevious = expj;
						expj.chainToNext = (expi.chainToNext);
						expj.chainToPrevious = expi;
						expi.chainToNext = expj;
					}
					continue;
				}
				// it should never arrive here
				System.out.println("error in chaining "+ expi.getExperimentDirectory() +" with ->" + expj.getExperimentDirectory());
			}
		}
	}
	
	public void setFirstImageForAllExperiments(boolean collate)
	{
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment expi = getItemAt(i);
			Experiment expFirst = expi.getFirstChainedExperiment(collate);
			expi.chainFirstImage_Ms = expFirst.camFirstImage_Ms + expFirst.offsetFirstCol_Ms;
		}
	}
	
	public void chainExperimentsUsingKymoIndexes(boolean collate) 
	{
		for (int i=0; i< getItemCount(); i++)
		{
			Experiment expi = getItemAt(i);
			expi.chainToPrevious = null;
			expi.chainToNext = null;
		}
		if (!collate) 
			return;
		
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment expi = getItemAt(i);
			if (expi.chainToNext != null || expi.chainToPrevious != null)
				continue;
			List <Experiment> list = new ArrayList<Experiment> ();
			list.add(expi);
			
			for (int j=0; j< getItemCount(); j++) 
			{
				if (i == j)
					continue;
				Experiment expj = getItemAt(j);
				if (!isSameDescriptors(expi, expj))
					continue;
				if (expj.chainToNext != null || expj.chainToPrevious != null)
					continue;
				list.add(expj);
			}
			
			if (list.size() < 2)
				continue;
			
			Collections.sort(list, new Comparators.Experiment_Start_Comparator ());
			for (int k = 0; k < list.size(); k++) 
			{
				Experiment expk = list.get(k);
				if (k > 0)
					expk.chainToPrevious = list.get(k-1);
				if (k < (list.size() -1))	 
					expk.chainToNext = list.get(k+1);
			}
		}
	}
		
	private boolean isSameDescriptors(Experiment exp, Experiment expi) 
	{
		boolean flag = true;
		flag &= expi.getField(EnumXLSColumnHeader.EXPT) .equals(exp.getField(EnumXLSColumnHeader.EXPT)) ; 
		flag &= expi.getField(EnumXLSColumnHeader.BOXID) .equals(exp.getField(EnumXLSColumnHeader.BOXID)) ;
		flag &= expi.getField(EnumXLSColumnHeader.COMMENT1) .equals(exp.getField(EnumXLSColumnHeader.COMMENT1));
		flag &= expi.getField(EnumXLSColumnHeader.COMMENT2) .equals(exp.getField(EnumXLSColumnHeader.COMMENT2));
		flag &= expi.getField(EnumXLSColumnHeader.STRAIN) .equals(exp.getField(EnumXLSColumnHeader.STRAIN));
		flag &= expi.getField(EnumXLSColumnHeader.SEX) .equals(exp.getField(EnumXLSColumnHeader.SEX));
		return flag;
	}

	public int getExperimentIndexFromExptName(String filename) 
	{
		int position = -1;
		if (filename != null) 
		{
			for (int i=0; i< getItemCount(); i++) 
			{
				if (filename.compareTo(getItemAt(i).toString()) == 0) 
				{
					position = i;
					break;
				}
			}
		}
		return position;
	}
	
	public Experiment getExperimentFromExptName(String filename) 
	{
		Experiment exp = null;
		for (int i=0; i < getItemCount(); i++) {
			String expString = getItemAt(i).toString();
			if (filename.compareTo(expString) == 0) 
			{
				exp = getItemAt(i);
				break;
			}
		}
		return exp;
	}
	
	// ---------------------
		
	public int addExperiment (Experiment exp, boolean allowDuplicates) 
	{
		String exptName = exp.toString();
		int index = getExperimentIndexFromExptName(exptName);
		if (allowDuplicates || index < 0)
		{
			addItem(exp);
			index = getExperimentIndexFromExptName(exptName);
		}
		return index;
	}
	
	public List<String> getFieldValuesFromAllExperiments(EnumXLSColumnHeader field) 
	{
		List<String> textList = new ArrayList<>();
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment exp = getItemAt(i);
			String text = exp.getField(field);
			if (!isFound (text, textList))
				textList.add(text);
		}
		return textList;
	}
	
	public void getFieldValuesToCombo(JComboBox<String> combo, EnumXLSColumnHeader header)
	{
		combo.removeAllItems();
		List<String> textList = getFieldValuesFromAllExperiments(header);
		java.util.Collections.sort(textList);
		for (String text: textList)
			combo.addItem(text);
	}

	private boolean isFound (String pattern, List<String> names) 
	{
		boolean found = false;
		if (names.size() > 0) 
		{
			for (String name: names) 
			{
				found = name.equals(pattern);
				if (found)
					break;
			}
		}
		return found;
	}
	
	public List<Experiment> getExperimentsAsList()
	{
		int nitems = getItemCount();
		List<Experiment> expList = new ArrayList<Experiment>(nitems);
		for (int i=0; i< nitems; i++) 
			expList.add(getItemAt(i));
		return expList;
	}
	
	public void setExperimentsFromList (List<Experiment> listExp)
	{
		removeAllItems();
		for (Experiment exp: listExp)
			addItem(exp);
	}

	
}
