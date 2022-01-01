package plugins.fmp.multicafe2.tools;


public enum EnumTransformOp 
{ 
	NONE("none"),
	R_RGB("R(RGB)"), G_RGB("G(RGB)"), B_RGB("B(RGB)"),  
	R2MINUS_GB ("2R-(G+B)"), G2MINUS_RB("2G-(R+B)"), B2MINUS_RG("2B-(R+G)"),
	GBMINUS_2R ("(G+B)-2R"), RBMINUS_2G("(R+B)-2G"), RGMINUS_2B("(R+G)-2B"),
	RGB_DIFFS("Sum(diffRGB)"),
	RGB ("(R+G+B)/3"),
	H_HSB ("H(HSB)"), S_HSB ("S(HSB)"), B_HSB("B(HSB)"),  
	XDIFFN("XDiffn"), YDIFFN("YDiffn"), YDIFFN2("YDiffn_1D"), XYDIFFN( "XYDiffn"), 
	REF_T0("subtract t[start]"), REF_PREVIOUS("subtract t[i-step]"), REF("subtract ref"),
	NORM_BRMINUSG("F. Rebaudo"),
	COLORARRAY1("color array"), RGB_TO_HSV("HSV"), RGB_TO_H1H2H3("H1H2H3"), 
	RTOGB ("R to G,B"),
	SUBFIRSTCOL("col-col0"),
	COLORDIFF_L1_Y("color L1diff"),
	COLORDIFF_L2_Y("color L2diff");
	
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
