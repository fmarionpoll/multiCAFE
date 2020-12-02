package plugins.fmp.multicafe.workinprogress_gpu;

import java.io.IOException;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLException;
//import com.nativelibs4java.opencl.CLFloatBuffer;
import com.nativelibs4java.opencl.CLKernel;
//import com.nativelibs4java.opencl.CLMem.MapFlags;
//import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.ochafik.io.ReadText;

import icy.system.IcyHandledException;



// modeled after adufour opencl4icy

public class OpenCLWrap {
	
	private CLContext   context;
	private CLQueue     queue;
	private CLProgram   program;
	private CLKernel    kernel;
	private boolean     runnable = false;


	public String initCL() {
	    String output = "";	    
	    try {
	        context = JavaCL.createBestContext();
	        queue = context.createDefaultQueue();
	        String programFile = ReadText.readText(OpenCLWrap.class.getResourceAsStream("CLfunctions.cl"));
	        program = context.createProgram(programFile).build();
	        
	        runnable = true;
	        output = "found OpenCL drivers v. " + context.getDevices()[0].getOpenCLVersion();
	    }
	    catch (IOException e) {
	        output = "Error (OpenCL lab): unable to load the OpenCL code.";
	        e.printStackTrace();
	    }
	    catch (CLException e) {
	        output = "Error (OpenCL lab): unable to create the OpenCL context.";
	        e.printStackTrace();
	    }
	    catch (CLBuildException e) {
	        output = "Error (OpenCL lab): unable to create the OpenCL context.";
	        e.printStackTrace();
	    }
	    catch (UnsatisfiedLinkError linkError) {
	        output = "Error (OpenCL lab): OpenCL drivers not found.";
	    }
	    catch (NoClassDefFoundError e) {
	        throw new IcyHandledException("Error: couldn't load the OpenCL drivers.\n(note: on Microsoft Windows, the drivers can only be loaded once)");
	    }
	    return output;
	}

	public void execute(EnumCLFunction function) {
        if (!runnable) 
        	throw new IcyHandledException("Cannot run the plug-in. Probably because OpenCL was not found or not initialized correctly");
        
        try {
            kernel = program.createKernel(function.name());
        }
        catch (CLBuildException e) {
            System.out.print("Unable to load OpenCL function \"" + function.toString() + "\":\n" + e.getMessage());
        }
        
        switch (function) {
            case MULTIPLY2ARRAYS: {
                Multiply2Arrays.run(context, queue, kernel);
            }
            break;
            
            default:
            break;
        }
    }
	
    public void clean() {
        if (queue != null) 
        	queue.release();
        if (context != null) 
        	context.release();
    }
	

}
