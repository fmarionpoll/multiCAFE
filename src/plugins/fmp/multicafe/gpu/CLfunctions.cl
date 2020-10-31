__kernel void multiply2arrays(__global float* a, __global float* b, __global float* output)
{
	int index = get_global_id(0);
	
	output[index] = a[index] * b[index];
}
