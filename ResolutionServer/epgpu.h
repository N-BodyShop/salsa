/**
EPGPU: Expressive Programming for Graphics Processing Units

A set of C++ wrapper macros and templates to embed OpenCL code
into your C++ application.

For OpenCL documentation, see:
http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/

Dr. Orion Lawlor, lawlor@alaska.edu, 2011-05-06 (Public Domain)
*/
#ifndef __OSL_EPGPU_CL_H
#define __OSL_EPGPU_CL_H

#include <stdio.h> /* I use printf a lot below. */
#include <string.h>
#include <math.h>

#include <algorithm> /* for std::max */
#include <string> 
#include <vector>

#if defined (__APPLE__) || defined(MACOSX)
    #include <OpenCL/opencl.h>
#else
    #include <CL/opencl.h>
#endif 


/* Define a bunch of OpenCL keywords, so they kinda-sorta work in C++. */
#ifndef EPGPU_NO_OPENCL_KEYWORDS

#define __constant const
#define __read_only const
#define __read_write const
#define __write_only /* empty */
typedef cl_mem image2d_t;

#endif


/* This is a simpler set of OpenCL wrappers *jokingly* called
     "ocd": OpenCL Developer's library.
*/

/** Print an error message, and exit. This is our error handling strategy. */
void ocdErrDie(int err,const char *code,const char *file,int line);

/** This macro is used to check OpenCL return codes.  Typical usage:
	ocdErr( clSomeFunction(way,too,many,arguments) );
or
	cl_uint err;
	clSomeOtherFunction(yadda,yadda,&err); ocdErr(err);
*/
#define ocdErr(errorcode) \
	do { int e=(errorcode);	if (CL_SUCCESS!=e) { \
		ocdErrDie(e,#errorcode,__FILE__,__LINE__); } } while (0)

/** Stringify this source code: return a quoted copy of this. */
#define ocdSOURCECODE(code) #code
/** Stringify, including macro expansions */
#define ocdEXPANDSTRING(code) ocdSOURCECODE(code)


/** Set up the OpenCL environment.  Writes out the new context and queue.
   Returns the device number chosen.
*/
cl_device_id  ocdInit(cl_context *new_context,cl_command_queue *new_queue);

/** Create a program object to run this code */
cl_program ocdBuildProgram(cl_context clCTX,cl_device_id device,const char *clCode);


/** Set this kernel argument to this value.  Avoids having to call "sizeof". */
template <typename T>
inline void ocdKernelArg(cl_kernel k,int number,T A) {
	ocdErr( clSetKernelArg(k, number, sizeof(T), (void *)&A) );
}

/********************* gpu_env ******************/

class gpu_buffer; // forward declarations
class gpu_kernel;
template <class dataType,int dimensions> class gpu_kernelT;
class gpu_any_datatype {}; // marker class

/** "Environment" for running GPU code: 
 Sets up the OpenCL device, command queue, etc.
 Accumulates all OpenCL code, to be compiled in one shot.
*/
class gpu_env {
public:
	cl_context clCTX; cl_command_queue clQUE; /* set up in constructor */ 
	cl_device_id clDevice;

/********* Code Managment ************/
	/* This is all the OpenCL code encountered so far.
	   By linking all OpenCL in the program at once, we can share
	   utility functions, struct definitions, etc.
	*/
	void add_code(const char *code) { 
		if (m_all_compiled!=0) { /* we're already compiled: don't recompile (performance penalty) */
			ocdErrDie(0,code,"Cannot add code after compilation!",0);
		}
		m_all_code+=code;
	}
	
	/* Show a copy of the accumulated source code so far */
	std::string show_code(int version=0);
	
	/* Return a compiled copy of the code added above. 
	   The first time this is called, the code is compiled,
	   but the result is cached so subsequent calls are very fast.
	*/
	cl_program all_compiled(void) {
		if (m_all_compiled==0) compile();
		return m_all_compiled;
	}
	
/************ Buffer Pool **********
   Recycling deleted buffers.  A full allocation costs 100+us, so 
   the speedup obtainable here is enormous!
*/
	enum {max_buffers=3}; // short leash, minimize memory overhead
	std::vector<gpu_buffer *> buffer_pool;
	bool buffer_reuse_enabled;
	
	/// Hand back the data stored by this doomed buffer
	void buffer_release(gpu_buffer *doomed);

	/// Try to find an existing buffer for this guy's size
	bool buffer_reuse(gpu_buffer *newborn);
	
	
	/** This method returns the current gpu_env singleton. 
	   Using a static method allows global variable initializers 
	   to more safely use gpu_env.
	*/
	static gpu_env &static_env(void);
	
	/** Return the cl_context currently in use. */
	static cl_context &get_ctx(void) {
		gpu_env &e=static_env();
		if (e.clCTX==0) e.compile();
		return e.clCTX;
	}
	/** Return the cl_command_queue currently in use. */
	static cl_command_queue &get_que(void) {
		gpu_env &e=static_env();
		if (e.clQUE==0) e.compile();
		return e.clQUE;
	}
	
	~gpu_env();
	
private:
	gpu_env(); /* call "static_env" above, not this! */
	std::string m_all_code; /* all OpenCL encountered so far */
	void compile(void);
	cl_program m_all_compiled; /* compiled version of above */
	
	gpu_env(const gpu_env &b); /* do not copy or assign gpu_env */
	void operator=(const gpu_env &b);
};

/************************** Storage Handling ************************/
/** Global memory access keyword, used in GPU argument lists.
  In C++, this decays to a cl_mem reference. 
  In OpenCL, the extra < and > are removed in precompile fixup.
*/
template <typename T>
class __global {
public:
	cl_mem pointer;
	__global() {pointer=0;}
	__global(cl_mem m) {pointer=m;}
	void operator=(cl_mem m) {pointer=m;}
	operator cl_mem () const {return pointer;}
};


/** Represents a block of on-GPU memory.  
  You should use one of the array versions,
  not this class directly! */
class gpu_buffer {
	gpu_env &env; /* environment this buffer is associated with */
	void *host_ptr;
protected:
	cl_mem device_ptr; // handle for GPU data 
public:
	size_t bytes; // number of bytes in buffer
	
	static const void *alloc_host; // special initial_values value, asking for page-locked host RAM
	
	/** Make an empty buffer */
	gpu_buffer(void) 
		:env(gpu_env::static_env()), 
		host_ptr(0), device_ptr(0), bytes(0) 
	{}
	
	/**
	  Create a new data storage buffer of this size.
	  If initial_values is NULL, it is ignored.
	  If initial_values is gpu_buffer::host_alloc, we will allocate page-locked host RAM
	  else the data at initial_values is written into the new buffer.
	*/
	gpu_buffer(size_t byte_count_,const void *initial_values=0);
	
	// Allow gpu_buffers to decay to cl_mem objects
	operator cl_mem (void) const {return device_ptr;}
	
	// Get the byte count
	size_t get_byte_count(void) const {return bytes;}
	
	// Map data onto CPU space
	void *map(cl_map_flags flags=CL_MAP_READ+CL_MAP_WRITE,size_t offset=0,size_t nbytes=0) {
		if (host_ptr!=0) unmap();
		cl_int errcode;
		host_ptr=clEnqueueMapBuffer(env.clQUE, 
			device_ptr,CL_TRUE,flags,
			offset,nbytes==0?bytes:nbytes,
			0,0,0,&errcode); ocdErr(errcode);
		return host_ptr;
	}
	
	// Unmap data (back into GPU space)
	void unmap(void) {
		if (host_ptr!=0) {
			ocdErr(clEnqueueUnmapMemObject(env.clQUE, 
				device_ptr,host_ptr,
				0,0,0));
			host_ptr=0;
		}
	}
	
	// Read data back to CPU
	void read(void *dest,size_t offset,size_t nbytes) const
	{
		ocdErr( clEnqueueReadBuffer(env.clQUE, 
		      device_ptr, CL_TRUE, offset, nbytes, 
		      dest, 0, NULL, NULL) );
	}
	// Write data from CPU
	void write(const void *src,size_t offset,size_t nbytes) const
	{
		ocdErr( clEnqueueWriteBuffer(env.clQUE, 
		      device_ptr, CL_TRUE, offset, nbytes, 
		      src, 0, NULL, NULL) );
	}
	
	// Force immediate memory deallocation.
	//   Unlike the destructor, no pooling/recycling is done.
	void deallocate(void) {
		if (device_ptr!=0) {
			unmap();
//printf("OpenCL raw memory dealloc ptr=%p, size %ld\n",device_ptr,get_byte_count());
			clReleaseMemObject(device_ptr);
			device_ptr=0;
		}
	}
	~gpu_buffer() {
		if (device_ptr!=0) env.buffer_release(this);
		deallocate();
	}
	void swapwith(gpu_buffer &arr) {
		std::swap(arr.device_ptr,device_ptr);
		std::swap(arr.host_ptr,host_ptr);
		std::swap(arr.bytes,bytes);
	}
private:
	gpu_buffer(const gpu_buffer &b); /* do not copy or assign gpu_buffers */
	void operator=(const gpu_buffer &b);
};
namespace std
{
    void swap(gpu_buffer& lhs, gpu_buffer& rhs) { lhs.swapwith(rhs); }
};

/** An on-GPU 1D array.  This is where you want to store most of your data! */
template <class T>
class gpu_array : public gpu_buffer {
public:
	int size;
	
	/** Create a new gpu array with this size.
	   If initial values are given, they are copied from CPU to GPU. */
	gpu_array(int size_,const T *initial_values=0) 
		:gpu_buffer(size_*sizeof(T),initial_values),
		size(size_) {}
	
	
	/** Create a new gpu array from this CPU vector. */
	gpu_array(const std::vector<T> &initial_values) 
		:gpu_buffer(initial_values.size()*sizeof(T),&initial_values[0]),
		size(initial_values.size()) {}
	
	// Map data onto CPU space
	T *map(cl_map_flags flags=CL_MAP_READ+CL_MAP_WRITE,size_t offset=0,size_t nbytes=0) {
		return (T *)gpu_buffer::map(flags,offset*sizeof(T),nbytes*sizeof(T));
	}
	
	// Read a contiguous number of T values, starting at this location.
	void read(T *dest,size_t first=0,size_t n=0) const {
		if (n==0) n=size;
		gpu_buffer::read((void *)dest,first*sizeof(T),n*sizeof(T));
	}
	// Write a contiguous number of T values, starting at this location.
	void write(const T *src,size_t first=0,size_t n=0) const {
		if (n==0) n=size;
		gpu_buffer::write((const void *)src,first*sizeof(T),n*sizeof(T));
	}
	
	
	// Read off a vector of T values, starting at this location.
	void read(std::vector<T> &dest,size_t first=0,size_t n=0) const {
		if (n==0) n=dest.size();
		gpu_buffer::read(&dest[0],first*sizeof(T),n*sizeof(T));
	}
	// Write in a contiguous vector of T values, starting at this location.
	void write(const std::vector<T> &src,size_t first=0,size_t n=0) const {
		if (n==0) n=src.size();
		gpu_buffer::write(&src[0],first*sizeof(T),n*sizeof(T));
	}
	
	
	// WARNING!  This works, but it's SLOW: 4-10 microseconds per call!
	//   Use a big bulk read for better efficiency.
	T operator[](size_t i) const {
		T ret; read(&ret,i,1); return ret;
	}
	T operator[](int i) const {
		T ret; read(&ret,i,1); return ret;
	}
	
	// Allow gpu_arrays to decay to __global<T *> objects
	operator __global<T *> (void) const {return device_ptr;}
	
	/* array=kernel(args); means "Run this kernel inside the dimensions of this array." */
	void operator=(gpu_kernelT<gpu_any_datatype,1>& src); // for ordinary kernels
	void operator=(gpu_kernelT<T,1>& src); // for FILL kernels
};
namespace std
{
    template<class T>
    void swap(gpu_array<T>& lhs, gpu_array<T>& rhs) { lhs.swapwith(rhs); }
};

/** An on-GPU row major 2D array */
template <class T>
class gpu_array2d : public gpu_buffer {
public:
	int w,h;
	gpu_array2d(int w_,int h_,const T *initial_values=0) 
		:gpu_buffer(w_*h_*sizeof(T),initial_values),
		 w(w_), h(h_) {}
	
	// Map all our data into CPU space
	T *map(cl_map_flags flags=CL_MAP_READ+CL_MAP_WRITE,size_t offset=0,size_t nbytes=0) {
		return (T *)gpu_buffer::map(flags,offset*sizeof(T),nbytes*sizeof(T));
	}
	
	/// Read a contiguous block of T values, starting at this location.
	void read(T *dest,int x=0,int y=0,size_t n=0) const {
		if (n==0) n=w*h;
		gpu_buffer::read((void *)dest,(x+y*w)*sizeof(T),n*sizeof(T));
	}
	/// Write a contiguous block of T values, starting at this location.
	void write(const T *src,int x=0,int y=0,size_t n=0) const {
		if (n==0) n=w*h;
		gpu_buffer::write((void *)src,(x+y*w)*sizeof(T),n*sizeof(T));
	}
	
	// Read the one value at this location
	// WARNING!  This works, but it's SLOW: 4-10 microseconds per call!
	//   Use a big bulk read for better efficiency.
	T operator()(int x,int y) const {
		T ret; read(&ret,x,y,1); return ret;
	}
	
	// Allow gpu_array2ds to decay to __global<T *> objects
	operator __global<T *> (void) const {return device_ptr;}
	
	/* array=kernel(args); means "Run this kernel inside the dimensions of this array." */
	void operator=(gpu_kernelT<gpu_any_datatype,2>& src); // for ordinary kernels
	void operator=(gpu_kernelT<T,2>& src); // for FILL kernels
};
namespace std
{
    template<class T>
    void swap(gpu_array2d<T>& lhs, gpu_array2d<T>& rhs) { lhs.swapwith(rhs); }
};
/** This specialization converts gpu_array parameters into cl_mem arguments */
template <typename T>
inline void ocdKernelArg(cl_kernel k,int number,const gpu_array2d<T> &A) {
	cl_mem m=A;
	ocdErr( clSetKernelArg(k, number, sizeof(cl_mem), (void *)&m) );
}



/** An on-GPU 2D image.  Images use texture memory, which is cached during read. */
template <class T>
class gpu_image2d {
	gpu_env &env;
	cl_mem device_ptr; // handle for GPU data 
public:
	int w,h;
	gpu_image2d(int w_,int h_) 
		:env(gpu_env::static_env()), device_ptr(0), w(w_), h(h_) 
	{
		cl_int errcode;
		cl_image_format fmt;
		fmt.image_channel_order=CL_INTENSITY; //CL_R; works on ATI & NVIDIA, not Intel // FIXME: structs?
		fmt.image_channel_data_type=CL_FLOAT; // FIXME: from T
		device_ptr=clCreateImage2D(env.clCTX,
			CL_MEM_READ_WRITE, &fmt,
			w,h, 0, // pitch: can't specify w*sizeof(T) unless also pass host ptr
			0,&errcode);  ocdErr(errcode);
	}
	// FIXME: ~gpu_image2d(), copy, assignment
	
	/// Read a contiguous block of T values, starting at this location.
	void read(T *dest,int x=0,int y=0,size_t cw=0,size_t ch=0) const {
		if (cw==0) cw=w;  if (ch==0) ch=h;
		size_t origin[3]; origin[0]=x; origin[1]=y; origin[2]=0;
		size_t region[3]; region[0]=cw; region[1]=ch; region[2]=1;
		ocdErr(clEnqueueReadImage(env.clQUE,
			device_ptr,CL_TRUE,
			origin,region,
			sizeof(T)*w,0,
			dest,
			0,0,0));
	}
	
	// Allow gpu_array2ds to decay to __global<T *> objects
	operator __global<T *> (void) const {return device_ptr;}
	// Allow gpu_buffers to decay to cl_mem objects
	operator cl_mem (void) const {return device_ptr;}
	
	/* image=kernel(args); means "Run this kernel inside the dimensions of this image." */
	void operator=(gpu_kernelT<gpu_any_datatype,2>& src); // for ordinary kernels
	void operator=(gpu_kernelT<T,-2>& src); // for FILL kernels
	void swapwith(gpu_image2d<T> &rhs) {
		std::swap(device_ptr,rhs.device_ptr);
		std::swap(w,rhs.w); std::swap(h,rhs.h);
	}
};
namespace std
{
    template<class T>
    void swap(gpu_image2d<T>& lhs, gpu_image2d<T>& rhs) { lhs.swapwith(rhs); }
};

/******************* Kernels ***********************/
/**
  Handle and cache for one compiled GPU kernel.
  Don't make these yourself, use the GPU_KERNEL macros below.
  
  This is the base class of a hierarchy of templated children:
    gpu_kernel: base class, cl_kernel handling, name, run method.
	
	gpu_kernelT<result type (or gpu_any_datatype if unknown), dimensions>:
		Does nothing, but is used by gpu_image::operator= for type checking.
	
	gpu_kernel_args<void (kernel argument list)>:
		Check and apply kernel arguments.  Specialized for each argument count.
	
	gpu_kernel_userKernelName:
		
*/
class gpu_kernel {
public:
	gpu_kernel(const std::string &name_)
		:env(gpu_env::static_env()), name(name_), first_arg(0), dimensions(0), 
		k(0)
	{}
	
	/** Return the number of our first user-supplied argument.  Typically 0, unless there are prearguments. */
	int get_first_arg(void) const {return first_arg;}
	
	/** Return our compiled kernel.  Caches the answer. */
	cl_kernel get_kernel(void) {
		if (k==0) compile_kernel();
		return k;
	}
	
	/** Estimate the desired workgroup size for this kernel. */
	cl_uint get_workgroupsize(size_t desired=256u);
	
	/**
	   Run this kernel with these 1D dimensions.
	   You must already have set all the kernel arguments.
	*/
	void run(int size);

	/**
	   Run this kernel with these 2D dimensions.
	   You must already have set all the kernel arguments.
	*/
	void run(int w,int h);
	
	~gpu_kernel() {
		if (k) clReleaseKernel(k);
	}
protected:
	gpu_env &env; /* where our compiled code is stored */
	std::string name; /* our kernel's OpenCL callable name */
	int first_arg; /* number of our first user kernel parameter (typically 0, unless there are prearguments) */
	int dimensions; /* 1d, 2d, 3d, etc  (0 for unknown) */

public:
	class groupsz {
	public:
		enum {max=3};
		size_t sz[max]; /* local workgroup size (0 for unknown) */
		size_t &operator[](int i) {return sz[i];}
		operator size_t * () {return &sz[0];}
		groupsz() {for (int i=0;i<max;i++) sz[i]=0u;}
	};
	groupsz override_local; /* size of local workgroup (if 0, compute automatically) */

private:
	cl_kernel k;
	void compile_kernel(void) {
		cl_int errcode;
		k = clCreateKernel(env.all_compiled(), name.c_str(), &errcode); ocdErr(errcode);
	}
	void operator=(const gpu_kernel &k); /* do not copy or assign gpu_kernels! */
	gpu_kernel(const gpu_kernel &k);
};

/* This is a thin wrapper around gpu_kernel to provide typechecking information.
  This way, gpu_array below won't work on kernels of the wrong datatype or dimensions.
*/
template <class dataType,int dimensions> 
class gpu_kernelT : public gpu_kernel {
public:
	gpu_kernelT(const std::string &name_) :gpu_kernel(name_) {}
};

/* array=kernel(args); means "Run this kernel inside the dimensions of this array." */
template <class T>
void gpu_array<T>::operator=(gpu_kernelT<gpu_any_datatype,1>& src) 
{ // for ordinary kernels
	src.run(size); 
}
template <class T>
void gpu_array<T>::operator=(gpu_kernelT<T,1>& src) 
{ // for FILL kernels, which need several pre-parameters
	cl_kernel k=src.get_kernel();
	ocdKernelArg(k,0,size);
	ocdKernelArg(k,1,device_ptr);
	src.run(size); 
}
template <class T>
void gpu_array2d<T>::operator=(gpu_kernelT<gpu_any_datatype,2>& src) 
{ // for ordinary kernels
	src.run(w,h); 
}
template <class T>
void gpu_array2d<T>::operator=(gpu_kernelT<T,2>& src) 
{ // for FILL kernels, which need several pre-parameters
	cl_kernel k=src.get_kernel();
	ocdKernelArg(k,0,w);
	ocdKernelArg(k,1,h);
	ocdKernelArg(k,2,device_ptr);
	src.run(w,h); 
}
template <class T>
void gpu_image2d<T>::operator=(gpu_kernelT<gpu_any_datatype,2>& src)
{ // for ordinary kernels
	src.run(w,h); 
}
template <class T>
void gpu_image2d<T>::operator=(gpu_kernelT<T,-2>& src) // for FILL kernels
{ // for FILL kernels: insert prearguments
	cl_kernel k=src.get_kernel();
	ocdKernelArg(k,0,w);
	ocdKernelArg(k,1,h);
	ocdKernelArg(k,2,device_ptr);
	src.run(w,h); 
}

/** Converts C++ kernel parameters into OpenCL argument types.
 Currently, this is the identity, but it could be specialized for:
     - Convert fancy_array_2d<T> into cl_mem
	 - Convert const .... into cl_mem with const magic
	 - ??
*/
template <typename T>
class gpu_passtype {public:
	typedef T type; /* type used in argument passing into OpenCL */
};


/** Generic superclass for kernel function objects.
  This class adds kernel argument types, and typechecking,
  to the generic gpu_kernelT class.
*/
template <typename ANY_FUNCTION,typename gpu_kernelT>
class gpu_kernel_args : public gpu_kernelT {
};

template <typename gpu_kernelT>  /* specialization for 0-argument functions */
class gpu_kernel_args<void (),gpu_kernelT> : public gpu_kernelT {public:
	gpu_kernel_args(const char *kernelName) :gpu_kernelT(kernelName) {}
	gpu_kernelT &operator() ()
	{
		return *this;
	}
};
/* specialization for 1-argument functions */
template <typename T0,typename gpu_kernelT> 
class gpu_kernel_args<void (T0),gpu_kernelT> : public gpu_kernelT {public:
	gpu_kernel_args(const char *kernelName) :gpu_kernelT(kernelName) {}
	gpu_kernelT &operator()(typename gpu_passtype<T0>::type A0)
	{
		cl_kernel k=gpu_kernelT::get_kernel(); 
		int i=gpu_kernelT::get_first_arg();
		ocdKernelArg(k,i+0,A0);
		return *this;
	}
};

/* specialization for 2-argument functions */
template <typename T0,typename T1,typename gpu_kernelT>  
class gpu_kernel_args<void (T0,T1),gpu_kernelT> : public gpu_kernelT {public:
	gpu_kernel_args(const char *kernelName) :gpu_kernelT(kernelName) {}
	gpu_kernelT &operator()(typename gpu_passtype<T0>::type A0,typename gpu_passtype<T1>::type A1)
	{
		cl_kernel k=gpu_kernelT::get_kernel(); 
		int i=gpu_kernelT::get_first_arg();
		ocdKernelArg(k,i+0,A0);
		ocdKernelArg(k,i+1,A1);
		return *this;
	}
};

/* specialization for 3-argument functions */
template <typename T0,typename T1,typename T2,typename gpu_kernelT>  
class gpu_kernel_args<void (T0,T1,T2),gpu_kernelT> : public gpu_kernelT {public:
	gpu_kernel_args(const char *kernelName) :gpu_kernelT(kernelName) {}
	gpu_kernelT &operator()(typename gpu_passtype<T0>::type A0,typename gpu_passtype<T1>::type A1,typename gpu_passtype<T2>::type A2)
	{
		cl_kernel k=gpu_kernelT::get_kernel(); 
		int i=gpu_kernelT::get_first_arg();
		ocdKernelArg(k,i+0,A0);
		ocdKernelArg(k,i+1,A1);
		ocdKernelArg(k,i+2,A2);
		return *this;
	}
};

/* specialization for 4-argument functions */
template <typename T0,typename T1,typename T2,typename T3,typename gpu_kernelT>  
class gpu_kernel_args<void (T0,T1,T2,T3),gpu_kernelT> : public gpu_kernelT {public:
	gpu_kernel_args(const char *kernelName) :gpu_kernelT(kernelName) {}
	gpu_kernelT &operator()(typename gpu_passtype<T0>::type A0,
		typename gpu_passtype<T1>::type A1,
		typename gpu_passtype<T2>::type A2,
		typename gpu_passtype<T3>::type A3
	)
	{
		cl_kernel k=gpu_kernelT::get_kernel(); 
		int i=gpu_kernelT::get_first_arg();
		ocdKernelArg(k,i+0,A0);
		ocdKernelArg(k,i+1,A1);
		ocdKernelArg(k,i+2,A2);
		ocdKernelArg(k,i+3,A3);
		return *this;
	}
};

/* specialization for 5-argument functions */
template <typename T0,typename T1,typename T2,typename T3,typename T4,typename gpu_kernelT>  
class gpu_kernel_args<void (T0,T1,T2,T3,T4),gpu_kernelT> : public gpu_kernelT {public:
	gpu_kernel_args(const char *kernelName) :gpu_kernelT(kernelName) {}
	gpu_kernelT &operator()(typename gpu_passtype<T0>::type A0,
		typename gpu_passtype<T1>::type A1,
		typename gpu_passtype<T2>::type A2,
		typename gpu_passtype<T3>::type A3,
		typename gpu_passtype<T4>::type A4
	)
	{
		cl_kernel k=gpu_kernelT::get_kernel(); 
		int i=gpu_kernelT::get_first_arg();
		ocdKernelArg(k,i+0,A0);
		ocdKernelArg(k,i+1,A1);
		ocdKernelArg(k,i+2,A2);
		ocdKernelArg(k,i+3,A3);
		ocdKernelArg(k,i+4,A4);
		return *this;
	}
};

/* specialization for 6-argument functions */
template <typename T0,typename T1,typename T2,typename T3,typename T4,typename T5,typename gpu_kernelT>  
class gpu_kernel_args<void (T0,T1,T2,T3,T4,T5),gpu_kernelT> : public gpu_kernelT {public:
	gpu_kernel_args(const char *kernelName) :gpu_kernelT(kernelName) {}
	gpu_kernelT &operator()(typename gpu_passtype<T0>::type A0,
		typename gpu_passtype<T1>::type A1,
		typename gpu_passtype<T2>::type A2,
		typename gpu_passtype<T3>::type A3,
		typename gpu_passtype<T4>::type A4,
		typename gpu_passtype<T5>::type A5
	)
	{
		cl_kernel k=gpu_kernelT::get_kernel(); 
		int i=gpu_kernelT::get_first_arg();
		ocdKernelArg(k,i+0,A0);
		ocdKernelArg(k,i+1,A1);
		ocdKernelArg(k,i+2,A2);
		ocdKernelArg(k,i+3,A3);
		ocdKernelArg(k,i+4,A4);
		ocdKernelArg(k,i+5,A5);
		return *this;
	}
};

/* specialization for 7-argument functions */
template <typename T0,typename T1,typename T2,typename T3,typename T4,typename T5,typename T6,typename gpu_kernelT>  
class gpu_kernel_args<void (T0,T1,T2,T3,T4,T5,T6),gpu_kernelT> : public gpu_kernelT {public:
	gpu_kernel_args(const char *kernelName) :gpu_kernelT(kernelName) {}
	gpu_kernelT &operator()(typename gpu_passtype<T0>::type A0,
		typename gpu_passtype<T1>::type A1,
		typename gpu_passtype<T2>::type A2,
		typename gpu_passtype<T3>::type A3,
		typename gpu_passtype<T4>::type A4,
		typename gpu_passtype<T5>::type A5,
		typename gpu_passtype<T6>::type A6
	)
	{
		cl_kernel k=gpu_kernelT::get_kernel(); 
		int i=gpu_kernelT::get_first_arg();
		ocdKernelArg(k,i+0,A0);
		ocdKernelArg(k,i+1,A1);
		ocdKernelArg(k,i+2,A2);
		ocdKernelArg(k,i+3,A3);
		ocdKernelArg(k,i+4,A4);
		ocdKernelArg(k,i+5,A5);
		ocdKernelArg(k,i+6,A6);
		return *this;
	}
};

/************************** Public User Interface *************************/

/**
 Add a quoted string of OpenCL code to the list accumulated in gpu_env.
 This macro also records the file and line number where the code was inserted from.
 This is useful for pragma's, and OpenCL-side defines that can't be replicated by the preprocessor.
*/
#define GPU_ADD_STATIC_CODE(codeString,identifier) \
static class gpu_add_static_helper_##identifier { public: \
	gpu_add_static_helper_##identifier() { gpu_env::static_env().add_code( \
		"/* From "__FILE__ ":" ocdEXPANDSTRING(__LINE__)" " #identifier " */\n" \
		codeString \
		"\n\n" \
	);} \
} gpu_add_static_helper_object_##identifier;


/** Add this piece of OpenCL code.   Automatically quoted. */
#define GPU_ONLY(code) \
GPU_ADD_STATIC_CODE(#code,GPUONLY##__FILE__##__LINE__)


/** Add this piece of shared OpenCL/C++ code. This is copied verbatim into 
*both* languages */
#define GPU_SHARED(sharedCode) \
GPU_ADD_STATIC_CODE(#sharedCode,GPUSHARED##__FILE__##__LINE__) \
sharedCode 


/**
 Declare a shared OpenCL/C++ struct with this name.
 The "sharedFields" are simple data members shared between OpenCL and C++.
 The "cplusplusOnlyCode" only appears in C++, and is omitted from OpenCL.
 	This can contain constructors, inline functions, and operators;
	and public and private parts; 
	but it cannot contain virtual methods, or any bare commas.

Typical usage:

GPU_STRUCT(vec2, // <- struct's name
// OpenCL and C++ declaration of fields
	float x; 
	float y;
,
// C++-only methods
	vec2(float x_,float y_) { x=x_; y=y_; }
	vec2 &operator+=(const vec2 &rhs) {x+=rhs.x; y+=rhs.y; }
	friend vec2 operator+(vec2 lhs,const vec2 &rhs) {lhs+=rhs; return lhs;}
)

FIXME: allow C++-only fields, and slice struct at runtime?  Plug into ocd type system?
*/
#define GPU_STRUCT(structName,sharedFields,cplusplusOnlyCode) \
struct structName { \
	sharedFields; \
	cplusplusOnlyCode; \
}; \
GPU_ADD_STATIC_CODE( \
	"typedef struct { \n" \
	#sharedFields " \n" \
	"} " #structName ";\n" \
,GPU_STRUCT__##structName)


/**
	Define a GPU kernel running over a 1D domain.
	In OpenCL, the name, arguments, and body code are concatenated in a string.
	In C++, a gpu_kernel object is created to accept the arguments.

WARNING: you MUST check i against the global size of your domain!
	
*/
#define GPU_KERNEL(kernelName,kernelArgs,kernelBodyCode) \
class gpu_kernel_##kernelName : \
	public gpu_kernel_args<void kernelArgs,gpu_kernelT<gpu_any_datatype,1> > { \
public: \
	gpu_kernel_##kernelName() :gpu_kernel_args<void kernelArgs,gpu_kernelT<gpu_any_datatype,1> >(#kernelName) { \
		dimensions=1; \
		GPU_ADD_STATIC_CODE( \
		"__kernel void " #kernelName #kernelArgs "\n" \
		"{\n"\
		"	int i=get_global_id(0);\n" \
		"	" #kernelBodyCode ";\n" \
		"}\n",GPU_KERNEL__##kernelName) \
	} \
} kernelName; 


/**
	Define a "fill" style GPU kernel to write to every element of a 1D domain.
	In OpenCL, the name, arguments, and body code are concatenated in a string.
	In C++, a gpu_kernel object is created to accept the arguments.
*/
#define GPU_FILLKERNEL(resultType,kernelName,kernelArgs,kernelBodyCode) \
class gpu_fillkernel_##kernelName : \
	public gpu_kernel_args<void kernelArgs,gpu_kernelT<resultType,1> > { \
public: \
	gpu_fillkernel_##kernelName() :gpu_kernel_args<void kernelArgs,gpu_kernelT<resultType,1> >(#kernelName) { \
		dimensions=1; \
		first_arg=2; /* leave room for (size,destination) prearguments below */ \
		GPU_ADD_STATIC_CODE( \
		"__kernel void " #kernelName "(int result_length,__global " #resultType " *result_array ,<__FILLKERNEL)"#kernelArgs "\n" \
		"{\n"\
		"	int i=get_global_id(0);\n" \
		"	if (i<result_length) { \n" \
		"		const int result_index=i;\n" \
		"		"#resultType" result=result_array[result_index];\n\n" \
		"		" #kernelBodyCode ";\n\n" \
		"		result_array[result_index]=result; \n" \
		"	}\n" \
		"}\n",GPU_FILLKERNEL__##kernelName) \
	} \
} kernelName;


/**
	Define a GPU kernel running over a 2D domain.
	In OpenCL, the name, arguments, and body code are concatenated in a string.
	In C++, a gpu_kernel object is created to accept the arguments.

WARNING: you MUST check i,j against the size of your global domain!
*/
#define GPU_KERNEL_2D(kernelName,kernelArgs,kernelBodyCode) \
class gpu_kernel_##kernelName : \
	public gpu_kernel_args<void kernelArgs,gpu_kernelT<gpu_any_datatype,2> > { \
public: \
	gpu_kernel_##kernelName() :gpu_kernel_args<void kernelArgs,gpu_kernelT<gpu_any_datatype,2> >(#kernelName) { \
		dimensions=2; \
		GPU_ADD_STATIC_CODE( \
		"__kernel void " #kernelName #kernelArgs "\n" \
		"{\n"\
		"	int i=get_global_id(0);\n" \
		"	int j=get_global_id(1);\n" \
		"	" #kernelBodyCode ";\n" \
		"}\n",GPU_KERNEL_2D__##kernelName) \
	} \
} kernelName; 


/**
	Define a "fill" style GPU kernel to write to every element of a 2D array.
	In OpenCL, the name, arguments, and body code are concatenated in a string.
	In C++, a gpu_kernel object is created to accept the arguments.
*/
#define GPU_FILLKERNEL_2D(resultType,kernelName,kernelArgs,kernelBodyCode) \
class gpu_fillkernel_##kernelName : \
	public gpu_kernel_args<void kernelArgs,gpu_kernelT<resultType,2> > { \
public: \
	gpu_fillkernel_##kernelName() :gpu_kernel_args<void kernelArgs,gpu_kernelT<resultType,2> >(#kernelName) { \
		dimensions=2; \
		first_arg=3; /* leave room for (size,destination) prearguments below */ \
		GPU_ADD_STATIC_CODE( \
		"__kernel void " #kernelName "(int w,int h,__global " #resultType " *result_array ,<__FILLKERNEL)"#kernelArgs "\n" \
		"{\n"\
		"	int i=get_global_id(0);\n" \
		"	int j=get_global_id(1);\n" \
		"	if (i<w && j<h) { \n" \
		"		const int result_index=i+j*w;\n" \
		"		"#resultType" result=result_array[result_index];\n\n" \
		"		" #kernelBodyCode ";\n\n" \
		"		result_array[result_index]=result; \n" \
		"	}\n" \
		"}\n",GPU_FILLKERNEL_2D__##kernelName) \
	} \
} kernelName;

/**
	Define a "fill" style GPU kernel to write to every element of a 2D image.
	
	WARNING: unlike the array case, the initial value of "result" is zero for images.
	
	In OpenCL, the name, arguments, and body code are concatenated in a string.
	In C++, a gpu_kernel object is created to accept the arguments.
*/
#define GPU_FILLKERNEL_IMAGE2D(resultType,kernelName,kernelArgs,kernelBodyCode) \
class gpu_fillkernel_##kernelName : \
	public gpu_kernel_args<void kernelArgs,gpu_kernelT<resultType,-2> > { \
public: \
	gpu_fillkernel_##kernelName() :gpu_kernel_args<void kernelArgs,gpu_kernelT<resultType,-2> >(#kernelName) { \
		dimensions=2; \
		first_arg=3; /* leave room for (size,destination) prearguments below */ \
		GPU_ADD_STATIC_CODE( \
		"__kernel void " #kernelName "(int w,int h,__write_only image2d_t result_image ,<__FILLKERNEL)"#kernelArgs "\n" \
		"{\n"\
		"	int i=get_global_id(0);\n" \
		"	int j=get_global_id(1);\n" \
		"	if (i<w && j<h) { \n" \
		"		const int2 result_index=(int2)(i,j);\n" \
		"		"#resultType" result=0; /*result_array[result_index];*/\n\n" \
		"		" #kernelBodyCode ";\n\n" \
		"		write_imagef(result_image,result_index,result); \n" \
		"	}\n" \
		"}\n",GPU_FILLKERNEL_2D__##kernelName) \
	} \
} kernelName;



/*
  FIXMEs!
     - Add C++ types for int2, int4, float2, float3, float4, ...
	 - 3D arrays, images, and kernels.
	 - Smarter array indexing in OpenCL (Fortran arr(i) syntax?  bounds check? more compiler-y work?)
	 - Robustness testing
*/

#endif
