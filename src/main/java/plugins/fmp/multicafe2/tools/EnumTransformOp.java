package plugins.fmp.multicafe2.tools;


public enum EnumTransformOp 
{ 
	NONE("none"), 
	REF_T0("subtract t[start]"), 
	REF_PREVIOUS("subtract t[i-step]"), 
	REF("subtract ref")
	;
	
	private String label;
	
	EnumTransformOp (String label) 
	{ 
		this.label = label; 
	}
	
	public String toString() 
	{ 
		return label; 
	}
	
	public static EnumTransformOp findByText(String abbr)
	{
	    for(EnumTransformOp v : values())
	    { 
	    	if ( v.toString().equals(abbr)) 
	    		return v;  
	    }
	    return null;
	}
}
