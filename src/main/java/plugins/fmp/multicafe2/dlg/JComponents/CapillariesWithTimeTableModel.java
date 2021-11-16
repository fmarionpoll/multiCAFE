package plugins.fmp.multicafe2.dlg.JComponents;

import java.util.TreeSet;

import javax.swing.table.AbstractTableModel;
import plugins.fmp.multicafe2.experiment.Capillaries;
import plugins.fmp.multicafe2.experiment.Experiment;
import plugins.fmp.multicafe2.experiment.ROI2DForKymo;



public class CapillariesWithTimeTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long 	serialVersionUID 	= 1L;
	private ExperimentCombo 	expList 			= null;
	private final String 		columnNames[] 		= { "Starting frame", "End frame" };
	private TreeSet<Long[]> capillariesIntervals = null;
	
	
	public CapillariesWithTimeTableModel (ExperimentCombo expList) {
		super();
		this.expList = expList;
	}
	
	@Override
	public int getRowCount() {
		if (expList != null && expList.getSelectedIndex() >= 0 ) {
    		Capillaries capillaries = getCapillariesOfSelectedExperiment();
    		capillariesIntervals = capillaries.getCapillariesIntervals();
			return capillariesIntervals.size();
    	}
        return 0;
	}
	
	private Capillaries getCapillariesOfSelectedExperiment() {
		Experiment exp = (Experiment) expList.getSelectedItem();
		return exp.capillaries;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
    public Class<?> getColumnClass(int columnIndex) {
    	switch (columnIndex) {
    	case 0: return Integer.class;
    	case 1: return Integer.class;
        }
    	return Integer.class;
    }
	
	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		ROI2DForKymo cap = getCapillariesOfSelectedExperiment().getROI2DForKymoAt(0, rowIndex);
    	if (cap != null) {
        	switch (columnIndex) {
        	case 0: return cap.getStart();
        	case 1: return cap.getEnd();
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
		ROI2DForKymo cap = getCapillariesOfSelectedExperiment().getROI2DForKymoAt(0, rowIndex);
    	
		if (cap != null) {
	    	switch (columnIndex) {
	    	case 0: cap.setStart ((int) aValue); break; 
	    	case 1: cap.setEnd ((int) aValue); break; 
	        }
		}	
	}
	
}
