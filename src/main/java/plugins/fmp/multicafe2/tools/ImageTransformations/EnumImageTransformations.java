package plugins.fmp.multicafe2.tools.ImageTransformations;


public enum EnumImageTransformations {
	R_RGB			("R(RGB)", 					new FromRGBtoLinearCombination(1, 0, 0)),
    G_RGB			("G(RGB)", 					new FromRGBtoLinearCombination(0, 1, 0)),
    B_RGB			("B(RGB)", 					new FromRGBtoLinearCombination(0, 0, 1)),
    R2MINUS_GB 		("2R-(G+B)", 				new FromRGBtoLinearCombination(2, -1, -1)), 
	G2MINUS_RB		("2G-(R+B)", 				new FromRGBtoLinearCombination(-1, 2, -1)), 
	B2MINUS_RG		("2B-(R+G)", 				new FromRGBtoLinearCombination(-1, -1, 2)), 
	GBMINUS_2R 		("(G+B)-2R", 				new FromRGBtoLinearCombination(-2, 1, 1)),  
	RBMINUS_2G		("(R+B)-2G", 				new FromRGBtoLinearCombination(1, -2, 1)),  
	RGMINUS_2B		("(R+G)-2B", 				new FromRGBtoLinearCombination(1, 1, -2)), 
	RGB_DIFFS		("Sum(diffRGB)", 			new FromRGBtoSumDiff()),
	RGB 			("(R+G+B)/3", 				new FromRGBtoLinearCombination(1/3, 1/3, 1/3)),
	H_HSB 			("H(HSB)", 					new FromRGBtoHSB(0)), 
	S_HSB 			("S(HSB)", 					new FromRGBtoHSB(1)), 
	B_HSB			("B(HSB)", 					new FromRGBtoHSB(2)),  
	XDIFFN			("XDiffn", 					new FromRGBtoXDiffn(3)), 
	YDIFFN			("YDiffn", 					null), 
	YDIFFN2			("YDiffn_1D", 				null), 
	XYDIFFN			( "XYDiffn", 				null), 
	REF_T0			("subtract t[start]", 		null), 
	REF_PREVIOUS	("subtract t[i-step]", 		null), 
	REF				("subtract ref", 			null),
	NORM_BRMINUSG	("F. Rebaudo", 				null),
	COLORARRAY1		("color array", 			null), 
	RGB_TO_HSV		("HSV", 					null), 
	RGB_TO_H1H2H3	("H1H2H3", 					null), 
	RTOGB 			("R to G,B", 				null),
	SUBTRACT_1RSTCOL("[t-t0]", 					null), 
	L1DIST_TO_1RSTCOL("L1[t-t0]", 				null),
	COLORDISTANCE_L1_Y("color dist L1", 		null), 
	COLORDISTANCE_L2_Y("color dist L2", 		null),
	ZIGZAG			("remove spikes", 			null),
	DERICHE			("edge detection", 			null), 
	DERICHE_COLOR	("Deriche's edges", 		null),
	MINUSHORIZAVG	("remove Hz traces", 		null); 

	private TransformImage klass;
    private String label;
	
    EnumImageTransformations(String label, TransformImage klass ) 
	{ 
		this.label = label; 
		this.klass = klass;
	}
    
	public String toString() 
	{ 
		return label; 
	}
	
	public TransformImage getFunction() 
	{ 
		return klass; 
	}

}
