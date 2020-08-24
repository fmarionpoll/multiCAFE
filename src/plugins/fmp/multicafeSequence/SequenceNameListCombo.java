package plugins.fmp.multicafeSequence;

import java.awt.Dimension;
import javax.swing.JComboBox;
import java.util.Vector;
import javax.swing.ComboBoxModel;



public class SequenceNameListCombo extends JComboBox<String> { 
	/**
	 * 
	 */
	private static final long serialVersionUID = -7489975211080267194L;
	
	public SequenceNameListCombo() { 
		super();		
	} 
	
	public SequenceNameListCombo(final Object items[]){ 
		super(); 
	} 
	
	public SequenceNameListCombo(Vector<?> items) { 
		super(); 
	} 
	
	public SequenceNameListCombo(ComboBoxModel<?> aModel) { 
		super(); 
	} 
	
	private boolean layingOut = false; 
	
	public void doLayout(){ 
		try { 
			layingOut = true; 
		    super.doLayout(); 
		} finally { 
		    layingOut = false; 
		} 
	} 
	
	public Dimension getSize(){ 
		Dimension dim = super.getSize(); 
		if (!layingOut) {
			int preferredWidth = getPreferredSize().width;
		    dim.width = Math.max(dim.width, preferredWidth);
		}
		return dim; 
	} 
}

