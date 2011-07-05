/**
EPGPU: Expressive Programming for Graphics Processing Units

This is the implementation of the big functions from epgpu.h.
See that header file for complete comments, etc.

Dr. Orion Lawlor, lawlor@alaska.edu, 2011-04-30 (Public Domain)
*/
#include <stdio.h>
#include <stdlib.h>
//#include <iostream>
#include "epgpu.h"

/***************** OpenCL utility code ************/

GPU_ADD_STATIC_CODE(ocdSOURCECODE(
/* From http://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN */
/* Make one bit of space between each bit of this integer */
inline unsigned int make_1space(unsigned int i) {
	i = (i | (i << 8)) & 0x00FF00FF;
	i = (i | (i << 4)) & 0x0F0F0F0F;
	i = (i | (i << 2)) & 0x33333333;
	i = (i | (i << 1)) & 0x55555555;
	return i;
}
/* Make two bits of space between each bit of this integer (3D morton order) 
  Explained here: http://stackoverflow.com/questions/1024754/how-to-compute-a-3d-morton-number-interleave-the-bits-of-3-ints
*/
inline unsigned int make_2spaces(unsigned int i) {
	i = (i | (i << 16)) & 0xFF0000FF;
	i = (i | (i << 8))  & 0x0F00F00F;
	i = (i | (i << 4))  & 0xc30c30c3;
	i = (i | (i << 2))  & 0x49249249;
	return i;
}
/* Interleave the bits of these two integers (convert to 2D Morton order) */
inline unsigned int interleave2D(unsigned int x,unsigned int y) {
	return make_1space(x) | (make_1space(y) << 1);
}
/* Interleave the bits of these three integers (convert to 3D Morton order) */
inline unsigned int interleave3D(unsigned int x,unsigned int y,unsigned int z) {
	return make_2spaces(x) | (make_2spaces(y) << 1) | (make_2spaces(z)<<2);
}
),bit_interleave_code);





/********************** ocd *****************/
/** Print an error message, and exit. This is our error handling strategy. */
void ocdErrDie(int err,const char *code,const char *file,int line)
{
	fprintf(stderr,"Fatal OpenCL Error at %s:%d.\n"
		" Return value %d from '%s'.  Exiting.\n",
			file,line,
			err,code);
	int ret;
#ifdef _WIN32
	ret=system("pause"); /* <- mostly so Windows users can read the error */
#endif
	abort();
	exit(ret);
}


/** Set up the OpenCL environment.  Writes out the new context and queue.
   Returns the device number chosen.
*/
cl_device_id  ocdInit(cl_context *new_context,cl_command_queue *new_queue)
{
	cl_int errcode; 

	// Get the platform
	enum {MAX_PLAT=8};
	cl_platform_id platforms[MAX_PLAT];
	cl_uint num_platforms=MAX_PLAT;
	ocdErr( clGetPlatformIDs(MAX_PLAT,platforms,&num_platforms));
	//printf("Found %d total platforms\n",num_platforms);
	cl_platform_id cpPlatform=platforms[0];

	//Get all the devices
	cl_uint nDevices = 0;      // number of devices available
	cl_uint targetDevice = 0;  // default device to compute on
	cl_uint nUnits;            // number of compute units (SM's on NV GPU)

	int devtype=CL_DEVICE_TYPE_ALL; // <- or just TYPE_GPU;
	ocdErr( clGetDeviceIDs(cpPlatform, devtype, 0, NULL, &nDevices) );
	cl_device_id* devs = new cl_device_id[nDevices];
	ocdErr( clGetDeviceIDs(cpPlatform, devtype, nDevices, devs, NULL) );
	cl_device_id device = devs[targetDevice];
	delete[] devs;

	ocdErr( clGetDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS, 
		sizeof(nUnits), &nUnits, NULL) );
	//printf("Found %u compute units..\n",nComputeUnits);

/* Try to set up OpenGL interoperability.
  If you get an error -9999 here, you need to initialize OpenGL (create a window, etc) before calling any OpenCL code.
*/
#if defined(GLX_H) // Linux (you need GL/glx.h)
/* See http://oscarbg.blogspot.com/2009/11/openclopengl-linux-interop-seen-in.html */
	cl_context_properties props[] = {
		CL_GL_CONTEXT_KHR, (cl_context_properties)glXGetCurrentContext(),
		CL_GLX_DISPLAY_KHR,  (cl_context_properties)glXGetCurrentDisplay(),
		CL_CONTEXT_PLATFORM, (cl_context_properties)cpPlatform, 
		0};
#elif defined(_WINGDI_) // Windows (from wingdi.h)
	cl_context_properties props[] = {
		CL_GL_CONTEXT_KHR,(cl_context_properties)wglGetCurrentContext(),
		CL_WGL_HDC_KHR,(cl_context_properties)wglGetCurrentDC(), 
		CL_CONTEXT_PLATFORM, (cl_context_properties)cpPlatform, 
		0};
#elif defined(__APPLE__) // OS X (from OpenGL/ CGLCurrent.h, may be a better #ifdef)
	CGLContextObj kCGLContext = CGLGetCurrentContext(); 
	CGLShareGroupObj kCGLShareGroup = CGLGetShareGroup(kCGLContext);
	cl_context_properties props[] = { 
		CL_CGL_SHAREGROUP_KHR, (cl_context_properties)kCGLShareGroup, 
		CL_CONTEXT_PLATFORM, (cl_context_properties)cpPlatform, 
		0};
#else// Skip OpenGL interoperability
	cl_context_properties *props=0;
#endif

	*new_context = clCreateContext(props, 1, &device, 
		NULL, NULL, &errcode); ocdErr(errcode);

	//Create a command-queue
	*new_queue = clCreateCommandQueue(*new_context,
   		device, 0, &errcode); ocdErr(errcode);

	/* If you find this print annoying, comment it out! */
	printf("OpenCL initialized with %d compute units\n",nUnits);
	
	return device;
}

/** Create a program object to run this code */
cl_program ocdBuildProgram(cl_context clCTX,cl_device_id device,const char *clCode)
{
	cl_int errcode;
	size_t len=strlen(clCode);
	cl_program p = clCreateProgramWithSource(clCTX, 
        	1, (const char **)&clCode, 
        	&len, &errcode); ocdErr(errcode);

	errcode=clBuildProgram(p, 1,&device, 
		  NULL, // compile options (e.g., preprocessor flags)
		  NULL, NULL); // callback
	if (errcode==CL_SUCCESS) return p;
	
/* Else some sort of build error: get some info */
	printf("EPGPU> OpenCL compile error on code: '%s'\n",clCode);
	
	size_t logsize=0;
	ocdErr(clGetProgramBuildInfo(p, device, CL_PROGRAM_BUILD_LOG, 0, NULL, &logsize));
	char *build_log = new char[logsize+1];
	ocdErr(clGetProgramBuildInfo(p, device, CL_PROGRAM_BUILD_LOG, logsize, build_log, NULL));
	build_log[logsize] = 0;
	printf("EPGPU> Build log: %s\n",build_log);
	delete[] build_log;
	
	ocdErr(errcode);
	return NULL;
}
/****************** gpu_env **************/
/* This method returns the current gpu_env singleton. 
   Using a static method allows global variable initializers 
   to more safely use this.
*/
gpu_env &gpu_env::static_env(void) {
	static gpu_env storage; 
	return storage;
}

/* The gpu_env singleton initializes OpenCL on creation. */
gpu_env::gpu_env() 
	:buffer_reuse_enabled(true),
	m_all_code("/* OpenCL code generated by gpu_env library */\n"),
	m_all_compiled(0)
{
	clDevice=0; clCTX=0; clQUE=0; // set up on first compile
}

/// Hand back the data stored by this doomed buffer
void gpu_env::buffer_release(gpu_buffer *doomed) {
	if (!buffer_reuse_enabled) return;
	
//printf("	Releasing buffer ptr=%p, size %ld\n",doomed,doomed->get_byte_count());
	if (buffer_pool.size()>=max_buffers) 
	{ /* We want cyclic buffering (LRU), so delete the oldest one */
		buffer_pool[0]->deallocate();
		delete buffer_pool[0];
		buffer_pool.erase(buffer_pool.begin());
	} 
	
	gpu_buffer *p=new gpu_buffer();
	std::swap(*p,*doomed); /* swap trick destructive assignment */
	buffer_pool.push_back(p);
}

/// Try to find an existing buffer for this size
bool gpu_env::buffer_reuse(gpu_buffer *newborn) {
	if (!buffer_reuse_enabled) return false;
//printf("Trying to reuse buffer of size %ld\n",newborn->get_byte_count());
	for (unsigned int i=0;i<buffer_pool.size();i++) {
		if (buffer_pool[i]->get_byte_count()==newborn->get_byte_count()) {
			std::swap(*newborn,*buffer_pool[i]);
			delete buffer_pool[i];
			buffer_pool.erase(buffer_pool.begin()+i);
//printf("	Found one!  ptr=%p\n",newborn);
			return true;
		}
	}
	// Couldn't find that size
//printf("	Not found. Allocating.\n");
	return false;
}


/* Replace every occurrence of f with g, in src */
inline void replace_all_of(std::string &src,const std::string &f,const std::string &g){
	for (size_t cur=0;(cur=src.find(f,cur))!=std::string::npos;) {
		size_t end=cur+f.size();
		src.replace(cur,end-cur,g);
		cur=end;
	}
}

/** Apply compile-time OpenCL replacements.  Typically, these are to work around
  limitations in our macro code generation.  This could be enhanced to nearly 
  a full compiler!
*/
std::string gpu_precompile_code(std::string src) {
	size_t cur;
/* C++ "__global<foo *>" declarations become OpenCL "__global foo *" */
	std::string global="__global<";
	for (cur=0;(cur=src.find(global,cur))!=std::string::npos;cur++) {
		cur+=global.size()-1; /* address of < */
		if (src[cur]!='<') ocdErrDie(cur,src.c_str(), "Expected __global< in gpu_precompile_code.",src[cur]);
		src[cur]=' '; /* replace < with space */
		cur=src.find(">",cur);
		if (cur==std::string::npos||src[cur]!='>') 
			ocdErrDie(cur,src.c_str(), "Couldn't find matching > after __global< in gpu_precompile_code.",0);
		src[cur]=' ';
	}
	
/* FILLKERNEL "<FILLKERNEL)(" pattern is removed. */
	replace_all_of(src,  ",<__FILLKERNEL)()"   ,")");  /* no other args case */
	replace_all_of(src,   "<__FILLKERNEL)("    ,""); /* with other args case */
	
	return src;
}

void gpu_env::compile(void) {
	if (clDevice==0) clDevice=ocdInit(&clCTX,&clQUE);
	std::string fixed=gpu_precompile_code(m_all_code);
	m_all_compiled = ocdBuildProgram(clCTX, clDevice, fixed.c_str());
}

std::string gpu_env::show_code(int version) {
	if (version==0) return m_all_code;
	else if (version==1) return gpu_precompile_code(m_all_code);
	else ocdErrDie(version,"gpu_env::show_code","logic error",0);
	return "--error--";
}

gpu_env::~gpu_env() {
	clReleaseProgram(m_all_compiled); m_all_compiled=0;
	clReleaseCommandQueue(clQUE);
	clReleaseContext(clCTX);
}

/************************** gpu_buffer *****************/
const void *gpu_buffer::alloc_host=(void *)(long)7;


	/**
	  Create a new data storage buffer of this size.
	  If initial_values is NULL, it is ignored.
	  If initial_values is gpu_buffer::host_alloc, we will allocate page-locked host RAM
	  else the data at initial_values is written into the new buffer.
	*/
gpu_buffer::gpu_buffer(size_t byte_count_,const void *initial_values) 
	:env(gpu_env::static_env()), host_ptr(0), device_ptr(0), bytes(byte_count_)
{
	cl_int errcode;
	cl_mem_flags flags=CL_MEM_READ_WRITE;
	if (initial_values==alloc_host) { // Host allocation
		flags+=CL_MEM_ALLOC_HOST_PTR;
		//printf("CL_MEM_ALLOC_HOST_PTR memory for %ud bytes\n",bytes);
	} 
	else {// Normal allocation--try to skip the allocation step
		if (env.buffer_reuse(this))
			goto skip_alloc;
	}

	device_ptr = clCreateBuffer(env.clCTX, 
		flags, 
		bytes, 0, &errcode); ocdErr(errcode);

//printf("OpenCL raw memory alloc ptr=%p, size %ld\n",device_ptr,get_byte_count());

skip_alloc:
	if (initial_values!=0 && initial_values!=alloc_host) 
		write(initial_values,0,bytes);
}

/************************* gpu_kernel **********************/

/** Estimate the desired workgroup size for this kernel. */
cl_uint gpu_kernel::get_workgroupsize(size_t desired) {
	size_t sz1=256, sz2=256;
    ocdErr(clGetKernelWorkGroupInfo(get_kernel(),env.clDevice,
		CL_KERNEL_WORK_GROUP_SIZE,
		sizeof(sz1),&sz1,0));
	ocdErr(clGetDeviceInfo(env.clDevice,
		CL_DEVICE_MAX_WORK_GROUP_SIZE,
		sizeof(sz2),&sz2,0));
	/* ATI sz1<= 512 here, sz2==256.
	   nVidia sz1<=32767, but sz2<=1024. 
	   Best performance seems to be sz==256 or smaller!
	   Grr... 
	*/
	return std::min(std::min(sz1,sz2),desired);
}

/**
   Run this kernel with these 1D dimensions.
   You must already have set all the kernel arguments.
*/
void gpu_kernel::run(int size)
{
	if (dimensions!=1) ocdErrDie(dimensions,"kernel invocation on 2d array","gpu_kernel::run(array2d)",0);

	groupsz L,G;
	if (override_local[0]!=0) L=override_local;
	else { /* figure out local workgroup size automatically */
		L[0] = size; 
		cl_uint sz=get_workgroupsize();
		while (L[0]>sz) L[0]=(L[0]+1)/2;
		//std::cout<<"1D kernel "<<dest.size<<" -> local size "<<L[0]<<" (fit in "<<sz<<")\n";
	}

	/* Round up global size to multiple of local size.
	   If this isn't a multiple, driver can ignore the kernel! */
	G[0] = (size+L[0]-1)/L[0]*L[0];

	/* Call the kernel */
	ocdErr( clEnqueueNDRangeKernel(env.clQUE, 
	      get_kernel(), 1, NULL, G, L, 0, NULL, NULL) );	
}

/**
   Run this kernel with these 2D dimensions.
   You must already have set all the kernel arguments.
*/
void gpu_kernel::run(int w,int h)
{
	//if (dimensions!=2) ocdErrDie(dimensions,"kernel invocation on 2d array","gpu_kernel::run(array2d)",0);

	groupsz L,G;
	if (override_local[0]!=0) L=override_local;
	else { /* figure out local workgroup size automatically */
		L[0] = w; 
		L[1] = h;

		/* Shrink local workgroup size so it fits in the hardware. 
		   Maintain square aspect ratio, for locality. */
		cl_uint sz=get_workgroupsize();
		while (L[0]>sz) L[0]=(L[0]+1)/2;
		while (L[1]>sz) L[1]=(L[1]+1)/2;
		while (L[0]*L[1]>sz) {  // shrink the longer axis (square for locality)
			if (L[0]>2*L[1]) L[0]=(L[0]+1)/2;
			else L[1]=(L[1]+1)/2;
		}
		//std::cout<<"2D kernel "<<dest.w<<"x"<<dest.h<<" -> local size "<<L[0]<<"x"<<L[1]<<" (fit in "<<sz<<")\n";
	}

	/* Round up global size to multiple of local size.
	   If this isn't a multiple, driver can ignore the kernel! */
	G[0] = (w+L[0]-1)/L[0]*L[0];
	G[1] = (h+L[1]-1)/L[1]*L[1];

	/* Call the kernel */
	ocdErr( clEnqueueNDRangeKernel(env.clQUE, 
	      get_kernel(), 2, NULL, G, L, 0, NULL, NULL) );	
}
