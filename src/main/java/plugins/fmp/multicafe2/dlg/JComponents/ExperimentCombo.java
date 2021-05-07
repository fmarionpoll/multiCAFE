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
				if (exp.kymoFirstCol_Ms == 0 && exp.kymoLastCol_Ms == 0) 
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
		flag &= expi.getField(EnumXLSColumnHeader.EXPT) .equals(exp.getField(EnumXLSColumnHeader.EXPT)) ; 
		flag &= expi.getField(EnumXLSColumnHeader.BOXID) .equals(exp.getField(EnumXLSColumnHeader.BOXID)) ;
		flag &= expi.getField(EnumXLSColumnHeader.COMMENT1) .equals(exp.getField(EnumXLSColumnHeader.COMMENT1));
		flag &= expi.getField(EnumXLSColumnHeader.COMMENT2) .equals(exp.getField(EnumXLSColumnHeader.COMMENT2));
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
	
	public List<String> getFieldFromAllExperiments(EnumXLSColumnHeader field) 
	{
		List<String> names = new ArrayList<>();
		for (int i=0; i< getItemCount(); i++) 
		{
			Experiment exp = getItemAt(i);
			String pattern = exp.getField(field);
			if (!isFound (pattern, names))
				names.add(pattern);
		}
		return names;
	}
	
	public void getHeaderToCombo(JComboBox<String> combo, EnumXLSColumnHeader header)
	{
		combo.removeAllItems();
		List<String> fieldList = getFieldFromAllExperiments(header);
		for (String field: fieldList)
			combo.addItem(field);
	}

	private boolean isFound (String pattern, List<String> names) 
	{
		boolean found = false;
		for (String name: names) 
		{
			found = name.equalsIgnoreCase(pattern);
			if (found)
				break;
		}
		return found;
	}
	
	public List<Experiment> getExperimentsAsList()
	{
		List<Experiment> expList = new ArrayList<Experiment>(getItemCount());
		for (int i=0; i< getItemCount(); i++) 
		{
			expList.add(getItemAt(i));
		}
		return expList;
	}
	
	public void setExperimentsFromList (List<Experiment> listExp)
	{
		removeAllItems();
		for (Experiment exp: listExp)
			addItem(exp);
	}

	
}
