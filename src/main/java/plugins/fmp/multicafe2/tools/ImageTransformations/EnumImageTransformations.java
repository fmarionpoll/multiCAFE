package plugins.fmp.multicafe2.tools.ImageTransformations;


public enum EnumImageTransformations {
	R_RGB("R(RGB)", new FromRtoRGB()),
    G_RGB("G(RGB)", new FromGtoRGB()),
    B_RGB("B(RGB)", new FromBtoRGB()); 

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
