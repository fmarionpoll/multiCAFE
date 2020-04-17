package plugins.fmp.multicafeSequence;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import plugins.fmp.multicafe.MultiCAFE;

public class CageTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3501225818220221949L;
	private MultiCAFE parent0 = null;
	
	public CageTableModel (MultiCAFE parent0) {
		super();
		this.parent0 = parent0;
	}
	
	
	@Override
    public int getColumnCount() {
		return 3;
	}
	
    @Override
    public Class<?> getColumnClass(int columnIndex) {
    	switch (columnIndex) {
    	case 0:
    	case 2:
    		return String.class;
    	case 1:
    		return Boolean.class;
        }
    	return String.class;
    }
    
    @Override
    public String getColumnName(int column) {
    	switch (column) {
    	case 0:	return "Cage#";
    	case 1: return "n flies";
    	case 2: return "comment";
        }
    	return "";
    }
    
    @Override
    public int getRowCount() {
    	if (parent0 != null && parent0.currentExperimentIndex >= 0 )
    		return parent0.expList.getExperiment(parent0.currentExperimentIndex).cages.cageList.size();
        return 0;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
    	List<Cage> cageList = null;
    	if (parent0 != null && parent0.currentExperimentIndex >=0 ) {
    		cageList = parent0.expList.getExperiment(parent0.currentExperimentIndex).cages.cageList;
    	}
    	if (cageList != null) {
        	switch (columnIndex) {
            case 0:
            	return cageList.get(rowIndex).cageID;
            case 1:
            	return cageList.get(rowIndex).cageNFlies;
            case 2:
            	return cageList.get(rowIndex).cageComment;
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
        Cage cage = null;
    	if (parent0 != null && parent0.currentExperimentIndex >=0 ) {
    		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex); 
    		cage = exp.cages.cageList.get(rowIndex);
    	}
    	if (cage != null) {
        	switch (columnIndex) {
            case 0:
            	cage.cageID = aValue.toString() ;
            case 1:
            	cage.cageNFlies = (int) aValue;
            case 2:
            	cage.cageComment = aValue.toString();
        	}
    	}
    }
}
