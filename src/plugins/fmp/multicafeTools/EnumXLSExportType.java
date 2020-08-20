package plugins.fmp.multicafeTools;

public enum EnumXLSExportType {
	TOPRAW ("topraw"),
	TOPLEVEL ("toplevel"),
	BOTTOMLEVEL ("bottomlevel"), 
	DERIVEDVALUES ("derivative"), 
	
	TOPLEVEL_LR ("toplevel_L+R"), 
	TOPLEVELDELTA ("topdelta"),
	TOPLEVELDELTA_LR ("topdelta_L+R"),
	
	SUMGULPS ("sumGulps"), 
	SUMGULPS_LR ("sumGulps_L+R"), 
	ISGULPS ("isGulps"),
	TTOGULP("tToGulp"),
	TTOGULP_LR("tToGulp_LR"),
	
	XYIMAGE ("xy-image"), 
	XYTOPCAGE ("xy-topcage"), 
	XYTIPCAPS ("xy-tipcaps"), 
	DISTANCE ("distance"), 
	ISALIVE ("_alive"), 
	SLEEP ("sleep");
	
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
