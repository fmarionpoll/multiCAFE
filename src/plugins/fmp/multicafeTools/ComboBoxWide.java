package plugins.fmp.multicafeTools;

import java.awt.Dimension;
import javax.swing.JComboBox;
import java.util.Vector;
import javax.swing.ComboBoxModel;



public class ComboBoxWide extends JComboBox<String> { 
	/**
	 * 
	 */
	private static final long serialVersionUID = -7489975211080267194L;
	
	public ComboBoxWide() { 
		super();		
	} 
	
	public ComboBoxWide(final Object items[]){ 
		super(); 
	} 
	
	public ComboBoxWide(Vector<?> items) { 
		super(); 
	} 
	
	public ComboBoxWide(ComboBoxModel<?> aModel) { 
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
		if (!layingOut) 
		    dim.width = Math.max(dim.width, getPreferredSize().width); 
		return dim; 
	} 
}

