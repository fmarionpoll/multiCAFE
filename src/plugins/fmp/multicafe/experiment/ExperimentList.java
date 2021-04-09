package plugins.fmp.multicafe.experiment;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JComboBox;


import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe.tools.toExcel.XLSExportOptions;

public class ExperimentList extends JComboBox<Experiment>
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public 	int 	index0 						= 0;
	public 	int 	index1 						= 0;
	public	int		maxSizeOfCapillaryArrays 	= 0;
	public 	String 	expListBinSubPath 			= null; 
	


	public ExperimentList () 
	{
	}
	
	public Experiment getMsColStartAndEndFromAllExperiments(XLSExportOptions options) 
	{
		Experiment expAll = new Experiment();
		Experiment exp0 = getItemAt(0);
		if (options.absoluteTime) 
		{
			expAll.setFileTimeImageFirst(exp0.getFileTimeImageFirst(true));
			expAll.setFileTimeImageLast(exp0.getFileTimeImageLast(true));
			for (int i=0; i < getItemCount(); i++) 
			{
				Experiment exp = getItemAt(i);
				if (expAll.getFileTimeImageFirst(false).compareTo(exp.getFileTimeImageFirst(true)) > 0) 
					expAll.setFileTimeImageFirst(exp.getFileTimeImageFirst(true));
				if (expAll.getFileTimeImageLast(false) .compareTo(exp.getFileTimeImageLast(true)) <0)
					expAll.setFileTimeImageLast(exp.getFileTimeImageLast(true));
			}
			expAll.camFirstImage_Ms = expAll.getFileTimeImageFirst(false).toMillis();
			expAll.camLastImage_Ms = expAll.getFileTimeImageLast(false).toMillis();	
		} 
		else 
		{
			expAll.camFirstImage_Ms = 0;
			expAll.camLastImage_Ms = 0;
			for (int i=0; i< getItemCount(); i++) 
			{
				Experiment exp = getItemAt(i);
				if (options.collateSeries && exp.previousExperiment != null)
					continue;
				double last = exp.getFileTimeImageLast(options.collateSeries).toMillis();
				double first = exp.getFileTimeImageFirst(options.collateSeries).toMillis();
				double diff = last - first;
				if (diff < 1) 
				{
					System.out.println("error when computing FileTime difference between last and first image; set dt between images = 1 ms");
					diff = exp.seqCamData.seq.getSizeT();
				}
				if (expAll.camLastImage_Ms < diff) 
					expAll.camLastImage_Ms = (long) diff;
			}
		}
		expAll.kymoFirstCol_Ms = expAll.camFirstImage_Ms;
		expAll.kymoLastCol_Ms = expAll.camLastImage_Ms;
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
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment exp = getItemAt(i);
			progress.setMessage("Load experiment "+ index +" of "+ nexpts);
			exp.setBinSubDirectory(expListBinSubPath);
			if (expListBinSubPath == null)
				exp.checkKymosDirectory(exp.getBinSubDirectory());
			flag &= exp.openSequenceAndMeasures(loadCapillaries, loadDrosoTrack);
			if (maxSizeOfCapillaryArrays < exp.capillaries.capillariesArrayList.size())
				maxSizeOfCapillaryArrays = exp.capillaries.capillariesArrayList.size();
			progress.incPosition();
			index++;
		}
		progress.close();
		return flag;
	}
	
	public void chainExperiments(boolean collate) 
	{
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment exp = getItemAt(i);
			if (!collate) 
			{
				exp.previousExperiment = null;
				exp.nextExperiment = null;
				continue;
			}
			for (int j=0; j< getItemCount(); j++) 
			{
				Experiment expi = getItemAt(j);
				if (expi.experimentID == exp.experimentID)
					continue;
				if (!isSameDescriptors(exp, expi))
					continue;
				
				// same exp series: if before, insert eventually
				if (expi.camLastImage_Ms < exp.camFirstImage_Ms) 
				{
					if (exp.previousExperiment == null)
						exp.previousExperiment = expi;
					else if (expi.camLastImage_Ms > exp.previousExperiment.camLastImage_Ms ) 
					{
						(exp.previousExperiment).nextExperiment = expi;
						expi.previousExperiment = exp.previousExperiment;
						expi.nextExperiment = exp;
						exp.previousExperiment = expi;
					}
					continue;
				}
				// same exp series: if after, insert eventually
				if (expi.camFirstImage_Ms > exp.camLastImage_Ms) 
				{
					if (exp.nextExperiment == null)
						exp.nextExperiment = expi;
					else if (expi.camFirstImage_Ms < exp.nextExperiment.camFirstImage_Ms ) 
					{
						(exp.nextExperiment).previousExperiment = expi;
						expi.nextExperiment = (exp.nextExperiment);
						expi.previousExperiment = exp;
						exp.nextExperiment = expi;
					}
					continue;
				}
				// it should never arrive here
				System.out.println("error in chaining "+ exp.getExperimentDirectory() +" with ->" + expi.getExperimentDirectory());
			}
		}
	}
	
	private boolean isSameDescriptors(Experiment exp, Experiment expi) 
	{
		boolean flag = true;
		flag &= expi.experiment .equals(exp.experiment); 
		flag &= expi.exp_boxID .equals(exp.exp_boxID);
		flag &= expi.comment1 .equals(exp.comment1);
		flag &= expi.comment2 .equals(exp.comment2);
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
	
	public Experiment addNewExperimentToList () 
	{
		Experiment exp = new Experiment();
		addItem(exp);
		return exp;
	}
	
	public int addExperiment (Experiment exp) 
	{
		addItem(exp);
		String exptName = exp.toString();
		return getExperimentIndexFromExptName(exptName);
	}

	public Experiment addNewExperimentToList (String expDirectory) 
	{
		boolean exists = false;
		String expDirectory0 = getDirectoryName(expDirectory);
		Experiment exp = null;			
		for (int i=0; i< getItemCount(); i++) 
		{
			exp = getItemAt(i);
			if (exp.getExperimentDirectory() .equals (expDirectory0)) 
			{	
				exists = true;
				break;
			}
		}
		
		if (!exists) 
		{
			exp = new Experiment(expDirectory0);
			exp.setExperimentDirectory(expDirectory0);
			exp.setImagesDirectory(Experiment.getImagesDirectoryAsParentFromFileName(expDirectory0));
			int experimentNewID  = 0;
			for (int j=0; j< getItemCount(); j++) 
			{
				Experiment expi = getItemAt(j);
				if (expi.experimentID > experimentNewID)
					experimentNewID = expi.experimentID;
			}
			exp.experimentID = experimentNewID + 1;
			addItem(exp);
		}
		return exp;
	}
	
	private String getDirectoryName(String filename) 
	{
		File f0 = new File(filename);
		String directoryPathName = f0.getAbsolutePath();
		if (!f0.isDirectory()) 
		{
			Path path = Paths.get(directoryPathName);
			directoryPathName = path.getParent().toString();
		}
		return directoryPathName;
	}
	
}
