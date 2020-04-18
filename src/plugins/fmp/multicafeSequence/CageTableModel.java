package plugins.fmp.multicafeSequence;


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
    	case 0: return String.class;
    	case 1: return Integer.class;
    	case 2: return String.class;
        }
    	return String.class;
    }
    
    @Override
    public String getColumnName(int column) {
    	switch (column) {
    	case 0:	return "Name";
    	case 1: return "n flies";
    	case 2: return "Comment";
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
    	Cage cage = null;
    	if (parent0 != null && parent0.currentExperimentIndex >=0 ) {
    		cage = parent0.expList.getExperiment(parent0.currentExperimentIndex).cages.cageList.get(rowIndex);
    	}
    	if (cage != null) {
        	switch (columnIndex) {
            case 0: 
            	return cage.roi.getName();
            case 1: return cage.cageNFlies;
            case 2: return cage.cageComment;
        	}
    	}
    	return null;
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
    	switch (columnIndex) {
        case 0: 
        	return false;
        case 1: 
        case 2: 
        	return true;
   	}
    	return false;
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Cage cage = null;
    	if (parent0 != null && parent0.currentExperimentIndex >=0 ) {
    		cage = parent0.expList.getExperiment(parent0.currentExperimentIndex).cages.cageList.get(rowIndex);
    	}
    	if (cage != null) {
        	switch (columnIndex) {
            case 0: cage.roi.setName(aValue.toString()); break;
            case 1: cage.cageNFlies = (int) aValue; break;
            case 2: cage.cageComment = aValue.toString(); break;
        	}
    	}
    }

}
