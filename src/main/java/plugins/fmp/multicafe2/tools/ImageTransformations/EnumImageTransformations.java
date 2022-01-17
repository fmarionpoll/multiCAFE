package plugins.fmp.multicafe2.tools.ImageTransformations;


public enum EnumImageTransformations {
	R_RGB("R(RGB)", FromRtoRGB.class),
    G_RGB("G(RGB)", FromGtoRGB.class),
    B_RGB("B(RGB)", FromBtoRGB.class); 

	private Class<?> klass;
    private String label;
	
    EnumImageTransformations(String label, Class<?> klass ) 
	{ 
		this.label = label; 
		this.klass = klass;
	}
    
	public String toString() 
	{ 
		return label; 
	}
	
	public Class<?> toClass() 
	{ 
		return klass; 
	}

}
