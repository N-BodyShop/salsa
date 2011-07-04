/**
 Trivial particle rendering library
 	- You pass in a block of particles
	- It renders the particles "correctly" (2D or 3D, CPU or GPU, etc.)
 
 Dr. Orion Sky Lawlor, lawlor@alaska.edu, 2011-06-14 (Public Domain)
*/
#ifndef __OSL_PARTICLELIB
#define __OSL_PARTICLELIB

/**
  Superclass of all particle renderers:
     CPU-side renderers, like point and splat.
	 GPU-side renderers, typically the OpenGL/VBO version.
*/
class particle_renderer {
public:
	typedef enum {
		needs_nothing=0, // do not pass anything more; ready to finish.
		needs_render=1, // pass particle data.
		needs_count=2, // pass particle counts only.
	} needs_t;

	/** 
	   Return an enum indicating what this renderer needs.  The typical usage is:
	    
	   particle_renderer::needs_t needs;
	   while (particle_renderer::needs_nothing!=(needs=renderer->needs_what())) {
	   	switch (needs) {
		   ...
		}
	   }
	*/
	virtual needs_t needs_what(void) =0;
	
	/** Call this in response to "needs_render".
	  Particle upload: add this one particle to the set to be rendered.
	    This is a bare particle with color.
	    This can only be called if "need_render" is true.
	*/
	virtual void render(const Vector3D<float> &location3D,unsigned char color)=0;
	
	/** Call this in response to "needs_counts". */
	virtual void render_count(size_t particle_count);
	
	/** End a rendering session.  This should always be called after all particles are uploaded. */
	virtual void finish(liveVizRequestMsg* m,ArrayElement *reducer)=0;
};


/**
  Begin a rendering session, typically in response to a liveViz call.
	 @param flush_cached Indicates the renderer should not use any stored particles (e.g., data changed).
	 @return The renderer to use to finish this rendering session.  Do not delete this object; call "finish" only.
*/
particle_renderer *start_particle_render(MyVizRequest &req_,bool flush_cached);


#endif
