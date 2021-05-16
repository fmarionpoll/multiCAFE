package plugins.fmp.multicafe2.tools.toExcel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public enum EnumXLSColumnHeader 
{
	PATH		("Path", 		0),
	DATE		("Date", 		1), 
	BOXID		("Box_ID", 		2), 
	CAM 		("Cam", 		3), 
	EXPT		("Expmt", 		4), 
	CAGEID 		("Cage_ID", 	5),
	COMMENT1	("Cmt1", 	6),
	COMMENT2	("Cmt2", 	7),
	CAP 		("Cap", 		8),
	CAPVOLUME	("Cap_ul", 		9), 
	CAPPIXELS 	("Cap_npixels", 10), 
	CAGECOMMENT	("Choice",		11),  
	CAPSTIM		("Cap_stim", 		12), 
	CAPCONC		("Cap_conc", 		13),
	NFLIES		("Nflies", 		14), 
	CAGEINDEX	("Cage", 		15), 
	DUM4		("Dum4", 		16); 
	
	
	private final String 	name;
	private final int 		value;
	
	
	EnumXLSColumnHeader (String label, int value) 
	{ 
		this.name = label;
		this.value = value;
	}
	
	public String getName() 
	{
		return name;
	}

	public int getValue() 
	{
		return value;
	}
	
	static final Map<String, EnumXLSColumnHeader> names = Arrays.stream(EnumXLSColumnHeader.values())
		      .collect(Collectors.toMap(EnumXLSColumnHeader::getName, Function.identity()));
	
	static final Map<Integer, EnumXLSColumnHeader> values = Arrays.stream(EnumXLSColumnHeader.values())
		      .collect(Collectors.toMap(EnumXLSColumnHeader::getValue, Function.identity()));
	
	public static EnumXLSColumnHeader fromName(final String name) 
	{
	    return names.get(name);
	}

	public static EnumXLSColumnHeader fromValue(final int value) 
	{
	    return values.get(value);
	}
	
	public String toString() 
	{ 
		return name; 
	}
	
	public static EnumXLSColumnHeader findByText(String abbr)
	{
	    for(EnumXLSColumnHeader v : values())
	    { 
	    	if ( v.toString().equals(abbr)) 
	    		return v;  
	    }
	    return null;
	}
}


