package plugins.fmp.multicafeSequence;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListModel;

public class SequenceNameListRenderer extends DefaultListCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7571369946954820177L;

	@Override
	   public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	      	Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	      	ListModel<?> model = list.getModel();
	      	int nitems = model.getSize();
	      	if (index < 0) 
	      		index = list.getSelectedIndex();
	      		   
	      	String lead = "["+(index+1)+":"+nitems+"] ";
	      	String tvalue = (String) value;
	      	if (tvalue != null) {
		      	int nch_lead = lead.length();
		      	int nch_tvalue = tvalue.length();
		      	int max = 70;
		      	if ((nch_lead + nch_tvalue) > max) {
		      		int nchars = max - 3 - nch_lead;
		      		tvalue = "..." + tvalue.substring(nch_tvalue - nchars);
		      	}
	      	}
			setText(lead+tvalue);

			return c;
	   }
}
