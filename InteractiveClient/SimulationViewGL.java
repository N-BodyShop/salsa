/*
  This class stores all the OpenGL-specific state for SimulationView.
  By moving GL into this separate class, SimulationView can be created
  even when jogl.jar is missing.
  
  Dr. Orion Sky Lawlor, lawlor@alaska.edu, 2011-03-17 (Public Domain)
*/
import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import java.awt.Dimension;
import com.sun.opengl.util.BufferUtil;
import com.sun.gluegen.runtime.BufferFactory;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Color;
import java.nio.ByteBuffer;

class SimulationViewGL {

	private SimulationView sv;
	public GLJPanel glcanvas;
	
	public SimulationViewGL(SimulationView sv_,int width2D,int height2D) {
		sv=sv_;
		
		GLCapabilities glcaps = new GLCapabilities();
		glcaps.setDoubleBuffered(true);
		glcaps.setHardwareAccelerated(true);
		
		glcanvas=new GLJPanel(glcaps);
		glcanvas.addGLEventListener(new myListener(this));
		
		sv.b=BufferUtil.newByteBuffer(sv.width3D*sv.height3D*sv.depth3D);
		//sv.b2=BufferUtil.newByteBuffer(sv.width2D*sv.height2D*3);
		b3=BufferUtil.newByteBuffer(256*3);
	}
	public javax.swing.JComponent getMainComponent() {
		return glcanvas;
	}
	public void remove() {
		sv.remove(glcanvas);
	}
	
	
	class myListener implements GLEventListener {
	SimulationViewGL svGL;
	myListener(SimulationViewGL svGL_) {svGL=svGL_;}
	/* These aren't used. */
	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2){}
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {}
	public void init(GLAutoDrawable arg0) { svGL.init(arg0); }
	public void display(GLAutoDrawable arg0) { svGL.display(arg0); }
	
	};
	
	/* OpenGL variables */
	int texture2D;
	int texture3D; /* textures used for 3D rendering (only) */
	int screen, colortable; 
	int framebuffer; /* framebuffer object */
	int texture[]=new int[1];
	boolean hasShaders=false; // if true, we can run GLSL code
	int my_program;
	ByteBuffer b3; // 1D colortable texture upload buffer
	
	/* Called by our listener */
	public void init(GLAutoDrawable arg0)
	{
		GL gl = arg0.getGL();
		
		String vendor=gl.glGetString(gl.GL_VENDOR);
		System.out.println("OpenGL vendor string: "+vendor);
		if (vendor.startsWith("Mesa") || vendor.startsWith("Tungsten"))
		{
			sv.fallbackSwingDelayed("Plain Swing is better than software OpenGL");
			return;
		}
		
		gl.glClearColor(0, 0, 0.0f, 0);
		gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, GL.GL_ONE);
		gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, GL.GL_ONE);
		if (gl.isFunctionAvailable("glCompileShaderARB"))
			hasShaders=true;

		if (hasShaders)
		{
			String shaderCode []=new String [1];
			shaderCode[0]=
			"varying vec2 texture_coordinate;" +
			"void main(){gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;" +
			"texture_coordinate = vec2(gl_MultiTexCoord0);}";
			String fragmentCode []=new String [1];
			fragmentCode[0]=
			"varying vec2 texture_coordinate; uniform sampler2D my_color_texture; uniform sampler2D my_screen_texture;"+
			"void main()" +
			"{ vec4 screenpix=texture2D(my_screen_texture, texture_coordinate);" +
			/* SUBTLE! 
			     The 0.997*screenpix.x is to avoid a texture lookup roundoff problem
				 on Tom's GeForce 7600 GS
			*/
			"gl_FragColor = texture2D(my_color_texture, vec2(0.997*screenpix.x,0.0));}";
			
			int codeLength[]=new int[1];

			int my_vertex_shader;
			int my_fragment_shader;

			// Create Shader And Program Objects
			my_program = gl.glCreateProgramObjectARB();
			my_vertex_shader = gl.glCreateShaderObjectARB(GL.GL_VERTEX_SHADER_ARB);
			my_fragment_shader = gl.glCreateShaderObjectARB(GL.GL_FRAGMENT_SHADER_ARB);

			// Load Shader Sources
			codeLength[0]=shaderCode[0].length();
			gl.glShaderSourceARB(my_vertex_shader, 1, shaderCode, (int [])null, 0);
			gl.glShaderSourceARB(my_fragment_shader, 1, fragmentCode, (int [])null, 0);

			// Compile The Shaders
			gl.glCompileShaderARB(my_vertex_shader);
			gl.glCompileShaderARB(my_fragment_shader);

			// Attach The Shader Objects To The Program Object
			gl.glAttachObjectARB(my_program, my_vertex_shader);
			gl.glAttachObjectARB(my_program, my_fragment_shader);

			// Link The Program Object
			gl.glLinkProgramARB(my_program);

			int size=10000;
			byte log[]=new byte[size];
			int one[]=new int[1];
			gl.glGetInfoLogARB(my_vertex_shader, size, one, 0, log, 0);
			gl.glGetInfoLogARB(my_fragment_shader, size, one, 0, log, 0);
			/*for(byte bytes: log)
				System.out.print((char)bytes);
			System.out.println();*/
		}

		gl.glGenTextures(1, texture, 0);
		screen=texture[0];
		gl.glGenTextures(1, texture, 0);
		colortable=texture[0];
		gl.glGenFramebuffersEXT(1, texture, 0);
		framebuffer=texture[0];
		gl.glGenTextures(1, texture, 0);
		texture2D=texture[0];
		gl.glGenTextures(1, texture, 0);
		texture3D=texture[0];
		sv.isNewImageData=true;
		
		
		
	/* The first time, run a performance test to see if 3D is even viable:
		Run an exponential search to determine the machine's rendering rate.
		Output for a typical modern card (GTX 460M) is 20000 megapixels/sec.
		Output for Mesa software rendering (Core i5 @ 2.5GHz) is 2 megapixel/sec.
		
		The volume dataset is like 134 million pixels, 
		so if you can't render 500 million/sec, we're below 5fps,
		so disable 3D volumes entirely, and go 2D only.
		
		FIXME: build a software rendering path, because you can certainly 
		beat Mesa's general OpenGL solution.
	*/
		gl.glDisable(GL.GL_DEPTH_TEST); /* similar state to voxel rendering */
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
		gl.glBlendEquation(GL.GL_MAX);
		gl.glLoadIdentity();
		gl.glScalef(0.5f/sv.width2D,0.5f/sv.height2D,0.0f);
		gl.glColor4f(0.01f,0.01f,0.01f,0.01f);
		gl.glFinish();
		long start=System.currentTimeMillis(), elapsed=0;
		int nrendered; /* in blocks of 100x100 = 10K pixels */
		for (nrendered=1;nrendered<=10000;nrendered*=2) {
			gl.glBegin(gl.GL_QUADS);
			for (int i=0;i<nrendered;i++) {
				gl.glVertex2f(0,0); 
				gl.glVertex2f(100,0); 
				gl.glVertex2f(100,100); 
				gl.glVertex2f(0,100); 
			}
			gl.glEnd();
			gl.glFinish();
			elapsed=System.currentTimeMillis()-start;
			if (elapsed>50) break; /* don't spend more than 50ms testing this */
		}
		double millionPerSecond=nrendered*0.01 / (elapsed*0.001); /* millions of pixels per second */
		if (millionPerSecond<500.0) { /* can't render full voxel dataset at 5fps */
			sv.disable3D=true; 
			
			/* This would be a good idea, but it kills ancient-X machines (crashes the X server!).
			  There, it's better to just remove jogl.jar before we even get to this point.
			sv.fallbackSwingDelayed("OpenGL exists, but is too slow.");
			*/
		}
		System.out.println("Your card can render "+(int)millionPerSecond+" megapixels per second:");
		if (sv.disable3D) System.out.println("   3D volume rendering disabled.");
		else System.out.println("   3D volume rendering enabled.");
	}
	
	private void texturemode(GL gl,int target,int filtermode) {
			gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, filtermode);
			gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, filtermode);
			gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_BORDER);
			gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_BORDER);
			gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_BORDER);
	
	}

	/* Called by our listener */
	public void display(GLAutoDrawable arg0)
	{
		if (sv.hasGL==false) return;
		GL gl = arg0.getGL();
		int j=gl.glGetError();
		if(j!=0)
			System.out.println("Beginning of display: " + j);
		gl.glPushAttrib(GL.GL_ALL_ATTRIB_BITS);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT|GL.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();						// Reset The View
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);			// Really Nice Perspective Calculation
		
		if (sv.disable3D || (sv.uptodate2D && sv.coord.equals(sv.coord2D))) 
		{ /* 2D screen rendering: colorize on CPU, upload texture, draw. */
			sv.debugNetwork("--display2D--");
			synchronized(sv.b2Lock) /* colorize on CPU */
			{
				ByteBuffer b2=BufferUtil.newByteBuffer(sv.pixels.length*3); // cwidth2D*cheight2D*3);
				b2.clear();
				for(int i=0;i<sv.pixels.length;i++)
				{
					int index=0xff&(int)sv.pixels[i];
					b2.put(sv.colorBar.cm_red[index]);
					b2.put(sv.colorBar.cm_green[index]);
					b2.put(sv.colorBar.cm_blue[index]);
				}
				b2.flip();
				gl.glBindTexture(GL.GL_TEXTURE_2D, texture2D);
				gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8, sv.cwidth2D, sv.cheight2D, 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, b2);
				texturemode(gl,GL.GL_TEXTURE_2D,GL.GL_LINEAR);
			}
			gl.glDisable(GL.GL_ALPHA_TEST); /* too agressive--loses too many points */
			gl.glDisable(GL.GL_BLEND);
			gl.glColor4f(1f, 1f, 1f, 1f);
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glBegin(GL.GL_QUADS);
				gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-1.0f, 1.0f, 0.0f);
				gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f( 1.0f, 1.0f, 0.0f);
				gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, 0.0f);
				gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, 0.0f);
			gl.glEnd();
			gl.glDisable(GL.GL_TEXTURE_2D);
		}
		else if (sv.uptodate3D) /* Use 3D, at least until 2D arrives... */
		{
			sv.debugNetwork("--display3D--");
			gl.glBindTexture(GL.GL_TEXTURE_2D, screen);
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, sv.width2D, sv.height2D, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);

			gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, framebuffer);
			gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0_EXT, GL.GL_TEXTURE_2D, screen, 0);


			gl.glClear(GL.GL_COLOR_BUFFER_BIT|GL.GL_DEPTH_BUFFER_BIT);
			gl.glViewport(0,0,sv.width2D,sv.height2D);
			gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glMatrixMode(GL.GL_MODELVIEW);
			gl.glLoadIdentity();


			if(sv.isNewImageData)
			{
				texture[0]=texture3D;
				gl.glDeleteTextures(1, texture, 0);
				gl.glGenTextures(1, texture, 0);
				texture3D=texture[0];
				gl.glBindTexture(GL.GL_TEXTURE_3D, texture3D);
				synchronized(sv.bLock)
				{
					gl.glTexImage3D(GL.GL_TEXTURE_3D, 0, GL.GL_LUMINANCE, sv.width3D, sv.height3D, sv.height3D, 0, GL.GL_LUMINANCE, GL.GL_UNSIGNED_BYTE, sv.b);
				}
				sv.isNewImageData=false;
			}
			else
				gl.glBindTexture(GL.GL_TEXTURE_3D, texture3D);

			texturemode(gl,GL.GL_TEXTURE_3D,GL.GL_NEAREST);
			gl.glLoadIdentity();
			gl.glDisable(GL.GL_ALPHA_TEST); /* too agressive--loses too many points */
			gl.glDisable(GL.GL_DEPTH_TEST); /* don't do Z buffer (screws up overlaps, esp. w/blending) */
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
			if(sv.maxMode)
				gl.glBlendEquation(GL.GL_MAX);
			else
				gl.glBlendEquation(GL.GL_FUNC_ADD);
			
			
			//scale and rotate
			double scale=sv.coord.factor/sv.coord3D.factor;
			double matrix []=new double[16];
			matrix[0]=sv.coord.x.x; matrix[4]=sv.coord.x.y; matrix[8]=sv.coord.x.z;  matrix[12]=0;
			matrix[1]=sv.coord.y.x; matrix[5]=sv.coord.y.y; matrix[9]=sv.coord.y.z;  matrix[13]=0;
			matrix[2]=sv.coord.z.x; matrix[6]=sv.coord.z.y; matrix[10]=sv.coord.z.z; matrix[14]=0;
			matrix[3]=0;	matrix[7]=0;	matrix[11]=0;	 matrix[15]=1;
			gl.glScaled(scale /(sv.width2D*sv.delta), scale/(sv.height2D*sv.delta), scale*0);
			gl.glMultMatrixd(matrix, 0);
			
			Vector3D o=sv.coord3D.origin.minus(sv.coord.origin); /* simulation-space distance between origins */
			o=o.scalarMultiply(sv.coord3D.factor);
			gl.glTranslated(o.x,o.y,o.z); // shift so 3D data lines up with 2D coordinate system
			
	
			/* Outline the 3D region with a box.  (Ugly!) */
			gl.glColor3d(0.2, 0.2, 0.2);
			gl.glBegin(GL.GL_LINE_LOOP);
				gl.glVertex3d(-1, 1, 1);
				gl.glVertex3d( 1, 1, 1);
				gl.glVertex3d( 1,-1, 1);
				gl.glVertex3d(-1,-1, 1);
			gl.glEnd();
			gl.glBegin(GL.GL_LINE_LOOP);
				gl.glVertex3d(-1, 1,-1);
				gl.glVertex3d( 1, 1,-1);
				gl.glVertex3d( 1,-1,-1);
				gl.glVertex3d(-1,-1,-1);
			gl.glEnd();
			gl.glBegin(GL.GL_LINES);
				gl.glVertex3d(-1, 1, 1);
				gl.glVertex3d(-1, 1,-1);

				gl.glVertex3d( 1, 1, 1);
				gl.glVertex3d( 1, 1,-1);

				gl.glVertex3d( 1,-1, 1);
				gl.glVertex3d( 1,-1,-1);

				gl.glVertex3d(-1,-1, 1);
				gl.glVertex3d(-1,-1,-1);
			gl.glEnd();

		
			/* Draw slices of the 3D volume */
			gl.glEnable(GL.GL_TEXTURE_3D);
			double intensity=1.0;
			switch (getFacing())
			{
				case facing_z:
					gl.glScalef(1, 1, 2);
					gl.glTranslatef(0, 0, -1.0f/2);
					gl.glBegin(GL.GL_QUADS);
					gl.glColor4d(intensity,intensity,intensity,intensity);
					for (int slice=0;slice<sv.depth3D;slice++)
					{
						float z=((slice)/((float)sv.depth3D)*1.0f);
						gl.glTexCoord3f(0.0f, 0.0f, z/1.0f);
						gl.glVertex3f(-1.0f, -1.0f, z);
						gl.glTexCoord3f(1.0f, 0.0f, z/1.0f);
						gl.glVertex3f(1.0f, -1.0f, z);
						gl.glTexCoord3f(1.0f, 1.0f, z/1.0f);
						gl.glVertex3f(1.0f, 1.0f, z);
						gl.glTexCoord3f(0.0f, 1.0f, z/1.0f);
						gl.glVertex3f(-1.0f, 1.0f, z);
					}
					gl.glEnd();
					break;
				case facing_y:
					gl.glScalef(1, 2, 1);
					gl.glTranslatef(0, -1.0f/2, 0);
					gl.glBegin(GL.GL_QUADS);
					gl.glColor4d(intensity,intensity,intensity,intensity);
					for (int slice=0;slice<sv.depth3D;slice++)
					{
						float y=(slice)/((float)sv.depth3D)*1.0f;
						gl.glTexCoord3f(0.0f, y/1.0f, 0.0f);
						gl.glVertex3f(-1.0f, y, -1.0f);
						gl.glTexCoord3f(1.0f, y/1.0f, 0);
						gl.glVertex3f(1.0f, y, -1.0f);
						gl.glTexCoord3f(1.0f, y/1.0f, 1.0f);
						gl.glVertex3f(1.0f, y, 1.0f);
						gl.glTexCoord3f(0.0f, y/1.0f, 1.0f);
						gl.glVertex3f(-1.0f, y, 1.0f);
					}
					gl.glEnd();
					break;
				case facing_x:
					gl.glScalef(2, 1, 1);
					gl.glTranslatef(-1.0f/2, 0 ,0);
					gl.glBegin(GL.GL_QUADS);
					gl.glColor4d(intensity,intensity,intensity,intensity);
					for (int slice=0;slice<sv.depth3D;slice++)
					{
						float x=(slice)/((float)sv.depth3D)*1.0f;
						gl.glTexCoord3f(x/1.0f, 0.0f, 0.0f);
						gl.glVertex3f(x, -1.0f, -1.0f);
						gl.glTexCoord3f(x/1.0f, 1.0f, 0);
						gl.glVertex3f(x, 1.0f, -1.0f);
						gl.glTexCoord3f(x/1.0f, 1.0f, 1.0f);
						gl.glVertex3f(x, 1.0f, 1.0f);
						gl.glTexCoord3f(x/1.0f, 0.0f, 1.0f);
						gl.glVertex3f(x, -1.0f, 1.0f);
					}
					gl.glEnd();
					break;
				default:
					System.out.println("Facing Error");
			}
			gl.glDisable(GL.GL_TEXTURE_3D);
			gl.glDisable(GL.GL_BLEND);

			if (hasShaders)
			{
				gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
				gl.glMatrixMode(GL.GL_PROJECTION);
				gl.glLoadIdentity();
				gl.glMatrixMode(GL.GL_MODELVIEW);
				gl.glLoadIdentity();
				gl.glBindTexture(GL.GL_TEXTURE_2D, screen);
				texturemode(gl,GL.GL_TEXTURE_2D,GL.GL_NEAREST);

				b3.clear();
				for(int i=0; i<256; i++)
				{
					b3.put(sv.colorBar.cm_red[i]);
					b3.put(sv.colorBar.cm_green[i]);
					b3.put(sv.colorBar.cm_blue[i]);
				}
				b3.flip();
				gl.glActiveTexture(GL.GL_TEXTURE1);
				gl.glBindTexture(GL.GL_TEXTURE_2D, colortable);
				gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8, sv.colorBar.tableSize, 1, 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, b3);
				texturemode(gl,GL.GL_TEXTURE_2D,GL.GL_NEAREST); /* do not blend family colors */
				gl.glActiveTexture(GL.GL_TEXTURE0);


				// Use The Program Object Instead Of Fixed Function OpenGL
				gl.glUseProgramObjectARB(my_program);
				gl.glUniform1i(gl.glGetUniformLocationARB(my_program, "my_screen_texture"), 0);
				gl.glUniform1i(gl.glGetUniformLocationARB(my_program, "my_color_texture"), 1);
				gl.glBegin(GL.GL_QUADS);
					gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-1.0f,  1.0f, 0.0f);
					gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f( 1.0f,  1.0f, 0.0f);
					gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, 0.0f);
					gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, 0.0f);
				gl.glEnd();

				gl.glUseProgramObjectARB(0);
			}
			else //no shaders--colorize rendered image on CPU
			{
				int w=sv.width2D;
				int h=sv.height2D;
				ByteBuffer temp=BufferFactory.newDirectByteBuffer(w*h);
				ByteBuffer temp2=BufferFactory.newDirectByteBuffer(w*h*3);
				gl.glReadPixels(0, 0, w, h, GL.GL_RED, GL.GL_UNSIGNED_BYTE, temp);
				gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
				gl.glMatrixMode(GL.GL_PROJECTION);
				gl.glLoadIdentity();
				gl.glMatrixMode(GL.GL_MODELVIEW);
				gl.glLoadIdentity();
				for(int i=0; i<w*h; i++)
				{
					int index=0xff&(int)temp.get(i);
					//if(index!=0)
						//System.out.println(index);
					temp2.put(sv.colorBar.cm_red[index]);
					temp2.put(sv.colorBar.cm_green[index]);
					temp2.put(sv.colorBar.cm_blue[index]);
				}
				temp2.flip();
				gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
				gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8, w, h, 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, temp2);
				texturemode(gl,GL.GL_TEXTURE_2D,GL.GL_LINEAR);
				gl.glEnable(GL.GL_TEXTURE_2D);
				gl.glBegin(GL.GL_QUADS);
					gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex3f(-1.0f,  1.0f, 0.0f);
					gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex3f( 1.0f,  1.0f, 0.0f);
					gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex3f( 1.0f, -1.0f, 0.0f);
					gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex3f(-1.0f, -1.0f, 0.0f);
				gl.glEnd();
				gl.glDisable(GL.GL_TEXTURE_2D);
			}
		}
		else { /* no 2D image, no 3D image (yet).  Just wait! */
			sv.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			java.awt.Graphics g= sv.getGraphics();
			g.setColor(java.awt.Color.green);
			g.drawString("Waiting for network...", 0, 80);
			
			/* Annoyingly, AWT text may not actually show up, so draw 
			   a big red yield sign... */
			gl.glLineWidth(20.0f);
			gl.glColor3d(0.9,0.2,0.0);
			gl.glBegin(gl.GL_LINE_LOOP);
			gl.glVertex2d(-0.3,0.2);
			gl.glVertex2d(+0.3,0.2);
			gl.glVertex2d(0.0,-0.3);
			gl.glEnd();
		}
		
		if(sv.networkBusy)
		{
			java.awt.Graphics g= sv.getGraphics();
			g.setColor(java.awt.Color.green);
			g.drawString("Loading New Image...", 0, 20);
		}
		//drawSelection(); //<- doesn't seem to work.  Why?
		gl.glPopAttrib();
		j=gl.glGetError();
		if(j!=0)
			System.out.println("End of display: " + j);
	}//end display
	
	static final int facing_z=1, facing_y=2, facing_x=3;

	/* Decide the order to draw the slices of our 3D volume image.
	  Pre: X, Y, and Z vectors are orthogonal to each other
	  Post: Return 
	      1 if the z axis is most parallel with camera 
	      2 if the Y axis is most parallel with camera 
		  3 if the x axis is most parallel with camera 
		  failure to compute returns a -1
	*/
	public int getFacing()
	{
		Vector3D x=sv.coord.x, y=sv.coord.y, z=sv.coord.z;
		if (Math.abs(z.z)>=Math.abs(z.x)&&Math.abs(z.z)>=Math.abs(z.y))
			return facing_z;
		else
			if (Math.abs(z.y)>=Math.abs(z.x)&&Math.abs(z.y)>=Math.abs(z.z))
				return facing_y;
			else
				if (Math.abs(z.x)>=Math.abs(z.z)&&Math.abs(z.x)>=Math.abs(z.y))
					return facing_x;
		return -1;
	}
	

};
