package plugins.fmp.multicafe2.dlg.JComponents;

import javax.swing.table.AbstractTableModel;

import plugins.fmp.multicafe2.experiment.Capillary;
import plugins.fmp.multicafe2.experiment.Experiment;


public class CapillaryTableModel extends AbstractTableModel  
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6325792669154093747L;
	private ExperimentCombo expList 	= null;
	
	
	
	public CapillaryTableModel (ExperimentCombo expList) 
	{
		super();
		this.expList = expList;
	}
	
	@Override
	public int getColumnCount() 
	{
		return 6;
	}
	
    @Override
    public Class<?> getColumnClass(int columnIndex) 
    {
    	switch (columnIndex) {
    	case 0: return String.class;
    	case 1: return Integer.class;
    	case 2: return Integer.class;
    	case 3:	return Double.class;
    	case 4: return String.class;
    	case 5: return String.class;
        }
    	return String.class;
    }
    
	@Override
	public String getColumnName(int column) 
	{
		switch (column) {
		case 0:	return "Name";
		case 1: return "cage nb";
		case 2: return "n flies";
		case 3: return "volume";
		case 4: return "stimulus";
		case 5: return "concentration";
		}
		return "";
	}
	
    @Override
    public int getRowCount() 
    {
    	if (expList != null && expList.getSelectedIndex() >= 0 ) {
    		Experiment exp = (Experiment) expList.getSelectedItem();
			return exp.capillaries.capillariesArrayList.size();
    	}
        return 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) 
    {
    	Capillary cap = null;
    	if (expList != null && expList.getSelectedIndex() >=0 ) 
    	{
    		Experiment exp = (Experiment) expList.getSelectedItem();
    		cap = exp.capillaries.capillariesArrayList.get(rowIndex);
    	}
    	if (cap != null) 
    	{
        	switch (columnIndex) 
        	{
            case 0: return cap.getRoiName();
            case 1: return cap.capCageID;
            case 2: return cap.capNFlies;
            case 3: return cap.capVolume;
            case 4: return cap.capStimulus;
            case 5: return cap.capConcentration;
        	}
    	}
    	return null;
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) 
    {
    	switch (columnIndex) 
    	{
        case 0: 
        	return false;
        default:
        	return true;
    	}
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) 
    {
    	Capillary cap = null;
    	if (expList != null && expList.getSelectedIndex() >=0 ) 
    	{
    		Experiment exp = (Experiment) expList.getSelectedItem();
    		cap = exp.capillaries.capillariesArrayList.get(rowIndex);
    	}
    	if (cap != null) 
    	{
        	switch (columnIndex) 
        	{
            case 0: cap.setRoiName(aValue.toString()); break;
            case 1: cap.capCageID = (int) aValue; break;
            case 2: cap.capNFlies = (int) aValue; break;
            case 3: cap.capVolume = (double) aValue; break;
            case 4: cap.capStimulus = aValue.toString(); break;
            case 5: cap.capConcentration = aValue.toString(); break;
        	}
    	}
    }

}
