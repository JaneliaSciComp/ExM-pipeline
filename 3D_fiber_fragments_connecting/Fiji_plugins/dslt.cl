__constant sampler_t MySampler = CLK_NORMALIZED_COORDS_FALSE |
							CLK_ADDRESS_CLAMP_TO_EDGE |
							CLK_FILTER_LINEAR;

__kernel void dslt_min(__read_only image3d_t in,
				   __global float* out,
				   __global int* n_out,
				   __constant float* filter,
				   int r,
				   int n,
				   float drx,
				   float dry,
				   float drz,
				   int ypitch,
				   int zpitch)
{
    const int x = get_global_id(0);
    const int y = get_global_id(1);
    const int z = get_global_id(2);
	const int id = z*zpitch + y*ypitch + x;

	float sum = 0.0f;
	
	float4 coord = (float4)(x+0.5, y+0.5, z+0.5, 0);
	float4 dstval = read_imagef(in, MySampler, coord) * filter[0];
	sum += dstval.x;
	#pragma unroll
	for(int i = 1; i <= r; i++){
		coord = (float4)(x+drx*i+0.5, y+dry*i+0.5, z+drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord) * filter[i];
		sum += dstval.x;
		coord = (float4)(x-drx*i+0.5, y-dry*i+0.5, z-drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord) * filter[i];
		sum += dstval.x;
	}

    if (out[id] > sum) {
		out[id] = sum;
		n_out[id] = n;
	}
}

__kernel void dslt_max(__read_only image3d_t in,
				   __global float* out,
				   __global int* n_out,
				   __constant float* filter,
				   int r,
				   int n,
				   float drx,
				   float dry,
				   float drz,
				   int ypitch,
				   int zpitch)
{
    const int x = get_global_id(0);
    const int y = get_global_id(1);
    const int z = get_global_id(2);
	const int id = z*zpitch + y*ypitch + x;

	float sum = 0.0f;
	
	float4 coord = (float4)(x+0.5, y+0.5, z+0.5, 0);
	float4 dstval = read_imagef(in, MySampler, coord) * filter[0];
	sum += dstval.x;
	#pragma unroll
	for(int i = 1; i <= r; i++){
		coord = (float4)(x+drx*i+0.5, y+dry*i+0.5, z+drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord) * filter[i];
		sum += dstval.x;
		coord = (float4)(x-drx*i+0.5, y-dry*i+0.5, z-drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord) * filter[i];
		sum += dstval.x;
	}

    if (out[id] < sum) {
		out[id] = sum;
		n_out[id] = n;
	}
}

__kernel void dslt_max2(__read_only image3d_t in,
				   __global float* out,
				   __global int* n_out,
				   __constant float* filter,
				   int r,
				   int n,
				   float drx,
				   float dry,
				   float drz,
				   int ypitch,
				   int zpitch)
{
    const int x = get_global_id(0);
    const int y = get_global_id(1);
    const int z = get_global_id(2);
	const int id = z*zpitch + y*ypitch + x;

	float sum1 = 0.0f;
	float sum2 = 0.0f;
	
	float4 coord = (float4)(x+0.5, y+0.5, z+0.5, 0);
	float4 dstval = read_imagef(in, MySampler, coord) * filter[0];
	sum1 += dstval.x;
	sum2 += dstval.x;
	#pragma unroll
	for(int i = 1; i <= r; i++){
		coord = (float4)(x+drx*i+0.5, y+dry*i+0.5, z+drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord) * filter[i];
		sum1 += dstval.x;
		coord = (float4)(x-drx*i+0.5, y-dry*i+0.5, z-drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord) * filter[i];
		sum2 += dstval.x;
	}

	float summax = (sum2 < sum1) ? sum2 : sum1;

    if (out[id] < summax) {
		out[id] = summax;
		n_out[id] = n;
	}
}

__kernel void dslt_l2(__read_only image3d_t in,
					  __global float* out,
					  __global int* n_in,
					  __constant float* filter,
					  __constant float* sincostable,
					  int r,
					  int knum,
					  float bx,
					  float by,
					  int ypitch,
					  int zpitch)
{
    const int x = get_global_id(0);
    const int y = get_global_id(1);
    const int z = get_global_id(2);
	const int id = z*zpitch + y*ypitch + x;
	
	const int nid = n_in[id];
	const float sinlati  = sincostable[nid*4];
	const float coslati  = sincostable[nid*4+1];
	const float sinlongi = sincostable[nid*4+2];
	const float coslongi = sincostable[nid*4+3];

	const float drx = bx*coslati*coslongi - by*sinlongi;
	const float dry = bx*coslati*sinlongi + by*coslongi;
	const float drz = -bx*sinlati;

	float sum = 0.0f;
	
	float4 coord = (float4)(x+0.5, y+0.5, z+0.5, 0);
	float4 dstval = read_imagef(in, MySampler, coord) * filter[0];
	sum += dstval.x;
	#pragma unroll
	for(int i = 1; i <= r; i++){
		coord = (float4)(x+drx*i+0.5, y+dry*i+0.5, z+drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord) * filter[i];
		sum += dstval.x;
		coord = (float4)(x-drx*i+0.5, y-dry*i+0.5, z-drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord) * filter[i];
		sum += dstval.x;
	}

   out[id] += sum / (float)knum;

}

__kernel void dslt_l2_2(__read_only image3d_t in,
					  __global float* out,
					  __global int* n_in,
					  __constant float* sincostable,
					  int r,
					  int w, int h, int d,
					  int ypitch,
					  int zpitch)
{
    const int x = get_global_id(0);
    const int y = get_global_id(1);
    const int z = get_global_id(2);
	const int id = z*zpitch + y*ypitch + x;

	const int nid = n_in[id];
	const float sinlati  = sincostable[nid*4];
	const float coslati  = sincostable[nid*4+1];
	const float sinlongi = sincostable[nid*4+2];
	const float coslongi = sincostable[nid*4+3];

	const float drx = sinlati*coslongi;
	const float dry = sinlati*sinlongi;
	const float drz = coslati;

	float maxval = 0.0f;
	int maxid = 0;
	
	float4 coord = (float4)(x+0.5, y+0.5, z+0.5, 0);
	float4 dstval = read_imagef(in, MySampler, coord);

	#pragma unroll
	for(int i = 1; i <= r; i++){
		coord = (float4)(x+drx*i+0.5, y+dry*i+0.5, z+drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord);
		bool cond = ((maxval < dstval.x) &&
					(coord.x >= 0.0) && (coord.x <= w-0.5) &&
					(coord.y >= 0.0) && (coord.y <= h-0.5) &&
					(coord.z >= 0.0) && (coord.z <= d-0.5));
		maxval =  cond ? dstval.x : maxval;
		maxid = cond ? i : maxid;
		
		coord = (float4)(x-drx*i+0.5, y-dry*i+0.5, z-drz*i+0.5, 0);
		dstval = read_imagef(in, MySampler, coord);
		cond = ((maxval < dstval.x) &&
				(coord.x >= 0.0) && (coord.x <= w-0.5) &&
				(coord.y >= 0.0) && (coord.y <= h-0.5) &&
				(coord.z >= 0.0) && (coord.z <= d-0.5));
		maxval = cond ? dstval.x : maxval;
		maxid = cond ? -i : maxid;
	}

	coord = (float4)(x+drx*maxid+0.5, y+dry*maxid+0.5, z+drz*maxid+0.5, 0);
	maxid = (int)(coord.z)*zpitch + (int)(coord.y)*ypitch + (int)(coord.x);
	dstval = read_imagef(in, MySampler, coord);

	maxid = n_in[maxid];
	float drx2 = sincostable[maxid*4]*sincostable[maxid*4+3];
	float dry2 = sincostable[maxid*4]*sincostable[maxid*4+2];
	float drz2 = sincostable[maxid*4+1];

	float dotv = drx*drx2+dry*dry2+drz*drz2;
    
	out[id] *= dotv*dotv;
}


__kernel void dslt_binarize(__read_only image3d_t in,
							__global float* dslt,
							__global float* out,
							float constC,
							int ypitch,
							int zpitch)
{
    const int x = get_global_id(0);
    const int y = get_global_id(1);
    const int z = get_global_id(2);
	const int id = z*zpitch + y*ypitch + x;

	float4 coord = (float4)(x+0.5, y+0.5, z+0.5, 0);
	float4 srcval = read_imagef(in, MySampler, coord);
	
	out[id] = (srcval.x > dslt[id] + constC) ? 1.0 : 0.0;
}

__kernel void dslt_elem_min(__global float* in,
							__global float* out,
							int ypitch,
							int zpitch)
{
    const int x = get_global_id(0);
    const int y = get_global_id(1);
    const int z = get_global_id(2);
	const int id = z*zpitch + y*ypitch + x;

	out[id] = (out[id] > in[id]) ? in[id] : out[id];
}

__kernel void label_init(__global int* in,
						 __global int* out,
						 int ypitch,
						 int zpitch)
{
	const int x = get_global_id(0);
    const int y = get_global_id(1);
    const int z = get_global_id(2);
	const int id = z*zpitch + y*ypitch + x;
	
	out[id] = id * in[id];
}

__kernel void swap(__global int* in,
				   __global int* out,
				   int ypitch,
				   int zpitch)
{
	const int x = get_global_id(0);
    const int y = get_global_id(1);
    const int z = get_global_id(2);
	const int id = z*zpitch + y*ypitch + x;
	
	int tmp = out[id];
	out[id] = in[id];
	in[id] = tmp;
}

__kernel void label_max(__global int* in,
						__global int* out,
						int ypitch,
						int zpitch)
{
    const int x = get_global_id(0)+1;
    const int y = get_global_id(1)+1;
    const int z = get_global_id(2)+1;
	const int id = z*zpitch + y*ypitch + x;

	int center = in[id];
	int val = center;
	if (center > 0) {
	int neighbor = in[id - zpitch - ypitch - 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - zpitch - ypitch];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - zpitch - ypitch + 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - zpitch          - 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - zpitch             ];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - zpitch          + 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - zpitch + ypitch - 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - zpitch + ypitch    ];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - zpitch + ypitch + 1];
	val = val < neighbor ? neighbor : val;

	neighbor = in[id - ypitch - 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - ypitch    ];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id - ypitch + 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id          - 1];
	val = val < neighbor ? neighbor : val;
	
	neighbor = in[id          + 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + ypitch - 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + ypitch    ];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + ypitch + 1];
	val = val < neighbor ? neighbor : val;

	neighbor = in[id + zpitch - ypitch - 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + zpitch - ypitch];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + zpitch - ypitch + 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + zpitch          - 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + zpitch             ];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + zpitch          + 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + zpitch + ypitch - 1];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + zpitch + ypitch    ];
	val = val < neighbor ? neighbor : val;
	neighbor = in[id + zpitch + ypitch + 1];
	val = val < neighbor ? neighbor : val;
	}
	out[id] = center > 0 ? val : 0;
}

__kernel void label_dif(__global int* in,
						__global int* out,
						int ypitch,
						int zpitch)
{
    const int x = get_global_id(0)+1;
    const int y = get_global_id(1)+1;
    const int z = get_global_id(2)+1;
	const int id = z*zpitch + y*ypitch + x;

	int center = in[id];
	int val = 0;
	if (center > 0) {
	int neighbor = in[id - zpitch - ypitch - 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - zpitch - ypitch];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - zpitch - ypitch + 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - zpitch          - 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - zpitch             ];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - zpitch          + 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - zpitch + ypitch - 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - zpitch + ypitch    ];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - zpitch + ypitch + 1];
	val = val | ((center != neighbor) & (neighbor > 0));

	neighbor = in[id - ypitch - 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - ypitch    ];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id - ypitch + 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id          - 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	
	neighbor = in[id          + 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + ypitch - 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + ypitch    ];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + ypitch + 1];
	val = val | ((center != neighbor) & (neighbor > 0));

	neighbor = in[id + zpitch - ypitch - 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + zpitch - ypitch];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + zpitch - ypitch + 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + zpitch          - 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + zpitch             ];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + zpitch          + 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + zpitch + ypitch - 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + zpitch + ypitch    ];
	val = val | ((center != neighbor) & (neighbor > 0));
	neighbor = in[id + zpitch + ypitch + 1];
	val = val | ((center != neighbor) & (neighbor > 0));
	}
	out[id] = val ? center : 0;
}