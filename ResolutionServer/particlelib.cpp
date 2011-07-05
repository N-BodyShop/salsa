/**
 Implementations of all particlelib classes and functions.

 Dr. Orion Sky Lawlor, lawlor@alaska.edu, 2011-07-03
*/
/* Total overkill: basically I just want "Vector3D" */
#include "config.h"
#include "tree_xdr.h"
#include "SiXFormat.h"
#include "TipsyFormat.h"
#include "SPH_Kernel.h"
#include "Interpolate.h"
#include "Group.h"
#include "ResolutionServer.h"
#include "Reductions.h"
#include "Space.h"

#include "ckarray.h" /* needed for ArrayElement::contribute */
#include "ResolutionServer.h" /* needed for "MyVizRequest" (should be moved in here) */
#include "particlelib.h"

/* Set these to zero IN THE *MAKEFILE* to turn off all GPU stuff  */
#ifndef PARTICLELIB_TRY_GPU
#define PARTICLELIB_TRY_GPU 0  /* OpenGL, for basic rendering */
#endif
#ifndef PARTICLELIB_TRY_GPU
#define PARTICLELIB_TRY_OPENCL 0 /* OpenCL, for vertex randomization and compression */
#endif

void particle_renderer::render_count(size_t particle_count) { 
	CkAbort("Called superclass particle_renderer::render_count in particlelib.cpp\n");
}

/**
  Common superclass for basically all real particle renderers.
*/
class typical_particle_renderer : public particle_renderer {
protected:
	/** Parameters for the current viz request. */
	MyVizRequest req;
	
	/** Dimensions of the current viz request, in pixels. */
	int wid,ht,depth;
	bool doVolumeRender;
	
	/** Return a pointer to this class's data storage area. */
	virtual const unsigned char *getImageData(void)=0;
public:
	/** Accept this new renderer request. */
	virtual void setup(MyVizRequest &req_,bool flush_cached)
	{
		req=req_;
		if (req.height==req.width*req.width) { /* 3D rendering */
			doVolumeRender=true;
			wid=req.width; ht=req.width; depth=req.width;
		} else { /* 2D rendering */
			doVolumeRender=false;
			wid=req.width; ht=req.height; depth=1;
		}
		/* Convert from length to 1/length: dot product now scales to axis size */
		req.x /= req.x.lengthSquared();
		req.y /= req.y.lengthSquared();
		req.z /= req.z.lengthSquared();
	}
	
	/* particle rendering calls need to be overridden here */
	
	/** End a rendering session.  This should always be called after all particles are uploaded. */
	virtual void finish(liveVizRequestMsg* m,ArrayElement *reducer)
	{
		liveVizDeposit(m, 0, 0, req.width, req.height, getImageData(), reducer, (req.doSplatter ? sum_image_data : max_image_data));
	}
};


/** 
  Common superclass for image-based particle renderers (again, all of them)
*/
class image_particle_renderer : public typical_particle_renderer {
protected:
	/** Renderable pixels: an array of row_stride * req.ht of them */
	std::vector<unsigned char> pixels;
	
	/** Return a reference to pixel (x,y) */
	inline unsigned char &at(int x,int y) {return pixels[x+wid*y];}
	
	/** Return a pointer to this class's data storage area. */
	virtual const unsigned char *getImageData(void) {return &pixels[0];}
	
	// Coordinate indexing:
	Vector3D<float> xAxis, yAxis, zAxis; // axes, scaled by pixel size (dot these)
	float xo, yo, zo; // offset to origin (subtract these)
public:
	virtual void setup(MyVizRequest &req_,bool flush_cached)
	{
		typical_particle_renderer::setup(req_,flush_cached);
		pixels.resize(wid*ht*depth); /* automatically zeroed */
		
		float hX=wid*0.5, hY=ht*0.5, hZ=depth*0.5;
		xAxis=req.x*hX, yAxis=req.y*hY, zAxis=req.z*hZ; /* pixel axes */
		xo=(dot(req.x,req.o)-1)*hX, yo=(dot(req.y,req.o)-1)*hY, zo=(dot(req.z,req.o)-1)*hZ;
	}
	
	/* Still need to override all the "render" calls in child classes. */
	
	virtual void finish(liveVizRequestMsg* m,ArrayElement *reducer) {
		typical_particle_renderer::finish(m,reducer);
		if (false && doVolumeRender){
			// Dump voxel image data, for debugging data compression step
			char fname[100];
			static int count=1;
			sprintf(fname,"voxel_%04d.dat",count++);
			FILE *f=fopen(fname,"wb");
			fwrite(&pixels[0],wid*ht*depth,1,f);
			fclose(f);
			printf("Wrote voxel data to '%s'\n",fname);
		}
	}
};

/** A CPU renderer does not need to cache particles (they're already on the CPU) 
  So this class DELETES ITSELF WHEN THE RENDERING IS FINISHED!
  This means you need to call "new" each time.
*/
class CPU_particle_renderer : public image_particle_renderer {
	bool needs;
public:
	CPU_particle_renderer() {needs=true;}
	
	/** Only needs particles the first time. */
	virtual needs_t needs_what(void) { 
		if (needs) {
			needs=false; // once we get the particles, we're done...
			return needs_render;
		} else return needs_nothing;
	}
	
	/* Still need to override the "render" calls in child classes. */
	
	virtual void finish(liveVizRequestMsg* m,ArrayElement *reducer) {
		image_particle_renderer::finish(m,reducer);
		delete this; /* throw ourselves away (no cache, so this is OK) */
	}
};


/** Simple CPU point 2D renderer. */
class CPU_point_2D: public CPU_particle_renderer {
public:
	virtual void render(const Vector3D<float> &location3D,unsigned char color) {
		float x=dot(xAxis,location3D)-xo;
		if (x<0 || x>=wid) return; /* point is offscreen: skip */
		float y=dot(yAxis,location3D)-yo;
		if (y<0 || y>=ht) return;
		// No z clipping planes for now
		// float z=dot(zAxis,location3D)-zo;
		// if (z<0 || z>=2.0) return;
		
		unsigned char &p=at((int)x,(int)y);
		if(p < color) p=color;
	}
};


/** Simple CPU point 3D renderer. */
class CPU_point_3D: public CPU_particle_renderer {
public:
	virtual void render(const Vector3D<float> &location3D,unsigned char color) {
		float x=dot(xAxis,location3D)-xo;
		if (x<0 || x>=wid) return; /* point is offscreen: skip */
		float y=dot(yAxis,location3D)-yo;
		if (y<0 || y>=ht) return;
		float z=dot(zAxis,location3D)-zo;
		if (z<0 || z>=depth) return;
		
		unsigned char &p=at((int)x,((int)y)+ht*(int)z);
		if(p < color) p=color;
	}
};

/** NULL renderer: doesn't actually render anything (perf. benchmark only) */
class NULL_renderer: public image_particle_renderer {
	bool first_time;
public:
	NULL_renderer() {first_time=true;}
	virtual needs_t needs_what(void) {
		if (first_time) return needs_render;
		else return needs_nothing;
	}
	
	virtual void render(const Vector3D<float> &location3D,unsigned char color) {
		/* do nothing */
		first_time=false;
	}
#if 0 /* skip the memory allocation/initialization/big liveViz deposit */
	virtual void setup(MyVizRequest &req_,bool flush_cached)
	{
		/* do nothing */
	}
	virtual void finish(liveVizRequestMsg* m,ArrayElement *reducer) {
		unsigned char stuff=0;
		liveVizDeposit(m, 0, 0, 0,0, &stuff, reducer, (req.doSplatter ? sum_image_data : max_image_data));
	}
#endif
};

#if PARTICLELIB_TRY_GPU
// Implemented below
typical_particle_renderer *start_GPU_render(MyVizRequest &req,bool doVolumeRender,bool flush_cached);
#endif

/**
  Begin a rendering session, typically in response to a liveViz call.
	 @param flush_cached Indicates the renderer should not use any stored particles.
	 @return The renderer to use to finish this rendering session.  Do not delete this object; call "finish" only.
*/
particle_renderer *start_particle_render(MyVizRequest &req,bool flush_cached)
{
	bool doVolumeRender=false;
	// STUPID 2D vs 3D hack: (2D or 3D should be a new field in req)
	if (req.height==req.width*req.width) doVolumeRender=true;
	
	typical_particle_renderer *ret=0;
	
	if (false) // NULL renderer for benchmarking/measuring overheads
	{
		static NULL_renderer *s=new NULL_renderer();
		ret=s;
	}

#if PARTICLELIB_TRY_GPU
	ret=start_GPU_render(req,doVolumeRender,flush_cached);
#endif
	
	if (ret==0) { /* fallback to CPU side */
		if (doVolumeRender) ret=new CPU_point_3D();
		else ret=new CPU_point_2D();
	}
	
	ret->setup(req,flush_cached);
	return ret;
}


/***************************** GPU ********************************/
#if PARTICLELIB_TRY_GPU
/*
This version tries rendering to the local GPU, by opening a
GLSL/glut connection.
*/
#include <X11/Xlib.h> /* for XOpenDisplay preflight check */
#include "GL/glew.h" /* OpenGL Extension Wrangler */
#include "GL/glew.c" /* avoid linking trouble, by just including body directly */

#define PARTICLELIB_USE_GLUT 0 /* use GLUT, or fall back to native X? */
#if PARTICLELIB_USE_GLUT
#  include <GL/glut.h>
#else
#  include <GL/glx.h>
#endif

#include "ogl/glsl.h"  /* Orion's GL Library utility code */
#include "ogl/glsl.cpp" 

// Check GL for errors after the last command
void oglCheck(const char *where) {
        GLenum e=glGetError();
        if (e==GL_NO_ERROR) return;
        //const GLubyte *errString = gluErrorString(e);
        printf("ResolutionServer/particlelib.cpp OpenGL error in %s (err=%d)\n",where,/*errString,*/e);
		abort();
}


/* Return NULL if we can successfully set up an OpenGL context. */
const char * start_GPU(void) {
	// OpenGL setup with GLUT and GLEW
	const char *displayName=":0";
#if PARTICLELIB_USE_GLUT
	setenv("DISPLAY",displayName,1);
	
	// Preflight the DISPLAY:
	if (NULL==XOpenDisplay(displayName)) return "No DISPLAY=:0 X backend available";
	glutInitWindowSize(80,80);
	int argc=1; char *argv[2]; argv[0]=(char *)"foo"; argv[1]=0; /* fake arguments */
	glutInit(&argc,argv); /* FIXME: glut can crash here, if bad GL setup */
	int mode=GLUT_DOUBLE | GLUT_RGBA | GLUT_DEPTH;
	glutInitDisplayMode (mode);
	/*int win=*/ glutCreateWindow("ResolutionServer Backend");
#else /* no GLUT, fall back to native X (UNIX only) */
    Display *display = XOpenDisplay( displayName );
    if( display == NULL ) return "No DISPLAY=:0 X backend available";
	
    if( !glXQueryExtension( display, NULL, NULL ) )
		return "No OpenGL available on DISPLAY=:0";
	const static int attrib[10]={GLX_RGBA,GLX_DOUBLEBUFFER,None};
	XVisualInfo* visualInfo=glXChooseVisual(display,0,(int *)attrib);
	if (visualInfo==NULL) return "Error in glXChooseVisual";
	GLXContext ctx = glXCreateContext(display, visualInfo, NULL, True );
	if (!ctx)  return "Error in glXCreateContext";
	
	Window w=XCreateSimpleWindow(display,RootWindow(display,0),
		0,0,100,100,0,0,0);
	if (!w)  return "Error in XCreateSimpleWindow";
	
	if (!glXMakeCurrent(display,w,ctx)) return "Error in glXMakeCurrent";
	
#endif
	
	glewInit(); /*<- needed for gl...ARB functions to work! */
	
	// If we don't have a modern OpenGL, nothing's going to work.
	if (0==glUseProgramObjectARB || 0==glGenFramebuffersEXT || 0==glBufferDataARB)
		return "OpenGL does not have sufficient extensions (upgrade drivers?)";
	
	/* We don't usually want these features for GPGPU-style rendering: */
	glDisable(GL_DEPTH_TEST);
	glDisable(GL_ALPHA_TEST);
	
	/* We'll use either sum or max as the blending operation */
	glEnable(GL_BLEND);
	glBlendFunc(GL_ONE, GL_ONE);
	oglCheck("After OpenGL setup");
	
	return NULL;
}

/* Tiny Timer functions
   Call timeit("section name") to start a section.
   Call timeit(NULL) to end a test.
*/
const char *last_where=NULL;
double first_t=0.0;
double last_t=0.0;
double timeit(const char *where) {
	glFinish();
	double t=CkWallTimer();
	if (last_where==NULL) { first_t=t; }
	else { // print the time of the last thing
		double elapsed=t-last_t;
		printf("%30s: %6.2f ms\n", last_where,elapsed*1.0e3);
	}
	last_t=t;
	last_where=where;
	return t-first_t;
}


struct GPU_particle {
public:
	Vector3D<float> pos;  // position of particle 
	unsigned char r; // color/X coordinate of vector/SPH center value
	unsigned char g; //       Y coordinate of vector/SPH radius
	unsigned char b; //       Z coordinate of vector
}; 

#if PARTICLELIB_TRY_OPENCL
#include <GL/glx.h> /* <- signals to EPGPU that we want OpenGL interop */
#include "epgpu.h"
#include "epgpu.cpp"


GPU_ADD_STATIC_CODE(
"typedef struct {\n"
"	float x,y,z; // position\n"
"	int color; // actually r,g,b color\n"
"} GPU_particle;\n"
,GPU_particle_struct);

/** "Randomly" choose whether to move particles around, or not. */
GPU_KERNEL(randomize_particles,(__global<GPU_particle *> p,int nParticles,unsigned int lowmask),
{
	int src=((i&~lowmask)<<1)|(i&lowmask);  // make a one-bit hole for the swap
	int dest=src+(lowmask+1); // fill in the hole for the destination
	int pseudorand=(0xDEECE66D*(src*dest)+0xB); // 32-bit variant of Knuth's 48-bit LCRG
	if (pseudorand&0x80 && dest<nParticles) 
	{ // swap src and dest
		GPU_particle tmp=p[src];
		p[src]=p[dest];
		p[dest]=tmp;
	}
}
);

// Randomly reorder this OpenGL VBO.  Randomization on the GPU side should be much faster, if I could just get it to work!
void OpenCL_randomize_VBO(GLuint VBO,size_t nParticles)
{
	cl_int err_from_gl;
	cl_mem vbo_cl = clCreateFromGLBuffer(gpu_env::get_ctx(), CL_MEM_READ_WRITE, VBO, &err_from_gl);
	ocdErr(err_from_gl);
	
	ocdErr(clEnqueueAcquireGLObjects(gpu_env::get_que(), 1, &vbo_cl, 0,0,0));
	
	for (unsigned int lowmask=15;lowmask<nParticles/2;lowmask=lowmask*4+3) {
		timeit("Butterfly randomization pass");
		randomize_particles(vbo_cl,nParticles,lowmask).run(nParticles/2);
	}
	
    ocdErr(clEnqueueReleaseGLObjects(gpu_env::get_que(), 1, &vbo_cl, 0,0,0));
}

#endif


class GPU_renderer : public image_particle_renderer {
	int pass_count; /* number of "render" passes we've received so far */
	std::vector<GPU_particle> pb; /* CPU-side data (only when !cache_full) */
	
	/* Handles for GPU objects (persistent from frame to frame) */
	GLuint VBO;  // OpenGL vertex buffer object, to store particle data
	bool VBO_done; // if true, the VBO is fully populated
	size_t VBO_size; // number of vertices in VBO
	GPU_particle *VBO_cpu; // CPU-side mapped version of our VBO
	size_t VBO_cpu_idx; // index into CPU-side buffer
	size_t VBO_writeblock_max; // pseudorandomizer
	
	GLuint tex; // OpenGL texture we're rendering to
	GLenum t; /* OpenGL texture type (typically GL_TEXTURE_2D) */
	GLuint FBO; // OpenGL framebuffer object, to render into tex
public:
	GPU_renderer() {
		pass_count=0;
		glGenBuffersARB(1,&VBO);  VBO_size=0; VBO_done=false;
		t=GL_TEXTURE_2D;
		glGenTextures(1,&tex);
		glGenFramebuffersEXT(1,&FBO);
	}
	~GPU_renderer() {
		glDeleteFramebuffersEXT(1,&FBO); FBO=0;
		glDeleteTextures(1,&tex); tex=0;
		glDeleteBuffersARB(1,&VBO); VBO=0;
	}
	
	virtual void setup(MyVizRequest &req_,bool flush_cached)
	{
		if (req_.coloring!=req.coloring
			|| req_.activeGroup!=req.activeGroup
			|| req_.doSplatter!=req.doSplatter
			|| flush_cached 
			|| VBO_done==false) 
		{ // flush our VBO cache
			pass_count=0;
			VBO_done=false;
			// FIXME: how do you erase an existing GPU VBO?  Do you really want to?
		}
		image_particle_renderer::setup(req_,flush_cached);
	}
	
	virtual needs_t needs_what(void) {
		pass_count++; 
		switch (pass_count) {
		case 1: 
			timeit("Count VBO size");
			VBO_size=0; VBO_done=false; 
			return needs_count; /* count particle pass */
		case 2: /* Allocate & map space for VBO (FIXME: in chunks?) */
			timeit("Allocate and map VBO");
			oglCheck("before VBO setup");
			printf("VBO size: %.1f Mparticles, %.3f GB\n",VBO_size*1.0e-6,VBO_size*sizeof(pb[0])*1.0e-9);
			glBindBufferARB(GL_ARRAY_BUFFER_ARB,VBO);
			
			/* Weird: reduce NVIDIA driver bug/memory leak by zeroing out the buffer first */
			glBufferDataARB(GL_ARRAY_BUFFER_ARB,0,0,GL_STATIC_DRAW_ARB);
			
			glBufferDataARB(GL_ARRAY_BUFFER_ARB,sizeof(pb[0])*VBO_size,
	        		0,GL_STATIC_DRAW_ARB);
			VBO_cpu=(GPU_particle *)glMapBuffer(GL_ARRAY_BUFFER_ARB,GL_WRITE_ONLY);
			VBO_cpu_idx=0;
			{
#define bits 12 // block size is 1<<(2*bits)
				int block=1<<(2*bits); /* randomization works on blocks of this power-of-two size */
				VBO_writeblock_max=VBO_size/block*block; // round down
			}
			oglCheck("after VBO setup");
			timeit("Copy data from CPU into VBO");
			return needs_render; /* copy particle data pass */
		case 3:
		case 4:
			pass_count=3;
			return needs_nothing; /* we're done */
		default:
			CkAbort("ResolutionServer/particlelib.cpp: GPU_renderer::needs_particles logic error (pass_count)\n");
		}
		return needs_nothing;
	}
	
	virtual void render_count(size_t particle_count) {
		VBO_size+=particle_count;
		//printf("Just got count of %ld particles, now at %ld total\n",particle_count,VBO_size);
	}
	
	virtual void render(const Vector3D<float> &location3D,unsigned char color) {
		if (pass_count!=2) CkAbort("Error!  ResolutionServer/particlelib.cpp: GPU_renderer::render called, but needs_particles is false!\n");
		// else pass_count==2: we're filling the VBO
		
		GPU_particle p; 
		p.pos=location3D;
		p.r=color;
		size_t idx=VBO_cpu_idx++; 
		
	#if 0 /* Pseudorandomize by flipping rows and columns by blocks.
	     Upload time is about doubled.
		 Render time is about halved.
		*/
		if (idx<VBO_writeblock_max)
		{
			const size_t xmask=(1<<bits)-1;
			const size_t xymask=(1<<(2*bits))-1;
			const size_t ymask=xymask-xmask;
			size_t x=idx&xmask, y=idx&ymask, high=idx&~xymask;
			idx=high|(x<<bits)|(y>>bits); // swap x and y
		}
		VBO_cpu[idx] = p; // write out data to the VBO
	#elif 1 /* Full randomization via "Fisher-Yates shuffle": swap with random element
	   Upload is 3x slower (about 10 seconds), plus some weird penalty afterwards.
	   Rendering is about 4x faster! (under 150ms)
	  */
		size_t src=rand()%(idx+1);
		VBO_cpu[idx] = VBO_cpu[src];
		VBO_cpu[src] = p;
	#else /* Do nothing: copy to GPU in-order. 
	    Upload is as fast as it can get (about 3 seconds)
		Rendering is as slow as it can get (about 650ms)
	    */
		VBO_cpu[idx] = p;
	#endif
		if (idx>VBO_size) CkAbort("Error!  ResolutionServer/particlelib.cpp: GPU_renderer::render idx exceeded size!\n");
	}
	
	virtual void finish(liveVizRequestMsg* m,ArrayElement *reducer) {
		oglCheck("at the start of GPU_renderer::finish");
	
	// Set up the particle data in the VBO
		glBindBufferARB(GL_ARRAY_BUFFER_ARB,VBO);
		if (!VBO_done) { /* First time: must copy data from CPU to GPU */
			timeit("Unmap VBO");
			if (false) { // <- DEBUG: color by location in VBO
				for (size_t i=0;i<VBO_size;i++)
					VBO_cpu[i].r=(unsigned char)(i*254.0/VBO_size); 
			}
			
			glUnmapBuffer(GL_ARRAY_BUFFER_ARB);
			VBO_cpu=NULL;
			VBO_cpu_idx=-999;
			
			glVertexPointer(3,GL_FLOAT,sizeof(GPU_particle),0);
			glColorPointer(3,GL_UNSIGNED_BYTE,sizeof(GPU_particle),
	        		(void *)sizeof(Vector3D<float>));
			glEnableClientState(GL_VERTEX_ARRAY);
			glEnableClientState(GL_COLOR_ARRAY);
			oglCheck("after VBO upload");
			
			timeit("Randomize VBO data");
#if PARTICLELIB_TRY_OPENCL
			// Randomly reorder this OpenGL VBO.  Randomization on GPU side is much faster.
			OpenCL_randomize_VBO(VBO,VBO_size);
#endif
			
			pb.resize(0); // flush CPU-side copy of data
			VBO_done=true;
		}
	
	// Set up the texture, and connect it to the framebuffer
		timeit("Set up FBO and texture");
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT,FBO);
		glBindTexture(t,tex);
		
		/**
		  Pixel format is important:
		  GL_RGBA8 works everywhere, but it's 4 bytes per pixel (about run out of RAM!)
		  GL_INTENSITY8 or GL_LUMINANCE8 works on NVIDIA, but fails (err -39) on ATI.
		  GL_R8 or GL_RG8 works on NVIDIA or ATI.
		  Everything fails (error 1) on Intel, but that's expected.
		*/
		GLenum formats[]={GL_R8,GL_RG8,GL_RGBA8,0};
		int texwid=wid, texht=ht;
		if (doVolumeRender) { // FIXME: automatic determination?
			texwid=16*wid; texht=32*ht; // because 16*32 == depth
		}
		
		for (int formatNo=0;formats[formatNo]!=0;formatNo++) {
			int level=0; // for (int level=0;texwid>>level>0;level++) // mipmap loop
			glTexImage2D(t,level,formats[formatNo], texwid>>level,texht>>level,0,
				GL_RGBA,GL_UNSIGNED_BYTE,0);
			glTexParameteri(t, GL_TEXTURE_MIN_FILTER,GL_LINEAR); /* no mipmaps */
			int err=glGetError();
			if (err==0) break; //That format worked fine!
			else { CkPrintf("Texture setup failure (%d) for format %d (%dx%d)... %s\n",
					err,formats[formatNo],texwid,texht,
					formats[formatNo+1]?"Retrying":"Giving up...");
			}
		}
		oglCheck("after tex setup");
		glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, 
					GL_COLOR_ATTACHMENT0_EXT, t, tex, 0);
		oglCheck("after tex attach");
		/* // OpenCL cooperation
		cl_mem rbm=clCreateFromGLTexture2D(gpu_env::get_ctx(),
			CL_MEM_READ_ONLY,t,0,tex,
			&errcode_img_create); ocdErr(errcode_img_create); */
	
	// Render into the texture
		timeit("Clear background");
		glViewport(0,0,texwid,texht);
		glClearColor(0.0,0.0,0.0,0.0); // clear the background to black
		glClear(GL_COLOR_BUFFER_BIT);
		oglCheck("after clear");
		
		if (req.doSplatter) { // splatter takes additive blending
			glBlendEquation(GL_FUNC_ADD);
		}
		else { // ordinary rendering is max blending mode
			glBlendEquation(GL_MAX);
		}
		
		if (req.radius == 0) { // normal case is single-pixel points
			glPointSize(1.0);
			glDisable(GL_POINT_SMOOTH);
		}
		else {
			glPointSize(req.radius); // in pixels
			glEnable(GL_POINT_SMOOTH);
		}
		
	// 3D volume rendering
		static GLhandleARB prog3D=MAKE_PROGRAM_OBJECT3(
		// GLSL vertex shader
		void main(void) {
			vec3 pos=gl_ModelViewMatrix*gl_Vertex; // in voxels
			
			// Bounds check is manual, to keep from leaving our tile
			const int n=512; // voxel size
			const int texwid=16*n; // output image size x (MUST be same as CPU!)
			const int texht =32*n; // output image size y
			if (pos.x<0.0 || pos.x>=n || pos.y<0.0 || pos.y>=n || pos.z<0.0 || pos.z>=n)
			{ // Out of bounds--discard way offscreen
				gl_Position=vec4(100.0,100.0,100.0,1.0); 
			} else { // In-bounds
				int z=int(pos.z); // z slice number gets split into X and Y tile numbers
				int tx=(z/32)*n; // tile X start
				int ty=(z%32)*n; // tile Y start
				gl_Position=vec4(
					(tx+pos.x)*(2.0/texwid)-1.0,
					(ty+pos.y)*(2.0/texht)-1.0,
					0.0,1.0);
			}
			color=gl_Color.r;
		}
		, // GLSL shared vertex/fragment code
		varying float color;
		, // GLSL pixel shader code
		void main(void) { 
			gl_FragColor=vec4(color,0.0,0.0,1.0);
		}
		);
	
	// 2D image rendering
		static GLhandleARB prog2D=MAKE_PROGRAM_OBJECT3(
		// GLSL vertex shader
		void main(void) {
			vec3 pos=gl_ModelViewMatrix*gl_Vertex; // in pixels
			// Bounds check is automatic, because we fill whole viewport.
			gl_Position=vec4(
				(pos.x)*(2.0)-1.0,
				(pos.y)*(2.0)-1.0,
				0.0,1.0);
			color=gl_Color.r;
		}
		, // GLSL shared vertex/fragment code
		varying float color;
		, // GLSL pixel shader code
		void main(void) { 
			gl_FragColor=vec4(color,0.0,0.0,1.0);
		}
		);
		
		// Upload axes
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		
		// Pick right shader
		if (doVolumeRender) {
			timeit("3D render");
			glUseProgramObjectARB(prog3D);
			// leave ccords in pixels
		} else {
			timeit("2D render");
			glUseProgramObjectARB(prog2D);
			// scale coords from pixels to 0-1 OpenGL
			glScalef(1.0f/texwid, 1.0f/texht,0.0);
		}
		
		float mat[4*4]; // COL-major matrix (OpenGL default)
		 /* pos.x             pos.y            pos.z          1.0 */
		mat[0]=xAxis.x; mat[4]=xAxis.y; mat[ 8]=xAxis.z; mat[12]=-xo;
		mat[1]=yAxis.x; mat[5]=yAxis.y; mat[ 9]=yAxis.z; mat[13]=-yo;
		mat[2]=zAxis.x; mat[6]=zAxis.y; mat[10]=zAxis.z; mat[14]=-zo;
		mat[3]=0.0f;    mat[7]=0.0f;    mat[11]=0.0f;    mat[15]=1.0f;
		glMultMatrixf(mat);
		
		if (0) { // plain GL matrix way (not shader like above)
			glTranslatef(-1.0,-1.0,0.0);
			glScalef(2.0,2.0,0.0);
		}
		glBindBufferARB(GL_ARRAY_BUFFER_ARB,VBO);
    	glDrawArrays(GL_POINTS,0,VBO_size);
    	glBindBufferARB(GL_ARRAY_BUFFER_ARB,0);
		glUseProgramObjectARB(0);
		oglCheck("after render");
		
	// Read back rendered pixels to CPU side
		timeit("Read back to CPU");
		glPixelStorei(GL_PACK_ALIGNMENT,1);
		glPixelStorei(GL_UNPACK_ALIGNMENT,1);
		if (doVolumeRender) 
		{ /* 3D mode: pull back columns to assemble tall skinny image */
			const int n=512; /* voxel resolution */
			for (int col=0;col<16;col++) {
				glReadPixels(col*n,0, //x,y start
					n,texht, // wid,ht
					GL_LUMINANCE,GL_UNSIGNED_BYTE,&pixels[col*n*texht]);
			}
		} else { /* 2D mode: simple single read */
			glReadPixels(0,0,
				texwid,texht,
				GL_LUMINANCE,GL_UNSIGNED_BYTE,&pixels[0]);
		}
		oglCheck("after readback");
	
	// Send CPU-side data off to liveViz
		timeit("Assemble via LiveViz");
		image_particle_renderer::finish(m,reducer);
		timeit(NULL);
		
	// Post-render cleanup 
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT,0);
		oglCheck("at the end of GPU_renderer::finish");
	}
};

typical_particle_renderer *start_GPU_render(MyVizRequest &req,bool doVolumeRender,bool flush_cached)
{
	static bool setup_GPU=false;
	static bool GPU_OK=false;
	if (!setup_GPU) {
		const char *err=start_GPU();
		if (err==NULL) GPU_OK=true;
		else CkPrintf("No GPU backend: %s\n",err);
		setup_GPU=true;
	}
	if (!GPU_OK) return NULL; // fall back to CPU
	
	static GPU_renderer *r=0;
	if (r==0) r=new GPU_renderer; /* <- FIXME: should be Cpv on SMP version! */
	r->setup(req,flush_cached);
	return r;
}
#endif /* PARTICLELIB_TRY_GPU */
