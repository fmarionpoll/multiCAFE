package plugins.fmp.multicafe2.dlg.JComponents;

import javax.swing.table.AbstractTableModel;
import plugins.fmp.multicafe2.experiment.Capillaries;
import plugins.fmp.multicafe2.experiment.CapillariesWithTime;
import plugins.fmp.multicafe2.experiment.Experiment;



public class CapillariesWithTimeTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ExperimentCombo expList 	= null;
	String columnNames[] = { "Selected", "Starts at" };
	
	public CapillariesWithTimeTableModel (ExperimentCombo expList) {
		super();
		this.expList = expList;
	}
	
	@Override
	public int getRowCount() {
		if (expList != null && expList.getSelectedIndex() >= 0 ) {
    		Experiment exp = (Experiment) expList.getSelectedItem();
    		Capillaries capillaries = exp.capillaries;
    		capillaries.CreateCapillariesWithTimeIfNull();
			return capillaries.capillariesWithTime.size();
    	}
        return 0;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
    public Class<?> getColumnClass(int columnIndex) {
    	switch (columnIndex) {
    	case 0: 
    		return Boolean.class;
    	case 1: 
    		return Integer.class;
        }
    	return Integer.class;
    }
	
	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		CapillariesWithTime cap = getCapillariesWithTimeAt(rowIndex);
    	if (cap != null) {
        	switch (columnIndex) {
        	case 0: return cap.selected;
            case 1: return cap.start;
        	}
    	}
    	return null;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		CapillariesWithTime cap = getCapillariesWithTimeAt(rowIndex);
		if (cap != null) {
	    	switch (columnIndex) {
	    	case 0: cap.selected = (boolean) aValue; break;
	        case 1: cap.start = (int) aValue; break; 
	        }
		}	
	}
	
	private CapillariesWithTime getCapillariesWithTimeAt(int rowIndex) {
		CapillariesWithTime cap = null;
    	if (expList != null && expList.getSelectedIndex() >=0 ) {
    		Experiment exp = (Experiment) expList.getSelectedItem();
    		cap = exp.capillaries.capillariesWithTime.get(rowIndex);
    	}
    	return cap;
	}

}
