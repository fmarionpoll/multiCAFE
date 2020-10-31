package plugins.fmp.multicafe.gpu;


import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLFloatBuffer;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;


public class Multiply2Arrays {
	
	public static int arraySize = 100;
	
	public static void run(CLContext context, CLQueue queue, CLKernel kernel) {
        final int ARRAY_SIZE = arraySize;
        
        float[] a = new float[ARRAY_SIZE];
        float[] b = new float[ARRAY_SIZE];
        float[] ab = new float[ARRAY_SIZE];
        
        // fill a and b with some values
        for (int i = 0; i < ARRAY_SIZE; i++)
        {
            a[i] = i;
            b[i] = i + 1;
        }
        
        long start, end;
        
        start = System.nanoTime();
        
        // input arguments should be mapped to pre-existing CL buffers
        CLFloatBuffer cl_inBuffer_a = context.createFloatBuffer(Usage.Input, ARRAY_SIZE);
        CLFloatBuffer cl_inBuffer_b = context.createFloatBuffer(Usage.Input, ARRAY_SIZE);
        
        // create a CLEvent, needed for synchronization purposes
        // CLEvent event;
        
        // map the GPU buffer to local memory
        FloatBuffer fb_a = cl_inBuffer_a.map(queue, MapFlags.Write);
        // write the local data to it
        fb_a.put(a);
        // rewind the buffer (needed on some drivers)
        fb_a.rewind();
        // release the mapping
        cl_inBuffer_a.unmap(queue, fb_a);
        
        // same for array b
        FloatBuffer fb_b = cl_inBuffer_b.map(queue, MapFlags.Write);
        fb_b.put(b);
        fb_b.rewind();
        cl_inBuffer_b.unmap(queue, fb_b);
        
        // proceed differently for the output: create first a "direct" float buffer
        FloatBuffer outBuffer = ByteBuffer.allocateDirect(ARRAY_SIZE * 4).order(context.getByteOrder()).asFloatBuffer();
        // share the reference directly with the GPU (with no copy)
        // NOTE: using this technique with copy=true for input parameters is less optimal than the
        // mapping version above
        CLFloatBuffer cl_outBuffer = context.createFloatBuffer(Usage.Output, outBuffer, false);
        
        // send the parameters to the kernel
        kernel.setArgs(cl_inBuffer_a, cl_inBuffer_b, cl_outBuffer);
        
        // run the GPU code
        kernel.enqueueNDRange(queue, new int[] { ARRAY_SIZE });
        
        // read the result
        cl_outBuffer.read(queue, outBuffer, true);
        
        // retrieve the content of the buffer into the output array
        outBuffer.get(ab);
        // rewind the output buffer (not necessary here, but ensures clean code)
        outBuffer.rewind();
        
        end = System.nanoTime();
        
        // print out first array values
        System.out.print("First values of a:  ");
        printFloatArray(a, 10);
        System.out.print("First values of b:  ");
        printFloatArray(b, 10);
        System.out.print("First values of a*b: ");
        printFloatArray(ab, 10);
        System.out.println("Computation time (OpenCL): " + (end - start) / 1000000 + " milliseconds");
    }
	
	private static void printFloatArray(float[] a, int i) {
        System.out.print("[ ");
        for (int j = 0; j < i; j++)
            System.out.print(a[j] + " ");
        System.out.println(" ]");
	}

}
