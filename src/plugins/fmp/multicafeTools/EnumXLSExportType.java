package plugins.fmp.multicafeTools;

public enum EnumXLSExportType {
	TOPLEVEL ("toplevel"), 
	BOTTOMLEVEL ("bottomlevel"), 
	DERIVEDVALUES ("derivative"), 
	SUMGULPS ("sumGulps"), 
	SUMGULPS_LR ("sumGulps_L+R"), 
	TOPLEVEL_LR ("toplevel_L+R"), 
	XYCENTER ("xycenter"), 
	DISTANCE ("distance"), 
	ISALIVE ("_alive"), 
	TOPLEVELDELTA ("topdelta"),
	TOPLEVELDELTA_LR ("topdelta_L+R");
	
	private String label;
	
	EnumXLSExportType (String label) { 
		this.label = label;
	}
	
	public String toString() { 
		return label;
	}
	
	public static EnumXLSExportType findByText(String abbr){
	    for(EnumXLSExportType v : values()) { 
	    	if( v.toString().equals(abbr)) { 
	    		return v; 
    		}  
    	}
	    return null;
	}
}
