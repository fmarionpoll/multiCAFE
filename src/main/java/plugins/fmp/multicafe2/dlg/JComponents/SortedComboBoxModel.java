package plugins.fmp.multicafe2.dlg.JComponents;

import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;

public class SortedComboBoxModel extends DefaultComboBoxModel<String> {
	  /**
	 * 
	 */
private static final long serialVersionUID = 1L;

	public SortedComboBoxModel() {
	    super();
	}
	
	public SortedComboBoxModel(String[] items) {
		Arrays.sort(items);
	    int size = items.length;
	    for (int i = 0; i < size; i++) {
	    	super.addElement(items[i]);
	    }
	    setSelectedItem(items[0]);
	}

	public SortedComboBoxModel(Vector<String> items) {
		Collections.sort(items);
	    int size = items.size();
	    for (int i = 0; i < size; i++) {
	    	super.addElement(items.elementAt(i));
	    }
	    setSelectedItem(items.elementAt(0));
	}

	@Override
	public void addElement(String element) {
		insertElementAt(element, 0);
	}

	@Override
	public void insertElementAt(String element, int index) {
		int size = getSize();
		for (index = 0; index < size; index++) {
			Comparable<String> c = (Comparable<String>) getElementAt(index);
			if (
//					c == null 
//					|| 
					c.compareTo(element) > 0
					) 
				break;
		}	
		super.insertElementAt(element, index);
	}
}
