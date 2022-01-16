package plugins.fmp.multicafe2.tools.ImageTransformations;

import java.util.function.UnaryOperator;

public enum ImgFunctions {
	R_RGB(FromRtoRGB),
    G_RGB(FromGtoRGB),
    B_RGB(ImageTransformations::FromBtoRGB); 

    UnaryOperator<Integer[]> function;

    private TestFunctions(UnaryOperator<Integer[]> function) {
        this.function = function;
    }

    public Integer[] call(Integer[] input) {
        return this.function.apply(input);
    }
}
