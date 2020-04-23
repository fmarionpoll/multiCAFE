package plugins.fmp.multicafeSequence;

import javax.swing.table.AbstractTableModel;

import plugins.fmp.multicafe.MultiCAFE;

public class CapillaryTableModel extends AbstractTableModel  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6325792669154093747L;
	private MultiCAFE parent0 = null;
	
	public CapillaryTableModel (MultiCAFE parent0) {
		super();
		this.parent0 = parent0;
	}
	
	@Override
	public int getColumnCount() {
		return 3;
	}
	
	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:	return "Name";
		case 1: return "stimulus";
		case 2: return "concentration";
		}
		return "";
	}
	
    @Override
    public Class<?> getColumnClass(int columnIndex) {
    	switch (columnIndex) {
    	case 0: return String.class;
    	case 1: return String.class;
    	case 2: return String.class;
        }
    	return String.class;
    }

    @Override
    public int getRowCount() {
    	if (parent0 != null && parent0.currentExperimentIndex >= 0 )
    		return parent0.expList.getExperiment(parent0.currentExperimentIndex).capillaries.capillariesArrayList.size();
        return 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
    	Capillary cap = null;
    	if (parent0 != null && parent0.currentExperimentIndex >=0 ) {
    		cap = parent0.expList.getExperiment(parent0.currentExperimentIndex).capillaries.capillariesArrayList.get(rowIndex);
    	}
    	if (cap != null) {
        	switch (columnIndex) {
            case 0: 
            	return cap.capillaryRoi.getName();
            case 1: return cap.stimulus;
            case 2: return cap.concentration;
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
    	Capillary cap = null;
    	if (parent0 != null && parent0.currentExperimentIndex >=0 ) {
    		cap = parent0.expList.getExperiment(parent0.currentExperimentIndex).capillaries.capillariesArrayList.get(rowIndex);
    	}
    	if (cap != null) {
        	switch (columnIndex) {
            case 0: cap.capillaryRoi.setName(aValue.toString()); break;
            case 1: cap.stimulus = aValue.toString(); break;
            case 2: cap.concentration = aValue.toString(); break;
        	}
    	}
    }

}
