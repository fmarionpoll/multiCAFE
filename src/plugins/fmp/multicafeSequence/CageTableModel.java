package plugins.fmp.multicafeSequence;


import javax.swing.table.AbstractTableModel;




public class CageTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3501225818220221949L;
	private ExperimentList expList 	= null;
	
	
	
	public CageTableModel (ExperimentList expList) {
		super();
		this.expList = expList;
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
    	if (expList != null && expList.currentExperimentIndex >= 0 )
    		return expList
    				.getCurrentExperiment()
    				.cages.cageList.size();
        return 0;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
    	Cage cage = null;
    	if (expList != null && expList.currentExperimentIndex >=0 ) {
    		cage = expList
    				.getCurrentExperiment()
    				.cages.cageList.get(rowIndex);
    	}
    	if (cage != null) {
        	switch (columnIndex) {
            case 0: return cage.roi.getName();
            case 1: return cage.cageNFlies;
            case 2: return cage.strCageComment;
        	}
    	}
    	return null;
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
    	switch (columnIndex) {
        case 0: 
        	return false;
        default: 
        	return true;
    	}
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Cage cage = null;
    	if (expList != null && expList.currentExperimentIndex >=0 ) {
    		cage = expList.getCurrentExperiment().cages.cageList.get(rowIndex);
    	}
    	if (cage != null) {
        	switch (columnIndex) {
            case 0: cage.roi.setName(aValue.toString()); break;
            case 1: cage.cageNFlies = (int) aValue; break;
            case 2: cage.strCageComment = aValue.toString(); break;
        	}
    	}
    }

}
