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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getValueAt(int arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}

}
