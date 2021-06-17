package plugins.fmp.multicafe2.dlg.JComponents;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.multicafe2.experiment.Experiment;
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
			expAll.kymoFirstCol_Ms = options.startAll_Ms;
			expAll.kymoLastCol_Ms = options.endAll_Ms;
		}
		else 
		{
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
				expAll.kymoFirstCol_Ms = expAll.camFirstImage_Ms;
				expAll.kymoLastCol_Ms = expAll.camLastImage_Ms;
								
			} 
			else 
			{
				expAll.camFirstImage_Ms = 0;
				for (int i=0; i< getItemCount(); i++) 
				{
					Experiment exp = getItemAt(i);
					if (options.collateSeries && exp.previousExperiment != null)
						continue;
					if (exp.kymoFirstCol_Ms < 0 && exp.kymoLastCol_Ms < 0) 
					{
						exp.kymoFirstCol_Ms = exp.camFirstImage_Ms;
						exp.kymoLastCol_Ms = exp.camFirstImage_Ms + exp.seqKymos.imageWidthMax * exp.kymoBinCol_Ms;
					}
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
				expAll.kymoFirstCol_Ms = expAll.camFirstImage_Ms;
				expAll.kymoLastCol_Ms = expAll.camLastImage_Ms;
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
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment exp = getItemAt(i);
			progress.setMessage("Load experiment "+ index +" of "+ nexpts);
			exp.setBinSubDirectory(expListBinSubDirectory);
			if (expListBinSubDirectory == null)
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
	
	public void chainExperimentsUsingCamIndexes(boolean collate) 
	{
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment expi = getItemAt(i);
			if (!collate) 
			{
				expi.previousExperiment = null;
				expi.nextExperiment = null;
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
					if (expi.previousExperiment == null)
						expi.previousExperiment = expj;
					else if (expj.camLastImage_Ms > expi.previousExperiment.camLastImage_Ms ) 
					{
						(expi.previousExperiment).nextExperiment = expj;
						expj.previousExperiment = expi.previousExperiment;
						expj.nextExperiment = expi;
						expi.previousExperiment = expj;
					}
					continue;
				}
				// same exp series: if after, insert eventually
				if (expj.camFirstImage_Ms > expi.camLastImage_Ms) 
				{
					if (expi.nextExperiment == null)
						expi.nextExperiment = expj;
					else if (expj.camFirstImage_Ms < expi.nextExperiment.camFirstImage_Ms ) 
					{
						(expi.nextExperiment).previousExperiment = expj;
						expj.nextExperiment = (expi.nextExperiment);
						expj.previousExperiment = expi;
						expi.nextExperiment = expj;
					}
					continue;
				}
				// it should never arrive here
				System.out.println("error in chaining "+ expi.getExperimentDirectory() +" with ->" + expj.getExperimentDirectory());
			}
		}
	}
	
	public void chainExperimentsUsingKymoIndexes(boolean collate) 
	{
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment expi = getItemAt(i);
			if (!collate) 
			{
				expi.previousExperiment = null;
				expi.nextExperiment = null;
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
				if (expj.kymoLastCol_Ms < expi.kymoFirstCol_Ms) 
				{
					if (expi.previousExperiment == null)
						expi.previousExperiment = expj;
					else if (expj.kymoLastCol_Ms > expi.previousExperiment.kymoLastCol_Ms ) 
					{
						(expi.previousExperiment).nextExperiment = expj;
						expj.previousExperiment = expi.previousExperiment;
						expj.nextExperiment = expi;
						expi.previousExperiment = expj;
					}
					continue;
				}
				// same exp series: if after, insert eventually
				if (expj.kymoFirstCol_Ms > expi.kymoLastCol_Ms) 
				{
					if (expi.nextExperiment == null)
						expi.nextExperiment = expj;
					else if (expj.kymoFirstCol_Ms < expi.nextExperiment.kymoFirstCol_Ms ) 
					{
						(expi.nextExperiment).previousExperiment = expj;
						expj.nextExperiment = (expi.nextExperiment);
						expj.previousExperiment = expi;
						expi.nextExperiment = expj;
					}
					continue;
				}
				// it should never arrive here
				System.out.println("error in chaining "+ expi.getExperimentDirectory() +" with ->" + expj.getExperimentDirectory() + " using kymograph indexes");
			}
		}
	}
	
	public static Experiment getFirstChainedExperiment (Experiment exp)
	{
		Experiment expi = exp;
		while (exp.previousExperiment != null) 
			expi = exp;
		return expi;
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
